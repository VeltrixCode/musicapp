package com.example.musicapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Spotify-inspired color palette strictly adhering to WCAG AA contrast standards.
 */
object AppColors {
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF181818)
    val SurfaceElevated = Color(0xFF282828)
    val CardHover = Color(0xFF2E2E2E)
    val Accent = Color(0xFF1DB954)
    val AccentHover = Color(0xFF1ED760)
    val AccentPressed = Color(0xFF169C46)

    val TextPrimary = Color(0xFFFFFFFF)
    // #B3B3B3 on #121212 yields ~7.5:1 contrast ratio (exceeds WCAG AA 4.5:1 requirement)
    val TextSecondary = Color(0xFFB3B3B3)
    val TextMuted = Color(0xFF8A8A8A)

    val Divider = Color(0xFF282828)
    val Border = Color(0xFF333333)

    val Error = Color(0xFFE91429)
    val ErrorHover = Color(0xFFFF3344)

    val WindowBar = Color(0xFF121212)
    val WindowButtonHover = Color(0xFF2A2A2A)
    val WindowCloseHover = Color(0xFFE81123)

    val ShimmerBase = Color(0xFF242424)
    val ShimmerHighlight = Color(0xFF383838)
}

@Immutable
data class Spacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        color = AppColors.TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = AppColors.TextPrimary,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = AppColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = AppColors.TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = AppColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        color = AppColors.TextPrimary,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = AppColors.TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
        color = AppColors.TextSecondary,
    )
)

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = Color.Black,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceElevated,
    onSurfaceVariant = AppColors.TextSecondary,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    outline = AppColors.Border,
    error = AppColors.Error,
    onError = Color.White,
)

val VibrantGradients = listOf(
    listOf(Color(0xFF503750), Color(0xFF1E1420)),
    listOf(Color(0xFF1E3264), Color(0xFF0E1628)),
    listOf(Color(0xFF27856A), Color(0xFF0E2F26)),
    listOf(Color(0xFF8D67AB), Color(0xFF2C193C)),
    listOf(Color(0xFFE8115B), Color(0xFF45061B)),
    listOf(Color(0xFF148A08), Color(0xFF072B04)),
    listOf(Color(0xFFBA5D07), Color(0xFF381B02)),
    listOf(Color(0xFF842A8C), Color(0xFF2B0A2E)),
)

fun fallbackGradientFor(seed: String): Brush {
    val idx = abs(seed.hashCode()) % VibrantGradients.size
    return Brush.verticalGradient(VibrantGradients[idx])
}

@Composable
fun AppTheme(
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSpacing provides spacing) {
        MaterialTheme(
            colorScheme = SpotifyDarkColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
