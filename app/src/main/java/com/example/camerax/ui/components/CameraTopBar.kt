package com.example.camerax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerax.model.AspectRatioOption
import com.example.camerax.model.FlashMode
import com.example.camerax.model.TimerOption
import com.example.ui.theme.CameraGold
import com.example.ui.theme.CameraGoldLight
import com.example.ui.theme.CameraGreen
import com.example.ui.theme.ObsidianBorder

@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    aspectRatio: AspectRatioOption,
    timer: TimerOption,
    aiEnhanceEnabled: Boolean,
    superNightEnabled: Boolean,
    batteryPercent: Int,
    freeStorageGb: Float,
    onCycleFlash: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleAiEnhance: () -> Unit,
    onToggleSuperNight: () -> Unit,
    onOpenColorStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Discreet telemetry row (Battery & Free Storage)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = if (batteryPercent > 20) Color(0xFF94A3B8) else Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryPercent%",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Storage",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${freeStorageGb} GB free",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Quick Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Mode button
            IconButton(
                onClick = onCycleFlash,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("flash_button")
            ) {
                val icon = when (flashMode) {
                    FlashMode.OFF -> Icons.Default.FlashOff
                    FlashMode.AUTO -> Icons.Default.FlashAuto
                    FlashMode.ON, FlashMode.TORCH -> Icons.Default.FlashOn
                }
                val tint = if (flashMode == FlashMode.OFF) Color.White else CameraGold
                Icon(
                    imageVector = icon,
                    contentDescription = "Flash: ${flashMode.name}",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Aspect Ratio button
            Surface(
                onClick = onCycleAspectRatio,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("aspect_ratio_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = "Aspect ratio",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = aspectRatio.label,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // Timer button
            IconButton(
                onClick = onCycleTimer,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("timer_button")
            ) {
                val (icon, tint) = when (timer) {
                    TimerOption.OFF -> Pair(Icons.Default.Timer, Color.White)
                    TimerOption.SEC_3 -> Pair(Icons.Default.Timer3, CameraGold)
                    TimerOption.SEC_5 -> Pair(Icons.Default.Timer, CameraGold)
                    TimerOption.SEC_10 -> Pair(Icons.Default.Timer10, CameraGold)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Timer ${timer.label}",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }

            // AI Enhancement Toggle
            IconButton(
                onClick = onToggleAiEnhance,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (aiEnhanceEnabled) CameraGold.copy(alpha = 0.25f) else Color.Transparent)
                    .testTag("ai_enhance_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Enhancement",
                    tint = if (aiEnhanceEnabled) CameraGoldLight else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Super Night Toggle
            IconButton(
                onClick = onToggleSuperNight,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (superNightEnabled) CameraGold.copy(alpha = 0.25f) else Color.Transparent)
                    .testTag("night_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = "Super Night Mode",
                    tint = if (superNightEnabled) CameraGoldLight else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Color Studio
            IconButton(
                onClick = onOpenColorStudio,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("color_studio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Color Studio",
                    tint = CameraGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Settings
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
