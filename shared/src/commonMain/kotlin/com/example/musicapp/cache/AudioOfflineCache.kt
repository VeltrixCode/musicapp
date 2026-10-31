package com.example.musicapp.cache

import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.provider.appHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import java.io.File

object AudioOfflineCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Default 2 GB
    var maxSizeBytes: Long = 2L * 1024L * 1024L * 1024L

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val cacheDir: File get() = File(AppStorage.getCacheDir()).apply { mkdirs() }

    fun isCached(trackId: String): Boolean {
        return getCachedFile(trackId)?.exists() == true
    }

    fun getCachedPath(trackId: String): String? {
        val file = getCachedFile(trackId)
        return if (file != null && file.exists()) file.absolutePath else null
    }

    private fun getCachedFile(trackId: String): File? {
        val sanitized = trackId.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(cacheDir, "$sanitized.mp3")
    }

    fun downloadTrack(
        track: Track,
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
        onCompleted: (String) -> Unit = {}
    ) {
        val trackId = track.id
        if (isCached(trackId)) {
            getCachedPath(trackId)?.let(onCompleted)
            return
        }

        scope.launch {
            try {
                _downloadProgress.update { it + (trackId to 0.05f) }
                val targetFile = getCachedFile(trackId) ?: return@launch
                val tempFile = File(cacheDir, "${targetFile.name}.download")

                val response = appHttpClient.get(streamUrl) {
                    headers.forEach { (k, v) -> header(k, v) }
                }

                val totalLength = response.contentLength() ?: 0L
                val channel: ByteReadChannel = response.bodyAsChannel()
                var writtenBytes = 0L

                tempFile.outputStream().use { fos ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(8192)
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            fos.write(bytes)
                            writtenBytes += bytes.size
                            if (totalLength > 0) {
                                val progress = (writtenBytes.toFloat() / totalLength).coerceIn(0.05f, 0.99f)
                                _downloadProgress.update { it + (trackId to progress) }
                            }
                        }
                    }
                }

                tempFile.renameTo(targetFile)
                _downloadProgress.update { it - trackId }
                enforceLruLimit()
                onCompleted(targetFile.absolutePath)
            } catch (e: Exception) {
                _downloadProgress.update { it - trackId }
            }
        }
    }

    fun preloadNextTrack(track: Track?, streamUrl: String?, headers: Map<String, String> = emptyMap()) {
        if (track == null || streamUrl == null) return
        if (!isCached(track.id)) {
            downloadTrack(track, streamUrl, headers)
        }
    }

    fun deleteFromCache(trackId: String) {
        getCachedFile(trackId)?.delete()
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun getUsedSizeBytes(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun enforceLruLimit() {
        val files = cacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".download") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxSizeBytes) return

        val sorted = files.sortedBy { it.lastModified() }
        for (f in sorted) {
            total -= f.length()
            f.delete()
            if (total <= maxSizeBytes * 0.85) break
        }
    }
}
