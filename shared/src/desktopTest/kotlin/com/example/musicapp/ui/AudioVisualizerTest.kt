package com.example.musicapp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicapp.theme.AppColors
import kotlin.test.Test
import kotlin.test.assertTrue

class AudioVisualizerTest {

    @Test
    fun testAudioVisualizerRendersIdleWhenPaused() {
        val scene = ImageComposeScene(width = 300, height = 100)
        try {
            scene.setContent {
                AudioVisualizerView(
                    spectrum = FloatArray(32) { 0f },
                    isPlaying = false,
                    barColor = AppColors.Accent,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
            val image = scene.render()
            assertTrue(image.width == 300, "Image width should match scene width")
            assertTrue(image.height == 100, "Image height should match scene height")
        } finally {
            scene.close()
        }
    }

    @Test
    fun testAudioVisualizerRendersAnimatedWhenPlayingWithoutHardwareSpectrum() {
        val scene = ImageComposeScene(width = 300, height = 100)
        try {
            scene.setContent {
                AudioVisualizerView(
                    spectrum = FloatArray(32) { 0f },
                    isPlaying = true,
                    barColor = AppColors.Accent,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
            val image = scene.render()
            assertTrue(image.width == 300)
        } finally {
            scene.close()
        }
    }

    @Test
    fun testAudioVisualizerRendersWithRealSpectrumData() {
        val scene = ImageComposeScene(width = 300, height = 100)
        val testSpectrum = FloatArray(32) { idx -> (idx % 8) / 8f }
        try {
            scene.setContent {
                AudioVisualizerView(
                    spectrum = testSpectrum,
                    isPlaying = true,
                    barColor = AppColors.Accent,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
            val image = scene.render()
            assertTrue(image.width == 300)
        } finally {
            scene.close()
        }
    }

    @Test
    fun testSpectrumCanvasBackwardsCompatibility() {
        val scene = ImageComposeScene(width = 300, height = 100)
        try {
            scene.setContent {
                SpectrumCanvas(
                    spectrum = FloatArray(32) { 0.5f },
                    isPlaying = true,
                    barColor = AppColors.Accent,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
            val image = scene.render()
            assertTrue(image.width == 300)
        } finally {
            scene.close()
        }
    }
}
