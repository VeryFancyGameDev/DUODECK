package com.duodeck.launcher.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

/** Smoothed phone tilt, each axis roughly -1..1 (Zero when 3D tilt is off). Read it inside draw/graphicsLayer lambdas. */
val LocalTilt = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

@Composable
fun TiltProvider(enabled: Boolean, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    val target = remember { floatArrayOf(0f, 0f) }
    val hasSensor = remember { mutableStateOf(false) }

    DisposableEffect(enabled) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // gravity on x/y: leaning the phone left/right and forward/back. 0.45 on y = comfortable holding angle.
                target[0] = (-e.values[0] / 6f).coerceIn(-1f, 1f)
                target[1] = (e.values[1] / 6f - 0.8f).coerceIn(-1f, 1f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (enabled && sm != null && acc != null) {
            sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
            hasSensor.value = true
        }
        onDispose { sm?.unregisterListener(l); hasSensor.value = false }
    }

    LaunchedEffect(enabled) {
        if (!enabled) { tilt.value = Offset.Zero; return@LaunchedEffect }
        var cur = Offset.Zero
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                val s = t / 1_000_000_000f
                // with no sensor (or a flat table) the tiles still sway gently so they read as 3D
                val tx = if (hasSensor.value) target[0] else kotlin.math.sin(s * 0.8f) * 0.5f
                val ty = if (hasSensor.value) target[1] else kotlin.math.cos(s * 0.6f) * 0.3f
                cur = Offset(cur.x + (tx - cur.x) * 0.12f, cur.y + (ty - cur.y) * 0.12f)
                tilt.value = cur
            }
        }
    }
    CompositionLocalProvider(LocalTilt provides tilt, content = content)
}
