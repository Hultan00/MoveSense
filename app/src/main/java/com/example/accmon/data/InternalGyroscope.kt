package com.example.accmon.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


class InternalGyroscope(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var isListening: Boolean = false

    private var onSensorChangedCallback: ((Float, Float, Float, Long) -> Unit)? = null

    init {
        initializeSensor()
    }

    private fun initializeSensor() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    fun startListening() {
        if (isListening) return

        gyroscopeSensor?.let {
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
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Get the current LocalDateTime
            val localDateTime = LocalDateTime.now()

            // Set the epoch date to 2000-01-01
            val epochDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0)

            // Calculate the nanoseconds since the epoch
            val nanoseconds = ChronoUnit.NANOS.between(epochDateTime, localDateTime)

            // Convert radians per second to degrees per second
            val degreesX = x * (180.0 / Math.PI)
            val degreesY = y * (180.0 / Math.PI)
            val degreesZ = z * (180.0 / Math.PI)

            // Notify the callback with the gyroscope values and time
            onSensorChangedCallback?.invoke(degreesX.toFloat(), degreesY.toFloat(), degreesZ.toFloat(), nanoseconds)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}
