package com.example.camerax.model

import android.net.Uri
import java.io.File

enum class CameraMode(val titleEn: String, val titleAr: String) {
    PHOTO("Photo", "صورة"),
    VIDEO("Video", "فيديو"),
    PORTRAIT("Portrait", "بورتريه"),
    NIGHT("Night", "ليلي"),
    PRO("Pro", "احترافي"),
    DOCUMENT("Document", "مستندات")
}

enum class FlashMode(val titleEn: String, val titleAr: String) {
    OFF("Off", "إيقاف"),
    AUTO("Auto", "تلقائي"),
    ON("On", "تشغيل"),
    TORCH("Torch", "إضاءة")
}

enum class AspectRatioOption(val titleEn: String, val ratio: Float, val label: String) {
    RATIO_4_3("4:3", 4f / 3f, "4:3"),
    RATIO_16_9("16:9", 16f / 9f, "16:9"),
    RATIO_1_1("1:1", 1f, "1:1"),
    RATIO_FULL("Full", 0f, "FULL")
}

enum class TimerOption(val seconds: Int, val label: String) {
    OFF(0, "OFF"),
    SEC_3(3, "3s"),
    SEC_5(5, "5s"),
    SEC_10(10, "10s")
}

enum class SteadyProLevel(val titleEn: String, val titleAr: String) {
    OFF("Off", "إيقاف"),
    STANDARD("Standard", "عادي"),
    ENHANCED("Enhanced", "محسّن"),
    ULTRA_STEADY("Ultra Steady", "فائق")
}

enum class ColorStudioPreset(val titleEn: String, val titleAr: String) {
    NATURAL("Natural", "طبيعي"),
    VIVID("Vivid", "حيوي"),
    WARM("Warm", "دافئ"),
    COOL("Cool", "بارد"),
    SOFT("Soft", "ناعم"),
    CINEMATIC("Cinematic", "سينمائي"),
    HIGH_CONTRAST("High Contrast", "تباين مرتفع"),
    CUSTOM("Custom", "مخصص")
}

data class ColorStudioConfig(
    val preset: ColorStudioPreset = ColorStudioPreset.NATURAL,
    val tone: Float = 0f,          // -100f to +100f
    val warmth: Float = 0f,        // -100f to +100f
    val intensity: Float = 50f,    // 0f to 100f (strength of preset effect)
    val contrast: Float = 0f,      // -50f to +50f
    val highlights: Float = 0f,    // -50f to +50f
    val shadows: Float = 0f,       // -50f to +50f
    val brightness: Float = 0f     // -50f to +50f
) {
    companion object {
        fun defaultForPreset(preset: ColorStudioPreset): ColorStudioConfig {
            return when (preset) {
                ColorStudioPreset.NATURAL -> ColorStudioConfig(preset = preset)
                ColorStudioPreset.VIVID -> ColorStudioConfig(
                    preset = preset,
                    tone = 10f,
                    warmth = 5f,
                    intensity = 70f,
                    contrast = 15f,
                    highlights = 10f,
                    shadows = -5f,
                    brightness = 5f
                )
                ColorStudioPreset.WARM -> ColorStudioConfig(
                    preset = preset,
                    warmth = 35f,
                    tone = 5f,
                    intensity = 65f,
                    contrast = 5f,
                    highlights = 5f,
                    shadows = 10f,
                    brightness = 0f
                )
                ColorStudioPreset.COOL -> ColorStudioConfig(
                    preset = preset,
                    warmth = -35f,
                    tone = -5f,
                    intensity = 65f,
                    contrast = 10f,
                    highlights = 10f,
                    shadows = -5f,
                    brightness = 0f
                )
                ColorStudioPreset.SOFT -> ColorStudioConfig(
                    preset = preset,
                    tone = 0f,
                    warmth = 10f,
                    intensity = 45f,
                    contrast = -20f,
                    highlights = -10f,
                    shadows = 20f,
                    brightness = 10f
                )
                ColorStudioPreset.CINEMATIC -> ColorStudioConfig(
                    preset = preset,
                    tone = -15f,
                    warmth = 15f,
                    intensity = 75f,
                    contrast = 25f,
                    highlights = -15f,
                    shadows = -10f,
                    brightness = -5f
                )
                ColorStudioPreset.HIGH_CONTRAST -> ColorStudioConfig(
                    preset = preset,
                    tone = 0f,
                    warmth = 0f,
                    intensity = 80f,
                    contrast = 40f,
                    highlights = 20f,
                    shadows = -25f,
                    brightness = 0f
                )
                ColorStudioPreset.CUSTOM -> ColorStudioConfig(preset = preset)
            }
        }
    }
}

enum class CameraFilter(val titleEn: String, val titleAr: String) {
    NONE("Natural", "طبيعي"),
    MONO("B&W", "أبيض وأسود"),
    VINTAGE("Vintage", "كلاسيكي"),
    CINEMATIC("Cinematic", "سينمائي"),
    WARM_GLOW("Warm Glow", "وهج دافئ"),
    COOL_BREEZE("Cool Breeze", "نسيم بارد"),
    SOFT("Soft", "ناعم"),
    RICH("Rich Colors", "ألوان غنية"),
    RETRO_FILM("Retro Film", "فيلم كلاسيكي")
}

data class AiEnhanceConfig(
    val enabled: Boolean = false, // Must be OFF by default per prompt
    val noiseReduction: Boolean = true,
    val clarityEnhance: Boolean = true,
    val hdrDynamicRange: Boolean = true,
    val nightBoost: Boolean = true
)

data class SuperNightConfig(
    val enabled: Boolean = false, // Must be OFF by default per prompt
    val holdWarning: Boolean = true
)

data class ProControlsState(
    val evBias: Int = 0,
    val iso: String = "AUTO",
    val shutter: String = "AUTO",
    val whiteBalance: String = "AUTO",
    val manualFocus: Float = -1f, // -1f = auto focus
    val showHistogram: Boolean = false,
    val showFocusPeaking: Boolean = false,
    val showZebra: Boolean = false,
    val showGrid: Boolean = false,
    val showLevel: Boolean = false
)

data class MediaItem(
    val id: String,
    val file: File,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L
)

data class CameraCapabilities(
    val hasBackCamera: Boolean = true,
    val hasFrontCamera: Boolean = true,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val hasFlash: Boolean = true,
    val hasTorch: Boolean = true,
    val supportedZoomSteps: List<Float> = listOf(1.0f, 2.0f, 5.0f),
    val supportsOpticalStabilization: Boolean = false,
    val supportsVideoStabilization: Boolean = false,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val exposureStep: Float = 0.5f,
    val hardwareLevel: String = "FULL",
    val hasUltraWideLens: Boolean = false
)
