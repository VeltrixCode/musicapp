package com.example.musicapp.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.FAVORITES_PLAYLIST_ID
import com.example.musicapp.library.Library
import com.example.musicapp.model.LrcLine
import com.example.musicapp.model.Track
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.service.CoverArtService
import com.example.musicapp.service.LyricsService
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.util.formatTrackDuration

enum class RightDrawerTab(val title: String) {
    NOW_PLAYING("Сейчас"),
    QUEUE("Очередь"),
    LYRICS("Текст")
}

@Composable
fun RightNowPlayingDrawer(
    player: AudioPlayer,
    library: Library,
    initialTab: RightDrawerTab = RightDrawerTab.NOW_PLAYING,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val current = player.current

    Column(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(AppColors.Surface)
            .padding(14.dp)
    ) {
        // Header with Tab Switcher & Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = AppColors.TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = AppColors.Accent,
                        height = 2.dp
                    )
                },
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                RightDrawerTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == tab) AppColors.TextPrimary else AppColors.TextMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            if (onTogglePin != null) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (isPinned) "Открепить панель" else "Закрепить панель",
                        tint = if (isPinned) AppColors.Accent else AppColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                enabled = !isPinned,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = if (isPinned) "Панель закреплена" else "Закрыть панель",
                    tint = if (isPinned) AppColors.TextMuted.copy(alpha = 0.35f) else AppColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tab Content
        when (selectedTab) {
            RightDrawerTab.NOW_PLAYING -> {
                NowPlayingDetailPane(
                    track = current,
                    player = player,
                    library = library,
                    onOpenLyrics = { selectedTab = RightDrawerTab.LYRICS }
                )
            }
            RightDrawerTab.QUEUE -> {
                QueuePane(player = player, current = current)
            }
            RightDrawerTab.LYRICS -> {
                LyricsPane(track = current, player = player)
            }
        }
    }
}

@Composable
private fun NowPlayingDetailPane(
    track: Track?,
    player: AudioPlayer,
    library: Library,
    onOpenLyrics: () -> Unit,
) {
    if (track == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ничего не играет", color = AppColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val artworkBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = track.id) {
        value = CoverArtService.loadArtworkForTrack(track)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // Large 280dp Cover Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(12.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(fallbackGradientFor(track.title)),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = artworkBitmap) { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title, Artist and Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { library.toggleFavorite(track) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (track.isFavorite) AppColors.Accent else AppColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Metadata Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceElevated)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (track.album.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Альбом", color = AppColors.TextMuted, fontSize = 12.sp)
                            Text(track.album, color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                    if (track.year != null && track.year > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Год", color = AppColors.TextMuted, fontSize = 12.sp)
                            Text(track.year.toString(), color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Длительность", color = AppColors.TextMuted, fontSize = 12.sp)
                        Text(formatTrackDuration(track, player), color = AppColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Источник", color = AppColors.TextMuted, fontSize = 12.sp)
                            Text(
                                if (track.isCloud) track.sourceProvider.name else "Локальный файл",
                                color = AppColors.Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick button to open lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceElevated)
                    .clickable { onOpenLyrics() }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Открыть текст песни", color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun QueuePane(
    player: AudioPlayer,
    current: Track?,
) {
    val queue = player.queue

    Column(Modifier.fillMaxSize()) {
        // Now Playing Section
        Text("СЕЙЧАС ИГРАЕТ", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))

        if (current != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.SurfaceElevated)
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            current.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Accent
                            ),
                            maxLines = 1
                        )
                        Text(current.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Text(
                        formatTrackDuration(current, player),
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Up Next Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ДАЛЕЕ В ОЧЕРЕДИ", style = MaterialTheme.typography.labelSmall)
            if (queue.size > 1) {
                TextButton(
                    onClick = { player.clearQueue() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Очистить",
                        color = AppColors.TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val upNext = remember(queue, current) {
            val cIdx = queue.indexOfFirst { it.id == current?.id }
            if (cIdx >= 0 && cIdx < queue.lastIndex) queue.subList(cIdx + 1, queue.size)
            else emptyList()
        }

        if (upNext.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Очередь воспроизведения пуста", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Включите любой альбом или добавьте треки в очередь", color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(upNext, key = { idx, t -> "${t.id}_$idx" }) { idx, t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { player.play(t, queue) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (idx + 1).toString(),
                            color = AppColors.TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(t.artist, style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted), maxLines = 1)
                        }
                        Text(
                            formatTrackDuration(t, player),
                            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { player.removeFromQueue(queue.indexOf(t)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Убрать",
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPane(
    track: Track?,
    player: AudioPlayer,
) {
    if (track == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ничего не играет", color = AppColors.TextMuted)
        }
        return
    }

    var lyrics by remember(track.id) { mutableStateOf<List<LrcLine>>(emptyList()) }
    var isLoading by remember(track.id) { mutableStateOf(true) }

    LaunchedEffect(track.id) {
        isLoading = true
        lyrics = if (player.lyrics.isNotEmpty()) player.lyrics else LyricsService.getLyrics(track)
        isLoading = false
    }

    val playbackState by player.playbackState.collectAsState()
    val listState = rememberLazyListState()

    // Determine currently active lyric line
    val activeIndex = remember(lyrics, playbackState.positionMs) {
        lyrics.indexOfLast { it.timestampMs <= playbackState.positionMs }
    }

    // Auto-scroll to active lyric line smoothly
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.Accent, modifier = Modifier.size(32.dp))
        }
    } else if (lyrics.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Текст песни не найден", color = AppColors.TextMuted, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            itemsIndexed(lyrics) { idx, line ->
                val isActive = idx == activeIndex
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.45f),
                        fontSize = if (isActive) 18.sp else 15.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { player.seekTo(line.timestampMs) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
