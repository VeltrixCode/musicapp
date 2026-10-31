package com.example.musicapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.musicapp.service.CoverArtService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.config.AppConfig
import com.example.musicapp.library.Library
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun CloudSourcesScreen(
    library: Library,
    player: AudioPlayer,
    onShowMessage: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val registry = player.providerRegistry

    val isGDriveSignedIn by registry.gdrive.isSignedIn.collectAsState()
    val isYandexSignedIn by registry.yandex.isSignedIn.collectAsState()
    val isSyncing by registry.isSyncing.collectAsState()

    var gdriveTokenInput by remember { mutableStateOf("") }
    var yandexTokenInput by remember { mutableStateOf("") }
    var showGDriveTokenDialog by remember { mutableStateOf(false) }
    var showYandexTokenDialog by remember { mutableStateOf(false) }

    // API Config State
    var googleClientIdInput by remember { mutableStateOf(AppConfig.googleClientId) }
    var googleClientSecretInput by remember { mutableStateOf(AppConfig.googleClientSecret) }
    var yandexClientIdInput by remember { mutableStateOf(AppConfig.yandexClientId) }
    var jamendoClientIdInput by remember { mutableStateOf(AppConfig.jamendoClientId) }
    var audiusAppNameInput by remember { mutableStateOf(AppConfig.audiusAppName) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testConnectionResult by remember { mutableStateOf<String?>(null) }

    val usedCacheMb = remember(AudioOfflineCache.getUsedSizeBytes()) {
        AudioOfflineCache.getUsedSizeBytes() / (1024L * 1024L)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .padding(bottom = 96.dp)
    ) {
        Text(
            text = "Облачные источники и Офлайн-кэш",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Подключите ваши облачные хранилища (Google Drive, Яндекс Диск) для стриминга и синхронизации музыки без скачивания.",
            style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextSecondary)
        )

        Spacer(Modifier.height(24.dp))

        // Provider Card 1: Google Drive
        CloudProviderItem(
            name = "Google Диск",
            description = "OAuth 2.0 PKCE • Чтение файлов audio/* • Потоковое воспроизведение с Range",
            isConnected = isGDriveSignedIn,
            onConnect = {
                showGDriveTokenDialog = true
            },
            onDisconnect = {
                scope.launch {
                    registry.gdrive.signOut()
                    onShowMessage("Google Диск отключен")
                }
            }
        )

        if (showGDriveTokenDialog) {
            TokenEntryBox(
                label = "OAuth Access Token (Google Drive)",
                value = gdriveTokenInput,
                onValueChange = { gdriveTokenInput = it },
                onSave = {
                    registry.gdrive.updateToken(gdriveTokenInput.trim())
                    showGDriveTokenDialog = false
                    onShowMessage("Google Диск успешно подключен!")
                },
                onCancel = { showGDriveTokenDialog = false }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Provider Card 2: Yandex Disk
        CloudProviderItem(
            name = "Яндекс Диск",
            description = "OAuth REST API • Потоковая передача аудио • Автоматическая синхронизация",
            isConnected = isYandexSignedIn,
            onConnect = {
                showYandexTokenDialog = true
            },
            onDisconnect = {
                scope.launch {
                    registry.yandex.signOut()
                    onShowMessage("Яндекс Диск отключен")
                }
            }
        )

        if (showYandexTokenDialog) {
            TokenEntryBox(
                label = "OAuth Токен (Яндекс Диск)",
                value = yandexTokenInput,
                onValueChange = { yandexTokenInput = it },
                onSave = {
                    registry.yandex.updateToken(yandexTokenInput.trim())
                    showYandexTokenDialog = false
                    onShowMessage("Яндекс Диск успешно подключен!")
                },
                onCancel = { showYandexTokenDialog = false }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Public Catalogs Status
        CloudProviderItem(
            name = "Jamendo & Audius",
            description = "Открытые каталоги музыки с лицензиями Creative Commons • Всегда активны",
            isConnected = true,
            isAlwaysActive = true,
            onConnect = {},
            onDisconnect = {}
        )

        Spacer(Modifier.height(28.dp))

        // SECTION: API Credentials & Keys (SecureStorage)
        Text(
            text = "Настройки API и Ключи сервисов",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Ключи сохраняются в системное зашифрованное хранилище SecureStorage (Windows DPAPI / Android Keystore).",
            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary)
        )
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Surface)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = googleClientIdInput,
                    onValueChange = { googleClientIdInput = it },
                    label = { Text("Google Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = googleClientSecretInput,
                    onValueChange = { googleClientSecretInput = it },
                    label = { Text("Google Client Secret") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = yandexClientIdInput,
                    onValueChange = { yandexClientIdInput = it },
                    label = { Text("Яндекс Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jamendoClientIdInput,
                    onValueChange = { jamendoClientIdInput = it },
                    label = { Text("Jamendo Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = audiusAppNameInput,
                    onValueChange = { audiusAppNameInput = it },
                    label = { Text("Audius App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            AppConfig.googleClientId = googleClientIdInput.trim()
                            AppConfig.googleClientSecret = googleClientSecretInput.trim()
                            AppConfig.yandexClientId = yandexClientIdInput.trim()
                            AppConfig.jamendoClientId = jamendoClientIdInput.trim()
                            AppConfig.audiusAppName = audiusAppNameInput.trim()
                            onShowMessage("Ключи успешно сохранены в SecureStorage!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Сохранить в SecureStorage", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            testConnectionResult = null
                            scope.launch {
                                try {
                                    val audiusOk = registry.audius.listTracks().items.isNotEmpty()
                                    val jamendoOk = if (AppConfig.jamendoClientId.isNotBlank()) {
                                        registry.jamendo.listTracks().items.isNotEmpty()
                                    } else false

                                    val parts = mutableListOf<String>()
                                    parts.add("Audius: ${if (audiusOk) "OK" else "Нет ответа"}")
                                    if (AppConfig.jamendoClientId.isNotBlank()) {
                                        parts.add("Jamendo: ${if (jamendoOk) "OK" else "Ошибка/Не авторизован"}")
                                    }
                                    if (isGDriveSignedIn) parts.add("Google Drive: Подключен")
                                    if (isYandexSignedIn) parts.add("Яндекс Диск: Подключен")

                                    testConnectionResult = parts.joinToString(" • ")
                                } catch (e: Exception) {
                                    testConnectionResult = "Сбой проверки: ${e.message}"
                                } finally {
                                    isTestingConnection = false
                                }
                            }
                        },
                        enabled = !isTestingConnection
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Rounded.NetworkCheck, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Проверить подключение", color = Color.White)
                    }
                }

                testConnectionResult?.let { result ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.SurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(result, color = AppColors.Accent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Sync Now Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Синхронизация медиатеки",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    )
                    Text(
                        "Ищет новые аудиофайлы во всех подключенных облаках и добавляет в медиатеку с дедупликацией.",
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        registry.syncAll(library.tracks.toList()) { newTracks ->
                            library.addTracks(newTracks)
                            onShowMessage("Синхронизировано новых треков: ${newTracks.size}")
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Синхронизировать сейчас", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Section: Offline Cache & Media Settings
        Text(
            text = "Управление офлайн-кэшем и медиа",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        )
        Spacer(Modifier.height(12.dp))

        var showClearCacheConfirm by remember { mutableStateOf(false) }
        var cacheLimitMb by remember {
            mutableStateOf((AudioOfflineCache.maxSizeBytes / (1024L * 1024L)).toFloat())
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Surface)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Использовано: $usedCacheMb МБ из ${cacheLimitMb.toInt()} МБ",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            "Автоматическое LRU-вытеснение защищает играющие и добавленные в очередь треки",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Button(
                        onClick = { showClearCacheConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Очистить кэш", color = Color.White, maxLines = 1, softWrap = false)
                    }
                }

                // Cache usage progress bar
                val progressFrac = (usedCacheMb.toFloat() / cacheLimitMb.coerceAtLeast(1f)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFrac },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (progressFrac > 0.85f) Color(0xFFFFA000) else AppColors.Accent,
                    trackColor = AppColors.SurfaceElevated
                )

                // Cache limit slider (512 MB to 20 GB)
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Лимит размера кэша", fontSize = 12.sp, color = AppColors.TextSecondary)
                        Text("${cacheLimitMb.toInt()} МБ (${(cacheLimitMb / 1024f).let { if (it >= 1f) "${(it * 10).toInt() / 10f} ГБ" else "${it.toInt()} МБ" }})", fontSize = 12.sp, color = AppColors.Accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = cacheLimitMb,
                        onValueChange = {
                            cacheLimitMb = it
                            AudioOfflineCache.maxSizeBytes = it.toLong() * 1024L * 1024L
                        },
                        valueRange = 512f..20480f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = AppColors.Accent,
                            activeTrackColor = AppColors.Accent,
                            inactiveTrackColor = AppColors.SurfaceElevated
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Setting: Load online cover art (iTunes Search API)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Загружать обложки из интернета", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("Поиск высокого разрешения через iTunes Search API при отсутствии локальных тегов", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                    Switch(
                        checked = CoverArtService.loadOnlineCovers,
                        onCheckedChange = { CoverArtService.loadOnlineCovers = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppColors.Accent,
                            uncheckedThumbColor = AppColors.TextMuted,
                            uncheckedTrackColor = AppColors.SurfaceElevated
                        )
                    )
                }
            }
        }

        if (showClearCacheConfirm) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirm = false },
                title = { Text("Очистить офлайн-кэш?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Все временно загруженные треки ($usedCacheMb МБ) будут удалены из локального хранилища. Локальные файлы и плейлисты не будут затронуты.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            AudioOfflineCache.clearCache()
                            showClearCacheConfirm = false
                            onShowMessage("Офлайн-кэш успешно очищен")
                        }
                    ) {
                        Text("Очистить", color = AppColors.Error, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirm = false }) {
                        Text("Отмена", color = AppColors.TextSecondary, maxLines = 1, softWrap = false)
                    }
                },
                containerColor = AppColors.SurfaceElevated
            )
        }
    }
}

@Composable
private fun CloudProviderItem(
    name: String,
    description: String,
    isConnected: Boolean,
    isAlwaysActive: Boolean = false,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isConnected) AppColors.Accent.copy(alpha = 0.2f) else AppColors.SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (isConnected) AppColors.Accent else AppColors.TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }

            if (!isAlwaysActive) {
                Spacer(Modifier.width(16.dp))
                if (isConnected) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text("Отключить", color = AppColors.Error)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) {
                        Text("Подключить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.Accent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Активен", color = AppColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TokenEntryBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Вставьте токен...", color = AppColors.TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = value.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                ) {
                    Text("Сохранить и подключить", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onCancel) {
                    Text("Отмена", color = Color.White)
                }
            }
        }
    }
}
