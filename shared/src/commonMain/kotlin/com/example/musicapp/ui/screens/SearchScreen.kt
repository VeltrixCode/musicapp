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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.player.PlaybackContext
import com.example.musicapp.player.PlaybackContextType
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.ui.AppScreen
import com.example.musicapp.util.formatTrackDuration
import com.example.musicapp.util.pluralRu
import kotlinx.coroutines.delay

private val GENRE_PALETTE = listOf(
    Color(0xFFE91429), Color(0xFF8D67AB), Color(0xFFBA5D07),
    Color(0xFF1E3264), Color(0xFF477D95), Color(0xFFE1118C),
    Color(0xFF509BF5), Color(0xFF7D4B32), Color(0xFF2E77D0),
    Color(0xFF0D73EC), Color(0xFF503750), Color(0xFFDC148C),
    Color(0xFF1DB954), Color(0xFFE66465), Color(0xFF9146FF)
)

@Composable
fun SearchScreen(
    library: Library,
    player: AudioPlayer,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onUploadSong: (() -> Unit)? = null,
    onUploadFolder: (() -> Unit)? = null,
    onNavigate: ((AppScreen) -> Unit)? = null,
    isCompact: Boolean = false,
) {
    val searchHistory = remember { mutableStateListOf<String>() }

    var isSearching by remember { mutableStateOf(false) }
    var cloudResults by remember { mutableStateOf<Map<TrackSource, List<RemoteTrack>>>(emptyMap()) }

    // Live search debouncing
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank() || !com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
            cloudResults = emptyMap()
            isSearching = false
            if (q.isNotBlank() && !searchHistory.contains(q)) {
                searchHistory.add(0, q)
                if (searchHistory.size > 10) searchHistory.removeAt(searchHistory.lastIndex)
            }
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        cloudResults = player.providerRegistry.searchAll(q)
        isSearching = false

        if (!searchHistory.contains(q)) {
            searchHistory.add(0, q)
            if (searchHistory.size > 10) searchHistory.removeAt(searchHistory.lastIndex)
        }
    }

    // Genre tiles from local tags only
    val localGenres = remember(library.visibleTracks.size) {
        library.visibleTracks
            .mapNotNull { it.genre?.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    // Local library search partitioned into Tracks, Albums, Artists
    val q = searchQuery.trim().lowercase()
    val matchingTracks = remember(searchQuery, library.visibleTracks.size) {
        if (q.isBlank()) emptyList()
        else library.visibleTracks.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            (it.genre?.lowercase()?.contains(q) == true)
        }
    }

    val matchingAlbums = remember(searchQuery, library.visibleTracks.size) {
        if (q.isBlank()) emptyList()
        else library.visibleTracks
            .filter { it.album.isNotBlank() && it.album != "Неизвестный альбом" && it.album.lowercase().contains(q) }
            .groupBy { it.album }
            .map { (albName, trList) ->
                Triple(albName, trList.first().artist, trList.size)
            }
            .sortedBy { it.first.lowercase() }
    }

    val matchingArtists = remember(searchQuery, library.visibleTracks.size) {
        if (q.isBlank()) emptyList()
        else library.visibleTracks
            .filter { it.artist.isNotBlank() && it.artist != "Неизвестный исполнитель" && it.artist.lowercase().contains(q) }
            .groupBy { it.artist }
            .map { (artName, trList) ->
                Pair(artName, trList.size)
            }
            .sortedBy { it.first.lowercase() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp)
    ) {
        item {
            Text(
                "Поиск",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }

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

            if (localGenres.isNotEmpty()) {
                item {
                    Text(
                        "Жанры медиатеки",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                }

                val columns = if (isCompact) 2 else 4
                val rowCount = (localGenres.size + columns - 1) / columns
                for (rowIndex in 0 until rowCount) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            for (colIndex in 0 until columns) {
                                val itemIndex = rowIndex * columns + colIndex
                                if (itemIndex < localGenres.size) {
                                    val (genreName, count) = localGenres[itemIndex]
                                    val color = GENRE_PALETTE[itemIndex % GENRE_PALETTE.size]
                                    LocalGenreCard(
                                        name = genreName,
                                        trackCount = count,
                                        color = color,
                                        onClick = { onSearchQueryChange(genreName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else if (library.visibleTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Жанры ещё не добавлены в медиатеку",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Добавьте треки или импортируйте папку с файлами, содержащими теги жанров",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            if (isCompact) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (onUploadSong != null) {
                                        Button(
                                            onClick = onUploadSong,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.Accent,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                        ) {
                                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Добавить файлы", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    if (onUploadFolder != null) {
                                        Button(
                                            onClick = onUploadFolder,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.SurfaceElevated,
                                                contentColor = AppColors.TextPrimary
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                        ) {
                                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Импорт папки", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (onUploadSong != null) {
                                        Button(
                                            onClick = onUploadSong,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.Accent,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Добавить файлы", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                    if (onUploadFolder != null) {
                                        Button(
                                            onClick = onUploadFolder,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppColors.SurfaceElevated,
                                                contentColor = AppColors.TextPrimary
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Импорт папки", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Live Search Results sectioned: Tracks, Albums, Artists
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

            // Section 1: Tracks
            if (matchingTracks.isNotEmpty()) {
                item {
                    Text(
                        "Треки (${matchingTracks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }
                items(matchingTracks, key = { it.id }) { track ->
                    SearchResultRow(
                        track = track,
                        durationText = formatTrackDuration(track, player),
                        badge = if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) "Локально" else null,
                        badgeColor = AppColors.Accent,
                        onClick = {
                            player.play(track, matchingTracks, PlaybackContext(PlaybackContextType.Search, searchQuery, "Поиск: $searchQuery"))
                        }
                    )
                }
            }

            // Section 2: Albums
            if (matchingAlbums.isNotEmpty()) {
                item {
                    Text(
                        "Альбомы (${matchingAlbums.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        ),
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )
                }
                items(matchingAlbums, key = { it.first }) { (albumName, artistName, trackCount) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (onNavigate != null) {
                                    onNavigate(AppScreen.SmartMixView(albumName))
                                } else {
                                    onSearchQueryChange(albumName)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(fallbackGradientFor(albumName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Album, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    albumName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "$artistName • ${pluralRu(trackCount, "трек", "трека", "треков")}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Artists
            if (matchingArtists.isNotEmpty()) {
                item {
                    Text(
                        "Исполнители (${matchingArtists.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        ),
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                    )
                }
                items(matchingArtists, key = { it.first }) { (artistName, trackCount) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (onNavigate != null) {
                                    onNavigate(AppScreen.SmartMixView(artistName))
                                } else {
                                    onSearchQueryChange(artistName)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    artistName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    pluralRu(trackCount, "трек", "трека", "треков"),
                                    style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Cloud Provider Results (if enabled)
            if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
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
            }

            if (!isSearching && matchingTracks.isEmpty() && matchingAlbums.isEmpty() && matchingArtists.isEmpty() && cloudResults.values.all { it.isEmpty() }) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "По запросу «$searchQuery» ничего не найдено",
                                style = MaterialTheme.typography.titleMedium.copy(color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Проверьте написание запроса или попробуйте найти исполнителя или трек по-другому",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalGenreCard(
    name: String,
    trackCount: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = pluralRu(trackCount, "трек", "трека", "треков"),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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
    badge: String? = null,
    badgeColor: Color = AppColors.Accent,
    license: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

            if (badge != null) {
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
}
