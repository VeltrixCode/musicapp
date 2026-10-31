package com.example.musicapp.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.musicapp.library.Library
import com.example.musicapp.platform.importLocalAudioFile
import com.example.musicapp.player.PlayerController
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.AppTheme
import com.example.musicapp.ui.MusicApp
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(width = 1150.dp, height = 750.dp)
    val library = remember { Library() }
    val player = remember { PlayerController() }

    Window(
        onCloseRequest = {
            player.release()
            exitApplication()
        },
        title = "MusicApp",
        undecorated = true,
        state = windowState,
    ) {
        // Drag-and-drop listener for audio files
        DisposableEffect(window) {
            val dropTarget = object : DropTarget() {
                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY)
                        val transferable = evt.transferable
                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            val droppedList = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                            val files = droppedList?.filterIsInstance<File>() ?: emptyList()
                            val audioExtensions = listOf("mp3", "wav", "m4a", "aac", "flac", "ogg")
                            for (f in files) {
                                if (f.isFile && f.extension.lowercase() in audioExtensions) {
                                    val track = importLocalAudioFile(f)
                                    library.addTrack(track)
                                }
                            }
                        }
                        evt.dropComplete(true)
                    } catch (e: Exception) {
                        evt.dropComplete(false)
                    }
                }
            }
            window.dropTarget = dropTarget
            onDispose { window.dropTarget = null }
        }

        AppTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Spacebar -> {
                                    player.toggle()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    player.seekTo((player.positionMs - 5000L).coerceAtLeast(0L))
                                    true
                                }
                                Key.DirectionRight -> {
                                    player.seekTo((player.positionMs + 5000L).coerceAtMost(player.durationMs))
                                    true
                                }
                                Key.DirectionUp -> {
                                    player.updateVolume(player.volume + 0.05f)
                                    true
                                }
                                Key.DirectionDown -> {
                                    player.updateVolume(player.volume - 0.05f)
                                    true
                                }
                                Key.N -> {
                                    player.next()
                                    true
                                }
                                Key.P -> {
                                    player.previous()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                // Custom Undecorated Title Bar (#121212)
                WindowDraggableArea(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(AppColors.WindowBar)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title & Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "MusicApp",
                                color = AppColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Window Control Buttons (Minimize, Maximize/Restore, Close)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WindowControlButton(
                                onClick = { windowState.isMinimized = true },
                                hoverColor = AppColors.WindowButtonHover
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Minimize,
                                    contentDescription = "Свернуть",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            WindowControlButton(
                                onClick = {
                                    windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                        WindowPlacement.Floating
                                    } else {
                                        WindowPlacement.Maximized
                                    }
                                },
                                hoverColor = AppColors.WindowButtonHover
                            ) {
                                Icon(
                                    imageVector = if (windowState.placement == WindowPlacement.Maximized) {
                                        Icons.Rounded.FilterNone
                                    } else {
                                        Icons.Rounded.CropSquare
                                    },
                                    contentDescription = "Развернуть",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            WindowControlButton(
                                onClick = {
                                    player.release()
                                    exitApplication()
                                },
                                hoverColor = AppColors.WindowCloseHover
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Закрыть",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Main App Content
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    MusicApp(player = player, library = library)
                }
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    hoverColor: Color,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 34.dp)
            .hoverable(interactionSource)
            .background(if (isHovered) hoverColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
