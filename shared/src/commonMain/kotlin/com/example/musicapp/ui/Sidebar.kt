package com.example.musicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.musicapp.library.FAVORITES_PLAYLIST_ID
import com.example.musicapp.library.Library
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.ui.components.FavoritesCoverBox
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.util.pluralRu
import kotlin.math.roundToInt

@Composable
internal fun AppSidebar(
    library: Library,
    currentScreen: AppScreen,
    sidebarWidth: Dp,
    onWidthChange: (Dp) -> Unit,
    isCompact: Boolean,
    onNavigate: (AppScreen) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val effectiveWidth = if (isCompact) 72.dp else sidebarWidth

    Row(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(effectiveWidth)
                .fillMaxHeight()
                .background(Color.Black)
                .padding(vertical = spacing.md, horizontal = if (isCompact) spacing.xs else spacing.sm)
        ) {
            // App Logo & Sidebar Collapse Toggle (no duplicate app title)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md, start = if (isCompact) 0.dp else spacing.xs),
                horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "JVXMusic",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!isCompact) {
                    IconButton(
                        onClick = { onWidthChange(if (sidebarWidth <= 80.dp) 240.dp else 72.dp) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (sidebarWidth <= 80.dp) Icons.AutoMirrored.Rounded.ArrowForward else Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Свернуть/Развернуть",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Nav 1: Home
            SidebarNavItem(
                label = "Главная",
                icon = Icons.Rounded.Home,
                isCompact = isCompact,
                isSelected = currentScreen is AppScreen.Home,
                onClick = { onNavigate(AppScreen.Home) }
            )

            // Nav 2: Search
            SidebarNavItem(
                label = "Поиск",
                icon = Icons.Rounded.Search,
                isCompact = isCompact,
                isSelected = currentScreen is AppScreen.Search,
                onClick = { onNavigate(AppScreen.Search) }
            )

            // Nav 3: Library
            SidebarNavItem(
                label = "Моя медиатека",
                icon = Icons.Rounded.LibraryMusic,
                isCompact = isCompact,
                isSelected = currentScreen is AppScreen.Library,
                onClick = { onNavigate(AppScreen.Library) }
            )

            if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
                // Nav 4: Cloud & Sources
                SidebarNavItem(
                    label = "Облако и Источники",
                    icon = Icons.Rounded.CloudQueue,
                    isCompact = isCompact,
                    isSelected = currentScreen is AppScreen.CloudSources,
                    onClick = { onNavigate(AppScreen.CloudSources) }
                )
            }

            Spacer(Modifier.height(spacing.sm))
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
            Spacer(Modifier.height(spacing.sm))

            var selectedFilter by remember { mutableStateOf("Плейлисты") }
            var sortOrder by remember { mutableStateOf("Пользовательский") }
            var isGrid by remember { mutableStateOf(false) }

            // Library Filter Chips & Controls
            if (!isCompact) {
                val availableFilters = if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
                    listOf("Плейлисты", "Альбомы", "Исполнители", "Скачанные")
                } else {
                    listOf("Плейлисты", "Альбомы", "Исполнители")
                }
                if (selectedFilter !in availableFilters) {
                    selectedFilter = "Плейлисты"
                }

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableFilters.forEach { f ->
                        val isSel = selectedFilter == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) AppColors.Accent else AppColors.SurfaceElevated)
                                .clickable {
                                    selectedFilter = f
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                f,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.Black else AppColors.TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Sort Dropdown & List/Grid Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { sortMenuOpen = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(sortOrder, fontSize = 11.sp, color = AppColors.TextSecondary)
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(12.dp))
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                            modifier = Modifier.background(AppColors.SurfaceElevated)
                        ) {
                            listOf("Пользовательский", "По алфавиту", "Недавние").forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order, fontSize = 12.sp) },
                                    onClick = {
                                        sortOrder = order
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isGrid = !isGrid },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isGrid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = "Вид",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onCreatePlaylist,
                            modifier = Modifier.size(26.dp),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = AppColors.TextSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Создать плейлист",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.size(36.dp).align(Alignment.CenterHorizontally),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppColors.SurfaceElevated,
                        contentColor = AppColors.TextSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Создать плейлист",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(spacing.xxs))

            var draggedPlaylistId by remember { mutableStateOf<String?>(null) }
            var dragOffsetY by remember { mutableStateOf(0f) }

            val displayedPlaylists = remember(library.visiblePlaylists.map { it.id }, sortOrder) {
                when (sortOrder) {
                    "По алфавиту" -> library.visiblePlaylists.sortedBy { it.name.lowercase() }
                    "Недавние" -> library.visiblePlaylists.reversed()
                    else -> library.visiblePlaylists.toList()
                }
            }
            val distinctAlbums = remember(library.visibleTracks.size) {
                library.visibleTracks.map { it.album }.filter { it.isNotBlank() && it != "Неизвестный альбом" }.distinct().sorted()
            }
            val distinctArtists = remember(library.visibleTracks.size) {
                library.visibleTracks.map { it.artist }.filter { it.isNotBlank() && it != "Неизвестный исполнитель" }.distinct().sorted()
            }

            // Playlists / Categories List
            LazyColumn(Modifier.weight(1f)) {
                when (selectedFilter) {
                    "Альбомы" -> {
                        itemsIndexed(distinctAlbums, key = { _, alb -> alb }) { _, alb ->
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemHovered by itemInteraction.collectIsHoveredAsState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .hoverable(itemInteraction)
                                    .background(if (isItemHovered) AppColors.CardHover else Color.Transparent)
                                    .clickable { onNavigate(AppScreen.SmartMixView(alb)) }
                                    .padding(horizontal = spacing.xs, vertical = spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(spacing.xs))
                                Text(alb, style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    "Исполнители" -> {
                        itemsIndexed(distinctArtists, key = { _, art -> art }) { _, art ->
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemHovered by itemInteraction.collectIsHoveredAsState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .hoverable(itemInteraction)
                                    .background(if (isItemHovered) AppColors.CardHover else Color.Transparent)
                                    .clickable { onNavigate(AppScreen.SmartMixView(art)) }
                                    .padding(horizontal = spacing.xs, vertical = spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(spacing.xs))
                                Text(art, style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    "Скачанные" -> {
                        val downloaded = library.getOfflineDownloadedTracks()
                        itemsIndexed(downloaded, key = { _, tr -> tr.id }) { _, tr ->
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemHovered by itemInteraction.collectIsHoveredAsState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .hoverable(itemInteraction)
                                    .background(if (isItemHovered) AppColors.CardHover else Color.Transparent)
                                    .clickable { onNavigate(AppScreen.Library) }
                                    .padding(horizontal = spacing.xs, vertical = spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(spacing.xs))
                                Text(tr.title, style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    else -> {
                        if (isGrid && !isCompact && effectiveWidth > 180.dp) {
                            val chunks = displayedPlaylists.chunked(2)
                            itemsIndexed(chunks, key = { idx, chunk -> "${idx}_${chunk.firstOrNull()?.id}" }) { _, chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunk.forEach { p ->
                                        val isSelected = currentScreen is AppScreen.PlaylistView && (currentScreen as AppScreen.PlaylistView).playlistId == p.id
                                        val isFav = p.id == FAVORITES_PLAYLIST_ID
                                        val itemInteraction = remember { MutableInteractionSource() }
                                        val isItemHovered by itemInteraction.collectIsHoveredAsState()

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .hoverable(itemInteraction)
                                                .background(if (isSelected) AppColors.SurfaceElevated else if (isItemHovered) AppColors.CardHover else Color.Transparent)
                                                .clickable { onNavigate(AppScreen.PlaylistView(p.id)) }
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (isFav) {
                                                FavoritesCoverBox(modifier = Modifier.fillMaxWidth().aspectRatio(1f), iconSize = 24.dp, shape = RoundedCornerShape(6.dp))
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(fallbackGradientFor(p.name)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(p.name, style = MaterialTheme.typography.bodySmall.copy(color = if (isSelected) AppColors.Accent else AppColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (chunk.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(displayedPlaylists, key = { _, p -> p.id }) { _, p ->
                                val isSelected = currentScreen is AppScreen.PlaylistView && (currentScreen as AppScreen.PlaylistView).playlistId == p.id
                                val isFav = p.id == FAVORITES_PLAYLIST_ID
                                val itemInteraction = remember { MutableInteractionSource() }
                                val isItemHovered by itemInteraction.collectIsHoveredAsState()
                                val isDragged = draggedPlaylistId == p.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDragged) 10f else 1f)
                                        .offset {
                                            if (isDragged) IntOffset(0, dragOffsetY.roundToInt())
                                            else IntOffset.Zero
                                        }
                                        .clip(RoundedCornerShape(8.dp))
                                        .hoverable(itemInteraction)
                                        .background(
                                            when {
                                                isDragged -> AppColors.SurfaceElevated.copy(alpha = 0.95f)
                                                isSelected -> AppColors.SurfaceElevated
                                                isItemHovered -> AppColors.CardHover
                                                else -> Color.Transparent
                                            }
                                        )
                                        .pointerInput(p.id, sortOrder) {
                                            if (!p.isSystem && sortOrder == "Пользовательский") {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedPlaylistId = p.id
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedPlaylistId = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedPlaylistId = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val threshold = 40.dp.toPx()
                                                        if (dragOffsetY > threshold) {
                                                            val moved = library.movePlaylist(p.id, moveUp = false)
                                                            if (moved) {
                                                                dragOffsetY -= threshold
                                                            } else {
                                                                dragOffsetY = dragOffsetY.coerceAtMost(threshold)
                                                            }
                                                        } else if (dragOffsetY < -threshold) {
                                                            val moved = library.movePlaylist(p.id, moveUp = true)
                                                            if (moved) {
                                                                dragOffsetY += threshold
                                                            } else {
                                                                dragOffsetY = dragOffsetY.coerceAtLeast(-threshold)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        .clickable { onNavigate(AppScreen.PlaylistView(p.id)) }
                                        .padding(horizontal = if (isCompact) spacing.xxs else spacing.xs, vertical = spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
                                ) {
                                    if (isFav) {
                                        FavoritesCoverBox(
                                            modifier = Modifier.size(32.dp),
                                            iconSize = 16.dp,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(fallbackGradientFor(p.name)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (!isCompact) {
                                        Spacer(Modifier.width(spacing.xs))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                p.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (isSelected) AppColors.Accent else if (isItemHovered) AppColors.TextPrimary else AppColors.TextSecondary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                pluralRu(p.trackIds.size, "трек", "трека", "треков"),
                                                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Manual playlist reordering (up/down arrows when custom sort)
                                        if (isItemHovered && sortOrder == "Пользовательский" && !p.isSystem) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { library.movePlaylist(p.id, moveUp = true) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Выше", tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(
                                                    onClick = { library.movePlaylist(p.id, moveUp = false) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Ниже", tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Draggable right edge handle for resizing sidebar
        if (!isCompact) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val newWidth = (sidebarWidth + dragAmount.dp).coerceIn(160.dp, 360.dp)
                            onWidthChange(newWidth)
                        }
                    }
            )
        }
    }
}

@Composable
internal fun SidebarNavItem(
    label: String,
    icon: ImageVector,
    isCompact: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val interactionSource = remember(label) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> AppColors.SurfaceElevated
                    isHovered -> AppColors.CardHover
                    else -> Color.Transparent
                }
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = if (isCompact) spacing.xs else spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AppColors.Accent else if (isHovered) AppColors.TextPrimary else AppColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        if (!isCompact) {
            Spacer(Modifier.width(spacing.sm))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColors.TextPrimary else if (isHovered) AppColors.TextPrimary else AppColors.TextSecondary,
                )
            )
        }
    }
}
