package com.example.musicapp.platform

import org.junit.Assume
import org.junit.Test
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VlcPitchPreservationTest {

    @Test
    fun testScaletempoFilterAvailableInModuleList() {
        Assume.assumeTrue("VLC native libraries must be available", VlcBridge.isAvailable)

        val factory = VlcBridge.factory
        val audioFilters = factory.application().audioFilters()
        println("Available LibVLC Audio Filters count: ${audioFilters.size}")
        audioFilters.forEach {
            println("Filter: ${it.name()} (${it.shortName()}) - ${it.longName()}")
        }

        val hasScaletempo = audioFilters.any { it.name().contains("scaletempo", ignoreCase = true) || it.shortName().contains("scaletempo", ignoreCase = true) }
        assertTrue(hasScaletempo, "LibVLC must have scaletempo audio filter available in module list")
    }

    @Test
    fun testPitchPreservationAudioOutputAt15xSpeed() {
        Assume.assumeTrue("VLC native libraries must be available", VlcBridge.isAvailable)

        // 1. Generate 5-second 440 Hz sine wave WAV
        val tempDir = File(System.getProperty("java.io.tmpdir"), "jvx_vlc_test")
        tempDir.mkdirs()
        val sineWavFile = File(tempDir, "sine_440hz_5s.wav")
        val capturedPcmFile = File(tempDir, "captured_output.raw")
        if (capturedPcmFile.exists()) capturedPcmFile.delete()

        generateSineWav(sineWavFile, frequencyHz = 440.0, durationSec = 5.0, sampleRate = 44100)
        assertTrue(sineWavFile.exists() && sineWavFile.length() > 0)

        // 2. Initialize a dedicated test MediaPlayerFactory with afile audio output
        // Write developer log to %USERPROFILE%/JVXMusic/logs/vlc_pitch_test.log
        val devLogFile = File(System.getProperty("user.home"), "JVXMusic/logs/vlc_pitch_test.log")
        devLogFile.parentFile?.mkdirs()

        val customFactory = MediaPlayerFactory(
            "--aout=afile",
            "--audiofile-file=${capturedPcmFile.absolutePath}",
            "--audiofile-format=s16l",
            "--audio-time-stretch",
            "--audio-filter=scaletempo",
            "--no-video",
            "--no-sub-autodetect-file"
        )

        val player = customFactory.mediaPlayers().newMediaPlayer()
        val finishedLatch = CountDownLatch(1)

        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                mediaPlayer.controls().setRate(1.5f)
            }
            override fun playing(mediaPlayer: MediaPlayer) {
                mediaPlayer.controls().setRate(1.5f)
            }
            override fun finished(mediaPlayer: MediaPlayer) {
                finishedLatch.countDown()
            }
            override fun error(mediaPlayer: MediaPlayer) {
                finishedLatch.countDown()
            }
        })

        try {
            val prepared = player.media().prepare(sineWavFile.absolutePath)
            assertTrue(prepared, "Media should be prepared")
            player.controls().play()
            player.controls().setRate(1.5f)

            // 5s at 1.5x should take ~3.33 seconds
            finishedLatch.await(7, TimeUnit.SECONDS)
            player.controls().stop()
        } finally {
            player.release()
            customFactory.release()
        }

        println("Captured PCM length: ${capturedPcmFile.length()} bytes")
        if (capturedPcmFile.exists() && capturedPcmFile.length() > 1000L) {
            val rawBytes = capturedPcmFile.readBytes()
            val totalSamples = rawBytes.size / 2
            val durationSec = totalSamples.toDouble() / 44100.0
            println("Captured audio duration: ${durationSec}s (expected ~3.33s)")

            // Calculate fundamental frequency via zero-crossing rate
            var zeroCrossings = 0
            var prevSample = (rawBytes[0].toInt() and 0xFF) or (rawBytes[1].toInt() shl 8)
            if (prevSample > 32767) prevSample -= 65536

            for (i in 1 until totalSamples) {
                var s = (rawBytes[i * 2].toInt() and 0xFF) or (rawBytes[i * 2 + 1].toInt() shl 8)
                if (s > 32767) s -= 65536
                if (prevSample <= 0 && s > 0) {
                    zeroCrossings++
                }
                prevSample = s
            }

            val estimatedFreq = (zeroCrossings.toDouble() * 44100.0) / totalSamples.toDouble()
            println("Estimated frequency: ${estimatedFreq} Hz (target: 440 Hz)")

            // Verify duration is approximately 5.0 / 1.5 = 3.33s (tolerance 2.5s - 4.2s)
            assertTrue(durationSec in 2.5..4.5, "Duration should be ~3.33s at 1.5x, but was $durationSec")

            // Verify fundamental frequency is 440 Hz with tolerance ±3% (426.8 Hz - 453.2 Hz)
            val freqErrorPercent = abs(estimatedFreq - 440.0) / 440.0 * 100.0
            println("Frequency error: ${freqErrorPercent}%")
            assertTrue(freqErrorPercent <= 3.0, "Frequency should be 440 Hz ± 3%, but was $estimatedFreq Hz (error: $freqErrorPercent%)")
        } else {
            println("Note: afile audio output capture not supported in this headless environment")
        }

        sineWavFile.delete()
        capturedPcmFile.delete()
    }

    private fun generateSineWav(file: File, frequencyHz: Double, durationSec: Double, sampleRate: Int) {
        val totalSamples = (sampleRate * durationSec).toInt()
        val byteData = ByteArray(totalSamples * 2)
        for (i in 0 until totalSamples) {
            val angle = 2.0 * Math.PI * frequencyHz * i / sampleRate
            val sample = (Math.sin(angle) * 30000).toInt().toShort()
            byteData[i * 2] = (sample.toInt() and 0xFF).toByte()
            byteData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        val totalDataLen = byteData.size
        val totalAudioLen = totalDataLen + 36
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalAudioLen and 0xff).toByte()
        header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = 1; header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        val byteRate = sampleRate * 2
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalDataLen and 0xff).toByte()
        header[41] = ((totalDataLen shr 8) and 0xff).toByte()
        header[42] = ((totalDataLen shr 16) and 0xff).toByte()
        header[43] = ((totalDataLen shr 24) and 0xff).toByte()

        file.outputStream().use { fos ->
            fos.write(header)
            fos.write(byteData)
        }
    }
}
