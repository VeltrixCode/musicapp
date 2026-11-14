package com.example.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.musicapp.theme.AppColors

@Composable
fun AppTooltip(
    text: String = "",
    tooltip: String = text,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val displayTooltip = if (text.isNotBlank()) text else tooltip
    var isHovered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Enter) {
                        isHovered = true
                    } else if (event.type == PointerEventType.Exit) {
                        isHovered = false
                    }
                }
            }
        }
    ) {
        content()

        if (isHovered && displayTooltip.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, 36) }
                    .zIndex(100f)
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
