package com.example.camerax.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorStudioBottomSheet(
    config: ColorStudioConfig,
    onConfigChange: (ColorStudioConfig) -> Unit,
    onSelectPreset: (ColorStudioPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Color Studio",
                        color = CameraGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Professional Photographic Styles",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSelectPreset(ColorStudioPreset.NATURAL) },
                        modifier = Modifier.testTag("reset_color_studio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Horizontal Row
            val presetsScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(presetsScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorStudioPreset.values().forEach { preset ->
                    val isSelected = preset == config.preset
                    Surface(
                        onClick = { onSelectPreset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CameraGold else ObsidianCard,
                        modifier = Modifier.testTag("preset_${preset.name.lowercase()}")
                    ) {
                        Text(
                            text = preset.titleEn,
                            color = if (isSelected) ObsidianDark else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sliders Section
            StudioSlider(
                label = "Tone (Green / Magenta)",
                value = config.tone,
                range = -100f..100f,
                onValueChange = { onConfigChange(config.copy(tone = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            StudioSlider(
                label = "Warmth (Blue / Amber)",
                value = config.warmth,
                range = -100f..100f,
                onValueChange = { onConfigChange(config.copy(warmth = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            StudioSlider(
                label = "Effect Intensity",
                value = config.intensity,
                range = 0f..100f,
                onValueChange = { onConfigChange(config.copy(intensity = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            StudioSlider(
                label = "Contrast",
                value = config.contrast,
                range = -50f..50f,
                onValueChange = { onConfigChange(config.copy(contrast = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            StudioSlider(
                label = "Highlights",
                value = config.highlights,
                range = -50f..50f,
                onValueChange = { onConfigChange(config.copy(highlights = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            StudioSlider(
                label = "Shadows",
                value = config.shadows,
                range = -50f..50f,
                onValueChange = { onConfigChange(config.copy(shadows = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            StudioSlider(
                label = "Brightness",
                value = config.brightness,
                range = -50f..50f,
                onValueChange = { onConfigChange(config.copy(brightness = it, preset = ColorStudioPreset.CUSTOM)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Text(
                text = "${if (value > 0) "+" else ""}${value.toInt()}",
                color = CameraGold,
                fontSize = 12.sp,
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
            )
        )
    }
}
