package com.example.musicapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.library.Library
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.launchAudioImport
import com.example.musicapp.player.PlayerController
import com.example.musicapp.player.RepeatMode
import kotlinx.coroutines.delay
import kotlin.math.abs

// ==========================================
// Spotify Design System
// ==========================================
private object SpotifyColors {
    val Black = Color(0xFF000000)
    val Background = Color(0xFF121212)
    val Sidebar = Color(0xFF000000)
    val Card = Color(0xFF181818)
    val CardHover = Color(0xFF242424)
    val SurfaceElevated = Color(0xFF282828)
    val Green = Color(0xFF1DB954)
    val GreenHover = Color(0xFF1ED760)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextMuted = Color(0xFF727272)
    val Divider = Color(0xFF282828)
    val Red = Color(0xFFE91429)
    val RedHover = Color(0xFFFF3344)
}

private val CoverGradients = listOf(
    listOf(Color(0xFF842A8C), Color(0xFF38184C)),
    listOf(Color(0xFF1E3264), Color(0xFF0E1626)),
    listOf(Color(0xFFE13300), Color(0xFF5B1700)),
    listOf(Color(0xFF148A08), Color(0xFF0A3B05)),
    listOf(Color(0xFFB02897), Color(0xFF4C0E40)),
    listOf(Color(0xFFE8115B), Color(0xFF540720)),
    listOf(Color(0xFF503750), Color(0xFF201620)),
    listOf(Color(0xFF27856A), Color(0xFF0F362B)),
)

private fun getGradientFor(text: String): Brush {
    val idx = abs(text.hashCode()) % CoverGradients.size
    return Brush.linearGradient(CoverGradients[idx])
}

private enum class NavSection { Library, Playlists }

// ==========================================
// Clean Vector Icons (Lucide / Phosphor style)
// ==========================================
@Composable
private fun LucideIcon(
    name: String,
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = SpotifyColors.TextSecondary,
    strokeWidth: Float = 2f,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 24f
        val sy = h / 24f

        when (name) {
            "search" -> {
                drawCircle(
                    color = tint,
                    radius = 6f * sx,
                    center = Offset(11f * sx, 11f * sy),
                    style = Stroke(width = strokeWidth * sx)
                )
                drawLine(
                    color = tint,
                    start = Offset(15.5f * sx, 15.5f * sy),
                    end = Offset(21f * sx, 21f * sy),
                    strokeWidth = strokeWidth * sx,
                    cap = StrokeCap.Round
                )
            }
            "library" -> {
                drawLine(color = tint, start = Offset(4f * sx, 5f * sy), end = Offset(4f * sx, 19f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(10f * sx, 5f * sy), end = Offset(10f * sx, 19f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(16f * sx, 6f * sy), end = Offset(19f * sx, 19f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "play" -> {
                val path = Path().apply {
                    moveTo(7f * sx, 5f * sy)
                    lineTo(19f * sx, 12f * sy)
                    lineTo(7f * sx, 19f * sy)
                    close()
                }
                drawPath(path, color = tint, style = Fill)
            }
            "pause" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(7f * sx, 5f * sy),
                    size = Size(3.5f * sx, 14f * sy),
                    cornerRadius = CornerRadius(1.5f * sx)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(13.5f * sx, 5f * sy),
                    size = Size(3.5f * sx, 14f * sy),
                    cornerRadius = CornerRadius(1.5f * sx)
                )
            }
            "skip-back" -> {
                val path = Path().apply {
                    moveTo(18f * sx, 6f * sy)
                    lineTo(8f * sx, 12f * sy)
                    lineTo(18f * sx, 18f * sy)
                    close()
                }
                drawPath(path, color = tint, style = Fill)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(5f * sx, 6f * sy),
                    size = Size(2.5f * sx, 12f * sy),
                    cornerRadius = CornerRadius(1f * sx)
                )
            }
            "skip-forward" -> {
                val path = Path().apply {
                    moveTo(6f * sx, 6f * sy)
                    lineTo(16f * sx, 12f * sy)
                    lineTo(6f * sx, 18f * sy)
                    close()
                }
                drawPath(path, color = tint, style = Fill)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(16.5f * sx, 6f * sy),
                    size = Size(2.5f * sx, 12f * sy),
                    cornerRadius = CornerRadius(1f * sx)
                )
            }
            "shuffle" -> {
                drawLine(color = tint, start = Offset(3f * sx, 18f * sy), end = Offset(8f * sx, 18f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(8f * sx, 18f * sy), end = Offset(16f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(16f * sx, 6f * sy), end = Offset(21f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(18f * sx, 3f * sy), end = Offset(21f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(18f * sx, 9f * sy), end = Offset(21f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)

                drawLine(color = tint, start = Offset(3f * sx, 6f * sy), end = Offset(8f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(8f * sx, 6f * sy), end = Offset(11f * sx, 10.5f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(13f * sx, 13.5f * sy), end = Offset(16f * sx, 18f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(16f * sx, 18f * sy), end = Offset(21f * sx, 18f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(18f * sx, 15f * sy), end = Offset(21f * sx, 18f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(18f * sx, 21f * sy), end = Offset(21f * sx, 18f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "repeat" -> {
                val path = Path().apply {
                    moveTo(17f * sx, 2f * sy)
                    lineTo(21f * sx, 6f * sy)
                    lineTo(17f * sx, 10f * sy)
                }
                drawPath(path, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val topArc = Path().apply {
                    moveTo(3f * sx, 12f * sy)
                    lineTo(3f * sx, 9f * sy)
                    quadraticTo(3f * sx, 6f * sy, 6f * sx, 6f * sy)
                    lineTo(20f * sx, 6f * sy)
                }
                drawPath(topArc, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round))

                val bottomArrow = Path().apply {
                    moveTo(7f * sx, 22f * sy)
                    lineTo(3f * sx, 18f * sy)
                    lineTo(7f * sx, 14f * sy)
                }
                drawPath(bottomArrow, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val bottomArc = Path().apply {
                    moveTo(21f * sx, 12f * sy)
                    lineTo(21f * sx, 15f * sy)
                    quadraticTo(21f * sx, 18f * sy, 18f * sx, 18f * sy)
                    lineTo(4f * sx, 18f * sy)
                }
                drawPath(bottomArc, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round))
            }
            "repeat-one" -> {
                // repeat loop with small 1
                drawLine(color = tint, start = Offset(12f * sx, 10f * sy), end = Offset(12f * sx, 14f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(10.5f * sx, 11f * sy), end = Offset(12f * sx, 10f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
            }
            "volume-high" -> {
                val horn = Path().apply {
                    moveTo(3f * sx, 9f * sy)
                    lineTo(7f * sx, 9f * sy)
                    lineTo(12f * sx, 4f * sy)
                    lineTo(12f * sx, 20f * sy)
                    lineTo(7f * sx, 15f * sy)
                    lineTo(3f * sx, 15f * sy)
                    close()
                }
                drawPath(horn, color = tint, style = Fill)
                val arc1 = Path().apply {
                    moveTo(15.5f * sx, 8.5f * sy)
                    quadraticTo(17.5f * sx, 12f * sy, 15.5f * sx, 15.5f * sy)
                }
                drawPath(arc1, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round))
                val arc2 = Path().apply {
                    moveTo(18.5f * sx, 5.5f * sy)
                    quadraticTo(21.5f * sx, 12f * sy, 18.5f * sx, 18.5f * sy)
                }
                drawPath(arc2, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round))
            }
            "volume-low" -> {
                val horn = Path().apply {
                    moveTo(3f * sx, 9f * sy)
                    lineTo(7f * sx, 9f * sy)
                    lineTo(12f * sx, 4f * sy)
                    lineTo(12f * sx, 20f * sy)
                    lineTo(7f * sx, 15f * sy)
                    lineTo(3f * sx, 15f * sy)
                    close()
                }
                drawPath(horn, color = tint, style = Fill)
                val arc1 = Path().apply {
                    moveTo(15.5f * sx, 8.5f * sy)
                    quadraticTo(17.5f * sx, 12f * sy, 15.5f * sx, 15.5f * sy)
                }
                drawPath(arc1, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round))
            }
            "volume-x" -> {
                val horn = Path().apply {
                    moveTo(3f * sx, 9f * sy)
                    lineTo(7f * sx, 9f * sy)
                    lineTo(12f * sx, 4f * sy)
                    lineTo(12f * sx, 20f * sy)
                    lineTo(7f * sx, 15f * sy)
                    lineTo(3f * sx, 15f * sy)
                    close()
                }
                drawPath(horn, color = tint, style = Fill)
                drawLine(color = tint, start = Offset(16f * sx, 9f * sy), end = Offset(21f * sx, 15f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(21f * sx, 9f * sy), end = Offset(16f * sx, 15f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "plus" -> {
                drawLine(color = tint, start = Offset(12f * sx, 5f * sy), end = Offset(12f * sx, 19f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(5f * sx, 12f * sy), end = Offset(19f * sx, 12f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "trash" -> {
                drawLine(color = tint, start = Offset(3f * sx, 6f * sy), end = Offset(21f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(8f * sx, 6f * sy), end = Offset(8f * sx, 3f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(8f * sx, 3f * sy), end = Offset(16f * sx, 3f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(16f * sx, 3f * sy), end = Offset(16f * sx, 6f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                val bin = Path().apply {
                    moveTo(5f * sx, 6f * sy)
                    lineTo(6.5f * sx, 20f * sy)
                    lineTo(17.5f * sx, 20f * sy)
                    lineTo(19f * sx, 6f * sy)
                }
                drawPath(bin, color = tint, style = Stroke(width = strokeWidth * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color = tint, start = Offset(10f * sx, 10f * sy), end = Offset(10f * sx, 16f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(14f * sx, 10f * sy), end = Offset(14f * sx, 16f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "clock" -> {
                drawCircle(color = tint, radius = 9f * sx, center = Offset(12f * sx, 12f * sy), style = Stroke(width = strokeWidth * sx))
                drawLine(color = tint, start = Offset(12f * sx, 7f * sy), end = Offset(12f * sx, 12f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(12f * sx, 12f * sy), end = Offset(16f * sx, 12f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "upload" -> {
                drawLine(color = tint, start = Offset(4f * sx, 17f * sy), end = Offset(4f * sx, 20f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(4f * sx, 20f * sy), end = Offset(20f * sx, 20f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(20f * sx, 20f * sy), end = Offset(20f * sx, 17f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(12f * sx, 15f * sy), end = Offset(12f * sx, 4f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(7f * sx, 9f * sy), end = Offset(12f * sx, 4f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(17f * sx, 9f * sy), end = Offset(12f * sx, 4f * sy), strokeWidth = strokeWidth * sx, cap = StrokeCap.Round)
            }
            "heart" -> {
                val heart = Path().apply {
                    moveTo(12f * sx, 20f * sy)
                    cubicTo(5f * sx, 14f * sy, 2f * sx, 9f * sy, 4f * sx, 5.5f * sy)
                    cubicTo(6f * sx, 2.5f * sy, 10f * sx, 3f * sy, 12f * sx, 6f * sy)
                    cubicTo(14f * sx, 3f * sy, 18f * sx, 2.5f * sy, 20f * sx, 5.5f * sy)
                    cubicTo(22f * sx, 9f * sy, 19f * sx, 14f * sy, 12f * sx, 20f * sy)
                    close()
                }
                drawPath(heart, color = tint, style = Stroke(width = strokeWidth * sx, join = StrokeJoin.Round))
            }
            "heart-filled" -> {
                val heart = Path().apply {
                    moveTo(12f * sx, 20f * sy)
                    cubicTo(5f * sx, 14f * sy, 2f * sx, 9f * sy, 4f * sx, 5.5f * sy)
                    cubicTo(6f * sx, 2.5f * sy, 10f * sx, 3f * sy, 12f * sx, 6f * sy)
                    cubicTo(14f * sx, 3f * sy, 18f * sx, 2.5f * sy, 20f * sx, 5.5f * sy)
                    cubicTo(22f * sx, 9f * sy, 19f * sx, 14f * sy, 12f * sx, 20f * sy)
                    close()
                }
                drawPath(heart, color = tint, style = Fill)
            }
        }
    }
}

// ==========================================
// Animated Sound Equalizer Indicator
// ==========================================
@Composable
private fun AnimatedEqualizer(
    color: Color = SpotifyColors.Green,
    modifier: Modifier = Modifier.size(16.dp),
) {
    val transition = rememberInfiniteTransition()
    val h1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), AnimRepeatMode.Reverse)
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), AnimRepeatMode.Reverse)
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), AnimRepeatMode.Reverse)
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w / 4f
        val gap = w / 8f

        drawRoundRect(color, Offset(0f, h * (1f - h1)), Size(barW, h * h1), CornerRadius(barW / 2))
        drawRoundRect(color, Offset(barW + gap, h * (1f - h2)), Size(barW, h * h2), CornerRadius(barW / 2))
        drawRoundRect(color, Offset((barW + gap) * 2, h * (1f - h3)), Size(barW, h * h3), CornerRadius(barW / 2))
    }
}

// ==========================================
// Spotify Interactive Scrub Slider
// ==========================================
@Composable
private fun SpotifyScrubBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.dp,
    activeColor: Color = SpotifyColors.Green,
    inactiveColor: Color = SpotifyColors.SurfaceElevated,
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
                        onSeek(newFrac)
                        tryAwaitRelease()
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + thumbRadius * 2)
        ) {
            val trackH = barHeight.toPx()
            val cy = size.height / 2f
            val w = size.width

            // Background inactive track
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Active track
            val fillW = (fraction.coerceIn(0f, 1f) * w)
            val fillCol = if (isHovered || isDragging) SpotifyColors.GreenHover else activeColor
            drawRoundRect(
                color = fillCol,
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(fillW, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Thumb visible on hover or drag (Bug 5 fix: no stray handle, visible only on hover)
            if (isHovered || isDragging) {
                drawCircle(
                    color = SpotifyColors.TextPrimary,
                    radius = thumbRadius.toPx(),
                    center = Offset(fillW, cy)
                )
            }
        }
    }
}

// ==========================================
// Root MusicApp Composable
// ==========================================
@Composable
fun MusicApp() {
    val library = remember { Library() }
    val player = remember { PlayerController() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var navSection by remember { mutableStateOf(NavSection.Library) }
    var openPlaylistId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    val spotifyColorScheme = darkColorScheme(
        primary = SpotifyColors.Green,
        onPrimary = SpotifyColors.Black,
        surface = SpotifyColors.Background,
        onSurface = SpotifyColors.TextPrimary,
        background = SpotifyColors.Background,
        onBackground = SpotifyColors.TextPrimary,
    )

    MaterialTheme(colorScheme = spotifyColorScheme) {
        Surface(Modifier.fillMaxSize(), color = SpotifyColors.Background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val isCompact = maxWidth < 900.dp

                Column(Modifier.fillMaxSize()) {
                    // Main area with Sidebar and Content
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        // Left Sidebar (collapses to icon rail below 900px)
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

                        // Main Content (Constrained max-width for ultra-wide screens)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(SpotifyColors.Background),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 1350.dp)
                                    .fillMaxWidth()
                            ) {
                                // Top Search & Action Bar (Bug 1 fixed: vertically centered, line-height fixed)
                                SpotifyTopBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onUploadSong = {
                                        launchAudioImport { track ->
                                            library.addTrack(track)
                                            toastMessage = "Трек «${track.title}» добавлен"
                                        }
                                    },
                                )

                                // Body
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = if (isCompact) 12.dp else 24.dp)
                                ) {
                                    when (navSection) {
                                        NavSection.Library -> {
                                            LibraryView(
                                                library = library,
                                                player = player,
                                                searchQuery = searchQuery,
                                                isCompact = isCompact,
                                                onToast = { toastMessage = it },
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
                                                onToast = { toastMessage = it },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Fixed Player Bar (Bug 3, 5, 6 fixed)
                    SpotifyNowPlayingBar(
                        player = player,
                        isCompact = isCompact,
                    )
                }

                // Toast Notification
                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                ) {
                    toastMessage?.let { msg ->
                        Surface(
                            color = SpotifyColors.Green,
                            shape = RoundedCornerShape(500.dp),
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                msg,
                                color = SpotifyColors.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
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
                toastMessage = "Плейлист «$name» создан"
            },
        )
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
    val sidebarWidth = if (isCompact) 72.dp else 240.dp

    Column(
        Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(SpotifyColors.Sidebar)
            .padding(vertical = 16.dp, horizontal = if (isCompact) 8.dp else 12.dp)
    ) {
        // App Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp, start = if (isCompact) 0.dp else 8.dp),
            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SpotifyColors.Green),
                contentAlignment = Alignment.Center
            ) {
                LucideIcon("play", modifier = Modifier.size(16.dp), tint = SpotifyColors.Black)
            }
            if (!isCompact) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "MusicApp",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpotifyColors.TextPrimary,
                        fontSize = 19.sp,
                    )
                )
            }
        }

        // Navigation Menu
        SidebarNavItem(
            label = "Моя медиатека",
            iconName = "library",
            isCompact = isCompact,
            isSelected = currentSection == NavSection.Library && openPlaylistId == null,
            onClick = { onSelectSection(NavSection.Library) }
        )

        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpotifyColors.Divider))
        Spacer(Modifier.height(14.dp))

        // Playlists Section Header
        if (!isCompact) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ПЛЕЙЛИСТЫ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SpotifyColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                )
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SpotifyColors.Card)
                        .clickable { onCreatePlaylist() },
                    contentAlignment = Alignment.Center
                ) {
                    LucideIcon("plus", modifier = Modifier.size(16.dp), tint = SpotifyColors.TextSecondary)
                }
            }
        } else {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SpotifyColors.Card)
                    .clickable { onCreatePlaylist() }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                LucideIcon("plus", modifier = Modifier.size(16.dp), tint = SpotifyColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Playlists List
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(library.playlists, key = { _, p -> p.id }) { _, p ->
                val isSelected = openPlaylistId == p.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SpotifyColors.SurfaceElevated else Color.Transparent)
                        .clickable { onSelectPlaylist(p.id) }
                        .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(getGradientFor(p.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon("play", modifier = Modifier.size(12.dp), tint = Color.White)
                    }
                    if (!isCompact) {
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) SpotifyColors.Green else SpotifyColors.TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${p.trackIds.size} трек(ов)",
                                style = MaterialTheme.typography.bodySmall.copy(color = SpotifyColors.TextMuted),
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
    iconName: String,
    isCompact: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> SpotifyColors.SurfaceElevated
                    isHovered -> SpotifyColors.CardHover
                    else -> Color.Transparent
                }
            )
            .hoverable(interactionSource)
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.Start
    ) {
        LucideIcon(
            name = iconName,
            tint = if (isSelected) SpotifyColors.Green else if (isHovered) SpotifyColors.TextPrimary else SpotifyColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        if (!isCompact) {
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) SpotifyColors.TextPrimary else if (isHovered) SpotifyColors.TextPrimary else SpotifyColors.TextSecondary,
                    fontSize = 14.sp
                )
            )
        }
    }
}

// ==========================================
// Top Bar with Vertically-Centered Search (Bug 1 Fixed)
// ==========================================
@Composable
private fun SpotifyTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUploadSong: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Search Input in Spotify Style - Custom BasicTextField for perfect line-height & vertical centering
        Box(
            Modifier
                .width(360.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(500.dp))
                .background(SpotifyColors.SurfaceElevated)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LucideIcon("search", modifier = Modifier.size(18.dp), tint = SpotifyColors.TextSecondary)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Что хочешь послушать?",
                            color = SpotifyColors.TextMuted,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = SpotifyColors.TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(SpotifyColors.Green),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { onSearchQueryChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = SpotifyColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Upload Button (Spotify Green Pill with upload icon)
        Button(
            onClick = onUploadSong,
            shape = RoundedCornerShape(500.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyColors.Green,
                contentColor = SpotifyColors.Black
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            modifier = Modifier.height(42.dp)
        ) {
            LucideIcon("upload", modifier = Modifier.size(16.dp), tint = SpotifyColors.Black)
            Spacer(Modifier.width(8.dp))
            Text("Загрузить трек", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ==========================================
// Library View
// ==========================================
@Composable
private fun LibraryView(
    library: Library,
    player: PlayerController,
    searchQuery: String,
    isCompact: Boolean,
    onToast: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    val visible = library.tracks.filter { t ->
        searchQuery.isBlank() ||
            t.title.contains(searchQuery, ignoreCase = true) ||
            t.artist.contains(searchQuery, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        // Hero Header Banner
        HeroBanner(
            title = "Моя медиатека",
            subtitle = "${library.tracks.size} трек(ов) • Spotify Style",
            badge = "КОЛЛЕКЦИЯ",
            gradient = getGradientFor("Library"),
            canPlay = visible.any { it.source != null },
            isPlaying = player.isPlaying && visible.any { it.id == player.current?.id },
            onPlayAll = {
                visible.firstOrNull { it.source != null }?.let { player.play(it, visible) }
            }
        )

        Spacer(Modifier.height(20.dp))

        // Tracks Table Header (Bug 2 Fixed: proper fixed widths, clock icon for duration)
        TableHeader(isCompact = isCompact)

        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LucideIcon("search", modifier = Modifier.size(48.dp), tint = SpotifyColors.TextMuted)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isBlank()) "В медиатеке пока нет треков" else "По запросу «$searchQuery» ничего не найдено",
                        color = SpotifyColors.TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Track Rows with Hover & Action buttons
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(visible, key = { _, track -> track.id }) { index, track ->
                    TrackRowItem(
                        index = index + 1,
                        track = track,
                        isPlaying = player.current?.id == track.id && player.isPlaying,
                        isCurrent = player.current?.id == track.id,
                        isCompact = isCompact,
                        playlists = library.playlists,
                        onPlay = { player.play(track, visible) },
                        onAddToPlaylist = { pId ->
                            library.addToPlaylist(pId, track.id)
                            onToast("Добавлено в плейлист")
                        },
                        onDelete = { pendingDelete = track },
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
                player.onTrackRemoved(track.id)
                library.removeTrack(track.id)
                onToast("Трек «${track.title}» удален")
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
    player: PlayerController,
    playlistId: String?,
    isCompact: Boolean,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
) {
    val playlist = library.playlists.find { it.id == playlistId }
    if (playlist == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Плейлист не найден", color = SpotifyColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Вернуться") }
        }
        return
    }

    val tracks = library.tracksIn(playlist)
    var pendingDeletePlaylist by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Назад к плейлистам", color = SpotifyColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { pendingDeletePlaylist = true }) {
                LucideIcon("trash", modifier = Modifier.size(16.dp), tint = SpotifyColors.Red)
                Spacer(Modifier.width(6.dp))
                Text("Удалить плейлист", color = SpotifyColors.Red, fontSize = 13.sp)
            }
        }

        HeroBanner(
            title = playlist.name,
            subtitle = "${tracks.size} трек(ов) в плейлисте",
            badge = "ПЛЕЙЛИСТ",
            gradient = getGradientFor(playlist.name),
            canPlay = tracks.any { it.source != null },
            isPlaying = player.isPlaying && tracks.any { it.id == player.current?.id },
            onPlayAll = {
                tracks.firstOrNull { it.source != null }?.let { player.play(it, tracks) }
            }
        )

        Spacer(Modifier.height(20.dp))
        TableHeader(isCompact = isCompact)

        if (tracks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("В этом плейлисте пока нет треков. Добавьте их из Медиатеки!", color = SpotifyColors.TextMuted)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRowItem(
                        index = index + 1,
                        track = track,
                        isPlaying = player.current?.id == track.id && player.isPlaying,
                        isCurrent = player.current?.id == track.id,
                        isCompact = isCompact,
                        playlists = library.playlists,
                        onPlay = { player.play(track, tracks) },
                        onAddToPlaylist = { pId ->
                            library.addToPlaylist(pId, track.id)
                            onToast("Добавлено в плейлист")
                        },
                        onRemoveFromPlaylist = {
                            library.removeFromPlaylist(playlist.id, track.id)
                            onToast("Удалено из плейлиста")
                        }
                    )
                }
            }
        }
    }

    if (pendingDeletePlaylist) {
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylist = false },
            title = { Text("Удалить плейлист «${playlist.name}»?") },
            text = { Text("Плейлист будет удален. Сами треки останутся в вашей медиатеке.") },
            confirmButton = {
                Button(
                    onClick = {
                        library.deletePlaylist(playlist.id)
                        pendingDeletePlaylist = false
                        onToast("Плейлист удален")
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.Red)
                ) { Text("Удалить", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlaylist = false }) { Text("Отмена") }
            },
            containerColor = SpotifyColors.SurfaceElevated
        )
    }
}

// ==========================================
// Hero Banner (56px circular play button with hover effect)
// ==========================================
@Composable
private fun HeroBanner(
    title: String,
    subtitle: String,
    badge: String,
    gradient: Brush,
    canPlay: Boolean,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPlayHovered by interactionSource.collectIsHoveredAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SpotifyColors.SurfaceElevated, SpotifyColors.Background)
                )
            )
            .padding(20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Large Cover Art with soft shadow
        Box(
            Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            LucideIcon("library", modifier = Modifier.size(54.dp), tint = Color.White.copy(alpha = 0.85f))
        }

        Spacer(Modifier.width(20.dp))

        Column(Modifier.weight(1f)) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SpotifyColors.TextSecondary,
                    letterSpacing = 1.2.sp
                )
            )
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpotifyColors.TextPrimary,
                    fontSize = 32.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = SpotifyColors.TextSecondary)
            )
            Spacer(Modifier.height(14.dp))

            // Big 56px Green Circular Play Button with scale hover
            Box(
                Modifier
                    .size(56.dp)
                    .scale(if (isPlayHovered && canPlay) 1.05f else 1.0f)
                    .clip(CircleShape)
                    .background(if (canPlay) (if (isPlayHovered) SpotifyColors.GreenHover else SpotifyColors.Green) else SpotifyColors.Card)
                    .hoverable(interactionSource)
                    .clickable(enabled = canPlay) { onPlayAll() },
                contentAlignment = Alignment.Center
            ) {
                LucideIcon(
                    name = if (isPlaying) "pause" else "play",
                    modifier = Modifier.size(24.dp),
                    tint = if (canPlay) SpotifyColors.Black else SpotifyColors.TextMuted
                )
            }
        }
    }
}

// ==========================================
// Table Header (Bug 2 Fixed: No wrap, Clock icon)
// ==========================================
@Composable
private fun TableHeader(isCompact: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#",
            color = SpotifyColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
        Text(
            "НАЗВАНИЕ",
            color = SpotifyColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (!isCompact) {
            Box(Modifier.width(80.dp), contentAlignment = Alignment.CenterEnd) {
                LucideIcon("clock", modifier = Modifier.size(15.dp), tint = SpotifyColors.TextMuted)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            "ДЕЙСТВИЯ",
            color = SpotifyColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpotifyColors.Divider))
    Spacer(Modifier.height(4.dp))
}

// ==========================================
// Track Row Item with Equalizer & Hover Actions
// ==========================================
@Composable
private fun TrackRowItem(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isCompact: Boolean,
    playlists: List<Playlist>,
    onPlay: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val rowInteraction = remember { MutableInteractionSource() }
    val isRowHovered by rowInteraction.collectIsHoveredAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isCurrent -> SpotifyColors.SurfaceElevated
                    isRowHovered -> SpotifyColors.CardHover
                    else -> Color.Transparent
                }
            )
            .hoverable(rowInteraction)
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number / Animated Equalizer / Play button on hover
        Box(Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
            when {
                isPlaying -> {
                    AnimatedEqualizer()
                }
                isRowHovered -> {
                    LucideIcon("play", modifier = Modifier.size(14.dp), tint = SpotifyColors.TextPrimary)
                }
                else -> {
                    Text(
                        index.toString(),
                        color = if (isCurrent) SpotifyColors.Green else SpotifyColors.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Thumbnail Cover Art
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(getGradientFor(track.title)),
            contentAlignment = Alignment.Center
        ) {
            LucideIcon("play", modifier = Modifier.size(14.dp), tint = Color.White)
        }

        Spacer(Modifier.width(12.dp))

        // Title and Artist
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isCurrent) SpotifyColors.Green else SpotifyColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodySmall.copy(color = SpotifyColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration (Bug 7 fixed: graceful fallback)
        if (!isCompact) {
            Text(
                if (track.durationMs > 0) formatTime(track.durationMs) else "--:--",
                color = SpotifyColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.width(80.dp),
                maxLines = 1
            )
        }

        Spacer(Modifier.width(16.dp))

        // Action Buttons (Reveal on row hover or keep visible for accessibility)
        Row(
            Modifier.width(70.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Add to Playlist Button
            Box {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    LucideIcon("plus", modifier = Modifier.size(16.dp), tint = SpotifyColors.TextSecondary)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(SpotifyColors.SurfaceElevated)
                ) {
                    if (playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Нет плейлистов", color = SpotifyColors.TextMuted) },
                            onClick = { menuOpen = false }
                        )
                    } else {
                        playlists.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, color = SpotifyColors.TextPrimary) },
                                onClick = {
                                    onAddToPlaylist(p.id)
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            if (onRemoveFromPlaylist != null) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onRemoveFromPlaylist() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = SpotifyColors.TextMuted, fontSize = 12.sp)
                }
            }

            if (onDelete != null) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    LucideIcon("trash", modifier = Modifier.size(16.dp), tint = SpotifyColors.TextMuted)
                }
            }
        }
    }
}

// ==========================================
// Bottom Fixed Player Bar (Bugs 3, 5, 6 Fixed)
// ==========================================
@Composable
private fun SpotifyNowPlayingBar(
    player: PlayerController,
    isCompact: Boolean,
) {
    // Keep playback ticking
    LaunchedEffect(Unit) {
        while (true) {
            player.tick()
            delay(250)
        }
    }

    val track = player.current
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = player.durationMs.coerceAtLeast(1L)
    val fraction = dragging ?: (player.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    var isLiked by remember { mutableStateOf(false) }

    Surface(
        color = SpotifyColors.Card,
        modifier = Modifier.fillMaxWidth().height(88.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Track Info & Heart (240.dp)
            Row(
                Modifier.width(if (isCompact) 180.dp else 240.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track != null) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(getGradientFor(track.title)),
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon("play", modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SpotifyColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = SpotifyColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { isLiked = !isLiked },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon(
                            name = if (isLiked) "heart-filled" else "heart",
                            modifier = Modifier.size(18.dp),
                            tint = if (isLiked) SpotifyColors.Green else SpotifyColors.TextSecondary
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SpotifyColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon("library", modifier = Modifier.size(20.dp), tint = SpotifyColors.TextMuted)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Ничего не играет",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SpotifyColors.TextMuted)
                    )
                }
            }

            // Center: Playback Controls & Progress Timeline (Weight 1f)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Controls Row (SVG vector icons, no blue emojis)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Shuffle
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { player.shuffle = !player.shuffle },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon(
                            "shuffle",
                            modifier = Modifier.size(18.dp),
                            tint = if (player.shuffle) SpotifyColors.Green else SpotifyColors.TextSecondary
                        )
                    }

                    // Previous
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { player.previous() },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon("skip-back", modifier = Modifier.size(20.dp), tint = SpotifyColors.TextPrimary)
                    }

                    // 40px Circular White Play / Pause Button
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SpotifyColors.TextPrimary)
                            .clickable(enabled = track?.source != null) { player.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon(
                            name = if (player.isPlaying) "pause" else "play",
                            modifier = Modifier.size(18.dp),
                            tint = SpotifyColors.Black
                        )
                    }

                    // Next
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { player.next() },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon("skip-forward", modifier = Modifier.size(20.dp), tint = SpotifyColors.TextPrimary)
                    }

                    // Repeat
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { player.cycleRepeat() },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon(
                            name = if (player.repeat == RepeatMode.One) "repeat-one" else "repeat",
                            modifier = Modifier.size(18.dp),
                            tint = if (player.repeat != RepeatMode.Off) SpotifyColors.Green else SpotifyColors.TextSecondary
                        )
                    }
                }

                // Progress Bar Row (Bug 5 Fixed: no stray handle, aligned labels on one baseline, thumb on hover)
                Row(
                    Modifier.fillMaxWidth(if (isCompact) 0.95f else 0.8f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(player.positionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyColors.TextMuted,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.width(36.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    SpotifyScrubBar(
                        fraction = fraction,
                        onSeek = { newFrac ->
                            player.seekTo((newFrac * duration).toLong())
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        formatTime(player.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyColors.TextMuted,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Right: Volume Controls (Bug 6 Fixed: aligned thumb, thinner track, subtle text)
            if (!isCompact) {
                Row(
                    Modifier.width(200.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val vol = player.volume
                    val volIcon = when {
                        vol == 0f -> "volume-x"
                        vol < 0.5f -> "volume-low"
                        else -> "volume-high"
                    }

                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { player.toggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideIcon(volIcon, modifier = Modifier.size(18.dp), tint = SpotifyColors.TextSecondary)
                    }

                    Spacer(Modifier.width(6.dp))

                    SpotifyScrubBar(
                        fraction = vol,
                        onSeek = { player.updateVolume(it) },
                        modifier = Modifier.width(96.dp),
                        barHeight = 4.dp,
                        thumbRadius = 5.dp
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "${(vol * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SpotifyColors.TextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// Dialogs
// ==========================================
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый плейлист", color = SpotifyColors.TextPrimary, fontWeight = FontWeight.Bold) },
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
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.Green)
            ) {
                Text("Создать", color = SpotifyColors.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = SpotifyColors.TextSecondary)
            }
        },
        containerColor = SpotifyColors.SurfaceElevated
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
                    "Трек будет удален из библиотеки и всех плейлистов, а файл будет удален с диска."
                } else {
                    "Трек будет удален из библиотеки и всех плейлистов."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyColors.Red)
            ) { Text("Удалить", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        containerColor = SpotifyColors.SurfaceElevated
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
