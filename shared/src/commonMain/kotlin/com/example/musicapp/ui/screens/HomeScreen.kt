package com.example.musicapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.FAVORITES_PLAYLIST_ID
import com.example.musicapp.library.Library
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.player.PlaybackContext
import com.example.musicapp.player.PlaybackContextType
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.ui.components.FavoritesCoverBox
import com.example.musicapp.ui.components.ContentCard
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    library: Library,
    player: AudioPlayer,
    onNavigatePlaylist: (String) -> Unit,
    onNavigateSmartMix: (String) -> Unit,
    onNavigateCloud: () -> Unit,
    isCompact: Boolean = false,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }

    val jamendoKey by com.example.musicapp.config.AppConfig.jamendoClientIdFlow.collectAsState()
    val hasJamendoKey = jamendoKey.isNotBlank()

    var jamendoPopular by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var audiusTrending by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var isLoadingJamendo by remember { mutableStateOf(hasJamendoKey) }
    var isLoadingAudius by remember { mutableStateOf(true) }

    val isOnline = com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled
    LaunchedEffect(hasJamendoKey, isOnline) {
        if (!isOnline) {
            isLoadingJamendo = false
            isLoadingAudius = false
            return@LaunchedEffect
        }
        if (hasJamendoKey) {
            launch {
                try {
                    jamendoPopular = player.providerRegistry.jamendo?.listTracks()?.items ?: emptyList()
                } catch (_: Throwable) {
                } finally {
                    isLoadingJamendo = false
                }
            }
        } else {
            isLoadingJamendo = false
        }
        launch {
            try {
                audiusTrending = player.providerRegistry.audius?.listTracks()?.items ?: emptyList()
            } catch (_: Throwable) {
            } finally {
                isLoadingAudius = false
            }
        }
    }

    val favoritesPlaylist = library.playlists.find { it.id == FAVORITES_PLAYLIST_ID }
    val favoriteTracks = favoritesPlaylist?.let { library.tracksIn(it) } ?: emptyList()
    val mixOfDay = remember(library.visibleTracks.size) { library.getMixOfTheDay() }
    val recentlyAdded = remember(library.visibleTracks.size) { library.getRecentlyAdded(12) }
    val frequentlyPlayed = remember(library.visibleTracks.size) { library.getFrequentlyPlayed(12) }
    val cloudTracks = remember(library.visibleTracks.size) { library.getCloudTracks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Onboarding card for new/empty library
        if (library.visibleTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F3B2E), Color(0xFF1E2838))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "Добро пожаловать в JVXMusic!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isOnline) "Ваша медиатека пока пуста. Начните с добавления локальных файлов или подключите облако для потокового воспроизведения."
                        else "Ваша медиатека пока пуста. Начните с добавления локальных музыкальных файлов или выберите папку с музыкой на вашем компьютере.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                com.example.musicapp.platform.launchAudioImport { tr -> library.addTrack(tr) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("Добавить файлы", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                com.example.musicapp.platform.launchFolderImport { trs -> library.addTracks(trs) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Импорт папки", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (isOnline) {
                            TextButton(
                                onClick = onNavigateCloud
                            ) {
                                Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = AppColors.Accent)
                                Spacer(Modifier.width(4.dp))
                                Text("Подключить облако", color = AppColors.Accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Time-of-day Greeting
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary
            )
        )

        Spacer(Modifier.height(18.dp))

        // Quick Cards Adaptive Grid (min ~280dp, max 4-5 columns)
        val quickCards = buildList {
            add(QuickCardData("Любимые треки", Icons.Rounded.Favorite, Color(0xFF0F5A3E), favoriteTracks))
            add(QuickCardData("Микс дня", Icons.Rounded.Shuffle, Color(0xFF1E3264), mixOfDay))
            add(QuickCardData("Свежее в медиатеке", Icons.Rounded.Explore, Color(0xFF148A3C), recentlyAdded))
            add(QuickCardData("Часто слушаемое", Icons.Rounded.PlayArrow, Color(0xFF8D67AB), frequentlyPlayed))
            if (isOnline) {
                add(QuickCardData("Новое в облаке", Icons.Rounded.CloudQueue, Color(0xFFE91429), cloudTracks))
                if (hasJamendoKey && jamendoPopular.isNotEmpty()) {
                    add(QuickCardData("Jamendo Тренды", Icons.Rounded.CloudDone, Color(0xFFBA5D07), jamendoPopular.map { it.toTrack() }))
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val totalCards = quickCards.size
            val maxColsPossible = (maxWidth / 220.dp).toInt().coerceIn(2, 6)
            val columns = when {
                totalCards <= 2 -> 2
                totalCards <= maxColsPossible -> totalCards
                totalCards == 4 && maxColsPossible in 2..3 -> 2
                totalCards == 4 && maxColsPossible >= 4 -> 4
                else -> (maxColsPossible downTo 2).firstOrNull { totalCards % it == 0 } ?: maxColsPossible
            }
            val chunks = quickCards.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunks.forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCards.forEach { card ->
                            val (cardType, cardId) = if (card.title == "Любимые треки") {
                                PlaybackContextType.Playlist to FAVORITES_PLAYLIST_ID
                            } else {
                                PlaybackContextType.SmartMix to card.title
                            }
                            val isCardPlaying = player.isContextPlaying(cardType, cardId)
                            QuickGridCard(
                                data = card,
                                isPlaying = isCardPlaying,
                                modifier = Modifier.weight(1f),
                                onPlay = {
                                    if (isCardPlaying) {
                                        player.pause()
                                    } else if (player.playbackContext.value?.type == cardType && player.playbackContext.value?.id == cardId && player.current != null) {
                                        player.resume()
                                    } else if (card.tracks.isNotEmpty()) {
                                        val ctx = PlaybackContext(cardType, cardId, card.title)
                                        player.play(card.tracks.first(), card.tracks, ctx)
                                    }
                                },
                                onClick = {
                                    when (card.title) {
                                        "Любимые треки" -> onNavigatePlaylist(FAVORITES_PLAYLIST_ID)
                                        "Новое в облаке" -> onNavigateCloud()
                                        else -> onNavigateSmartMix(card.title)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Shelf 1: "Недавно слушали"
        val recentlyPlayed = library.recentlyPlayedIds.mapNotNull { id -> library.visibleTracks.find { it.id == id } }
        if (recentlyPlayed.isNotEmpty()) {
            ContentShelf(
                title = "Недавно слушали",
                tracks = recentlyPlayed,
                player = player,
                onTrackClick = { t ->
                    val ctx = PlaybackContext(PlaybackContextType.TrackShelf, "Недавно слушали", "Недавно слушали")
                    player.play(t, recentlyPlayed, ctx)
                }
            )
            Spacer(Modifier.height(28.dp))
        }

        // Shelf 2: "Сделано для вас" (shown only when library has at least 5 tracks)
        if (library.visibleTracks.size >= 5 && (mixOfDay.isNotEmpty() || recentlyAdded.isNotEmpty() || frequentlyPlayed.isNotEmpty())) {
            MixesShelf(
                mixOfDay = mixOfDay,
                recentTracks = recentlyAdded,
                frequentTracks = frequentlyPlayed,
                player = player,
                onSelectMix = { onNavigateSmartMix(it) }
            )
            Spacer(Modifier.height(28.dp))
        }

        if (isOnline) {
            // Shelf 3: "Новое в облаке"
            if (cloudTracks.isNotEmpty()) {
                ContentShelf(
                    title = "Синхронизировано из облака",
                    tracks = cloudTracks,
                    player = player,
                    onTrackClick = { t -> player.play(t, cloudTracks) }
                )
                Spacer(Modifier.height(28.dp))
            }

            // Shelf 4: "Популярное в Jamendo (Creative Commons)" - shown only when client key is configured
            if (hasJamendoKey) {
                if (isLoadingJamendo) {
                    ShelfSkeleton(title = "Популярное в Jamendo (Лицензия CC)")
                    Spacer(Modifier.height(28.dp))
                } else if (jamendoPopular.isNotEmpty()) {
                    val converted = jamendoPopular.map { it.toTrack() }
                    ContentShelf(
                        title = "Популярное в Jamendo (Лицензия CC)",
                        tracks = converted,
                        player = player,
                        badge = "JAMENDO",
                        onTrackClick = { t -> player.play(t, converted) }
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            // Shelf 5: "Тренды Audius"
            if (isLoadingAudius) {
                ShelfSkeleton(title = "Открытая музыка Audius")
            } else if (audiusTrending.isNotEmpty()) {
                val converted = audiusTrending.map { it.toTrack() }
                ContentShelf(
                    title = "Открытая музыка Audius",
                    tracks = converted,
                    player = player,
                    badge = "AUDIUS",
                    onTrackClick = { t -> player.play(t, converted) }
                )
            }
        }
    }
}

@Composable
private fun ShimmerCardSkeleton() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceElevated)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = alpha * 0.7f))
        )
    }
}

@Composable
private fun ShelfSkeleton(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(5) {
                ShimmerCardSkeleton()
            }
        }
    }
}

private data class QuickCardData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val tracks: List<Track>,
)

@Composable
private fun QuickGridCard(
    data: QuickCardData,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .hoverable(interactionSource)
            .background(if (isHovered) AppColors.CardHover else AppColors.Surface)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (data.title == "Любимые треки") {
                FavoritesCoverBox(
                    modifier = Modifier.size(56.dp),
                    iconSize = 24.dp,
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Brush.linearGradient(listOf(data.color, data.color.copy(alpha = 0.6f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = data.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) AppColors.Accent else AppColors.TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Play button appears on hover (fade + slide 200ms) or is always visible if playing
            AnimatedVisibility(
                visible = isHovered || isPlaying,
                enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { 20 },
                exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { 20 },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(AppColors.Accent)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Слушать",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContentShelf(
    title: String,
    tracks: List<Track>,
    player: AudioPlayer,
    badge: String? = null,
    onTrackClick: (Track) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val canScrollBackward by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    val canScrollForward by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible < tracks.lastIndex
        }
    }
    val showArrows = tracks.size > 4 && (canScrollBackward || canScrollForward)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            )
            if (showArrows) {
                Row {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val target = (listState.firstVisibleItemIndex - 3).coerceAtLeast(0)
                                listState.animateScrollToItem(target)
                            }
                        },
                        enabled = canScrollBackward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Назад",
                            tint = if (canScrollBackward) AppColors.TextSecondary else AppColors.TextMuted
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val target = (listState.firstVisibleItemIndex + 3).coerceAtMost(tracks.lastIndex)
                                listState.animateScrollToItem(target)
                            }
                        },
                        enabled = canScrollForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Вперед",
                            tint = if (canScrollForward) AppColors.TextSecondary else AppColors.TextMuted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                val isPlayingThis = player.isContextPlaying(PlaybackContextType.TrackShelf, title) && player.current?.id == track.id
                ContentCard(
                    title = track.title,
                    subtitle = track.artist,
                    badgeText = badge ?: if (track.isCloud) track.sourceProvider.name else null,
                    track = track,
                    isPlaying = isPlayingThis,
                    onClick = { onTrackClick(track) },
                    onPlayClick = {
                        if (isPlayingThis) {
                            player.pause()
                        } else if (player.playbackContext.value?.type == PlaybackContextType.TrackShelf && player.playbackContext.value?.id == title && player.current?.id == track.id) {
                            player.resume()
                        } else {
                            val ctx = PlaybackContext(PlaybackContextType.TrackShelf, title, title)
                            player.play(track, tracks, ctx)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MixesShelf(
    mixOfDay: List<Track>,
    recentTracks: List<Track>,
    frequentTracks: List<Track>,
    player: AudioPlayer,
    onSelectMix: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Сделано для вас",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isMixDayPlaying = player.isContextPlaying(PlaybackContextType.SmartMix, "Микс дня")
            ContentCard(
                title = "Микс дня",
                subtitle = "Случайная подборка треков из вашей медиатеки на каждый день",
                cardWidth = 190.dp,
                isPlaying = isMixDayPlaying,
                onClick = { onSelectMix("Микс дня") },
                onPlayClick = {
                    if (isMixDayPlaying) {
                        player.pause()
                    } else if (player.playbackContext.value?.type == PlaybackContextType.SmartMix && player.playbackContext.value?.id == "Микс дня" && player.current != null) {
                        player.resume()
                    } else if (mixOfDay.isNotEmpty()) {
                        val ctx = PlaybackContext(PlaybackContextType.SmartMix, "Микс дня", "Микс дня")
                        player.play(mixOfDay.first(), mixOfDay, ctx)
                    }
                }
            )

            val isRecentPlaying = player.isContextPlaying(PlaybackContextType.SmartMix, "Недавно добавленное")
            ContentCard(
                title = "Недавно добавленное",
                subtitle = "Свежие треки, добавленные в вашу библиотеку",
                cardWidth = 190.dp,
                isPlaying = isRecentPlaying,
                onClick = { onSelectMix("Недавно добавленное") },
                onPlayClick = {
                    if (isRecentPlaying) {
                        player.pause()
                    } else if (player.playbackContext.value?.type == PlaybackContextType.SmartMix && player.playbackContext.value?.id == "Недавно добавленное" && player.current != null) {
                        player.resume()
                    } else if (recentTracks.isNotEmpty()) {
                        val ctx = PlaybackContext(PlaybackContextType.SmartMix, "Недавно добавленное", "Недавно добавленное")
                        player.play(recentTracks.first(), recentTracks, ctx)
                    }
                }
            )

            val isFrequentPlaying = player.isContextPlaying(PlaybackContextType.SmartMix, "Часто слушаемое")
            ContentCard(
                title = "Часто слушаемое",
                subtitle = "Треки с наибольшим числом воспроизведений",
                cardWidth = 190.dp,
                isPlaying = isFrequentPlaying,
                onClick = { onSelectMix("Часто слушаемое") },
                onPlayClick = {
                    if (isFrequentPlaying) {
                        player.pause()
                    } else if (player.playbackContext.value?.type == PlaybackContextType.SmartMix && player.playbackContext.value?.id == "Часто слушаемое" && player.current != null) {
                        player.resume()
                    } else if (frequentTracks.isNotEmpty()) {
                        val ctx = PlaybackContext(PlaybackContextType.SmartMix, "Часто слушаемое", "Часто слушаемое")
                        player.play(frequentTracks.first(), frequentTracks, ctx)
                    }
                }
            )
        }
    }
}
