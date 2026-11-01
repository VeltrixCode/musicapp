package com.example.musicapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import kotlin.math.roundToInt
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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.library.Library
import com.example.musicapp.model.LrcLine
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.player.EqualizerPreset
import com.example.musicapp.player.RepeatMode
import com.example.musicapp.service.CoverArtService
import com.example.musicapp.theme.fallbackGradientFor
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.ui.components.AppTooltip
import com.example.musicapp.util.formatTime

@Composable
internal fun NowPlayingBar(
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
    val hasTrack = track != null
    val playbackState by player.playbackState.collectAsState()
    val spectrum by player.spectrumState.collectAsState()
    val sleepSec by player.sleepTimerRemainingSec.collectAsState()

    var draggingFraction by remember { mutableStateOf<Float?>(null) }
    val effectiveTotalDuration = if (playbackState.durationMs > 0L) playbackState.durationMs else (track?.durationMs ?: 0L)
    val duration = effectiveTotalDuration.coerceAtLeast(1L)
    val fraction = draggingFraction ?: (playbackState.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    // Load active artwork (with online cover search fallback)
    val activeArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = track?.id) {
        value = if (track != null) CoverArtService.loadArtworkForTrack(track) else null
    }

    Surface(
        color = AppColors.Surface,
        modifier = Modifier.fillMaxWidth().height(if (isCompact) 64.dp else 92.dp)
    ) {
        if (isCompact) {
            // Mobile Compact Mini Player Layout
            Box(Modifier.fillMaxSize()) {
                // Top Progress indicator line (2dp)
                val isLiveTrack = track?.isLiveStream == true || track?.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER
                val progFraction = if (hasTrack) (if (isLiveTrack) 1f else fraction) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopStart)
                        .background(AppColors.SurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progFraction)
                            .fillMaxHeight()
                            .background(AppColors.Accent)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Track Thumbnail (44.dp) + Title & Artist (taking weight(1f)) -> Click opens fullscreen
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onOpenFullscreen() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (track != null) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(spacing.xs))
                            Column(Modifier.weight(1f)) {
                                AppTooltip(text = track.title) {
                                    Text(
                                        track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TextPrimary,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    track.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AppColors.TextSecondary,
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppColors.SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = AppColors.TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(spacing.xs))
                            Text(
                                "Ничего не играет",
                                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextMuted, fontSize = 13.sp)
                            )
                        }
                    }

                    // Mobile Controls: Favorite, Play/Pause, Next (All with 48x48dp touch targets)
                    if (track != null) {
                        IconButton(
                            onClick = { library.toggleFavorite(track) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (track.isFavorite) "В избранном" else "Добавить в избранное",
                                tint = if (track.isFavorite) AppColors.Accent else AppColors.TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(enabled = hasTrack && track.source != null) { player.toggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.TextPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (player.isPlaying) "Пауза" else "Воспроизведение",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { player.next() },
                            enabled = hasTrack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Следующий",
                                tint = AppColors.TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else {
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
                        AppTooltip(text = track.title) {
                            Text(
                                track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                    IconButton(
                        onClick = { player.setShuffle(!player.shuffle) },
                        enabled = hasTrack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Перемешать",
                            tint = if (!hasTrack) AppColors.TextMuted else if (player.shuffle) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.previous() },
                        enabled = hasTrack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Предыдущий",
                            tint = if (hasTrack) AppColors.TextPrimary else AppColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (hasTrack) AppColors.TextPrimary else AppColors.SurfaceElevated)
                            .clickable(enabled = hasTrack && track?.source != null) { player.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (player.isPlaying) "Пауза" else "Воспроизведение",
                            tint = if (hasTrack) Color.Black else AppColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.next() },
                        enabled = hasTrack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Следующий",
                            tint = if (hasTrack) AppColors.TextPrimary else AppColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.cycleRepeat() },
                        enabled = hasTrack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (player.repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Повтор",
                            tint = if (!hasTrack) AppColors.TextMuted else if (player.repeat != RepeatMode.Off) AppColors.Accent else AppColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Progress Bar Row (Accurate drag + tooltip + click)
                val isLiveTrack = track?.isLiveStream == true || track?.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER
                Row(
                    modifier = Modifier.fillMaxWidth(if (isCompact) 0.95f else 0.82f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLiveTrack) {
                        Text(
                            "LIVE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppColors.Accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Start
                            ),
                            modifier = Modifier.width(36.dp)
                        )
                    } else {
                        val displayPos = draggingFraction?.let { (it * duration).toLong() } ?: playbackState.positionMs
                        Text(
                            formatTime(if (hasTrack) displayPos else 0L),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppColors.TextMuted,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Start
                            ),
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    Spacer(Modifier.width(spacing.xs))

                    CustomScrubBar(
                        fraction = if (hasTrack) (if (isLiveTrack) 1f else fraction) else 0f,
                        durationMs = duration,
                        enabled = hasTrack && !isLiveTrack,
                        onSeekToFraction = { newFrac ->
                            if (!isLiveTrack) {
                                val targetMs = (newFrac * duration).toLong()
                                player.seekTo(targetMs)
                                draggingFraction = null
                            }
                        },
                        onDragFraction = { newFrac ->
                            if (!isLiveTrack) {
                                draggingFraction = newFrac
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(spacing.xs))

                    if (isLiveTrack) {
                        Text(
                            "LIVE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppColors.Accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.width(36.dp)
                        )
                    } else {
                        Text(
                            formatTime(effectiveTotalDuration),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppColors.TextMuted,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }

            // Right Column: Tools (Queue/NowPlaying, Volume, Fullscreen, More Menu)
            if (!isCompact) {
                var moreMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .width(280.dp)
                        .padding(end = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onToggleQueue,
                        enabled = hasTrack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Очередь и текст",
                            tint = if (hasTrack) AppColors.TextSecondary else AppColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(spacing.xxs))

                    val vol = player.volume
                    val volIcon = when {
                        vol == 0f -> Icons.AutoMirrored.Rounded.VolumeOff
                        vol < 0.4f -> Icons.AutoMirrored.Rounded.VolumeMute
                        vol < 0.7f -> Icons.AutoMirrored.Rounded.VolumeDown
                        else -> Icons.AutoMirrored.Rounded.VolumeUp
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
                        modifier = Modifier.width(88.dp)
                    )

                    Spacer(Modifier.width(spacing.xxs))

                    IconButton(
                        onClick = onOpenFullscreen,
                        enabled = hasTrack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = "Полноэкранный режим",
                            tint = if (hasTrack) AppColors.TextSecondary else AppColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(spacing.xxs))

                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Дополнительно",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false },
                            modifier = Modifier.background(AppColors.SurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Эквалайзер") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = AppColors.Accent)
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenEqualizer()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Таймер сна")
                                        if (sleepSec != null) {
                                            Text("${sleepSec!! / 60} мин", color = AppColors.Accent, fontSize = 12.sp)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = if (sleepSec != null) AppColors.Accent else AppColors.TextSecondary)
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenSleepTimer()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Скорость воспроизведения")
                                        Text("${player.speed}x", color = if (player.speed != 1.0f) AppColors.Accent else AppColors.TextMuted, fontSize = 12.sp)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Speed, contentDescription = null, tint = if (player.speed != 1.0f) AppColors.Accent else AppColors.TextSecondary)
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenSpeed()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Полноэкранный режим") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Fullscreen, contentDescription = null, tint = AppColors.TextSecondary)
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenFullscreen()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
internal fun CustomScrubBar(
    fraction: Float,
    durationMs: Long,
    onSeekToFraction: (Float) -> Unit,
    onDragFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }
    var hoverFraction by remember { mutableStateOf<Float?>(null) }

    val interactiveModifier = if (enabled) {
        Modifier
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
                            isDragging = false
                            onSeekToFraction(curFrac)
                            break
                        }
                        curFrac = (change.position.x / w).coerceIn(0f, 1f)
                        onDragFraction(curFrac)
                        change.consume()
                    }
                }
            }
    } else Modifier

    Box(
        modifier = modifier.then(interactiveModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        val activeFrac = if (isDragging) fraction else hoverFraction
        AnimatedVisibility(
            visible = enabled && (isHovered || isDragging) && activeFrac != null,
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

            drawRoundRect(
                color = AppColors.SurfaceElevated,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            val fillW = (fraction.coerceIn(0f, 1f) * w)
            val fillCol = if (!enabled) AppColors.TextMuted else if (isHovered || isDragging) AppColors.AccentHover else Color.White
            drawRoundRect(
                color = fillCol,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            if (enabled && (isHovered || isDragging)) {
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius.toPx(),
                    center = Offset(fillW, cy)
                )
            }
        }
    }
}

@Composable
internal fun VolumeScrubBar(
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

@Composable
internal fun SpectrumCanvas(
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

@Composable
internal fun AnimatedEqualizer(
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
internal fun FrozenEqualizer(
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

@Composable
internal fun QueueDrawer(
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

@Composable
internal fun FullscreenNowPlayingView(
    player: AudioPlayer,
    library: Library,
    onClose: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val track = player.current
    val playbackState by player.playbackState.collectAsState()
    val spectrum by player.spectrumState.collectAsState()
    var showLyrics by remember { mutableStateOf(false) }

    val activeArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = track?.id) {
        value = if (track != null) CoverArtService.loadArtworkForTrack(track) else null
    }

    val dominantColor = remember(track?.id, activeArtwork) {
        val src = track?.source ?: track?.id
        if (src != null && activeArtwork != null) {
            ArtworkCache.getDominantColor(src) ?: ArtworkCache.computeAndStoreDominantColor(src, activeArtwork!!)
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
                LyricsView(
                    lyrics = player.lyrics,
                    positionMs = playbackState.positionMs,
                    modifier = Modifier.weight(0.8f).fillMaxWidth(0.8f)
                )
            } else {
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

            SpectrumCanvas(
                spectrum = spectrum,
                barColor = AppColors.Accent,
                modifier = Modifier.fillMaxWidth(0.6f).height(36.dp)
            )

            Spacer(Modifier.height(spacing.md))

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

            val effectiveDuration = if (playbackState.durationMs > 0L) playbackState.durationMs else (track?.durationMs ?: 0L)
            val duration = effectiveDuration.coerceAtLeast(1L)
            val fraction = (playbackState.positionMs.toFloat() / duration).coerceIn(0f, 1f)

            val isFullscreenLive = track?.isLiveStream == true || track?.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER
            Row(
                modifier = Modifier.fillMaxWidth(0.7f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFullscreenLive) {
                    Text("LIVE", color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text(formatTime(playbackState.positionMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Spacer(Modifier.width(spacing.sm))
                CustomScrubBar(
                    fraction = if (isFullscreenLive) 1f else fraction,
                    durationMs = duration,
                    enabled = !isFullscreenLive,
                    onSeekToFraction = { if (!isFullscreenLive) player.seekTo((it * duration).toLong()) },
                    onDragFraction = {},
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(spacing.sm))
                if (isFullscreenLive) {
                    Text("LIVE", color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text(formatTime(effectiveDuration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(spacing.md))

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

@Composable
internal fun LyricsView(
    lyrics: List<LrcLine>,
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

@Composable
internal fun EqualizerDialog(
    player: AudioPlayer,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val bandCount = player.getEqualizerBandCount()
    val gainRange = player.getEqualizerGainRange()
    val frequencies = player.getEqualizerBandFrequencies()
    var selectedBandIndex by remember { mutableStateOf(0) }

    val presets = listOf(
        EqualizerPreset.BassBoost,
        EqualizerPreset.Rock,
        EqualizerPreset.Pop,
        EqualizerPreset.Vocal,
        EqualizerPreset.Classical
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionLeft -> {
                            selectedBandIndex = (selectedBandIndex - 1).coerceAtLeast(0)
                            true
                        }
                        Key.DirectionRight -> {
                            selectedBandIndex = (selectedBandIndex + 1).coerceAtMost(bandCount - 1)
                            true
                        }
                        Key.DirectionUp -> {
                            val curGain = player.equalizerGains.getOrElse(selectedBandIndex) { 0f }
                            player.setEqualizerBand(selectedBandIndex, (curGain + 1f).coerceAtMost(gainRange.endInclusive))
                            true
                        }
                        Key.DirectionDown -> {
                            val curGain = player.equalizerGains.getOrElse(selectedBandIndex) { 0f }
                            player.setEqualizerBand(selectedBandIndex, (curGain - 1f).coerceAtLeast(gainRange.start))
                            true
                        }
                        else -> false
                    }
                } else false
            },
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = AppColors.Accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Эквалайзер", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Controls Row: Enable Switch & Reset Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = player.isEqualizerEnabled,
                            onCheckedChange = { player.setEqualizerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.Accent,
                                checkedTrackColor = AppColors.Accent.copy(alpha = 0.35f),
                                uncheckedThumbColor = AppColors.TextMuted,
                                uncheckedTrackColor = AppColors.SurfaceElevated
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (player.isEqualizerEnabled) "Включен" else "Выключен",
                            fontSize = 13.sp,
                            color = if (player.isEqualizerEnabled) AppColors.TextPrimary else AppColors.TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = { player.resetEqualizer() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Сбросить", color = AppColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(spacing.sm))

                // Presets row: Buttons
                Text("Пресеты", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { p ->
                        val isSelected = player.equalizerPreset == p
                        Button(
                            onClick = { player.setEqualizerPreset(p) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AppColors.Accent else AppColors.SurfaceElevated,
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(p.title, fontSize = 10.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                Spacer(Modifier.height(spacing.md))

                // Sliders Row with Edge Scale on the left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edge scale: +max dB, 0 dB, -min dB
                    Column(
                        modifier = Modifier.height(160.dp).padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("+${gainRange.endInclusive.toInt()} dB", fontSize = 9.sp, color = AppColors.TextMuted)
                        Text("0 dB", fontSize = 9.sp, color = AppColors.TextMuted)
                        Text("${gainRange.start.toInt()} dB", fontSize = 9.sp, color = AppColors.TextMuted)
                    }

                    // Vertical Sliders
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (idx in 0 until bandCount) {
                            val freq = frequencies.getOrElse(idx) { 1000 }
                            val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                            val gain = player.equalizerGains.getOrElse(idx) { 0f }
                            VerticalEqualizerSlider(
                                gain = gain,
                                gainRange = gainRange,
                                isSelected = selectedBandIndex == idx,
                                enabled = player.isEqualizerEnabled,
                                frequencyLabel = freqLabel,
                                onGainChange = { newGain ->
                                    selectedBandIndex = idx
                                    player.setEqualizerBand(idx, newGain)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text("Готово", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = AppColors.Surface
    )
}

@Composable
private fun VerticalEqualizerSlider(
    gain: Float,
    gainRange: ClosedFloatingPointRange<Float>,
    isSelected: Boolean,
    enabled: Boolean,
    frequencyLabel: String,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minGain = gainRange.start
    val maxGain = gainRange.endInclusive
    val span = (maxGain - minGain).coerceAtLeast(1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // dB value above slider
        Text(
            text = "${if (gain > 0) "+" else ""}${gain.roundToInt()} dB",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AppColors.Accent else AppColors.TextPrimary,
            maxLines = 1
        )

        Spacer(Modifier.height(8.dp))

        // Vertical Track (width 28dp, height 160dp)
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SurfaceElevated)
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) AppColors.Accent else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .pointerInput(enabled, minGain, maxGain) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val posFraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        val newGain = minGain + posFraction * (maxGain - minGain)
                        onGainChange(newGain)
                    }
                }
                .pointerInput(enabled, minGain, maxGain) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { pos ->
                        val posFraction = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                        val newGain = minGain + posFraction * (maxGain - minGain)
                        onGainChange(newGain)
                    }
                }
                .drawBehind {
                    val trackWidth = 4.dp.toPx()
                    val centerX = size.width / 2f
                    val zeroY = size.height * (1f - (0f - minGain) / span)
                    val currentY = size.height * (1f - (gain.coerceIn(minGain, maxGain) - minGain) / span)

                    // Background center vertical line
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = trackWidth
                    )

                    // Center 0 dB marker notch
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(centerX - 6.dp.toPx(), zeroY),
                        end = Offset(centerX + 6.dp.toPx(), zeroY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Fill bar from 0 dB line up or down
                    if (enabled) {
                        drawLine(
                            color = AppColors.Accent,
                            start = Offset(centerX, zeroY),
                            end = Offset(centerX, currentY),
                            strokeWidth = trackWidth
                        )
                        // Thumb circle
                        drawCircle(
                            color = AppColors.Accent,
                            radius = 7.dp.toPx(),
                            center = Offset(centerX, currentY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(centerX, currentY)
                        )
                    } else {
                        drawCircle(
                            color = AppColors.TextMuted,
                            radius = 5.dp.toPx(),
                            center = Offset(centerX, currentY)
                        )
                    }
                }
        )

        Spacer(Modifier.height(8.dp))

        // Frequency label below slider
        Text(
            text = frequencyLabel,
            fontSize = 10.sp,
            color = AppColors.TextMuted,
            maxLines = 1
        )
    }
}

@Composable
internal fun SleepTimerDialog(
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

@Composable
internal fun SpeedDialog(
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
