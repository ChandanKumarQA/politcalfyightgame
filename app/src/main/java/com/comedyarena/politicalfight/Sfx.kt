package com.comedyarena.politicalfight

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.abs

/** Procedurally generated sound effects + looping background music (no audio files needed). */
object Sfx {
    var soundOn = true
    var musicOn = true
    private const val SR = 22050
    private var music: AudioTrack? = null
    private val pool = Executors.newCachedThreadPool()

    @Suppress("DEPRECATION")
    private fun track(d: ShortArray): AudioTrack {
        val t = AudioTrack(AudioManager.STREAM_MUSIC, SR, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, d.size * 2, AudioTrack.MODE_STATIC)
        t.write(d, 0, d.size)
        return t
    }

    private fun synth(freqs: IntArray, ms: Int, vol: Float, wave: Int): ShortArray {
        val n = SR * ms / 1000
        val out = ShortArray(n * freqs.size)
        var idx = 0
        for (f in freqs) for (i in 0 until n) {
            val ph = (i.toDouble() * f / SR) % 1.0
            val s = when (wave) { 0 -> if (ph < 0.5) 1.0 else -1.0; 1 -> 4 * abs(ph - 0.5) - 1; else -> Math.random() * 2 - 1 }
            val env = 1.0 - i.toDouble() / n
            out[idx++] = (s * env * vol * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun play(freqs: IntArray, ms: Int = 70, vol: Float = 0.35f, wave: Int = 0) {
        if (!soundOn) return
        pool.execute {
            try {
                val d = synth(freqs, ms, vol, wave)
                val t = track(d)
                t.play()
                Thread.sleep(d.size * 1000L / SR + 80)
                t.release()
            } catch (e: Exception) { }
        }
    }

    fun click() = play(intArrayOf(700, 900), 40, 0.25f, 1)
    fun punch() = play(intArrayOf(300, 200, 120), 45, 0.4f, 2)
    fun kick() = play(intArrayOf(220, 150, 100, 80), 45, 0.45f, 2)
    fun block() = play(intArrayOf(900, 700), 30, 0.25f, 0)
    fun special() = play(intArrayOf(400, 500, 650, 800, 1000), 60, 0.3f, 1)
    fun win() = play(intArrayOf(523, 659, 784, 1047), 150, 0.3f, 1)
    fun lose() = play(intArrayOf(330, 280, 230, 160), 180, 0.3f, 1)
    fun fight() = play(intArrayOf(500, 700), 120, 0.3f, 0)

    fun startMusic() {
        stopMusic()
        if (!musicOn) return
        try {
            val mel = intArrayOf(392, 440, 523, 587, 659, 587, 523, 440, 392, 523, 659, 784, 659, 587, 523, 392)
            val bass = intArrayOf(98, 98, 131, 98, 110, 110, 147, 110, 98, 98, 131, 98, 147, 147, 131, 98)
            val n = SR * 220 / 1000
            val d = ShortArray(n * mel.size)
            var idx = 0
            for (k in mel.indices) for (i in 0 until n) {
                val p1 = (i.toDouble() * mel[k] / SR) % 1.0
                val p2 = (i.toDouble() * bass[k] / SR) % 1.0
                val tri = 4 * abs(p1 - 0.5) - 1
                val sq = if (p2 < 0.5) 1.0 else -1.0
                var s = tri * 0.5 + sq * 0.22
                if (k % 2 == 0 && i < n / 8) s += (Math.random() * 2 - 1) * (1.0 - i.toDouble() / (n / 8)) * 0.5
                d[idx++] = (s.coerceIn(-1.0, 1.0) * 0.18 * Short.MAX_VALUE).toInt().toShort()
            }
            val t = track(d)
            t.setLoopPoints(0, d.size, -1)
            t.play()
            music = t
        } catch (e: Exception) { }
    }

    fun stopMusic() {
        try { music?.stop(); music?.release() } catch (e: Exception) { }
        music = null
    }
}
