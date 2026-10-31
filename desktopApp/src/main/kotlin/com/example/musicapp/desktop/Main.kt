package com.example.musicapp.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
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
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File

fun main() = application {
    var isMiniPlayer by remember { mutableStateOf(false) }

    val windowState = rememberWindowState(
        size = if (isMiniPlayer) DpSize(380.dp, 140.dp) else DpSize(1150.dp, 750.dp)
    )

    val library = remember { Library() }
    val player = remember { PlayerController() }
    var isDraggingFilesOver by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = {
            com.example.musicapp.proxy.LocalStreamingProxy.stop()
            player.release()
            exitApplication()
        },
        title = "JVXMusic",
        icon = painterResource("icon.png"),
        undecorated = true,
        alwaysOnTop = isMiniPlayer,
        state = windowState,
    ) {
        // Drag-and-drop listener for audio files and folders
        DisposableEffect(window) {
            val dropTargetListener = object : DropTargetListener {
                override fun dragEnter(dtde: DropTargetDragEvent) {
                    isDraggingFilesOver = true
                }
                override fun dragOver(dtde: DropTargetDragEvent) {
                    isDraggingFilesOver = true
                }
                override fun dropActionChanged(dtde: DropTargetDragEvent) {}
                override fun dragExit(dte: DropTargetEvent) {
                    isDraggingFilesOver = false
                }
                override fun drop(evt: DropTargetDropEvent) {
                    isDraggingFilesOver = false
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY)
                        val transferable = evt.transferable
                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            val droppedList = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                            val files = droppedList?.filterIsInstance<File>() ?: emptyList()
                            val audioExtensions = listOf("mp3", "wav", "m4a", "aac", "flac", "ogg")
                            val newTracks = mutableListOf<com.example.musicapp.model.Track>()

                            for (item in files) {
                                if (item.isDirectory) {
                                    item.walkTopDown().filter { it.isFile && it.extension.lowercase() in audioExtensions }.forEach { f ->
                                        newTracks.add(importLocalAudioFile(f))
                                    }
                                } else if (item.isFile && item.extension.lowercase() in audioExtensions) {
                                    newTracks.add(importLocalAudioFile(item))
                                }
                            }
                            if (newTracks.isNotEmpty()) {
                                library.addTracks(newTracks)
                            }
                        }
                        evt.dropComplete(true)
                    } catch (e: Exception) {
                        evt.dropComplete(false)
                    }
                }
            }
            val dropTarget = DropTarget(window, dropTargetListener)
            window.dropTarget = dropTarget
            onDispose { window.dropTarget = null }
        }

        AppTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when {
                                event.key == Key.Spacebar -> {
                                    player.toggle()
                                    true
                                }
                                event.key == Key.DirectionLeft -> {
                                    player.seekTo((player.positionMs - 5000L).coerceAtLeast(0L))
                                    true
                                }
                                event.key == Key.DirectionRight -> {
                                    player.seekTo((player.positionMs + 5000L).coerceAtMost(player.durationMs))
                                    true
                                }
                                event.key == Key.DirectionUp -> {
                                    player.updateVolume(player.volume + 0.05f)
                                    true
                                }
                                event.key == Key.DirectionDown -> {
                                    player.updateVolume(player.volume - 0.05f)
                                    true
                                }
                                event.key == Key.N -> {
                                    player.next()
                                    true
                                }
                                event.key == Key.P -> {
                                    player.previous()
                                    true
                                }
                                event.key == Key.M -> {
                                    player.toggleMute()
                                    true
                                }
                                event.key == Key.L -> {
                                    player.current?.let { library.toggleFavorite(it) }
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                if (isMiniPlayer) {
                    // Mini Player UI
                    MiniPlayerView(
                        player = player,
                        onExpand = {
                            isMiniPlayer = false
                            windowState.size = DpSize(1150.dp, 750.dp)
                        },
                        onClose = {
                            player.release()
                            exitApplication()
                        }
                    )
                } else {
                    // Full MusicApp UI
                    Column(Modifier.fillMaxSize()) {
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
                                        "JVXMusic",
                                        color = AppColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Window Control Buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Mini-player toggle
                                    WindowControlButton(
                                        onClick = {
                                            isMiniPlayer = true
                                            windowState.size = DpSize(380.dp, 140.dp)
                                        },
                                        hoverColor = AppColors.WindowButtonHover
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PictureInPicture,
                                            contentDescription = "Мини-плеер",
                                            tint = AppColors.TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Minimize
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

                                    // Maximize/Restore
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

                                    // Close
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

                        // App Content
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            MusicApp(player = player, library = library)
                        }
                    }
                }

                // Drag and Drop Overlay
                AnimatedVisibility(
                    visible = isDraggingFilesOver,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(2.dp, AppColors.Accent, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.FileUpload,
                                contentDescription = null,
                                tint = AppColors.Accent,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Перетащите аудиофайлы или папки сюда",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Поддерживаются MP3, WAV, FLAC, M4A, AAC, OGG",
                                color = AppColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.ui.window.WindowScope.MiniPlayerView(
    player: PlayerController,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    val track = player.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border)
    ) {
        // Drag header
        WindowDraggableArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(AppColors.WindowBar)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("JVXMusic Mini", color = AppColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row {
                    WindowControlButton(onClick = onExpand, hoverColor = AppColors.WindowButtonHover) {
                        Icon(imageVector = Icons.Rounded.PictureInPicture, contentDescription = "Развернуть", tint = AppColors.TextSecondary, modifier = Modifier.size(13.dp))
                    }
                    WindowControlButton(onClick = onClose, hoverColor = AppColors.WindowCloseHover) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Закрыть", tint = AppColors.TextSecondary, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // Mini player content
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track?.title ?: "Ничего не играет", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(track?.artist ?: "", color = AppColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.previous() }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White).clickable { player.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { player.next() }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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
            .size(width = 38.dp, height = 30.dp)
            .hoverable(interactionSource)
            .background(if (isHovered) hoverColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
