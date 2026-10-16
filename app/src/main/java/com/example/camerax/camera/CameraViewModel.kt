package com.example.camerax.camera

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPointFactory
import androidx.camera.video.Recording
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camerax.model.AiEnhanceConfig
import com.example.camerax.model.AspectRatioOption
import com.example.camerax.model.CameraCapabilities
import com.example.camerax.model.CameraFilter
import com.example.camerax.model.CameraMode
import com.example.camerax.model.ColorStudioConfig
import com.example.camerax.model.ColorStudioPreset
import com.example.camerax.model.FlashMode
import com.example.camerax.model.MediaItem
import com.example.camerax.model.ProControlsState
import com.example.camerax.model.SteadyProLevel
import com.example.camerax.model.SuperNightConfig
import com.example.camerax.model.TimerOption
import com.example.camerax.repository.MediaRepository
import com.example.camerax.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CameraUiState(
    val currentMode: CameraMode = CameraMode.PHOTO,
    val isBackCamera: Boolean = true,
    val flashMode: FlashMode = FlashMode.OFF,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val timer: TimerOption = TimerOption.OFF,
    val timerCountdown: Int = 0,
    val zoomRatio: Float = 1.0f,
    val capabilities: CameraCapabilities = CameraCapabilities(),
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSec: Int = 0,
    val isCapturing: Boolean = false,
    val processingMessage: String? = null,
    val shutterFlash: Boolean = false,
    val latestMedia: MediaItem? = null,
    val colorStudioConfig: ColorStudioConfig = ColorStudioConfig(),
    val selectedFilter: CameraFilter = CameraFilter.NONE,
    val aiEnhanceConfig: AiEnhanceConfig = AiEnhanceConfig(),
    val superNightConfig: SuperNightConfig = SuperNightConfig(),
    val steadyProLevel: SteadyProLevel = SteadyProLevel.OFF,
    val proControls: ProControlsState = ProControlsState(),
    val extendedZoom100xEnabled: Boolean = false,
    val batteryPercent: Int = 85,
    val freeStorageGb: Float = 12.4f,
    val focusPointNormalized: Pair<Float, Float>? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    val mediaRepository = MediaRepository(application)
    val hardwareManager = CameraHardwareManager(application)
    val sensorManager = OrientationSensorManager(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    var activeCameraControl: CameraControl? = null
    var activeCameraInfo: CameraInfo? = null
    var activeRecording: Recording? = null

    private var recordingTimerJob: Job? = null
    private var countdownTimerJob: Job? = null

    init {
        // Load initial capabilities
        val initialCaps = hardwareManager.getCapabilities(isBackCamera = true)
        val initialPrefs = settingsRepository.preferences.value

        _uiState.value = _uiState.value.copy(
            capabilities = initialCaps,
            extendedZoom100xEnabled = initialPrefs.extendedZoom100x,
            aiEnhanceConfig = AiEnhanceConfig(enabled = initialPrefs.aiEnhanceEnabled),
            superNightConfig = SuperNightConfig(enabled = initialPrefs.superNightEnabled),
            steadyProLevel = initialPrefs.steadyProLevel,
            selectedFilter = initialPrefs.selectedFilter,
            colorStudioConfig = initialPrefs.colorStudioConfig,
            aspectRatio = initialPrefs.aspectRatio,
            proControls = ProControlsState(
                showGrid = initialPrefs.showGrid,
                showLevel = initialPrefs.showLevel,
                showHistogram = initialPrefs.showHistogram,
                showFocusPeaking = initialPrefs.showFocusPeaking
            )
        )

        // Read battery and storage
        refreshDeviceStats()

        // Sync media items
        viewModelScope.launch {
            mediaRepository.refreshMediaList()
            mediaRepository.latestMedia.collect { item ->
                _uiState.value = _uiState.value.copy(latestMedia = item)
            }
        }
    }

    fun startOrientationSensor() {
        sensorManager.start()
    }

    fun stopOrientationSensor() {
        sensorManager.stop()
    }

    fun setCameraMode(mode: CameraMode) {
        val autoNight = if (mode == CameraMode.NIGHT) {
            SuperNightConfig(enabled = true)
        } else {
            SuperNightConfig(enabled = settingsRepository.preferences.value.superNightEnabled)
        }

        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            superNightConfig = autoNight
        )
    }

    fun toggleCameraFacing() {
        val newFacingBack = !_uiState.value.isBackCamera
        val newCaps = hardwareManager.getCapabilities(newFacingBack)
        _uiState.value = _uiState.value.copy(
            isBackCamera = newFacingBack,
            capabilities = newCaps,
            zoomRatio = 1.0f
        )
        activeCameraControl?.setZoomRatio(1.0f)
    }

    fun setFlashMode(flashMode: FlashMode) {
        _uiState.value = _uiState.value.copy(flashMode = flashMode)
        val torchEnabled = flashMode == FlashMode.TORCH
        activeCameraControl?.enableTorch(torchEnabled)
    }

    fun cycleFlashMode() {
        val next = when (_uiState.value.flashMode) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.TORCH
            FlashMode.TORCH -> FlashMode.OFF
        }
        setFlashMode(next)
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _uiState.value = _uiState.value.copy(aspectRatio = ratio)
        settingsRepository.updateAspectRatio(ratio)
    }

    fun cycleAspectRatio() {
        val next = when (_uiState.value.aspectRatio) {
            AspectRatioOption.RATIO_4_3 -> AspectRatioOption.RATIO_16_9
            AspectRatioOption.RATIO_16_9 -> AspectRatioOption.RATIO_1_1
            AspectRatioOption.RATIO_1_1 -> AspectRatioOption.RATIO_FULL
            AspectRatioOption.RATIO_FULL -> AspectRatioOption.RATIO_4_3
        }
        setAspectRatio(next)
    }

    fun setTimer(timer: TimerOption) {
        _uiState.value = _uiState.value.copy(timer = timer)
    }

    fun cycleTimer() {
        val next = when (_uiState.value.timer) {
            TimerOption.OFF -> TimerOption.SEC_3
            TimerOption.SEC_3 -> TimerOption.SEC_5
            TimerOption.SEC_5 -> TimerOption.SEC_10
            TimerOption.SEC_10 -> TimerOption.OFF
        }
        setTimer(next)
    }

    fun setZoomRatio(ratio: Float) {
        val minLimit = if (_uiState.value.capabilities.hasUltraWideLens || _uiState.value.capabilities.minZoomRatio < 0.95f) {
            _uiState.value.capabilities.minZoomRatio.coerceAtMost(0.5f)
        } else {
            _uiState.value.capabilities.minZoomRatio
        }
        val maxLimit = if (_uiState.value.extendedZoom100xEnabled) 100f else _uiState.value.capabilities.maxZoomRatio
        val clamped = ratio.coerceIn(minLimit, maxLimit)
        _uiState.value = _uiState.value.copy(zoomRatio = clamped)

        // For values above physical maxZoomRatio, CameraX will use max hardware zoom, and display crop takes effect
        val hwZoom = clamped.coerceIn(_uiState.value.capabilities.minZoomRatio, _uiState.value.capabilities.maxZoomRatio)
        activeCameraControl?.setZoomRatio(hwZoom)
    }

    fun onPinchZoom(deltaScale: Float) {
        val current = _uiState.value.zoomRatio
        setZoomRatio(current * deltaScale)
    }

    fun setExposureBias(bias: Int) {
        val clamped = bias.coerceIn(
            _uiState.value.capabilities.minExposureIndex,
            _uiState.value.capabilities.maxExposureIndex
        )
        _uiState.value = _uiState.value.copy(
            proControls = _uiState.value.proControls.copy(evBias = clamped)
        )
        activeCameraControl?.setExposureCompensationIndex(clamped)
    }

    fun triggerFocusAndMetering(factory: MeteringPointFactory, x: Float, y: Float, normX: Float, normY: Float) {
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()
        activeCameraControl?.startFocusAndMetering(action)

        _uiState.value = _uiState.value.copy(focusPointNormalized = Pair(normX, normY))
        viewModelScope.launch {
            delay(2500)
            if (_uiState.value.focusPointNormalized == Pair(normX, normY)) {
                _uiState.value = _uiState.value.copy(focusPointNormalized = null)
            }
        }
    }

    fun setColorStudioConfig(config: ColorStudioConfig) {
        _uiState.value = _uiState.value.copy(colorStudioConfig = config)
        settingsRepository.updateColorStudio(config)
    }

    fun setColorStudioPreset(preset: ColorStudioPreset) {
        val config = ColorStudioConfig.defaultForPreset(preset)
        setColorStudioConfig(config)
    }

    fun setFilter(filter: CameraFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        settingsRepository.updateFilter(filter)
    }

    fun toggleAiEnhance() {
        val current = _uiState.value.aiEnhanceConfig.enabled
        val updated = !current
        _uiState.value = _uiState.value.copy(
            aiEnhanceConfig = _uiState.value.aiEnhanceConfig.copy(enabled = updated)
        )
        settingsRepository.updateAiEnhance(updated)
    }

    fun toggleSuperNight() {
        val current = _uiState.value.superNightConfig.enabled
        val updated = !current
        _uiState.value = _uiState.value.copy(
            superNightConfig = _uiState.value.superNightConfig.copy(enabled = updated)
        )
        settingsRepository.updateSuperNight(updated)
    }

    fun setSteadyProLevel(level: SteadyProLevel) {
        _uiState.value = _uiState.value.copy(steadyProLevel = level)
        settingsRepository.updateSteadyPro(level)
    }

    fun toggleExtendedZoom100x() {
        val updated = !_uiState.value.extendedZoom100xEnabled
        _uiState.value = _uiState.value.copy(extendedZoom100xEnabled = updated)
        settingsRepository.updateExtendedZoom(updated)
    }

    fun toggleGrid() {
        val updated = !_uiState.value.proControls.showGrid
        _uiState.value = _uiState.value.copy(
            proControls = _uiState.value.proControls.copy(showGrid = updated)
        )
        settingsRepository.updateShowGrid(updated)
    }

    fun toggleLevel() {
        val updated = !_uiState.value.proControls.showLevel
        _uiState.value = _uiState.value.copy(
            proControls = _uiState.value.proControls.copy(showLevel = updated)
        )
        settingsRepository.updateShowLevel(updated)
    }

    fun toggleHistogram() {
        val updated = !_uiState.value.proControls.showHistogram
        _uiState.value = _uiState.value.copy(
            proControls = _uiState.value.proControls.copy(showHistogram = updated)
        )
        settingsRepository.updateShowHistogram(updated)
    }

    fun toggleFocusPeaking() {
        val updated = !_uiState.value.proControls.showFocusPeaking
        _uiState.value = _uiState.value.copy(
            proControls = _uiState.value.proControls.copy(showFocusPeaking = updated)
        )
        settingsRepository.updateShowFocusPeaking(updated)
    }

    fun startRecordingTimer() {
        _uiState.value = _uiState.value.copy(isRecording = true, recordingDurationSec = 0)
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    recordingDurationSec = _uiState.value.recordingDurationSec + 1
                )
            }
        }
    }

    fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _uiState.value = _uiState.value.copy(isRecording = false, recordingDurationSec = 0)
        refreshDeviceStats()
    }

    fun flashShutterAnimation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(shutterFlash = true)
            delay(80)
            _uiState.value = _uiState.value.copy(shutterFlash = false)
        }
    }

    fun executeCaptureWithTimer(onCapture: () -> Unit) {
        val timerSec = _uiState.value.timer.seconds
        if (timerSec == 0) {
            onCapture()
            return
        }

        countdownTimerJob?.cancel()
        countdownTimerJob = viewModelScope.launch {
            for (i in timerSec downTo 1) {
                _uiState.value = _uiState.value.copy(timerCountdown = i)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(timerCountdown = 0)
            onCapture()
        }
    }

    fun cancelTimerCountdown() {
        countdownTimerJob?.cancel()
        countdownTimerJob = null
        _uiState.value = _uiState.value.copy(timerCountdown = 0)
    }

    fun processCapturedImage(tempFile: File, onComplete: (MediaItem) -> Unit) {
        viewModelScope.launch {
            val isNight = _uiState.value.superNightConfig.enabled || _uiState.value.currentMode == CameraMode.NIGHT
            val msg = if (isNight) "Processing Night exposure…" else null
            _uiState.value = _uiState.value.copy(isCapturing = true, processingMessage = msg)

            val item = mediaRepository.processAndSavePhoto(
                tempFile = tempFile,
                studioConfig = _uiState.value.colorStudioConfig,
                filter = _uiState.value.selectedFilter,
                aiConfig = _uiState.value.aiEnhanceConfig,
                nightConfig = _uiState.value.superNightConfig,
                isDocumentMode = _uiState.value.currentMode == CameraMode.DOCUMENT
            )

            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                processingMessage = null,
                latestMedia = item
            )
            refreshDeviceStats()
            onComplete(item)
        }
    }

    private fun refreshDeviceStats() {
        try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = getApplication<Application>().registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85

            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val freeGb = (availableBytes.toFloat() / (1024f * 1024f * 1024f))

            _uiState.value = _uiState.value.copy(
                batteryPercent = batteryPct,
                freeStorageGb = Math.round(freeGb * 10f) / 10f
            )
        } catch (e: Exception) {}
    }
}
