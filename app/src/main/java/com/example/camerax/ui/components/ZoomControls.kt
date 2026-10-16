package com.example.camerax.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import com.example.ui.theme.CameraGold
import com.example.ui.theme.CameraRed
import com.example.ui.theme.ObsidianDark

@Composable
fun ZoomControls(
    currentZoom: Float,
    supportedSteps: List<Float>,
    minZoom: Float,
    maxZoom: Float,
    extendedZoomEnabled: Boolean,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPrecisionSlider by remember { mutableStateOf(false) }

    val effectiveMaxZoom = if (extendedZoomEnabled) 100f else maxZoom
    val effectiveMinZoom = if (supportedSteps.any { it < 0.95f } || minZoom < 0.95f) 0.5f else minZoom
    val steps = remember(supportedSteps, extendedZoomEnabled, maxZoom) {
        val list = supportedSteps.toMutableList()
        if (extendedZoomEnabled) {
            if (!list.contains(10f)) list.add(10f)
            if (!list.contains(30f)) list.add(30f)
            if (!list.contains(100f)) list.add(100f)
        }
        list.distinct().sorted()
    }

    val isZoomActive = Math.abs(currentZoom - 1.0f) > 0.05f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Precision slider expandable when long-pressed or tapped
        AnimatedVisibility(
            visible = showPrecisionSlider,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%.1fx", currentZoom),
                    color = CameraGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Slider(
                    value = currentZoom,
                    onValueChange = onZoomChange,
                    valueRange = effectiveMinZoom..effectiveMaxZoom,
                    colors = SliderDefaults.colors(
                        thumbColor = CameraGold,
                        activeTrackColor = CameraGold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("zoom_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Zoom Steps Pills + Red "X" Reset Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Zoom Pills Container
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEach { step ->
                    val isSelected = Math.abs(currentZoom - step) < 0.2f
                    val isUltraWide = step < 0.95f
                    val label = when {
                        isUltraWide -> if (step == 0.5f) "0.5x" else "${step}x"
                        step >= 10f -> "${step.toInt()}x"
                        step % 1f == 0f -> "${step.toInt()}x"
                        else -> "${step}x"
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CameraGold else Color.White.copy(alpha = 0.12f))
                            .clickable {
                                onZoomChange(step)
                                showPrecisionSlider = !showPrecisionSlider
                            }
                            .testTag("zoom_pill_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) ObsidianDark else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }

            // Red "X" Reset Zoom Button: Appears when any zoom is active (e.g. 0.5x, 2x, 50x, 100x)
            AnimatedVisibility(
                visible = isZoomActive,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CameraRed)
                            .clickable {
                                onZoomChange(1.0f)
                                showPrecisionSlider = false
                            }
                            .testTag("reset_zoom_red_x_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset zoom to 1x",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
