package com.example.musicapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.library.FAVORITES_PLAYLIST_ID
import com.example.musicapp.library.Library
import com.example.musicapp.library.SortColumn
import com.example.musicapp.library.SortDirection
import com.example.musicapp.model.Track
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.platform.loadArtwork
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.ui.components.FavoritesCoverBox
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.util.formatTrackDuration
import com.example.musicapp.util.pluralRu

@Composable
internal fun HeroBanner(
    title: String,
    subtitle: String,
    badge: String,
    coverSource: String?,
    canPlay: Boolean,
    isPlaying: Boolean,
    isFavorites: Boolean = false,
    onPlayAll: () -> Unit,
    onDeletePlaylist: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPlayHovered by interactionSource.collectIsHoveredAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPlayHovered && canPlay) 1.05f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing)
    )

    // Load artwork and compute dominant color
    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = coverSource) {
        value = if (coverSource != null) loadArtwork(coverSource) else null
    }

    val dominantColor = remember(coverSource, coverBitmap, isFavorites) {
        if (isFavorites) {
            Color(0xFF0F5A3E)
        } else if (coverSource != null && coverBitmap != null) {
            ArtworkCache.getDominantColor(coverSource) ?: ArtworkCache.computeAndStoreDominantColor(coverSource, coverBitmap!!)
        } else {
            Color(0xFF282828)
        }
    }

    val bgGradient = remember(dominantColor) {
        Brush.verticalGradient(
            listOf(dominantColor.copy(alpha = 0.85f), AppColors.Background)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgGradient)
            .padding(spacing.md),
        verticalAlignment = Alignment.Bottom
    ) {
        // Large 180dp Cover Art with soft shadow
        Box(
            modifier = Modifier
                .size(180.dp)
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .let {
                    if (isFavorites) it.background(Color.Transparent)
                    else it.background(fallbackGradientFor(title))
                },
            contentAlignment = Alignment.Center
        ) {
            if (isFavorites) {
                FavoritesCoverBox(
                    modifier = Modifier.fillMaxSize(),
                    iconSize = 72.dp,
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Crossfade(targetState = coverBitmap) { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Обложка $title",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(spacing.lg))

        Column(Modifier.weight(1f)) {
            Text(badge, style = MaterialTheme.typography.labelSmall)
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextSecondary)
            )
            Spacer(Modifier.height(spacing.md))

            // Big 56dp Play button + More menu (if playlist actions provided)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(scaleAnim)
                        .clip(CircleShape)
                        .background(
                            if (canPlay) {
                                if (isPlayHovered) AppColors.AccentHover else AppColors.Accent
                            } else {
                                AppColors.SurfaceElevated
                            }
                        )
                        .hoverable(interactionSource)
                        .clickable(enabled = canPlay) { onPlayAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Слушать",
                        tint = if (canPlay) Color.Black else AppColors.TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (onDeletePlaylist != null) {
                    Spacer(Modifier.width(spacing.sm))
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Действия с плейлистом",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(AppColors.SurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = AppColors.Error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        "Удалить плейлист",
                                        color = AppColors.Error,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onDeletePlaylist()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StickyCompactHeader(
    title: String,
    dominantColor: Color,
    canPlay: Boolean,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it },
    ) {
        Surface(
            color = dominantColor.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (canPlay) AppColors.Accent else AppColors.SurfaceElevated)
                        .clickable(enabled = canPlay) { onPlayAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Слушать",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistDetailView(
    library: Library,
    player: AudioPlayer,
    playlistId: String?,
    isCompact: Boolean,
    onBack: () -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
    onRemoveFromPlaylist: (String, Track) -> Unit,
    onRename: (Track) -> Unit,
) {
    val spacing = LocalSpacing.current
    val playlist = library.playlists.find { it.id == playlistId }
    if (playlist == null) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Плейлист не найден", color = AppColors.TextSecondary)
            Spacer(Modifier.height(spacing.sm))
            Button(onClick = onBack) { Text("Вернуться") }
        }
        return
    }

    val tracks = library.tracksIn(playlist)
    var pendingDeletePlaylist by remember { mutableStateOf(false) }

    val isFav = playlist.id == FAVORITES_PLAYLIST_ID
    val dominantColor = remember(tracks, isFav) {
        if (isFav) Color(0xFF0F5A3E)
        else tracks.firstOrNull { it.source != null }?.source?.let { ArtworkCache.getDominantColor(it) } ?: Color(0xFF1E3264)
    }

    val listState = rememberLazyListState()
    val showStickyHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val showExtendedColumns = maxWidth > 1100.dp

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    val trackCount = tracks.size
                    HeroBanner(
                        title = playlist.name,
                        subtitle = if (playlist.description.isNotBlank()) "${playlist.description} • ${pluralRu(trackCount, "трек", "трека", "треков")}" else pluralRu(trackCount, "трек", "трека", "треков"),
                        badge = if (playlist.isSystem) "АВТОПЛЕЙЛИСТ" else "ПЛЕЙЛИСТ",
                        coverSource = tracks.firstOrNull { it.source != null }?.source,
                        canPlay = tracks.any { it.source != null },
                        isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
                        isFavorites = isFav,
                        onPlayAll = {
                            tracks.firstOrNull { it.source != null }?.let { player.play(it, tracks) }
                        },
                        onDeletePlaylist = if (!playlist.isSystem) { { pendingDeletePlaylist = true } } else null
                    )
                    Spacer(Modifier.height(spacing.sm))
                }

                stickyHeader {
                    Box(modifier = Modifier.fillMaxWidth().background(AppColors.Background)) {
                        TableHeader(
                            sortColumn = SortColumn.Number,
                            sortDirection = SortDirection.Ascending,
                            onSort = {},
                            isCompact = isCompact,
                            showExtendedColumns = showExtendedColumns
                        )
                    }
                }

                if (tracks.isEmpty()) {
                    val isSyncing = player.providerRegistry.isSyncing.value
                    if (isSyncing) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            TableSkeleton(rows = 5)
                        }
                    } else {
                        item {
                            Box(
                                Modifier.fillMaxWidth().height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = AppColors.TextMuted,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        "В этом плейлисте пока нет треков",
                                        style = MaterialTheme.typography.titleMedium.copy(color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Найдите треки в медиатеке и добавьте их в плейлист через контекстное меню трека (три точки)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = onBack,
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                                        shape = CircleShape
                                    ) {
                                        Text("Перейти в медиатеку", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                        TrackRowItem(
                            index = index + 1,
                            track = track,
                            isPlaying = player.current?.id == track.id && player.isPlaying,
                            isCurrent = player.current?.id == track.id,
                            searchQuery = "",
                            isCompact = isCompact,
                            showExtendedColumns = showExtendedColumns,
                            playlists = library.playlists,
                            durationText = formatTrackDuration(track, player),
                            onPlay = { player.play(track, tracks) },
                            onPlayNext = { player.playNext(track) },
                            onAddToQueue = { player.addToQueue(track) },
                            onAddToPlaylist = { pId -> onAddToPlaylist(pId, track) },
                            onToggleFavorite = { library.toggleFavorite(track) },
                            onRename = { onRename(track) },
                            onRemoveFromPlaylist = { onRemoveFromPlaylist(playlist.id, track) }
                        )
                    }
                }
            }

            StickyCompactHeader(
                title = playlist.name,
                dominantColor = dominantColor,
                canPlay = tracks.any { it.source != null },
                isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
                onPlayAll = {
                    tracks.firstOrNull { it.source != null }?.let { player.play(it, tracks) }
                },
                visible = showStickyHeader
            )
        }
    }

    if (pendingDeletePlaylist) {
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylist = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Удалить плейлист «${playlist.name}»?", fontWeight = FontWeight.Bold) },
            text = { Text("Плейлист будет удален. Сами треки останутся в вашей медиатеке.") },
            confirmButton = {
                Button(
                    onClick = {
                        library.deletePlaylist(playlist.id)
                        pendingDeletePlaylist = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                ) { Text("Удалить", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlaylist = false }) { Text("Отмена") }
            },
            containerColor = AppColors.SurfaceElevated
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SmartMixDetailView(
    title: String,
    library: Library,
    player: AudioPlayer,
    isCompact: Boolean,
    onBack: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val tracks = remember(title, library.visibleTracks.size) {
        when (title) {
            "Микс дня" -> library.getMixOfTheDay()
            "Недавно добавленное", "Свежее в медиатеке" -> library.getRecentlyAdded(50)
            "Часто слушаемое" -> library.getFrequentlyPlayed(50)
            "Новое в облаке" -> library.getCloudTracks()
            else -> library.visibleTracks.filter { it.album.equals(title, ignoreCase = true) || it.artist.equals(title, ignoreCase = true) }.ifEmpty { library.visibleTracks.toList() }
        }
    }

    val dominantColor = remember(tracks) {
        tracks.firstOrNull { it.source != null }?.source?.let { ArtworkCache.getDominantColor(it) } ?: Color(0xFF1E3264)
    }

    val listState = rememberLazyListState()
    val showStickyHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val showExtendedColumns = maxWidth > 1100.dp

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onBack) {
                            Text("← Назад к главной", color = AppColors.TextSecondary, fontSize = 13.sp)
                        }
                    }

                    val trackCount = tracks.size
                    HeroBanner(
                        title = title,
                        subtitle = pluralRu(trackCount, "трек", "трека", "треков"),
                        badge = "УМНЫЙ МИКС",
                        coverSource = tracks.firstOrNull { it.source != null }?.source,
                        canPlay = tracks.isNotEmpty(),
                        isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
                        onPlayAll = {
                            if (tracks.isNotEmpty()) player.play(tracks.first(), tracks)
                        }
                    )

                    Spacer(Modifier.height(spacing.md))
                }

                stickyHeader {
                    Box(modifier = Modifier.fillMaxWidth().background(AppColors.Background)) {
                        TableHeader(
                            sortColumn = SortColumn.Number,
                            sortDirection = SortDirection.Ascending,
                            onSort = {},
                            isCompact = isCompact,
                            showExtendedColumns = showExtendedColumns
                        )
                    }
                }

                itemsIndexed(tracks, key = { _, t -> t.id }) { idx, track ->
                    TrackRowItem(
                        index = idx + 1,
                        track = track,
                        isPlaying = player.current?.id == track.id && player.isPlaying,
                        isCurrent = player.current?.id == track.id,
                        searchQuery = "",
                        isCompact = isCompact,
                        showExtendedColumns = showExtendedColumns,
                        playlists = library.playlists,
                        durationText = formatTrackDuration(track, player),
                        onPlay = { player.play(track, tracks) },
                        onPlayNext = { player.playNext(track) },
                        onAddToQueue = { player.addToQueue(track) },
                        onAddToPlaylist = { pId -> library.addToPlaylist(pId, track.id) },
                        onToggleFavorite = { library.toggleFavorite(track) },
                        onRename = {},
                        onDelete = null
                    )
                }
            }

            StickyCompactHeader(
                title = title,
                dominantColor = dominantColor,
                canPlay = tracks.isNotEmpty(),
                isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
                onPlayAll = {
                    if (tracks.isNotEmpty()) player.play(tracks.first(), tracks)
                },
                visible = showStickyHeader
            )
        }
    }
}

@Composable
internal fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, coverUrl: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Новый плейлист", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название плейлиста") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("Ссылка или путь к обложке (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), description.trim(), coverUrl.trim().takeIf { it.isNotBlank() })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text("Создать", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = AppColors.TextSecondary) }
        },
        containerColor = AppColors.SurfaceElevated
    )
}
