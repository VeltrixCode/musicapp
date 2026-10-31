package com.example.musicapp.dedup

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.AppStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
enum class VersionMarker(val displayLabel: String) {
    LIVE("Live"),
    REMIX("Remix"),
    ACOUSTIC("Acoustic"),
    INSTRUMENTAL("Instrumental"),
    COVER("Cover"),
    REMASTER("Remaster"),
    RADIO_EDIT("Radio Edit"),
    EXTENDED("Extended"),
    ORIGINAL("Original");

    companion object {
        private val EXTENDED_REGEX = Regex("""(?i)\b(extended|extended mix|long version)\b""")
        private val RADIO_EDIT_REGEX = Regex("""(?i)\b(radio edit|single edit|clean)\b""")
        private val REMASTER_REGEX = Regex("""(?i)\b(remaster(ed)?|20\d\d remaster|digital remaster)\b""")
        private val ACOUSTIC_REGEX = Regex("""(?i)\b(acoustic|unplugged)\b""")
        private val INSTRUMENTAL_REGEX = Regex("""(?i)\b(instrumental|karaoke|backing track)\b""")
        private val COVER_REGEX = Regex("""(?i)\b(cover|tribute)\b""")
        private val LIVE_REGEX = Regex("""(?i)\b(live|concert|in concert|at [a-z0-9\s]+)\b""")
        private val REMIX_REGEX = Regex("""(?i)\b(remix|rmx|vip mix|club mix|dub mix|dance mix)\b""")

        fun extract(title: String): VersionMarker {
            val lower = title.lowercase()
            return when {
                EXTENDED_REGEX.containsMatchIn(lower) -> EXTENDED
                RADIO_EDIT_REGEX.containsMatchIn(lower) -> RADIO_EDIT
                REMASTER_REGEX.containsMatchIn(lower) -> REMASTER
                ACOUSTIC_REGEX.containsMatchIn(lower) -> ACOUSTIC
                INSTRUMENTAL_REGEX.containsMatchIn(lower) -> INSTRUMENTAL
                COVER_REGEX.containsMatchIn(lower) -> COVER
                LIVE_REGEX.containsMatchIn(lower) -> LIVE
                REMIX_REGEX.containsMatchIn(lower) -> REMIX
                else -> ORIGINAL
            }
        }
    }
}

@Serializable
data class TrackVariant(
    val trackId: String,
    val sourceProvider: TrackSource,
    val format: String,
    val isLossless: Boolean,
    val bitrateKbps: Int?,
    val durationMs: Long,
    val isLocal: Boolean,
    val isCached: Boolean,
    val isCloud: Boolean,
    val license: String? = null,
    val webPageUrl: String? = null,
)

@Serializable
data class Work(
    val id: String,
    val title: String,
    val artist: String,
    val versionMarker: VersionMarker = VersionMarker.ORIGINAL,
    val variants: List<TrackVariant> = emptyList(),
    val preferredVariantId: String? = null,
    val isManual: Boolean = false,
)

@Serializable
data class DedupDecisions(
    val manualMerges: Map<String, String> = emptyMap(), // trackId -> workId
    val manualSplits: List<List<String>> = emptyList(), // pairs of trackIds that must not merge
    val preferredVariants: Map<String, String> = emptyMap(), // workId -> trackId
)

object DeduplicationStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private const val FILE_NAME = "dedup_decisions.json"
    private var cachedDecisions: DedupDecisions? = null

    fun load(): DedupDecisions {
        cachedDecisions?.let { return it }
        val raw = AppStorage.read(FILE_NAME) ?: return DedupDecisions()
        val loaded = runCatching { json.decodeFromString<DedupDecisions>(raw) }.getOrDefault(DedupDecisions())
        cachedDecisions = loaded
        return loaded
    }

    fun save(decisions: DedupDecisions) {
        cachedDecisions = decisions
        runCatching {
            AppStorage.write(FILE_NAME, json.encodeToString(decisions))
        }
    }

    fun mergeTracks(trackIds: Set<String>, existingWorkId: String? = null): String {
        val current = load()
        val targetWorkId = existingWorkId ?: "manual_work_" + trackIds.first().hashCode().toString(16)
        val updatedMerges = current.manualMerges.toMutableMap()
        trackIds.forEach { updatedMerges[it] = targetWorkId }
        save(current.copy(manualMerges = updatedMerges))
        return targetWorkId
    }

    fun splitVariant(workId: String, trackIdToDetach: String) {
        val current = load()
        val updatedMerges = current.manualMerges.toMutableMap()
        updatedMerges.remove(trackIdToDetach)

        // Find other track IDs in this manual work to forbid auto-merge
        val otherTracksInWork = current.manualMerges.filter { it.value == workId && it.key != trackIdToDetach }.keys
        val updatedSplits = current.manualSplits.toMutableList()
        otherTracksInWork.forEach { otherId ->
            updatedSplits.add(listOf(trackIdToDetach, otherId))
        }

        save(current.copy(manualMerges = updatedMerges, manualSplits = updatedSplits))
    }

    fun setPreferredVariant(workId: String, trackId: String) {
        val current = load()
        val updated = current.preferredVariants.toMutableMap()
        updated[workId] = trackId
        save(current.copy(preferredVariants = updated))
    }
}

object DeduplicationService {

    fun normalizeText(s: String): String =
        s.lowercase()
            .replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), " ")
            .replace(Regex("""[^a-z0-9а-яё\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun inferFormat(track: Track): String {
        track.format?.let { if (it.isNotBlank()) return it.lowercase() }
        val pathOrUrl = track.source?.lowercase() ?: ""
        return when {
            pathOrUrl.endsWith(".flac") -> "flac"
            pathOrUrl.endsWith(".wav") -> "wav"
            pathOrUrl.endsWith(".alac") -> "alac"
            pathOrUrl.endsWith(".mp3") -> "mp3"
            pathOrUrl.endsWith(".aac") || pathOrUrl.endsWith(".m4a") -> "aac"
            pathOrUrl.endsWith(".ogg") -> "ogg"
            pathOrUrl.endsWith(".opus") -> "opus"
            track.mimeType?.contains("flac") == true -> "flac"
            track.mimeType?.contains("wav") == true -> "wav"
            track.mimeType?.contains("mpeg") == true -> "mp3"
            track.mimeType?.contains("aac") == true -> "aac"
            track.mimeType?.contains("ogg") == true -> "ogg"
            else -> if (track.isLossless) "flac" else "mp3"
        }
    }

    fun isFormatLossless(format: String): Boolean =
        format in setOf("flac", "wav", "alac", "aiff", "pcm")

    fun toVariant(track: Track): TrackVariant {
        val fmt = inferFormat(track)
        val lossless = track.isLossless || isFormatLossless(fmt)
        val isCached = track.cachedFilePath != null || track.isOfflineAvailable
        val isLocal = track.sourceProvider == TrackSource.LOCAL
        val isCloud = track.isCloud
        return TrackVariant(
            trackId = track.id,
            sourceProvider = track.sourceProvider,
            format = fmt,
            isLossless = lossless,
            bitrateKbps = track.bitrateKbps ?: if (lossless) 1411 else 320,
            durationMs = track.durationMs,
            isLocal = isLocal,
            isCached = isCached,
            isCloud = isCloud,
            license = track.license,
            webPageUrl = track.webPageUrl
        )
    }

    /**
     * Checks whether two tracks can be automatically deduplicated into the same Work.
     * Order:
     * 1. MusicBrainz ID / ISRC match
     * 2. AcoustID fingerprint match
     * 3. Normalized artist + title + IDENTICAL VersionMarker + duration within ±3 seconds
     */
    fun canAutoMerge(t1: Track, t2: Track, manualSplits: List<List<String>> = emptyList()): Boolean {
        if (t1.id == t2.id) return true

        // Check manual split blacklist
        for (splitPair in manualSplits) {
            if ((splitPair.contains(t1.id) && splitPair.contains(t2.id))) {
                return false
            }
        }

        // 1. MusicBrainz recording ID match
        if (!t1.musicBrainzId.isNullOrBlank() && !t2.musicBrainzId.isNullOrBlank()) {
            if (t1.musicBrainzId.equals(t2.musicBrainzId, ignoreCase = true)) {
                return true
            }
        }

        // 1b. ISRC match
        if (!t1.isrc.isNullOrBlank() && !t2.isrc.isNullOrBlank()) {
            if (t1.isrc.equals(t2.isrc, ignoreCase = true)) {
                return true
            }
        }

        // 2. AcoustID fingerprint match
        if (!t1.audioFingerprint.isNullOrBlank() && !t2.audioFingerprint.isNullOrBlank()) {
            if (t1.audioFingerprint == t2.audioFingerprint) {
                return true
            }
        }

        // 3. Version markers MUST match (Live, Remix, Acoustic, etc. do NOT merge)
        val vm1 = VersionMarker.extract(t1.title)
        val vm2 = VersionMarker.extract(t2.title)
        if (vm1 != vm2) return false

        // 4. Normalized title & artist match
        val normTitle1 = normalizeText(t1.title)
        val normTitle2 = normalizeText(t2.title)
        if (normTitle1 != normTitle2 || normTitle1.isBlank()) return false

        val normArtist1 = normalizeText(t1.artist)
        val normArtist2 = normalizeText(t2.artist)
        if (normArtist1 != normArtist2 || normArtist1.isBlank()) return false

        // 5. Duration within ±3 seconds (3000 ms)
        if (t1.durationMs > 0L && t2.durationMs > 0L) {
            val diff = abs(t1.durationMs - t2.durationMs)
            if (diff > 3000L) return false
        }

        return true
    }

    /**
     * Groups a collection of tracks into Work entities, respecting manual decisions.
     */
    fun groupTracksIntoWorks(
        tracks: List<Track>,
        decisions: DedupDecisions = DeduplicationStore.load()
    ): List<Work> {
        val works = mutableListOf<Work>()
        val processedTrackIds = mutableSetOf<String>()

        // 1. Process manual merges first
        val manualGroups = tracks.filter { decisions.manualMerges.containsKey(it.id) }
            .groupBy { decisions.manualMerges[it.id]!! }

        manualGroups.forEach { (workId, groupTracks) ->
            val first = groupTracks.first()
            val variants = groupTracks.map { toVariant(it) }
            works.add(
                Work(
                    id = workId,
                    title = first.title,
                    artist = first.artist,
                    versionMarker = VersionMarker.extract(first.title),
                    variants = variants,
                    preferredVariantId = decisions.preferredVariants[workId],
                    isManual = true
                )
            )
            groupTracks.forEach { processedTrackIds.add(it.id) }
        }

        // 2. Process remaining tracks with automatic deduplication
        val remaining = tracks.filter { it.id !in processedTrackIds }
        val autoGroups = mutableListOf<MutableList<Track>>()

        for (track in remaining) {
            var matchedGroup: MutableList<Track>? = null
            for (group in autoGroups) {
                if (canAutoMerge(group.first(), track, decisions.manualSplits)) {
                    matchedGroup = group
                    break
                }
            }
            if (matchedGroup != null) {
                matchedGroup.add(track)
            } else {
                autoGroups.add(mutableListOf(track))
            }
        }

        // 3. Build Works from automatic groups
        for (group in autoGroups) {
            val first = group.first()
            val workId = "work_" + (normalizeText(first.artist) + "_" + normalizeText(first.title) + "_" + VersionMarker.extract(first.title).name).hashCode().toString(16)
            val variants = group.map { toVariant(it) }
            works.add(
                Work(
                    id = workId,
                    title = first.title,
                    artist = first.artist,
                    versionMarker = VersionMarker.extract(first.title),
                    variants = variants,
                    preferredVariantId = decisions.preferredVariants[workId],
                    isManual = false
                )
            )
        }

        return works
    }

    /**
     * Helper overload to pick the best track from a list of tracks directly.
     */
    fun pickBestVariant(tracks: List<Track>, preferredSource: TrackSource? = null): Track? {
        if (tracks.isEmpty()) return null
        val tracksById = tracks.associateBy { it.id }
        val dummyWork = Work(
            id = "temp",
            title = tracks.first().title,
            artist = tracks.first().artist,
            variants = tracks.map { toVariant(it) }
        )
        return pickBestVariant(dummyWork, tracksById, preferredSource)
    }

    /**
     * Selects the best variant for playback:
     * - User preferred variant if specified
     * - User preferred source if specified and present
     * - Local > Cached > Cloud
     * - Lossless > Lossy
     * - Higher bitrate
     */
    fun pickBestVariant(
        work: Work,
        tracksById: Map<String, Track>,
        preferredSource: TrackSource? = null
    ): Track? {
        if (work.variants.isEmpty()) return null

        // 1. Explicit user choice for this work
        if (work.preferredVariantId != null) {
            val preferredTrack = tracksById[work.preferredVariantId]
            if (preferredTrack != null) return preferredTrack
        }

        // 2. Global preferred source if set and available in variants
        if (preferredSource != null) {
            val matchedBySource = work.variants.filter { it.sourceProvider == preferredSource }
            if (matchedBySource.isNotEmpty()) {
                val bestInSource = matchedBySource.maxWithOrNull(variantComparator())
                if (bestInSource != null) {
                    val tr = tracksById[bestInSource.trackId]
                    if (tr != null) return tr
                }
            }
        }

        // 3. Ranked comparison: Local > Cached > Cloud, then Lossless > Lossy, then bitrate
        val bestVariant = work.variants.maxWithOrNull(variantComparator())
        return bestVariant?.let { tracksById[it.trackId] } ?: tracksById[work.variants.first().trackId]
    }

    private fun variantComparator(): Comparator<TrackVariant> {
        return Comparator { a, b ->
            // Availability score: Local (3) > Cached (2) > Cloud (1)
            val availA = if (a.isLocal) 3 else if (a.isCached) 2 else 1
            val availB = if (b.isLocal) 3 else if (b.isCached) 2 else 1
            if (availA != availB) return@Comparator availA.compareTo(availB)

            // Lossless (1) > Lossy (0)
            val lossA = if (a.isLossless) 1 else 0
            val lossB = if (b.isLossless) 1 else 0
            if (lossA != lossB) return@Comparator lossA.compareTo(lossB)

            // Bitrate score
            val brA = a.bitrateKbps ?: 0
            val brB = b.bitrateKbps ?: 0
            brA.compareTo(brB)
        }
    }
}
