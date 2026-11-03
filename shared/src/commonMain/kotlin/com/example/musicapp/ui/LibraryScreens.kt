package com.example.musicapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.library.SortColumn
import com.example.musicapp.library.SortDirection
import com.example.musicapp.model.Track
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.player.PlayerController
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.ui.components.ContentCard
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.util.formatTrackDuration
import com.example.musicapp.util.pluralRu

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryView(
    library: Library,
    player: AudioPlayer,
    searchQuery: String,
    isCompact: Boolean,
    onDeleteTrack: (Track) -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
    onRename: (Track) -> Unit,
    onNavigate: ((AppScreen) -> Unit)? = null,
    onUploadSong: (() -> Unit)? = null,
    onUploadFolder: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val savedSettings = remember { (player as? PlayerController)?.settingsRepository?.load() ?: AppSettings() }
    var sortColumn by remember {
        mutableStateOf(
            SortColumn.entries.find { it.name.equals(savedSettings.librarySortColumn, ignoreCase = true) } ?: SortColumn.Number
        )
    }
    var sortDirection by remember {
        mutableStateOf(
            SortDirection.entries.find { it.name.equals(savedSettings.librarySortDirection, ignoreCase = true) } ?: SortDirection.Ascending
        )
    }
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    var selectedFilter by remember { mutableStateOf("Все") }
    var isGridView by remember { mutableStateOf(false) }
    val isSyncing by player.providerRegistry.isSyncing.collectAsState()

    val onSortChange: (SortColumn) -> Unit = { col ->
        val newDir = if (sortColumn == col) {
            if (sortDirection == SortDirection.Ascending) SortDirection.Descending else SortDirection.Ascending
        } else {
            SortDirection.Ascending
        }
        sortColumn = col
        sortDirection = newDir
        (player as? PlayerController)?.settingsRepository?.let { repo ->
            repo.save(repo.load().copy(librarySortColumn = col.name, librarySortDirection = newDir.name))
        }
    }

    LaunchedEffect(Unit) {
        library.checkFilesAvailability()
    }

    val isOnline = com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled
    val baseList = when {
        isOnline && selectedFilter == "Скачанные" -> library.getOfflineDownloadedTracks()
        isOnline && selectedFilter == "Облачные" -> library.getCloudTracks()
        isOnline && selectedFilter == "Google Диск" -> library.visibleTracks.filter { it.sourceProvider == com.example.musicapp.model.TrackSource.GOOGLE_DRIVE }
        isOnline && selectedFilter == "Яндекс Диск" -> library.visibleTracks.filter { it.sourceProvider == com.example.musicapp.model.TrackSource.YANDEX_DISK }
        else -> library.visibleTracks
    }

    val filtered = baseList.filter { t ->
        searchQuery.isBlank() ||
            t.title.contains(searchQuery, ignoreCase = true) ||
            t.artist.contains(searchQuery, ignoreCase = true) ||
            t.album.contains(searchQuery, ignoreCase = true)
    }

    val sorted = remember(filtered, sortColumn, sortDirection) {
        val comparator = when (sortColumn) {
            SortColumn.Number -> compareBy<Track> { library.visibleTracks.indexOf(it) }
            SortColumn.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortColumn.Artist -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SortColumn.Album -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
            SortColumn.Duration -> compareBy { it.durationMs }
            SortColumn.AddedAt -> compareBy { it.addedAt }
            SortColumn.PlayCount -> compareBy { it.playCount }
        }
        if (sortDirection == SortDirection.Ascending) filtered.sortedWith(comparator)
        else filtered.sortedWith(comparator.reversed())
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
                    val trackCount = library.visibleTracks.size
                    HeroBanner(
                        title = "Моя медиатека",
                        subtitle = pluralRu(trackCount, "трек", "трека", "треков"),
                        badge = "КОЛЛЕКЦИЯ",
                        coverSource = sorted.firstOrNull { it.source != null }?.source,
                        canPlay = sorted.any { it.source != null },
                        isPlaying = player.isPlaying && sorted.any { it.id == player.current?.id },
                        onPlayAll = {
                            sorted.firstOrNull { it.source != null }?.let { player.play(it, sorted) }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(spacing.sm))
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOnline) {
                            val availableFilters = buildList {
                                add("Все")
                                if (library.visibleTracks.any { it.sourceProvider == com.example.musicapp.model.TrackSource.GOOGLE_DRIVE }) {
                                    add("Google Диск")
                                }
                                if (library.visibleTracks.any { it.sourceProvider == com.example.musicapp.model.TrackSource.YANDEX_DISK }) {
                                    add("Яндекс Диск")
                                }
                                add("Облачные")
                                add("Скачанные")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableFilters.forEach { filter ->
                                    val isSel = selectedFilter == filter
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AppColors.Accent,
                                            selectedLabelColor = Color.Black,
                                            containerColor = AppColors.SurfaceElevated,
                                            labelColor = AppColors.TextPrimary
                                        )
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        // Right-aligned toolbar group: sort dropdown + list/grid toggle (no lonely icons)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box {
                                TextButton(
                                    onClick = { sortMenuOpen = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val sortLabel = when (sortColumn) {
                                        SortColumn.Number -> "По порядку"
                                        SortColumn.Title -> "По названию"
                                        SortColumn.Artist -> "По исполнителю"
                                        SortColumn.Album -> "По альбому"
                                        SortColumn.Duration -> "По длительности"
                                        SortColumn.AddedAt -> "По дате"
                                        SortColumn.PlayCount -> "По популярности"
                                    }
                                    Text(sortLabel, fontSize = 12.sp, color = AppColors.TextSecondary)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                    modifier = Modifier.background(AppColors.SurfaceElevated)
                                ) {
                                    listOf(
                                        SortColumn.Number to "По порядку",
                                        SortColumn.Title to "По названию",
                                        SortColumn.Artist to "По исполнителю",
                                        SortColumn.Album to "По альбому",
                                        SortColumn.Duration to "По длительности",
                                        SortColumn.AddedAt to "По дате",
                                        SortColumn.PlayCount to "По популярности"
                                    ).forEach { (col, title) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    title,
                                                    fontSize = 12.sp,
                                                    color = if (col == sortColumn) AppColors.Accent else AppColors.TextPrimary
                                                )
                                            },
                                            onClick = {
                                                onSortChange(col)
                                                sortMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { isGridView = !isGridView },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                    contentDescription = "Вид",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(spacing.xs))
                }

                stickyHeader {
                    Box(modifier = Modifier.fillMaxWidth().background(AppColors.Background)) {
                        TableHeader(
                            sortColumn = sortColumn,
                            sortDirection = sortDirection,
                            onSort = onSortChange,
                            isCompact = isCompact,
                            showExtendedColumns = showExtendedColumns
                        )
                    }
                }

                if (sorted.isEmpty()) {
                    if (isSyncing) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            TableSkeleton(rows = 6)
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
                                    if (searchQuery.isNotBlank()) {
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
                                            "Проверьте написание или сбросьте строку поиска",
                                            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.LibraryMusic,
                                            contentDescription = null,
                                            tint = AppColors.Accent,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Text(
                                            "В вашей медиатеке пока нет треков",
                                            style = MaterialTheme.typography.titleMedium.copy(color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            if (isOnline) "Добавьте локальные музыкальные файлы или подключите облачные источники"
                                            else "Добавьте локальные музыкальные файлы или импортируйте папку с музыкой",
                                            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (onUploadSong != null) {
                                                Button(
                                                    onClick = onUploadSong,
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated),
                                                    shape = CircleShape
                                                ) {
                                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Добавить файлы", color = AppColors.TextPrimary, fontSize = 12.sp)
                                                }
                                            }
                                            if (onUploadFolder != null) {
                                                Button(
                                                    onClick = onUploadFolder,
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated),
                                                    shape = CircleShape
                                                ) {
                                                    Icon(Icons.Rounded.DriveFolderUpload, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Импорт папки", color = AppColors.TextPrimary, fontSize = 12.sp)
                                                }
                                            }
                                            if (isOnline && onNavigate != null) {
                                                Button(
                                                    onClick = { onNavigate(AppScreen.CloudSources) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                                                    shape = CircleShape
                                                ) {
                                                    Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Подключить облако", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(sorted, key = { _, track -> track.id }) { index, track ->
                        TrackRowItem(
                            index = index + 1,
                            track = track,
                            isPlaying = player.current?.id == track.id && player.isPlaying,
                            isCurrent = player.current?.id == track.id,
                            searchQuery = searchQuery,
                            isCompact = isCompact,
                            showExtendedColumns = showExtendedColumns,
                            playlists = library.playlists,
                            durationText = formatTrackDuration(track, player),
                            onPlay = { player.play(track, sorted) },
                            onPlayNext = { player.playNext(track) },
                            onAddToQueue = { player.addToQueue(track) },
                            onAddToPlaylist = { pId -> onAddToPlaylist(pId, track) },
                            onToggleFavorite = { library.toggleFavorite(track) },
                            onRename = { onRename(track) },
                            onDelete = { pendingDelete = track },
                            onRelocateFile = {
                                com.example.musicapp.platform.launchAudioImport { imp ->
                                    imp.source?.let { library.updateTrackSource(track.id, it) }
                                }
                            }
                        )
                    }
                }
            }

            StickyCompactHeader(
                title = "Моя медиатека",
                dominantColor = Color(0xFF1E3264),
                canPlay = sorted.any { it.source != null },
                isPlaying = player.isPlaying && sorted.any { it.id == player.current?.id },
                onPlayAll = {
                    sorted.firstOrNull { it.source != null }?.let { player.play(it, sorted) }
                },
                visible = showStickyHeader
            )
        }
    }

    pendingDelete?.let { track ->
        DeleteTrackDialog(
            track = track,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteTrack(track)
                pendingDelete = null
            }
        )
    }
}

@Composable
internal fun AlbumsGroupView(
    library: Library,
    player: AudioPlayer,
    onNavigateToAlbum: (String) -> Unit,
    onBack: () -> Unit,
) {
    val albumsMap = remember(library.visibleTracks.size) {
        library.visibleTracks
            .filter { it.album.isNotBlank() && it.album != "Неизвестный альбом" }
            .groupBy { it.album }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    val listState = rememberLazyGridState()
    val showStickyHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HeroBanner(
                title = "Альбомы",
                subtitle = pluralRu(albumsMap.size, "альбом", "альбома", "альбомов"),
                badge = "КОЛЛЕКЦИЯ",
                coverSource = albumsMap.firstOrNull()?.second?.firstOrNull()?.source,
                canPlay = albumsMap.isNotEmpty(),
                isPlaying = false,
                onPlayAll = {
                    val all = albumsMap.flatMap { it.second }
                    if (all.isNotEmpty()) player.play(all.first(), all)
                }
            )

            if (albumsMap.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("В медиатеке пока нет альбомов", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Теги альбомов появятся здесь автоматически после импорта треков.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 170.dp),
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(albumsMap, key = { it.first }) { (albumName, albumTracks) ->
                        val firstTrack = albumTracks.firstOrNull()
                        ContentCard(
                            title = albumName,
                            subtitle = firstTrack?.artist ?: pluralRu(albumTracks.size, "трек", "трека", "треков"),
                            cardWidth = 175.dp,
                            badgeText = "${albumTracks.size}",
                            isPlaying = player.isPlaying && albumTracks.any { it.id == player.current?.id },
                            onClick = { onNavigateToAlbum(albumName) },
                            onPlayClick = {
                                if (albumTracks.isNotEmpty()) player.play(albumTracks.first(), albumTracks)
                            }
                        )
                    }
                }
            }
        }

        StickyCompactHeader(
            title = "Альбомы",
            dominantColor = AppColors.SurfaceElevated,
            canPlay = albumsMap.isNotEmpty(),
            isPlaying = false,
            onPlayAll = {
                val all = albumsMap.flatMap { it.second }
                if (all.isNotEmpty()) player.play(all.first(), all)
            },
            visible = showStickyHeader
        )
    }
}

@Composable
internal fun ArtistsGroupView(
    library: Library,
    player: AudioPlayer,
    onNavigateToArtist: (String) -> Unit,
    onBack: () -> Unit,
) {
    val artistsMap = remember(library.visibleTracks.size) {
        library.visibleTracks
            .filter { it.artist.isNotBlank() && it.artist != "Неизвестный исполнитель" }
            .groupBy { it.artist }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    val listState = rememberLazyGridState()
    val showStickyHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HeroBanner(
                title = "Исполнители",
                subtitle = pluralRu(artistsMap.size, "исполнитель", "исполнителя", "исполнителей"),
                badge = "КОЛЛЕКЦИЯ",
                coverSource = artistsMap.firstOrNull()?.second?.firstOrNull()?.source,
                canPlay = artistsMap.isNotEmpty(),
                isPlaying = false,
                onPlayAll = {
                    val all = artistsMap.flatMap { it.second }
                    if (all.isNotEmpty()) player.play(all.first(), all)
                }
            )

            if (artistsMap.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("В медиатеке пока нет исполнителей", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Имена исполнителей появятся здесь после импорта треков.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(artistsMap, key = { it.first }) { (artistName, artistTracks) ->
                        ContentCard(
                            title = artistName,
                            subtitle = pluralRu(artistTracks.size, "трек", "трека", "треков"),
                            cardWidth = 165.dp,
                            isArtist = true,
                            isPlaying = player.isPlaying && artistTracks.any { it.id == player.current?.id },
                            onClick = { onNavigateToArtist(artistName) },
                            onPlayClick = {
                                if (artistTracks.isNotEmpty()) player.play(artistTracks.first(), artistTracks)
                            }
                        )
                    }
                }
            }
        }

        StickyCompactHeader(
            title = "Исполнители",
            dominantColor = AppColors.SurfaceElevated,
            canPlay = artistsMap.isNotEmpty(),
            isPlaying = false,
            onPlayAll = {
                val all = artistsMap.flatMap { it.second }
                if (all.isNotEmpty()) player.play(all.first(), all)
            },
            visible = showStickyHeader
        )
    }
}

@Composable
internal fun RenameTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Редактировать трек", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Исполнитель") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, artist) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text("Сохранить", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = AppColors.SurfaceElevated
    )
}

@Composable
internal fun DeleteTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Удалить трек «${track.title}»?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (track.source != null) {
                    "Трек будет удален из медиатеки и плейлистов. Действие можно отменить через уведомление."
                } else {
                    "Трек будет удален из медиатеки."
                }
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)) {
                Text("Удалить", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = AppColors.SurfaceElevated
    )
}
