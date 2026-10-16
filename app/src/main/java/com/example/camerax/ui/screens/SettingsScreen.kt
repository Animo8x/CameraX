package com.example.camerax.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerax.model.SteadyProLevel
import com.example.camerax.repository.SettingsRepository
import com.example.ui.theme.CameraGold
import com.example.ui.theme.CameraRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by settingsRepository.preferences.collectAsState()

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CameraX Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianDark)
            )
        },
        containerColor = ObsidianDark,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Brand Header Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.camerax_brand_icon),
                        contentDescription = "CameraX Brand Icon",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "CameraX Pro",
                            color = CameraGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Flagship Computational Photography",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Section 1: Photography
            SettingsSectionHeader(icon = Icons.Default.CameraAlt, title = "1. Photography Settings")
            SettingsToggleItem(
                title = "High Quality JPEG",
                description = "Captures at 95% JPEG quality with minimal compression artifacts",
                checked = prefs.highQualityJpeg,
                onCheckedChange = { settingsRepository.updateHighQualityJpeg(it) }
            )

            // Section 2: Video Recording
            SettingsSectionHeader(icon = Icons.Default.Videocam, title = "2. Video Recording")
            SettingsToggleItem(
                title = "60 FPS Recording Mode",
                description = "Records smoother 60 frames per second video when supported by device",
                checked = prefs.videoFps == 60,
                onCheckedChange = { settingsRepository.updateVideoFps(if (it) 60 else 30) }
            )

            // Section 3: Color Studio
            SettingsSectionHeader(icon = Icons.Default.Palette, title = "3. Color Studio")
            SettingsInfoItem(
                title = "Active Preset: ${prefs.colorStudioConfig.preset.titleEn}",
                description = "Tone: ${prefs.colorStudioConfig.tone.toInt()} | Warmth: ${prefs.colorStudioConfig.warmth.toInt()} | Intensity: ${prefs.colorStudioConfig.intensity.toInt()}%"
            )

            // Section 4: Filters
            SettingsSectionHeader(icon = Icons.Default.Filter, title = "4. Filters")
            SettingsInfoItem(
                title = "Current Filter: ${prefs.selectedFilter.titleEn}",
                description = "Filters remain OFF by default for true-to-life natural photography"
            )

            // Section 5: AI Enhancement
            SettingsSectionHeader(icon = Icons.Default.AutoAwesome, title = "5. AI Enhancement")
            SettingsToggleItem(
                title = "Enable AI Enhancement",
                description = "Smart edge clarity, noise reduction, and balanced dynamic range (OFF by default)",
                checked = prefs.aiEnhanceEnabled,
                onCheckedChange = { settingsRepository.updateAiEnhance(it) }
            )

            // Section 6: Super Night Mode
            SettingsSectionHeader(icon = Icons.Default.Bedtime, title = "6. Super Night Mode")
            SettingsToggleItem(
                title = "Enable Super Night Mode",
                description = "Enhances low-light exposure with shadow lift and noise suppression (OFF by default)",
                checked = prefs.superNightEnabled,
                onCheckedChange = { settingsRepository.updateSuperNight(it) }
            )

            // Section 7: HDR
            SettingsSectionHeader(icon = Icons.Default.Contrast, title = "7. HDR & Dynamic Range")
            SettingsInfoItem(
                title = "Auto HDR Processing",
                description = "Preserves details in extreme highlights and deep shadows using sensor metering"
            )

            // Section 8: Zoom & Optics
            SettingsSectionHeader(icon = Icons.Default.ZoomIn, title = "8. Zoom & Optics")
            SettingsToggleItem(
                title = "Extended Zoom 100X",
                description = "Enables digital zoom up to 100X with crop enlargement (OFF by default)",
                checked = prefs.extendedZoom100x,
                onCheckedChange = { settingsRepository.updateExtendedZoom(it) }
            )

            // Section 9: Steady Pro
            SettingsSectionHeader(icon = Icons.Default.CenterFocusStrong, title = "9. Steady Pro Stabilization")
            SettingsToggleItem(
                title = "Ultra Steady Video Mode",
                description = "Enables maximum electronic/optical stabilization for action recording",
                checked = prefs.steadyProLevel == SteadyProLevel.ULTRA_STEADY,
                onCheckedChange = {
                    settingsRepository.updateSteadyPro(if (it) SteadyProLevel.ULTRA_STEADY else SteadyProLevel.OFF)
                }
            )

            // Section 10: Pro Mode Controls
            SettingsSectionHeader(icon = Icons.Default.Tune, title = "10. Pro Mode Controls")
            SettingsToggleItem(
                title = "Show Rule-of-Thirds Grid",
                description = "Overlays a 3x3 alignment grid on the camera viewfinder",
                checked = prefs.showGrid,
                onCheckedChange = { settingsRepository.updateShowGrid(it) }
            )
            SettingsToggleItem(
                title = "Show Horizon Leveler",
                description = "Displays live tilt angle and turns green when perfectly level",
                checked = prefs.showLevel,
                onCheckedChange = { settingsRepository.updateShowLevel(it) }
            )
            SettingsToggleItem(
                title = "Live Histogram",
                description = "Real-time luminance distribution graph on viewfinder",
                checked = prefs.showHistogram,
                onCheckedChange = { settingsRepository.updateShowHistogram(it) }
            )

            // Section 11: Storage & Quality
            SettingsSectionHeader(icon = Icons.Default.SdCard, title = "11. Storage & Quality")
            SettingsInfoItem(
                title = "Storage Location",
                description = "Pictures/CameraX and Movies/CameraX in device storage"
            )

            // Section 12: Interface & Privacy
            SettingsSectionHeader(icon = Icons.Default.PrivacyTip, title = "12. Interface & Privacy")
            SettingsToggleItem(
                title = "Shutter Sound",
                description = "Play subtle auditory feedback when taking photos",
                checked = prefs.shutterSound,
                onCheckedChange = { settingsRepository.updateShutterSound(it) }
            )
            SettingsToggleItem(
                title = "Geotagging Location",
                description = "Embed GPS coordinates in photo EXIF data (strictly requires user consent)",
                checked = prefs.saveLocation,
                onCheckedChange = { settingsRepository.updateSaveLocation(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Button
            Button(
                onClick = { settingsRepository.resetAllToDefaults() },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_all_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = CameraRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Settings to Default", color = CameraRed, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CameraGold,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = CameraGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianDark,
                    checkedTrackColor = CameraGold,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = ObsidianBorder
                )
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}
