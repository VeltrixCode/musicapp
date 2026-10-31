package com.example.musicapp.proxy

import com.example.musicapp.model.StreamSource
import com.example.musicapp.provider.appHttpClient
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
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

object LocalStreamingProxy {
    private val registeredStreams = ConcurrentHashMap<String, StreamSource>()
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    var serverPort: Int = 0
        private set

    init {
        start()
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

        server = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            routing {
                get("/stream/{id}") {
                    val trackId = call.parameters["id"]
                    val stream = trackId?.let { registeredStreams[it] }
                    if (stream == null) {
                        call.respondText("Track stream not registered", status = HttpStatusCode.NotFound)
                        return@get
                    }

                    val incomingRange = call.request.headers[HttpHeaders.Range]

                    try {
                        val upstreamResponse = appHttpClient.get(stream.url) {
                            stream.headers.forEach { (k, v) -> header(k, v) }
                            if (incomingRange != null) {
                                header(HttpHeaders.Range, incomingRange)
                            }
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

                        val status = if (incomingRange != null) HttpStatusCode.PartialContent else HttpStatusCode.OK
                        val channel = upstreamResponse.bodyAsChannel()
                        call.respondOutputStream(status = status) {
                            channel.copyTo(this)
                        }
                    } catch (e: Exception) {
                        call.respondText("Stream proxy error: ${e.message}", status = HttpStatusCode.BadGateway)
                    }
                }
            }
        }.start(wait = false)
    }

    fun registerStream(trackId: String, streamSource: StreamSource): String {
        registeredStreams[trackId] = streamSource
        if (server == null) start()
        return "http://127.0.0.1:$serverPort/stream/$trackId"
    }

    fun unregisterStream(trackId: String) {
        registeredStreams.remove(trackId)
    }
}
