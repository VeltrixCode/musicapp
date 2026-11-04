package com.example.musicapp.util

val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "ogg", "opus", "m4a", "aac", "alac", "wma")
val VALID_AUDIO_FORMATS = setOf("FLAC", "MP3", "AAC", "OGG", "OPUS", "WAV", "ALAC")

/**
 * Unpacks a 32-bit integer FourCC into an ASCII string.
 * In VLC, FourCC is packed in little-endian order (first character is in lowest byte).
 */
internal fun fourccToString(value: Int): String {
    if (value == 0) return ""
    val chars = charArrayOf(
        (value and 0xFF).toChar(),
        ((value shr 8) and 0xFF).toChar(),
        ((value shr 16) and 0xFF).toChar(),
        ((value shr 24) and 0xFF).toChar()
    )
    return chars.filter { it in ' '..'~' }.joinToString("").trim().lowercase()
}

/**
 * Maps FourCC and codec description to a normalized format name and lossless flag.
 * Checks FourCC first with priority. Codec description is inspected only if FourCC is
 * empty or unrecognized.
 */
internal fun mapCodecToFormat(fourcc: String, desc: String = ""): Pair<String, Boolean> {
    val fcc = fourcc.trim().lowercase()
    val d = desc.trim().lowercase()

    // 1. Check FourCC first
    if (fcc.isNotEmpty()) {
        when {
            fcc.contains("flac") -> return Pair("FLAC", true)
            fcc.contains("mpga") || fcc.contains("mp3") -> return Pair("MP3", false)
            fcc.contains("mp4a") || fcc.contains("aac") -> return Pair("AAC", false)
            fcc.contains("vorb") || fcc.contains("ogg") -> return Pair("OGG", false)
            fcc.contains("opus") -> return Pair("OPUS", false)
            fcc.contains("araw") || fcc.contains("pcm") || fcc.contains("wav") ||
                    fcc.contains("s16l") || fcc.contains("twos") || fcc.contains("fl32") || fcc.contains("fl64") -> return Pair("WAV", true)
            fcc.contains("alac") -> return Pair("ALAC", true)
        }
    }

    // 2. Fallback to codec description only if FourCC is empty or unrecognized
    if (d.isNotEmpty()) {
        when {
            d.contains("layer 3") || d.contains("layer iii") -> return Pair("MP3", false)
            d.contains("flac") -> return Pair("FLAC", true)
            d.contains("aac") -> return Pair("AAC", false)
            d.contains("vorbis") || d.contains("ogg") -> return Pair("OGG", false)
            d.contains("opus") -> return Pair("OPUS", false)
            d.contains("pcm") || d.contains("wave") -> return Pair("WAV", true)
            d.contains("alac") -> return Pair("ALAC", true)
        }
    }

    // 3. Unrecognized -> strictly AUDIO
    return Pair("AUDIO", false)
}

/**
 * Detects format from file name and MIME type with strict whitelist normalization.
 * If MIME is unknown or application/octet-stream, extension is trimmed and checked against AUDIO_EXTENSIONS.
 */
internal fun detectFormatFromName(name: String, mime: String?): String {
    val rawMime = mime?.lowercase()?.trim() ?: ""
    val hasExplicitMime = rawMime.isNotEmpty() &&
            rawMime != "application/octet-stream" &&
            rawMime != "binary/octet-stream"

    if (hasExplicitMime) {
        return when {
            rawMime.contains("flac") -> "FLAC"
            rawMime.contains("wav") -> "WAV"
            rawMime.contains("ogg") || rawMime.contains("vorbis") -> "OGG"
            rawMime.contains("mp4") || rawMime.contains("aac") || rawMime.contains("m4a") -> "AAC"
            rawMime.contains("mp3") || rawMime.contains("mpeg") -> "MP3"
            rawMime.contains("opus") -> "OPUS"
            else -> "AUDIO"
        }
    }

    // When MIME is unknown or octet-stream, check extension with strict whitelist
    val ext = name.substringAfterLast('.', "").trim().lowercase()
    return if (ext in AUDIO_EXTENSIONS) {
        ext.uppercase()
    } else {
        "AUDIO"
    }
}
