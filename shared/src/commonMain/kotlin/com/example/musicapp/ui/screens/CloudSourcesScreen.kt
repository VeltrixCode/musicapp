package com.example.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.config.AppConfig
import com.example.musicapp.library.Library
import com.example.musicapp.platform.currentTimeMillis
import com.example.musicapp.player.AudioPlayer
import com.example.musicapp.service.CoverArtService
import com.example.musicapp.service.LyricsService
import com.example.musicapp.settings.SettingsRepository
import com.example.musicapp.theme.AppColors
import kotlinx.coroutines.launch

private fun formatSyncTime(timeMs: Long?): String {
    if (timeMs == null || timeMs <= 0L) return "Ещё не проводилась"
    val diffSec = (currentTimeMillis() - timeMs) / 1000L
    return when {
        diffSec < 60 -> "Только что"
        diffSec < 3600 -> "${diffSec / 60} мин. назад"
        diffSec < 86400 -> "${diffSec / 3600} ч. назад"
        else -> "${diffSec / 86400} дн. назад"
    }
}

@Composable
fun CloudSourcesScreen(
    library: Library,
    player: AudioPlayer,
    onShowMessage: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val registry = player.providerRegistry

    val settingsRepo = remember { SettingsRepository() }
    var appSettings by remember { mutableStateOf(settingsRepo.load()) }

    // Synchronize settings with singleton services
    remember(appSettings.fetchOnlineLyrics) { LyricsService.fetchOnlineLyrics = appSettings.fetchOnlineLyrics }
    remember(appSettings.offlineMode) {
        LyricsService.offlineMode = appSettings.offlineMode
    }

    val isGDriveSignedIn by registry.gdrive.isSignedIn.collectAsState()
    val isYandexSignedIn by registry.yandex.isSignedIn.collectAsState()
    val isSyncing by registry.isSyncing.collectAsState()

    // Auto-fetch real user account emails from APIs
    LaunchedEffect(isGDriveSignedIn) {
        if (isGDriveSignedIn && (appSettings.gdriveAccountEmail == null || appSettings.gdriveAccountEmail == "Google OAuth User" || appSettings.gdriveAccountEmail == "Google Token User")) {
            val email = registry.gdrive.fetchAccountEmail()
            if (!email.isNullOrBlank()) {
                appSettings = appSettings.copy(gdriveAccountEmail = email)
                settingsRepo.save(appSettings)
            }
        }
    }
    LaunchedEffect(isYandexSignedIn) {
        if (isYandexSignedIn && (appSettings.yandexAccountEmail == null || appSettings.yandexAccountEmail == "Yandex OAuth User")) {
            val email = registry.yandex.fetchAccountEmail()
            if (!email.isNullOrBlank()) {
                appSettings = appSettings.copy(yandexAccountEmail = email)
                settingsRepo.save(appSettings)
            }
        }
    }

    var gdriveTokenInput by remember { mutableStateOf("") }
    var yandexTokenInput by remember { mutableStateOf("") }
    var showGDriveTokenDialog by remember { mutableStateOf(false) }
    var showYandexTokenDialog by remember { mutableStateOf(false) }

    // Folder sync input dialogs
    var showGDriveFolderDialog by remember { mutableStateOf(false) }
    var showYandexFolderDialog by remember { mutableStateOf(false) }
    var folderInput by remember { mutableStateOf("") }
    var gdriveFolders by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var yandexFolders by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoadingFolders by remember { mutableStateOf(false) }

    // API Config State
    var googleClientIdInput by remember { mutableStateOf(AppConfig.googleClientId) }
    var googleClientSecretInput by remember { mutableStateOf(AppConfig.googleClientSecret) }
    var showGoogleSecret by remember { mutableStateOf(false) }
    var yandexClientIdInput by remember { mutableStateOf(AppConfig.yandexClientId) }
    var jamendoClientIdInput by remember { mutableStateOf(AppConfig.jamendoClientId) }
    var audiusAppNameInput by remember { mutableStateOf(AppConfig.audiusAppName) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testConnectionResult by remember { mutableStateOf<String?>(null) }

    val usedCacheMb = remember(AudioOfflineCache.getUsedSizeBytes()) {
        AudioOfflineCache.getUsedSizeBytes() / (1024L * 1024L)
    }

    val appSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.Black,
        checkedTrackColor = AppColors.Accent,
        checkedBorderColor = AppColors.Accent,
        uncheckedThumbColor = Color(0xFFC0C0C0),
        uncheckedTrackColor = Color(0xFF282828),
        uncheckedBorderColor = Color(0xFF757575),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 16.dp, horizontal = 0.dp)
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
            accountEmail = appSettings.gdriveAccountEmail ?: if (isGDriveSignedIn) "Google Drive Account" else null,
            folderId = appSettings.gdriveSyncFolderId,
            lastSyncTime = formatSyncTime(appSettings.lastSyncTimeMs),
            onSelectFolder = {
                folderInput = appSettings.gdriveSyncFolderId ?: ""
                showGDriveFolderDialog = true
                isLoadingFolders = true
                scope.launch {
                    gdriveFolders = registry.gdrive.listFolders()
                    isLoadingFolders = false
                }
            },
            onConnect = {
                if (googleClientIdInput.isBlank()) {
                    onShowMessage("Сначала укажите Google Client ID в секции «Конфигурация ключей API» ниже.")
                } else {
                    showGDriveTokenDialog = true
                }
            },
            onDisconnect = {
                scope.launch {
                    registry.gdrive.signOut()
                    appSettings = appSettings.copy(gdriveAccountEmail = null, gdriveSyncFolderId = null)
                    settingsRepo.save(appSettings)
                    onShowMessage("Google Диск отключен, токен удален из SecureStorage")
                }
            }
        )

        if (showGDriveTokenDialog) {
            TokenEntryBox(
                label = "OAuth Access Token (Google Drive)",
                value = gdriveTokenInput,
                onValueChange = { gdriveTokenInput = it },
                onOAuthClick = {
                    scope.launch {
                        onShowMessage("Запуск авторизации Google (OAuth 2.0 PKCE)...")
                        val res = com.example.musicapp.oauth.GoogleOAuth.performSignIn()
                        if (res.isSuccess) {
                            val token = res.getOrNull()
                            if (!token.isNullOrBlank()) {
                                registry.gdrive.updateToken(token)
                                appSettings = appSettings.copy(gdriveAccountEmail = "Google OAuth User")
                                settingsRepo.save(appSettings)
                                gdriveTokenInput = ""
                                showGDriveTokenDialog = false
                                onShowMessage("Google Диск успешно подключен!")
                            }
                        } else {
                            onShowMessage("Ошибка входа Google: ${res.exceptionOrNull()?.message}")
                        }
                    }
                },
                onSave = {
                    registry.gdrive.updateToken(gdriveTokenInput.trim())
                    appSettings = appSettings.copy(gdriveAccountEmail = "Google Token User")
                    settingsRepo.save(appSettings)
                    gdriveTokenInput = ""
                    showGDriveTokenDialog = false
                    onShowMessage("Google Диск успешно подключен!")
                },
                onCancel = {
                    gdriveTokenInput = ""
                    showGDriveTokenDialog = false
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Provider Card 2: Yandex Disk
        CloudProviderItem(
            name = "Яндекс Диск",
            description = "OAuth REST API • Потоковая передача аудио • Автоматическая синхронизация",
            isConnected = isYandexSignedIn,
            accountEmail = appSettings.yandexAccountEmail ?: if (isYandexSignedIn) "Yandex Disk Account" else null,
            folderId = appSettings.yandexSyncFolderId,
            lastSyncTime = formatSyncTime(appSettings.lastSyncTimeMs),
            onSelectFolder = {
                folderInput = appSettings.yandexSyncFolderId ?: ""
                showYandexFolderDialog = true
                isLoadingFolders = true
                scope.launch {
                    yandexFolders = registry.yandex.listFolders()
                    isLoadingFolders = false
                }
            },
            onConnect = {
                if (yandexClientIdInput.isBlank()) {
                    onShowMessage("Сначала укажите Яндекс Client ID в секции «Конфигурация ключей API» ниже.")
                } else {
                    showYandexTokenDialog = true
                }
            },
            onDisconnect = {
                scope.launch {
                    registry.yandex.signOut()
                    appSettings = appSettings.copy(yandexAccountEmail = null, yandexSyncFolderId = null)
                    settingsRepo.save(appSettings)
                    onShowMessage("Яндекс Диск отключен, токен удален из SecureStorage")
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
                    appSettings = appSettings.copy(yandexAccountEmail = "Yandex OAuth User")
                    settingsRepo.save(appSettings)
                    yandexTokenInput = ""
                    showYandexTokenDialog = false
                    onShowMessage("Яндекс Диск успешно подключен!")
                },
                onCancel = {
                    yandexTokenInput = ""
                    showYandexTokenDialog = false
                }
            )
        }

        // Folder selection dialogs
        if (showGDriveFolderDialog) {
            FolderSelectionDialog(
                title = "Папка синхронизации Google Диска",
                currentFolder = folderInput,
                folders = gdriveFolders,
                isLoading = isLoadingFolders,
                onDismiss = { showGDriveFolderDialog = false },
                onSave = { selected ->
                    appSettings = appSettings.copy(gdriveSyncFolderId = selected.takeIf { it.isNotBlank() })
                    settingsRepo.save(appSettings)
                    showGDriveFolderDialog = false
                    onShowMessage("Папка Google Диска сохранена")
                }
            )
        }

        if (showYandexFolderDialog) {
            FolderSelectionDialog(
                title = "Папка синхронизации Яндекс Диска",
                currentFolder = folderInput,
                folders = yandexFolders,
                isLoading = isLoadingFolders,
                onDismiss = { showYandexFolderDialog = false },
                onSave = { selected ->
                    appSettings = appSettings.copy(yandexSyncFolderId = selected.takeIf { it.isNotBlank() })
                    settingsRepo.save(appSettings)
                    showYandexFolderDialog = false
                    onShowMessage("Папка Яндекс Диска сохранена")
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Catalog 1: Audius
        CloudProviderItem(
            name = "Audius",
            description = "Децентрализованный открытый каталог музыки • Всегда активен",
            isConnected = true,
            isAlwaysActive = true,
            onConnect = {},
            onDisconnect = {}
        )

        Spacer(Modifier.height(16.dp))

        // Catalog 2: Jamendo
        val hasJamendoKey = jamendoClientIdInput.isNotBlank()
        CloudProviderItem(
            name = "Jamendo Music",
            description = if (hasJamendoKey) "Каталог независимой музыки (Creative Commons) • Подключен"
                          else "Нужен ключ API (укажите в блоке «Конфигурация ключей API» ниже)",
            isConnected = hasJamendoKey,
            isAlwaysActive = false,
            connectButtonText = if (hasJamendoKey) "Подключен" else "Нужен ключ",
            onConnect = {
                onShowMessage("Укажите Jamendo Client ID в блоке «Конфигурация ключей API» ниже и сохраните его.")
            },
            onDisconnect = {
                jamendoClientIdInput = ""
                AppConfig.jamendoClientId = ""
                onShowMessage("Jamendo Client ID удален")
            }
        )

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
                            val now = currentTimeMillis()
                            appSettings = appSettings.copy(lastSyncTimeMs = now)
                            settingsRepo.save(appSettings)
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

        // SECTION: Privacy & Network (Point 6)
        Text(
            text = "Приватность и Сеть",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "User-Agent приложения: ${LyricsService.USER_AGENT}",
            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted)
        )
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Surface)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Privacy toggle 1: Online lyrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Загружать тексты песен из интернета",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "По умолчанию выключено. При включении название и исполнитель трека отправляются на открытый сервис lrclib.net для поиска синхронизированных караоке-текстов.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = appSettings.fetchOnlineLyrics,
                        onCheckedChange = { enabled ->
                            appSettings = appSettings.copy(fetchOnlineLyrics = enabled)
                            LyricsService.fetchOnlineLyrics = enabled
                            settingsRepo.save(appSettings)
                        },
                        colors = appSwitchColors
                    )
                }

                // Privacy toggle 2: Online covers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Загружать обложки альбомов из интернета",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "По умолчанию выключено. При включении название исполнителя и альбома/трека отправляются в открытые сервисы iTunes и MusicBrainz / Cover Art Archive для поиска обложки. Локальные пути к файлам и имена файлов никогда не передаются.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = appSettings.fetchOnlineCovers,
                        onCheckedChange = { enabled ->
                            appSettings = appSettings.copy(fetchOnlineCovers = enabled)
                            settingsRepo.save(appSettings)
                        },
                        colors = appSwitchColors
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))

                // Privacy toggle 3: Offline mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Офлайн-режим",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Блокирует все внешние сетевые подключения приложения (к облачным дискам, каталогам, поиску текстов и обложек). Воспроизводятся только локальные файлы и сохранённый офлайн-кэш.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = appSettings.offlineMode,
                        onCheckedChange = { enabled ->
                            appSettings = appSettings.copy(offlineMode = enabled)
                            LyricsService.offlineMode = enabled
                            settingsRepo.save(appSettings)
                        },
                        colors = appSwitchColors
                    )
                }
            }
        }

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
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    visualTransformation = if (showGoogleSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGoogleSecret = !showGoogleSecret }) {
                            Icon(
                                imageVector = if (showGoogleSecret) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showGoogleSecret) "Скрыть" else "Показать",
                                tint = AppColors.TextSecondary
                            )
                        }
                    },
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Использовано диска для кэша", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "$usedCacheMb МБ из ${cacheLimitMb.toInt()} МБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Button(
                        onClick = { showClearCacheConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceElevated)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.Error)
                        Spacer(Modifier.width(6.dp))
                        Text("Очистить кэш", color = AppColors.Error)
                    }
                }

                // Progress Bar of Cache
                val progress = (usedCacheMb.toFloat() / cacheLimitMb.coerceAtLeast(1f)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AppColors.Accent,
                    trackColor = AppColors.SurfaceElevated
                )

                // Slider to set Cache Limit
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Максимальный размер кэша", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                        Text("${cacheLimitMb.toInt()} МБ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppColors.Accent)
                    }
                    Slider(
                        value = cacheLimitMb,
                        onValueChange = { cacheLimitMb = it },
                        onValueChangeFinished = {
                            AudioOfflineCache.maxSizeBytes = cacheLimitMb.toLong() * 1024L * 1024L
                        },
                        valueRange = 100f..5000f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = AppColors.Accent,
                            activeTrackColor = AppColors.Accent,
                            inactiveTrackColor = AppColors.SurfaceElevated
                        )
                    )
                }
            }
        }

        if (showClearCacheConfirm) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirm = false },
                title = { Text("Очистить офлайн-кэш музыки?", color = AppColors.TextPrimary) },
                text = {
                    Text(
                        "Все временно закэшированные потоковые аудиофайлы будут удалены с диска. Локальная музыка и плейлисты не пострадают.",
                        color = AppColors.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            AudioOfflineCache.clearCache()
                            showClearCacheConfirm = false
                            onShowMessage("Офлайн-кэш успешно очищен!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error)
                    ) {
                        Text("Удалить", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirm = false }) {
                        Text("Отмена", color = AppColors.TextSecondary)
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
    accountEmail: String? = null,
    folderId: String? = null,
    lastSyncTime: String? = null,
    onSelectFolder: (() -> Unit)? = null,
    isAlwaysActive: Boolean = false,
    connectButtonText: String = "Подключить",
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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            Text(connectButtonText, color = Color.Black, fontWeight = FontWeight.Bold)
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

            // Extended connected account info: account, folder, last sync time (Point 5)
            if (isConnected && !isAlwaysActive) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(16.dp))
                        Text(
                            text = accountEmail ?: "Аккаунт подключен",
                            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary, fontWeight = FontWeight.Medium)
                        )
                    }

                    if (lastSyncTime != null) {
                        Text(
                            text = "Синхронизация: $lastSyncTime",
                            style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted, fontSize = 11.sp)
                        )
                    }
                }

                if (onSelectFolder != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Папка: ${folderId ?: "Вся музыка (корень)"}",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary)
                            )
                        }
                        TextButton(onClick = onSelectFolder) {
                            Text("Изменить папку", fontSize = 12.sp, color = AppColors.Accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSelectionDialog(
    title: String,
    currentFolder: String,
    folders: List<Pair<String, String>> = emptyList(),
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentFolder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                Text(
                    "Выберите папку из списка или укажите вручную:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Загрузка папок из хранилища...", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    }
                } else if (folders.isNotEmpty()) {
                    Text("Найденные папки в облаке:", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.Background)
                            .padding(4.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (text.isBlank()) AppColors.SurfaceElevated else Color.Transparent)
                                    .clickable { text = "" }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Корень хранилища (вся музыка)", style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary))
                            }
                        }
                        items(folders) { pair ->
                            val fId = pair.first
                            val fName = pair.second
                            val isSel = text == fId || text == fName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) AppColors.SurfaceElevated else Color.Transparent)
                                    .clickable { text = fId }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = if (isSel) AppColors.Accent else AppColors.TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(fName, style = MaterialTheme.typography.bodySmall.copy(color = if (isSel) AppColors.Accent else AppColors.TextPrimary), maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("ID или имя папки") },
                    placeholder = { Text("Оставьте пустым для всей музыки", color = AppColors.TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
            ) {
                Text("Выбрать", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.SurfaceElevated
    )
}

@Composable
private fun TokenEntryBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onOAuthClick: (() -> Unit)? = null,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            if (onOAuthClick != null) {
                OutlinedButton(
                    onClick = onOAuthClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Accent)
                ) {
                    Text("Войти через браузер (OAuth 2.0 PKCE)", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("или введите токен вручную:", style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextMuted))
                Spacer(Modifier.height(4.dp))
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Вставьте токен...", color = AppColors.TextMuted) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
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
