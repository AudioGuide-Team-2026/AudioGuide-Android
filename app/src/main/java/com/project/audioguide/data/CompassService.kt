package com.project.audioguide.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class CompassService(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val tag = "CompassService"
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var onAzimuthChanged: ((Float) -> Unit)? = null

    fun isAvailable(): Boolean = rotationVectorSensor != null

    fun start(onAzimuthChanged: (Float) -> Unit) {
        if (rotationVectorSensor == null) return
        this.onAzimuthChanged = onAzimuthChanged
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onAzimuthChanged = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        var azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (azimuthDegrees < 0f) azimuthDegrees += 360f
        onAzimuthChanged?.invoke(azimuthDegrees)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}