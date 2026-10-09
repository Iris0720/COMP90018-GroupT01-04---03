package com.example.comp90018.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

// Handles step counter sensor tracking.
class StepTracker(context: Context) {

    private val applicationContext = context.applicationContext

    private val sensorManager =
        applicationContext.getSystemService(Context.SENSOR_SERVICE)
                as SensorManager

    private var activeListener: SensorEventListener? = null

    // Android versions before Q do not require this permission.
    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(
        onStepCount: (Long) -> Unit,
        onStatus: (String) -> Unit
    ) {
        if (activeListener != null) return

        if (!hasPermission()) {
            onStatus("Physical activity permission is required.")
            return
        }

        val sensor = sensorManager.getDefaultSensor(
            Sensor.TYPE_STEP_COUNTER
        )

        if (sensor == null) {
            onStatus("Step counter is unavailable on this device.")
            return
        }

        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                // Ignore callbacks from a listener that has been stopped.
                if (activeListener !== this) return

                if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) {
                    return
                }

                val value = event.values.firstOrNull() ?: return

                if (!value.isFinite() || value < 0f) return

                // This is the sensor's cumulative total,
                // not the number of steps in this activity.
                onStatus("Step tracking connected")
                onStepCount(value.toLong())
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int
            ) {
                // No action needed here.
            }
        }

        activeListener = listener

        try {
            val registered = sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                Handler(Looper.getMainLooper())
            )

            if (registered) {
                onStatus("Waiting for step data...")
            } else {
                stop()
                onStatus("Unable to start step tracking.")
            }
        } catch (error: SecurityException) {
            stop()
            onStatus("Physical activity permission is required.")
        }
    }

    fun stop() {
        val listener = activeListener ?: return

        activeListener = null
        sensorManager.unregisterListener(listener)
    }
}