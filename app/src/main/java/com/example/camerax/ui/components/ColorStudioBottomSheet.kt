package com.example.camerax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerax.model.ColorStudioConfig
import com.example.camerax.model.ColorStudioPreset
import com.example.ui.theme.CameraGold
import com.example.ui.theme.ObsidianDark

@Composable
fun ColorStudioBottomSheet(
    config: ColorStudioConfig,
    onConfigChange: (ColorStudioConfig) -> Unit,
    onSelectPreset: (ColorStudioPreset) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Warmth & Tone, 1: Light & Contrast, 2: Intensity

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.84f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CameraGold.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("color_studio_compact_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Title, Preset name, Reset, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Color Studio",
                        color = CameraGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${config.preset.titleEn}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSelectPreset(ColorStudioPreset.NATURAL) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("reset_color_studio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Presets Horizontal Row
            val presetsScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetsScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ColorStudioPreset.values().forEach { preset ->
                    val isSelected = preset == config.preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CameraGold else Color.White.copy(alpha = 0.12f))
                            .clickable { onSelectPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("preset_${preset.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.titleEn,
                            color = if (isSelected) ObsidianDark else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Selector: [Warmth & Tone] - [Light & Contrast] - [Intensity]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf("Warmth & Tone", "Light & Contrast", "Intensity")
                tabs.forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CameraGold.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) CameraGold else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Active Sliders according to selected Tab
            when (activeTab) {
                0 -> {
                    // Warmth & Tone
                    CompactStudioSlider(
                        label = "Warmth (Blue ↔ Amber)",
                        value = config.warmth,
                        range = -100f..100f,
                        onValueChange = { onConfigChange(config.copy(warmth = it, preset = ColorStudioPreset.CUSTOM)) }
                    )
                    CompactStudioSlider(
                        label = "Tone (Green ↔ Magenta)",
                        value = config.tone,
                        range = -100f..100f,
                        onValueChange = { onConfigChange(config.copy(tone = it, preset = ColorStudioPreset.CUSTOM)) }
                    )
                }
                1 -> {
                    // Light & Contrast
                    CompactStudioSlider(
                        label = "Brightness",
                        value = config.brightness,
                        range = -50f..50f,
                        onValueChange = { onConfigChange(config.copy(brightness = it, preset = ColorStudioPreset.CUSTOM)) }
                    )
                    CompactStudioSlider(
                        label = "Contrast",
                        value = config.contrast,
                        range = -50f..50f,
                        onValueChange = { onConfigChange(config.copy(contrast = it, preset = ColorStudioPreset.CUSTOM)) }
                    )
                }
                2 -> {
                    // Intensity & Highlights
                    CompactStudioSlider(
                        label = "Overall Intensity",
                        value = config.intensity,
                        range = 0f..100f,
                        onValueChange = { onConfigChange(config.copy(intensity = it, preset = ColorStudioPreset.CUSTOM)) }
                    )
                    CompactStudioSlider(
                        label = "Shadows Lift",
                        value = config.shadows,
                        range = -50f..50f,
                        onValueChange = { onConfigChange(config.copy(shadows = it, preset = ColorStudioPreset.CUSTOM)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactStudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color(0xFFCBD5E1), fontSize = 11.sp)
            Text(
                text = "${if (value > 0) "+" else ""}${value.toInt()}",
                color = CameraGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CameraGold,
                activeTrackColor = CameraGold,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
