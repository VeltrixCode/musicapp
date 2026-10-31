package com.example.musicapp.cache

import androidx.compose.ui.graphics.ImageBitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertNull

class ArtworkCacheConcurrencyTest {

    @Test
    fun testConcurrentPutAndGetDoesNotThrowConcurrentModificationException() {
        val errorRef = AtomicReference<Throwable?>(null)
        val numThreads = 8
        val iterationsPerThread = 200
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(numThreads)

        // Create a dummy 1x1 ImageBitmap
        val dummyBitmap = ImageBitmap(1, 1)

        val threads = (0 until numThreads).map { threadIdx ->
            thread(start = false) {
                try {
                    startLatch.await()
                    for (i in 0 until iterationsPerThread) {
                        val key = "item_${(threadIdx * 100 + i) % 150}"
                        if (i % 2 == 0) {
                            ArtworkCache.put(key, dummyBitmap)
                        } else {
                            ArtworkCache.get(key)
                        }
                        if (i % 50 == 0) {
                            ArtworkCache.getDominantColor(key)
                        }
                    }
                } catch (t: Throwable) {
                    errorRef.compareAndSet(null, t)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        threads.forEach { it.start() }
        startLatch.countDown()

        val completed = doneLatch.await(10, TimeUnit.SECONDS)
        assertNull(errorRef.get(), "Concurrent access to ArtworkCache threw an exception: ${errorRef.get()}")
        kotlin.test.assertTrue(completed, "All threads must complete in time")
    }
}
