package com.example.musicapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicapp.library.Library
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.launchAudioImport
import com.example.musicapp.player.PlayerController
import com.example.musicapp.player.RepeatMode
import kotlinx.coroutines.delay

private enum class Tab { Library, Playlists }

@Composable
fun MusicApp() {
    val library = remember { Library() }
    val player = remember { PlayerController() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var tab by remember { mutableStateOf(Tab.Library) }
    var openPlaylistId by remember { mutableStateOf<String?>(null) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.width(160.dp).fillMaxHeight().padding(12.dp)) {
                        TextButton(onClick = { tab = Tab.Library; openPlaylistId = null }) { Text("Library") }
                        TextButton(onClick = { tab = Tab.Playlists; openPlaylistId = null }) { Text("Playlists") }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                        when (tab) {
                            Tab.Library -> LibraryScreen(library, player)
                            Tab.Playlists -> PlaylistsScreen(
                                library = library,
                                player = player,
                                openId = openPlaylistId,
                                onOpen = { openPlaylistId = it },
                                onBack = { openPlaylistId = null },
                            )
                        }
                    }
                }
                NowPlayingBar(player)
            }
        }
    }
}

@Composable
private fun LibraryScreen(library: Library, player: PlayerController) {
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    var query by remember { mutableStateOf("") }
    val visible = library.tracks.filter { t ->
        query.isBlank() ||
            t.title.contains(query, ignoreCase = true) ||
            t.artist.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Library", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Button(onClick = { launchAudioImport { library.addTrack(it) } }) { Text("Upload song") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search title or artist") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(visible, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    playlists = library.playlists,
                    onPlay = { player.play(track, visible) },
                    onAdd = { playlistId -> library.addToPlaylist(playlistId, track.id) },
                    onDelete = { pendingDelete = track },
                )
            }
        }
    }

    pendingDelete?.let { track ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${track.title}\"?") },
            text = {
                Text(
                    if (track.source != null) {
                        "This removes it from your library and playlists, and deletes the uploaded file. This can't be undone."
                    } else {
                        "This removes it from your library and playlists."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    player.onTrackRemoved(track.id)
                    library.removeTrack(track.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlaylistsScreen(
    library: Library,
    player: PlayerController,
    openId: String?,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    val playlist = library.playlists.find { it.id == openId }

    if (playlist == null) {
        Column(Modifier.fillMaxSize()) {
            Text("Playlists", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New playlist") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        library.createPlaylist(newName.trim())
                        newName = ""
                    }
                }) { Text("Create") }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(library.playlists, key = { it.id }) { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onOpen(p.id) }, modifier = Modifier.weight(1f)) {
                            Text("${p.name} (${p.trackIds.size})")
                        }
                        TextButton(onClick = { library.deletePlaylist(p.id) }) { Text("Delete") }
                    }
                }
            }
        }
    } else {
        val tracks = library.tracksIn(playlist)
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text(playlist.name, style = MaterialTheme.typography.headlineSmall)
            }
            Row {
                Button(
                    onClick = { tracks.firstOrNull()?.let { player.play(it, tracks) } },
                    enabled = tracks.any { it.source != null },
                ) { Text("Play all") }
            }
            LazyColumn {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        playlists = library.playlists,
                        onPlay = { player.play(track, tracks) },
                        onAdd = { playlistId -> library.addToPlaylist(playlistId, track.id) },
                        onRemoveFromPlaylist = { library.removeFromPlaylist(playlist.id, track.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    playlists: List<Playlist>,
    onPlay: () -> Unit,
    onAdd: (String) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge)
            val details = buildList {
                add(track.artist)
                if (track.durationMs > 0) add(formatTime(track.durationMs))
                if (track.source == null) add("sample")
            }
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onPlay) { Text("Play") }
        Box {
            TextButton(onClick = { menuOpen = true }) { Text("+ Playlist") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                playlists.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = {
                            onAdd(p.id)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        if (onRemoveFromPlaylist != null) {
            TextButton(onClick = onRemoveFromPlaylist) { Text("Remove") }
        }
        if (onDelete != null) {
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun NowPlayingBar(player: PlayerController) {
    // Keeps the progress bar moving while something plays.
    LaunchedEffect(Unit) {
        while (true) {
            player.tick()
            delay(250)
        }
    }

    val track = player.current
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = player.durationMs.coerceAtLeast(1L)
    val fraction = dragging ?: (player.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(track?.title ?: "Nothing playing", style = MaterialTheme.typography.titleSmall)
            if (track != null && track.source == null) {
                Text(
                    "Sample track: upload a song to hear audio",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Slider(
                value = fraction,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { player.seekTo((it * duration).toLong()) }
                    dragging = null
                },
                enabled = track?.source != null && player.durationMs > 0,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${formatTime(player.positionMs)} / ${formatTime(player.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { player.previous() }) { Text("⏮") }
                Button(onClick = { player.toggle() }, enabled = track?.source != null) {
                    Text(if (player.isPlaying) "Pause" else "Play")
                }
                TextButton(onClick = { player.next() }) { Text("⏭") }
                TextButton(onClick = { player.shuffle = !player.shuffle }) {
                    Text(if (player.shuffle) "Shuffle on" else "Shuffle off")
                }
                TextButton(onClick = { player.cycleRepeat() }) {
                    Text(
                        when (player.repeat) {
                            RepeatMode.Off -> "Repeat off"
                            RepeatMode.All -> "Repeat all"
                            RepeatMode.One -> "Repeat one"
                        }
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
