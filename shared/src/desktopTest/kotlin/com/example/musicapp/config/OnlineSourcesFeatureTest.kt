package com.example.musicapp.config

import com.example.musicapp.library.Library
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.provider.ProviderRegistry
import com.example.musicapp.provider.closeAppHttpClient
import com.example.musicapp.provider.isAppHttpClientCreated
import com.example.musicapp.proxy.LocalStreamingProxy
import com.example.musicapp.security.SecureStorage
import com.example.musicapp.service.CoverArtService
import com.example.musicapp.service.LyricsService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnlineSourcesFeatureTest {

    private var initialDevFlag = false

    @Before
    fun setUp() {
        initialDevFlag = AppFeatures.devOnlineSourcesEnabled
        AppFeatures.devOnlineSourcesEnabled = false
        closeAppHttpClient()
        LocalStreamingProxy.stop()
        SecureStorage.accessCount.set(0)
    }

    @After
    fun tearDown() {
        AppFeatures.devOnlineSourcesEnabled = initialDevFlag
        closeAppHttpClient()
        LocalStreamingProxy.stop()
    }

    @Test
    fun testOnlineSourcesDisabled_RegistryIsEmpty() {
        assertFalse(AppFeatures.isOnlineSourcesEnabled)
        val registry = ProviderRegistry()

        assertTrue(registry.all().isEmpty())
        assertTrue(registry.allProviders.isEmpty())
        assertNull(registry.getProviderFor(TrackSource.GOOGLE_DRIVE))
        assertNull(registry.getProviderFor(TrackSource.YANDEX_DISK))
        assertNull(registry.getProviderFor(TrackSource.JAMENDO))
        assertNull(registry.getProviderFor(TrackSource.AUDIUS))
        assertNull(registry.getProviderFor(TrackSource.INTERNET_ARCHIVE))
        assertNull(registry.getProviderFor(TrackSource.SUBSONIC))
        assertNull(registry.getProviderFor(TrackSource.WEBDAV))
        assertNull(registry.getProviderFor(TrackSource.RADIO_BROWSER))

        // Accessing properties directly when disabled throws descriptive error
        assertFailsWith<IllegalStateException> { registry.gdrive }
        assertFailsWith<IllegalStateException> { registry.jamendo }
    }

    @Test
    fun testOnlineSourcesDisabled_ProxyIsNotListeningAndPortIsZero() {
        assertFalse(AppFeatures.isOnlineSourcesEnabled)
        assertEquals(0, LocalStreamingProxy.serverPort)
        assertTrue(LocalStreamingProxy.sessions.isEmpty())
    }

    @Test
    fun testOnlineSourcesDisabled_HttpClientIsNotCreated() {
        assertFalse(AppFeatures.isOnlineSourcesEnabled)
        assertFalse(isAppHttpClientCreated)
    }

    @Test
    fun testOnlineSourcesDisabled_ZeroSecureStorageAccessOnConfigInspection() {
        assertFalse(AppFeatures.isOnlineSourcesEnabled)
        SecureStorage.accessCount.set(0)

        // Reading config properties when online sources is disabled does not touch vault
        assertEquals("", AppConfig.jamendoClientId)
        assertEquals("JVXMusic", AppConfig.audiusAppName)
        assertEquals("", AppConfig.googleClientId)
        assertEquals("", AppConfig.subsonicServer)
        assertEquals("", AppConfig.webdavServer)

        assertEquals(0, SecureStorage.accessCount.get())
    }

    @Test
    fun testOnlineSourcesDisabled_ZeroNetworkRequestsDuringLocalPlayback() = runBlocking {
        assertFalse(AppFeatures.isOnlineSourcesEnabled)
        var networkRequestsCount = 0

        val mockClient = HttpClient(MockEngine {
            networkRequestsCount++
            respond("Unexpected network call", HttpStatusCode.BadRequest)
        })

        val localFile = File.createTempFile("test_local_track", ".mp3")
        localFile.deleteOnExit()

        val localTrack = Track(
            id = "local_track_1",
            title = "Offline Song",
            artist = "Local Artist",
            album = "Local Album",
            year = 2024,
            source = localFile.absolutePath,
            durationMs = 180_000L,
            sourceProvider = TrackSource.LOCAL
        )

        // 1. Cover Art for local track does not trigger network
        val cover = CoverArtService.loadArtworkForTrack(localTrack, mockClient)
        // 2. Lyrics for local track does not trigger network
        val lyrics = LyricsService.getLyrics(localTrack, mockClient)

        // 3. Local playback resolves direct path without starting proxy
        val streamSource = StreamSource(url = localFile.absolutePath)
        val preparedUrl = com.example.musicapp.platform.prepareStreamUrlForPlayback(localTrack.id, streamSource)
        assertEquals(localFile.absolutePath, preparedUrl)

        // Assertions:
        // No network requests were fired
        assertEquals(0, networkRequestsCount)
        assertFalse(isAppHttpClientCreated)

        // Local file plays directly without proxy session registration
        assertEquals(0, LocalStreamingProxy.serverPort)
        assertEquals(0, LocalStreamingProxy.sessions.size)
    }

    @Test
    fun testOnlineSourcesDisabled_PreservesCloudTracksInLibraryDataWithoutShowingInVisibleTracks() {
        assertFalse(AppFeatures.isOnlineSourcesEnabled)
        val library = Library()

        val localTrack = Track(
            id = "loc_1",
            title = "My Local Track",
            artist = "Local Artist",
            album = "Local Album",
            year = 2024,
            source = "C:\\Music\\loc1.mp3",
            durationMs = 200_000L,
            sourceProvider = TrackSource.LOCAL
        )

        val cloudTrack = Track(
            id = "cloud_1",
            title = "Cloud Jamendo Track",
            artist = "Jamendo Artist",
            album = "Jamendo Album",
            year = 2024,
            source = "https://jamendo.com/stream/123",
            durationMs = 210_000L,
            sourceProvider = TrackSource.JAMENDO
        )

        library.tracks.clear()
        library.tracks.addAll(listOf(localTrack, cloudTrack))

        // visibleTracks only contains local tracks
        val visible = library.visibleTracks
        assertEquals(1, visible.size)
        assertEquals("loc_1", visible.first().id)

        // Raw tracks list preserves cloud track (no data loss)
        assertEquals(2, library.tracks.size)
        assertTrue(library.tracks.any { it.id == "cloud_1" })

        // When feature flag is flipped to true, cloud track immediately appears in visibleTracks
        AppFeatures.devOnlineSourcesEnabled = true
        val visibleOnline = library.visibleTracks
        assertEquals(2, visibleOnline.size)
        assertTrue(visibleOnline.any { it.id == "cloud_1" })
    }

    @Test
    fun testOnlineSourcesEnabled_RegistryInitializesProviders() {
        AppFeatures.devOnlineSourcesEnabled = true
        assertTrue(AppFeatures.isOnlineSourcesEnabled)

        val registry = ProviderRegistry()
        assertEquals(8, registry.all().size)
        assertNotNull(registry.gdrive)
        assertNotNull(registry.yandex)
        assertNotNull(registry.jamendo)
        assertNotNull(registry.audius)
        assertNotNull(registry.archive)
        assertNotNull(registry.subsonic)
        assertNotNull(registry.webdav)
        assertNotNull(registry.radio)
    }
}
