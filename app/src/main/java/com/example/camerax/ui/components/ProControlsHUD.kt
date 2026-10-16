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
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerax.model.ProControlsState
import com.example.ui.theme.CameraGold
import com.example.ui.theme.ObsidianDark

@Composable
fun ProControlsHUD(
    proControls: ProControlsState,
    minEv: Int,
    maxEv: Int,
    evStep: Float,
    onEvChange: (Int) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLevel: () -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleFocusPeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEvSlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showEvSlider) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EV: ${if (proControls.evBias > 0) "+" else ""}${proControls.evBias}",
                    color = CameraGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(55.dp)
                )
                Slider(
                    value = proControls.evBias.toFloat(),
                    onValueChange = { onEvChange(it.toInt()) },
                    valueRange = minEv.toFloat()..maxEv.toFloat(),
                    steps = (maxEv - minEv) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = CameraGold,
                        activeTrackColor = CameraGold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ev_slider")
                )
            }
        }

        // Pro Function Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // EV Pill
            ProHudButton(
                label = "EV",
                value = "${if (proControls.evBias > 0) "+" else ""}${proControls.evBias}",
                isActive = showEvSlider || proControls.evBias != 0,
                onClick = { showEvSlider = !showEvSlider },
                testTag = "pro_ev_btn"
            )

            // Grid Toggle
            ProHudButton(
                label = "GRID",
                value = if (proControls.showGrid) "ON" else "OFF",
                isActive = proControls.showGrid,
                onClick = onToggleGrid,
                testTag = "pro_grid_btn"
            )

            // Horizon Level Toggle
            ProHudButton(
                label = "LEVEL",
                value = if (proControls.showLevel) "ON" else "OFF",
                isActive = proControls.showLevel,
                onClick = onToggleLevel,
                testTag = "pro_level_btn"
            )

            // Histogram Toggle
            ProHudButton(
                label = "HISTO",
                value = if (proControls.showHistogram) "ON" else "OFF",
                isActive = proControls.showHistogram,
                onClick = onToggleHistogram,
                testTag = "pro_histo_btn"
            )

            // Peaking Toggle
            ProHudButton(
                label = "PEAK",
                value = if (proControls.showFocusPeaking) "ON" else "OFF",
                isActive = proControls.showFocusPeaking,
                onClick = onToggleFocusPeaking,
                testTag = "pro_peak_btn"
            )
        }
    }
}

@Composable
private fun ProHudButton(
    label: String,
    value: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) CameraGold else Color.White.copy(alpha = 0.12f),
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isActive) ObsidianDark else Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 11.sp,
                color = if (isActive) ObsidianDark else Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
