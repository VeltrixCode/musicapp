package com.example.musicapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.library.SortColumn
import com.example.musicapp.library.SortDirection
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.launchAudioImport
import com.example.musicapp.platform.loadArtwork
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.player.PlayerController
import com.example.musicapp.player.RepeatMode
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.AppTheme
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.util.pluralRu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NavSection { Library, Playlists }

/**
 * Root MusicApp Composable.
 * Features:
 * - Spotify Dark Theme with contrast adhering to WCAG AA
 * - Responsive layout: desktop side navigation vs compact mobile navigation
 * - Debounced search with match highlighting
 * - Column sorting with indicators
 * - Undo deletion with Snackbar
 * - Custom scrub bar & volume slider with hover-only thumbs
 * - Animated 3-bar equalizer on active playing track
 * - Embedded ID3 artwork extraction with smooth gradient fallbacks
 */
@Composable
fun MusicApp(
    player: AudioPlayer = remember { PlayerController() },
    library: Library = remember { Library() },
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var navSection by remember { mutableStateOf(NavSection.Library) }
    var openPlaylistId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Search debounce 250ms
    LaunchedEffect(searchQuery) {
        delay(250)
        debouncedQuery = searchQuery.trim()
    }

    AppTheme {
        val spacing = LocalSpacing.current

        Surface(
            modifier = modifier.fillMaxSize(),
            color = AppColors.Background,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 900.dp

                Column(Modifier.fillMaxSize()) {
                    // Main content area
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Sidebar Navigation
                        SpotifySidebar(
                            library = library,
                            currentSection = navSection,
                            openPlaylistId = openPlaylistId,
                            isCompact = isCompact,
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

                        // Main Viewport (Constrained to max-width 1400dp and centered)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(AppColors.Background),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 1400.dp)
                                    .fillMaxWidth()
                            ) {
                                // Top Bar (Search + Upload)
                                SpotifyTopBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onUploadSong = {
                                        launchAudioImport { track ->
                                            library.addTrack(track)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Трек «${track.title}» добавлен в медиатеку")
                                            }
                                        }
                                    },
                                    isCompact = isCompact,
                                )

                                // Main View Content
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = if (isCompact) spacing.sm else spacing.lg)
                                ) {
                                    when (navSection) {
                                        NavSection.Library -> {
                                            LibraryView(
                                                library = library,
                                                player = player,
                                                searchQuery = debouncedQuery,
                                                isCompact = isCompact,
                                                onDeleteTrack = { track ->
                                                    val snapshot = library.removeTrack(track.id)
                                                    player.onTrackRemoved(track.id)
                                                    if (snapshot != null) {
                                                        coroutineScope.launch {
                                                            val result = snackbarHostState.showSnackbar(
                                                                message = "Трек «${track.title}» удален",
                                                                actionLabel = "Отменить",
                                                                duration = androidx.compose.material3.SnackbarDuration.Short
                                                            )
                                                            if (result == SnackbarResult.ActionPerformed) {
                                                                library.undoDelete()
                                                            } else {
                                                                library.finalizeDelete(track)
                                                            }
                                                        }
                                                    }
                                                },
                                                onAddToPlaylist = { pId, track ->
                                                    library.addToPlaylist(pId, track.id)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Трек добавлен в плейлист")
                                                    }
                                                }
                                            )
                                        }
                                        NavSection.Playlists -> {
                                            PlaylistDetailView(
                                                library = library,
                                                player = player,
                                                playlistId = openPlaylistId,
                                                isCompact = isCompact,
                                                onBack = {
                                                    navSection = NavSection.Library
                                                    openPlaylistId = null
                                                },
                                                onAddToPlaylist = { pId, track ->
                                                    library.addToPlaylist(pId, track.id)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Трек добавлен в плейлист")
                                                    }
                                                },
                                                onRemoveFromPlaylist = { pId, track ->
                                                    library.removeFromPlaylist(pId, track.id)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Трек удален из плейлиста")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Fixed Player Bar (Stretches 100% of window width)
                    SpotifyNowPlayingBar(
                        player = player,
                        isCompact = isCompact,
                    )
                }

                // Global Snackbar Host for Delete Undo & Alerts
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = AppColors.SurfaceElevated,
                        contentColor = AppColors.TextPrimary,
                        actionColor = AppColors.Accent,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                library.createPlaylist(name)
                showCreatePlaylistDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Плейлист «$name» создан")
                }
            }
        )
    }
}

// ==========================================
// Top Bar with Centered Search & Upload Button
// ==========================================
@Composable
private fun SpotifyTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUploadSong: () -> Unit,
    isCompact: Boolean,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isCompact) spacing.sm else spacing.lg,
                vertical = spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Search Input Box
        Box(
            modifier = Modifier
                .width(if (isCompact) 220.dp else 360.dp)
                .height(44.dp)
                .clip(CircleShape)
                .background(AppColors.SurfaceElevated)
                .padding(horizontal = spacing.sm),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Поиск",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(spacing.xs))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Что хочешь послушать?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.TextMuted,
                                fontSize = 13.5.sp,
                            ),
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(AppColors.Accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Очистить поиск",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Upload Song Pill Button
        Button(
            onClick = onUploadSong,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.xs),
            modifier = Modifier.height(42.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FileUpload,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            if (!isCompact) {
                Spacer(Modifier.width(spacing.xs))
                Text("Загрузить трек", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ==========================================
// Sidebar Component
// ==========================================
@Composable
private fun SpotifySidebar(
    library: Library,
    currentSection: NavSection,
    openPlaylistId: String?,
    isCompact: Boolean,
    onSelectSection: (NavSection) -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val sidebarWidth = if (isCompact) 72.dp else 240.dp

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(Color.Black)
            .padding(vertical = spacing.md, horizontal = if (isCompact) spacing.xs else spacing.sm)
    ) {
        // App Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = spacing.md, start = if (isCompact) 0.dp else spacing.xs),
            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
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
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isCompact) {
                Spacer(Modifier.width(spacing.xs))
                Text(
                    "MusicApp",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                )
            }
        }

        // Library Nav Item
        val isLibrarySelected = currentSection == NavSection.Library && openPlaylistId == null
        SidebarNavItem(
            label = "Моя медиатека",
            icon = Icons.Rounded.LibraryMusic,
            isCompact = isCompact,
            isSelected = isLibrarySelected,
            onClick = { onSelectSection(NavSection.Library) }
        )

        Spacer(Modifier.height(spacing.sm))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
        Spacer(Modifier.height(spacing.sm))

        // Playlists Section Header
        if (!isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ПЛЕЙЛИСТЫ",
                    style = MaterialTheme.typography.labelSmall
                )
                IconButton(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = AppColors.TextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Создать плейлист",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            IconButton(
                onClick = onCreatePlaylist,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterHorizontally),
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

        Spacer(Modifier.height(spacing.xs))

        // Playlists List
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(library.playlists, key = { _, p -> p.id }) { _, p ->
                val isSelected = openPlaylistId == p.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AppColors.SurfaceElevated else Color.Transparent)
                        .clickable { onSelectPlaylist(p.id) }
                        .padding(horizontal = if (isCompact) spacing.xxs else spacing.xs, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fallbackGradientFor(p.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (!isCompact) {
                        Spacer(Modifier.width(spacing.xs))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) AppColors.Accent else AppColors.TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val trackCount = p.trackIds.size
                            Text(
                                "$trackCount ${pluralRu(trackCount, "трек", "трека", "треков")}",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCompact: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
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
            .clickable { onClick() }
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

// ==========================================
// Hero Banner (56dp circle, scale 1.05 hover)
// ==========================================
@Composable
private fun HeroBanner(
    title: String,
    subtitle: String,
    badge: String,
    coverBitmap: ImageBitmap?,
    canPlay: Boolean,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPlayHovered by interactionSource.collectIsHoveredAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPlayHovered && canPlay) 1.05f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.SurfaceElevated, AppColors.Background)
                )
            )
            .padding(spacing.md),
        verticalAlignment = Alignment.Bottom
    ) {
        // Large Cover Art
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(fallbackGradientFor(title)),
            contentAlignment = Alignment.Center
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap,
                    contentDescription = "Обложка $title",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(Modifier.width(spacing.md))

        Column(Modifier.weight(1f)) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextSecondary)
            )
            Spacer(Modifier.height(spacing.sm))

            // 56dp Green Play Button with scale animation
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
        }
    }
}

// ==========================================
// Table Header with Sorting
// ==========================================
@Composable
private fun TableHeader(
    sortColumn: SortColumn,
    sortDirection: SortDirection,
    onSort: (SortColumn) -> Unit,
    isCompact: Boolean,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number column: 40dp
        Box(
            modifier = Modifier
                .width(40.dp)
                .clickable { onSort(SortColumn.Number) },
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

        // Title column: weight(1f)
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onSort(SortColumn.Title) },
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

        // Duration column: 80dp
        if (!isCompact) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .clickable { onSort(SortColumn.Duration) },
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sortColumn == SortColumn.Duration) {
                        Icon(
                            imageVector = if (sortDirection == SortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Длительность",
                        tint = if (sortColumn == SortColumn.Duration) AppColors.Accent else AppColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Action buttons column: 96dp
        Box(
            modifier = Modifier.width(96.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                "ДЕЙСТВИЯ",
                style = MaterialTheme.typography.labelSmall.copy(color = AppColors.TextMuted)
            )
        }
    }

    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    Spacer(Modifier.height(spacing.xxs))
}

// ==========================================
// Track Row Item
// ==========================================
@Composable
private fun TrackRowItem(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    searchQuery: String,
    isCompact: Boolean,
    playlists: List<Playlist>,
    onPlay: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    var menuOpen by remember { mutableStateOf(false) }
    val rowInteraction = remember { MutableInteractionSource() }
    val isRowHovered by rowInteraction.collectIsHoveredAsState()

    // Asynchronously load embedded ID3 artwork
    val artworkBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = track.source) {
        val src = track.source
        value = if (src != null) loadArtwork(src) else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isCurrent -> AppColors.SurfaceElevated
                    isRowHovered -> AppColors.CardHover
                    else -> Color.Transparent
                }
            )
            .hoverable(rowInteraction)
            .clickable { onPlay() }
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number / Animated Equalizer / Play Icon: 40dp
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when {
                isPlaying -> {
                    AnimatedEqualizer()
                }
                isCurrent -> {
                    // Frozen equalizer state when paused
                    FrozenEqualizer()
                }
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

        // Thumbnail Cover Art
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(fallbackGradientFor(track.title)),
            contentAlignment = Alignment.Center
        ) {
            if (artworkBitmap != null) {
                Image(
                    bitmap = artworkBitmap!!,
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

        Spacer(Modifier.width(spacing.sm))

        // Title and Artist: weight(1f)
        Column(Modifier.weight(1f)) {
            Text(
                text = highlightQuery(track.title, searchQuery, isCurrent),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = highlightQuery(track.artist, searchQuery, isCurrent = false),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration: 80dp
        if (!isCompact) {
            Text(
                text = if (track.durationMs > 0) formatTime(track.durationMs) else "--:--",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.width(80.dp),
                maxLines = 1
            )
        }

        // Actions: 96dp
        Row(
            modifier = Modifier.width(96.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            val showActions = isRowHovered || isCompact

            if (showActions) {
                // Add to Playlist Button
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Добавить в плейлист",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

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
                }

                // Remove from Playlist Button
                if (onRemoveFromPlaylist != null) {
                    IconButton(
                        onClick = onRemoveFromPlaylist,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Убрать из плейлиста",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Delete Track Button
                if (onDelete != null) {
                    val deleteInteraction = remember { MutableInteractionSource() }
                    val isDeleteHovered by deleteInteraction.collectIsHoveredAsState()

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .hoverable(deleteInteraction)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Удалить трек",
                            tint = if (isDeleteHovered) AppColors.ErrorHover else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

// ==========================================
// Library View
// ==========================================
@Composable
private fun LibraryView(
    library: Library,
    player: AudioPlayer,
    searchQuery: String,
    isCompact: Boolean,
    onDeleteTrack: (Track) -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
) {
    val spacing = LocalSpacing.current
    var sortColumn by remember { mutableStateOf(SortColumn.Number) }
    var sortDirection by remember { mutableStateOf(SortDirection.Ascending) }
    var pendingDelete by remember { mutableStateOf<Track?>(null) }

    val filtered = library.tracks.filter { t ->
        searchQuery.isBlank() ||
            t.title.contains(searchQuery, ignoreCase = true) ||
            t.artist.contains(searchQuery, ignoreCase = true)
    }

    val sorted = remember(filtered, sortColumn, sortDirection) {
        val comparator = when (sortColumn) {
            SortColumn.Number -> compareBy<Track> { library.tracks.indexOf(it) }
            SortColumn.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortColumn.Artist -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SortColumn.Duration -> compareBy { it.durationMs }
        }
        if (sortDirection == SortDirection.Ascending) filtered.sortedWith(comparator)
        else filtered.sortedWith(comparator.reversed())
    }

    Column(Modifier.fillMaxSize()) {
        val trackCount = library.tracks.size
        // Hero Header Banner
        HeroBanner(
            title = "Моя медиатека",
            subtitle = "$trackCount ${pluralRu(trackCount, "трек", "трека", "треков")}",
            badge = "КОЛЛЕКЦИЯ",
            coverBitmap = null,
            canPlay = sorted.any { it.source != null },
            isPlaying = player.isPlaying && sorted.any { it.id == player.current?.id },
            onPlayAll = {
                sorted.firstOrNull { it.source != null }?.let { player.play(it, sorted) }
            }
        )

        Spacer(Modifier.height(spacing.md))

        // Table Header
        TableHeader(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            onSort = { col ->
                if (sortColumn == col) {
                    sortDirection = if (sortDirection == SortDirection.Ascending) SortDirection.Descending else SortDirection.Ascending
                } else {
                    sortColumn = col
                    sortDirection = SortDirection.Ascending
                }
            },
            isCompact = isCompact
        )

        if (sorted.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        if (searchQuery.isBlank()) "В медиатеке пока нет треков" else "По запросу «$searchQuery» ничего не найдено",
                        style = MaterialTheme.typography.bodyLarge.copy(color = AppColors.TextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(sorted, key = { _, track -> track.id }) { index, track ->
                    TrackRowItem(
                        index = index + 1,
                        track = track,
                        isPlaying = player.current?.id == track.id && player.isPlaying,
                        isCurrent = player.current?.id == track.id,
                        searchQuery = searchQuery,
                        isCompact = isCompact,
                        playlists = library.playlists,
                        onPlay = { player.play(track, sorted) },
                        onAddToPlaylist = { pId -> onAddToPlaylist(pId, track) },
                        onDelete = { pendingDelete = track }
                    )
                }
            }
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

// ==========================================
// Playlist Detail View
// ==========================================
@Composable
private fun PlaylistDetailView(
    library: Library,
    player: AudioPlayer,
    playlistId: String?,
    isCompact: Boolean,
    onBack: () -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
    onRemoveFromPlaylist: (String, Track) -> Unit,
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

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Назад к медиатеке", color = AppColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { pendingDeletePlaylist = true }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = AppColors.Error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(spacing.xxs))
                Text("Удалить плейлист", color = AppColors.Error, fontSize = 13.sp)
            }
        }

        val trackCount = tracks.size
        HeroBanner(
            title = playlist.name,
            subtitle = "$trackCount ${pluralRu(trackCount, "трек", "трека", "треков")}",
            badge = "ПЛЕЙЛИСТ",
            coverBitmap = null,
            canPlay = tracks.any { it.source != null },
            isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
            onPlayAll = {
                tracks.firstOrNull { it.source != null }?.let { player.play(it, tracks) }
            }
        )

        Spacer(Modifier.height(spacing.md))
        TableHeader(
            sortColumn = SortColumn.Number,
            sortDirection = SortDirection.Ascending,
            onSort = {},
            isCompact = isCompact
        )

        if (tracks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "В этом плейлисте пока нет треков. Добавьте их из Медиатеки!",
                    color = AppColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRowItem(
                        index = index + 1,
                        track = track,
                        isPlaying = player.current?.id == track.id && player.isPlaying,
                        isCurrent = player.current?.id == track.id,
                        searchQuery = "",
                        isCompact = isCompact,
                        playlists = library.playlists,
                        onPlay = { player.play(track, tracks) },
                        onAddToPlaylist = { pId -> onAddToPlaylist(pId, track) },
                        onRemoveFromPlaylist = { onRemoveFromPlaylist(playlist.id, track) }
                    )
                }
            }
        }
    }

    if (pendingDeletePlaylist) {
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylist = false },
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

// ==========================================
// Bottom Fixed Player Bar
// ==========================================
@Composable
private fun SpotifyNowPlayingBar(
    player: AudioPlayer,
    isCompact: Boolean,
) {
    val spacing = LocalSpacing.current
    val track = player.current
    var draggingFraction by remember { mutableStateOf<Float?>(null) }
    val duration = player.durationMs.coerceAtLeast(1L)
    val fraction = draggingFraction ?: (player.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    var isLiked by remember { mutableStateOf(false) }

    // Artwork of active track
    val activeArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = track?.source) {
        val src = track?.source
        value = if (src != null) loadArtwork(src) else null
    }

    Surface(
        color = AppColors.Surface,
        modifier = Modifier.fillMaxWidth().height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Track Info + Heart Icon (240dp)
            Row(
                modifier = Modifier.width(if (isCompact) 180.dp else 260.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track != null) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fallbackGradientFor(track.title)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeArtwork != null) {
                            Image(
                                bitmap = activeArtwork!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(spacing.sm))
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
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(spacing.xxs))
                    // Heart icon positioned right next to title / artist
                    IconButton(
                        onClick = { isLiked = !isLiked },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isLiked) "В избранном" else "Добавить в избранное",
                            tint = if (isLiked) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        "Ничего не играет",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextMuted)
                    )
                }
            }

            // Center Column: Controls + Progress Bar (weight 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { player.setShuffle(!player.shuffle) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Перемешать",
                            tint = if (player.shuffle) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = { player.previous() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Предыдущий",
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 40dp Circular White Play / Pause Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppColors.TextPrimary)
                            .clickable(enabled = track?.source != null) { player.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (player.isPlaying) "Пауза" else "Воспроизведение",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = { player.next() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Следующий",
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = { player.cycleRepeat() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (player.repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Повтор",
                            tint = if (player.repeat != RepeatMode.Off) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Progress Bar Row (Labels aligned on same baseline)
                Row(
                    modifier = Modifier.fillMaxWidth(if (isCompact) 0.95f else 0.8f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(player.positionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppColors.TextMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier.width(36.dp)
                    )

                    Spacer(Modifier.width(spacing.xs))

                    CustomScrubBar(
                        fraction = fraction,
                        onSeek = { newFrac ->
                            player.seekTo((newFrac * duration).toLong())
                        },
                        onDrag = { newFrac ->
                            draggingFraction = newFrac
                        },
                        onRelease = { finalFrac ->
                            player.seekTo((finalFrac * duration).toLong())
                            draggingFraction = null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(spacing.xs))

                    Text(
                        formatTime(player.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppColors.TextMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Right Column: Volume Controls with Tooltip
            if (!isCompact) {
                Row(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(end = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val vol = player.volume
                    val volIcon = when {
                        vol == 0f -> Icons.Rounded.VolumeOff
                        vol < 0.4f -> Icons.Rounded.VolumeMute
                        vol < 0.7f -> Icons.Rounded.VolumeDown
                        else -> Icons.Rounded.VolumeUp
                    }

                    IconButton(
                        onClick = { player.toggleMute() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = volIcon,
                            contentDescription = "Громкость",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(spacing.xxs))

                    VolumeScrubBar(
                        volume = vol,
                        onVolumeChange = { player.updateVolume(it) },
                        modifier = Modifier.width(96.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// Custom Scrub Bar (4dp track, thumb on hover)
// ==========================================
@Composable
private fun CustomScrubBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onRelease: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val newFrac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onDrag(newFrac)
                        val released = tryAwaitRelease()
                        isDragging = false
                        if (released) {
                            onRelease(newFrac)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + thumbRadius * 2)
        ) {
            val trackH = barHeight.toPx()
            val cy = size.height / 2f
            val w = size.width

            // Background Track
            drawRoundRect(
                color = AppColors.SurfaceElevated,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Active Track Fill
            val fillW = (fraction.coerceIn(0f, 1f) * w)
            val fillCol = if (isHovered || isDragging) AppColors.AccentHover else Color.White
            drawRoundRect(
                color = fillCol,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Thumb visible ONLY when hovered or dragged
            if (isHovered || isDragging) {
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius.toPx(),
                    center = Offset(fillW, cy)
                )
            }
        }
    }
}

// ==========================================
// Volume Bar with Percentage Tooltip
// ==========================================
@Composable
private fun VolumeScrubBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val newVol = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onVolumeChange(newVol)
                        tryAwaitRelease()
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Tooltip above slider on hover
        AnimatedVisibility(
            visible = isHovered || isDragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(bottom = 24.dp)
        ) {
            Surface(
                color = AppColors.SurfaceElevated,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppColors.TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val trackH = 4.dp.toPx()
            val cy = size.height / 2f
            val w = size.width

            drawRoundRect(
                color = AppColors.SurfaceElevated,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            val fillW = (volume.coerceIn(0f, 1f) * w)
            val fillCol = if (isHovered || isDragging) AppColors.AccentHover else Color.White
            drawRoundRect(
                color = fillCol,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            if (isHovered || isDragging) {
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(fillW, cy)
                )
            }
        }
    }
}

// ==========================================
// Animated Equalizer (3 bars)
// ==========================================
@Composable
private fun AnimatedEqualizer(
    color: Color = AppColors.Accent,
    modifier: Modifier = Modifier.size(16.dp),
) {
    val transition = rememberInfiniteTransition()
    val h1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), AnimRepeatMode.Reverse)
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), AnimRepeatMode.Reverse)
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), AnimRepeatMode.Reverse)
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w / 4f
        val gap = w / 8f

        drawRoundRect(color, Offset(0f, h * (1f - h1)), Size(barW, h * h1), CornerRadius(barW / 2))
        drawRoundRect(color, Offset(barW + gap, h * (1f - h2)), Size(barW, h * h2), CornerRadius(barW / 2))
        drawRoundRect(color, Offset((barW + gap) * 2, h * (1f - h3)), Size(barW, h * h3), CornerRadius(barW / 2))
    }
}

@Composable
private fun FrozenEqualizer(
    color: Color = AppColors.TextSecondary,
    modifier: Modifier = Modifier.size(16.dp),
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w / 4f
        val gap = w / 8f

        drawRoundRect(color, Offset(0f, h * 0.4f), Size(barW, h * 0.6f), CornerRadius(barW / 2))
        drawRoundRect(color, Offset(barW + gap, h * 0.2f), Size(barW, h * 0.8f), CornerRadius(barW / 2))
        drawRoundRect(color, Offset((barW + gap) * 2, h * 0.5f), Size(barW, h * 0.5f), CornerRadius(barW / 2))
    }
}

// ==========================================
// Dialogs & Helpers
// ==========================================
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text("Создать", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.SurfaceElevated
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
        title = { Text("Удалить трек «${track.title}»?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (track.source != null) {
                    "Трек будет удален из медиатеки и всех плейлистов. Вы сможете отменить это действие через уведомление."
                } else {
                    "Трек будет удален из медиатеки."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
            ) { Text("Удалить", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = AppColors.SurfaceElevated
    )
}

private fun highlightQuery(text: String, query: String, isCurrent: Boolean): AnnotatedString {
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
        // Default style
        addStyle(
            style = SpanStyle(color = if (isCurrent) AppColors.Accent else AppColors.TextPrimary),
            start = 0,
            end = text.length
        )
        // Highlight match
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

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
