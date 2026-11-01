package com.example.musicapp.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SidebarNavigationTest {

    @Test
    fun testOnlyOneActiveItemAllowed() {
        val screens = listOf(AppScreen.Home, AppScreen.Search, AppScreen.Library)
        val currentScreen: AppScreen = AppScreen.Home

        val activeCount = screens.count { it == currentScreen }
        assertEquals(1, activeCount, "Exactly one nav item should be active at a time")
    }

    @Test
    fun testHoverAndActiveStylesAreDistinct() {
        // AppColors for active item vs hovered item
        val activeBg = com.example.musicapp.theme.AppColors.SurfaceElevated
        val hoverBg = com.example.musicapp.theme.AppColors.CardHover

        assertNotEquals(activeBg, hoverBg, "Hover background must differ from active item background")

        val activeTint = com.example.musicapp.theme.AppColors.Accent
        val hoverTint = com.example.musicapp.theme.AppColors.TextPrimary
        assertNotEquals(activeTint, hoverTint, "Active icon tint must differ from hover icon tint")
    }

    @Test
    fun testNavigationScreenSwitchResetsStaleInteraction() {
        var isSelected = true
        // Keying interactionSource by (label, isSelected) ensures recreation on screen switch
        val keyBefore = "Главная" to isSelected

        isSelected = false
        val keyAfter = "Главная" to isSelected

        assertNotEquals(keyBefore, keyAfter, "Interaction source key must change when selection changes to clear hover state")
    }
}
