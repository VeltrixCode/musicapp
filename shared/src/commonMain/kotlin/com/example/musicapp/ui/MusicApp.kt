package com.example.musicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.launchAudioImport
import com.example.musicapp.player.PlayerController
import com.example.musicapp.player.RepeatMode
import kotlinx.coroutines.delay
import kotlin.math.abs

private object SpotifyColors {
    val Black = Color(0xFF000000)
    val Background = Color(0xFF121212)
    val Sidebar = Color(0xFF000000)
    val Card = Color(0xFF181818)
    val CardHover = Color(0xFF282828)
    val SurfaceElevated = Color(0xFF242424)
    val SurfaceHover = Color(0xFF282828)
    val Green = Color(0xFF1DB954)
    val GreenHover = Color(0xFF1ED760)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextMuted = Color(0xFF727272)
    val Divider = Color(0xFF282828)
    val Red = Color(0xFFE91429)
}

private val CoverGradients = listOf(
    listOf(Color(0xFF842A8C), Color(0xFF38184C)),
    listOf(Color(0xFF1E3264), Color(0xFF0E1626)),
    listOf(Color(0xFFE13300), Color(0xFF5B1700)),
    listOf(Color(0xFF148A08), Color(0xFF0A3B05)),
    listOf(Color(0xFFB02897), Color(0xFF4C0E40)),
    listOf(Color(0xFFE8115B), Color(0xFF540720)),
    listOf(Color(0xFF503750), Color(0xFF201620)),
    listOf(Color(0xFF27856A), Color(0xFF0F362B)),
)

private fun getGradientFor(text: String): Brush {
    val idx = abs(text.hashCode()) % CoverGradients.size
    val colors = CoverGradients[idx]
    return Brush.linearGradient(colors)
}

private enum class NavSection { Library, Playlists }

@Composable
fun MusicApp() {
    val library = remember { Library() }
    val player = remember { PlayerController() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var navSection by remember { mutableStateOf(NavSection.Library) }
    var openPlaylistId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val spotifyColorScheme = darkColorScheme(
        primary = SpotifyColors.Green,
        onPrimary = SpotifyColors.Black,
        surface = SpotifyColors.Background,
        onSurface = SpotifyColors.TextPrimary,
        background = SpotifyColors.Background,
        onBackground = SpotifyColors.TextPrimary,
    )

    MaterialTheme(colorScheme = spotifyColorScheme) {
        Surface(Modifier.fillMaxSize(), color = SpotifyColors.Background) {
            Column(Modifier.fillMaxSize()) {
                // Top row: Sidebar + Main Content Area
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // Left Sidebar
                    SpotifySidebar(
                        library = library,
                        currentSection = navSection,
                        openPlaylistId = openPlaylistId,
                        onSelectSection = {
                            navSection = it
                            openPlaylistId = null
                        },
                        onSelectPlaylist = {
                            navSection = NavSection.Playlists
                            openPlaylistId = it
                        },
                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                    )

                    // Main Content
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(SpotifyColors.Background)
                    ) {
                        // Top Navigation / Search Bar
                        SpotifyTopBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onUploadSong = {
                                launchAudioImport { library.addTrack(it) }
                            },
                        )

                        // Content Body
                        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                            when (navSection) {
                                NavSection.Library -> {
                                    LibraryView(
                                        library = library,
                                        player = player,
                                        searchQuery = searchQuery,
                                    )
                                }
                                NavSection.Playlists -> {
                                    PlaylistDetailView(
                                        library = library,
                                        player = player,
                                        playlistId = openPlaylistId,
                                        onBack = {
                                            navSection = NavSection.Library
                                            openPlaylistId = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Fixed Bottom Player Bar
                SpotifyNowPlayingBar(player = player, library = library)
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                library.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
        )
    }
}

@Composable
private fun SpotifySidebar(
    library: Library,
    currentSection: NavSection,
    openPlaylistId: String?,
    onSelectSection: (NavSection) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    Column(
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(SpotifyColors.Sidebar)
            .padding(16.dp)
    ) {
        // App Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp, start = 8.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SpotifyColors.Green),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", fontSize = 20.sp, color = SpotifyColors.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "MusicApp",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SpotifyColors.TextPrimary,
                    fontSize = 20.sp,
                )
            )
        }

        // Navigation Menu
        SidebarNavItem(
            label = "Моя медиатека",
            icon = "📚",
            isSelected = currentSection == NavSection.Library && openPlaylistId == null,
            onClick = { onSelectSection(NavSection.Library) }
        )

        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpotifyColors.Divider))
        Spacer(Modifier.height(16.dp))

        // Playlists Section Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "ПЛЕЙЛИСТЫ",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SpotifyColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onCreatePlaylist() },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = SpotifyColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Playlists List
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(library.playlists, key = { _, p -> p.id }) { _, p ->
                val isSelected = openPlaylistId == p.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) SpotifyColors.SurfaceElevated else Color.Transparent)
                        .clickable { onSelectPlaylist(p.id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(getGradientFor(p.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isSelected) SpotifyColors.Green else SpotifyColors.TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${p.trackIds.size} трек(ов)",
                            style = MaterialTheme.typography.bodySmall.copy(color = SpotifyColors.TextMuted),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SpotifyColors.SurfaceElevated else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) SpotifyColors.TextPrimary else SpotifyColors.TextSecondary,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun SpotifyTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUploadSong: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Search Input in Spotify Style
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Что хочешь послушать?",
                    color = SpotifyColors.TextMuted,
                    fontSize = 13.sp
                )
            },
            leadingIcon = { Text("🔍", fontSize = 14.sp) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Text(
                        "✕",
                        color = SpotifyColors.TextSecondary,
                        modifier = Modifier.clickable { onSearchQueryChange("") }.padding(4.dp)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(500.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SpotifyColors.SurfaceElevated,
                unfocusedContainerColor = SpotifyColors.SurfaceElevated,
                focusedBorderColor = SpotifyColors.TextSecondary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = SpotifyColors.TextPrimary,
                unfocusedTextColor = SpotifyColors.TextPrimary,
            ),
            modifier = Modifier.width(360.dp).height(48.dp)
        )

        // Upload Button (Spotify Green Pill)
        Button(
            onClick = onUploadSong,
            shape = RoundedCornerShape(500.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyColors.Green,
                contentColor = SpotifyColors.Black
            ),
            modifier = Modifier.height(44.dp)
        ) {
            Text("＋ Загрузить трек", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LibraryView(
    library: Library,
    player: PlayerController,
    searchQuery: String,
) {
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    val visible = library.tracks.filter { t ->
        searchQuery.isBlank() ||
            t.title.contains(searchQuery, ignoreCase = true) ||
            t.artist.contains(searchQuery, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        // Hero Banner
        HeroBanner(
            title = "Моя медиатека",
            subtitle = "${library.tracks.size} трек(ов)",
            badge = "КОЛЛЕКЦИЯ",
            gradient = getGradientFor("Library"),
            canPlay = visible.any { it.source != null },
            isPlaying = player.isPlaying && visible.any { it.id == player.current?.id },
            onPlayAll = {
                visible.firstOrNull { it.source != null }?.let { player.play(it, visible) }
            }
        )

        Spacer(Modifier.height(20.dp))

        // Tracks Table Header
        TableHeader()

        // Track rows
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(visible, key = { _, track -> track.id }) { index, track ->
                TrackRowItem(
                    index = index + 1,
                    track = track,
                    isPlaying = player.current?.id == track.id && player.isPlaying,
                    isCurrent = player.current?.id == track.id,
                    playlists = library.playlists,
                    onPlay = { player.play(track, visible) },
                    onAddToPlaylist = { pId -> library.addToPlaylist(pId, track.id) },
                    onDelete = { pendingDelete = track },
                )
            }
        }
    }

    pendingDelete?.let { track ->
        DeleteTrackDialog(
            track = track,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                player.onTrackRemoved(track.id)
                library.removeTrack(track.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun PlaylistDetailView(
    library: Library,
    player: PlayerController,
    playlistId: String?,
    onBack: () -> Unit,
) {
    val playlist = library.playlists.find { it.id == playlistId }
    if (playlist == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Плейлист не найден", color = SpotifyColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Вернуться") }
        }
        return
    }

    val tracks = library.tracksIn(playlist)
    var pendingDeletePlaylist by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Назад", color = SpotifyColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { pendingDeletePlaylist = true }) {
                Text("Удалить плейлист", color = SpotifyColors.Red, fontSize = 13.sp)
            }
        }

        HeroBanner(
            title = playlist.name,
            subtitle = "${tracks.size} трек(ов)",
            badge = "ПЛЕЙЛИСТ",
            gradient = getGradientFor(playlist.name),
            canPlay = tracks.any { it.source != null },
            isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
            onPlayAll = {
                tracks.firstOrNull { it.source != null }?.let { player.play(it, tracks) }
            }
        )

        Spacer(Modifier.height(20.dp))
        TableHeader()

        if (tracks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("В этом плейлисте пока нет треков. Добавьте их из Медиатеки!", color = SpotifyColors.TextMuted)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRowItem(
                        index = index + 1,
                        track = track,
                        isPlaying = player.current?.id == track.id && player.isPlaying,
                        isCurrent = player.current?.id == track.id,
                        playlists = library.playlists,
                        onPlay = { player.play(track, tracks) },
                        onAddToPlaylist = { pId -> library.addToPlaylist(pId, track.id) },
                        onRemoveFromPlaylist = { library.removeFromPlaylist(playlist.id, track.id) }
                    )
                }
            }
        }
    }

    if (pendingDeletePlaylist) {
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylist = false },
            title = { Text("Удалить плейлист \"${playlist.name}\"?") },
            text = { Text("Плейлист будет удален. Треки останутся в вашей медиатеке.") },
            confirmButton = {
                TextButton(onClick = {
                    library.deletePlaylist(playlist.id)
                    pendingDeletePlaylist = false
                    onBack()
                }) { Text("Удалить", color = SpotifyColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlaylist = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun HeroBanner(
    title: String,
    subtitle: String,
    badge: String,
    gradient: Brush,
    canPlay: Boolean,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SpotifyColors.SurfaceElevated, SpotifyColors.Background)
                )
            )
            .padding(20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Big Square Cover
        Box(
            Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text("♫", fontSize = 54.sp, color = Color.White.copy(alpha = 0.85f))
        }

        Spacer(Modifier.width(20.dp))

        Column(Modifier.weight(1f)) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SpotifyColors.TextSecondary,
                    letterSpacing = 1.sp
                )
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpotifyColors.TextPrimary,
                    fontSize = 32.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = SpotifyColors.TextSecondary)
            )
            Spacer(Modifier.height(12.dp))

            // Big Green Circular Play Button
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canPlay) SpotifyColors.Green else SpotifyColors.SurfaceHover)
                    .clickable(enabled = canPlay) { onPlayAll() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isPlaying) "⏸" else "▶",
                    fontSize = 20.sp,
                    color = SpotifyColors.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(width = 0.dp, color = Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", color = SpotifyColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(36.dp))
        Text("НАЗВАНИЕ", color = SpotifyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("ДЛИТЕЛЬНОСТЬ", color = SpotifyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Text("ДЕЙСТВИЯ", color = SpotifyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpotifyColors.Divider))
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun TrackRowItem(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    playlists: List<Playlist>,
    onPlay: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) SpotifyColors.SurfaceElevated else Color.Transparent)
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number or playing icon
        Box(Modifier.width(36.dp)) {
            if (isPlaying) {
                Text("▶", color = SpotifyColors.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    index.toString(),
                    color = if (isCurrent) SpotifyColors.Green else SpotifyColors.TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        // Thumbnail artwork
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(getGradientFor(track.title)),
            contentAlignment = Alignment.Center
        ) {
            Text("♪", color = Color.White, fontSize = 16.sp)
        }

        Spacer(Modifier.width(12.dp))

        // Title and Artist
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isCurrent) SpotifyColors.Green else SpotifyColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodySmall.copy(color = SpotifyColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            if (track.durationMs > 0) formatTime(track.durationMs) else "--:--",
            color = SpotifyColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(90.dp)
        )

        // Actions
        Row(Modifier.width(100.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Text(
                    "＋",
                    color = SpotifyColors.TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { menuOpen = true }
                        .padding(6.dp)
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(SpotifyColors.SurfaceElevated)
                ) {
                    if (playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Нет плейлистов", color = SpotifyColors.TextMuted) },
                            onClick = { menuOpen = false }
                        )
                    } else {
                        playlists.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, color = SpotifyColors.TextPrimary) },
                                onClick = {
                                    onAddToPlaylist(p.id)
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            if (onRemoveFromPlaylist != null) {
                Text(
                    "✕",
                    color = SpotifyColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onRemoveFromPlaylist() }
                        .padding(6.dp)
                )
            }

            if (onDelete != null) {
                Text(
                    "🗑",
                    color = SpotifyColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotifyNowPlayingBar(
    player: PlayerController,
    library: Library,
) {
    // Keep playback tick active
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

    Surface(
        color = SpotifyColors.Card,
        modifier = Modifier.fillMaxWidth().height(88.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Current Track Info (220.dp)
            Row(
                Modifier.width(220.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track != null) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(getGradientFor(track.title)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♫", color = Color.White, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpotifyColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = SpotifyColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SpotifyColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", color = SpotifyColors.TextMuted, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Ничего не играет",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SpotifyColors.TextMuted)
                    )
                }
            }

            // Center: Playback Controls & Timeline (Weight 1f)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Shuffle
                    Text(
                        "🔀",
                        fontSize = 16.sp,
                        color = if (player.shuffle) SpotifyColors.Green else SpotifyColors.TextMuted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { player.shuffle = !player.shuffle }
                            .padding(4.dp)
                    )

                    // Previous
                    Text(
                        "⏮",
                        fontSize = 18.sp,
                        color = SpotifyColors.TextPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { player.previous() }
                            .padding(4.dp)
                    )

                    // Play / Pause Circular Button
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SpotifyColors.TextPrimary)
                            .clickable(enabled = track?.source != null) { player.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (player.isPlaying) "⏸" else "▶",
                            color = SpotifyColors.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Next
                    Text(
                        "⏭",
                        fontSize = 18.sp,
                        color = SpotifyColors.TextPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { player.next() }
                            .padding(4.dp)
                    )

                    // Repeat
                    Text(
                        when (player.repeat) {
                            RepeatMode.Off -> "🔁"
                            RepeatMode.All -> "🔁"
                            RepeatMode.One -> "🔂"
                        },
                        fontSize = 16.sp,
                        color = if (player.repeat != RepeatMode.Off) SpotifyColors.Green else SpotifyColors.TextMuted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { player.cycleRepeat() }
                            .padding(4.dp)
                    )
                }

                // Progress Bar Row
                Row(
                    Modifier.fillMaxWidth(0.85f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(player.positionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyColors.TextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.width(36.dp)
                    )

                    Slider(
                        value = fraction,
                        onValueChange = { dragging = it },
                        onValueChangeFinished = {
                            dragging?.let { player.seekTo((it * duration).toLong()) }
                            dragging = null
                        },
                        enabled = track?.source != null && player.durationMs > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = SpotifyColors.TextPrimary,
                            activeTrackColor = SpotifyColors.Green,
                            inactiveTrackColor = SpotifyColors.SurfaceElevated,
                        ),
                        modifier = Modifier.weight(1f).height(18.dp)
                    )

                    Text(
                        formatTime(player.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyColors.TextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Right: Volume Controls (200.dp)
            Row(
                Modifier.width(200.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val vol = player.volume
                val volIcon = when {
                    vol == 0f -> "🔇"
                    vol < 0.5f -> "🔉"
                    else -> "🔊"
                }

                Text(
                    volIcon,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { player.toggleMute() }
                        .padding(4.dp)
                )

                Slider(
                    value = vol,
                    onValueChange = { player.updateVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyColors.TextPrimary,
                        activeTrackColor = SpotifyColors.Green,
                        inactiveTrackColor = SpotifyColors.SurfaceElevated,
                    ),
                    modifier = Modifier.width(100.dp).height(18.dp)
                )

                Spacer(Modifier.width(6.dp))
                Text(
                    "${(vol * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SpotifyColors.TextMuted,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист", color = SpotifyColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название плейлиста") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.Green)
            ) {
                Text("Создать", color = SpotifyColors.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = SpotifyColors.TextSecondary)
            }
        },
        containerColor = SpotifyColors.SurfaceElevated
    )
}

@Composable
private fun DeleteTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить трек \"${track.title}\"?") },
        text = {
            Text(
                if (track.source != null) {
                    "Трек будет удален из библиотеки и всех плейлистов, а файл будет удален с диска."
                } else {
                    "Трек будет удален из библиотеки и всех плейлистов."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.Red)
            ) { Text("Удалить", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = SpotifyColors.SurfaceElevated
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
