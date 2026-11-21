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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
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
import com.example.musicapp.theme.ThemeBackdrop
import com.example.musicapp.theme.ThemeConfig
import com.example.musicapp.theme.ThemeIllustration
import com.example.musicapp.theme.ThemePattern
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
    var showBackdrop by remember { mutableStateOf(currentSettings.showThemeBackdrop) }
    var customHexInput by remember { mutableStateOf(selectedAccentHex ?: "") }
    var hexError by remember { mutableStateOf(false) }

    fun commitSettings(
        themeId: String,
        accentHex: String?,
        playerMode: String,
        backdrop: Boolean = showBackdrop
    ) {
        selectedThemeId = themeId
        selectedAccentHex = accentHex
        selectedPlayerViewMode = playerMode
        showBackdrop = backdrop
        AppColors.applyTheme(themeId, accentHex, backdrop)
        val cur = settingsRepository.load()
        settingsRepository.save(
            cur.copy(
                themePresetId = themeId,
                customAccentHex = accentHex,
                playerViewMode = playerMode,
                showThemeBackdrop = backdrop,
                themeConfig = ThemeConfig(
                    presetId = themeId,
                    customAccentHex = accentHex,
                    patternEnabled = backdrop
                )
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 480.dp, max = 680.dp)
            .fillMaxHeight(0.88f)
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

                // Interactive Mini Preview Card with Rich Visual Artwork & Theme Backdrop
                val activePreset = ThemePresets.fromId(selectedThemeId)
                val activeBorderGradient = activePreset.cardBorderGradient
                Text(
                    "ПРЕДПРОСМОТР ТЕМЫ И ОФОРМЛЕНИЯ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextMuted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(Modifier.height(spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(activePreset.background)
                        .then(
                            if (activeBorderGradient != null) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    brush = Brush.horizontalGradient(activeBorderGradient),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            } else {
                                Modifier.border(1.dp, activePreset.border, RoundedCornerShape(14.dp))
                            }
                        )
                ) {
                    // Live theme backdrop inside preview card
                    if (showBackdrop && activePreset.pattern != ThemePattern.NONE) {
                        ThemeBackdrop(
                            pattern = activePreset.pattern,
                            accent = AppColors.Accent,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    Column(modifier = Modifier.padding(spacing.md)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Theme miniature artwork cover thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(activePreset.surfaceElevated)
                                        .border(1.dp, activePreset.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ThemeIllustration(
                                        kind = activePreset.illustrationKind,
                                        accent = AppColors.Accent,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = AppColors.Accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(spacing.sm))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Демо-трек",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = activePreset.textPrimary
                                            )
                                        )
                                        if (activePreset.badgeText.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(activePreset.defaultAccent.copy(alpha = 0.18f))
                                                    .border(0.8.dp, activePreset.defaultAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    activePreset.badgeText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = activePreset.defaultAccent,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        if (activePreset.patternLabel.isNotBlank()) "Стиль: ${activePreset.patternLabel}" else activePreset.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = activePreset.textSecondary
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
                                    color = AppColors.OnAccent
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
                                .background(activePreset.surfaceElevated)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "ТЕМА ОФОРМЛЕНИЯ И АРТ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AppColors.TextMuted,
                            letterSpacing = 1.sp
                        )
                    )

                    // Ambient backdrop texture toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                commitSettings(selectedThemeId, selectedAccentHex, selectedPlayerViewMode, !showBackdrop)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Фоновые текстуры",
                            fontSize = 11.sp,
                            color = if (showBackdrop) AppColors.Accent else AppColors.TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (showBackdrop) AppColors.Accent else AppColors.SurfaceElevated)
                                .border(1.dp, if (showBackdrop) AppColors.Accent else AppColors.Border, RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showBackdrop) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = AppColors.OnAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.xs))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePresets.all.forEach { preset ->
                        val isSelected = preset.id.equals(selectedThemeId, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AppColors.SurfaceElevated else AppColors.Surface)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) AppColors.Accent else AppColors.Border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    commitSettings(preset.id, selectedAccentHex, selectedPlayerViewMode, showBackdrop)
                                }
                                .padding(10.dp)
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
                                    // Visual Miniature Illustration Artwork Preview Card
                                    Box(
                                        modifier = Modifier
                                            .size(width = 82.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(preset.background)
                                            .border(1.dp, preset.border.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    ) {
                                        ThemeIllustration(
                                            kind = preset.illustrationKind,
                                            accent = preset.defaultAccent,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (preset.badgeText.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(3.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color.Black.copy(alpha = 0.7f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    preset.badgeText,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = preset.defaultAccent,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                preset.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) AppColors.Accent else AppColors.TextPrimary
                                                )
                                            )
                                            if (preset.patternLabel.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(preset.defaultAccent.copy(alpha = 0.12f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        preset.patternLabel,
                                                        fontSize = 9.5.sp,
                                                        color = preset.defaultAccent,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            preset.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = AppColors.TextSecondary,
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 1
                                        )

                                        Spacer(Modifier.height(3.dp))

                                        // Mini color swatches
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(Modifier.size(11.dp).clip(CircleShape).background(preset.background).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                                            Box(Modifier.size(11.dp).clip(CircleShape).background(preset.surface).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                                            Box(Modifier.size(11.dp).clip(CircleShape).background(preset.defaultAccent))
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                preset.defaultAccent.toHexRgb(),
                                                fontSize = 10.sp,
                                                color = AppColors.TextMuted
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(AppColors.Accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Выбрано",
                                            tint = AppColors.OnAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                                    tint = com.example.musicapp.theme.contrastColorFor(opt.color),
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
                                "2 колонки, часы, визуализатор и текст",
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
                            commitSettings("JVX_DEFAULT", null, "PHANTOM_DASHBOARD", true)
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
                            contentColor = AppColors.OnAccent
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
