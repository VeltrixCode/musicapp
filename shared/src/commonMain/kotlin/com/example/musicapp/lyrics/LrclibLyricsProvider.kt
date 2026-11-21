package com.example.musicapp.lyrics

import com.example.musicapp.model.LrcLine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLQueryComponent
import kotlinx.serialization.Serializable

@Serializable
data class LrclibResponse(
    val id: Long? = null,
    val name: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
)

object LrclibLyricsProvider {
    const val USER_AGENT = "JVXMusic/1.0 (https://github.com/VeltrixCode/MusicApp)"

    // Regex for parenthetical/bracket tails: (original), (remastered...), (live...), (feat. ...), [feat. ...], (2020 Remaster), etc.
    private val PARENTHETICAL_TAIL_REGEX = Regex(
        """\s*[\(\[][^\)\]]*(?:remaster|live|original|feat\.?|ft\.?|featuring|deluxe|bonus|version|edit|mono|stereo|anniversary|mix|acoustic|instrumental|video|audio)[^\)\]]*[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    private val TRAILING_FEAT_REGEX = Regex(
        """\s+(?:feat\.?|ft\.?|featuring)\s+.*$""",
        RegexOption.IGNORE_CASE
    )

    private val PARENTHETICAL_FEAT_REGEX = Regex(
        """\s*[\(\[](?:feat\.?|ft\.?|featuring)\s+[^\)\]]+[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    private val SUBTITLE_DELIMITER_REGEX = Regex(
        """\s+[-–—/:|]\s+.*$"""
    )

    private val ANY_PARENTHETICAL_REGEX = Regex(
        """\s*[\(\[][^\)\]]*[\)\]]"""
    )

    private val MULTI_ARTIST_DELIMITER_REGEX = Regex(
        """\s*(?:,|&|/|\bx\b|\bvs\.?|\bwith\b)\s+.*$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Cleans track title: strips parenthetical/bracket tails such as (original), (remastered...), (live...), (feat. ...), [feat. ...], (2020 Remaster).
     */
    fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        var prev: String
        do {
            prev = cleaned
            cleaned = PARENTHETICAL_TAIL_REGEX.replace(cleaned, "").trim()
        } while (cleaned != prev)
        cleaned = TRAILING_FEAT_REGEX.replace(cleaned, "").trim()
        return if (cleaned.isNotBlank()) cleaned else title.trim()
    }

    /**
     * Cleans artist if it contains featured artists feat. / ft.
     */
    fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        cleaned = PARENTHETICAL_FEAT_REGEX.replace(cleaned, "").trim()
        cleaned = TRAILING_FEAT_REGEX.replace(cleaned, "").trim()
        return if (cleaned.isNotBlank()) cleaned else artist.trim()
    }

    /**
     * Shortens title by removing subtitles, delimiters (-, /, etc.) and any remaining brackets/parentheses.
     */
    fun shortenTitle(title: String): String {
        var shortened = cleanTitle(title)
        shortened = SUBTITLE_DELIMITER_REGEX.replace(shortened, "").trim()
        shortened = ANY_PARENTHETICAL_REGEX.replace(shortened, "").trim()
        if (shortened.contains(" - ")) {
            shortened = shortened.substringBefore(" - ").trim()
        }
        return if (shortened.isNotBlank()) shortened else cleanTitle(title)
    }

    /**
     * Shortens artist by taking the primary artist (stripping delimiters ',', '&', '/', 'x', 'vs', etc.).
     */
    fun shortenArtist(artist: String): String {
        var shortened = cleanArtist(artist)
        shortened = MULTI_ARTIST_DELIMITER_REGEX.replace(shortened, "").trim()
        return if (shortened.isNotBlank()) shortened else cleanArtist(artist)
    }

    /**
     * Queries LRCLIB API directly. Returns pair of (LrcLines, rawLyricsString) or null if not found.
     */
    suspend fun queryLrclib(
        client: HttpClient,
        artist: String,
        title: String,
        durationMs: Long = 0L,
        includeDuration: Boolean = true
    ): Pair<List<LrcLine>, String>? {
        val encodedArtist = artist.encodeURLQueryComponent()
        val encodedTitle = title.encodeURLQueryComponent()
        val durationSec = if (includeDuration && durationMs > 0L) (durationMs / 1000).toString() else ""
        val durationParam = if (durationSec.isNotEmpty()) "&duration=$durationSec" else ""
        val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle$durationParam"

        return runCatching {
            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }
            if (response.status != HttpStatusCode.OK) {
                return@runCatching null
            }
            val resp = response.body<LrclibResponse>()

            val synced = resp.syncedLyrics
            if (synced != null && synced.isNotBlank()) {
                val lines = LrcParser.parse(synced)
                if (lines.isNotEmpty()) {
                    return@runCatching Pair(lines, synced)
                }
            }

            val plain = resp.plainLyrics
            if (plain != null && plain.isNotBlank()) {
                val lines = plain.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                    LrcLine(timestampMs = idx * 4000L, text = line)
                }
                if (lines.isNotEmpty()) {
                    return@runCatching Pair(lines, plain)
                }
            }
            null
        }.getOrNull()
    }

    /**
     * Fetches lyrics with primary cleaned query and fallback shortened query.
     */
    suspend fun fetchLyrics(
        client: HttpClient,
        artist: String,
        title: String,
        durationMs: Long = 0L
    ): Pair<List<LrcLine>, String>? {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        // 1. Initial query with cleaned title and artist
        var result = queryLrclib(
            client = client,
            artist = cleanedArtist,
            title = cleanedTitle,
            durationMs = durationMs,
            includeDuration = durationMs > 0L
        )

        // 2. If initial query fails (returns null or empty), repeat query with shortened title and artist
        if (result == null || result.first.isEmpty()) {
            val shortenedTitle = shortenTitle(cleanedTitle)
            val shortenedArtist = shortenArtist(cleanedArtist)

            if (shortenedTitle != cleanedTitle || shortenedArtist != cleanedArtist) {
                result = queryLrclib(
                    client = client,
                    artist = shortenedArtist,
                    title = shortenedTitle,
                    durationMs = durationMs,
                    includeDuration = durationMs > 0L
                )
            }

            // If still not found and duration was specified, retry without duration filter
            if ((result == null || result.first.isEmpty()) && durationMs > 0L) {
                val queryTitle = if (shortenedTitle.isNotBlank()) shortenedTitle else cleanedTitle
                val queryArtist = if (shortenedArtist.isNotBlank()) shortenedArtist else cleanedArtist
                result = queryLrclib(
                    client = client,
                    artist = queryArtist,
                    title = queryTitle,
                    durationMs = 0L,
                    includeDuration = false
                )
            }
        }

        return result
    }
}
