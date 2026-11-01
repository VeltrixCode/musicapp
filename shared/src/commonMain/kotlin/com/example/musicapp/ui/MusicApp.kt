package com.example.musicapp.ui

import com.example.musicapp.util.formatTime
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.derivedStateOf
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.settings.SettingsRepository
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.DriveFolderUpload
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SearchOff
import com.example.musicapp.ui.components.FavoritesCoverBox
import com.example.musicapp.ui.components.ContentCard
import com.example.musicapp.ui.components.RightNowPlayingDrawer
import com.example.musicapp.ui.components.TrackActionMenu
import com.example.musicapp.service.CoverArtService
import com.example.musicapp.util.formatTrackDuration
import com.example.musicapp.util.getEffectiveDurationMs
import com.example.musicapp.ui.screens.CloudSourcesScreen
import com.example.musicapp.ui.screens.HomeScreen
import com.example.musicapp.ui.screens.SearchScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.MoreHoriz
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
import androidx.compose.material.icons.rounded.Warning
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
    object Albums : AppScreen()
    object Artists : AppScreen()
    object CloudSources : AppScreen()
    data class PlaylistView(val playlistId: String) : AppScreen()
    data class SmartMixView(val title: String) : AppScreen()
}

/**
 * Root JVXMusic Composable.
 * Features:
 * - JVX Dark Theme & Signature Emerald Accent (#00D26A)
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
            (player as? PlayerController)?.dismissAllErrors()
            backStack.add(screen)
            forwardStack.clear()
        }
    }

    fun goBack() {
        if (backStack.size > 1) {
            (player as? PlayerController)?.dismissAllErrors()
            val curr = backStack.removeLast()
            forwardStack.add(curr)
        }
    }

    fun goForward() {
        if (forwardStack.isNotEmpty()) {
            (player as? PlayerController)?.dismissAllErrors()
            val next = forwardStack.removeLast()
            backStack.add(next)
        }
    }

    val currentScreen = backStack.lastOrNull() ?: AppScreen.Home

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    val settingsRepo = remember { (player as? PlayerController)?.settingsRepository ?: SettingsRepository() }
    val initialSettings = remember { settingsRepo.load() }
    var sidebarExpandedWidth by remember { mutableStateOf(initialSettings.sidebarWidthDp.coerceIn(180f, 360f).dp) }
    var isSidebarCollapsed by remember { mutableStateOf(initialSettings.isSidebarCollapsed) }
    var isRightPanelPinned by remember { mutableStateOf(initialSettings.isRightPanelPinned) }
    var showQueueDrawer by remember { mutableStateOf(initialSettings.isRightPanelOpen || initialSettings.isRightPanelPinned) }

    // Dialogs & panels
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
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

    LaunchedEffect(player, library) {
        (player as? PlayerController)?.onTrackDurationResolved = { trackId, resolvedDur ->
            library.updateTrackDuration(trackId, resolvedDur)
        }
    }

    AppTheme {
        val spacing = LocalSpacing.current

        Surface(
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.B && event.type == KeyEventType.KeyDown) {
                        isSidebarCollapsed = !isSidebarCollapsed
                        settingsRepo.save(settingsRepo.load().copy(isSidebarCollapsed = isSidebarCollapsed))
                        true
                    } else {
                        false
                    }
                },
            color = AppColors.Background,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val isMobile = maxWidth < 600.dp
                val isCompact = maxWidth < 900.dp
                val autoCollapse = maxWidth < 900.dp
                val effectiveCollapsed = isSidebarCollapsed || autoCollapse
                val targetSidebarWidth = if (effectiveCollapsed) 72.dp else sidebarExpandedWidth
                val animatedSidebarWidth by animateDpAsState(
                    targetValue = targetSidebarWidth,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
                val showSidebarLabels = !effectiveCollapsed && (animatedSidebarWidth >= sidebarExpandedWidth - 8.dp)

                Column(Modifier.fillMaxSize()) {
                    // Main layout
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Left Sidebar Navigation with Resizable Width (Visible for all non-mobile screens)
                        if (!isMobile) {
                            AppSidebar(
                                library = library,
                                currentScreen = currentScreen,
                                sidebarWidth = animatedSidebarWidth,
                                onWidthChange = {
                                    sidebarExpandedWidth = it
                                    settingsRepo.save(settingsRepo.load().copy(sidebarWidthDp = it.value))
                                },
                                collapsed = effectiveCollapsed,
                                onToggleCollapse = {
                                    isSidebarCollapsed = !isSidebarCollapsed
                                    settingsRepo.save(settingsRepo.load().copy(isSidebarCollapsed = isSidebarCollapsed))
                                },
                                showLabels = showSidebarLabels,
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
                                    .fillMaxSize()
                            ) {
                                // Top Action Bar with History Back/Forward
                                AppTopBar(
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
                                    isSyncing = (player as? PlayerController)?.providerRegistry?.isSyncing?.collectAsState()?.value ?: false,
                                    isOffline = settingsRepo.settings.collectAsState().value.offlineMode,
                                )

                                // Fallback warning banner if VLC is unavailable (shown once per session, dismissible)
                                val fallbackNotice by player.fallbackWarningNotice.collectAsState()
                                fallbackNotice?.let { notice ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = spacing.md, vertical = spacing.xxs)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppColors.Warning.copy(alpha = 0.2f))
                                            .border(1.dp, AppColors.Warning.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(10.dp))
                                                Text(notice, color = AppColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(onClick = { player.dismissFallbackWarning() }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

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
                                            (fadeIn(animationSpec = tween(200, easing = LinearEasing)) +
                                                    slideInHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it / 10 })
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(150, easing = LinearEasing)) +
                                                            slideOutHorizontally(animationSpec = tween(150, easing = FastOutSlowInEasing)) { -it / 10 }
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
                                                    searchQuery = searchQuery,
                                                    onSearchQueryChange = { searchQuery = it },
                                                    onTrackClick = { t -> player.play(t, listOf(t)) },
                                                    onUploadSong = {
                                                        launchAudioImport { track ->
                                                            library.addTrack(track)
                                                            coroutineScope.launch { snackbarHostState.showSnackbar("Добавлен трек: ${track.title}") }
                                                        }
                                                    },
                                                    onUploadFolder = {
                                                        launchFolderImport { tracks ->
                                                            tracks.forEach { library.addTrack(it) }
                                                            coroutineScope.launch { snackbarHostState.showSnackbar("Импортировано треков: ${tracks.size}") }
                                                        }
                                                    },
                                                    onNavigate = { navigateTo(it) },
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
                                                    onRename = { renamingTrack = it },
                                                    onNavigate = { navigateTo(it) },
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
                                                    }
                                                )
                                            }
                                            is AppScreen.Albums -> {
                                                AlbumsGroupView(
                                                    library = library,
                                                    player = player,
                                                    onNavigateToAlbum = { alb -> navigateTo(AppScreen.SmartMixView(alb)) },
                                                    onBack = { goBack() }
                                                )
                                            }
                                            is AppScreen.Artists -> {
                                                ArtistsGroupView(
                                                    library = library,
                                                    player = player,
                                                    onNavigateToArtist = { art -> navigateTo(AppScreen.SmartMixView(art)) },
                                                    onBack = { goBack() }
                                                )
                                            }
                                            is AppScreen.CloudSources -> {
                                                if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
                                                    CloudSourcesScreen(
                                                        library = library,
                                                        player = player,
                                                        onShowMessage = { msg ->
                                                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                                        }
                                                    )
                                                } else {
                                                    HomeScreen(
                                                        library = library,
                                                        player = player,
                                                        onNavigatePlaylist = { pId -> navigateTo(AppScreen.PlaylistView(pId)) },
                                                        onNavigateSmartMix = { mixTitle -> navigateTo(AppScreen.SmartMixView(mixTitle)) },
                                                        onNavigateCloud = { navigateTo(AppScreen.Home) },
                                                        isCompact = isCompact
                                                    )
                                                }
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

                        // Right Drawer ("Сейчас играет" / Очередь / Синхронизированный текст)
                        AnimatedVisibility(
                            visible = showQueueDrawer || isRightPanelPinned,
                            enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it },
                            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it }
                        ) {
                            RightNowPlayingDrawer(
                                player = player,
                                library = library,
                                isPinned = isRightPanelPinned,
                                onTogglePin = {
                                    val nextPinned = !isRightPanelPinned
                                    isRightPanelPinned = nextPinned
                                    if (nextPinned) showQueueDrawer = true
                                    settingsRepo.save(settingsRepo.load().copy(isRightPanelPinned = nextPinned, isRightPanelOpen = showQueueDrawer))
                                },
                                onClose = {
                                    if (!isRightPanelPinned) {
                                        showQueueDrawer = false
                                        settingsRepo.save(settingsRepo.load().copy(isRightPanelOpen = false))
                                    }
                                }
                            )
                        }
                    }

                    // Bottom Fixed Player Bar (Stretches full width)
                    NowPlayingBar(
                        player = player,
                        library = library,
                        isCompact = isCompact,
                        onOpenFullscreen = { isFullscreenNowPlaying = true },
                        onToggleQueue = {
                            if (!isRightPanelPinned) {
                                val next = !showQueueDrawer
                                showQueueDrawer = next
                                settingsRepo.save(settingsRepo.load().copy(isRightPanelOpen = next))
                            }
                        },
                        onOpenEqualizer = { showEqualizerDialog = true },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        onOpenSpeed = { showSpeedDialog = true },
                    )

                    // Bottom Navigation Bar for Mobile devices (<600dp)
                    if (isMobile) {
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
                            if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
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
                }

                // Fullscreen Now Playing Overlay
                if (isFullscreenNowPlaying) {
                    FullscreenNowPlayingView(
                        player = player,
                        library = library,
                        onOpenEqualizer = { showEqualizerDialog = true },
                        onOpenSpeed = { showSpeedDialog = true },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        onClose = { isFullscreenNowPlaying = false }
                    )
                }

                // Floating Error Snackbar above bottom player (no layout jump)
                val currentAppError by player.appError.collectAsState()
                val errorBottomPadding = if (isMobile) (56.dp + 76.dp + 12.dp) else (76.dp + 16.dp)

                AnimatedVisibility(
                    visible = currentAppError != null,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = errorBottomPadding, start = 16.dp, end = 16.dp)
                ) {
                    currentAppError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E1C1C))
                                .border(1.dp, AppColors.Error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(err.message, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (err.actionLabel != null && err.onAction != null) {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                val action = err.onAction
                                                player.dismissError()
                                                action?.invoke()
                                            }
                                        ) {
                                            Text(err.actionLabel!!, color = AppColors.Accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(onClick = { player.dismissError() }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
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
            onCreate = { name, description, coverUrl ->
                library.createPlaylist(name, description, coverUrl)
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
