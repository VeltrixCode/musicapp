package com.example.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.musicapp.theme.AppColors
import kotlinx.coroutines.delay

class TooltipPositionProvider(
    val spacingPx: Int = 8,
    val marginPx: Int = 8
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Horizontally center relative to anchor
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(0)
        val minX = marginPx.coerceAtMost(maxX)
        x = x.coerceIn(minX, maxX)

        // Try placing below anchor first; if it overflows window bottom, place above
        val yBelow = anchorBounds.bottom + spacingPx
        val yAbove = anchorBounds.top - popupContentSize.height - spacingPx

        val y = if (yBelow + popupContentSize.height <= windowSize.height - marginPx) {
            yBelow
        } else if (yAbove >= marginPx) {
            yAbove
        } else {
            val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(0)
            val minY = marginPx.coerceAtMost(maxY)
            yBelow.coerceIn(minY, maxY)
        }

        return IntOffset(x, y)
    }
}

@Composable
fun AppTooltip(
    text: String = "",
    tooltip: String = text,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val displayTooltip = if (text.isNotBlank()) text else tooltip
    if (displayTooltip.isBlank()) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isVisible by remember { mutableStateOf(false) }
    var isPressedDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            isPressedDismissed = false
            delay(500)
            if (!isPressedDismissed) {
                isVisible = true
            }
        } else {
            isVisible = false
            isPressedDismissed = false
        }
    }

    val density = LocalDensity.current
    val positionProvider = remember(density) {
        TooltipPositionProvider(
            spacingPx = with(density) { 6.dp.roundToPx() },
            marginPx = with(density) { 8.dp.roundToPx() }
        )
    }

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            isVisible = false
                            isPressedDismissed = true
                        }
                    }
                }
            }
    ) {
        content()

        if (isVisible) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.SurfaceElevated)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayTooltip,
                        color = AppColors.TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
