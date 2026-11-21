package com.example.musicapp.theme

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Background decorative texture and backdrop pattern styles.
 */
enum class ThemePattern {
    NONE,
    EMERALD_WAVES,
    CYBER_GRID,
    RETRO_VINYL,
    NEBULA_STARS,
    NORDIC_FROST,
    ANIME_LOFI,
    OCEAN_WAVES,
    CRIMSON_ECLIPSE,
    SUNSET_RAYS,
    PHANTOM_MATRIX
}

/**
 * Aesthetic illustration kinds for theme previews and cards.
 */
enum class ThemeIllustrationKind {
    EMERALD_STUDIO,
    CYBERPUNK_SYNTHWAVE,
    RETRO_VINYL,
    DEEP_SPACE_NEBULA,
    NORDIC_FROST,
    ANIME_LOFI,
    MIDNIGHT_OCEAN,
    CRIMSON_MOON,
    SUNSET_AMBER,
    PHANTOM_NOIR,
    AMOLED_MINIMAL,
    DEFAULT
}

/**
 * Serializable theme configuration supporting custom palettes and backdrop visual textures.
 */
@Serializable
data class ThemeConfig(
    val presetId: String = "JVX_DEFAULT",
    val customAccentHex: String? = null,
    val patternEnabled: Boolean = true,
    val patternStyle: String = "DEFAULT",
    val ambientGlowEnabled: Boolean = true,
)

/**
 * Theme preset definition providing palette colors, decorative visual patterns, and artwork metadata.
 */
@Immutable
data class ThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardHover: Color,
    val defaultAccent: Color,
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xFFB3B3B3),
    val textMuted: Color = Color(0xFF8A8A8A),
    val divider: Color = Color(0xFF282828),
    val border: Color = Color(0xFF333333),
    val windowBar: Color = background,
    val windowButtonHover: Color = surfaceElevated,
    val shimmerBase: Color = surfaceElevated,
    val shimmerHighlight: Color = cardHover,
    val isMonochrome: Boolean = false,
    val pattern: ThemePattern = ThemePattern.NONE,
    val badgeText: String = "",
    val badgeColor: Color = defaultAccent,
    val patternLabel: String = "",
    val cardBorderGradient: List<Color>? = null,
    val gradientOverlay: List<Color>? = null,
    val ambientGlow: Color? = null,
    val illustrationKind: ThemeIllustrationKind = ThemeIllustrationKind.DEFAULT,
)

/**
 * Curated preset themes for JVXMusic, including Cyberpunk, Retro Vinyl, Deep Space, Nordic Frost, and Anime Lo-Fi.
 */
object ThemePresets {
    val JVX_EMERALD = ThemePreset(
        id = "JVX_DEFAULT",
        name = "JVX Emerald",
        description = "Фирменная тёмная палитра с изумрудным акцентом",
        background = Color(0xFF121212),
        surface = Color(0xFF181818),
        surfaceElevated = Color(0xFF282828),
        cardHover = Color(0xFF2E2E2E),
        defaultAccent = Color(0xFF00D26A),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFB3B3B3),
        textMuted = Color(0xFF8A8A8A),
        divider = Color(0xFF282828),
        border = Color(0xFF333333),
        pattern = ThemePattern.EMERALD_WAVES,
        badgeText = "STUDIO",
        patternLabel = "Акустические звуковые волны",
        cardBorderGradient = listOf(Color(0xFF00D26A), Color(0xFF10B981)),
        illustrationKind = ThemeIllustrationKind.EMERALD_STUDIO,
    )

    val CYBERPUNK_VIOLET = ThemePreset(
        id = "CYBERPUNK",
        name = "Cyberpunk Neon",
        description = "Неоновый киберпанк с фиолетовым свечением и 3D-сеткой",
        background = Color(0xFF0B0614),
        surface = Color(0xFF160D29),
        surfaceElevated = Color(0xFF241442),
        cardHover = Color(0xFF321B5C),
        defaultAccent = Color(0xFFD946EF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA78BFA),
        textMuted = Color(0xFF7C3AED),
        divider = Color(0xFF27134A),
        border = Color(0xFF3A1F6E),
        pattern = ThemePattern.CYBER_GRID,
        badgeText = "SYNTHWAVE",
        patternLabel = "Неоновая 3D-сетка и сканлайны",
        cardBorderGradient = listOf(Color(0xFFD946EF), Color(0xFF00E5FF)),
        ambientGlow = Color(0xFFD946EF),
        illustrationKind = ThemeIllustrationKind.CYBERPUNK_SYNTHWAVE,
    )

    val RETRO_VINYL = ThemePreset(
        id = "RETRO_VINYL",
        name = "Retro Vinyl & Cassette",
        description = "Аналоговое звучание 80-х, тёплый шеллак и виниловые дорожки",
        background = Color(0xFF14110E),
        surface = Color(0xFF1D1713),
        surfaceElevated = Color(0xFF2A211B),
        cardHover = Color(0xFF382B22),
        defaultAccent = Color(0xFFE5A93C),
        textPrimary = Color(0xFFFFF7ED),
        textSecondary = Color(0xFFD4BFA7),
        textMuted = Color(0xFF9E856D),
        divider = Color(0xFF2A211B),
        border = Color(0xFF3D2F25),
        pattern = ThemePattern.RETRO_VINYL,
        badgeText = "VINYL",
        patternLabel = "Концентрические виниловые дорожки",
        cardBorderGradient = listOf(Color(0xFFE5A93C), Color(0xFF92400E)),
        illustrationKind = ThemeIllustrationKind.RETRO_VINYL,
    )

    val DEEP_SPACE = ThemePreset(
        id = "DEEP_SPACE",
        name = "Deep Space Nebula",
        description = "Дальний космос, мерцающие звёзды и созвездия",
        background = Color(0xFF070814),
        surface = Color(0xFF0F1226),
        surfaceElevated = Color(0xFF181C3D),
        cardHover = Color(0xFF222854),
        defaultAccent = Color(0xFF818CF8),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        divider = Color(0xFF191F44),
        border = Color(0xFF252D63),
        pattern = ThemePattern.NEBULA_STARS,
        badgeText = "COSMIC",
        patternLabel = "Звёздная туманность и пульсары",
        cardBorderGradient = listOf(Color(0xFF818CF8), Color(0xFFC084FC)),
        ambientGlow = Color(0xFF818CF8),
        illustrationKind = ThemeIllustrationKind.DEEP_SPACE_NEBULA,
    )

    val NORDIC_FROST = ThemePreset(
        id = "NORDIC_FROST",
        name = "Nordic Frost",
        description = "Северное сияние, арктический лёд и кристаллы инея",
        background = Color(0xFF091318),
        surface = Color(0xFF0F1E26),
        surfaceElevated = Color(0xFF172D3A),
        cardHover = Color(0xFF203B4C),
        defaultAccent = Color(0xFF2DD4BF),
        textPrimary = Color(0xFFF0FDFA),
        textSecondary = Color(0xFF99F6E4),
        textMuted = Color(0xFF5EEAD4),
        divider = Color(0xFF162A36),
        border = Color(0xFF1E3A4B),
        pattern = ThemePattern.NORDIC_FROST,
        badgeText = "AURORA",
        patternLabel = "Ленты полярного сияния",
        cardBorderGradient = listOf(Color(0xFF2DD4BF), Color(0xFF38BDF8)),
        ambientGlow = Color(0xFF2DD4BF),
        illustrationKind = ThemeIllustrationKind.NORDIC_FROST,
    )

    val ANIME_LOFI = ThemePreset(
        id = "ANIME_LOFI",
        name = "Anime Lo-Fi Twilight",
        description = "Эстетика токийского заката, лепестки сакуры и ностальгия",
        background = Color(0xFF150B1C),
        surface = Color(0xFF1F112A),
        surfaceElevated = Color(0xFF2D183D),
        cardHover = Color(0xFF3C2050),
        defaultAccent = Color(0xFFFB7185),
        textPrimary = Color(0xFFFFF1F2),
        textSecondary = Color(0xFFFECDD3),
        textMuted = Color(0xFFFDA4AF),
        divider = Color(0xFF2B163B),
        border = Color(0xFF3F1F54),
        pattern = ThemePattern.ANIME_LOFI,
        badgeText = "LO-FI",
        patternLabel = "Сумерки и лепестки сакуры",
        cardBorderGradient = listOf(Color(0xFFFB7185), Color(0xFFA855F7)),
        ambientGlow = Color(0xFFFB7185),
        illustrationKind = ThemeIllustrationKind.ANIME_LOFI,
    )

    val PHANTOM = ThemePreset(
        id = "PHANTOM",
        name = "Phantom Cyber-Noir",
        description = "Ультра-тёмный минимализм",
        background = Color(0xFF090909),
        surface = Color(0xFF131313),
        surfaceElevated = Color(0xFF1D1D1D),
        cardHover = Color(0xFF252525),
        defaultAccent = Color(0xFFF3F4F6),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF8E8E93),
        textMuted = Color(0xFF555555),
        divider = Color(0xFF1C1C1C),
        border = Color(0xFF242424),
        isMonochrome = true,
        pattern = ThemePattern.PHANTOM_MATRIX,
        badgeText = "NOIR",
        patternLabel = "Изометрическая матрица",
        cardBorderGradient = listOf(Color(0xFFE5E7EB), Color(0xFF4B5563)),
        illustrationKind = ThemeIllustrationKind.PHANTOM_NOIR,
    )

    val MIDNIGHT_BLUE = ThemePreset(
        id = "MIDNIGHT_BLUE",
        name = "Midnight Ocean",
        description = "Глубокая ночная палитра синего океана",
        background = Color(0xFF0A0F1D),
        surface = Color(0xFF111827),
        surfaceElevated = Color(0xFF1F2937),
        cardHover = Color(0xFF283548),
        defaultAccent = Color(0xFF38BDF8),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        divider = Color(0xFF192231),
        border = Color(0xFF1E293B),
        pattern = ThemePattern.OCEAN_WAVES,
        badgeText = "OCEAN",
        patternLabel = "Океанические волны",
        cardBorderGradient = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
        illustrationKind = ThemeIllustrationKind.MIDNIGHT_OCEAN,
    )

    val SUNSET_AMBER = ThemePreset(
        id = "SUNSET_AMBER",
        name = "Sunset Amber",
        description = "Тёплые закатные тона и медовый янтарь",
        background = Color(0xFF130E0A),
        surface = Color(0xFF1C1510),
        surfaceElevated = Color(0xFF2B2019),
        cardHover = Color(0xFF3A2C22),
        defaultAccent = Color(0xFFF59E0B),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD4A373),
        textMuted = Color(0xFFA67C52),
        divider = Color(0xFF281D15),
        border = Color(0xFF3D2C20),
        pattern = ThemePattern.SUNSET_RAYS,
        badgeText = "SUNSET",
        patternLabel = "Закатные золотые лучи",
        cardBorderGradient = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)),
        illustrationKind = ThemeIllustrationKind.SUNSET_AMBER,
    )

    val CRIMSON = ThemePreset(
        id = "CRIMSON",
        name = "Crimson Moon",
        description = "Глубокий вампирский кармин и кровавая луна",
        background = Color(0xFF12080A),
        surface = Color(0xFF1D0D11),
        surfaceElevated = Color(0xFF2E141C),
        cardHover = Color(0xFF3E1B26),
        defaultAccent = Color(0xFFFF2A55),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFE292A3),
        textMuted = Color(0xFFA65063),
        divider = Color(0xFF2D121A),
        border = Color(0xFF461E2A),
        pattern = ThemePattern.CRIMSON_ECLIPSE,
        badgeText = "CRIMSON",
        patternLabel = "Лунное затмение и орбиты",
        cardBorderGradient = listOf(Color(0xFFFF2A55), Color(0xFF991B1B)),
        illustrationKind = ThemeIllustrationKind.CRIMSON_MOON,
    )

    val AMOLED_BLACK = ThemePreset(
        id = "AMOLED",
        name = "AMOLED Pitch Black",
        description = "Абсолютный чёрный 100% для максимального контраста",
        background = Color(0xFF000000),
        surface = Color(0xFF0C0C0C),
        surfaceElevated = Color(0xFF161616),
        cardHover = Color(0xFF222222),
        defaultAccent = Color(0xFFE2E8F0),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFB0B0B0),
        textMuted = Color(0xFF707070),
        divider = Color(0xFF181818),
        border = Color(0xFF222222),
        pattern = ThemePattern.NONE,
        badgeText = "OLED",
        patternLabel = "Чистый контраст без текстур",
        illustrationKind = ThemeIllustrationKind.AMOLED_MINIMAL,
    )

    val all: List<ThemePreset> = listOf(
        JVX_EMERALD,
        CYBERPUNK_VIOLET,
        RETRO_VINYL,
        DEEP_SPACE,
        NORDIC_FROST,
        ANIME_LOFI,
        PHANTOM,
        MIDNIGHT_BLUE,
        SUNSET_AMBER,
        CRIMSON,
        AMOLED_BLACK
    )

    val ALL: List<ThemePreset> get() = all

    fun fromId(id: String?): ThemePreset =
        all.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: JVX_EMERALD
}

/**
 * Curated accent swatches for quick color personalization.
 */
@Immutable
data class AccentOption(
    val id: String,
    val name: String,
    val color: Color,
    val hex: String
)

val DefaultAccentOptions: List<AccentOption> = listOf(
    AccentOption("EMERALD", "Изумруд", Color(0xFF00D26A), "#00D26A"),
    AccentOption("PHANTOM_WHITE", "Phantom White", Color(0xFFFFFFFF), "#FFFFFF"),
    AccentOption("ELECTRIC_CYAN", "Неон Циан", Color(0xFF00E5FF), "#00E5FF"),
    AccentOption("SKY_BLUE", "Небесный", Color(0xFF38BDF8), "#38BDF8"),
    AccentOption("NEON_FUCHSIA", "Неон Фуксия", Color(0xFFD946EF), "#D946EF"),
    AccentOption("SUNSET_AMBER", "Янтарь", Color(0xFFF59E0B), "#F59E0B"),
    AccentOption("CRIMSON_RED", "Красный", Color(0xFFFF2A55), "#FF2A55"),
    AccentOption("VIOLET", "Фиалка", Color(0xFF8B5CF6), "#8B5CF6"),
    AccentOption("LIME", "Лайм", Color(0xFFA3E635), "#A3E635"),
    AccentOption("SLATE", "Серебро", Color(0xFF94A3B8), "#94A3B8"),
)

fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length == 6) {
        val rgb = clean.toLongOrNull(16) ?: return null
        return Color(0xFF000000 or rgb)
    } else if (clean.length == 8) {
        val argb = clean.toLongOrNull(16) ?: return null
        return Color(argb)
    }
    return null
}

fun Color.toHexRgb(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    fun byteToHex(v: Int): String {
        val chars = "0123456789ABCDEF"
        return "${chars[(v shr 4) and 0x0F]}${chars[v and 0x0F]}"
    }
    return "#${byteToHex(r)}${byteToHex(g)}${byteToHex(b)}"
}

fun Color.lighten(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * f,
        green = green + (1f - green) * f,
        blue = blue + (1f - blue) * f,
        alpha = alpha
    )
}

fun Color.darken(factor: Float): Color {
    val f = (1f - factor.coerceIn(0f, 1f))
    return Color(
        red = red * f,
        green = green * f,
        blue = blue * f,
        alpha = alpha
    )
}

val DarkOnAccent: Color = Color(0xFF121212)
val LightOnAccent: Color = Color(0xFFFFFFFF)

/**
 * Calculates standard WCAG 2.1 relative luminance (L) for a given sRGB color:
 * L = 0.2126 * R + 0.7152 * G + 0.0722 * B
 * where linear sRGB component = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055) ^ 2.4.
 */
fun relativeLuminance(color: Color): Double {
    fun toLinear(c: Float): Double {
        val cd = c.toDouble()
        return if (cd <= 0.04045) {
            cd / 12.92
        } else {
            ((cd + 0.055) / 1.055).pow(2.4)
        }
    }
    val rLinear = toLinear(color.red)
    val gLinear = toLinear(color.green)
    val bLinear = toLinear(color.blue)
    return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
}

@kotlin.jvm.JvmName("colorRelativeLuminance")
fun Color.relativeLuminance(): Double = relativeLuminance(this)

/**
 * Contrast ratio between color C and white: (1.05) / (L + 0.05).
 */
fun contrastRatioWithWhite(color: Color): Double {
    val l = relativeLuminance(color)
    return 1.05 / (l + 0.05)
}

/**
 * Contrast ratio between color C and black: (L + 0.05) / 0.05.
 */
fun contrastRatioWithBlack(color: Color): Double {
    val l = relativeLuminance(color)
    return (l + 0.05) / 0.05
}

/**
 * General WCAG 2.1 contrast ratio between two colors: (L1 + 0.05) / (L2 + 0.05).
 */
fun contrastRatio(c1: Color, c2: Color): Double {
    val l1 = relativeLuminance(c1)
    val l2 = relativeLuminance(c2)
    val lighter = if (l1 >= l2) l1 else l2
    val darker = if (l1 < l2) l1 else l2
    return (lighter + 0.05) / (darker + 0.05)
}

@kotlin.jvm.JvmName("colorContrastRatioWith")
fun Color.contrastRatioWith(other: Color): Double = contrastRatio(this, other)

/**
 * WCAG 2.1 check: evaluates whether black text provides a strictly higher contrast ratio than white text.
 */
fun isLightColor(color: Color): Boolean {
    val whiteContrast = contrastRatioWithWhite(color)
    val blackContrast = contrastRatioWithBlack(color)
    return blackContrast > whiteContrast
}

/**
 * Chooses the text color (White or Black/Dark #121212) that provides the HIGHER contrast ratio
 * meeting or maximizing WCAG 2.1 contrast standards.
 */
fun contrastColorFor(
    color: Color,
    darkColor: Color = DarkOnAccent,
    lightColor: Color = LightOnAccent
): Color {
    val whiteContrast = contrastRatioWithWhite(color)
    val blackContrast = contrastRatioWithBlack(color)
    return if (blackContrast > whiteContrast) darkColor else lightColor
}

/**
 * JVXMusic reactive color palette strictly adhering to WCAG AA contrast standards.
 * Dynamically re-evaluates across the entire app whenever currentPalette or customAccent is updated.
 */
object AppColors {
    var currentPalette: ThemePreset by mutableStateOf(ThemePresets.JVX_EMERALD)
    var customAccent: Color? by mutableStateOf(null)
    var backdropEnabled: Boolean by mutableStateOf(true)

    val Background: Color get() = currentPalette.background
    val Surface: Color get() = currentPalette.surface
    val SurfaceElevated: Color get() = currentPalette.surfaceElevated
    val CardHover: Color get() = currentPalette.cardHover

    val Accent: Color get() = customAccent ?: currentPalette.defaultAccent
    val AccentHover: Color get() = Accent.lighten(0.12f)
    val AccentPressed: Color get() = Accent.darken(0.15f)
    val OnAccent: Color get() = contrastColorFor(Accent)

    val TextPrimary: Color get() = currentPalette.textPrimary
    val TextSecondary: Color get() = currentPalette.textSecondary
    val TextMuted: Color get() = currentPalette.textMuted

    val Divider: Color get() = currentPalette.divider
    val Border: Color get() = currentPalette.border

    val Error = Color(0xFFE91429)
    val ErrorHover = Color(0xFFFF3344)
    val Warning = Color(0xFFFFB300)

    val WindowBar: Color get() = currentPalette.windowBar
    val WindowButtonHover: Color get() = currentPalette.windowButtonHover
    val WindowCloseHover = Color(0xFFE81123)

    val ShimmerBase: Color get() = currentPalette.shimmerBase
    val ShimmerHighlight: Color get() = currentPalette.shimmerHighlight

    val CurrentPreset: ThemePreset get() = currentPalette
    val Pattern: ThemePattern get() = currentPalette.pattern
    val BadgeText: String get() = currentPalette.badgeText
    val BadgeColor: Color get() = currentPalette.badgeColor
    val CardBorderGradient: List<Color>? get() = currentPalette.cardBorderGradient
    val GradientOverlay: List<Color>? get() = currentPalette.gradientOverlay
    val AmbientGlow: Color? get() = currentPalette.ambientGlow
    val IllustrationKind: ThemeIllustrationKind get() = currentPalette.illustrationKind

    fun applyTheme(presetId: String, customAccentHex: String?, enableBackdrop: Boolean = true) {
        val preset = ThemePresets.fromId(presetId)
        currentPalette = preset
        customAccent = customAccentHex?.let { parseHexColor(it) }
        backdropEnabled = enableBackdrop
    }

    fun applyTheme(config: ThemeConfig) {
        applyTheme(config.presetId, config.customAccentHex, config.patternEnabled)
    }
}

/**
 * Procedural vector illustration for theme previews, mockups and artwork thumbnails.
 */
@Composable
fun ThemeIllustration(
    kind: ThemeIllustrationKind,
    modifier: Modifier = Modifier,
    accent: Color = AppColors.Accent
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (kind) {
            ThemeIllustrationKind.CYBERPUNK_SYNTHWAVE -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E0A3C), Color(0xFF0D031A), Color(0xFF07000F))
                    )
                )
                val sunR = (w * 0.22f).coerceAtMost(h * 0.35f)
                val sunCenter = Offset(w * 0.5f, h * 0.45f)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF007A), Color(0xFFFF8A00)),
                        startY = sunCenter.y - sunR,
                        endY = sunCenter.y + sunR
                    ),
                    radius = sunR,
                    center = sunCenter
                )
                val slitCount = 4
                for (i in 1..slitCount) {
                    val slitY = sunCenter.y + (sunR * (i.toFloat() / (slitCount + 1)))
                    val slitH = (h * 0.025f * (i.toFloat() / slitCount)).coerceAtLeast(1.5f)
                    drawRect(
                        color = Color(0xFF0D031A),
                        topLeft = Offset(sunCenter.x - sunR, slitY),
                        size = Size(sunR * 2f, slitH)
                    )
                }
                val horizonY = h * 0.58f
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(0f, horizonY),
                    end = Offset(w, horizonY),
                    strokeWidth = 2f
                )
                val vanishX = w * 0.5f
                val rayCount = 8
                for (i in 0..rayCount) {
                    val bottomX = (w / rayCount) * i
                    drawLine(
                        color = Color(0xFFD946EF).copy(alpha = 0.55f),
                        start = Offset(vanishX, horizonY),
                        end = Offset(bottomX, h),
                        strokeWidth = 1.2f
                    )
                }
                val hLines = 5
                for (i in 1..hLines) {
                    val progress = (i.toFloat() / hLines)
                    val curved = progress * progress
                    val lineY = horizonY + (h - horizonY) * curved
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.35f + 0.35f * progress),
                        start = Offset(0f, lineY),
                        end = Offset(w, lineY),
                        strokeWidth = 1f
                    )
                }
            }

            ThemeIllustrationKind.RETRO_VINYL -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF221A15), Color(0xFF130E0B))
                    )
                )
                val discCenter = Offset(w * 0.45f, h * 0.5f)
                val discR = (w * 0.38f).coerceAtMost(h * 0.44f)
                drawCircle(color = Color(0xFF121212), radius = discR, center = discCenter)
                val grooveSteps = 5
                for (i in 1..grooveSteps) {
                    val gr = discR * (0.42f + 0.52f * (i.toFloat() / grooveSteps))
                    drawCircle(
                        color = Color(0xFFE5A93C).copy(alpha = 0.18f),
                        radius = gr,
                        center = discCenter,
                        style = Stroke(width = 1f)
                    )
                }
                val labelR = discR * 0.36f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
                        center = discCenter,
                        radius = labelR
                    ),
                    radius = labelR,
                    center = discCenter
                )
                drawCircle(color = Color(0xFF0A0A0A), radius = labelR * 0.22f, center = discCenter)
                val armPivot = Offset(w * 0.88f, h * 0.18f)
                val armElbow = Offset(w * 0.68f, h * 0.42f)
                val armHead = Offset(discCenter.x + discR * 0.72f, discCenter.y - discR * 0.25f)
                drawLine(color = Color(0xFFD4AF37), start = armPivot, end = armElbow, strokeWidth = 2.5f)
                drawLine(color = Color(0xFFE2E8F0), start = armElbow, end = armHead, strokeWidth = 2f)
                drawCircle(color = Color(0xFFD4AF37), radius = 3.5f, center = armPivot)
                drawCircle(color = Color(0xFFFF2A55), radius = 2.5f, center = armHead)
            }

            ThemeIllustrationKind.DEEP_SPACE_NEBULA -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF090A1A), Color(0xFF030308)),
                        center = Offset(w * 0.35f, h * 0.4f),
                        radius = w * 0.8f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF818CF8).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(w * 0.4f, h * 0.45f),
                        radius = w * 0.35f
                    ),
                    radius = w * 0.35f,
                    center = Offset(w * 0.4f, h * 0.45f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC084FC).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(w * 0.7f, h * 0.3f),
                        radius = w * 0.25f
                    ),
                    radius = w * 0.25f,
                    center = Offset(w * 0.7f, h * 0.3f)
                )
                val planetCenter = Offset(w * 0.28f, h * 0.38f)
                val planetR = (w * 0.12f).coerceAtMost(h * 0.2f)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF312E81)),
                        start = Offset(planetCenter.x - planetR, planetCenter.y - planetR),
                        end = Offset(planetCenter.x + planetR, planetCenter.y + planetR)
                    ),
                    radius = planetR,
                    center = planetCenter
                )
                drawOval(
                    color = Color(0xFF818CF8).copy(alpha = 0.65f),
                    topLeft = Offset(planetCenter.x - planetR * 1.8f, planetCenter.y - planetR * 0.45f),
                    size = Size(planetR * 3.6f, planetR * 0.9f),
                    style = Stroke(width = 1.5f)
                )
                fun drawSparkle(center: Offset, size: Float, color: Color) {
                    drawLine(color, Offset(center.x - size, center.y), Offset(center.x + size, center.y), 1.5f)
                    drawLine(color, Offset(center.x, center.y - size), Offset(center.x, center.y + size), 1.5f)
                    drawCircle(Color.White, size * 0.25f, center)
                }
                drawSparkle(Offset(w * 0.72f, h * 0.28f), 6f, Color(0xFF818CF8))
                drawSparkle(Offset(w * 0.82f, h * 0.68f), 4.5f, Color(0xFFC084FC))
                drawSparkle(Offset(w * 0.52f, h * 0.75f), 3.5f, Color(0xFF38BDF8))
                val starPositions = listOf(
                    0.15f to 0.2f, 0.45f to 0.15f, 0.6f to 0.55f, 0.88f to 0.2f,
                    0.2f to 0.8f, 0.68f to 0.85f, 0.9f to 0.5f, 0.35f to 0.65f
                )
                starPositions.forEach { (rx, ry) ->
                    drawCircle(Color.White.copy(alpha = 0.75f), radius = 1.2f, center = Offset(w * rx, h * ry))
                }
            }

            ThemeIllustrationKind.NORDIC_FROST -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF04171E), Color(0xFF08222C), Color(0xFF0F323F))
                    )
                )
                val path1 = Path().apply {
                    moveTo(0f, h * 0.35f)
                    cubicTo(w * 0.25f, h * 0.15f, w * 0.55f, h * 0.45f, w, h * 0.2f)
                    lineTo(w, h * 0.45f)
                    cubicTo(w * 0.6f, h * 0.6f, w * 0.3f, h * 0.3f, 0f, h * 0.5f)
                    close()
                }
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2DD4BF).copy(alpha = 0.4f),
                            Color(0xFF00D26A).copy(alpha = 0.35f),
                            Color(0xFF38BDF8).copy(alpha = 0.45f)
                        )
                    )
                )
                val mountainPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.75f)
                    lineTo(w * 0.22f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.72f)
                    lineTo(w * 0.62f, h * 0.45f)
                    lineTo(w * 0.82f, h * 0.68f)
                    lineTo(w, h * 0.58f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = mountainPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF102833), Color(0xFF08151B))
                    )
                )
                val snowPeak1 = Path().apply {
                    moveTo(w * 0.62f, h * 0.45f)
                    lineTo(w * 0.57f, h * 0.53f)
                    lineTo(w * 0.62f, h * 0.51f)
                    lineTo(w * 0.67f, h * 0.54f)
                    close()
                }
                drawPath(snowPeak1, color = Color(0xFFE0F2FE).copy(alpha = 0.85f))
                val cx = w * 0.82f
                val cy = h * 0.24f
                drawLine(Color(0xFFE0F2FE), Offset(cx - 5f, cy), Offset(cx + 5f, cy), 1.2f)
                drawLine(Color(0xFFE0F2FE), Offset(cx, cy - 5f), Offset(cx, cy + 5f), 1.2f)
                drawLine(Color(0xFFE0F2FE), Offset(cx - 3.5f, cy - 3.5f), Offset(cx + 3.5f, cy + 3.5f), 1f)
                drawLine(Color(0xFFE0F2FE), Offset(cx - 3.5f, cy + 3.5f), Offset(cx + 3.5f, cy - 3.5f), 1f)
            }

            ThemeIllustrationKind.ANIME_LOFI -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C103F), Color(0xFF5B1B54), Color(0xFFFB7185), Color(0xFFFDBA74))
                    )
                )
                val sunR = (w * 0.22f).coerceAtMost(h * 0.35f)
                val sunCenter = Offset(w * 0.68f, h * 0.42f)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF1F2), Color(0xFFFBBF24)),
                        startY = sunCenter.y - sunR,
                        endY = sunCenter.y + sunR
                    ),
                    radius = sunR,
                    center = sunCenter
                )
                val bldgColor = Color(0xFF180A22)
                val bldgs = listOf(
                    0.05f to (h * 0.45f), 0.16f to (h * 0.55f), 0.28f to (h * 0.38f),
                    0.42f to (h * 0.62f), 0.54f to (h * 0.48f), 0.68f to (h * 0.68f),
                    0.80f to (h * 0.52f), 0.92f to (h * 0.42f)
                )
                bldgs.forEachIndexed { idx, (rx, topY) ->
                    val bw = w * 0.12f
                    drawRect(color = bldgColor, topLeft = Offset(w * rx - bw * 0.5f, topY), size = Size(bw, h - topY))
                    if (idx == 2 || idx == 7) {
                        drawLine(color = Color(0xFFFB7185), start = Offset(w * rx, topY - 6f), end = Offset(w * rx, topY), strokeWidth = 1f)
                    }
                }
                val petals = listOf(Offset(w * 0.2f, h * 0.25f), Offset(w * 0.35f, h * 0.38f), Offset(w * 0.12f, h * 0.65f), Offset(w * 0.5f, h * 0.2f))
                petals.forEach { p ->
                    drawOval(
                        color = Color(0xFFFFB6C1).copy(alpha = 0.8f),
                        topLeft = Offset(p.x - 3f, p.y - 1.8f),
                        size = Size(6f, 3.6f)
                    )
                }
            }

            ThemeIllustrationKind.EMERALD_STUDIO -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1E16), Color(0xFF09120D))
                    )
                )
                val barCount = 9
                val barW = (w / (barCount * 1.8f)).coerceAtLeast(3f)
                val heights = listOf(0.35f, 0.6f, 0.85f, 0.45f, 0.95f, 0.7f, 0.5f, 0.78f, 0.4f)
                val baseY = h * 0.82f
                heights.forEachIndexed { i, frac ->
                    val bx = (w * 0.1f) + i * (w * 0.8f / barCount)
                    val barH = (h * 0.6f) * frac
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF00D26A), Color(0xFF059669)),
                            startY = baseY - barH,
                            endY = baseY
                        ),
                        topLeft = Offset(bx, baseY - barH),
                        size = Size(barW, barH)
                    )
                }
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.5f)
                    cubicTo(w * 0.25f, h * 0.25f, w * 0.45f, h * 0.75f, w * 0.7f, h * 0.35f)
                    cubicTo(w * 0.85f, h * 0.15f, w * 0.95f, h * 0.45f, w, h * 0.5f)
                }
                drawPath(wavePath, color = Color(0xFF34D399), style = Stroke(width = 1.8f))
            }

            ThemeIllustrationKind.PHANTOM_NOIR -> {
                drawRect(color = Color(0xFF0A0A0A))
                val cx = w * 0.5f
                val cy = h * 0.5f
                val s = (w * 0.26f).coerceAtMost(h * 0.32f)
                val topPath = Path().apply {
                    moveTo(cx, cy - s)
                    lineTo(cx + s * 0.866f, cy - s * 0.5f)
                    lineTo(cx, cy)
                    lineTo(cx - s * 0.866f, cy - s * 0.5f)
                    close()
                }
                drawPath(topPath, color = Color(0xFF202020))
                drawPath(topPath, color = Color(0xFFF3F4F6), style = Stroke(width = 1.2f))
                val leftPath = Path().apply {
                    moveTo(cx - s * 0.866f, cy - s * 0.5f)
                    lineTo(cx, cy)
                    lineTo(cx, cy + s)
                    lineTo(cx - s * 0.866f, cy + s * 0.5f)
                    close()
                }
                drawPath(leftPath, color = Color(0xFF141414))
                drawPath(leftPath, color = Color(0xFF9CA3AF), style = Stroke(width = 1.2f))
                val rightPath = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + s * 0.866f, cy - s * 0.5f)
                    lineTo(cx + s * 0.866f, cy + s * 0.5f)
                    lineTo(cx, cy + s)
                    close()
                }
                drawPath(rightPath, color = Color(0xFF1A1A1A))
                drawPath(rightPath, color = Color(0xFF6B7280), style = Stroke(width = 1.2f))
                val pad = 6f
                val bl = 8f
                drawLine(Color(0xFFE5E7EB), Offset(pad, pad), Offset(pad + bl, pad), 1.5f)
                drawLine(Color(0xFFE5E7EB), Offset(pad, pad), Offset(pad, pad + bl), 1.5f)
                drawLine(Color(0xFFE5E7EB), Offset(w - pad, h - pad), Offset(w - pad - bl, h - pad), 1.5f)
                drawLine(Color(0xFFE5E7EB), Offset(w - pad, h - pad), Offset(w - pad, h - pad - bl), 1.5f)
            }

            ThemeIllustrationKind.MIDNIGHT_OCEAN -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF070F22), Color(0xFF0D1D3A), Color(0xFF0F2F5A))
                    )
                )
                drawCircle(color = Color(0xFFBAE6FD), radius = w * 0.12f, center = Offset(w * 0.78f, h * 0.3f))
                drawCircle(color = Color(0xFF070F22), radius = w * 0.11f, center = Offset(w * 0.74f, h * 0.28f))
                val wave1 = Path().apply {
                    moveTo(0f, h * 0.65f)
                    cubicTo(w * 0.25f, h * 0.55f, w * 0.5f, h * 0.75f, w * 0.75f, h * 0.6f)
                    cubicTo(w * 0.88f, h * 0.52f, w * 0.95f, h * 0.62f, w, h * 0.58f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(wave1, color = Color(0xFF0369A1).copy(alpha = 0.5f))
                val wave2 = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(w * 0.3f, h * 0.85f, w * 0.6f, h * 0.7f, w, h * 0.76f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(wave2, color = Color(0xFF38BDF8).copy(alpha = 0.4f))
            }

            ThemeIllustrationKind.CRIMSON_MOON -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E080D), Color(0xFF0F0407))
                    )
                )
                val moonCenter = Offset(w * 0.5f, h * 0.5f)
                val moonR = (w * 0.25f).coerceAtMost(h * 0.35f)
                drawCircle(color = Color(0xFFFF2A55).copy(alpha = 0.25f), radius = moonR * 1.35f, center = moonCenter, style = Stroke(width = 1.5f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF2A55), Color(0xFF991B1B)),
                        center = moonCenter,
                        radius = moonR
                    ),
                    radius = moonR,
                    center = moonCenter
                )
                drawCircle(color = Color(0xFF0F0407), radius = moonR * 0.88f, center = Offset(moonCenter.x - moonR * 0.35f, moonCenter.y - moonR * 0.2f))
            }

            ThemeIllustrationKind.SUNSET_AMBER -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF261608), Color(0xFF150C04))
                    )
                )
                val sunCenter = Offset(w * 0.5f, h * 0.8f)
                val sunR = (w * 0.22f).coerceAtMost(h * 0.32f)
                for (angle in -70..70 step 20) {
                    val rad = Math.toRadians(angle.toDouble() - 90.0)
                    val endX = sunCenter.x + (w * 0.9f * cos(rad)).toFloat()
                    val endY = sunCenter.y + (h * 0.9f * sin(rad)).toFloat()
                    drawLine(Color(0xFFF59E0B).copy(alpha = 0.3f), sunCenter, Offset(endX, endY), 1.5f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                        center = sunCenter,
                        radius = sunR
                    ),
                    radius = sunR,
                    center = sunCenter
                )
            }

            ThemeIllustrationKind.AMOLED_MINIMAL, ThemeIllustrationKind.DEFAULT -> {
                drawRect(color = Color(0xFF050505))
                val ringCenter = Offset(w * 0.5f, h * 0.5f)
                val ringR = (w * 0.24f).coerceAtMost(h * 0.34f)
                drawCircle(color = accent.copy(alpha = 0.8f), radius = ringR, center = ringCenter, style = Stroke(width = 1.5f))
                drawCircle(color = accent, radius = 3f, center = ringCenter)
            }
        }
    }
}

/**
 * Ambient backdrop pattern renderer with subtle alpha overlay for app background and preview cards.
 */
@Composable
fun ThemeBackdrop(
    pattern: ThemePattern,
    modifier: Modifier = Modifier,
    accent: Color = AppColors.Accent
) {
    if (pattern == ThemePattern.NONE) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (pattern) {
            ThemePattern.CYBER_GRID -> {
                val horizonY = (h * 0.65f).coerceAtLeast(h - 280f)
                val vanishX = w * 0.5f
                val rayCount = 14
                for (i in 0..rayCount) {
                    val bottomX = (w / rayCount) * i
                    drawLine(
                        color = accent.copy(alpha = 0.09f),
                        start = Offset(vanishX, horizonY),
                        end = Offset(bottomX, h),
                        strokeWidth = 1f
                    )
                }
                val gridLines = 8
                for (i in 1..gridLines) {
                    val p = i.toFloat() / gridLines
                    val lineY = horizonY + (h - horizonY) * (p * p)
                    drawLine(
                        color = accent.copy(alpha = 0.05f + 0.08f * p),
                        start = Offset(0f, lineY),
                        end = Offset(w, lineY),
                        strokeWidth = 1f
                    )
                }
                val scanStep = 24f
                var curY = 0f
                while (curY < h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.015f),
                        start = Offset(0f, curY),
                        end = Offset(w, curY),
                        strokeWidth = 1f
                    )
                    curY += scanStep
                }
            }

            ThemePattern.RETRO_VINYL -> {
                val center = Offset(w * 0.95f, -h * 0.1f)
                val maxR = (w * 1.2f).coerceAtLeast(h * 1.2f)
                var r = 120f
                while (r < maxR) {
                    drawCircle(
                        color = accent.copy(alpha = 0.045f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1f)
                    )
                    r += 38f
                }
            }

            ThemePattern.NEBULA_STARS -> {
                val starSeeds = listOf(
                    0.08f to 0.12f, 0.22f to 0.35f, 0.45f to 0.18f, 0.62f to 0.42f,
                    0.85f to 0.15f, 0.92f to 0.55f, 0.15f to 0.72f, 0.38f to 0.85f,
                    0.72f to 0.78f, 0.88f to 0.88f, 0.32f to 0.52f, 0.55f to 0.7f,
                    0.78f to 0.28f, 0.05f to 0.45f, 0.95f to 0.35f, 0.5f to 0.95f
                )
                starSeeds.forEachIndexed { i, (rx, ry) ->
                    val sx = w * rx
                    val sy = h * ry
                    val isSparkle = i % 4 == 0
                    if (isSparkle) {
                        val sl = 5f
                        drawLine(accent.copy(alpha = 0.35f), Offset(sx - sl, sy), Offset(sx + sl, sy), 1f)
                        drawLine(accent.copy(alpha = 0.35f), Offset(sx, sy - sl), Offset(sx, sy + sl), 1f)
                        drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.5f, center = Offset(sx, sy))
                    } else {
                        val radius = if (i % 3 == 0) 2f else 1.2f
                        drawCircle(Color.White.copy(alpha = 0.45f), radius = radius, center = Offset(sx, sy))
                    }
                }
            }

            ThemePattern.NORDIC_FROST -> {
                val ribbon = Path().apply {
                    moveTo(0f, h * 0.12f)
                    cubicTo(w * 0.3f, h * 0.04f, w * 0.6f, h * 0.22f, w, h * 0.08f)
                    lineTo(w, h * 0.25f)
                    cubicTo(w * 0.65f, h * 0.35f, w * 0.35f, h * 0.15f, 0f, h * 0.28f)
                    close()
                }
                drawPath(ribbon, color = accent.copy(alpha = 0.065f))
            }

            ThemePattern.ANIME_LOFI -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, accent.copy(alpha = 0.045f)),
                        startY = h * 0.5f,
                        endY = h
                    )
                )
                val seeds = listOf(0.18f to 0.22f, 0.32f to 0.45f, 0.65f to 0.35f, 0.82f to 0.6f, 0.48f to 0.75f)
                seeds.forEach { (rx, ry) ->
                    drawOval(
                        color = accent.copy(alpha = 0.2f),
                        topLeft = Offset(w * rx, h * ry),
                        size = Size(8f, 5f)
                    )
                }
            }

            ThemePattern.EMERALD_WAVES -> {
                val wave1 = Path().apply {
                    moveTo(0f, h * 0.82f)
                    cubicTo(w * 0.25f, h * 0.75f, w * 0.55f, h * 0.9f, w, h * 0.8f)
                }
                drawPath(wave1, color = accent.copy(alpha = 0.08f), style = Stroke(width = 1.5f))
                val wave2 = Path().apply {
                    moveTo(0f, h * 0.88f)
                    cubicTo(w * 0.35f, h * 0.95f, w * 0.7f, h * 0.82f, w, h * 0.92f)
                }
                drawPath(wave2, color = accent.copy(alpha = 0.05f), style = Stroke(width = 1f))
            }

            ThemePattern.OCEAN_WAVES -> {
                val wave = Path().apply {
                    moveTo(0f, h * 0.85f)
                    cubicTo(w * 0.3f, h * 0.8f, w * 0.6f, h * 0.92f, w, h * 0.84f)
                }
                drawPath(wave, color = accent.copy(alpha = 0.07f), style = Stroke(width = 1.5f))
            }

            ThemePattern.CRIMSON_ECLIPSE -> {
                val center = Offset(w * 0.9f, h * 0.2f)
                drawCircle(color = accent.copy(alpha = 0.05f), radius = 180f, center = center, style = Stroke(width = 1f))
                drawCircle(color = accent.copy(alpha = 0.035f), radius = 260f, center = center, style = Stroke(width = 1f))
            }

            ThemePattern.SUNSET_RAYS -> {
                val center = Offset(w * 0.5f, h)
                for (a in -60..60 step 30) {
                    val rad = Math.toRadians(a.toDouble() - 90.0)
                    val ex = center.x + (w * cos(rad)).toFloat()
                    val ey = center.y + (h * sin(rad)).toFloat()
                    drawLine(accent.copy(alpha = 0.04f), center, Offset(ex, ey), 1.2f)
                }
            }

            ThemePattern.PHANTOM_MATRIX -> {
                val pad = 24f
                val bl = 16f
                drawLine(Color.White.copy(alpha = 0.1f), Offset(pad, pad), Offset(pad + bl, pad), 1f)
                drawLine(Color.White.copy(alpha = 0.1f), Offset(pad, pad), Offset(pad, pad + bl), 1f)
                drawLine(Color.White.copy(alpha = 0.1f), Offset(w - pad, h - pad), Offset(w - pad - bl, h - pad), 1f)
                drawLine(Color.White.copy(alpha = 0.1f), Offset(w - pad, h - pad), Offset(w - pad, h - pad - bl), 1f)
            }

            ThemePattern.NONE -> {}
        }
    }
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
        color = Color.White,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = Color.White,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = Color.White,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = Color.White,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color.White,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        color = Color.White,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color(0xFFB3B3B3),
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
        color = Color(0xFFB3B3B3),
    )
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

fun neutralCoverBrush(): Brush =
    Brush.verticalGradient(listOf(AppColors.SurfaceElevated, AppColors.Surface))

@Composable
fun AppTheme(
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
) {
    val dynamicColorScheme = darkColorScheme(
        primary = AppColors.Accent,
        onPrimary = AppColors.OnAccent,
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

    CompositionLocalProvider(LocalSpacing provides spacing) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
