package com.kang.administrator.zhisuntestapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import com.kang.administrator.zhisuntestapplication.camera.CameraManager
import com.kang.administrator.zhisuntestapplication.camera.FrontLightMode

internal class AmbientLightManager(private val context: Context) : SensorEventListener {
    private var cameraManager: CameraManager? = null
    private var lightSensor: Sensor? = null

    fun start(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (FrontLightMode.readPref(sharedPrefs) === FrontLightMode.AUTO) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun stop() {
        if (lightSensor != null) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(this)
            cameraManager = null
            lightSensor = null
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val ambientLightLux = sensorEvent.values[0]
        if (cameraManager != null) {
            if (ambientLightLux <= TOO_DARK_LUX) {
                cameraManager!!.setTorch(true)
            } else if (ambientLightLux >= BRIGHT_ENOUGH_LUX) {
                cameraManager!!.setTorch(false)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // do nothing
    }

    companion object {

        private val TOO_DARK_LUX = 45.0f
        private val BRIGHT_ENOUGH_LUX = 450.0f
    }

}