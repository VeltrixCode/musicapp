package com.example.musicapp.proxy

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.StreamSource
import com.example.musicapp.platform.VlcBridge
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as CIOClient
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
import io.ktor.http.ContentType
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.routing
import io.ktor.utils.io.copyTo
import java.net.ServerSocket
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val PROXY_SESSION_GRACE_PERIOD_MS = 5_000L // 5 seconds grace window for initial registration
const val DIRECT_CDN_URL_TTL_MS = 10 * 60 * 1000L // 10 minutes sliding TTL for resolved direct CDN URL

data class ProxySession(
    val token: String,
    val trackId: String,
    @Volatile var streamSource: StreamSource,
    val onRefreshNeeded: (suspend () -> StreamSource?)? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    @Volatile var lastAccessMs: Long = System.currentTimeMillis(),
    val ttlMs: Long = 30 * 60 * 1000L, // 30 minutes sliding inactivity TTL
    @Volatile var resolvedDirectUrl: String? = null,
    @Volatile var resolvedDirectUrlExpiresAtMs: Long = 0L,
) {
    fun touch() {
        lastAccessMs = System.currentTimeMillis()
    }

    fun isDirectUrlValid(): Boolean {
        val url = resolvedDirectUrl
        return !url.isNullOrBlank() && System.currentTimeMillis() < resolvedDirectUrlExpiresAtMs
    }

    fun setDirectUrl(url: String, ttlMs: Long = DIRECT_CDN_URL_TTL_MS) {
        resolvedDirectUrl = url
        resolvedDirectUrlExpiresAtMs = System.currentTimeMillis() + ttlMs
    }

    fun clearDirectUrl() {
        resolvedDirectUrl = null
        resolvedDirectUrlExpiresAtMs = 0L
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

    private val proxyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val proxyHttpClient: HttpClient by lazy {
        HttpClient(CIOClient) {
            followRedirects = false
            engine {
                maxConnectionsCount = 10
                requestTimeout = 0  // unlimited for streaming
                endpoint.keepAliveTime = 60_000
                endpoint.connectTimeout = 10_000
            }
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
            configureStreamingProxyRoutes(
                application = this,
                sessionsMap = sessions,
                hostValidator = { host -> isValidHost(host) }
            )
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
        val session = ProxySession(
            token = sessionToken,
            trackId = trackId,
            streamSource = streamSource,
            onRefreshNeeded = onRefreshNeeded
        )
        sessions[sessionToken] = session

        // Proactive async pre-fetch of direct CDN URL (e.g. Google Drive / CDN) in background
        if (streamSource.url.startsWith("http://") || streamSource.url.startsWith("https://")) {
            proxyScope.launch {
                try {
                    if (!session.isDirectUrlValid()) {
                        fetchUpstream(
                            streamSource = streamSource,
                            rangeHeader = "bytes=0-0",
                            httpClient = proxyHttpClient,
                            directUrlOverride = null,
                            onRedirectResolved = { cdnUrl ->
                                session.setDirectUrl(cdnUrl)
                            }
                        )
                    }
                } catch (_: Throwable) {
                    // Pre-fetch is opportunistic; fallback to on-demand resolution in route
                }
            }
        }

        return "http://127.0.0.1:$serverPort/stream/$sessionToken"
    }

    fun registerSession(
        trackId: String,
        streamSource: StreamSource,
        onRefreshNeeded: (suspend () -> StreamSource?)? = null,
    ): String = registerStream(trackId, streamSource, onRefreshNeeded)

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
 * Resolves a concrete audio MIME type for LibVLC streaming, avoiding generic octet-stream fallback.
 */
fun resolveAudioContentType(streamSource: StreamSource, upstreamContentType: String?): String {
    val sourceCt = streamSource.contentType?.trim()
    val isGeneric = { ct: String? ->
        ct.isNullOrBlank() ||
        ct.equals("application/octet-stream", ignoreCase = true) ||
        ct.equals("binary/octet-stream", ignoreCase = true)
    }

    if (!isGeneric(sourceCt)) return sourceCt!!
    if (!isGeneric(upstreamContentType)) return upstreamContentType!!

    val url = streamSource.url.lowercase()
    return when {
        url.contains(".flac") -> "audio/flac"
        url.contains(".ogg") -> "audio/ogg"
        url.contains(".wav") -> "audio/wav"
        url.contains(".m4a") || url.contains(".aac") -> "audio/aac"
        else -> "audio/mpeg"
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
    configureStreamingProxyRoutes(
        application = application,
        sessionsMap = sessionsMap,
        hostValidator = hostValidator,
        clientProvider = { LocalStreamingProxy.proxyHttpClient }
    )
}

fun configureStreamingProxyRoutes(
    application: Application,
    sessionsMap: ConcurrentHashMap<String, ProxySession>,
    hostValidator: (String?) -> Boolean,
    clientProvider: () -> HttpClient,
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
            val knownType = resolveAudioContentType(session.streamSource, null)
            if (knownLength != null && knownLength > 0L) {
                call.response.header(HttpHeaders.ContentType, knownType)
                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                call.response.header(HttpHeaders.ContentLength, knownLength.toString())
                call.respond(HttpStatusCode.OK)
                return@head
            }

            try {
                val directUrlToUse = if (session.isDirectUrlValid()) session.resolvedDirectUrl else null
                if (directUrlToUse == null) {
                    session.clearDirectUrl()
                }

                var upstream = LocalStreamingProxy.fetchUpstream(
                    streamSource = session.streamSource,
                    rangeHeader = "bytes=0-0",
                    httpClient = clientProvider(),
                    directUrlOverride = directUrlToUse,
                    onRedirectResolved = { session.setDirectUrl(it) }
                )

                if (directUrlToUse != null && upstream.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.Gone, HttpStatusCode.NotFound)) {
                    session.clearDirectUrl()
                    upstream = LocalStreamingProxy.fetchUpstream(
                        streamSource = session.streamSource,
                        rangeHeader = "bytes=0-0",
                        httpClient = clientProvider(),
                        directUrlOverride = null,
                        onRedirectResolved = { session.setDirectUrl(it) }
                    )
                }

                val contentType = resolveAudioContentType(session.streamSource, upstream.headers[HttpHeaders.ContentType])
                val contentRange = upstream.headers[HttpHeaders.ContentRange]
                val totalLength = contentRange?.substringAfterLast('/')?.toLongOrNull()
                    ?: upstream.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    ?: session.streamSource.contentLength

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
            val incomingRange = call.request.headers[HttpHeaders.Range]
            val session = token?.let { sessionsMap[it] }?.takeIf { !it.isExpired() }
            if (session == null) {
                val exists = token != null && sessionsMap.containsKey(token)
                VlcBridge.logEngineError("Proxy 404: token=$token, existsInMap=$exists, sessionsCount=${sessionsMap.size}, Range=$incomingRange")
                if (token != null) sessionsMap.remove(token)
                call.respondText("Stream session not found or expired", status = HttpStatusCode.NotFound)
                return@get
            }

            // Touch session last access timestamp
            session.touch()

            try {
                var streamSource = session.streamSource
                val directUrlToUse = if (session.isDirectUrlValid()) session.resolvedDirectUrl else null
                if (directUrlToUse == null) {
                    session.clearDirectUrl()
                }

                var upstreamResponse = LocalStreamingProxy.fetchUpstream(
                    streamSource = streamSource,
                    rangeHeader = incomingRange,
                    httpClient = clientProvider(),
                    directUrlOverride = directUrlToUse,
                    onRedirectResolved = { session.setDirectUrl(it) }
                )

                // If cached direct URL failed (401/403/404/410), reset direct URL and retry once with original upstream URL
                if (directUrlToUse != null && upstreamResponse.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.Gone, HttpStatusCode.NotFound)) {
                    session.clearDirectUrl()
                    upstreamResponse = LocalStreamingProxy.fetchUpstream(
                        streamSource = streamSource,
                        rangeHeader = incomingRange,
                        httpClient = clientProvider(),
                        directUrlOverride = null,
                        onRedirectResolved = { session.setDirectUrl(it) }
                    )
                }

                // If original upstream also failed with 401/403/410 (access token expired), refresh stream source link once
                if (upstreamResponse.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.Gone)) {
                    val refreshed = session.onRefreshNeeded?.invoke()
                    if (refreshed != null) {
                        session.streamSource = refreshed
                        streamSource = refreshed
                        session.clearDirectUrl()
                        upstreamResponse = LocalStreamingProxy.fetchUpstream(
                            streamSource = streamSource,
                            rangeHeader = incomingRange,
                            httpClient = clientProvider(),
                            directUrlOverride = null,
                            onRedirectResolved = { session.setDirectUrl(it) }
                        )
                    }
                }

                if (upstreamResponse.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                    upstreamResponse.headers[HttpHeaders.ContentRange]?.let {
                        call.response.header(HttpHeaders.ContentRange, it)
                    }
                    VlcBridge.logEngineError("Proxy 416 Requested Range Not Satisfiable for token=$token (Range: $incomingRange)")
                    call.respond(HttpStatusCode.RequestedRangeNotSatisfiable)
                    return@get
                }

                if (upstreamResponse.status.value >= 400) {
                    VlcBridge.logEngineError("Proxy upstream HTTP error ${upstreamResponse.status.value} for token=$token (Range: $incomingRange, URL: ${streamSource.url.take(120)})")
                }

                val isRangeRequest = !incomingRange.isNullOrBlank()
                val resolvedContentType = resolveAudioContentType(session.streamSource, upstreamResponse.headers[HttpHeaders.ContentType])

                val upstreamContentRange = upstreamResponse.headers[HttpHeaders.ContentRange]
                val knownTotalLength = upstreamContentRange?.substringAfterLast('/')?.toLongOrNull()
                    ?: upstreamResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                    ?: session.streamSource.contentLength

                val (outgoingStatus, outgoingContentRange, outgoingContentLength) = when {
                    // Upstream already responded with 206 Partial Content
                    upstreamResponse.status == HttpStatusCode.PartialContent -> {
                        val cr = upstreamContentRange ?: if (knownTotalLength != null && knownTotalLength > 0L) {
                            "bytes 0-${knownTotalLength - 1}/$knownTotalLength"
                        } else null
                        Triple(HttpStatusCode.PartialContent, cr, upstreamResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull())
                    }

                    // Client requested Range (e.g. Range: bytes=0-), but upstream returned 200 OK
                    isRangeRequest && upstreamResponse.status == HttpStatusCode.OK -> {
                        val rangeRegex = Regex("""bytes=(\d+)-(\d+)?""")
                        val match = incomingRange?.let { rangeRegex.find(it) }
                        val start = match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
                        val explicitEnd = match?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }?.toLongOrNull()
                        val total = knownTotalLength ?: -1L
                        val end = explicitEnd ?: if (total > 0L) (total - 1L) else null

                        val cr = when {
                            total > 0L && end != null -> "bytes $start-$end/$total"
                            total > 0L -> "bytes $start-${total - 1}/$total"
                            end != null -> "bytes $start-$end/*"
                            else -> "bytes $start-*/*"
                        }
                        val len = if (end != null) (end - start + 1).coerceAtLeast(0L) else null
                        Triple(HttpStatusCode.PartialContent, cr, len)
                    }

                    else -> {
                        Triple(upstreamResponse.status, upstreamContentRange, upstreamResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull())
                    }
                }

                if (outgoingContentRange != null) {
                    call.response.header(HttpHeaders.ContentRange, outgoingContentRange)
                }
                call.response.header(HttpHeaders.AcceptRanges, "bytes")

                val channel = upstreamResponse.bodyAsChannel()
                val parsedContentType = runCatching {
                    ContentType.parse(resolvedContentType)
                }.getOrDefault(ContentType.Application.OctetStream)

                call.respondBytesWriter(
                    contentType = parsedContentType,
                    status = outgoingStatus,
                    contentLength = outgoingContentLength
                ) {
                    try {
                        channel.copyTo(this)
                    } catch (_: Throwable) {
                        // Игнорируем штатный обрыв клиентом (VLC) при нажатии Стоп или смене трека
                        channel.cancel(CancellationException("Client disconnected"))
                    }
                }
            } catch (e: Exception) {
                val errDetails = "${e.javaClass.simpleName}: ${e.message}"
                VlcBridge.logEngineError("Proxy stream exception for token=$token (Range: $incomingRange): $errDetails\n${e.stackTraceToString().take(600)}")
                call.respondText("Stream error: $errDetails", status = HttpStatusCode.BadGateway)
            }
        }
    }
}
