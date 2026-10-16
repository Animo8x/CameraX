package com.example.camerax.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerax.model.AiEnhanceConfig
import com.example.camerax.model.CameraFilter
import com.example.camerax.model.ColorStudioConfig
import com.example.camerax.model.ColorStudioPreset
import com.example.camerax.model.MediaItem
import com.example.camerax.model.SuperNightConfig
import com.example.camerax.processing.ImageProcessingPipeline
import com.example.camerax.repository.MediaRepository
import com.example.ui.theme.CameraGold
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isComparingOriginal by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Editor adjustment states
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(0f) }
    var warmth by remember { mutableStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(CameraFilter.NONE) }
    var selectedPreset by remember { mutableStateOf(ColorStudioPreset.NATURAL) }
    var aiEnhanceEnabled by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableStateOf(0f) }

    BackHandler { onNavigateBack() }

    // Load initial bitmap
    LaunchedEffect(mediaItem) {
        withContext(Dispatchers.IO) {
            val bmp = BitmapFactory.decodeFile(mediaItem.file.absolutePath)
            originalBitmap = bmp
            currentBitmap = bmp
        }
    }

    // Recalculate preview when parameters change
    fun applyLiveEdits() {
        val base = originalBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            var working = base
            if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                working = Bitmap.createBitmap(working, 0, 0, working.width, working.height, matrix, true)
            }

            val studioConfig = ColorStudioConfig(
                preset = selectedPreset,
                brightness = brightness,
                contrast = contrast,
                warmth = warmth,
                intensity = 60f
            )

            val processed = ImageProcessingPipeline.processBitmap(
                source = working,
                studioConfig = studioConfig,
                filter = selectedFilter,
                aiConfig = AiEnhanceConfig(enabled = aiEnhanceEnabled),
                nightConfig = SuperNightConfig(enabled = false)
            )

            withContext(Dispatchers.Main) {
                currentBitmap = processed
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Photo Studio Editor",
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
                actions = {
                    // Reset edits
                    IconButton(
                        onClick = {
                            brightness = 0f
                            contrast = 0f
                            warmth = 0f
                            selectedFilter = CameraFilter.NONE
                            selectedPreset = ColorStudioPreset.NATURAL
                            aiEnhanceEnabled = false
                            rotationAngle = 0f
                            currentBitmap = originalBitmap
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset edits",
                            tint = Color.White
                        )
                    }

                    // Save Copy Button
                    Button(
                        onClick = {
                            val bmpToSave = currentBitmap ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                mediaRepository.saveEditedCopy(mediaItem, bmpToSave)
                                isSaving = false
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CameraGold),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_copy_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = ObsidianDark, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Save Copy", color = ObsidianDark, fontWeight = FontWeight.Bold)
                        }
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
        ) {
            // Main image preview area with "Hold to compare Original"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (isComparingOriginal) originalBitmap else currentBitmap
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Photo Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Hold to compare Original overlay button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isComparingOriginal) CameraGold else Color.Black.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isComparingOriginal = true
                                    tryAwaitRelease()
                                    isComparingOriginal = false
                                }
                            )
                        }
                        .testTag("compare_original_button")
                ) {
                    Text(
                        text = if (isComparingOriginal) "SHOWING ORIGINAL" else "HOLD TO COMPARE",
                        color = if (isComparingOriginal) ObsidianDark else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Editor Controls Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Quick Tools Row: Rotate 90, AI Enhance Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate 90 degrees
                    Surface(
                        onClick = {
                            rotationAngle = (rotationAngle + 90f) % 360f
                            applyLiveEdits()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Rotate90DegreesCw,
                                contentDescription = "Rotate",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate 90°", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // AI Enhancement Toggle
                    Surface(
                        onClick = {
                            aiEnhanceEnabled = !aiEnhanceEnabled
                            applyLiveEdits()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (aiEnhanceEnabled) CameraGold else ObsidianCard,
                        modifier = Modifier.testTag("editor_ai_enhance")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Enhance",
                                tint = if (aiEnhanceEnabled) ObsidianDark else CameraGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI Enhance",
                                color = if (aiEnhanceEnabled) ObsidianDark else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filters row
                Text("Filters", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CameraFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            onClick = {
                                selectedFilter = filter
                                applyLiveEdits()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CameraGold else ObsidianCard
                        ) {
                            Text(
                                text = filter.titleEn,
                                color = if (isSelected) ObsidianDark else Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Adjustment Sliders
                EditorSlider(
                    label = "Brightness",
                    value = brightness,
                    range = -50f..50f,
                    onValueChange = {
                        brightness = it
                        applyLiveEdits()
                    }
                )

                EditorSlider(
                    label = "Contrast",
                    value = contrast,
                    range = -50f..50f,
                    onValueChange = {
                        contrast = it
                        applyLiveEdits()
                    }
                )

                EditorSlider(
                    label = "Warmth",
                    value = warmth,
                    range = -50f..50f,
                    onValueChange = {
                        warmth = it
                        applyLiveEdits()
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            modifier = Modifier.width(80.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CameraGold,
                activeTrackColor = CameraGold,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${if (value > 0) "+" else ""}${value.toInt()}",
            color = CameraGold,
            fontSize = 11.sp,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
