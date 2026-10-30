package com.duodeck.launcher.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Reads the phone's hardware step counter (total steps since boot). */
object Steps {
    fun available(ctx: Context): Boolean =
        (ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    suspend fun read(ctx: Context): Long? {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(3000) {
            suspendCancellableCoroutine { cont ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) {
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resume(e.values[0].toLong())
                    }
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                }
                sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(l) }
            }
        }
    }

    /** 100 steps = 1 Play Coin, up to [DAILY_CAP] coins a day. */
    const val STEPS_PER_COIN = 100
    const val DAILY_CAP = 10
}
