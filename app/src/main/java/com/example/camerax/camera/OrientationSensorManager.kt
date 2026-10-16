package com.example.camerax.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class DeviceOrientationAngles(
    val rollDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val isLevel: Boolean = false // within +/- 1.5 degrees of level
)

class OrientationSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _orientationAngles = MutableStateFlow(DeviceOrientationAngles())
    val orientationAngles: StateFlow<DeviceOrientationAngles> = _orientationAngles.asStateFlow()

    private var isListening = false

    fun start() {
        if (!isListening && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Calculate roll (rotation around Y/Z)
        val rollRad = atan2(-ax.toDouble(), ay.toDouble())
        val rollDeg = Math.toDegrees(rollRad).toFloat()

        // Normalize roll relative to portrait (0 deg) or landscape (90 / -90 deg)
        val normalizedRoll = when {
            rollDeg in -45f..45f -> rollDeg
            rollDeg in 45f..135f -> rollDeg - 90f
            rollDeg in -135f..-45f -> rollDeg + 90f
            else -> if (rollDeg > 0) rollDeg - 180f else rollDeg + 180f
        }

        val pitchRad = atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble()))
        val pitchDeg = Math.toDegrees(pitchRad).toFloat()

        val isLevel = kotlin.math.abs(normalizedRoll) < 1.5f

        _orientationAngles.value = DeviceOrientationAngles(
            rollDegrees = normalizedRoll,
            pitchDegrees = pitchDeg,
            isLevel = isLevel
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
