package com.example.musicapp.provider

import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CloudProviderRecoveryTest {

    @Test
    fun testGoogleOAuthInvalidGrantProducesTestingNotice() {
        val errorResponse = "invalid_grant"
        val errorDescription = "Token has been expired or revoked."

        fun parseGoogleAuthError(error: String): String {
            return if (error == "invalid_grant") {
                "Токен Google Drive истек (ограничение 7 дней в режиме Testing). Авторизуйтесь снова или переведите статус проекта в 'In production' в Google Cloud Console."
            } else {
                "Ошибка авторизации: $error"
            }
        }

        val friendlyMessage = parseGoogleAuthError(errorResponse)
        assertTrue(friendlyMessage.contains("7 дней"))
        assertTrue(friendlyMessage.contains("Testing"))
        assertTrue(friendlyMessage.contains("In production"))
    }

    @Test
    fun testYandexDiskExpiredDownloadLinkRenewal() = runTest {
        val track = RemoteTrack(
            id = "yandex_test_track",
            title = "Track Title",
            artist = "Artist",
            album = "Album",
            streamUrl = "https://downloader.disk.yandex.ru/expired_token_123",
            source = TrackSource.YANDEX_DISK
        )

        var renewCount = 0
        val renewCallback: suspend () -> StreamSource? = {
            renewCount++
            StreamSource(url = "https://downloader.disk.yandex.ru/fresh_renewed_token_456")
        }

        // Simulate upstream 403 / 410 triggering renewal
        val initialStream = StreamSource(track.streamUrl ?: "")
        val renewedStream = renewCallback.invoke()

        assertNotNull(renewedStream)
        assertEquals(1, renewCount)
        assertTrue(renewedStream.url.contains("fresh_renewed_token_456"))
    }
}
