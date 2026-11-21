package com.example.musicapp.platform

import com.example.musicapp.library.newId
import com.example.musicapp.model.Track
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Utility for extracting and importing audio tracks from ZIP archives and directories.
 * Provides protection against Zip-Slip path traversal vulnerabilities, supports nested
 * directories and archives, and extracts accompanying synchronized lyrics (.lrc files).
 */
object ZipAudioExtractor {

    val AUDIO_EXTENSIONS: Set<String> = setOf("mp3", "wav", "m4a", "aac", "flac", "ogg")
    val ARCHIVE_EXTENSIONS: Set<String> = setOf("zip")

    fun isAudioFile(file: File): Boolean =
        file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS

    fun isArchiveFile(file: File): Boolean =
        file.isFile && file.extension.lowercase() in ARCHIVE_EXTENSIONS

    fun getUploadsDir(): File =
        File(AppStorage.dir, "uploads").apply { mkdirs() }

    /**
     * Extracts all audio tracks from [zipFile] into [destinationDir].
     * Any accompanying `.lrc` lyrics files in the archive matching the track names
     * are automatically extracted alongside their audio files.
     * Nested ZIP archives are recursively unpacked.
     */
    fun extractTracksFromZip(
        zipFile: File,
        destinationDir: File = getUploadsDir()
    ): List<Track> {
        if (!zipFile.exists() || !zipFile.isFile) return emptyList()
        destinationDir.mkdirs()
        val canonicalDest = destinationDir.canonicalFile

        val (zf, closeable) = openZipFile(zipFile) ?: return emptyList()
        val extractedTracks = mutableListOf<Track>()
        val nestedZips = mutableListOf<File>()

        try {
            val entries = zf.entries().asSequence().toList()

            // Map lowercase base path -> ZipEntry for .lrc lyrics files
            // e.g. "subfolder/song" -> ZipEntry("subfolder/song.lrc")
            val lrcEntries = mutableMapOf<String, ZipEntry>()
            for (entry in entries) {
                if (entry.isDirectory) continue
                val normalizedName = entry.name.replace('\\', '/')
                if (normalizedName.contains("..")) continue
                val fileName = normalizedName.substringAfterLast('/')
                if (normalizedName.startsWith("__MACOSX/") || fileName.startsWith("._") || fileName.startsWith(".")) {
                    continue
                }
                val ext = fileName.substringAfterLast('.', "").lowercase()
                if (ext == "lrc") {
                    val base = normalizedName.substringBeforeLast('.').lowercase()
                    lrcEntries[base] = entry
                }
            }

            for (entry in entries) {
                if (entry.isDirectory) continue
                val normalizedName = entry.name.replace('\\', '/')
                if (normalizedName.contains("..")) continue
                val fileName = normalizedName.substringAfterLast('/')
                if (normalizedName.startsWith("__MACOSX/") || fileName.startsWith("._") || fileName.startsWith(".")) {
                    continue
                }

                // Check Zip-Slip path traversal: ensure entry name cannot escape destinationDir
                val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._\\-\\s\\u0400-\\u04FF]"), "_")
                if (safeFileName.isBlank() || safeFileName.contains("..")) {
                    continue
                }

                val ext = fileName.substringAfterLast('.', "").lowercase()

                if (ext in AUDIO_EXTENSIONS) {
                    val trackId = newId()
                    val destFile = File(destinationDir, "${trackId}_$safeFileName").canonicalFile
                    if (!destFile.path.startsWith(canonicalDest.path + File.separator)) {
                        // Malicious entry path detected, skip
                        continue
                    }

                    zf.getInputStream(entry).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Extract matching .lrc file if present in the archive
                    val baseName = normalizedName.substringBeforeLast('.').lowercase()
                    val matchingLrc = lrcEntries[baseName]
                    if (matchingLrc != null) {
                        val destLrc = File(destinationDir, "${destFile.nameWithoutExtension}.lrc").canonicalFile
                        if (destLrc.path.startsWith(canonicalDest.path + File.separator)) {
                            zf.getInputStream(matchingLrc).use { lrcIn ->
                                FileOutputStream(destLrc).use { lrcOut ->
                                    lrcIn.copyTo(lrcOut)
                                }
                            }
                        }
                    }

                    extractedTracks.add(importedTrack(destFile.absolutePath, safeFileName))
                } else if (ext in ARCHIVE_EXTENSIONS) {
                    val tempZip = File(destinationDir, "nested_${newId()}_$safeFileName").canonicalFile
                    if (tempZip.path.startsWith(canonicalDest.path + File.separator)) {
                        zf.getInputStream(entry).use { input ->
                            FileOutputStream(tempZip).use { output ->
                                input.copyTo(output)
                            }
                        }
                        nestedZips.add(tempZip)
                    }
                }
            }
        } finally {
            runCatching { closeable.close() }
        }

        // Recursively unpack nested ZIP archives
        for (nestedZip in nestedZips) {
            try {
                extractedTracks.addAll(extractTracksFromZip(nestedZip, destinationDir))
            } finally {
                runCatching { nestedZip.delete() }
            }
        }

        return extractedTracks
    }

    /**
     * Recursively walks [directory] and imports all supported audio files and ZIP archives.
     * Accompanying `.lrc` files next to audio files are copied alongside them into [destinationDir].
     */
    fun importDirectory(
        directory: File,
        destinationDir: File = getUploadsDir()
    ): List<Track> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        destinationDir.mkdirs()
        val canonicalDest = destinationDir.canonicalFile
        val tracks = mutableListOf<Track>()

        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in AUDIO_EXTENSIONS) {
                    val trackId = newId()
                    val destFile = File(destinationDir, "${trackId}_${file.name}").canonicalFile
                    if (destFile.path.startsWith(canonicalDest.path + File.separator)) {
                        runCatching { file.copyTo(destFile, overwrite = true) }

                        // Check accompanying .lrc in the same source directory
                        val lrc = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
                        if (lrc.exists() && lrc.isFile) {
                            val destLrc = File(destinationDir, "${destFile.nameWithoutExtension}.lrc").canonicalFile
                            if (destLrc.path.startsWith(canonicalDest.path + File.separator)) {
                                runCatching { lrc.copyTo(destLrc, overwrite = true) }
                            }
                        }

                        tracks.add(importedTrack(destFile.absolutePath, file.name))
                    }
                } else if (ext in ARCHIVE_EXTENSIONS) {
                    tracks.addAll(extractTracksFromZip(file, destinationDir))
                }
            }
        }

        return tracks
    }

    /**
     * Imports a single file (audio or ZIP archive) or recursively imports a directory.
     */
    fun importFileOrArchive(
        file: File,
        destinationDir: File = getUploadsDir()
    ): List<Track> {
        if (!file.exists()) return emptyList()
        return when {
            file.isDirectory -> importDirectory(file, destinationDir)
            isArchiveFile(file) -> extractTracksFromZip(file, destinationDir)
            isAudioFile(file) -> {
                destinationDir.mkdirs()
                val canonicalDest = destinationDir.canonicalFile
                val dest = File(destinationDir, "${newId()}_${file.name}").canonicalFile
                if (!dest.path.startsWith(canonicalDest.path + File.separator)) {
                    return emptyList()
                }
                file.copyTo(dest, overwrite = true)
                val lrc = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
                if (lrc.exists() && lrc.isFile) {
                    val destLrc = File(destinationDir, "${dest.nameWithoutExtension}.lrc").canonicalFile
                    if (destLrc.path.startsWith(canonicalDest.path + File.separator)) {
                        runCatching { lrc.copyTo(destLrc, overwrite = true) }
                    }
                }
                listOf(importedTrack(dest.absolutePath, file.name))
            }
            else -> emptyList()
        }
    }

    private fun openZipFile(zipFile: File): Pair<ZipFile, AutoCloseable>? {
        // Try UTF-8 first
        return try {
            val zf = ZipFile(zipFile, StandardCharsets.UTF_8)
            Pair(zf, zf)
        } catch (e: Exception) {
            // Fallback for Windows Russian archives (CP866)
            try {
                val zf = ZipFile(zipFile, Charset.forName("CP866"))
                Pair(zf, zf)
            } catch (e2: Exception) {
                // Fallback to platform default charset
                try {
                    val zf = ZipFile(zipFile)
                    Pair(zf, zf)
                } catch (e3: Exception) {
                    null
                }
            }
        }
    }
}
