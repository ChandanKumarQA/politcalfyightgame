package com.comedyarena.politicalfight.game3d

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.comedyarena.politicalfight.game3d.core.Sfx
import java.util.Random
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/** Pure-Kotlin synthesized sound: every effect is rendered to PCM at start-up and mixed on a worker thread. No audio files. */
class Audio {
    private val SR = 22050
    private val rnd = Random(7)
    private class Voice(val buf: ShortArray, val gain: Float) { var pos = 0 }
    private val voices = ArrayList<Voice>()
    private val fx = HashMap<String, ShortArray>()
    private val SA = floatArrayOf(146.83f, 155.56f, 185.0f, 196.0f, 220.0f, 233.08f, 277.18f, 293.66f, 369.99f)
    @Volatile var musicOn = true
    @Volatile private var running = false
    @Volatile private var paused = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    // ---------------- synthesis helpers (render into float buffers) ----------------
    private fun wave(type: Int, ph: Double): Float {
        val p = ph - Math.floor(ph)
        return when (type) {
            0 -> sin(p * 2 * PI).toFloat()
            1 -> if (p < 0.5) 1f else -1f
            2 -> (2 * p - 1).toFloat()
            else -> (if (p < 0.5) 4 * p - 1 else 3 - 4 * p).toFloat()
        }
    }
    /** type: 0 sine 1 square 2 saw 3 triangle */
    private fun tone(buf: FloatArray, t0: Float, f: Float, dur: Float, type: Int, vol: Float, slide: Float = 0f) {
        val s0 = (t0 * SR).toInt(); val n = (dur * SR).toInt(); var ph = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SR
            val fr = if (slide > 0f) f * (max(20f, slide) / f).pow(t / dur) else f
            ph += fr.toDouble() / SR
            val env = if (t < 0.008f) 0.0001f * (vol / 0.0001f).pow(t / 0.008f) else vol * (0.0001f / vol).pow((t - 0.008f) / (dur - 0.008f))
            val idx = s0 + i; if (idx >= buf.size) break
            buf[idx] += wave(type, ph) * env
        }
    }
    /** filter: 0 lowpass 1 highpass 2 bandpass */
    private fun noise(buf: FloatArray, t0: Float, dur: Float, vol: Float, freq: Float, filter: Int, q: Float = 0.7f) {
        val s0 = (t0 * SR).toInt(); val n = (dur * SR).toInt()
        val w0 = 2 * PI * min(freq, SR * 0.45f) / SR; val cw = kotlin.math.cos(w0); val alpha = sin(w0) / (2 * q)
        var b0: Double; var b1: Double; var b2: Double
        val a0 = 1 + alpha; val a1 = -2 * cw; val a2 = 1 - alpha
        when (filter) {
            0 -> { b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = (1 - cw) / 2 }
            1 -> { b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = (1 + cw) / 2 }
            else -> { b0 = alpha; b1 = 0.0; b2 = -alpha }
        }
        b0 /= a0; b1 /= a0; b2 /= a0; val na1 = a1 / a0; val na2 = a2 / a0
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        for (i in 0 until n) {
            val x = (rnd.nextFloat() * 2 - 1).toDouble()
            val y = b0 * x + b1 * x1 + b2 * x2 - na1 * y1 - na2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            val t = i.toFloat() / SR; val env = vol * (0.0001f / vol).pow(t / dur)
            val idx = s0 + i; if (idx >= buf.size) break
            buf[idx] += (y * env).toFloat()
        }
    }
    private fun bake(sec: Float, fn: (FloatArray) -> Unit): ShortArray {
        val b = FloatArray((sec * SR).toInt()); fn(b)
        return ShortArray(b.size) { (max(-1f, min(1f, b[it])) * 32000f).toInt().toShort() }
    }

    init {
        fx["hitL"] = bake(0.25f) { noise(it, 0f, 0.12f, 0.7f, 1500f, 0); tone(it, 0f, 190f, 0.12f, 1, 0.5f, 50f) }
        fx["hitH"] = bake(0.35f) { noise(it, 0f, 0.22f, 1.0f, 900f, 0); tone(it, 0f, 140f, 0.22f, 1, 0.5f, 50f) }
        fx["block"] = bake(0.2f) { noise(it, 0f, 0.08f, 0.5f, 2500f, 2); tone(it, 0f, 520f, 0.08f, 3, 0.3f, 300f) }
        fx["whoosh"] = bake(0.3f) { noise(it, 0f, 0.16f, 0.25f, 1800f, 2, 0.8f) }
        fx["jump"] = bake(0.3f) { tone(it, 0f, 260f, 0.14f, 0, 0.25f, 520f) }
        fx["land"] = bake(0.2f) { noise(it, 0f, 0.07f, 0.35f, 500f, 0) }
        fx["super"] = bake(0.7f) { tone(it, 0f, 200f, 0.5f, 2, 0.35f, 1200f); tone(it, 0.05f, 400f, 0.5f, 1, 0.18f, 1600f); noise(it, 0f, 0.5f, 0.4f, 4000f, 1) }
        fx["ready"] = bake(0.4f) { tone(it, 0f, 660f, 0.12f, 3, 0.35f); tone(it, 0.1f, 880f, 0.18f, 3, 0.35f) }
        fx["boom"] = bake(0.7f) { noise(it, 0f, 0.5f, 1.0f, 700f, 0); tone(it, 0f, 120f, 0.5f, 0, 0.9f, 30f) }
        fx["ko"] = bake(1.2f) { noise(it, 0f, 0.7f, 1.0f, 600f, 0); tone(it, 0f, 200f, 0.8f, 2, 0.5f, 40f); tone(it, 0.4f, 90f, 0.6f, 0, 0.8f, 30f) }
        fx["bell"] = bake(1.0f) { tone(it, 0f, 880f, 0.9f, 0, 0.35f); tone(it, 0f, 1320f, 0.7f, 0, 0.2f) }
        fx["ui"] = bake(0.15f) { tone(it, 0f, 720f, 0.07f, 3, 0.25f, 960f) }
        fx["win"] = bake(0.9f) { b -> floatArrayOf(523f, 659f, 784f, 1046f).forEachIndexed { i, f -> tone(b, i * 0.13f, f, 0.3f, 3, 0.35f) } }
        fx["lose"] = bake(1.0f) { b -> floatArrayOf(392f, 330f, 262f, 196f).forEachIndexed { i, f -> tone(b, i * 0.18f, f, 0.35f, 2, 0.22f) } }
        // music pieces
        fx["m_kick"] = bake(0.22f) { tone(it, 0f, 150f, 0.18f, 0, 0.9f, 45f) }
        fx["m_snare"] = bake(0.1f) { noise(it, 0f, 0.07f, 0.35f, 3500f, 2, 2f) }
        fx["m_hat"] = bake(0.06f) { noise(it, 0f, 0.03f, 0.12f, 7000f, 1) }
        for (i in SA.indices) {
            fx["m_b$i"] = bake(0.8f) { tone(it, 0f, SA[i] / 2, 0.74f, 3, 0.55f) }
            fx["m_l$i"] = bake(0.4f) { tone(it, 0f, SA[i], 0.35f, 2, 0.12f) }
        }
    }

    fun play(name: String, gain: Float = 1f) { val b = fx[name] ?: return; synchronized(voices) { if (voices.size < 24) voices.add(Voice(b, gain * 0.9f)) } }

    fun sfx(s: Sfx) = when (s) {
        Sfx.BELL -> play("bell"); Sfx.SUPER_READY -> play("ready"); Sfx.JUMP -> play("jump"); Sfx.LAND -> play("land")
        Sfx.WHOOSH -> play("whoosh"); Sfx.SUPER -> play("super"); Sfx.HIT_LIGHT -> play("hitL"); Sfx.HIT_HEAVY -> play("hitH")
        Sfx.BLOCK -> play("block"); Sfx.KO -> play("ko"); Sfx.BOOM -> play("boom")
    }
    fun ui() = play("ui"); fun win() = play("win"); fun lose() = play("lose")

    // ---------------- mixer thread ----------------
    fun start() {
        if (running) { paused = false; track?.play(); return }
        running = true; paused = false
        val min = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(max(min, 4096) * 2).setTransferMode(AudioTrack.MODE_STREAM).build()
        track = t; t.play()
        thread = Thread({ loop(t) }, "kc-audio").also { it.isDaemon = true; it.start() }
    }

    fun pause() { paused = true; try { track?.pause(); track?.flush() } catch (e: Exception) { } }
    fun resume() { if (!running) { start(); return }; paused = false; try { track?.play() } catch (e: Exception) { } }
    fun stop() { running = false; try { thread?.join(300) } catch (e: Exception) { }; try { track?.stop(); track?.release() } catch (e: Exception) { }; track = null; thread = null }

    private fun loop(t: AudioTrack) {
        val chunk = 512; val out = ShortArray(chunk); val acc = FloatArray(chunk)
        val spb = (60f / 138f / 2f * SR).toInt(); var next = 0; var step = 0
        val musicVoices = ArrayList<Voice>()
        while (running) {
            if (paused) { try { Thread.sleep(40) } catch (e: Exception) { }; continue }
            java.util.Arrays.fill(acc, 0f)
            // music scheduling: trigger at step boundaries inside this chunk (quantised to chunk start for simplicity)
            next -= chunk
            if (next <= 0) {
                next += spb
                if (musicOn) {
                    val s = step % 16
                    if ("x..x..x.x..x..x."[s] == 'x') musicVoices.add(Voice(fx["m_kick"]!!, 0.2f * 1.4f))
                    if (s % 4 == 2) musicVoices.add(Voice(fx["m_snare"]!!, 0.2f * 1.2f))
                    if (s % 2 == 1) musicVoices.add(Voice(fx["m_hat"]!!, 0.2f))
                    if (s % 4 == 0) { val bn = intArrayOf(0, 0, 3, 0, 0, 4, 3, 1)[(step shr 2) % 8]; musicVoices.add(Voice(fx["m_b$bn"]!!, 0.22f)) }
                    if ((s % 2 == 0 && rnd.nextFloat() < 0.55f) || s == 15) musicVoices.add(Voice(fx["m_l${intArrayOf(0, 2, 3, 4, 5, 6, 7, 8)[rnd.nextInt(8)]}"]!!, 0.2f))
                }
                step++
            }
            mix(musicVoices, acc); synchronized(voices) { mix(voices, acc) }
            for (i in 0 until chunk) out[i] = (max(-1f, min(1f, acc[i])) * 32767f).toInt().toShort()
            try { t.write(out, 0, chunk) } catch (e: Exception) { break }
        }
    }

    private fun mix(list: ArrayList<Voice>, acc: FloatArray) {
        var i = 0
        while (i < list.size) {
            val v = list[i]; val b = v.buf; var p = v.pos; val g = v.gain / 32768f
            var k = 0
            while (k < acc.size && p < b.size) { acc[k] += b[p] * g; k++; p++ }
            v.pos = p
            if (p >= b.size) list.removeAt(i) else i++
        }
    }
}
