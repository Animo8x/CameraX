package com.example.camerax.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Range
import com.example.camerax.model.CameraCapabilities

class CameraHardwareManager(private val context: Context) {

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun getCapabilities(isBackCamera: Boolean): CameraCapabilities {
        try {
            val targetFacing = if (isBackCamera) {
                CameraCharacteristics.LENS_FACING_BACK
            } else {
                CameraCharacteristics.LENS_FACING_FRONT
            }

            var selectedCameraId: String? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == targetFacing) {
                    selectedCameraId = id
                    break
                }
            }

            if (selectedCameraId == null) {
                return CameraCapabilities()
            }

            val characteristics = cameraManager.getCameraCharacteristics(selectedCameraId)

            // Zoom range
            var minZoom = 1.0f
            var maxZoom = 10.0f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val zoomRange: Range<Float>? = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                if (zoomRange != null) {
                    minZoom = zoomRange.lower
                    maxZoom = zoomRange.upper
                }
            } else {
                val maxDigital = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 10f
                maxZoom = maxDigital.coerceAtLeast(1.0f)
            }

            // Flash & Torch
            val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // Stabilization
            val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            val supportsOis = oisModes?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } ?: false

            val videoStabModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            val supportsVideoStab = videoStabModes?.any {
                it == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON
            } ?: false

            // Exposure compensation
            val expRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: Range(-4, 4)
            val expStepRational = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val expStep = expStepRational?.toFloat() ?: 0.5f

            var hasUltraWide = minZoom < 0.95f
            for (id in cameraManager.cameraIdList) {
                try {
                    val c = cameraManager.getCameraCharacteristics(id)
                    if (c.get(CameraCharacteristics.LENS_FACING) == targetFacing) {
                        val focals = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        if (focals != null && focals.any { it < 3.0f }) {
                            hasUltraWide = true
                        }
                    }
                } catch (ignored: Exception) {}
            }

            // Generate clean sensible zoom pills for this camera
            val zoomSteps = mutableListOf<Float>()
            if (hasUltraWide || minZoom < 0.95f) {
                val uwVal = if (minZoom < 0.95f) roundToOneDecimal(minZoom) else 0.5f
                zoomSteps.add(uwVal)
            }
            zoomSteps.add(1.0f)
            if (maxZoom >= 2.0f) zoomSteps.add(2.0f)
            if (maxZoom >= 3.0f && (hasUltraWide || maxZoom >= 5.0f)) zoomSteps.add(3.0f)
            if (maxZoom >= 5.0f) zoomSteps.add(5.0f)
            if (maxZoom >= 10.0f) zoomSteps.add(10.0f)

            val hardwareLevelInt = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val hardwareLevelStr = when (hardwareLevelInt) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                else -> "LEGACY"
            }

            return CameraCapabilities(
                hasBackCamera = true,
                hasFrontCamera = true,
                minZoomRatio = minZoom,
                maxZoomRatio = maxZoom,
                hasFlash = flashAvailable,
                hasTorch = flashAvailable,
                supportedZoomSteps = zoomSteps.distinct().sorted(),
                supportsOpticalStabilization = supportsOis,
                supportsVideoStabilization = supportsVideoStab,
                minExposureIndex = expRange.lower,
                maxExposureIndex = expRange.upper,
                exposureStep = expStep,
                hardwareLevel = hardwareLevelStr,
                hasUltraWideLens = hasUltraWide
            )
        } catch (e: Exception) {
            return CameraCapabilities()
        }
    }

    private fun roundToOneDecimal(value: Float): Float {
        return Math.round(value * 10f) / 10f
    }
}
