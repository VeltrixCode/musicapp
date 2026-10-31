package com.example.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.theme.AppColors
import com.example.musicapp.util.formatTrackDuration
import kotlinx.coroutines.delay

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
    GenreCategory("Метал", Color(0xFF2E77D0)),
    GenreCategory("Акустика", Color(0xFF0D73EC)),
    GenreCategory("Эмбиент", Color(0xFF503750)),
    GenreCategory("R&B", Color(0xFFDC148C)),
)

@Composable
fun SearchScreen(
    library: Library,
    player: AudioPlayer,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    isCompact: Boolean = false,
) {
    // Dynamic history without fake stubs
    val searchHistory = remember { mutableStateListOf<String>() }

    var isSearching by remember { mutableStateOf(false) }
    var cloudResults by remember { mutableStateOf<Map<TrackSource, List<RemoteTrack>>>(emptyMap()) }

    // Live search with debounce from the top bar query
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            cloudResults = emptyMap()
            isSearching = false
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        cloudResults = player.providerRegistry.searchAll(q)
        isSearching = false

        // Automatically record successful query in recent history
        if (!searchHistory.contains(q)) {
            searchHistory.add(0, q)
            if (searchHistory.size > 10) searchHistory.removeAt(searchHistory.lastIndex)
        }
    }

    val localMatches = remember(searchQuery, library.tracks.size) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) emptyList() else library.tracks.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp)
    ) {
        // When no search query: Show Recent History & Spotify-style Genre Grid
        if (searchQuery.isBlank()) {
            if (searchHistory.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                        TextButton(
                            onClick = { searchHistory.clear() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Очистить",
                                color = AppColors.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        searchHistory.take(5).forEach { h ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppColors.SurfaceElevated)
                                    .clickable { onSearchQueryChange(h) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = AppColors.TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        h,
                                        color = AppColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Все жанры и категории",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }

            // Genre cards grid: 2 columns on mobile/compact, 4 columns on desktop
            val columns = if (isCompact) 2 else 4
            val rowCount = (GENRES.size + columns - 1) / columns
            for (rowIndex in 0 until rowCount) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (colIndex in 0 until columns) {
                            val itemIndex = rowIndex * columns + colIndex
                            if (itemIndex < GENRES.size) {
                                val genre = GENRES[itemIndex]
                                SpotifyGenreCard(
                                    category = genre,
                                    onClick = { onSearchQueryChange(genre.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
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
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }
                items(localMatches, key = { it.id }) { track ->
                    SearchResultRow(
                        track = track,
                        durationText = formatTrackDuration(track, player),
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
                            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                        )
                    }
                    items(remoteList, key = { it.id }) { remote ->
                        val track = remote.toTrack()
                        SearchResultRow(
                            track = track,
                            durationText = formatTrackDuration(track, player),
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
                        Text("По запросу «$searchQuery» ничего не найдено", color = AppColors.TextMuted)
                    }
                }
            }
        }
    }
}

/**
 * Spotify-fidelity Genre Tile:
 * Vibrant color, bold top-left text, 25-degree tilted art card in bottom-right corner.
 */
@Composable
private fun SpotifyGenreCard(
    category: GenreCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(category.color)
            .clickable { onClick() }
    ) {
        // Category Name (Top Left)
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 17.sp
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            maxLines = 2,
            softWrap = true
        )

        // 25-degree tilted artwork thumbnail in bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 6.dp)
                .size(66.dp)
                .graphicsLayer {
                    rotationZ = 25f
                    shadowElevation = 8.dp.toPx()
                }
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    track: Track,
    durationText: String,
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
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = AppColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    ),
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                        maxLines = 1
                    )
                    if (license != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "• $license",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppColors.TextMuted,
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                durationText,
                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

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
