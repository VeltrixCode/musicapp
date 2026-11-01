package com.example.musicapp.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.model.Playlist
import com.example.musicapp.library.SortColumn
import com.example.musicapp.library.SortDirection
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.service.CoverArtService
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.util.formatTime

@Composable
internal fun TableHeader(
    sortColumn: SortColumn,
    sortDirection: SortDirection,
    onSort: (SortColumn) -> Unit,
    isCompact: Boolean,
    showExtendedColumns: Boolean = false,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number: 40dp (hidden on compact screens)
        if (!isCompact) {
            Box(
                modifier = Modifier.width(40.dp).clickable { onSort(SortColumn.Number) },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sortColumn == SortColumn.Number) AppColors.Accent else AppColors.TextMuted
                        )
                    )
                    if (sortColumn == SortColumn.Number) {
                        Icon(
                            imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Title: weight
        Box(
            modifier = Modifier.weight(if (showExtendedColumns) 1.2f else 1f).clickable { onSort(SortColumn.Title) },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "НАЗВАНИЕ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (sortColumn == SortColumn.Title) AppColors.Accent else AppColors.TextMuted
                    )
                )
                if (sortColumn == SortColumn.Title) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Extended Columns when screen > 1100dp: Album & Date Added
        if (showExtendedColumns) {
            Box(
                modifier = Modifier.weight(0.8f).clickable { onSort(SortColumn.Album) },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "АЛЬБОМ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sortColumn == SortColumn.Album) AppColors.Accent else AppColors.TextMuted
                        )
                    )
                    if (sortColumn == SortColumn.Album) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.width(120.dp).clickable { onSort(SortColumn.AddedAt) },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ДОБАВЛЕНО",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sortColumn == SortColumn.AddedAt) AppColors.Accent else AppColors.TextMuted
                        )
                    )
                    if (sortColumn == SortColumn.AddedAt) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Duration column
        if (!isCompact) {
            Box(
                modifier = Modifier.width(92.dp).clickable { onSort(SortColumn.Duration) },
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ДЛИТ.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sortColumn == SortColumn.Duration) AppColors.Accent else AppColors.TextMuted
                        )
                    )
                    if (sortColumn == SortColumn.Duration) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Spacer for more actions column
        Spacer(Modifier.width(40.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackRowItem(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    searchQuery: String,
    isCompact: Boolean,
    showExtendedColumns: Boolean = false,
    playlists: List<Playlist>,
    durationText: String = "",
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    var menuOpen by remember { mutableStateOf(false) }
    var contextMenuOpen by remember { mutableStateOf(false) }

    val rowInteraction = remember { MutableInteractionSource() }
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val isRowFocused by rowInteraction.collectIsFocusedAsState()

    // Artwork with multi-tier cache & online cover lookup
    val artworkBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = track.id) {
        value = CoverArtService.loadArtworkForTrack(track)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isCompact) 56.dp else 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isCurrent -> AppColors.SurfaceElevated
                    isRowHovered -> AppColors.CardHover
                    else -> Color.Transparent
                }
            )
            .focusable(interactionSource = rowInteraction)
            .border(
                width = if (isRowFocused) 1.5.dp else 0.dp,
                color = if (isRowFocused) AppColors.Accent else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .hoverable(rowInteraction)
            .combinedClickable(
                onClick = { onPlay() },
                onDoubleClick = { onPlay() }
            )
            .padding(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track number / Animated Equalizer / Play Icon: 40dp (hidden on compact)
            if (!isCompact) {
                Box(
                    modifier = Modifier.width(40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when {
                        isPlaying -> AnimatedEqualizer()
                        isCurrent -> FrozenEqualizer()
                        isRowHovered -> {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Воспроизвести",
                                tint = AppColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            Text(
                                index.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppColors.TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Thumbnail Cover Art (40dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fallbackGradientFor(track.title)),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = artworkBitmap) { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(spacing.sm))

            // Title and Artist: weight
            Column(Modifier.weight(if (showExtendedColumns) 1.2f else 1f)) {
                Text(
                    text = highlightQuery(track.title, searchQuery, isCurrent),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled && (track.isOfflineAvailable || track.cachedFilePath != null)) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Офлайн",
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled && track.isCloud) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppColors.SurfaceElevated)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                track.sourceProvider.displayName,
                                color = AppColors.TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    if (track.isLossless || track.format?.lowercase() in listOf("flac", "wav", "alac")) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppColors.Accent.copy(alpha = 0.15f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "LOSSLESS",
                                color = AppColors.Accent,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    } else if (track.bitrateKbps != null && track.bitrateKbps > 0) {
                        Text(
                            "${track.bitrateKbps}k",
                            color = AppColors.TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    Text(
                        text = highlightQuery(track.artist, searchQuery, isCurrent = false),
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Extended Columns when screen > 1100dp: Album & Date Added
            if (showExtendedColumns) {
                Text(
                    text = if (track.album.isNotBlank() && track.album != "Неизвестный альбом") track.album else "—",
                    style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.8f).padding(end = 8.dp)
                )
                Text(
                    text = com.example.musicapp.util.formatAddedDate(track.addedAt),
                    style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(120.dp)
                )
            }

            // Duration: 92dp (with Favorite Heart directly adjacent to duration on hover)
            if (!isCompact) {
                Row(
                    modifier = Modifier.width(92.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isRowHovered || isRowFocused || track.isFavorite) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "В избранное",
                                tint = if (track.isFavorite) AppColors.Accent else AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(28.dp))
                    }

                    Text(
                        text = if (durationText.isNotBlank()) durationText else (if (track.durationMs > 0) formatTime(track.durationMs) else "--:--"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier.width(60.dp),
                        maxLines = 1
                    )
                }
            }

            // Favorite button directly accessible on compact screens
            if (isCompact) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (track.isFavorite) AppColors.Accent else AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Actions: 40dp (desktop) / 44dp (compact) More Options Button
            Box(
                modifier = Modifier.width(if (isCompact) 44.dp else 40.dp),
                contentAlignment = Alignment.Center
            ) {
                val showActions = isRowHovered || isRowFocused || isCompact || contextMenuOpen

                if (showActions) {
                    IconButton(
                        onClick = { contextMenuOpen = true },
                        modifier = Modifier.size(if (isCompact) 44.dp else 28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Меню трека",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(if (isCompact) 20.dp else 18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = contextMenuOpen,
                        onDismissRequest = { contextMenuOpen = false },
                        modifier = Modifier.background(AppColors.SurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Воспроизвести") },
                            onClick = {
                                contextMenuOpen = false
                                onPlay()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Играть следующим") },
                            onClick = {
                                contextMenuOpen = false
                                onPlayNext()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Добавить в очередь") },
                            onClick = {
                                contextMenuOpen = false
                                onAddToQueue()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (track.isFavorite) "Убрать из любимых" else "В любимые") },
                            onClick = {
                                contextMenuOpen = false
                                onToggleFavorite()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Добавить в плейлист…") },
                            onClick = {
                                contextMenuOpen = false
                                menuOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Переименовать…") },
                            onClick = {
                                contextMenuOpen = false
                                onRename()
                            }
                        )
                        if (track.license != null) {
                            DropdownMenuItem(
                                text = { Text("Лицензия: ${track.license}", style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted)) },
                                onClick = {},
                                enabled = false
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Удалить", color = AppColors.Error) },
                                onClick = {
                                contextMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }

                // Add to Playlist Dropdown
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(AppColors.SurfaceElevated)
                ) {
                    if (playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Нет плейлистов", color = AppColors.TextMuted) },
                            onClick = { menuOpen = false }
                        )
                    } else {
                        playlists.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, color = AppColors.TextPrimary) },
                                onClick = {
                                    onAddToPlaylist(p.id)
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.size(28.dp))
            }
        }
    }
}
}

@Composable
fun TableSkeleton(rows: Int = 6, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        )
    )
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = alpha)))
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = alpha)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = alpha)))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = alpha)))
                }
                Box(Modifier.width(36.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = alpha)))
            }
        }
    }
}

internal fun highlightQuery(text: String, query: String, isCurrent: Boolean): AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString {
            append(text)
            addStyle(
                style = SpanStyle(color = if (isCurrent) AppColors.Accent else AppColors.TextPrimary),
                start = 0,
                end = text.length
            )
        }
    }

    val startIndex = text.indexOf(query, ignoreCase = true)
    if (startIndex < 0) {
        return buildAnnotatedString {
            append(text)
            addStyle(
                style = SpanStyle(color = if (isCurrent) AppColors.Accent else AppColors.TextPrimary),
                start = 0,
                end = text.length
            )
        }
    }

    return buildAnnotatedString {
        append(text)
        addStyle(
            style = SpanStyle(color = if (isCurrent) AppColors.Accent else AppColors.TextPrimary),
            start = 0,
            end = text.length
        )
        addStyle(
            style = SpanStyle(
                color = AppColors.Accent,
                fontWeight = FontWeight.ExtraBold,
                background = AppColors.SurfaceElevated
            ),
            start = startIndex,
            end = startIndex + query.length
        )
    }
}
