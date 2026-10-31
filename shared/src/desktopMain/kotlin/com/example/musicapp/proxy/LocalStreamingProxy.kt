package com.example.musicapp.proxy

import com.example.musicapp.model.StreamSource
import com.example.musicapp.provider.appHttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ProxySession(
    val token: String,
    val trackId: String,
    @Volatile var streamSource: StreamSource,
    val onRefreshNeeded: (suspend () -> StreamSource?)? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
)

object LocalStreamingProxy {
    private val sessions = ConcurrentHashMap<String, ProxySession>()
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    var serverPort: Int = 0
        private set

    init {
        start()
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
            routing {
                head("/stream/{token}") {
                    val host = call.request.headers[HttpHeaders.Host]
                    if (!isValidHost(host)) {
                        call.respond(HttpStatusCode.Forbidden, "Invalid Host header")
                        return@head
                    }

                    val token = call.parameters["token"]
                    val session = token?.let { sessions[it] }
                    if (session == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@head
                    }

                    try {
                        val upstream = appHttpClient.get(session.streamSource.url) {
                            session.streamSource.headers.forEach { (k, v) -> header(k, v) }
                            header(HttpHeaders.Range, "bytes=0-0")
                        }

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
                    if (!isValidHost(host)) {
                        call.respond(HttpStatusCode.Forbidden, "Invalid Host header")
                        return@get
                    }

                    val token = call.parameters["token"]
                    val session = token?.let { sessions[it] }
                    if (session == null) {
                        call.respondText("Stream session not found or expired", status = HttpStatusCode.NotFound)
                        return@get
                    }

                    val incomingRange = call.request.headers[HttpHeaders.Range]

                    try {
                        var streamSource = session.streamSource
                        var upstreamResponse = fetchUpstream(streamSource, incomingRange)

                        // Requirement 4: If Yandex Disk / cloud link expired (401, 403, 410), refresh link without stopping playback
                        if (upstreamResponse.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.Gone)) {
                            val refreshed = session.onRefreshNeeded?.invoke()
                            if (refreshed != null) {
                                session.streamSource = refreshed
                                streamSource = refreshed
                                upstreamResponse = fetchUpstream(streamSource, incomingRange)
                            }
                        }

                        if (upstreamResponse.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                            upstreamResponse.headers[HttpHeaders.ContentRange]?.let {
                                call.response.header(HttpHeaders.ContentRange, it)
                            }
                            call.respond(HttpStatusCode.RequestedRangeNotSatisfiable)
                            return@get
                        }

                        // Forward headers
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
        }.start(wait = false)
    }

    private suspend fun fetchUpstream(streamSource: StreamSource, rangeHeader: String?) =
        appHttpClient.get(streamSource.url) {
            header(HttpHeaders.UserAgent, "JVXMusic/1.0 (Desktop Player)")
            streamSource.headers.forEach { (k, v) -> header(k, v) }
            if (rangeHeader != null) {
                header(HttpHeaders.Range, rangeHeader)
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
     * Registers a new playback session with a cryptographically random single-use token.
     * Does NOT expose track ID or credentials in URL.
     */
    fun registerStream(
        trackId: String,
        streamSource: StreamSource,
        onRefreshNeeded: (suspend () -> StreamSource?)? = null,
    ): String {
        if (server == null) start()
        val sessionToken = UUID.randomUUID().toString().replace("-", "")
        sessions[sessionToken] = ProxySession(
            token = sessionToken,
            trackId = trackId,
            streamSource = streamSource,
            onRefreshNeeded = onRefreshNeeded
        )
        return "http://127.0.0.1:$serverPort/stream/$sessionToken"
    }

    fun unregisterSession(token: String) {
        sessions.remove(token)
    }

    fun clearSessionsForTrack(trackId: String) {
        sessions.entries.removeIf { it.value.trackId == trackId }
    }

    @Synchronized
    fun stop() {
        try {
            server?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
            server = null
            sessions.clear()
        } catch (_: Exception) {}
    }
}
