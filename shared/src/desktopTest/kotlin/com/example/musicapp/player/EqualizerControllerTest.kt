package com.example.musicapp.player

import com.example.musicapp.platform.PlayerEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EqualizerControllerTest {

    private class MockEqualizerEngine : PlayerEngine {
        var isEnabled = true
        var preamp = 0f
        val bandGains = FloatArray(10) { 0f }

        override val supportsSpectrum: Boolean = false
        override val supportsPreamp: Boolean = true

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
        override fun setEqualizerPreamp(gainDb: Float) {
            preamp = gainDb.coerceIn(-20f, 20f)
        }
        override fun getEqualizerPreamp(): Float = preamp
        override fun getEqualizerBandCount(): Int = 10
        override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = -20f..20f
        override fun getEqualizerBandFrequencies(): List<Int> = listOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000)
        override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}
        override fun positionMs(): Long = 0L
        override fun durationMs(): Long = 0L
        override fun release() {}
    }

    private class MockJavaFxStyleEngine : PlayerEngine {
        override val supportsSpectrum: Boolean = true
        override val supportsPreamp: Boolean = false

        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {}
        override fun play() {}
        override fun pause() {}
        override fun seekTo(ms: Long) {}
        override fun setVolume(volume: Float) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {}
        override fun getEqualizerBandCount(): Int = 10
        override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = -24f..12f
        override fun getEqualizerBandFrequencies(): List<Int> = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}
        override fun positionMs(): Long = 0L
        override fun durationMs(): Long = 0L
        override fun release() {}
    }

    @Test
    fun testDynamicEngineEqualizerAndPreampReconfiguration() {
        val vlcEngine = MockEqualizerEngine()
        val vlcController = PlayerController(customEngine = vlcEngine)
        try {
            assertFalse(vlcController.supportsSpectrum)
            assertTrue(vlcController.supportsPreamp)
            assertEquals(-20f..20f, vlcController.getEqualizerGainRange())
            assertEquals(listOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000), vlcController.getEqualizerBandFrequencies())

            vlcController.setEqualizerPreamp(5.5f)
            assertEquals(5.5f, vlcController.equalizerPreamp)
            assertEquals(5.5f, vlcEngine.preamp)
        } finally {
            vlcController.release()
        }

        val fxEngine = MockJavaFxStyleEngine()
        val fxController = PlayerController(customEngine = fxEngine)
        try {
            assertTrue(fxController.supportsSpectrum)
            assertFalse(fxController.supportsPreamp)
            assertEquals(-24f..12f, fxController.getEqualizerGainRange())
            assertEquals(listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000), fxController.getEqualizerBandFrequencies())
        } finally {
            fxController.release()
        }
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
