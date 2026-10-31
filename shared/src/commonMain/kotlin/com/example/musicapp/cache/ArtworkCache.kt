package com.example.musicapp.cache

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.example.musicapp.platform.synchronizedPlatform

object ArtworkCache {
    private const val MAX_ENTRIES = 100
    private val lock = Any()
    private val map = mutableMapOf<String, ImageBitmap>()
    private val accessOrder = mutableListOf<String>()
    private val dominantColorMap = mutableMapOf<String, Color>()

    fun get(path: String): ImageBitmap? = synchronizedPlatform(lock) {
        val bitmap = map[path]
        if (bitmap != null) {
            accessOrder.remove(path)
            accessOrder.add(path)
        }
        bitmap
    }

    fun put(path: String, bitmap: ImageBitmap) = synchronizedPlatform(lock) {
        if (map.containsKey(path)) {
            accessOrder.remove(path)
        } else if (accessOrder.size >= MAX_ENTRIES) {
            val oldest = accessOrder.removeAt(0)
            map.remove(oldest)
            dominantColorMap.remove(oldest)
        }
        accessOrder.add(path)
        map[path] = bitmap
    }

    fun getDominantColor(path: String): Color? = synchronizedPlatform(lock) {
        dominantColorMap[path]
    }

    fun clear() = synchronizedPlatform(lock) {
        map.clear()
        accessOrder.clear()
        dominantColorMap.clear()
    }

    fun computeAndStoreDominantColor(path: String, bitmap: ImageBitmap): Color {
        synchronizedPlatform(lock) {
            val existing = dominantColorMap[path]
            if (existing != null) return existing
        }

        val color = runCatching {
            val pixelMap = bitmap.toPixelMap()
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0L
            val stepX = (bitmap.width / 16).coerceAtLeast(1)
            val stepY = (bitmap.height / 16).coerceAtLeast(1)

            for (x in 0 until bitmap.width step stepX) {
                for (y in 0 until bitmap.height step stepY) {
                    val pixel = pixelMap[x, y]
                    // Skip nearly completely black or completely white transparent pixels
                    val lum = 0.299f * pixel.red + 0.587f * pixel.green + 0.114f * pixel.blue
                    if (lum in 0.08f..0.92f) {
                        rSum += (pixel.red * 255).toLong()
                        gSum += (pixel.green * 255).toLong()
                        bSum += (pixel.blue * 255).toLong()
                        count++
                    }
                }
            }
            if (count > 0) {
                val rawR = (rSum.toFloat() / count / 255f).coerceIn(0f, 1f)
                val rawG = (gSum.toFloat() / count / 255f).coerceIn(0f, 1f)
                val rawB = (bSum.toFloat() / count / 255f).coerceIn(0f, 1f)

                // Convert RGB to HSL
                val max = maxOf(rawR, rawG, rawB)
                val min = minOf(rawR, rawG, rawB)
                val delta = max - min
                var h = when {
                    delta == 0f -> 0f
                    max == rawR -> (60f * (((rawG - rawB) / delta) % 6f) + 360f) % 360f
                    max == rawG -> (60f * (((rawB - rawR) / delta) + 2f) + 360f) % 360f
                    else -> (60f * (((rawR - rawG) / delta) + 4f) + 360f) % 360f
                }
                var l = (max + min) / 2f
                var s = if (delta == 0f) 0f else if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)

                // Clamp ONLY maximum lightness (HSL L <= 0.35) and ensure not pitch black (L >= 0.18)
                // Boost saturation slightly so dark tones remain vibrant and distinctly tinted
                val finalL = l.coerceIn(0.18f, 0.35f)
                val finalS = if (s > 0.05f) s.coerceIn(0.40f, 0.90f) else s

                // HSL to RGB
                val c = (1f - kotlin.math.abs(2f * finalL - 1f)) * finalS
                val xVal = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
                val m = finalL - c / 2f

                val (rPrime, gPrime, bPrime) = when ((h / 60f).toInt()) {
                    0 -> Triple(c, xVal, 0f)
                    1 -> Triple(xVal, c, 0f)
                    2 -> Triple(0f, c, xVal)
                    3 -> Triple(0f, xVal, c)
                    4 -> Triple(xVal, 0f, c)
                    else -> Triple(c, 0f, xVal)
                }

                Color(
                    red = (rPrime + m).coerceIn(0f, 1f),
                    green = (gPrime + m).coerceIn(0f, 1f),
                    blue = (bPrime + m).coerceIn(0f, 1f),
                    alpha = 1.0f
                )
            } else null
        }.getOrNull() ?: Color(0xFF0F3D2E) // JVX signature deep emerald fallback

        val finalColor = color
        synchronizedPlatform(lock) {
            dominantColorMap[path] = finalColor
        }
        return finalColor
    }
}
