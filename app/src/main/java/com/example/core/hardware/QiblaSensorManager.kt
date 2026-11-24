package com.example.core.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

/**
 * Observes device magnetic azimuth (0..360 degrees clockwise from North)
 * using TYPE_ROTATION_VECTOR (preferred) or accelerometer+magnetometer fallback.
 */
fun observeCompassAzimuth(context: Context): Flow<Float> = callbackFlow {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    if (sensorManager == null) {
        trySend(0f)
        awaitClose { }
        return@callbackFlow
    }

    val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val rotationMatrix = FloatArray(9)
    val orientation = FloatArray(3)
    val gravity = FloatArray(3)
    val geomagnetic = FloatArray(3)
    var hasGravity = false
    var hasMag = false
    var smoothed = 0f

    fun emitAzimuth(rawDeg: Float) {
        val normalized = (rawDeg + 360f) % 360f
        // Smooth low-pass filter handling 0/360 wrap
        var delta = normalized - smoothed
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        smoothed = (smoothed + 0.25f * delta + 360f) % 360f
        trySend((smoothed * 10f).roundToInt() / 10f)
    }

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    emitAzimuth(deg)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, 3)
                    hasGravity = true
                    if (hasMag && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        emitAzimuth(Math.toDegrees(orientation[0].toDouble()).toFloat())
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    hasMag = true
                    if (hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        emitAzimuth(Math.toDegrees(orientation[0].toDouble()).toFloat())
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    if (rotationSensor != null) {
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    } else if (accelSensor != null && magSensor != null) {
        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
    } else {
        trySend(0f)
    }

    awaitClose {
        sensorManager.unregisterListener(listener)
    }
}
