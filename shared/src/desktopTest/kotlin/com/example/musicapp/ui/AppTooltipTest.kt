package com.example.musicapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.musicapp.ui.components.AppTooltip
import com.example.musicapp.ui.components.TooltipPositionProvider
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTooltipTest {

    @Test
    fun testTooltipAnchorSizeMatchesContentSizeAndDoesNotShiftSibling() {
        val scene = ImageComposeScene(width = 800, height = 600)
        var anchorBoxWidth = 0
        var anchorBoxHeight = 0
        var contentWidth = 0
        var siblingPositionX = 0

        try {
            scene.setContent {
                Row {
                    AppTooltip(
                        text = "This is an extremely long tooltip text that previously caused layout shifts and pushed sibling elements away by hundreds of pixels",
                        modifier = Modifier.onGloballyPositioned { coords ->
                            anchorBoxWidth = coords.size.width
                            anchorBoxHeight = coords.size.height
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 48.dp)
                                .onGloballyPositioned { coords ->
                                    contentWidth = coords.size.width
                                }
                        )
                    }

                    // Sibling element in the row
                    Box(
                        modifier = Modifier
                            .size(32.dp, 32.dp)
                            .onGloballyPositioned { coords ->
                                siblingPositionX = coords.positionInRoot().x.roundToInt()
                            }
                    )
                }
            }

            scene.render()

            // 1. AppTooltip anchor outer Box measured size must match exactly the inner button content size
            assertTrue(contentWidth > 0, "Content width should be measured")
            assertEquals(contentWidth, anchorBoxWidth, "Anchor Box width must exactly match content width")
            assertEquals(contentWidth, anchorBoxHeight, "Anchor Box height must exactly match content height")

            // 2. Sibling position must start immediately after the 48dp button (anchorBoxWidth), not shifted by tooltip width
            assertEquals(anchorBoxWidth, siblingPositionX, "Sibling X position must equal anchor width without tooltip expansion")
        } finally {
            scene.close()
        }
    }

    @Test
    fun testTooltipPositionProviderBelowAnchor() {
        val provider = TooltipPositionProvider(spacingPx = 6, marginPx = 8)
        val anchorBounds = IntRect(left = 100, top = 50, right = 150, bottom = 90) // 50x40 anchor
        val windowSize = IntSize(width = 800, height = 600)
        val popupContentSize = IntSize(width = 120, height = 30)

        val position = provider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )

        // Horizontally centered: 100 + (50 - 120)/2 = 100 - 35 = 65
        assertEquals(65, position.x)
        // Vertically below: anchorBounds.bottom (90) + spacingPx (6) = 96
        assertEquals(96, position.y)
    }

    @Test
    fun testTooltipPositionProviderFlipsAboveWhenNearBottom() {
        val provider = TooltipPositionProvider(spacingPx = 6, marginPx = 8)
        // Anchor near bottom: bottom is 580 in a 600px window
        val anchorBounds = IntRect(left = 100, top = 540, right = 150, bottom = 580)
        val windowSize = IntSize(width = 800, height = 600)
        val popupContentSize = IntSize(width = 100, height = 30)

        val position = provider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )

        // yBelow would be 580 + 6 = 586, + height (30) = 616 > 600 - 8 (592)
        // So it must flip above: anchorBounds.top (540) - popupContentSize.height (30) - spacingPx (6) = 504
        assertEquals(504, position.y)
        assertEquals(75, position.x) // 100 + (50 - 100)/2 = 75
    }

    @Test
    fun testTooltipPositionProviderClampsToWindowMargins() {
        val provider = TooltipPositionProvider(spacingPx = 6, marginPx = 8)
        // Anchor at far left: left = 0, width = 30
        val anchorLeft = IntRect(left = 0, top = 50, right = 30, bottom = 80)
        val windowSize = IntSize(width = 500, height = 500)
        val popupContentSize = IntSize(width = 100, height = 30)

        val posLeft = provider.calculatePosition(
            anchorBounds = anchorLeft,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )
        // x would be 0 + (30-100)/2 = -35 -> clamped to marginPx = 8
        assertEquals(8, posLeft.x)

        // Anchor at far right: right = 500, left = 470
        val anchorRight = IntRect(left = 470, top = 50, right = 500, bottom = 80)
        val posRight = provider.calculatePosition(
            anchorBounds = anchorRight,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )
        // maxX = 500 - 100 - 8 = 392
        // x would be 470 + (30-100)/2 = 435 -> clamped to 392
        assertEquals(392, posRight.x)
    }

    @Test
    fun testTooltipAnchorSizeWithTooltipParamAndBlankSupport() {
        val scene = ImageComposeScene(width = 800, height = 600)
        var anchor1Width = 0
        var anchor2Width = 0
        var content1Width = 0
        var content2Width = 0

        try {
            scene.setContent {
                Row {
                    // Test `tooltip = ...` backward compatibility parameter
                    AppTooltip(
                        tooltip = "Tooltip using tooltip parameter",
                        modifier = Modifier.onGloballyPositioned { coords ->
                            anchor1Width = coords.size.width
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp, 36.dp)
                                .onGloballyPositioned { coords ->
                                    content1Width = coords.size.width
                                }
                        )
                    }

                    // Test blank tooltip fallback
                    AppTooltip(
                        text = "",
                        tooltip = "",
                        modifier = Modifier.onGloballyPositioned { coords ->
                            anchor2Width = coords.size.width
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp, 28.dp)
                                .onGloballyPositioned { coords ->
                                    content2Width = coords.size.width
                                }
                        )
                    }
                }
            }

            scene.render()

            assertTrue(content1Width > 0)
            assertEquals(content1Width, anchor1Width, "Anchor with tooltip param must match content size exactly")
            assertTrue(content2Width > 0)
            assertEquals(content2Width, anchor2Width, "Blank tooltip anchor must match content size exactly")
        } finally {
            scene.close()
        }
    }
}
