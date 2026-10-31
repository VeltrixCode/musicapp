package com.example.musicapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.theme.AppColors

@Composable
fun TrackActionMenu(
    track: Track,
    expanded: Boolean,
    playlists: List<Playlist>,
    player: AudioPlayer,
    onDismiss: () -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
    onRename: (Track) -> Unit,
    onDelete: (Track) -> Unit,
    onShowInfo: (Track) -> Unit,
) {
    val isCached = AudioOfflineCache.isCached(track.id)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Воспроизвести следующим") },
            leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null) },
            onClick = {
                player.playNext(track)
                onDismiss()
            }
        )

        DropdownMenuItem(
            text = { Text("Добавить в очередь") },
            leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null) },
            onClick = {
                player.addToQueue(track)
                onDismiss()
            }
        )

        // Offline Download / Delete
        if (track.sourceProvider != TrackSource.LOCAL) {
            if (isCached) {
                DropdownMenuItem(
                    text = { Text("Удалить из офлайн-кэша") },
                    leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = AppColors.Error) },
                    onClick = {
                        player.deleteOffline(track.id)
                        onDismiss()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Скачать для офлайн") },
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null, tint = AppColors.Accent) },
                    onClick = {
                        player.downloadForOffline(track)
                        onDismiss()
                    }
                )
            }
        }

        // Add to Playlist items
        playlists.forEach { p ->
            DropdownMenuItem(
                text = { Text("В «${p.name}»") },
                leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                onClick = {
                    onAddToPlaylist(p.id, track)
                    onDismiss()
                }
            )
        }

        DropdownMenuItem(
            text = { Text("Информация об источнике") },
            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            onClick = {
                onShowInfo(track)
                onDismiss()
            }
        )

        if (track.sourceProvider == TrackSource.LOCAL) {
            DropdownMenuItem(
                text = { Text("Редактировать теги") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    onRename(track)
                    onDismiss()
                }
            )
        }

        DropdownMenuItem(
            text = { Text("Удалить из медиатеки", color = AppColors.Error) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.Error) },
            onClick = {
                onDelete(track)
                onDismiss()
            }
        )
    }
}
