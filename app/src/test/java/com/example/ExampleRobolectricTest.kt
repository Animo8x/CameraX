package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.camerax.model.CameraMode
import com.example.camerax.model.ColorStudioConfig
import com.example.camerax.model.ColorStudioPreset
import com.example.camerax.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CameraX", appName)
    }

    @Test
    fun `settings repository default values adhere to specifications`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)
        val prefs = repo.preferences.value

        // Specifications rule: Super night, AI enhance, Extended zoom must be OFF by default
        assertFalse(prefs.superNightEnabled)
        assertFalse(prefs.aiEnhanceEnabled)
        assertFalse(prefs.extendedZoom100x)
        assertEquals(ColorStudioPreset.NATURAL, prefs.colorStudioConfig.preset)
    }

    @Test
    fun `color studio presets generate valid configs`() {
        val vivid = ColorStudioConfig.defaultForPreset(ColorStudioPreset.VIVID)
        assertNotNull(vivid)
        assertEquals(ColorStudioPreset.VIVID, vivid.preset)

        val cinematic = ColorStudioConfig.defaultForPreset(ColorStudioPreset.CINEMATIC)
        assertEquals(ColorStudioPreset.CINEMATIC, cinematic.preset)
    }
}
