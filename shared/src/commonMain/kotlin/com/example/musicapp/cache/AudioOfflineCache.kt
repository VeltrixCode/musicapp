package com.example.musicapp.cache

import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.provider.appHttpClient
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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

object AudioOfflineCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Default 2 GB
    var maxSizeBytes: Long = 2L * 1024L * 1024L * 1024L

    @Volatile
    var currentlyPlayingTrackId: String? = null

    @Volatile
    var currentlyQueuedTrackIds: Set<String> = emptySet()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    val cacheDir: File get() = File(AppStorage.getCacheDir()).apply { mkdirs() }

    fun isCached(trackId: String): Boolean {
        return getCachedFile(trackId)?.exists() == true
    }

    fun getCachedPath(trackId: String, touch: Boolean = true): String? {
        val file = getCachedFile(trackId)
        return if (file != null && file.exists()) {
            if (touch) {
                runCatching { file.setLastModified(System.currentTimeMillis()) }
            }
            file.absolutePath
        } else null
    }

    fun getCachedFile(trackId: String): File? {
        val sanitized = trackId.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(cacheDir, "$sanitized.mp3")
    }

    fun downloadTrack(
        track: Track,
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
        onCompleted: (String) -> Unit = {}
    ) {
        // Live radio streams cannot be downloaded for offline
        if (track.isLiveStream || track.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER) {
            return
        }
        val trackId = track.id
        if (isCached(trackId)) {
            getCachedPath(trackId)?.let(onCompleted)
            return
        }

        scope.launch {
            val targetFile = getCachedFile(trackId) ?: return@launch
            val tempFile = File(cacheDir, "${targetFile.name}.${UUID.randomUUID()}.download")

            try {
                _downloadProgress.update { it + (trackId to 0.05f) }

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
                    fos.flush()
                }

                // Atomic write: move temporary file to final target file
                atomicMove(tempFile, targetFile)

                _downloadProgress.update { it - trackId }
                enforceLruLimit()
                onCompleted(targetFile.absolutePath)
            } catch (e: Exception) {
                tempFile.delete()
                _downloadProgress.update { it - trackId }
            }
        }
    }

    private fun atomicMove(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            // Fallback for filesystems that do not support ATOMIC_MOVE
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    fun preloadNextTrack(track: Track?, streamUrl: String?, headers: Map<String, String> = emptyMap()) {
        if (track == null || streamUrl == null) return
        if (track.isLiveStream || track.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER) return
        if (!isCached(track.id)) {
            downloadTrack(track, streamUrl, headers)
        }
    }

    fun deleteFromCache(trackId: String) {
        getCachedFile(trackId)?.delete()
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        com.example.musicapp.service.CoverArtService.clearCoverCache()
    }

    fun getUsedSizeBytes(): Long {
        return cacheDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    fun enforceLruLimit(protectedIds: Set<String> = emptySet()) {
        val files = cacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".download") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxSizeBytes) return

        val combinedProtected = protectedIds +
                listOfNotNull(currentlyPlayingTrackId) +
                currentlyQueuedTrackIds

        val protectedFileNames = combinedProtected.mapNotNull { getCachedFile(it)?.name }.toSet()

        // Sort by least recently modified/accessed first
        val candidates = files.filter { it.name !in protectedFileNames }.sortedBy { it.lastModified() }
        for (f in candidates) {
            val len = f.length()
            if (f.delete()) {
                total -= len
            }
            if (total <= (maxSizeBytes * 0.85).toLong()) break
        }
    }
}
