package com.example.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.settings.SettingsRepository
import com.example.musicapp.theme.AccentOption
import com.example.musicapp.theme.AppColors
import com.example.musicapp.theme.DefaultAccentOptions
import com.example.musicapp.theme.LocalSpacing
import com.example.musicapp.theme.ThemePreset
import com.example.musicapp.theme.ThemePresets
import com.example.musicapp.theme.parseHexColor
import com.example.musicapp.theme.toHexRgb

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationDialog(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val currentSettings = remember { settingsRepository.load() }

    var selectedThemeId by remember { mutableStateOf(currentSettings.themePresetId) }
    var selectedAccentHex by remember { mutableStateOf(currentSettings.customAccentHex) }
    var selectedPlayerViewMode by remember { mutableStateOf(currentSettings.playerViewMode) }
    var customHexInput by remember { mutableStateOf(selectedAccentHex ?: "") }
    var hexError by remember { mutableStateOf(false) }

    fun commitSettings(themeId: String, accentHex: String?, playerMode: String) {
        selectedThemeId = themeId
        selectedAccentHex = accentHex
        selectedPlayerViewMode = playerMode
        AppColors.applyTheme(themeId, accentHex)
        val cur = settingsRepository.load()
        settingsRepository.save(
            cur.copy(
                themePresetId = themeId,
                customAccentHex = accentHex,
                playerViewMode = playerMode
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 480.dp, max = 680.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Surface(
            color = AppColors.Surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppColors.Accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = AppColors.Accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(spacing.sm))
                        Column {
                            Text(
                                "Оформление и темы",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                            )
                            Text(
                                "Персонализация интерфейса и цветов плеера",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppColors.TextSecondary
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(spacing.md))

                // Interactive Mini Preview Card
                Text(
                    "ПРЕДПРОСМОТР",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.height(spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Background)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
                        .padding(spacing.md)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(spacing.sm))
                                Column {
                                    Text(
                                        "PHANTOM / СНАЙПЕР",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TextPrimary
                                        )
                                    )
                                    Text(
                                        "Демонстрация активного стиля",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = AppColors.TextSecondary
                                        )
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AppColors.Accent)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Акцент",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (com.example.musicapp.theme.isLightColor(AppColors.Accent)) Color.Black else Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(spacing.sm))

                        // Progress scrubber preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(AppColors.SurfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Accent)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.md))

                // Section 1: Themes
                Text(
                    "ТЕМА ОФОРМЛЕНИЯ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.height(spacing.xs))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePresets.all.forEach { preset ->
                        val isSelected = preset.id.equals(selectedThemeId, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppColors.SurfaceElevated else AppColors.Surface)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AppColors.Accent else AppColors.Border,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    commitSettings(preset.id, selectedAccentHex, selectedPlayerViewMode)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Mini palette swatch dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(preset.background)
                                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        )
                                        Box(
                                            Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(preset.surface)
                                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        )
                                        Box(
                                            Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(preset.defaultAccent)
                                        )
                                    }

                                    Spacer(Modifier.width(spacing.sm))

                                    Column {
                                        Text(
                                            preset.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) AppColors.Accent else AppColors.TextPrimary
                                            )
                                        )
                                        Text(
                                            preset.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = AppColors.TextSecondary,
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Выбрано",
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(spacing.md))

                // Section 2: Accent Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "ЦВЕТОВОЙ АКЦЕНТ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AppColors.TextMuted,
                            letterSpacing = 1.sp
                        )
                    )

                    if (selectedAccentHex != null) {
                        Text(
                            "Сбросить к цветам темы",
                            fontSize = 11.sp,
                            color = AppColors.Accent,
                            modifier = Modifier.clickable {
                                customHexInput = ""
                                hexError = false
                                commitSettings(selectedThemeId, null, selectedPlayerViewMode)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(spacing.xs))

                // Curated color swatches
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultAccentOptions.forEach { opt ->
                        val isOptSelected = (selectedAccentHex != null && selectedAccentHex!!.equals(opt.hex, ignoreCase = true)) ||
                                (selectedAccentHex == null && AppColors.Accent == opt.color)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(opt.color)
                                .border(
                                    width = if (isOptSelected) 2.5.dp else 1.dp,
                                    color = if (isOptSelected) Color.White else Color.Black.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    customHexInput = opt.hex
                                    hexError = false
                                    commitSettings(selectedThemeId, opt.hex, selectedPlayerViewMode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isOptSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = opt.name,
                                    tint = if (com.example.musicapp.theme.isLightColor(opt.color)) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(spacing.sm))

                // Custom HEX code input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.SurfaceElevated)
                            .border(
                                1.dp,
                                if (hexError) AppColors.Error else AppColors.Border,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = customHexInput,
                            onValueChange = { input ->
                                customHexInput = input
                                val parsed = parseHexColor(input)
                                if (parsed != null) {
                                    hexError = false
                                    commitSettings(selectedThemeId, input, selectedPlayerViewMode)
                                } else if (input.isNotBlank()) {
                                    hexError = true
                                } else {
                                    hexError = false
                                }
                            },
                            textStyle = TextStyle(
                                color = AppColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(AppColors.Accent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (customHexInput.isEmpty()) {
                                    Text(
                                        "Свой HEX (например #00D26A)...",
                                        color = AppColors.TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(Modifier.width(spacing.xs))

                    // Live preview box of the entered hex
                    val parsedColor = parseHexColor(customHexInput)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(parsedColor ?: AppColors.Accent)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                    )
                }

                Spacer(Modifier.height(spacing.md))

                // Section 3: Player View Mode (Reference Phantom Dashboard vs Classic)
                Text(
                    "РЕЖИМ ПОЛНОЭКРАННОГО ПЛЕЕРА",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.height(spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Phantom Dashboard option
                    val isPhantom = selectedPlayerViewMode == "PHANTOM_DASHBOARD"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPhantom) AppColors.SurfaceElevated else AppColors.Surface)
                            .border(
                                width = if (isPhantom) 1.5.dp else 1.dp,
                                color = if (isPhantom) AppColors.Accent else AppColors.Border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                commitSettings(selectedThemeId, selectedAccentHex, "PHANTOM_DASHBOARD")
                            }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Dashboard,
                                    contentDescription = null,
                                    tint = if (isPhantom) AppColors.Accent else AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isPhantom) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Phantom Dashboard",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPhantom) AppColors.Accent else AppColors.TextPrimary
                                )
                            )
                            Text(
                                "Стиль референса: 2 колонки, часы, визуализатор и текст",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppColors.TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }

                    // Classic option
                    val isClassic = selectedPlayerViewMode == "CLASSIC"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isClassic) AppColors.SurfaceElevated else AppColors.Surface)
                            .border(
                                width = if (isClassic) 1.5.dp else 1.dp,
                                color = if (isClassic) AppColors.Accent else AppColors.Border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                commitSettings(selectedThemeId, selectedAccentHex, "CLASSIC")
                            }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ViewAgenda,
                                    contentDescription = null,
                                    tint = if (isClassic) AppColors.Accent else AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isClassic) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Классический",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isClassic) AppColors.Accent else AppColors.TextPrimary
                                )
                            )
                            Text(
                                "Центрированная обложка с эквалайзером",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppColors.TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.lg))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            customHexInput = ""
                            hexError = false
                            commitSettings("JVX_DEFAULT", null, "PHANTOM_DASHBOARD")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Сброс", fontSize = 12.5.sp)
                    }

                    Spacer(Modifier.width(spacing.xs))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = if (com.example.musicapp.theme.isLightColor(AppColors.Accent)) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Готово", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
