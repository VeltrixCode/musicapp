package com.example.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class GenreCategory(val name: String, val color: Color)

private val GENRES = listOf(
    GenreCategory("Рок", Color(0xFFE91429)),
    GenreCategory("Поп", Color(0xFF8D67AB)),
    GenreCategory("Хип-хоп", Color(0xFFBA5D07)),
    GenreCategory("Электроника", Color(0xFF1E3264)),
    GenreCategory("Джаз", Color(0xFF477D95)),
    GenreCategory("Лоу-фай", Color(0xFFE1118C)),
    GenreCategory("Инди", Color(0xFF509BF5)),
    GenreCategory("Классика", Color(0xFF7D4B32)),
)

@Composable
fun SearchScreen(
    library: Library,
    player: AudioPlayer,
    onTrackClick: (Track) -> Unit,
    isCompact: Boolean = false,
) {
    var query by remember { mutableStateOf("") }
    val searchHistory = remember { mutableStateListOf("Synthwave", "Chill", "Acoustic") }
    val scope = rememberCoroutineScope()

    var isSearching by remember { mutableStateOf(false) }
    var cloudResults by remember { mutableStateOf<Map<TrackSource, List<RemoteTrack>>>(emptyMap()) }

    // Live search with debounce
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            cloudResults = emptyMap()
            isSearching = false
            return@LaunchedEffect
        }
        delay(350)
        isSearching = true
        cloudResults = player.providerRegistry.searchAll(q)
        isSearching = false
    }

    val localMatches = remember(query, library.tracks.size) {
        val q = query.trim().lowercase()
        if (q.isBlank()) emptyList() else library.tracks.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Что хочешь послушать?", color = AppColors.TextMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.TextSecondary)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Очистить", tint = AppColors.TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.SurfaceElevated,
                    unfocusedContainerColor = AppColors.Surface,
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(Modifier.height(16.dp))
        }

        // When no search query: Show Recent History & Colorful Genre Grid
        if (query.isBlank()) {
            if (searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Недавние запросы",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                        )
                        IconButton(onClick = { searchHistory.clear() }) {
                            Text("Очистить", color = AppColors.TextMuted, fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchHistory.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppColors.SurfaceElevated)
                                    .clickable { query = h }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.History, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(h, color = AppColors.TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                Text(
                    "Все жанры и категории",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                )
                Spacer(Modifier.height(16.dp))
            }

            // Genre cards grid (2 or 4 columns)
            val columns = if (isCompact) 2 else 4
            for (row in 0 until (GENRES.size / columns)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0 until columns) {
                            val genre = GENRES[row * columns + col]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(genre.color)
                                    .clickable {
                                        query = genre.name
                                        if (!searchHistory.contains(genre.name)) {
                                            searchHistory.add(0, genre.name)
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Live Search Results
            if (isSearching) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // Section 1: Local Library Tracks
            if (localMatches.isNotEmpty()) {
                item {
                    Text(
                        "Треки в медиатеке (${localMatches.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        ),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                items(localMatches, key = { it.id }) { track ->
                    SearchResultRow(
                        track = track,
                        badge = "Локально",
                        badgeColor = AppColors.Accent,
                        onClick = { onTrackClick(track) }
                    )
                }
            }

            // Section 2: Cloud Provider Results
            cloudResults.forEach { (source, remoteList) ->
                if (remoteList.isNotEmpty()) {
                    item {
                        Text(
                            text = "${source.name} (${remoteList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            ),
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(remoteList, key = { it.id }) { remote ->
                        val track = remote.toTrack()
                        SearchResultRow(
                            track = track,
                            badge = source.name,
                            badgeColor = if (source == TrackSource.JAMENDO) Color(0xFFBA5D07) else Color(0xFF1E3264),
                            license = remote.license,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }

            if (!isSearching && localMatches.isEmpty() && cloudResults.values.all { it.isEmpty() }) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("По запросу «$query» ничего не найдено", color = AppColors.TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    track: Track,
    badge: String,
    badgeColor: Color,
    license: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AppColors.TextPrimary), maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(track.artist, style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary), maxLines = 1)
                    if (license != null) {
                        Spacer(Modifier.width(6.dp))
                        Text("• $license", style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted, fontSize = 10.sp), maxLines = 1)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
