package com.example.musicapp.theme

import androidx.compose.ui.graphics.Color
import com.example.musicapp.settings.AppSettings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeTest {

    @Test
    fun testAllThemePresetsAreDefinedAndValid() {
        val presets = ThemePresets.all
        assertTrue(presets.isNotEmpty(), "Presets should not be empty")

        // Ensure key themes are present
        assertNotNull(presets.find { it.id == "PHANTOM" })
        assertNotNull(presets.find { it.id == "JVX_DEFAULT" })
        assertNotNull(presets.find { it.id == "MIDNIGHT_BLUE" })
        assertNotNull(presets.find { it.id == "CYBERPUNK" })
        assertNotNull(presets.find { it.id == "SUNSET_AMBER" })
        assertNotNull(presets.find { it.id == "CRIMSON" })
        assertNotNull(presets.find { it.id == "AMOLED" })
        assertNotNull(presets.find { it.id == "RETRO_VINYL" })
        assertNotNull(presets.find { it.id == "DEEP_SPACE" })
        assertNotNull(presets.find { it.id == "NORDIC_FROST" })
        assertNotNull(presets.find { it.id == "ANIME_LOFI" })

        // Check color integrity and visual metadata
        presets.forEach { p ->
            assertTrue(p.name.isNotBlank(), "Preset name should not be blank")
            assertTrue(p.background.alpha > 0f)
            assertTrue(p.surface.alpha > 0f)
            assertTrue(p.defaultAccent.alpha > 0f)
            assertNotNull(p.pattern, "Preset pattern should not be null")
            assertNotNull(p.illustrationKind, "Preset illustrationKind should not be null")
            assertTrue(p.badgeText.isNotBlank(), "Preset badgeText should not be blank")
        }
    }

    @Test
    fun testThemePresetFromIdLookup() {
        assertEquals(ThemePresets.PHANTOM, ThemePresets.fromId("PHANTOM"))
        assertEquals(ThemePresets.PHANTOM, ThemePresets.fromId("phantom"))
        assertEquals(ThemePresets.JVX_EMERALD, ThemePresets.fromId("JVX_DEFAULT"))
        assertEquals(ThemePresets.CYBERPUNK_VIOLET, ThemePresets.fromId("CYBERPUNK"))
        assertEquals(ThemePresets.RETRO_VINYL, ThemePresets.fromId("RETRO_VINYL"))
        assertEquals(ThemePresets.DEEP_SPACE, ThemePresets.fromId("DEEP_SPACE"))
        assertEquals(ThemePresets.NORDIC_FROST, ThemePresets.fromId("NORDIC_FROST"))
        assertEquals(ThemePresets.ANIME_LOFI, ThemePresets.fromId("ANIME_LOFI"))
        assertEquals(ThemePresets.JVX_EMERALD, ThemePresets.fromId("unknown_id"))
        assertEquals(ThemePresets.JVX_EMERALD, ThemePresets.fromId(null))
    }

    @Test
    fun testThemeConfigSerialization() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val config = ThemeConfig(
            presetId = "CYBERPUNK",
            customAccentHex = "#00E5FF",
            patternEnabled = true,
            patternStyle = "CYBER_GRID",
            ambientGlowEnabled = true
        )
        val encoded = json.encodeToString(config)
        assertTrue(encoded.contains("\"presetId\":\"CYBERPUNK\""))
        assertTrue(encoded.contains("\"patternEnabled\":true"))

        val decoded = json.decodeFromString<ThemeConfig>(encoded)
        assertEquals("CYBERPUNK", decoded.presetId)
        assertEquals("#00E5FF", decoded.customAccentHex)
        assertTrue(decoded.patternEnabled)

        // Backwards compatibility with empty json
        val defaultDecoded = json.decodeFromString<ThemeConfig>("{}")
        assertEquals("JVX_DEFAULT", defaultDecoded.presetId)
        assertNull(defaultDecoded.customAccentHex)
        assertTrue(defaultDecoded.patternEnabled)
    }

    @Test
    fun testAppColorsApplyTheme() {
        // Apply Phantom
        AppColors.applyTheme("PHANTOM", null)
        assertEquals(ThemePresets.PHANTOM.background, AppColors.Background)
        assertEquals(ThemePresets.PHANTOM.surface, AppColors.Surface)
        assertEquals(ThemePresets.PHANTOM.defaultAccent, AppColors.Accent)

        // Apply Custom Accent
        AppColors.applyTheme("PHANTOM", "#00E5FF")
        assertEquals(Color(0xFF00E5FF), AppColors.Accent)

        // Revert to Default
        AppColors.applyTheme("JVX_DEFAULT", null)
        assertEquals(ThemePresets.JVX_EMERALD.background, AppColors.Background)
        assertEquals(ThemePresets.JVX_EMERALD.defaultAccent, AppColors.Accent)
    }

    @Test
    fun testParseHexColor() {
        assertEquals(Color(0xFF00D26A), parseHexColor("#00D26A"))
        assertEquals(Color(0xFF00D26A), parseHexColor("00D26A"))
        assertEquals(Color(0xFFFFFFFF), parseHexColor("#FFFFFF"))
        assertEquals(Color(0xFF000000), parseHexColor("#000000"))
        assertEquals(Color(0x80FF0000), parseHexColor("#80FF0000"))

        assertNull(parseHexColor(""))
        assertNull(parseHexColor("   "))
        assertNull(parseHexColor("#123"))
        assertNull(parseHexColor("#12345"))
        assertNull(parseHexColor("ZZZZZZ"))
    }

    @Test
    fun testColorToHexRgb() {
        assertEquals("#00D26A", Color(0xFF00D26A).toHexRgb())
        assertEquals("#FFFFFF", Color.White.toHexRgb())
        assertEquals("#000000", Color.Black.toHexRgb())
    }

    @Test
    fun testColorLightenAndDarken() {
        val base = Color(0xFF808080)
        val lighter = base.lighten(0.2f)
        assertTrue(lighter.red > base.red)
        assertTrue(lighter.green > base.green)
        assertTrue(lighter.blue > base.blue)

        val darker = base.darken(0.2f)
        assertTrue(darker.red < base.red)
        assertTrue(darker.green < base.green)
        assertTrue(darker.blue < base.blue)
    }

    @Test
    fun testAppSettingsThemeSerialization() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
        val settings = AppSettings(
            themePresetId = "PHANTOM",
            customAccentHex = "#00E5FF",
            playerViewMode = "CLASSIC"
        )

        val serialized = json.encodeToString(settings)
        assertTrue(serialized.contains("\"themePresetId\": \"PHANTOM\""))
        assertTrue(serialized.contains("\"customAccentHex\": \"#00E5FF\""))
        assertTrue(serialized.contains("\"playerViewMode\": \"CLASSIC\""))

        val deserialized = json.decodeFromString<AppSettings>(serialized)
        assertEquals("PHANTOM", deserialized.themePresetId)
        assertEquals("#00E5FF", deserialized.customAccentHex)
        assertEquals("CLASSIC", deserialized.playerViewMode)
    }

    @Test
    fun testAppSettingsBackwardsCompatibility() {
        val json = Json { ignoreUnknownKeys = true }
        val legacyJson = """
            {
                "volume": 0.5,
                "shuffle": true
            }
        """.trimIndent()

        val parsed = json.decodeFromString<AppSettings>(legacyJson)
        assertEquals("JVX_DEFAULT", parsed.themePresetId)
        assertNull(parsed.customAccentHex)
        assertEquals("PHANTOM_DASHBOARD", parsed.playerViewMode)
    }
}
