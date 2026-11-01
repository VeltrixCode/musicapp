package com.example.musicapp.player

import com.example.musicapp.platform.PlayerEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EqualizerControllerTest {

    private class MockEqualizerEngine : PlayerEngine {
        var isEnabled = true
        val bandGains = FloatArray(10) { 0f }

        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {}
        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit, onError: ((String) -> Unit)?) {}
        override fun play() {}
        override fun pause() {}
        override fun seekTo(ms: Long) {}
        override fun setVolume(volume: Float) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
            if (bandIndex in bandGains.indices) {
                bandGains[bandIndex] = gainDb
            }
        }
        override fun setEqualizerEnabled(enabled: Boolean) {
            isEnabled = enabled
        }
        override fun getEqualizerBandCount(): Int = 10
        override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = -20f..20f
        override fun getEqualizerBandFrequencies(): List<Int> = listOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000)
        override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}
        override fun positionMs(): Long = 0L
        override fun durationMs(): Long = 0L
        override fun release() {}
    }

    @Test
    fun testEqualizerEnableToggleAndReset() {
        val mockEngine = MockEqualizerEngine()
        val controller = PlayerController(customEngine = mockEngine)

        try {
            // Enable / Disable switch
            controller.setEqualizerEnabled(false)
            assertFalse(controller.isEqualizerEnabled)
            assertFalse(mockEngine.isEnabled)

            controller.setEqualizerEnabled(true)
            assertTrue(controller.isEqualizerEnabled)
            assertTrue(mockEngine.isEnabled)

            // Adjust bands with range clamping
            controller.setEqualizerBand(0, 25f) // Should clamp to +20f
            controller.setEqualizerBand(1, -30f) // Should clamp to -20f
            assertEquals(20f, controller.equalizerGains[0])
            assertEquals(-20f, controller.equalizerGains[1])
            assertEquals(20f, mockEngine.bandGains[0])
            assertEquals(-20f, mockEngine.bandGains[1])

            // Reset equalizer
            controller.resetEqualizer()
            for (i in 0 until 10) {
                assertEquals(0f, controller.equalizerGains[i])
                assertEquals(0f, mockEngine.bandGains[i])
            }
            assertEquals(EqualizerPreset.Flat, controller.equalizerPreset)
        } finally {
            controller.release()
        }
    }

    @Test
    fun testEqualizerPresets() {
        val mockEngine = MockEqualizerEngine()
        val controller = PlayerController(customEngine = mockEngine)

        try {
            controller.setEqualizerPreset(EqualizerPreset.BassBoost)
            assertEquals("Усиление баса", controller.equalizerPreset.title)
            assertEquals(6f, controller.equalizerGains[0])

            controller.setEqualizerPreset(EqualizerPreset.Rock)
            assertEquals("Рок", controller.equalizerPreset.title)
            assertEquals(4f, controller.equalizerGains[0])

            controller.setEqualizerPreset(EqualizerPreset.Pop)
            assertEquals("Поп", controller.equalizerPreset.title)

            controller.setEqualizerPreset(EqualizerPreset.Vocal)
            assertEquals("Вокал", controller.equalizerPreset.title)

            controller.setEqualizerPreset(EqualizerPreset.Classical)
            assertEquals("Классика", controller.equalizerPreset.title)
        } finally {
            controller.release()
        }
    }
}
