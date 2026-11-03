package com.example.musicapp.proxy

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.StreamSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import java.net.ServerSocket
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

const val PROXY_SESSION_GRACE_PERIOD_MS = 5_000L // 5 seconds grace window for initial registration

data class ProxySession(
    val token: String,
    val trackId: String,
    @Volatile var streamSource: StreamSource,
    val onRefreshNeeded: (suspend () -> StreamSource?)? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    @Volatile var lastAccessMs: Long = System.currentTimeMillis(),
    val ttlMs: Long = 30 * 60 * 1000L, // 30 minutes sliding inactivity TTL
    @Volatile var resolvedDirectUrl: String? = null,
) {
    fun touch() {
        lastAccessMs = System.currentTimeMillis()
    }

    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        // Sliding timeout: expires after 30 minutes of complete inactivity
        if (now - lastAccessMs > ttlMs) return true

        // Fact of playback: active playing track or queued tracks never expire prematurely
        val activeTrack = LocalStreamingProxy.activePlayingTrackId ?: AudioOfflineCache.currentlyPlayingTrackId
        val queuedTracks = if (LocalStreamingProxy.activeQueuedTrackIds.isNotEmpty()) {
            LocalStreamingProxy.activeQueuedTrackIds
        } else {
            AudioOfflineCache.currentlyQueuedTrackIds
        }

        // 1. If this track is actively playing or queued, it stays alive regardless of age
        if (trackId == activeTrack || queuedTracks.contains(trackId)) {
            return false
        }

        // 2. If active playback is established, tracks outside active/queue expire immediately
        val hasActivePlayback = activeTrack != null || queuedTracks.isNotEmpty()
        if (hasActivePlayback) {
            return true
        }

        // 3. If playback is not yet established, newly registered session is protected within initial grace period
        if (now - createdAtMs < PROXY_SESSION_GRACE_PERIOD_MS) {
            return false
        }

        // Grace period elapsed without becoming active or queued
        return true
    }
}

object LocalStreamingProxy {
    @Volatile
    var activePlayingTrackId: String? = null
    val activeQueuedTrackIds = ConcurrentHashMap.newKeySet<String>()

    fun setActivePlayingTrack(trackId: String?) {
        activePlayingTrackId = trackId
    }

    fun setActiveQueuedTracks(trackIds: Collection<String>) {
        activeQueuedTrackIds.clear()
        activeQueuedTrackIds.addAll(trackIds)
    }

    val sessions = ConcurrentHashMap<String, ProxySession>()
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    var serverPort: Int = 0
        private set

    val proxyHttpClient: HttpClient by lazy {
        HttpClient {
            followRedirects = false
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { ss ->
            return ss.localPort
        }
    }

    @Synchronized
    fun start() {
        if (server != null) return
        val port = findFreePort()
        serverPort = port

        // Listen STRICTLY on 127.0.0.1 (Loopback only)
        server = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            configureStreamingProxyRoutes(this, sessions) { host -> isValidHost(host) }
        }.start(wait = false)
    }

    fun isSameOrigin(from: URI, to: URI): Boolean {
        val sameScheme = from.scheme.equals(to.scheme, ignoreCase = true)
        val sameHost = from.host?.equals(to.host, ignoreCase = true) == true
        val fromPort = if (from.port != -1) from.port else (if (from.scheme.equals("https", true)) 443 else 80)
        val toPort = if (to.port != -1) to.port else (if (to.scheme.equals("https", true)) 443 else 80)
        val samePort = fromPort == toPort
        val isDowngrade = from.scheme.equals("https", ignoreCase = true) && to.scheme.equals("http", ignoreCase = true)
        return sameScheme && sameHost && samePort && !isDowngrade
    }

    suspend fun fetchUpstream(
        streamSource: StreamSource,
        rangeHeader: String?,
        httpClient: HttpClient = proxyHttpClient,
        maxRedirectHops: Int = 5,
        directUrlOverride: String? = null,
        onRedirectResolved: ((String) -> Unit)? = null,
    ): HttpResponse {
        val initialUri = runCatching { URI(directUrlOverride ?: streamSource.url) }.getOrNull()
            ?: throw IllegalArgumentException("Invalid stream source URL: ${directUrlOverride ?: streamSource.url}")

        var currentUri = initialUri
        var currentHeaders = if (directUrlOverride != null) emptyMap() else streamSource.headers.toMap()
        var hops = 0

        while (true) {
            val response = httpClient.get(currentUri.toString()) {
                header(HttpHeaders.UserAgent, "JVXMusic/1.0 (Desktop Player)")
                currentHeaders.forEach { (k, v) -> header(k, v) }
                if (rangeHeader != null) {
                    header(HttpHeaders.Range, rangeHeader)
                }
            }

            val status = response.status
            val isRedirect = status in listOf(
                HttpStatusCode.MovedPermanently, // 301
                HttpStatusCode.Found,            // 302
                HttpStatusCode.SeeOther,         // 303
                HttpStatusCode.TemporaryRedirect,// 307
                HttpStatusCode.PermanentRedirect// 308
            )

            if (!isRedirect) {
                if (currentUri != initialUri) {
                    onRedirectResolved?.invoke(currentUri.toString())
                }
                return response
            }

            val location = response.headers[HttpHeaders.Location]
            if (location.isNullOrBlank()) {
                return response
            }

            hops++
            if (hops > maxRedirectHops) {
                return response
            }

            val nextUri = runCatching {
                currentUri.resolve(location)
            }.getOrNull() ?: runCatching { URI(location) }.getOrNull() ?: return response

            // Strip Authorization and Cookie headers on foreign host/port or https->http downgrade
            val sameOrigin = isSameOrigin(initialUri, nextUri)
            if (!sameOrigin) {
                currentHeaders = currentHeaders.filterNot { (k, _) ->
                    k.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                            k.equals(HttpHeaders.Cookie, ignoreCase = true)
                }
            }

            currentUri = nextUri
        }
    }

    fun isValidHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val expected1 = "127.0.0.1:$serverPort"
        val expected2 = "localhost:$serverPort"
        val expected3 = "127.0.0.1"
        val expected4 = "localhost"
        return host == expected1 || host == expected2 || host == expected3 || host == expected4
    }

    /**
     * Registers a new playback session with a random single-use token and sliding TTL.
     */
    fun registerStream(
        trackId: String,
        streamSource: StreamSource,
        onRefreshNeeded: (suspend () -> StreamSource?)? = null,
    ): String {
        if (server == null) start()
        cleanupExpiredSessions()

        val sessionToken = UUID.randomUUID().toString().replace("-", "")
        sessions[sessionToken] = ProxySession(
            token = sessionToken,
            trackId = trackId,
            streamSource = streamSource,
            onRefreshNeeded = onRefreshNeeded
        )
        return "http://127.0.0.1:$serverPort/stream/$sessionToken"
    }

    fun invalidateSession(token: String) {
        sessions.remove(token)
    }

    fun invalidateSessionsForTrack(trackId: String) {
        sessions.entries.removeIf { it.value.trackId == trackId }
    }

    fun invalidateAllSessions() {
        sessions.clear()
    }

    fun cleanupExpiredSessions() {
        sessions.entries.removeIf { it.value.isExpired() }
    }

    @Synchronized
    fun stop() {
        try {
            server?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
            server = null
            serverPort = 0
            sessions.clear()
        } catch (_: Exception) {}
    }
}

/**
 * Route configuration shared between the live server and Ktor testApplication.
 */
fun configureStreamingProxyRoutes(
    application: Application,
    sessionsMap: ConcurrentHashMap<String, ProxySession>,
    hostValidator: (String?) -> Boolean = { host ->
        host?.startsWith("127.0.0.1") == true || host?.startsWith("localhost") == true
    }
) {
    application.routing {
        head("/stream/{token}") {
            val host = call.request.headers[HttpHeaders.Host]
            if (!hostValidator(host)) {
                call.respond(HttpStatusCode.Forbidden, "Invalid Host header")
                return@head
            }

            val token = call.parameters["token"]
            val session = token?.let { sessionsMap[it] }?.takeIf { !it.isExpired() }
            if (session == null) {
                if (token != null) sessionsMap.remove(token)
                call.respond(HttpStatusCode.NotFound)
                return@head
            }

            // Touch session last access timestamp
            session.touch()

            // If content length and type are already known from provider metadata, respond immediately (0ms latency)
            val knownLength = session.streamSource.contentLength
            val knownType = session.streamSource.contentType ?: "audio/mpeg"
            if (knownLength != null && knownLength > 0L) {
                call.response.header(HttpHeaders.ContentType, knownType)
                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                call.response.header(HttpHeaders.ContentLength, knownLength.toString())
                call.respond(HttpStatusCode.OK)
                return@head
            }

            try {
                val upstream = LocalStreamingProxy.fetchUpstream(
                    streamSource = session.streamSource,
                    rangeHeader = "bytes=0-0",
                    directUrlOverride = session.resolvedDirectUrl,
                    onRedirectResolved = { session.resolvedDirectUrl = it }
                )
                val contentType = upstream.headers[HttpHeaders.ContentType] ?: "audio/mpeg"
                val contentRange = upstream.headers[HttpHeaders.ContentRange]
                val totalLength = contentRange?.substringAfterLast('/')?.toLongOrNull()
                    ?: upstream.headers[HttpHeaders.ContentLength]?.toLongOrNull()

                call.response.header(HttpHeaders.ContentType, contentType)
                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                if (totalLength != null && totalLength > 0) {
                    call.response.header(HttpHeaders.ContentLength, totalLength.toString())
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadGateway)
            }
        }

        get("/stream/{token}") {
            val host = call.request.headers[HttpHeaders.Host]
            if (!hostValidator(host)) {
                call.respond(HttpStatusCode.Forbidden, "Invalid Host header")
                return@get
            }

            val token = call.parameters["token"]
            val session = token?.let { sessionsMap[it] }?.takeIf { !it.isExpired() }
            if (session == null) {
                if (token != null) sessionsMap.remove(token)
                call.respondText("Stream session not found or expired", status = HttpStatusCode.NotFound)
                return@get
            }

            // Touch session last access timestamp
            session.touch()

            val incomingRange = call.request.headers[HttpHeaders.Range]

            try {
                var streamSource = session.streamSource
                var upstreamResponse = LocalStreamingProxy.fetchUpstream(
                    streamSource = streamSource,
                    rangeHeader = incomingRange,
                    directUrlOverride = session.resolvedDirectUrl,
                    onRedirectResolved = { session.resolvedDirectUrl = it }
                )

                // If cached direct URL failed (e.g. expired CDN URL with 403/404/410), fall back to original upstream URL
                if (session.resolvedDirectUrl != null && upstreamResponse.status in listOf(HttpStatusCode.Forbidden, HttpStatusCode.Gone, HttpStatusCode.NotFound)) {
                    session.resolvedDirectUrl = null
                    upstreamResponse = LocalStreamingProxy.fetchUpstream(
                        streamSource = streamSource,
                        rangeHeader = incomingRange,
                        onRedirectResolved = { session.resolvedDirectUrl = it }
                    )
                }

                // Upstream 401/403/410 link refresh & retry
                if (upstreamResponse.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.Gone)) {
                    val refreshed = session.onRefreshNeeded?.invoke()
                    if (refreshed != null) {
                        session.streamSource = refreshed
                        streamSource = refreshed
                        session.resolvedDirectUrl = null
                        upstreamResponse = LocalStreamingProxy.fetchUpstream(
                            streamSource = streamSource,
                            rangeHeader = incomingRange,
                            onRedirectResolved = { session.resolvedDirectUrl = it }
                        )
                    }
                }

                if (upstreamResponse.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                    upstreamResponse.headers[HttpHeaders.ContentRange]?.let {
                        call.response.header(HttpHeaders.ContentRange, it)
                    }
                    call.respond(HttpStatusCode.RequestedRangeNotSatisfiable)
                    return@get
                }

                upstreamResponse.headers[HttpHeaders.ContentType]?.let {
                    call.response.header(HttpHeaders.ContentType, it)
                } ?: call.response.header(HttpHeaders.ContentType, "audio/mpeg")

                upstreamResponse.headers[HttpHeaders.ContentLength]?.let {
                    call.response.header(HttpHeaders.ContentLength, it)
                }
                upstreamResponse.headers[HttpHeaders.ContentRange]?.let {
                    call.response.header(HttpHeaders.ContentRange, it)
                }
                call.response.header(HttpHeaders.AcceptRanges, "bytes")

                val status = upstreamResponse.status
                val channel = upstreamResponse.bodyAsChannel()
                call.respondOutputStream(status = status) {
                    channel.copyTo(this)
                }
            } catch (e: Exception) {
                call.respondText("Stream error", status = HttpStatusCode.BadGateway)
            }
        }
    }
}
