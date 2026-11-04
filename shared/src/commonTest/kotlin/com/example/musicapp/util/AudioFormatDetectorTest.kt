package com.example.musicapp.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioFormatDetectorTest {

    @Test
    fun testFourccToStringLittleEndian() {
        // 'f' (0x66), 'l' (0x6c), 'a' (0x61), 'c' (0x63)
        // Little endian integer: 0x63616c66
        val flacInt = 0x66 or (0x6c shl 8) or (0x61 shl 16) or (0x63 shl 24)
        assertEquals("flac", fourccToString(flacInt))

        // 'm', 'p', 'g', 'a'
        val mpgaInt = 'm'.code or ('p'.code shl 8) or ('g'.code shl 16) or ('a'.code shl 24)
        assertEquals("mpga", fourccToString(mpgaInt))

        // 'm', 'p', '4', 'a'
        val mp4aInt = 'm'.code or ('p'.code shl 8) or ('4'.code shl 16) or ('a'.code shl 24)
        assertEquals("mp4a", fourccToString(mp4aInt))

        // 0 returns empty
        assertEquals("", fourccToString(0))
    }

    @Test
    fun testMapCodecToFormat() {
        // FLAC fourcc
        val (flacFmt, flacLossless) = mapCodecToFormat("flac", "Free Lossless Audio Codec")
        assertEquals("FLAC", flacFmt)
        assertTrue(flacLossless)

        // mpga fourcc
        val (mpgaFmt, mpgaLossless) = mapCodecToFormat("mpga", "MPEG Audio layer 1/2/3")
        assertEquals("MP3", mpgaFmt)
        assertFalse(mpgaLossless)

        // mp4a fourcc
        val (aacFmt, aacLossless) = mapCodecToFormat("mp4a", "MPEG AAC Audio")
        assertEquals("AAC", aacFmt)
        assertFalse(aacLossless)

        // Empty fourcc with MP3 description
        val (mp3DescFmt1, mp3DescLossless1) = mapCodecToFormat("", "MPEG Audio layer 3")
        assertEquals("MP3", mp3DescFmt1)
        assertFalse(mp3DescLossless1)

        val (mp3DescFmt2, mp3DescLossless2) = mapCodecToFormat("", "MPEG Audio layer iii")
        assertEquals("MP3", mp3DescFmt2)
        assertFalse(mp3DescLossless2)

        // Garbage fourcc and description
        val (junkFmt, junkLossless) = mapCodecToFormat("xyz9", "Unknown non-audio stream")
        assertEquals("AUDIO", junkFmt)
        assertFalse(junkLossless)

        // WAV variants
        val (wavTwos, wavTwosLossless) = mapCodecToFormat("twos", "")
        assertEquals("WAV", wavTwos)
        assertTrue(wavTwosLossless)

        val (wavFl32, wavFl32Lossless) = mapCodecToFormat("fl32", "")
        assertEquals("WAV", wavFl32)
        assertTrue(wavFl32Lossless)

        val (wavS16l, wavS16lLossless) = mapCodecToFormat("s16l", "")
        assertEquals("WAV", wavS16l)
        assertTrue(wavS16lLossless)
    }

    @Test
    fun testDetectFormatFromName() {
        // Track 01. Intro with null / octet-stream MIME must return AUDIO (not INTRO)
        assertEquals("AUDIO", detectFormatFromName("Track 01. Intro", null))
        assertEquals("AUDIO", detectFormatFromName("Track 01. Intro", "application/octet-stream"))
        assertEquals("AUDIO", detectFormatFromName("Track 01. Intro", "binary/octet-stream"))

        // Track with explicit audio MIME overrides name
        assertEquals("FLAC", detectFormatFromName("Track 01. Intro", "audio/flac"))
        assertEquals("MP3", detectFormatFromName("Track 01. Intro", "audio/mpeg"))

        // file.FLAC with null / octet-stream MIME must return FLAC
        assertEquals("FLAC", detectFormatFromName("file.FLAC", null))
        assertEquals("FLAC", detectFormatFromName("file.FLAC", "application/octet-stream"))
        assertEquals("FLAC", detectFormatFromName("song.flac", null))

        // noext with null / octet-stream MIME must return AUDIO
        assertEquals("AUDIO", detectFormatFromName("noext", null))
        assertEquals("AUDIO", detectFormatFromName("noext", "application/octet-stream"))

        // Other supported extensions
        assertEquals("WAV", detectFormatFromName("audio_sample.wav", null))
        assertEquals("OGG", detectFormatFromName("music.ogg", null))
        assertEquals("OPUS", detectFormatFromName("podcast.opus", null))
        assertEquals("AAC", detectFormatFromName("ringtone.aac", null))
        assertEquals("M4A", detectFormatFromName("voice.m4a", null))
    }
}
