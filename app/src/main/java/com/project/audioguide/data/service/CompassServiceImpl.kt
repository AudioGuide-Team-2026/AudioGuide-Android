package com.project.audioguide.data.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.project.audioguide.domain.service.CompassService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class CompassServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CompassService {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    override fun getUserAzimuth(): Flow<Float> = callbackFlow {
        val sensorCallback = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    val orientation = FloatArray(3)

                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuth = (azimuth + 360) % 360

                    trySend(azimuth)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationVectorSensor?.also { sensor ->
            sensorManager.registerListener(sensorCallback, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(sensorCallback)
        }
    }
}
