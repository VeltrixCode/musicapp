package com.example.musicapp.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CrashReporterTest {

    @Test
    fun testSanitizePasswordsAndTokens() {
        val input = "Error calling https://subsonic.example.com/rest/ping.view?u=alice&t=c5f782a39&s=salt123&password=mySecretPassword"
        val sanitized = CrashReporter.sanitize(input)
        assertFalse(sanitized.contains("mySecretPassword"), "Password must be sanitized")
        assertFalse(sanitized.contains("c5f782a39"), "Token must be sanitized")
        assertFalse(sanitized.contains("salt123"), "Salt must be sanitized")
        assertTrue(sanitized.contains("password=***"))
        assertTrue(sanitized.contains("t=***"))
        assertTrue(sanitized.contains("s=***"))
        assertTrue(sanitized.contains("u=***"))
    }

    @Test
    fun testSanitizeAccessTokenAndClientSecret() {
        val input = "OAuth error: access_token=ya29.a0AfH6SM...&client_secret=GOCSPX-superSecretKey123"
        val sanitized = CrashReporter.sanitize(input)
        assertFalse(sanitized.contains("ya29.a0AfH6SM"), "access_token must be masked")
        assertFalse(sanitized.contains("GOCSPX-superSecretKey123"), "client_secret must be masked")
        assertTrue(sanitized.contains("access_token=***"))
        assertTrue(sanitized.contains("client_secret=***"))
    }

    @Test
    fun testSanitizePasswordsInUrl() {
        val input = "Failed connecting to https://subsonicUser:superSecretPass@music.example.org:4533/rest/stream"
        val sanitized = CrashReporter.sanitize(input)
        assertFalse(sanitized.contains("superSecretPass"), "Password in URL authority must be masked")
        assertTrue(sanitized.contains("https://subsonicUser:***@music.example.org:4533/rest/stream"))
    }

    @Test
    fun testSanitizeAuthHeaders() {
        val input = "Failed request: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz.123"
        val sanitized = CrashReporter.sanitize(input)
        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"), "JWT bearer token must be masked")
        assertTrue(sanitized.contains("Bearer ***") || sanitized.contains("Bearer: ***"))
    }

    @Test
    fun testHandleUncaughtExceptionWritesSanitizedFile() {
        val exception = RuntimeException("Connection error with token=superSecretToken12345")
        val file: File? = CrashReporter.handleUncaughtException(Thread.currentThread(), exception, showDialog = false)
        assertNotNull(file)
        assertTrue(file.exists())
        try {
            val content = file.readText()
            assertTrue(content.contains("JVXMusic Crash Log"))
            assertFalse(content.contains("superSecretToken12345"), "Crash log file must never contain raw tokens")
            assertTrue(content.contains("token=***"))
        } finally {
            file.delete()
        }
    }
}
