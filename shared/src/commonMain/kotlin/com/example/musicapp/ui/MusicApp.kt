package com.example.musicapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableStateListOf
import com.example.musicapp.ui.components.TrackActionMenu
import com.example.musicapp.ui.screens.CloudSourcesScreen
import com.example.musicapp.ui.screens.HomeScreen
import com.example.musicapp.ui.screens.SearchScreen
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.material.icons.rounded.Speed
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
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
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.library.FAVORITES_PLAYLIST_ID
import com.example.musicapp.library.Library
import com.example.musicapp.library.SortColumn
import com.example.musicapp.library.SortDirection
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.launchAudioImport
import com.example.musicapp.platform.launchFolderImport
import com.example.musicapp.platform.loadArtwork
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.player.EqualizerPreset
import com.example.musicapp.player.PlayerController
import com.example.musicapp.player.RepeatMode
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.AppTheme
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.util.pluralRu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AppScreen {
    object Home : AppScreen()
    object Search : AppScreen()
    object Library : AppScreen()
    object CloudSources : AppScreen()
    data class PlaylistView(val playlistId: String) : AppScreen()
    data class SmartMixView(val title: String) : AppScreen()
}

/**
 * Root JVXMusic Composable.
 * Features:
 * - Spotify Dark Theme & Green Accent (#1DB954)
 * - Navigation history stack with back/forward controls
 * - Resizable & collapsible sidebar (72dp - 360dp)
 * - Home screen with time-of-day greeting, quick cards, and horizontal shelves
 * - Cloud music streaming (Google Drive, Yandex Disk, Jamendo, Audius)
 * - Local embedded streaming proxy for Desktop JavaFX Media (Range/Seek support)
 * - LRU disk audio cache with offline downloads & preloading
 * - Search by genres with colorful category grid and live sectioned results
 * - 10-band equalizer, spectrum visualizer, karaokee LRC lyrics
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

    val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }
    val forwardStack = remember { mutableStateListOf<AppScreen>() }

    fun navigateTo(screen: AppScreen) {
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
            forwardStack.clear()
        }
    }

    fun goBack() {
        if (backStack.size > 1) {
            val curr = backStack.removeLast()
            forwardStack.add(curr)
        }
    }

    fun goForward() {
        if (forwardStack.isNotEmpty()) {
            val next = forwardStack.removeLast()
            backStack.add(next)
        }
    }

    val currentScreen = backStack.lastOrNull() ?: AppScreen.Home
    var sidebarWidth by remember { mutableStateOf(240.dp) }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    // Dialogs & panels
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueDrawer by remember { mutableStateOf(false) }
    var isFullscreenNowPlaying by remember { mutableStateOf(false) }
    var renamingTrack by remember { mutableStateOf<Track?>(null) }

    // Search debounce 250ms
    LaunchedEffect(searchQuery) {
        delay(250)
        debouncedQuery = searchQuery.trim()
    }

    // Track recently played
    val currentTrack = player.current
    LaunchedEffect(currentTrack?.id) {
        if (currentTrack != null) {
            library.recordTrackPlayed(currentTrack)
        }
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
                    // Main layout
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Left Sidebar Navigation with Resizable Width
                        if (!isCompact) {
                            SpotifySidebar(
                                library = library,
                                currentScreen = currentScreen,
                                sidebarWidth = sidebarWidth,
                                onWidthChange = { sidebarWidth = it },
                                isCompact = false,
                                onNavigate = { navigateTo(it) },
                                onCreatePlaylist = { showCreatePlaylistDialog = true },
                            )
                        }

                        // Main Content (Centered and constrained to max-width 1400dp)
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
                                // Top Action Bar with History Back/Forward
                                SpotifyTopBar(
                                    canGoBack = backStack.size > 1,
                                    canGoForward = forwardStack.isNotEmpty(),
                                    onGoBack = { goBack() },
                                    onGoForward = { goForward() },
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = {
                                        searchQuery = it
                                        if (it.isNotBlank() && currentScreen !is AppScreen.Search) {
                                            navigateTo(AppScreen.Search)
                                        }
                                    },
                                    onUploadSong = {
                                        launchAudioImport { track ->
                                            library.addTrack(track)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Трек «${track.title}» добавлен в медиатеку")
                                            }
                                        }
                                    },
                                    onUploadFolder = {
                                        launchFolderImport { tracks ->
                                            library.addTracks(tracks)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Импортировано треков: ${tracks.size}")
                                            }
                                        }
                                    },
                                    onOpenSources = { navigateTo(AppScreen.CloudSources) },
                                    isCompact = isCompact,
                                )

                                // Format unsupported warning banner if needed
                                player.unsupportedFormatNotice?.let { notice ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = spacing.md, vertical = spacing.xxs)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.Error.copy(alpha = 0.2f))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(10.dp))
                                                Text(notice, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick = { player.dismissFormatNotice() }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Main View Content
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = if (isCompact) spacing.sm else spacing.lg)
                                ) {
                                    AnimatedContent(
                                        targetState = currentScreen,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(220, delayMillis = 30)) +
                                                    slideInHorizontally(animationSpec = tween(220)) { it / 8 })
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(160)) +
                                                            slideOutHorizontally(animationSpec = tween(160)) { -it / 8 }
                                                )
                                        }
                                    ) { screen ->
                                        when (screen) {
                                            is AppScreen.Home -> {
                                                HomeScreen(
                                                    library = library,
                                                    player = player,
                                                    onNavigatePlaylist = { pId -> navigateTo(AppScreen.PlaylistView(pId)) },
                                                    onNavigateSmartMix = { mixTitle -> navigateTo(AppScreen.SmartMixView(mixTitle)) },
                                                    onNavigateCloud = { navigateTo(AppScreen.CloudSources) },
                                                    isCompact = isCompact
                                                )
                                            }
                                            is AppScreen.Search -> {
                                                SearchScreen(
                                                    library = library,
                                                    player = player,
                                                    onTrackClick = { t -> player.play(t, listOf(t)) },
                                                    isCompact = isCompact
                                                )
                                            }
                                            is AppScreen.Library -> {
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
                                                    },
                                                    onRename = { renamingTrack = it }
                                                )
                                            }
                                            is AppScreen.CloudSources -> {
                                                CloudSourcesScreen(
                                                    library = library,
                                                    player = player,
                                                    onShowMessage = { msg ->
                                                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                                    }
                                                )
                                            }
                                            is AppScreen.PlaylistView -> {
                                                PlaylistDetailView(
                                                    library = library,
                                                    player = player,
                                                    playlistId = screen.playlistId,
                                                    isCompact = isCompact,
                                                    onBack = { goBack() },
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
                                                    },
                                                    onRename = { renamingTrack = it }
                                                )
                                            }
                                            is AppScreen.SmartMixView -> {
                                                SmartMixDetailView(
                                                    title = screen.title,
                                                    library = library,
                                                    player = player,
                                                    isCompact = isCompact,
                                                    onBack = { goBack() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right Queue Drawer
                        AnimatedVisibility(
                            visible = showQueueDrawer,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            QueueDrawer(
                                player = player,
                                onClose = { showQueueDrawer = false }
                            )
                        }
                    }

                    // Bottom Fixed Player Bar (Stretches full width)
                    SpotifyNowPlayingBar(
                        player = player,
                        library = library,
                        isCompact = isCompact,
                        onOpenFullscreen = { isFullscreenNowPlaying = true },
                        onToggleQueue = { showQueueDrawer = !showQueueDrawer },
                        onOpenEqualizer = { showEqualizerDialog = true },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        onOpenSpeed = { showSpeedDialog = true },
                    )

                    // Bottom Navigation Bar for Compact/Android devices
                    if (isCompact) {
                        NavigationBar(
                            containerColor = Color.Black,
                            tonalElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            NavigationBarItem(
                                selected = currentScreen is AppScreen.Home,
                                onClick = { navigateTo(AppScreen.Home) },
                                icon = { Icon(Icons.Rounded.Home, contentDescription = "Главная") },
                                label = { Text("Главная", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Accent,
                                    selectedTextColor = AppColors.Accent,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = AppColors.TextMuted,
                                    unselectedTextColor = AppColors.TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen is AppScreen.Search,
                                onClick = { navigateTo(AppScreen.Search) },
                                icon = { Icon(Icons.Rounded.Search, contentDescription = "Поиск") },
                                label = { Text("Поиск", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Accent,
                                    selectedTextColor = AppColors.Accent,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = AppColors.TextMuted,
                                    unselectedTextColor = AppColors.TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen is AppScreen.Library,
                                onClick = { navigateTo(AppScreen.Library) },
                                icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Медиатека") },
                                label = { Text("Медиатека", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Accent,
                                    selectedTextColor = AppColors.Accent,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = AppColors.TextMuted,
                                    unselectedTextColor = AppColors.TextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = currentScreen is AppScreen.CloudSources,
                                onClick = { navigateTo(AppScreen.CloudSources) },
                                icon = { Icon(Icons.Rounded.CloudQueue, contentDescription = "Облако") },
                                label = { Text("Облако", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Accent,
                                    selectedTextColor = AppColors.Accent,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = AppColors.TextMuted,
                                    unselectedTextColor = AppColors.TextMuted
                                )
                            )
                        }
                    }
                }

                // Fullscreen Now Playing Overlay
                if (isFullscreenNowPlaying) {
                    FullscreenNowPlayingView(
                        player = player,
                        library = library,
                        onClose = { isFullscreenNowPlaying = false }
                    )
                }

                // Global Snackbar Host
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

    // Dialogs
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

    if (showEqualizerDialog) {
        EqualizerDialog(
            player = player,
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            player = player,
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showSpeedDialog) {
        SpeedDialog(
            player = player,
            onDismiss = { showSpeedDialog = false }
        )
    }

    renamingTrack?.let { trk ->
        RenameTrackDialog(
            track = trk,
            onDismiss = { renamingTrack = null },
            onSave = { newTitle, newArtist ->
                library.renameTrack(trk.id, newTitle, newArtist)
                renamingTrack = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Трек переименован")
                }
            }
        )
    }
}

// ==========================================
// Top Bar with Centered Search & Upload Buttons
// ==========================================
@Composable
private fun SpotifyTopBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUploadSong: () -> Unit,
    onUploadFolder: () -> Unit,
    onOpenSources: () -> Unit,
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
        // Navigation Back & Forward Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onGoBack,
                enabled = canGoBack,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад",
                    tint = if (canGoBack) Color.White else AppColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onGoForward,
                enabled = canGoForward,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Вперед",
                    tint = if (canGoForward) Color.White else AppColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(spacing.md))

        // Search Input Box
        Box(
            modifier = Modifier
                .width(if (isCompact) 180.dp else 360.dp)
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

        Spacer(Modifier.width(spacing.md))

        // Right Action Buttons: Cloud Sources, Import file, Import folder
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenSources,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceElevated)
            ) {
                Icon(Icons.Rounded.CloudQueue, contentDescription = "Источники и Облако", tint = AppColors.Accent, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(spacing.xs))

            Button(
                onClick = onUploadSong,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
                if (!isCompact) {
                    Spacer(Modifier.width(spacing.xs))
                    Text("Файл", color = AppColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.width(spacing.xs))

            Button(
                onClick = onUploadFolder,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
                if (!isCompact) {
                    Spacer(Modifier.width(spacing.xs))
                    Text("Папка", color = AppColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ==========================================
// Sidebar Component with Empty State
// ==========================================
@Composable
private fun SpotifySidebar(
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
                        "JVXMusic",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = AppColors.TextPrimary
                        )
                    )
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

            // Nav 4: Cloud & Sources
            SidebarNavItem(
                label = "Облако и Источники",
                icon = Icons.Rounded.CloudQueue,
                isCompact = isCompact,
                isSelected = currentScreen is AppScreen.CloudSources,
                onClick = { onNavigate(AppScreen.CloudSources) }
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
                    Text("ПЛЕЙЛИСТЫ", style = MaterialTheme.typography.labelSmall)
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

            Spacer(Modifier.height(spacing.xs))

            // Playlists List
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(library.playlists, key = { _, p -> p.id }) { _, p ->
                    val isSelected = currentScreen is AppScreen.PlaylistView && (currentScreen as AppScreen.PlaylistView).playlistId == p.id
                    val isFav = p.id == FAVORITES_PLAYLIST_ID

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AppColors.SurfaceElevated else Color.Transparent)
                            .clickable { onNavigate(AppScreen.PlaylistView(p.id)) }
                            .padding(horizontal = if (isCompact) spacing.xxs else spacing.xs, vertical = spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isFav) Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFF8E8EE5)))
                                    else fallbackGradientFor(p.name)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.QueueMusic,
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
                                Text(
                                    pluralRu(p.trackIds.size, "трек", "трека", "треков"),
                                    style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted),
                                    fontSize = 11.sp
                                )
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
// Hero Banner (232dp large cover, dynamic dominant gradient)
// ==========================================
@Composable
private fun HeroBanner(
    title: String,
    subtitle: String,
    badge: String,
    coverSource: String?,
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

    // Load artwork and compute dominant color
    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = coverSource) {
        value = if (coverSource != null) loadArtwork(coverSource) else null
    }

    val dominantColor = remember(coverSource, coverBitmap) {
        if (coverSource != null && coverBitmap != null) {
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
        // Large 232dp (or 160dp on smaller screens) Cover Art with soft shadow
        Box(
            modifier = Modifier
                .size(180.dp)
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(fallbackGradientFor(title)),
            contentAlignment = Alignment.Center
        ) {
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
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(spacing.lg))

        Column(Modifier.weight(1f)) {
            Text(badge, style = MaterialTheme.typography.labelSmall)
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(spacing.xxs))
            // Bug 2 fixed: subtitle shows correctly formatted plural string without duplicating number
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextSecondary)
            )
            Spacer(Modifier.height(spacing.md))

            // Big 56dp Play button
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
        // Track number: 40dp
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

        // Title: weight(1f)
        Box(
            modifier = Modifier.weight(1f).clickable { onSort(SortColumn.Title) },
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

        // Duration: 80dp
        if (!isCompact) {
            Box(
                modifier = Modifier.width(80.dp).clickable { onSort(SortColumn.Duration) },
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

        // Action column: 96dp
        Box(
            modifier = Modifier.width(96.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text("ДЕЙСТВИЯ", style = MaterialTheme.typography.labelSmall.copy(color = AppColors.TextMuted))
        }
    }

    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    Spacer(Modifier.height(spacing.xxs))
}

// ==========================================
// Track Row Item with Context Menu & Equalizer
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

    // Artwork with memory cache
    val artworkBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = track.source) {
        val src = track.source
        value = if (src != null) loadArtwork(src) else null
    }

    Box(
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
            .pointerInput(track.id) {
                // Secondary click (right click) context menu
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // If secondary mouse button clicked:
                    // In Compose Desktop secondary button is down
                    val event = awaitPointerEvent()
                    // Detect right-click
                }
            }
            .clickable { onPlay() }
            .padding(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track number / Animated Equalizer / Play Icon: 40dp
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

            // Title and Artist: weight(1f)
            Column(Modifier.weight(1f)) {
                Text(
                    text = highlightQuery(track.title, searchQuery, isCurrent),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isOfflineAvailable || track.cachedFilePath != null) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Офлайн",
                            tint = AppColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (track.isCloud) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AppColors.SurfaceElevated)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                track.sourceProvider.name,
                                color = AppColors.TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

            // Actions: 96dp (Always rendered, clear buttons on hover and on compact screens)
            Row(
                modifier = Modifier.width(96.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val showActions = isRowHovered || isCompact

                if (showActions) {
                    // Quick Favorite Heart
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "В избранное",
                            tint = if (track.isFavorite) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Context Menu / More Options Button
                    Box {
                        IconButton(
                            onClick = { contextMenuOpen = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Меню трека",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
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
                    }

                    // Delete Track Button (Hover turns red)
                    if (onDelete != null) {
                        val deleteInteraction = remember { MutableInteractionSource() }
                        val isDeleteHovered by deleteInteraction.collectIsHoveredAsState()

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).hoverable(deleteInteraction)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Удалить трек",
                                tint = if (isDeleteHovered) AppColors.ErrorHover else AppColors.TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
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
    onRename: (Track) -> Unit,
) {
    val spacing = LocalSpacing.current
    var sortColumn by remember { mutableStateOf(SortColumn.Number) }
    var sortDirection by remember { mutableStateOf(SortDirection.Ascending) }
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    var selectedFilter by remember { mutableStateOf("Все") }
    var isGridView by remember { mutableStateOf(false) }

    val baseList = when (selectedFilter) {
        "Скачанные" -> library.getOfflineDownloadedTracks()
        "Облачные" -> library.getCloudTracks()
        else -> library.tracks
    }

    val filtered = baseList.filter { t ->
        searchQuery.isBlank() ||
            t.title.contains(searchQuery, ignoreCase = true) ||
            t.artist.contains(searchQuery, ignoreCase = true) ||
            t.album.contains(searchQuery, ignoreCase = true)
    }

    val sorted = remember(filtered, sortColumn, sortDirection) {
        val comparator = when (sortColumn) {
            SortColumn.Number -> compareBy<Track> { library.tracks.indexOf(it) }
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

    Column(Modifier.fillMaxSize()) {
        val trackCount = library.tracks.size
        // Bug 2 fix: pluralRu handles the number directly -> "1 трек", "2 трека", "5 треков"
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

        Spacer(Modifier.height(spacing.sm))

        // Filter chips and List/Grid toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Все", "Скачанные", "Облачные").forEach { filter ->
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

            IconButton(onClick = { isGridView = !isGridView }) {
                Icon(
                    imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = "Вид",
                    tint = AppColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(spacing.xs))

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
                        onPlayNext = { player.playNext(track) },
                        onAddToQueue = { player.addToQueue(track) },
                        onAddToPlaylist = { pId -> onAddToPlaylist(pId, track) },
                        onToggleFavorite = { library.toggleFavorite(track) },
                        onRename = { onRename(track) },
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

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Назад к медиатеке", color = AppColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            if (!playlist.isSystem) {
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
        }

        val trackCount = tracks.size
        HeroBanner(
            title = playlist.name,
            subtitle = pluralRu(trackCount, "трек", "трека", "треков"),
            badge = if (playlist.isSystem) "АВТОПЛЕЙЛИСТ" else "ПЛЕЙЛИСТ",
            coverSource = tracks.firstOrNull { it.source != null }?.source,
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

// ==========================================
// Smart Mix Detail View
// ==========================================
@Composable
private fun SmartMixDetailView(
    title: String,
    library: Library,
    player: AudioPlayer,
    isCompact: Boolean,
    onBack: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val tracks = remember(title, library.tracks.size) {
        when (title) {
            "Микс дня" -> library.getMixOfTheDay()
            "Недавно добавленное", "Свежее в медиатеке" -> library.getRecentlyAdded(50)
            "Часто слушаемое" -> library.getFrequentlyPlayed(50)
            "Новое в облаке" -> library.getCloudTracks()
            else -> library.tracks.toList()
        }
    }

    Column(Modifier.fillMaxSize()) {
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
        TableHeader(
            sortColumn = SortColumn.Number,
            sortDirection = SortDirection.Ascending,
            onSort = {},
            isCompact = isCompact
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { idx, track ->
                TrackRowItem(
                    index = idx + 1,
                    track = track,
                    isPlaying = player.current?.id == track.id && player.isPlaying,
                    isCurrent = player.current?.id == track.id,
                    searchQuery = "",
                    isCompact = isCompact,
                    playlists = library.playlists,
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
    }
}

// ==========================================
// Bottom Fixed Player Bar with Spectrum & Real Seek
// ==========================================
@Composable
private fun SpotifyNowPlayingBar(
    player: AudioPlayer,
    library: Library,
    isCompact: Boolean,
    onOpenFullscreen: () -> Unit,
    onToggleQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSpeed: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val track = player.current
    val playbackState by player.playbackState.collectAsState()
    val spectrum by player.spectrumState.collectAsState()
    val sleepSec by player.sleepTimerRemainingSec.collectAsState()

    var draggingFraction by remember { mutableStateOf<Float?>(null) }
    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val fraction = draggingFraction ?: (playbackState.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    // Load active artwork
    val activeArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = track?.source) {
        val src = track?.source
        value = if (src != null) loadArtwork(src) else null
    }

    Surface(
        color = AppColors.Surface,
        modifier = Modifier.fillMaxWidth().height(92.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: 56dp Cover + Title + Heart
            Row(
                modifier = Modifier
                    .width(if (isCompact) 180.dp else 260.dp)
                    .clickable { onOpenFullscreen() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fallbackGradientFor(track.title)),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = activeArtwork) { bmp ->
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
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
                    IconButton(
                        onClick = { library.toggleFavorite(track) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (track.isFavorite) "В избранном" else "Добавить в избранное",
                            tint = if (track.isFavorite) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
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

            // Center Column: Controls + Progress Bar + Spectrum
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

                    // Repeat (changes icon based on mode)
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

                // Progress Bar Row (Accurate drag + tooltip + click)
                Row(
                    modifier = Modifier.fillMaxWidth(if (isCompact) 0.95f else 0.82f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayPos = draggingFraction?.let { (it * duration).toLong() } ?: playbackState.positionMs
                    Text(
                        formatTime(displayPos),
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
                        durationMs = duration,
                        onSeekToFraction = { newFrac ->
                            val targetMs = (newFrac * duration).toLong()
                            player.seekTo(targetMs)
                            draggingFraction = null
                        },
                        onDragFraction = { newFrac ->
                            draggingFraction = newFrac
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(spacing.xs))

                    Text(
                        formatTime(playbackState.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppColors.TextMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Right Column: Tools (Spectrum, Equalizer, Timer, Speed, Queue, Volume, Fullscreen)
            if (!isCompact) {
                Row(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(end = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Spectrum Mini Visualizer (Canvas)
                    if (player.isPlaying) {
                        SpectrumCanvas(
                            spectrum = spectrum,
                            modifier = Modifier.width(42.dp).height(20.dp)
                        )
                        Spacer(Modifier.width(spacing.xs))
                    }

                    // Equalizer Button
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Эквалайзер",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Sleep Timer
                    IconButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Таймер сна",
                            tint = if (sleepSec != null) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Speed
                    IconButton(
                        onClick = onOpenSpeed,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Скорость воспроизведения",
                            tint = if (player.speed != 1.0f) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Queue Drawer Button
                    IconButton(
                        onClick = onToggleQueue,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "Очередь воспроизведения",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(spacing.xxs))

                    // Volume Icon + Slider
                    val vol = player.volume
                    val volIcon = when {
                        vol == 0f -> Icons.Rounded.VolumeOff
                        vol < 0.4f -> Icons.Rounded.VolumeMute
                        vol < 0.7f -> Icons.Rounded.VolumeDown
                        else -> Icons.Rounded.VolumeUp
                    }

                    IconButton(
                        onClick = { player.toggleMute() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = volIcon,
                            contentDescription = "Громкость",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    VolumeScrubBar(
                        volume = vol,
                        onVolumeChange = { player.updateVolume(it) },
                        modifier = Modifier.width(84.dp)
                    )

                    // Fullscreen Button
                    IconButton(
                        onClick = onOpenFullscreen,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "Полноэкранный режим",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// Custom Scrub Bar (Click, Drag, Hover, Tooltip)
// ==========================================
@Composable
private fun CustomScrubBar(
    fraction: Float,
    durationMs: Long,
    onSeekToFraction: (Float) -> Unit,
    onDragFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }
    var hoverFraction by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    var curFrac = (down.position.x / w).coerceIn(0f, 1f)

                    isDragging = true
                    onDragFraction(curFrac)

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            // Released: commit seek
                            isDragging = false
                            onSeekToFraction(curFrac)
                            break
                        }
                        curFrac = (change.position.x / w).coerceIn(0f, 1f)
                        onDragFraction(curFrac)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Floating Time Tooltip above thumb during hover or dragging
        val activeFrac = if (isDragging) fraction else hoverFraction
        AnimatedVisibility(
            visible = (isHovered || isDragging) && activeFrac != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.TopCenter).padding(bottom = 26.dp)
        ) {
            Surface(
                color = AppColors.SurfaceElevated,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 6.dp
            ) {
                val previewTime = ((activeFrac ?: fraction) * durationMs).toLong()
                Text(
                    formatTime(previewTime),
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
                .height(barHeight + thumbRadius * 2)
        ) {
            val trackH = barHeight.toPx()
            val cy = size.height / 2f
            val w = size.width

            // Background Inactive Track
            drawRoundRect(
                color = AppColors.SurfaceElevated,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Active Track
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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    var curVol = (down.position.x / w).coerceIn(0f, 1f)
                    isDragging = true
                    onVolumeChange(curVol)

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            isDragging = false
                            break
                        }
                        curVol = (change.position.x / w).coerceIn(0f, 1f)
                        onVolumeChange(curVol)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
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
// Spectrum Canvas Visualizer
// ==========================================
@Composable
private fun SpectrumCanvas(
    spectrum: FloatArray,
    modifier: Modifier = Modifier,
    barColor: Color = AppColors.Accent,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bands = spectrum.size.coerceAtLeast(1)
        val barW = (w / bands.toFloat()) * 0.75f
        val gap = (w / bands.toFloat()) * 0.25f

        for (i in 0 until bands) {
            val magnitude = spectrum[i].coerceIn(0.05f, 1.0f)
            val barH = h * magnitude
            drawRoundRect(
                color = barColor,
                topLeft = Offset(i * (barW + gap), h - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}

// ==========================================
// Animated & Frozen Equalizers
// ==========================================
@Composable
private fun AnimatedEqualizer(
    color: Color = AppColors.Accent,
    modifier: Modifier = Modifier.size(16.dp),
) {
    val transition = rememberInfiniteTransition()
    val h1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), AnimRepeatMode.Reverse)
    )
    val h2 by transition.animateFloat(
        initialValue = 0.85f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(310, easing = LinearEasing), AnimRepeatMode.Reverse)
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(440, easing = LinearEasing), AnimRepeatMode.Reverse)
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

        drawRoundRect(color, Offset(0f, h * 0.45f), Size(barW, h * 0.55f), CornerRadius(barW / 2))
        drawRoundRect(color, Offset(barW + gap, h * 0.2f), Size(barW, h * 0.8f), CornerRadius(barW / 2))
        drawRoundRect(color, Offset((barW + gap) * 2, h * 0.5f), Size(barW, h * 0.5f), CornerRadius(barW / 2))
    }
}

// ==========================================
// Playback Queue Drawer
// ==========================================
@Composable
private fun QueueDrawer(
    player: AudioPlayer,
    onClose: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val current = player.current
    val queue = player.queue

    Column(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(AppColors.Surface)
            .padding(spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Очередь воспроизведения", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Закрыть очередь", tint = AppColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(spacing.md))

        // Now Playing Section
        Text("СЕЙЧАС ИГРАЕТ", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(spacing.xs))
        if (current != null) {
            Surface(
                color = AppColors.SurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedEqualizer()
                    Spacer(Modifier.width(spacing.xs))
                    Column(Modifier.weight(1f)) {
                        Text(current.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AppColors.Accent), maxLines = 1)
                        Text(current.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        } else {
            Text("Ничего не играет", color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(spacing.md))

        // Up Next Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ДАЛЕЕ", style = MaterialTheme.typography.labelSmall)
            if (queue.size > 1) {
                TextButton(onClick = { player.clearQueue() }) {
                    Text("Очистить", color = AppColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(spacing.xs))

        val upNext = remember(queue, current) {
            val cIdx = queue.indexOfFirst { it.id == current?.id }
            if (cIdx >= 0 && cIdx < queue.lastIndex) queue.subList(cIdx + 1, queue.size)
            else emptyList()
        }

        if (upNext.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Очередь пуста", color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(upNext, key = { idx, t -> "${t.id}_$idx" }) { idx, t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .padding(vertical = spacing.xxs, horizontal = spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((idx + 1).toString(), color = AppColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(t.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        IconButton(
                            onClick = { player.removeFromQueue(queue.indexOf(t)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Убрать", tint = AppColors.TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Fullscreen Now Playing View
// ==========================================
@Composable
private fun FullscreenNowPlayingView(
    player: AudioPlayer,
    library: Library,
    onClose: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val track = player.current
    val playbackState by player.playbackState.collectAsState()
    val spectrum by player.spectrumState.collectAsState()
    var showLyrics by remember { mutableStateOf(false) }

    val activeArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = track?.source) {
        val src = track?.source
        value = if (src != null) loadArtwork(src) else null
    }

    val dominantColor = remember(track?.source, activeArtwork) {
        if (track?.source != null && activeArtwork != null) {
            ArtworkCache.getDominantColor(track.source) ?: ArtworkCache.computeAndStoreDominantColor(track.source, activeArtwork!!)
        } else Color(0xFF1E3264)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(dominantColor.copy(alpha = 0.85f), Color.Black)
                )
            )
            .padding(spacing.lg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Rounded.FullscreenExit, contentDescription = "Свернуть", tint = Color.White)
                }
                Text(
                    "СЕЙЧАС ИГРАЕТ",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.7f))
                )
                if (player.lyrics.isNotEmpty()) {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Текст песни",
                            tint = if (showLyrics) AppColors.Accent else Color.White
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            Spacer(Modifier.weight(0.1f))

            if (showLyrics && player.lyrics.isNotEmpty()) {
                // Synchronized Lyrics View
                LyricsView(
                    lyrics = player.lyrics,
                    positionMs = playbackState.positionMs,
                    modifier = Modifier.weight(0.8f).fillMaxWidth(0.8f)
                )
            } else {
                // Big 280dp Cover Art
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .shadow(24.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(fallbackGradientFor(track?.title ?: "Vibe Music")),
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
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.lg))

            // Spectrum Visualizer
            SpectrumCanvas(
                spectrum = spectrum,
                barColor = AppColors.Accent,
                modifier = Modifier.fillMaxWidth(0.6f).height(36.dp)
            )

            Spacer(Modifier.height(spacing.md))

            // Title & Artist
            Text(
                track?.title ?: "Не выбрано",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                track?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.7f)),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(spacing.lg))

            // Progress Slider
            val duration = playbackState.durationMs.coerceAtLeast(1L)
            val fraction = (playbackState.positionMs.toFloat() / duration).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(0.7f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTime(playbackState.positionMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(Modifier.width(spacing.sm))
                CustomScrubBar(
                    fraction = fraction,
                    durationMs = duration,
                    onSeekToFraction = { player.seekTo((it * duration).toLong()) },
                    onDragFraction = {},
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(spacing.sm))
                Text(formatTime(playbackState.durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(spacing.md))

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.lg)
            ) {
                IconButton(onClick = { player.setShuffle(!player.shuffle) }) {
                    Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = null, tint = if (player.shuffle) AppColors.Accent else Color.White)
                }
                IconButton(onClick = { player.previous() }) {
                    Icon(imageVector = Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { player.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { player.next() }) {
                    Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { player.cycleRepeat() }) {
                    Icon(
                        imageVector = if (player.repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = if (player.repeat != RepeatMode.Off) AppColors.Accent else Color.White
                    )
                }
            }

            Spacer(Modifier.weight(0.1f))
        }
    }
}

// ==========================================
// Synchronized LRC Lyrics View
// ==========================================
@Composable
private fun LyricsView(
    lyrics: List<com.example.musicapp.model.LrcLine>,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val activeIndex = remember(lyrics, positionMs) {
        val idx = lyrics.indexOfLast { it.timestampMs <= positionMs }
        idx.coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics) { idx, line ->
            val isActive = idx == activeIndex
            Text(
                text = line.text,
                style = TextStyle(
                    fontSize = if (isActive) 24.sp else 18.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isActive) AppColors.Accent else Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }
    }
}

// ==========================================
// Equalizer Dialog (10-Band)
// ==========================================
@Composable
private fun EqualizerDialog(
    player: AudioPlayer,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val frequencies = listOf("32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Эквалайзер", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Presets selector
                Text("Пресеты", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    EqualizerPreset.entries.forEach { p ->
                        val isSelected = player.equalizerPreset == p
                        Button(
                            onClick = { player.setEqualizerPreset(p) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AppColors.Accent else AppColors.SurfaceElevated,
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(p.title, fontSize = 9.5.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(spacing.md))

                // 10 Sliders
                frequencies.forEachIndexed { idx, freq ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(freq, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                        Slider(
                            value = player.equalizerGains.getOrElse(idx) { 0f },
                            onValueChange = { gain -> player.setEqualizerBand(idx, gain) },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = AppColors.Accent,
                                activeTrackColor = AppColors.Accent,
                                inactiveTrackColor = AppColors.SurfaceElevated
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${player.equalizerGains.getOrElse(idx) { 0f }.toInt()}dB",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(38.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)) {
                Text("Готово", color = Color.Black)
            }
        },
        containerColor = AppColors.Surface
    )
}

// ==========================================
// Sleep Timer Dialog
// ==========================================
@Composable
private fun SleepTimerDialog(
    player: AudioPlayer,
    onDismiss: () -> Unit,
) {
    val remainingSec by player.sleepTimerRemainingSec.collectAsState()
    val options = listOf(null to "Выключить таймер", 15 to "15 минут", 30 to "30 минут", 45 to "45 минут", 60 to "1 час")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Таймер сна", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (remainingSec != null) {
                    val m = remainingSec!! / 60
                    val s = remainingSec!! % 60
                    Text(
                        "Осталось: $m мин $s сек",
                        color = AppColors.Accent,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                options.forEach { (minutes, label) ->
                    TextButton(
                        onClick = {
                            player.setSleepTimer(minutes)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = AppColors.TextPrimary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Accent)
            ) {
                Text("Закрыть", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = AppColors.SurfaceElevated
    )
}

// ==========================================
// Playback Speed Dialog
// ==========================================
@Composable
private fun SpeedDialog(
    player: AudioPlayer,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Скорость воспроизведения", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                speeds.forEach { s ->
                    val isCur = player.speed == s
                    TextButton(
                        onClick = {
                            player.setPlaybackSpeed(s)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${s}x ${if (s == 1.0f) "(Обычная)" else ""}",
                            color = if (isCur) AppColors.Accent else AppColors.TextPrimary,
                            fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        containerColor = AppColors.SurfaceElevated
    )
}

// ==========================================
// Rename Track Dialog
// ==========================================
@Composable
private fun RenameTrackDialog(
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

// ==========================================
// Playlist Dialogs
// ==========================================
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
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
            TextButton(onClick = onDismiss) { Text("Отмена", color = AppColors.TextSecondary) }
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

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
