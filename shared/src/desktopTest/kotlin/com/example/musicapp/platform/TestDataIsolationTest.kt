package com.example.musicapp.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestDataIsolationTest {

    @Test
    fun testAppStorageDoesNotTouchUserHomeInTests() {
        val storageDir = AppStorage.dir
        val userHome = System.getProperty("user.home")
        val realUserJvx = File(userHome, "JVXMusic").absolutePath

        assertFalse(
            storageDir.absolutePath.equals(realUserJvx, ignoreCase = true),
            "Tests must not point to real user directory $realUserJvx, current is ${storageDir.absolutePath}"
        )
        assertTrue(
            storageDir.absolutePath.contains("build", ignoreCase = true) ||
            storageDir.absolutePath.contains("test-jvx-data", ignoreCase = true) ||
            storageDir.absolutePath.contains("temp", ignoreCase = true) ||
            storageDir.absolutePath.contains("tmp", ignoreCase = true),
            "Storage directory must be inside a build or temp directory: ${storageDir.absolutePath}"
        )
    }

    @Test
    fun testTestIsolationGuardFailsIfAttemptingToAccessUserHome() {
        val originalProp = System.getProperty("jvx.data.dir")
        try {
            System.clearProperty("jvx.data.dir")
            AppStorage.setCustomDataDir(null)

            val exception = assertFailsWith<IllegalStateException> {
                AppStorage.dir
            }
            assertTrue(
                exception.message?.contains("TEST ISOLATION VIOLATION") == true,
                "Must throw TEST ISOLATION VIOLATION when attempting to access user home while isolation is active"
            )
        } finally {
            if (originalProp != null) {
                System.setProperty("jvx.data.dir", originalProp)
            }
        }
    }

    @Test
    fun testCustomDataDirOverride() {
        val tempDir = File.createTempFile("jvx_isolated_test_", "").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }

        try {
            AppStorage.setCustomDataDir(tempDir)
            assertEquals(tempDir.absolutePath, AppStorage.dir.absolutePath)

            AppStorage.write("test_file.txt", "hello isolation")
            assertEquals("hello isolation", File(tempDir, "test_file.txt").readText())
        } finally {
            AppStorage.setCustomDataDir(null)
            tempDir.deleteRecursively()
        }
    }
}
