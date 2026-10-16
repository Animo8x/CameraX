package com.example.camerax.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.camerax.model.CameraMode
import com.example.camerax.model.MediaItem
import com.example.ui.theme.CameraGold
import com.example.ui.theme.CameraRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDark

@Composable
fun CameraBottomBar(
    currentMode: CameraMode,
    isRecording: Boolean,
    latestMedia: MediaItem?,
    onSelectMode: (CameraMode) -> Unit,
    onShutterClick: () -> Unit,
    onShutterLongPress: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cameraFlipAngle by remember { mutableStateOf(0f) }
    val animatedFlipAngle by animateFloatAsState(targetValue = cameraFlipAngle, label = "flip")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Selector Carousel
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraMode.values().forEach { mode ->
                val isSelected = mode == currentMode
                Column(
                    modifier = Modifier
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("mode_${mode.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mode.titleEn.uppercase(),
                        color = if (isSelected) CameraGold else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(CameraGold)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Controls Row: Gallery Thumbnail, Shutter Button, Switch Camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Thumbnail
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .background(ObsidianDark)
                    .clickable { onOpenGallery() }
                    .testTag("gallery_thumbnail_button"),
                contentAlignment = Alignment.Center
            ) {
                if (latestMedia != null && latestMedia.file.exists()) {
                    AsyncImage(
                        model = latestMedia.file,
                        contentDescription = "Last captured media",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Flagship Shutter Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .pointerInput(currentMode, isRecording) {
                        detectTapGestures(
                            onTap = { onShutterClick() },
                            onLongPress = {
                                if (currentMode == CameraMode.PHOTO) {
                                    onShutterLongPress()
                                }
                            }
                        )
                    }
                    .testTag("shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (currentMode == CameraMode.VIDEO) {
                    if (isRecording) {
                        // Stop recording: Red rounded square
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CameraRed)
                        )
                    } else {
                        // Start recording: Red circle
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(CameraRed)
                        )
                    }
                } else {
                    // Photo / Pro / Portrait / Night / Document: White ring with solid core
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Flip Camera Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable {
                        cameraFlipAngle += 180f
                        onSwitchCamera()
                    }
                    .testTag("switch_camera_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch camera",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(animatedFlipAngle)
                )
            }
        }
    }
}
