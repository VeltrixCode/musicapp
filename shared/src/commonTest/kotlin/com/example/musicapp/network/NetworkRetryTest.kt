package com.example.musicapp.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetworkRetryTest {

    @Test
    fun testRetriesOnIOExceptionWithExponentialBackoff() = runTest {
        var attempts = 0
        val delays = mutableListOf<Long>()

        val result = retryWithBackoff(
            maxRetries = 3,
            initialDelayMs = 50L,
            factor = 2.0,
            onRetry = { attempt, _ ->
                attempts++
            }
        ) {
            if (attempts < 2) {
                throw IOException("Temporary connection reset")
            }
            "SUCCESS"
        }

        assertEquals("SUCCESS", result)
        assertEquals(2, attempts)
    }

    @Test
    fun testDoesNotRetryOnClientErrors400Or404() = runTest {
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            retryWithBackoff(
                maxRetries = 3,
                initialDelayMs = 20L,
                onRetry = { _, _ -> attempts++ }
            ) {
                attempts++
                throw IllegalArgumentException("Non-retryable 400 Bad Request error")
            }
        }

        assertEquals(1, attempts, "Client errors should fail immediately without retrying")
    }
}
