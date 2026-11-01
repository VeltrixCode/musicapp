package com.example.musicapp.platform

import com.example.musicapp.cache.ArtworkCache
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrackMetadataArtworkTest {

    @Test
    fun testReadTrackInfoAndArtworkAndDominantColor() = runBlocking {
        // 1. Create a minimal valid MP3 file with MPEG-1 Layer 3 audio frames
        val tempFile = File.createTempFile("test_track_", ".mp3")
        tempFile.deleteOnExit()

        val frame = ByteArray(417)
        frame[0] = 0xFF.toByte()
        frame[1] = 0xFB.toByte() // MPEG 1 Layer 3, no protection
        frame[2] = 0x90.toByte() // 128 kbps, 44100 Hz
        frame[3] = 0x64.toByte()

        FileOutputStream(tempFile).use { fos ->
            repeat(10) {
                fos.write(frame)
            }
        }

        // 2. Generate a 32x32 emerald image fixture
        val img = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(0x00, 0xD2, 0x6A) // Emerald
        g.fillRect(0, 0, 32, 32)
        g.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val imageBytes = baos.toByteArray()

        // 3. Write ID3v2 tags using jaudiotagger
        val mp3 = MP3File(tempFile)
        val tag = mp3.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, "Test Track Title")
        tag.setField(FieldKey.ARTIST, "Test Artist")
        tag.setField(FieldKey.ALBUM, "Test Album")
        tag.setField(FieldKey.GENRE, "Synthwave")
        tag.setField(FieldKey.YEAR, "2024")

        val artwork = ArtworkFactory.getNew()
        artwork.binaryData = imageBytes
        artwork.mimeType = "image/png"
        tag.setField(artwork)
        mp3.commit()

        // 4. Test readTrackInfo reads all 5 metadata fields
        val info = readTrackInfo(tempFile.absolutePath)
        assertNotNull(info, "readTrackInfo must return non-null TrackInfo")
        assertEquals("Test Track Title", info.title)
        assertEquals("Test Artist", info.artist)
        assertEquals("Test Album", info.album)
        assertEquals("Synthwave", info.genre)
        assertEquals(2024, info.year)

        // 5. Test loadArtwork extracts ImageBitmap and computes dominant color
        ArtworkCache.clear()
        val bitmap = loadArtwork(tempFile.absolutePath)
        assertNotNull(bitmap, "loadArtwork must successfully extract ImageBitmap from embedded artwork")
        assertTrue(bitmap.width > 0 && bitmap.height > 0, "Bitmap must have positive dimensions")

        // 6. Test dominant color computation
        val dominantColor = ArtworkCache.getDominantColor(tempFile.absolutePath)
        assertNotNull(dominantColor, "ArtworkCache must have stored dominant color")
        // Check that dominant color is in the green/emerald spectrum
        assertTrue(dominantColor.green > dominantColor.red, "Green channel should dominate for emerald fixture")
        assertTrue(dominantColor.green > dominantColor.blue, "Green channel should dominate for emerald fixture")
    }
}
