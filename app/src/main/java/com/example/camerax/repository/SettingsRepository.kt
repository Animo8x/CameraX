package com.example.camerax.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.camerax.model.AspectRatioOption
import com.example.camerax.model.CameraFilter
import com.example.camerax.model.ColorStudioConfig
import com.example.camerax.model.ColorStudioPreset
import com.example.camerax.model.SteadyProLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppPreferences(
    val extendedZoom100x: Boolean = false,
    val aiEnhanceEnabled: Boolean = false,
    val superNightEnabled: Boolean = false,
    val steadyProLevel: SteadyProLevel = SteadyProLevel.OFF,
    val selectedFilter: CameraFilter = CameraFilter.NONE,
    val colorStudioConfig: ColorStudioConfig = ColorStudioConfig(),
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val saveLocation: Boolean = false,
    val shutterSound: Boolean = true,
    val highQualityJpeg: Boolean = true,
    val videoResolution: String = "1080p",
    val videoFps: Int = 30,
    val darkMode: Boolean = true,
    val showGrid: Boolean = false,
    val showLevel: Boolean = false,
    val showHistogram: Boolean = false,
    val showFocusPeaking: Boolean = false
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("camerax_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): AppPreferences {
        return AppPreferences(
            extendedZoom100x = prefs.getBoolean("extended_zoom_100x", false),
            aiEnhanceEnabled = prefs.getBoolean("ai_enhance_enabled", false),
            superNightEnabled = prefs.getBoolean("super_night_enabled", false),
            steadyProLevel = SteadyProLevel.valueOf(
                prefs.getString("steady_pro_level", SteadyProLevel.OFF.name) ?: SteadyProLevel.OFF.name
            ),
            selectedFilter = CameraFilter.valueOf(
                prefs.getString("selected_filter", CameraFilter.NONE.name) ?: CameraFilter.NONE.name
            ),
            colorStudioConfig = ColorStudioConfig(
                preset = ColorStudioPreset.valueOf(
                    prefs.getString("studio_preset", ColorStudioPreset.NATURAL.name) ?: ColorStudioPreset.NATURAL.name
                ),
                tone = prefs.getFloat("studio_tone", 0f),
                warmth = prefs.getFloat("studio_warmth", 0f),
                intensity = prefs.getFloat("studio_intensity", 50f),
                contrast = prefs.getFloat("studio_contrast", 0f),
                highlights = prefs.getFloat("studio_highlights", 0f),
                shadows = prefs.getFloat("studio_shadows", 0f),
                brightness = prefs.getFloat("studio_brightness", 0f)
            ),
            aspectRatio = AspectRatioOption.valueOf(
                prefs.getString("aspect_ratio", AspectRatioOption.RATIO_4_3.name) ?: AspectRatioOption.RATIO_4_3.name
            ),
            saveLocation = prefs.getBoolean("save_location", false),
            shutterSound = prefs.getBoolean("shutter_sound", true),
            highQualityJpeg = prefs.getBoolean("high_quality_jpeg", true),
            videoResolution = prefs.getString("video_resolution", "1080p") ?: "1080p",
            videoFps = prefs.getInt("video_fps", 30),
            darkMode = prefs.getBoolean("dark_mode", true),
            showGrid = prefs.getBoolean("show_grid", false),
            showLevel = prefs.getBoolean("show_level", false),
            showHistogram = prefs.getBoolean("show_histogram", false),
            showFocusPeaking = prefs.getBoolean("show_focus_peaking", false)
        )
    }

    fun updateExtendedZoom(enabled: Boolean) {
        prefs.edit().putBoolean("extended_zoom_100x", enabled).apply()
        _preferences.value = _preferences.value.copy(extendedZoom100x = enabled)
    }

    fun updateAiEnhance(enabled: Boolean) {
        prefs.edit().putBoolean("ai_enhance_enabled", enabled).apply()
        _preferences.value = _preferences.value.copy(aiEnhanceEnabled = enabled)
    }

    fun updateSuperNight(enabled: Boolean) {
        prefs.edit().putBoolean("super_night_enabled", enabled).apply()
        _preferences.value = _preferences.value.copy(superNightEnabled = enabled)
    }

    fun updateSteadyPro(level: SteadyProLevel) {
        prefs.edit().putString("steady_pro_level", level.name).apply()
        _preferences.value = _preferences.value.copy(steadyProLevel = level)
    }

    fun updateFilter(filter: CameraFilter) {
        prefs.edit().putString("selected_filter", filter.name).apply()
        _preferences.value = _preferences.value.copy(selectedFilter = filter)
    }

    fun updateColorStudio(config: ColorStudioConfig) {
        prefs.edit()
            .putString("studio_preset", config.preset.name)
            .putFloat("studio_tone", config.tone)
            .putFloat("studio_warmth", config.warmth)
            .putFloat("studio_intensity", config.intensity)
            .putFloat("studio_contrast", config.contrast)
            .putFloat("studio_highlights", config.highlights)
            .putFloat("studio_shadows", config.shadows)
            .putFloat("studio_brightness", config.brightness)
            .apply()
        _preferences.value = _preferences.value.copy(colorStudioConfig = config)
    }

    fun updateAspectRatio(ratio: AspectRatioOption) {
        prefs.edit().putString("aspect_ratio", ratio.name).apply()
        _preferences.value = _preferences.value.copy(aspectRatio = ratio)
    }

    fun updateSaveLocation(enabled: Boolean) {
        prefs.edit().putBoolean("save_location", enabled).apply()
        _preferences.value = _preferences.value.copy(saveLocation = enabled)
    }

    fun updateShutterSound(enabled: Boolean) {
        prefs.edit().putBoolean("shutter_sound", enabled).apply()
        _preferences.value = _preferences.value.copy(shutterSound = enabled)
    }

    fun updateHighQualityJpeg(enabled: Boolean) {
        prefs.edit().putBoolean("high_quality_jpeg", enabled).apply()
        _preferences.value = _preferences.value.copy(highQualityJpeg = enabled)
    }

    fun updateVideoResolution(res: String) {
        prefs.edit().putString("video_resolution", res).apply()
        _preferences.value = _preferences.value.copy(videoResolution = res)
    }

    fun updateVideoFps(fps: Int) {
        prefs.edit().putInt("video_fps", fps).apply()
        _preferences.value = _preferences.value.copy(videoFps = fps)
    }

    fun updateDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
        _preferences.value = _preferences.value.copy(darkMode = dark)
    }

    fun updateShowGrid(show: Boolean) {
        prefs.edit().putBoolean("show_grid", show).apply()
        _preferences.value = _preferences.value.copy(showGrid = show)
    }

    fun updateShowLevel(show: Boolean) {
        prefs.edit().putBoolean("show_level", show).apply()
        _preferences.value = _preferences.value.copy(showLevel = show)
    }

    fun updateShowHistogram(show: Boolean) {
        prefs.edit().putBoolean("show_histogram", show).apply()
        _preferences.value = _preferences.value.copy(showHistogram = show)
    }

    fun updateShowFocusPeaking(show: Boolean) {
        prefs.edit().putBoolean("show_focus_peaking", show).apply()
        _preferences.value = _preferences.value.copy(showFocusPeaking = show)
    }

    fun resetAllToDefaults() {
        prefs.edit().clear().apply()
        _preferences.value = AppPreferences()
    }
}
