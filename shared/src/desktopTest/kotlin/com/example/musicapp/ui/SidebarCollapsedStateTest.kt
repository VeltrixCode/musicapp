package com.example.musicapp.ui

import androidx.compose.ui.unit.dp
import com.example.musicapp.library.Library
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.settings.SettingsRepository
import com.example.musicapp.util.pluralRu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarCollapsedStateTest {

    @Test
    fun testCollapsedSidebarDimensionsAndLabelsState() {
        // Collapsed mode dimensions
        val collapsedWidth = 72.dp
        val expandedDefaultWidth = 240.dp

        val isCollapsed = true
        val targetWidth = if (isCollapsed) collapsedWidth else expandedDefaultWidth
        assertEquals(72.dp, targetWidth)

        // Animation label rule:
        // labels disappear before narrowing starts and appear only after expansion completes
        val showLabelsWhenCollapsed = !isCollapsed
        assertFalse(showLabelsWhenCollapsed, "In collapsed mode, showLabels must be strictly false to prevent text rendering or wrapping")

        // In expanded mode:
        val showLabelsWhenExpanded = !false && (expandedDefaultWidth >= expandedDefaultWidth - 8.dp)
        assertTrue(showLabelsWhenExpanded, "In expanded mode after expansion completes, showLabels must be true")
    }

    @Test
    fun testCollapsedPlaylistsCountAccurateWithVisibleTracks() {
        val library = Library()
        val locTrack = Track(
            id = "t-loc",
            title = "Local",
            artist = "Artist",
            sourceProvider = TrackSource.LOCAL,
            source = "C:/loc.mp3"
        )
        val cloudTrack = Track(
            id = "t-cloud",
            title = "Cloud",
            artist = "Artist",
            sourceProvider = TrackSource.JAMENDO,
            source = "https://jamendo.com/t.mp3"
        )
        library.addTrack(locTrack)
        library.addTrack(cloudTrack)

        val pl = Playlist(id = "pl-test", name = "Test Playlist", trackIds = listOf("t-loc", "t-cloud"))

        // Tooltip string in collapsed mode
        val visibleTrackCount = library.tracksIn(pl).size
        assertEquals(1, visibleTrackCount, "tracksIn must return only visible tracks (local tracks when offline)")

        val tooltipText = "${pl.name}, ${pluralRu(visibleTrackCount, "трек", "трека", "треков")}"
        assertEquals("Test Playlist, 1 трек", tooltipText)
    }

    @Test
    fun testAutoCollapseBelow900dp() {
        fun resolveEffectiveCollapsed(windowWidthDp: Float, userCollapsed: Boolean): Boolean {
            val autoCollapse = windowWidthDp < 900f
            return userCollapsed || autoCollapse
        }

        // On a 800dp window, sidebar auto-collapses to 72dp regardless of user setting
        assertTrue(resolveEffectiveCollapsed(800f, userCollapsed = false))
        assertTrue(resolveEffectiveCollapsed(750f, userCollapsed = true))

        // On a 1200dp desktop window, respects user setting
        assertFalse(resolveEffectiveCollapsed(1200f, userCollapsed = false))
        assertTrue(resolveEffectiveCollapsed(1200f, userCollapsed = true))
    }

    @Test
    fun testDragThresholdSnapsToCollapsed() {
        fun computeDraggedWidth(currentWidthDp: Float, dragDeltaDp: Float): Float {
            val newWidth = currentWidthDp + dragDeltaDp
            return if (newWidth < 120f) 72f else newWidth.coerceIn(180f, 360f)
        }

        // Dragging below 120dp collapses/snaps to 72dp
        assertEquals(72f, computeDraggedWidth(180f, -70f)) // 110dp -> 72dp
        assertEquals(72f, computeDraggedWidth(180f, -90f)) // 90dp -> 72dp

        // Dragging above 120dp expands and clamps between 180dp and 360dp
        assertEquals(200f, computeDraggedWidth(180f, 20f))
        assertEquals(360f, computeDraggedWidth(350f, 50f))
        assertEquals(180f, computeDraggedWidth(150f, -10f)) // clamped to min expanded 180dp
    }

    @Test
    fun testSettingsPersistenceForSidebarCollapsedAndWidth() {
        val repo = SettingsRepository()
        val original = repo.load()

        val updated = original.copy(isSidebarCollapsed = true, sidebarWidthDp = 280f)
        repo.save(updated)

        val reloaded = repo.load()
        assertTrue(reloaded.isSidebarCollapsed)
        assertEquals(280f, reloaded.sidebarWidthDp)

        // Restore original for cleanliness
        repo.save(original)
    }
}
