package com.duodeck.launcher.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Original, royalty-free home-menu loops generated on the device the first time they are needed
 * (a few chords of soft plucks over a pad). Nothing is downloaded and no third-party audio is bundled.
 */
object MusicSynth {
    val names = listOf("Plaza", "Sparkle", "Night Light")
    val blurbs = listOf("Warm and mellow, like a sunny lobby", "Bright and bouncy bell tones", "Slow, sleepy evening mood")

    private const val SR = 22050

    private class Track(val bpm: Int, val chords: List<IntArray>, val pattern: IntArray, val octave: Int)

    private val tracks = listOf(
        Track(86, listOf(
            intArrayOf(60, 64, 67, 71), intArrayOf(57, 60, 64, 67), intArrayOf(53, 57, 60, 64), intArrayOf(55, 59, 62, 64),
            intArrayOf(52, 55, 59, 62), intArrayOf(57, 60, 64, 67), intArrayOf(50, 53, 57, 60), intArrayOf(55, 59, 62, 65),
        ), intArrayOf(0, 1, 2, 3, 2, 1, 2, 1), 12),
        Track(104, listOf(
            intArrayOf(62, 66, 69, 73), intArrayOf(59, 62, 66, 69), intArrayOf(55, 59, 62, 66), intArrayOf(57, 61, 64, 66),
            intArrayOf(62, 66, 69, 73), intArrayOf(54, 57, 61, 64), intArrayOf(55, 59, 62, 66), intArrayOf(57, 62, 64, 67),
        ), intArrayOf(0, 2, 3, 2, 1, 3, 2, 3), 12),
        Track(70, listOf(
            intArrayOf(57, 60, 64, 67), intArrayOf(53, 57, 60, 64), intArrayOf(48, 52, 55, 59), intArrayOf(55, 59, 62, 64),
            intArrayOf(57, 60, 64, 71), intArrayOf(50, 53, 57, 60), intArrayOf(52, 55, 59, 62), intArrayOf(53, 57, 60, 64),
        ), intArrayOf(0, 1, 2, 1, 3, 2, 1, 2), 12),
    )

    fun ensure(ctx: Context, index: Int): File {
        val i = index.coerceIn(0, tracks.lastIndex)
        val f = File(ctx.cacheDir, "duodeck_home_v1_$i.wav")
        if (!f.exists() || f.length() < 10_000) writeWav(f, render(tracks[i]))
        return f
    }

    private fun render(t: Track): ShortArray {
        val beat = 60.0 / t.bpm
        val total = (t.chords.size * 4 * beat * SR).toInt()
        val buf = FloatArray(total + SR * 4)

        fun note(start: Double, midi: Int, dur: Double, amp: Float, pad: Boolean) {
            val freq = 440.0 * 2.0.pow((midi - 69) / 12.0)
            val s0 = (start * SR).toInt()
            val n = min((dur * SR).toInt(), buf.size - s0)
            val w = 2.0 * PI * freq / SR
            for (i in 0 until n) {
                val tt = i.toDouble() / SR
                val ph = w * i
                val v: Double
                val env: Double
                if (pad) {
                    env = min(1.0, tt / 0.7) * min(1.0, max(0.0, (dur - tt) / 0.9))
                    v = sin(ph) + 0.25 * sin(2 * ph)
                } else {
                    env = min(1.0, tt / 0.006) * exp(-tt * 3.2)
                    v = sin(ph) + 0.35 * sin(2 * ph) + 0.12 * sin(3 * ph)
                }
                buf[s0 + i] += (v * env * amp).toFloat()
            }
        }

        for ((ci, chord) in t.chords.withIndex()) {
            val bar = ci * 4 * beat
            chord.forEach { note(bar, it - 12, 4 * beat * 1.04, 0.05f, true) }
            note(bar, chord[0] - 12, 1.6, 0.13f, false)
            note(bar + 2 * beat, chord[0] - 12, 1.4, 0.10f, false)
            for (k in 0 until 8) {
                val idx = t.pattern[k % t.pattern.size] % chord.size
                note(bar + k * beat / 2, chord[idx] + t.octave, 1.2, 0.11f, false)
            }
        }

        // Fold the ring-out tail back onto the start so the loop has no gap or click.
        for (i in total until buf.size) {
            val j = i - total
            if (j < total) buf[j] += buf[i]
        }
        var peak = 0f
        for (i in 0 until total) peak = max(peak, abs(buf[i]))
        val scale = if (peak > 0f) 0.8f / peak else 1f
        return ShortArray(total) { (buf[it] * scale * 32767f).toInt().coerceIn(-32768, 32767).toShort() }
    }

    private fun writeWav(f: File, pcm: ShortArray) {
        val dataLen = pcm.size * 2
        val bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()).putInt(36 + dataLen).put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(SR).putInt(SR * 2).putShort(2).putShort(16)
        bb.put("data".toByteArray()).putInt(dataLen)
        pcm.forEach { bb.putShort(it) }
        f.writeBytes(bb.array())
    }
}

/** Plays the home-menu loop while DuoDeck is on screen. */
class MusicManager(private val ctx: Context) {
    private var player: MediaPlayer? = null
    private var key = ""
    private var wantPlay = false
    private var volume = 0.5f
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun configure(s: Settings) {
        volume = s.musicVolume
        runCatching { player?.setVolume(volume, volume) }
        if (s.musicMode == MusicMode.OFF) {
            key = "off"; stop(); return
        }
        val newKey = "${s.musicMode}|${s.musicTrack}|${s.musicUri}"
        if (newKey == key) return
        key = newKey
        stop()
        job = scope.launch {
            val source: Any? = when (s.musicMode) {
                MusicMode.BUILTIN -> withContext(Dispatchers.Default) { MusicSynth.ensure(ctx, s.musicTrack) }
                MusicMode.CUSTOM -> s.musicUri?.let { Uri.parse(it) }
                MusicMode.OFF -> null
            } ?: return@launch
            val mp = MediaPlayer()
            try {
                if (source is File) mp.setDataSource(source.absolutePath) else mp.setDataSource(ctx, source as Uri)
                mp.setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                mp.isLooping = true
                mp.setVolume(volume, volume)
                mp.setOnPreparedListener { if (wantPlay) it.start() }
                mp.prepareAsync()
                player = mp
            } catch (_: Exception) {
                mp.release()
            }
        }
    }

    fun resume() {
        wantPlay = true
        runCatching { player?.takeIf { !it.isPlaying }?.start() }
    }

    fun pause() {
        wantPlay = false
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    private fun stop() {
        job?.cancel()
        runCatching { player?.release() }
        player = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
