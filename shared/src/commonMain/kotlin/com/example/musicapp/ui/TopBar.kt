package com.example.musicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.LocalSpacing

@Composable
internal fun AppTopBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUploadSong: () -> Unit,
    onUploadFolder: () -> Unit,
    onOpenSources: () -> Unit,
    isCompact: Boolean,
    isSyncing: Boolean = false,
    isOffline: Boolean = false,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isCompact) spacing.sm else spacing.lg,
                vertical = spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Navigation Back & Forward Buttons (Two distinct circular buttons separated by 8dp gap)
        if (!isCompact) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (canGoBack) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = canGoBack, onClick = onGoBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                        tint = if (canGoBack) Color.White else AppColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (canGoForward) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = canGoForward, onClick = onGoForward),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Вперед",
                        tint = if (canGoForward) Color.White else AppColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(spacing.md))
        }

        // Search Input Box
        Box(
            modifier = if (isCompact) {
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceElevated)
                    .padding(horizontal = spacing.sm)
            } else {
                Modifier
                    .width(360.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceElevated)
                    .padding(horizontal = spacing.sm)
            },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Поиск",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(spacing.xs))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            if (isCompact) "Поиск" else "Что хочешь послушать?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.TextMuted,
                                fontSize = 13.5.sp,
                            ),
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(AppColors.Accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Очистить поиск",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(spacing.md))

        // Right Action Buttons: Cloud Sources (if enabled), Import file, Import folder
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
                Surface(
                    onClick = onOpenSources,
                    shape = CircleShape,
                    color = AppColors.SurfaceElevated,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = 6.dp)
                    ) {
                        val (statusIcon, statusText, statusColor) = when {
                            isSyncing -> Triple(Icons.Rounded.Sync, "Синхронизация", Color(0xFFFFA000))
                            isOffline -> Triple(Icons.Rounded.CloudOff, "Офлайн", AppColors.TextMuted)
                            else -> Triple(Icons.Rounded.CloudDone, "Онлайн", AppColors.Accent)
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Источники: $statusText",
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                        if (!isCompact) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor
                            )
                        }
                    }
                }
                Spacer(Modifier.width(spacing.xs))
            }

            if (isCompact) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Добавить",
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(AppColors.SurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Добавить файлы", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuOpen = false
                                onUploadSong()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Импорт папки", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                menuOpen = false
                                onUploadFolder()
                            }
                        )
                    }
                }
            } else {
                Button(
                    onClick = onUploadSong,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text("Файл", color = AppColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.width(spacing.xs))

                Button(
                    onClick = onUploadFolder,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text("Папка", color = AppColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
