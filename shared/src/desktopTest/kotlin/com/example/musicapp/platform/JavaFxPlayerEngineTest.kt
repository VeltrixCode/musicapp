package com.example.musicapp.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class JavaFxPlayerEngineTest {
    @Test
    fun testEngineInstantiationAndLoad() {
        val engine = createPlayerEngine()
        assertNotNull(engine)

        // Create a dummy temp mp3 file
        val tempFile = File.createTempFile("test_audio", ".mp3")
        tempFile.writeBytes(ByteArray(1024))
        try {
            engine.load(tempFile.absolutePath, onReady = {}, onFinished = {})
            Thread.sleep(300)
            engine.release()
            Thread.sleep(100)
        } finally {
            tempFile.delete()
        }
    }
}
