package com.example.camerax.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camerax.model.AspectRatioOption
import com.example.camerax.model.CameraFilter
import com.example.camerax.processing.ImageProcessingPipeline
import androidx.core.content.ContextCompat
import com.example.camerax.camera.CameraViewModel
import com.example.camerax.model.CameraMode
import com.example.camerax.model.FlashMode
import com.example.camerax.ui.components.CameraBottomBar
import com.example.camerax.ui.components.CameraTopBar
import com.example.camerax.ui.components.ColorStudioBottomSheet
import com.example.camerax.ui.components.DocumentGuideOverlay
import com.example.camerax.ui.components.FiltersBottomSheet
import com.example.camerax.ui.components.GridLinesOverlay
import com.example.camerax.ui.components.HistogramOverlay
import com.example.camerax.ui.components.HorizonLevelOverlay
import com.example.camerax.ui.components.NightModeProcessingBanner
import com.example.camerax.ui.components.ProControlsHUD
import com.example.camerax.ui.components.RecordingBadge
import com.example.camerax.ui.components.TimerCountdownOverlay
import com.example.camerax.ui.components.ZoomControls
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("MissingPermission")
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val orientationAngles by viewModel.sensorManager.orientationAngles.collectAsState()

    var showColorStudioSheet by remember { mutableStateOf(false) }
    var showFiltersSheet by remember { mutableStateOf(false) }

    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()

    // Sensor lifecycle
    DisposableEffect(Unit) {
        viewModel.startOrientationSensor()
        onDispose {
            viewModel.stopOrientationSensor()
        }
    }

    // CameraX Bind & Rebind on facing or mode change
    LaunchedEffect(uiState.isBackCamera, previewView) {
        val pView = previewView ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val cameraSelector = if (uiState.isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pView.surfaceProvider)
                }

                val iCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                val vCapture = VideoCapture.withOutput(recorder)

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    iCapture,
                    vCapture
                )

                activeCamera = camera
                imageCapture = iCapture
                videoCapture = vCapture
                viewModel.activeCameraControl = camera.cameraControl
                viewModel.activeCameraInfo = camera.cameraInfo

                // Restore zoom
                camera.cameraControl.setZoomRatio(uiState.zoomRatio.coerceAtMost(uiState.capabilities.maxZoomRatio))
            } catch (e: Exception) {
                // Handle camera binding error gracefully
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Photo capture execution
    fun executePhotoCapture() {
        val iCapture = imageCapture ?: return
        val tempFile = File.createTempFile("raw_cap_", ".jpg", context.cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        // Set flash mode
        iCapture.flashMode = when (uiState.flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON, FlashMode.TORCH -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }

        viewModel.flashShutterAnimation()

        iCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.processCapturedImage(tempFile) {
                        // Image saved and processed
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    tempFile.delete()
                }
            }
        )
    }

    // Video recording toggle
    fun toggleVideoRecording() {
        val vCapture = videoCapture ?: return

        if (uiState.isRecording) {
            viewModel.activeRecording?.stop()
            viewModel.activeRecording = null
            viewModel.stopRecordingTimer()
        } else {
            val videoFile = viewModel.mediaRepository.createNewVideoFile()
            val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

            try {
                val pending = vCapture.output.prepareRecording(context, fileOutputOptions)
                    .withAudioEnabled()

                val recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            viewModel.startRecordingTimer()
                        }
                        is VideoRecordEvent.Finalize -> {
                            viewModel.stopRecordingTimer()
                            viewModel.mediaRepository.notifyVideoRecorded(videoFile)
                            coroutineScope.launch {
                                viewModel.mediaRepository.refreshMediaList()
                            }
                        }
                    }
                }
                viewModel.activeRecording = recording
            } catch (e: SecurityException) {
                // If microphone permission is missing, start recording without audio
                val pending = vCapture.output.prepareRecording(context, fileOutputOptions)
                val recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Start) {
                        viewModel.startRecordingTimer()
                    } else if (event is VideoRecordEvent.Finalize) {
                        viewModel.stopRecordingTimer()
                        viewModel.mediaRepository.notifyVideoRecorded(videoFile)
                        coroutineScope.launch {
                            viewModel.mediaRepository.refreshMediaList()
                        }
                    }
                }
                viewModel.activeRecording = recording
            } catch (e: Exception) {
                // Handle video start error
            }
        }
    }

    // Live Color Studio Hardware RenderEffect on Android 12+ (API 31+)
    LaunchedEffect(uiState.colorStudioConfig, uiState.selectedFilter, previewView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pView = previewView ?: return@LaunchedEffect
            val matrix = ImageProcessingPipeline.createCombinedColorMatrix(
                uiState.colorStudioConfig,
                uiState.selectedFilter
            )
            val effect = android.graphics.RenderEffect.createColorFilterEffect(
                android.graphics.ColorMatrixColorFilter(matrix)
            )
            pView.setRenderEffect(effect)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Aspect Ratio Box Container (1:1, 4:3, 16:9, Full)
        val viewfinderModifier = when (uiState.aspectRatio) {
            AspectRatioOption.RATIO_1_1 -> Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.Center)
            AspectRatioOption.RATIO_4_3 -> Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .align(Alignment.Center)
            AspectRatioOption.RATIO_16_9 -> Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .align(Alignment.Center)
            AspectRatioOption.RATIO_FULL -> Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        }

        Box(
            modifier = viewfinderModifier
                .clipToBounds()
                .graphicsLayer {
                    scaleX = uiState.digitalZoomScale
                    scaleY = uiState.digitalZoomScale
                }
        ) {
            // Camera Viewfinder (PreviewView) with pinch-to-zoom
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1.0f) {
                                viewModel.onPinchZoom(zoom)
                            }
                        }
                    }
            )

            // Live Color Studio Real-time Tint Layers (Tone, Warmth, Brightness, Filters)
            val studio = uiState.colorStudioConfig
            val warmthFactor = (studio.warmth / 100f).coerceIn(-1f, 1f)
            if (warmthFactor > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFA500).copy(alpha = warmthFactor * 0.28f))
                )
            } else if (warmthFactor < -0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF38BDF8).copy(alpha = -warmthFactor * 0.28f))
                )
            }

            val toneFactor = (studio.tone / 100f).coerceIn(-1f, 1f)
            if (toneFactor > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF4ADE80).copy(alpha = toneFactor * 0.20f))
                )
            } else if (toneFactor < -0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF43F5E).copy(alpha = -toneFactor * 0.20f))
                )
            }

            if (studio.brightness > 5f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = (studio.brightness / 100f) * 0.35f))
                )
            } else if (studio.brightness < -5f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (-studio.brightness / 100f) * 0.45f))
                )
            }

            when (uiState.selectedFilter) {
                CameraFilter.MONO -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    )
                }
                CameraFilter.VINTAGE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD4A373).copy(alpha = 0.22f))
                    )
                }
                CameraFilter.CINEMATIC -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F766E).copy(alpha = 0.20f))
                    )
                }
                CameraFilter.WARM_GLOW -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF59E0B).copy(alpha = 0.22f))
                    )
                }
                CameraFilter.COOL_BREEZE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0284C7).copy(alpha = 0.22f))
                    )
                }
                else -> {}
            }

            // Viewfinder Overlays inside the framed viewport
            if (uiState.proControls.showGrid) {
                GridLinesOverlay()
            }

            if (uiState.currentMode == CameraMode.DOCUMENT) {
                DocumentGuideOverlay()
            }
        }

        if (uiState.proControls.showLevel) {
            HorizonLevelOverlay(orientation = orientationAngles)
        }

        if (uiState.proControls.showHistogram) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp, start = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                HistogramOverlay()
            }
        }



        // Timer Countdown Overlay
        TimerCountdownOverlay(countdown = uiState.timerCountdown)

        // Super Night processing message
        NightModeProcessingBanner(message = uiState.processingMessage)

        // Recording active badge
        if (uiState.isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 90.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                RecordingBadge(durationSeconds = uiState.recordingDurationSec)
            }
        }

        // Top UI Controls & Telemetry
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            CameraTopBar(
                flashMode = uiState.flashMode,
                aspectRatio = uiState.aspectRatio,
                timer = uiState.timer,
                aiEnhanceEnabled = uiState.aiEnhanceConfig.enabled,
                superNightEnabled = uiState.superNightConfig.enabled,
                batteryPercent = uiState.batteryPercent,
                freeStorageGb = uiState.freeStorageGb,
                onCycleFlash = { viewModel.cycleFlashMode() },
                onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                onCycleTimer = { viewModel.cycleTimer() },
                onToggleAiEnhance = { viewModel.toggleAiEnhance() },
                onToggleSuperNight = { viewModel.toggleSuperNight() },
                onOpenColorStudio = { showColorStudioSheet = true },
                onOpenSettings = onNavigateToSettings
            )
        }

        // Bottom Controls HUD, Zoom Pills, and Shutter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Pro Controls HUD if in PRO mode
            if (uiState.currentMode == CameraMode.PRO) {
                ProControlsHUD(
                    proControls = uiState.proControls,
                    minEv = uiState.capabilities.minExposureIndex,
                    maxEv = uiState.capabilities.maxExposureIndex,
                    evStep = uiState.capabilities.exposureStep,
                    onEvChange = { viewModel.setExposureBias(it) },
                    onToggleGrid = { viewModel.toggleGrid() },
                    onToggleLevel = { viewModel.toggleLevel() },
                    onToggleHistogram = { viewModel.toggleHistogram() },
                    onToggleFocusPeaking = { viewModel.toggleFocusPeaking() }
                )
            }

            // Zoom Pills
            ZoomControls(
                currentZoom = uiState.zoomRatio,
                supportedSteps = uiState.capabilities.supportedZoomSteps,
                minZoom = uiState.capabilities.minZoomRatio,
                maxZoom = uiState.capabilities.maxZoomRatio,
                extendedZoomEnabled = uiState.extendedZoom100xEnabled,
                onZoomChange = { viewModel.setZoomRatio(it) }
            )

            // Shutter & Mode Carousel Bar
            CameraBottomBar(
                currentMode = uiState.currentMode,
                isRecording = uiState.isRecording,
                latestMedia = uiState.latestMedia,
                onSelectMode = { viewModel.setCameraMode(it) },
                onShutterClick = {
                    if (uiState.currentMode == CameraMode.VIDEO) {
                        toggleVideoRecording()
                    } else {
                        viewModel.executeCaptureWithTimer {
                            executePhotoCapture()
                        }
                    }
                },
                onShutterLongPress = {
                    // Burst capture
                    repeat(3) {
                        executePhotoCapture()
                    }
                },
                onSwitchCamera = { viewModel.toggleCameraFacing() },
                onOpenGallery = onNavigateToGallery
            )
        }

        // Color Studio Modal
        if (showColorStudioSheet) {
            ColorStudioBottomSheet(
                config = uiState.colorStudioConfig,
                onConfigChange = { viewModel.setColorStudioConfig(it) },
                onSelectPreset = { viewModel.setColorStudioPreset(it) },
                onDismiss = { showColorStudioSheet = false }
            )
        }

        // Filters Modal
        if (showFiltersSheet) {
            FiltersBottomSheet(
                currentFilter = uiState.selectedFilter,
                onSelectFilter = { viewModel.setFilter(it) },
                onDismiss = { showFiltersSheet = false }
            )
        }
    }
}
