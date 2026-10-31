package com.example.musicapp.cache

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

object ArtworkCache {
    private const val MAX_ENTRIES = 100
    private val map = mutableMapOf<String, ImageBitmap>()
    private val accessOrder = mutableListOf<String>()
    private val dominantColorMap = mutableMapOf<String, Color>()

    fun get(path: String): ImageBitmap? = map[path]

    fun put(path: String, bitmap: ImageBitmap) {
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

    fun getDominantColor(path: String): Color? = dominantColorMap[path]

    fun computeAndStoreDominantColor(path: String, bitmap: ImageBitmap): Color {
        val existing = dominantColorMap[path]
        if (existing != null) return existing

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
                    val lum = 0.299f * pixel.red + 0.587f * pixel.green + 0.114f * pixel.blue
                    if (lum in 0.15f..0.85f) {
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
                val lum = 0.299f * rawR + 0.587f * rawG + 0.114f * rawB
                // Clamp max luminance so the header gradient remains moody and readable
                val factor = if (lum > 0.62f) 0.62f / lum else 1.0f
                Color(
                    red = (rawR * factor).coerceIn(0f, 1f),
                    green = (rawG * factor).coerceIn(0f, 1f),
                    blue = (rawB * factor).coerceIn(0f, 1f),
                    alpha = 1.0f
                )
            } else null
        }.getOrNull() ?: Color(0xFF1E3264)

        dominantColorMap[path] = color
        return color
    }
}
