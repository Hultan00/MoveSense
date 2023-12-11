package com.example.accmon.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class InternalAccelerometer(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null
    private var isListening: Boolean = false

    private var onSensorChangedCallback: ((Float, Float, Float, Long) -> Unit)? = null

    init {
        initializeSensor()
    }

    private fun initializeSensor() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startListening() {
        if (isListening) return

        accelerometerSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            isListening = true
        }
    }

    fun stopListening() {
        if (!isListening) return

        sensorManager?.unregisterListener(this)
        isListening = false
    }

    fun setOnSensorChangedCallback(callback: (Float, Float, Float, Long) -> Unit) {
        onSensorChangedCallback = callback
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Get the current LocalDateTime
            val localDateTime = LocalDateTime.now()

            // Set the epoch date to 2000-01-01
            val epochDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0)

            // Calculate the nanoseconds since the epoch
            val nanoseconds = ChronoUnit.NANOS.between(epochDateTime, localDateTime)

            // Notify the callback with the gyroscope values and time
            onSensorChangedCallback?.invoke(x, y, z, nanoseconds)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}
