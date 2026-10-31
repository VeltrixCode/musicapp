package com.example.musicapp.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import java.io.IOException

suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 500L,
    factor: Double = 2.0,
    onRetry: ((attempt: Int, error: Throwable) -> Unit)? = null,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    var lastException: Throwable? = null

    for (attempt in 1..maxRetries) {
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            val isRetryable = when (e) {
                is IOException -> true
                is ServerResponseException -> {
                    val status = e.response.status
                    status in listOf(
                        HttpStatusCode.InternalServerError,
                        HttpStatusCode.BadGateway,
                        HttpStatusCode.ServiceUnavailable,
                        HttpStatusCode.GatewayTimeout
                    )
                }
                is ClientRequestException -> {
                    e.response.status == HttpStatusCode.TooManyRequests
                }
                else -> false
            }

            if (!isRetryable || attempt == maxRetries) {
                throw e
            }

            onRetry?.invoke(attempt, e)
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }

    throw lastException ?: IllegalStateException("Retry failed")
}
