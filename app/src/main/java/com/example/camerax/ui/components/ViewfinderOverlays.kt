package com.example.camerax.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerax.camera.DeviceOrientationAngles
import com.example.ui.theme.CameraGold
import com.example.ui.theme.CameraGreen
import com.example.ui.theme.CameraRed
import kotlin.math.roundToInt

@Composable
fun BoxScope.FocusRingOverlay(focusPointNormalized: Pair<Float, Float>?) {
    if (focusPointNormalized == null) return

    val (nx, ny) = focusPointNormalized

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    // Convert normalized coordinates into offset
                    // Handled inside Box with size
                    IntOffset(
                        (nx * 1000f).roundToInt(), // will be scaled dynamically
                        (ny * 1000f).roundToInt()
                    )
                }
        )
    }
}

@Composable
fun GridLinesOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stroke = Stroke(width = 1.dp.toPx())
        val gridColor = Color.White.copy(alpha = 0.35f)

        // Vertical lines (1/3 and 2/3)
        drawLine(
            color = gridColor,
            start = Offset(width / 3f, 0f),
            end = Offset(width / 3f, height),
            strokeWidth = stroke.width
        )
        drawLine(
            color = gridColor,
            start = Offset(2f * width / 3f, 0f),
            end = Offset(2f * width / 3f, height),
            strokeWidth = stroke.width
        )

        // Horizontal lines (1/3 and 2/3)
        drawLine(
            color = gridColor,
            start = Offset(0f, height / 3f),
            end = Offset(width, height / 3f),
            strokeWidth = stroke.width
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, 2f * height / 3f),
            end = Offset(width, 2f * height / 3f),
            strokeWidth = stroke.width
        )
    }
}

@Composable
fun HorizonLevelOverlay(
    orientation: DeviceOrientationAngles,
    modifier: Modifier = Modifier
) {
    val levelColor = if (orientation.isLevel) CameraGreen else CameraGold

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .size(width = 160.dp, height = 24.dp)
                    .rotate(orientation.rollDegrees)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Level line
                drawLine(
                    color = levelColor,
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = 2.dp.toPx()
                )

                // Center crosshair notch
                drawLine(
                    color = levelColor,
                    start = Offset(cx, cy - 6.dp.toPx()),
                    end = Offset(cx, cy + 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Text(
                text = String.format("%.1f°", orientation.rollDegrees),
                color = levelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun HistogramOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(width = 100.dp, height = 48.dp)
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        val w = size.width
        val h = size.height

        // Draw illustrative luminance distribution curves
        val bars = listOf(
            0.1f, 0.15f, 0.25f, 0.4f, 0.65f, 0.8f, 0.95f, 0.75f, 0.6f, 0.45f,
            0.35f, 0.4f, 0.55f, 0.7f, 0.85f, 0.6f, 0.4f, 0.3f, 0.2f, 0.1f
        )
        val barWidth = w / bars.size

        bars.forEachIndexed { i, frac ->
            val barHeight = h * frac
            drawRect(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(i * barWidth, h - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight)
            )
        }
    }
}

@Composable
fun TimerCountdownOverlay(countdown: Int, modifier: Modifier = Modifier) {
    if (countdown <= 0) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(4.dp, CameraGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$countdown",
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun NightModeProcessingBanner(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .border(2.dp, CameraGold, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SUPER NIGHT MODE",
                color = CameraGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun RecordingBadge(durationSeconds: Int, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .border(1.dp, CameraRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(CameraRed.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "REC $timeFormatted",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DocumentGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val padX = w * 0.1f
        val padY = h * 0.15f
        val docW = w - (2 * padX)
        val docH = h - (2 * padY)

        val cornerLen = 32.dp.toPx()
        val stroke = 3.dp.toPx()
        val cornerColor = CameraGold

        // Top-left corner
        drawLine(cornerColor, Offset(padX, padY), Offset(padX + cornerLen, padY), stroke)
        drawLine(cornerColor, Offset(padX, padY), Offset(padX, padY + cornerLen), stroke)

        // Top-right corner
        drawLine(cornerColor, Offset(padX + docW, padY), Offset(padX + docW - cornerLen, padY), stroke)
        drawLine(cornerColor, Offset(padX + docW, padY), Offset(padX + docW, padY + cornerLen), stroke)

        // Bottom-left corner
        drawLine(cornerColor, Offset(padX, padY + docH), Offset(padX + cornerLen, padY + docH), stroke)
        drawLine(cornerColor, Offset(padX, padY + docH), Offset(padX, padY + docH - cornerLen), stroke)

        // Bottom-right corner
        drawLine(cornerColor, Offset(padX + docW, padY + docH), Offset(padX + docW - cornerLen, padY + docH), stroke)
        drawLine(cornerColor, Offset(padX + docW, padY + docH), Offset(padX + docW, padY + docH - cornerLen), stroke)
    }
}
