package com.comedyarena.politicalfight.game3d

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.comedyarena.politicalfight.game3d.core.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Native Canvas UI drawn over the GL surface: all screens, HUD, touch controls. Implements engine Events. */
class UiView(private val ctx: Context, private val audio: Audio) : View(ctx), Events {
    lateinit var engine: Engine
    var onMatchEnd: (() -> Unit)? = null

    private enum class Screen { TITLE, SELECT, LEVELS, FIGHT, PAUSE, RESULT }
    private enum class A { VS, LV, BACK, FIGHT, CARD, STAGE, CPU, TAB, CELL, LV_START, LV_BACK, PAUSE, RESUME, MUSIC, QUIT, NEXT, REMATCH, CHAR }
    private class Hit(val l: Float, val t: Float, val r: Float, val b: Float, val a: A, val arg: Int)
    private class PopFx(val text: String, val x: Float, val y: Float, val color: Int, val t0: Long)
    private class PadBtn(val key: Int, var cx: Float, var cy: Float, val r: Float, val label: String, val color: Int)

    private var screen = Screen.TITLE
    private var story = false
    private var sel1 = 0; private var sel2 = 3; private var phase = 0; private var stageSel = 0; private var cpuSel = 1
    private var lv = 1; private var selLv = 1; private var page = 0
    private var info: LevelInfo? = null; private var selInfo: LevelInfo? = null
    private val stars = IntArray(LEVEL_COUNT + 1); private var lastLevel = 1
    private val prefs = ctx.getSharedPreferences("netafight3d", Context.MODE_PRIVATE)
    private val face: Typeface = try { Typeface.createFromAsset(ctx.assets, "fonts/Bangers.ttf") } catch (e: Exception) { Typeface.DEFAULT_BOLD }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).also { it.typeface = face }
    private val rect = RectF(); private val path = Path()
    private val hits = ArrayList<Hit>(); private var pressed: Hit? = null
    private var u = 1f; private var DW = 640f; private var DH = 360f
    private val portraits = arrayOfNulls<Bitmap>(Roster.all.size)
    private val starPath = Path()

    // hud state
    private var g1 = 1f; private var g2 = 1f
    private @Volatile var bannerText = ""; private @Volatile var bannerStart = 0L; private @Volatile var bannerDur = 1.0f
    private @Volatile var resultAt = 0L; private @Volatile var resultWin = false
    private @Volatile var flashColor = 0; private @Volatile var flashAt = 0L
    private val pops = ConcurrentLinkedQueue<PopFx>()
    private var resStars = 0; private var resHead = ""
    private val pad = ArrayList<PadBtn>(); private val padHeld = BooleanArray(8)

    init {
        loadSave()
        for (i in 0 until 5) { val a = -Math.PI / 2 + i * 2 * Math.PI / 5; val b = a + Math.PI / 5
            val x1 = cos(a).toFloat(); val y1 = sin(a).toFloat(); val x2 = cos(b).toFloat() * 0.42f; val y2 = sin(b).toFloat() * 0.42f
            if (i == 0) starPath.moveTo(x1, y1) else starPath.lineTo(x1, y1); starPath.lineTo(x2, y2) }
        starPath.close()
        isFocusable = true
    }

    fun attach(e: Engine) {
        engine = e
        e.post { e.preview(sel1, sel2, 4.2f); e.previewWide = true; e.applyStage(0) }
    }

    // ------------------------------------------------------------ persistence
    private fun loadSave() {
        val s = prefs.getString("stars", "") ?: ""
        for (i in 1..LEVEL_COUNT) if (i - 1 < s.length) stars[i] = (s[i - 1] - '0').coerceIn(0, 3)
        lastLevel = prefs.getInt("last", 1).coerceIn(1, LEVEL_COUNT)
    }
    private fun writeSave() {
        val sb = StringBuilder(); for (i in 1..LEVEL_COUNT) sb.append(('0' + stars[i]))
        prefs.edit().putString("stars", sb.toString()).putInt("last", lastLevel).apply()
    }

    // ------------------------------------------------------------ Events (called from the GL thread)
    private val now get() = System.nanoTime()
    override fun onRound(n: Int) { bannerText = "ROUND $n"; bannerDur = 1.05f; bannerStart = now }
    override fun onBanner(text: String) { bannerText = text; bannerDur = 3f; bannerStart = now }
    override fun onResult(p1Won: Boolean) { resultWin = p1Won; resultAt = now + 1_200_000_000L }
    override fun pop(text: String, sx: Float, sy: Float, color: Int) { pops.add(PopFx(text, sx, sy, color, now)); while (pops.size > 12) pops.poll() }
    override fun flash(color: Int) { flashColor = color; flashAt = now }
    override fun vibe(ms: Int) {
        try {
            val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") v.vibrate(ms.toLong())
        } catch (e: Exception) { }
    }
    override fun sfx(s: Sfx) = audio.sfx(s)

    // ------------------------------------------------------------ flow
    private fun goSelect(st: Boolean? = null) {
        if (st != null) story = st
        audio.ui(); val a = sel1; val b = sel2; val sg = stageSel
        engine.post { engine.preview(a, b, 2.7f); engine.previewWide = false; engine.applyStage(sg) }
        screen = Screen.SELECT
    }
    private fun goTitle() {
        story = false; engine.paused = false; val a = sel1; val b = sel2
        engine.post { engine.preview(a, b, 4.2f); engine.previewWide = true }
        screen = Screen.TITLE
    }
    private fun startFight() {
        if (story) { goLevels(); return }
        audio.ui(); engine.paused = false; hudInit(); screen = Screen.FIGHT
        val a = sel1; val b = sel2; val l = cpuSel.toFloat(); val s = stageSel
        engine.post { engine.startMatch(a, b, l, s) }
    }
    private fun hudInit() { g1 = 1f; g2 = 1f; bannerText = ""; resultAt = 0L; pops.clear() }
    private fun goLevels() {
        audio.ui(); engine.paused = false; story = true
        val a = sel1; val b = sel2
        engine.post { engine.preview(a, b, 4.2f); engine.previewWide = true }
        screen = Screen.LEVELS; selectLevel(lastLevel)
    }
    private fun selectLevel(n: Int) { selLv = n; page = (n - 1) / 50; selInfo = levelInfo(n) }
    private fun startLevel(n0: Int) {
        val n = n0.coerceIn(1, LEVEL_COUNT); story = true; lv = n; selLv = n; val inf = levelInfo(n); info = inf
        lastLevel = n; writeSave(); audio.ui(); engine.paused = false; hudInit(); screen = Screen.FIGHT
        val a = sel1; engine.post { engine.startLevel(inf, a) }
    }
    private fun pauseGame(on: Boolean) {
        if (screen != Screen.FIGHT && screen != Screen.PAUSE) return
        engine.paused = on; screen = if (on) Screen.PAUSE else Screen.FIGHT; audio.ui()
        if (on) { engine.clearInput(); java.util.Arrays.fill(padHeld, false) }
    }
    fun onHostPause() { if (screen == Screen.FIGHT) pauseGame(true) }

    /** @return true if consumed; false means the host should leave the app. */
    fun back(): Boolean {
        when (screen) {
            Screen.FIGHT -> { pauseGame(true); return true }
            Screen.PAUSE -> { pauseGame(false); return true }
            Screen.RESULT -> { if (story) goLevels() else goSelect(); return true }
            Screen.LEVELS -> { goSelect(true); return true }
            Screen.SELECT -> { goTitle(); return true }
            Screen.TITLE -> return false
        }
    }

    private fun pickCard(i: Int) {
        audio.ui()
        val idx = if (i >= Roster.all.size) (Math.random() * Roster.all.size).toInt() else i
        if (phase == 0 || story) { sel1 = idx; engine.post { engine.swapPreview(1, idx) } } else { sel2 = idx; engine.post { engine.swapPreview(2, idx) } }
        phase = if (story) 0 else 1 - phase
    }

    private fun act(h: Hit) {
        when (h.a) {
            A.VS -> goSelect(false)
            A.LV -> goSelect(true)
            A.BACK -> goTitle()
            A.FIGHT -> startFight()
            A.CARD -> pickCard(h.arg)
            A.STAGE -> { stageSel = h.arg; audio.ui(); engine.post { engine.applyStage(h.arg) } }
            A.CPU -> { cpuSel = h.arg; audio.ui() }
            A.TAB -> { page = h.arg; audio.ui() }
            A.CELL -> { if (h.arg == selLv) startLevel(h.arg) else { selectLevel(h.arg); audio.ui() } }
            A.LV_START -> startLevel(selLv)
            A.LV_BACK -> goSelect(true)
            A.PAUSE -> pauseGame(true)
            A.RESUME -> pauseGame(false)
            A.MUSIC -> { audio.musicOn = !audio.musicOn; audio.ui() }
            A.QUIT -> { engine.paused = false; if (story) goLevels() else goSelect() }
            A.NEXT -> startLevel(lv + 1)
            A.REMATCH -> { hudInit(); screen = Screen.FIGHT; engine.post { engine.rematch() }; audio.ui() }
            A.CHAR -> { if (story) goLevels() else goSelect() }
        }
    }

    // ------------------------------------------------------------ input
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val a = e.actionMasked
        if (screen == Screen.FIGHT) {
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
                val i = e.actionIndex; val h = hitAt(e.getX(i) / u, e.getY(i) / u)
                if (h != null && h.a == A.PAUSE) { act(h); return true }
            }
            handlePad(e); return true
        }
        when (a) {
            MotionEvent.ACTION_DOWN -> pressed = hitAt(e.x / u, e.y / u)
            MotionEvent.ACTION_MOVE -> { val h = hitAt(e.x / u, e.y / u); if (h !== pressed) pressed = if (h != null && pressed != null && h.a == pressed!!.a && h.arg == pressed!!.arg) pressed else pressed }
            MotionEvent.ACTION_UP -> { val h = hitAt(e.x / u, e.y / u); val pr = pressed; pressed = null
                if (h != null && pr != null && h.a == pr.a && h.arg == pr.arg) act(h) }
            MotionEvent.ACTION_CANCEL -> pressed = null
        }
        return true
    }
    private fun hitAt(x: Float, y: Float): Hit? { for (i in hits.indices.reversed()) { val h = hits[i]; if (x >= h.l && x <= h.r && y >= h.t && y <= h.b) return h }; return null }

    private fun handlePad(e: MotionEvent) {
        val held = BooleanArray(8); val a = e.actionMasked
        if (a != MotionEvent.ACTION_CANCEL) for (i in 0 until e.pointerCount) {
            if ((a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_POINTER_UP) && i == e.actionIndex) continue
            val x = e.getX(i) / u; val y = e.getY(i) / u
            for (b in pad) { val dx = x - b.cx; val dy = y - b.cy; if (dx * dx + dy * dy <= b.r * b.r * 1.5f) held[b.key] = true }
        }
        setKeys(held)
    }
    private fun setKeys(h: BooleanArray) {
        engine.kUp = h[0]; engine.kDown = h[1]; engine.kLeft = h[2]; engine.kRight = h[3]
        if (h[4] && !padHeld[4]) engine.ePunch = true
        if (h[5] && !padHeld[5]) engine.eKick = true
        if (h[6] && !padHeld[6]) engine.eGrab = true
        if (h[7] && !padHeld[7]) engine.eSup = true
        System.arraycopy(h, 0, padHeld, 0, 8)
    }

    /** Hardware keyboard support (desktop / emulator). */
    fun key(code: Int, down: Boolean, repeat: Boolean): Boolean {
        if (down && !repeat) {
            if (code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_P) { back(); return true }
            if (code == KeyEvent.KEYCODE_ENTER) {
                when (screen) { Screen.TITLE -> goSelect(false); Screen.SELECT -> startFight(); Screen.LEVELS -> startLevel(selLv)
                    Screen.RESULT -> if (story && resultWin && lv < LEVEL_COUNT) startLevel(lv + 1) else act(Hit(0f, 0f, 0f, 0f, A.REMATCH, 0))
                    else -> {} }
                return true
            }
        }
        if (screen != Screen.FIGHT) return false
        when (code) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> engine.kLeft = down
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> engine.kRight = down
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> engine.kUp = down
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> engine.kDown = down
            KeyEvent.KEYCODE_J -> if (down && !repeat) engine.ePunch = true
            KeyEvent.KEYCODE_K -> if (down && !repeat) engine.eKick = true
            KeyEvent.KEYCODE_I -> if (down && !repeat) engine.eGrab = true
            KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_SPACE -> if (down && !repeat) engine.eSup = true
            else -> return false
        }
        return true
    }

    // ------------------------------------------------------------ drawing helpers
    private fun c(i: Int) = i or -0x1000000
    private fun text(cv: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.CENTER, stroke: Int = 0xFF1A0D06.toInt(), sw: Float = size * 0.2f) {
        p.textSize = size; p.textAlign = align
        if (sw > 0f) { p.style = Paint.Style.STROKE; p.strokeWidth = sw; p.strokeJoin = Paint.Join.ROUND; p.color = stroke; cv.drawText(s, x, y, p) }
        p.style = Paint.Style.FILL; p.color = color; cv.drawText(s, x, y, p)
    }
    private fun wrap(s: String, size: Float, maxW: Float): List<String> {
        p.textSize = size; p.style = Paint.Style.FILL
        val out = ArrayList<String>(); var line = ""
        for (w in s.split(" ")) { val t = if (line.isEmpty()) w else "$line $w"; if (p.measureText(t) > maxW && line.isNotEmpty()) { out.add(line); line = w } else line = t }
        if (line.isNotEmpty()) out.add(line); return out
    }
    private fun panel(cv: Canvas, l: Float, t: Float, r: Float, b: Float, fill: Int = 0xCC1A0D06.toInt(), border: Int = 0xFFFBBF24.toInt()) {
        rect.set(l, t, r, b); p.style = Paint.Style.FILL; p.color = fill; cv.drawRoundRect(rect, 8f, 8f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = border; cv.drawRoundRect(rect, 8f, 8f, p)
    }
    private fun btn(cv: Canvas, a: A, arg: Int, l: Float, t: Float, r: Float, b: Float, label: String, col: Int, size: Float = 16f, sel: Boolean = false, fg: Int = Color.WHITE) {
        hits.add(Hit(l, t, r, b, a, arg))
        val down = pressed?.let { it.a == a && it.arg == arg } == true; val dy = if (down) 2f else 0f
        rect.set(l, t + 3f, r, b + 3f); p.style = Paint.Style.FILL; p.color = 0xFF1A0D06.toInt(); cv.drawRoundRect(rect, 8f, 8f, p)
        rect.set(l, t + dy, r, b + dy); p.color = col; cv.drawRoundRect(rect, 8f, 8f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = if (sel) 3.5f else 2f; p.color = if (sel) Color.WHITE else 0xFF1A0D06.toInt(); cv.drawRoundRect(rect, 8f, 8f, p)
        text(cv, label, (l + r) / 2, (t + b) / 2 + size * 0.34f + dy, size, fg, sw = size * 0.14f)
    }
    private fun star(cv: Canvas, cx: Float, cy: Float, r: Float, filled: Boolean) {
        cv.save(); cv.translate(cx, cy); cv.scale(r, r)
        p.style = Paint.Style.FILL; p.color = if (filled) 0xFFFFC83D.toInt() else 0x55000000; cv.drawPath(starPath, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.12f; p.color = if (filled) 0xFF8A5A00.toInt() else 0xFF888888.toInt(); cv.drawPath(starPath, p)
        cv.restore()
    }
    private fun bar(cv: Canvas, l: Float, t: Float, w: Float, h: Float, frac: Float, color: Int, rtl: Boolean = false) {
        rect.set(l, t, l + w, t + h); p.style = Paint.Style.FILL; p.color = 0xAA000000.toInt(); cv.drawRoundRect(rect, h / 2, h / 2, p)
        val fw = w * clampf(frac, 0f, 1f)
        if (fw > 1f) { if (rtl) rect.set(l + w - fw, t, l + w, t + h) else rect.set(l, t, l + fw, t + h); p.color = color; cv.drawRoundRect(rect, h / 2, h / 2, p) }
        rect.set(l, t, l + w, t + h); p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0xFF1A0D06.toInt(); cv.drawRoundRect(rect, h / 2, h / 2, p)
    }

    // ------------------------------------------------------------ portraits (2D cartoon faces, rendered once)
    private fun portrait(i: Int): Bitmap {
        portraits[i]?.let { return it }
        val d = Roster.all[i]; val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888); val cv = Canvas(bmp)
        val q = Paint(Paint.ANTI_ALIAS_FLAG); val ink = 0xFF1A0D06.toInt()
        fun oval(cx: Float, cy: Float, rx: Float, ry: Float, col: Int, ol: Boolean = true) {
            rect.set(cx - rx, cy - ry, cx + rx, cy + ry); q.style = Paint.Style.FILL; q.color = col; cv.drawOval(rect, q)
            if (ol) { q.style = Paint.Style.STROKE; q.strokeWidth = 3f; q.color = ink; cv.drawOval(rect, q) }
        }
        val skin = c(d.skin); val hair = c(d.hair); val hc = c(d.hatColor)
        // shoulders
        rect.set(14f, 92f, 114f, 150f); q.style = Paint.Style.FILL; q.color = c(d.top); cv.drawRoundRect(rect, 30f, 30f, q)
        q.style = Paint.Style.STROKE; q.strokeWidth = 3f; q.color = ink; cv.drawRoundRect(rect, 30f, 30f, q)
        oval(64f, 98f, 20f, 7f, c(d.accent), false)
        if (d.hairStyle == "afro") oval(64f, 46f, 46f, 42f, hair)
        if (d.hairStyle == "bun") oval(40f, 14f, 13f, 12f, hair)
        oval(26f, 64f, 7f, 10f, skin); oval(102f, 64f, 7f, 10f, skin)
        oval(64f, 60f, 36f, 38f, skin)
        when (d.hairStyle) {
            "short", "bun" -> { path.reset(); path.addArc(RectF(27f, 20f, 101f, 90f), 190f, 160f); path.close(); q.style = Paint.Style.FILL; q.color = hair; cv.drawPath(path, q) }
            "slick" -> { path.reset(); path.addArc(RectF(27f, 19f, 101f, 85f), 185f, 170f); path.close(); q.style = Paint.Style.FILL; q.color = hair; cv.drawPath(path, q) }
            "afro" -> { path.reset(); path.addArc(RectF(26f, 18f, 102f, 80f), 180f, 180f); path.close(); q.style = Paint.Style.FILL; q.color = hair; cv.drawPath(path, q) }
            "spiky" -> { q.style = Paint.Style.FILL; q.color = hair
                for (k in 0 until 6) { val x = 34f + k * 12f; path.reset(); path.moveTo(x - 7f, 36f); path.lineTo(x + (k - 2.5f) * 2f, 6f + (k % 2) * 6f); path.lineTo(x + 7f, 36f); path.close(); cv.drawPath(path, q) }
                path.reset(); path.addArc(RectF(27f, 22f, 101f, 80f), 190f, 160f); path.close(); cv.drawPath(path, q) }
        }
        if (d.beard) { path.reset(); path.addArc(RectF(28f, 40f, 100f, 104f), 10f, 160f); path.close(); q.style = Paint.Style.FILL; q.color = hair; cv.drawPath(path, q) }
        // eyes
        for (sx in intArrayOf(-1, 1)) { oval(64f + sx * 15f, 58f, 8f, 10f, Color.WHITE); oval(64f + sx * 14f, 60f, 3.5f, 5f, 0xFF120A06.toInt(), false) }
        oval(64f, 72f, 5f, 4f, skin)
        if (d.stache > 0) { q.style = Paint.Style.FILL; q.color = hair; val t = if (d.stache == 2) 6f else 4f
            oval(55f, 80f, 11f, t, hair, false); oval(73f, 80f, 11f, t, hair, false) }
        oval(64f, 90f, 7f, 3.5f, 0xFF3A0D0D.toInt(), false)
        if (d.glasses) { q.style = Paint.Style.STROKE; q.strokeWidth = 2.5f; q.color = 0xFF111111.toInt()
            cv.drawCircle(49f, 58f, 11f, q); cv.drawCircle(79f, 58f, 11f, q); cv.drawLine(60f, 58f, 68f, 58f, q) }
        when (d.hat) {
            "topi" -> { path.reset(); path.moveTo(30f, 38f); path.lineTo(98f, 38f); path.lineTo(92f, 10f); path.lineTo(36f, 10f); path.close(); q.style = Paint.Style.FILL; q.color = hc; cv.drawPath(path, q); q.style = Paint.Style.STROKE; q.strokeWidth = 3f; q.color = ink; cv.drawPath(path, q) }
            "turban" -> { oval(64f, 28f, 42f, 26f, hc); q.style = Paint.Style.STROKE; q.strokeWidth = 3f; q.color = 0x88FFFFFF.toInt(); cv.drawLine(28f, 30f, 98f, 20f, q); cv.drawLine(26f, 40f, 100f, 32f, q); oval(64f, 4f, 10f, 8f, hc) }
            "cap" -> { path.reset(); path.addArc(RectF(27f, 14f, 101f, 70f), 180f, 180f); path.close(); q.style = Paint.Style.FILL; q.color = hc; cv.drawPath(path, q); q.style = Paint.Style.STROKE; q.strokeWidth = 3f; q.color = ink; cv.drawPath(path, q); rect.set(60f, 38f, 116f, 46f); q.style = Paint.Style.FILL; q.color = hc; cv.drawRoundRect(rect, 4f, 4f, q); q.style = Paint.Style.STROKE; cv.drawRoundRect(rect, 4f, 4f, q) }
            "crown" -> { path.reset(); path.moveTo(32f, 36f); path.lineTo(34f, 8f); path.lineTo(48f, 24f); path.lineTo(64f, 4f); path.lineTo(80f, 24f); path.lineTo(94f, 8f); path.lineTo(96f, 36f); path.close()
                q.style = Paint.Style.FILL; q.color = hc; cv.drawPath(path, q); q.style = Paint.Style.STROKE; q.strokeWidth = 3f; q.color = ink; cv.drawPath(path, q); oval(64f, 26f, 5f, 5f, 0xFFEF4444.toInt(), false) }
        }
        portraits[i] = bmp; return bmp
    }

    // ------------------------------------------------------------ main draw
    override fun onDraw(cv: Canvas) {
        u = min(height / 360f, width / 560f); DW = width / u; DH = height / u
        hits.clear(); pad.clear()
        cv.save(); cv.scale(u, u)
        // results become visible 1.2 s after the match ends
        if (screen == Screen.FIGHT && resultAt != 0L && now >= resultAt && engine.scr == Scr.RESULT) showResult()
        when (screen) {
            Screen.TITLE -> drawTitle(cv)
            Screen.SELECT -> drawSelect(cv)
            Screen.LEVELS -> drawLevels(cv)
            Screen.FIGHT -> { drawHud(cv); drawPad(cv) }
            Screen.PAUSE -> { drawHud(cv); drawPause(cv) }
            Screen.RESULT -> { drawHud(cv); drawResult(cv) }
        }
        cv.restore()
        postInvalidateOnAnimation()
    }

    private fun showResult() {
        resultAt = 0L
        val a = engine.p1 ?: return
        val ratio = a.hp / a.def.hp; val st = if (resultWin) (if (ratio > 0.66f) 3 else if (ratio > 0.33f) 2 else 1) else 0
        resStars = st; resHead = HEADLINES[(Math.random() * HEADLINES.size).toInt()]
        if (story && resultWin) { if (stars[lv] < st) stars[lv] = st; lastLevel = min(LEVEL_COUNT, lv + 1); writeSave() }
        screen = Screen.RESULT; if (resultWin) audio.win() else audio.lose()
        onMatchEnd?.invoke()
    }

    private fun drawTitle(cv: Canvas) {
        cv.save(); cv.rotate(-3f, DW / 2, 90f)
        text(cv, "NETA", DW / 2, 90f, 78f, 0xFFFBBF24.toInt(), stroke = 0xFFB3261E.toInt(), sw = 14f)
        text(cv, "FIGHT", DW / 2, 150f, 78f, 0xFFFFFFFF.toInt(), stroke = 0xFFB3261E.toInt(), sw = 14f)
        cv.restore()
        text(cv, "★ THE GREAT POWER BATTLE ★ 3D", DW / 2, 178f, 15f, 0xFFFFE066.toInt())
        btn(cv, A.VS, 0, DW / 2 - 158f, 214f, DW / 2 - 8f, 262f, "VS CPU", 0xFFD62839.toInt(), 24f)
        btn(cv, A.LV, 0, DW / 2 + 8f, 214f, DW / 2 + 158f, 262f, "500 LEVELS", 0xFF2563EB.toInt(), 24f)
        val l = wrap("Political satire & parody game. All characters are original and fictional. Cartoon violence — don't imitate in real life.", 10f, DW - 60f)
        for ((i, s) in l.withIndex()) text(cv, s, DW / 2, DH - 28f + i * 12f, 10f, 0xFFFFFFFF.toInt(), sw = 2f)
    }

    private fun infoPanel(cv: Canvas, d: CharDef, who: String, l: Float, w: Float) {
        panel(cv, l, 36f, l + w, 194f)
        text(cv, who, l + 8f, 52f, 11f, 0xFFFFE066.toInt(), Paint.Align.LEFT, sw = 2f)
        text(cv, d.name, l + 8f, 74f, 21f, Color.WHITE, Paint.Align.LEFT, sw = 3.5f)
        text(cv, d.team + " · " + d.title, l + 8f, 88f, 9f, 0xFFD6C7A8.toInt(), Paint.Align.LEFT, sw = 1.5f)
        text(cv, "SUPER · " + d.sup.name, l + 8f, 104f, 11f, 0xFFFBBF24.toInt(), Paint.Align.LEFT, sw = 2f)
        var y = 116f; for (s in wrap(d.sup.desc, 9f, w - 16f).take(3)) { text(cv, s, l + 8f, y, 9f, 0xFFEFE6D2.toInt(), Paint.Align.LEFT, sw = 1.5f); y += 10f }
        val stats = arrayOf("PWR" to d.pwr * 70f, "SPD" to d.spd * 70f, "DEF" to (1f / d.def) * 55f + d.hp / 4f)
        for ((i, s) in stats.withIndex()) { val yy = 150f + i * 14f; text(cv, s.first, l + 8f, yy + 8f, 9f, Color.WHITE, Paint.Align.LEFT, sw = 1.5f)
            bar(cv, l + 38f, yy, w - 50f, 9f, max(8f, min(100f, s.second)) / 100f, 0xFFFBBF24.toInt()) }
    }

    private fun drawSelect(cv: Canvas) {
        btn(cv, A.BACK, 0, 8f, 8f, 70f, 30f, "‹ BACK", 0xCC3A2A1A.toInt(), 12f)
        text(cv, if (story) "PICK YOUR FIGHTER" else "SELECT YOUR NETA", DW / 2, 28f, 24f, 0xFFFBBF24.toInt(), stroke = 0xFFB3261E.toInt(), sw = 5f)
        val pw = min(172f, DW * 0.3f)
        infoPanel(cv, Roster.all[sel1], "1P · YOU", 6f, pw)
        if (!story) infoPanel(cv, Roster.all[sel2], "CPU", DW - pw - 6f, pw)
        val label = if (story) "PICK YOUR FIGHTER" else if (phase == 0) "PICK YOUR NETA  (1P)" else "PICK CPU OPPONENT"
        val n = Roster.all.size + 1; val gap = 4f; val cw = (DW - 16f - gap * (n - 1)) / n; val top = DH - 128f; val ch = 62f
        text(cv, label, DW / 2, top - 6f, 11f, Color.WHITE, sw = 2.5f)
        for (i in 0 until n) {
            val l = 8f + i * (cw + gap); val d = if (i < Roster.all.size) Roster.all[i] else null
            hits.add(Hit(l, top, l + cw, top + ch, A.CARD, i))
            val s1 = i == sel1; val s2 = !story && i == sel2
            rect.set(l, top, l + cw, top + ch); p.style = Paint.Style.FILL; p.color = if (d?.boss == true) 0xFF6B1E1E.toInt() else 0xDD2A1A10.toInt(); cv.drawRoundRect(rect, 6f, 6f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = if (s1 || s2) 3.5f else 1.5f; p.color = if (s1) 0xFF38BDF8.toInt() else if (s2) 0xFFEF4444.toInt() else 0xFF8A6A3A.toInt(); cv.drawRoundRect(rect, 6f, 6f, p)
            if (d != null) { rect.set(l + 2f, top + 2f, l + cw - 2f, top + ch - 14f); cv.drawBitmap(portrait(i), null, rect, null) }
            else text(cv, "?", l + cw / 2, top + 36f, 34f, 0xFFFBBF24.toInt(), sw = 4f)
            text(cv, d?.name?.split(" ")?.get(0) ?: "RANDOM", l + cw / 2, top + ch - 4f, if (cw < 46f) 8f else 9f, Color.WHITE, sw = 1.8f)
            if (s1) { text(cv, "1P", l + 3f, top + 11f, 9f, 0xFF38BDF8.toInt(), Paint.Align.LEFT, sw = 2f) }
            if (s2) { text(cv, "CPU", l + cw - 3f, top + 11f, 9f, 0xFFEF4444.toInt(), Paint.Align.RIGHT, sw = 2f) }
        }
        val by = DH - 52f
        if (!story) {
            text(cv, "STAGE", 12f, by + 8f, 9f, 0xFFD6C7A8.toInt(), Paint.Align.LEFT, sw = 1.5f)
            for (i in 0 until 3) btn(cv, A.STAGE, i, 12f + i * 66f, by + 12f, 74f + i * 66f, by + 34f, STAGE_NAMES[i].split(" ")[0], if (i == stageSel) 0xFFD62839.toInt() else 0xFF3A2A1A.toInt(), 10f, i == stageSel)
            text(cv, "CPU LEVEL", 224f, by + 8f, 9f, 0xFFD6C7A8.toInt(), Paint.Align.LEFT, sw = 1.5f)
            for (i in 0 until 3) btn(cv, A.CPU, i, 224f + i * 58f, by + 12f, 276f + i * 58f, by + 34f, LEVEL_NAMES[i], if (i == cpuSel) 0xFFD62839.toInt() else 0xFF3A2A1A.toInt(), 10f, i == cpuSel)
        }
        btn(cv, A.FIGHT, 0, DW - 170f, by, DW - 10f, by + 40f, if (story) "LEVELS ›" else "FIGHT!", 0xFFD62839.toInt(), 22f)
    }

    private fun drawLevels(cv: Canvas) {
        btn(cv, A.LV_BACK, 0, 8f, 6f, 70f, 28f, "‹ BACK", 0xCC3A2A1A.toInt(), 12f)
        text(cv, "500 LEVELS", DW / 2, 27f, 22f, 0xFFFBBF24.toInt(), stroke = 0xFFB3261E.toInt(), sw = 4.5f)
        val tw = (DW - 16f - 9f * 3f) / 10f
        for (i in 0 until 10) btn(cv, A.TAB, i, 8f + i * (tw + 3f), 34f, 8f + i * (tw + 3f) + tw, 54f, "${i * 50 + 1}-${i * 50 + 50}", if (i == page) 0xFFD62839.toInt() else 0xCC3A2A1A.toInt(), if (tw < 50f) 8f else 9f, i == page)
        val gw = (DW - 16f - 9f * 3f) / 10f; val gh = 31f; val gt = 60f
        panel(cv, 4f, gt - 4f, DW - 4f, gt + 5 * (gh + 3f) + 1f, 0x88100804.toInt(), 0xFF8A6A3A.toInt())
        for (k in 0 until 50) {
            val n = page * 50 + k + 1; val col = k % 10; val row = k / 10
            val l = 8f + col * (gw + 3f); val t = gt + row * (gh + 3f); val boss = n % 10 == 0; val sel = n == selLv
            btn(cv, A.CELL, n, l, t, l + gw, t + gh, "", if (boss) 0xFF8C1D2A.toInt() else 0xFF3A2A1A.toInt(), 8f, sel)
            text(cv, n.toString(), l + gw / 2, t + 15f, 13f, if (boss) 0xFFFFC83D.toInt() else Color.WHITE, sw = 2.4f)
            for (s in 0 until 3) star(cv, l + gw / 2 + (s - 1) * 8f, t + 24f, 3.6f, s < stars[n])
        }
        val iy = gt + 5 * (gh + 3f) + 8f
        panel(cv, 6f, iy, DW - 6f, DH - 6f)
        val si = selInfo
        if (si != null) {
            val d = si.def
            text(cv, "LEVEL ${si.n} · ${d.name}" + (if (si.boss) "  [BOSS]" else "") + " · " + STAGE_NAMES[si.stage], 16f, iy + 20f, 16f, Color.WHITE, Paint.Align.LEFT, sw = 3f)
            val sub = si.tier + " · HP " + d.hp.toInt() + " · POWER ×" + String.format("%.2f", d.pwr) + " · CPU SKILL " + (si.ai / 3f * 100f).toInt() + "%" + (if (si.modNames.isNotEmpty()) " · TWIST: " + si.modNames.joinToString(" + ") else "")
            var y = iy + 36f; for (s in wrap(sub, 10f, DW - 190f).take(2)) { text(cv, s, 16f, y, 10f, 0xFFFFE066.toInt(), Paint.Align.LEFT, sw = 1.8f); y += 12f }
            text(cv, "Tap a level to select, tap again to start.", 16f, DH - 14f, 9f, 0xFFD6C7A8.toInt(), Paint.Align.LEFT, sw = 1.5f)
        }
        btn(cv, A.LV_START, 0, DW - 160f, iy + 8f, DW - 14f, iy + 46f, "START ›", 0xFFD62839.toInt(), 20f)
    }

    private fun drawHud(cv: Canvas) {
        val a = engine.p1 ?: return; val b = engine.p2 ?: return
        val bw = DW / 2 - 52f
        val r1 = a.hp / a.def.hp; val r2 = b.hp / b.def.hp
        g1 = if (g1 > r1) max(r1, g1 - 0.006f) else r1; g2 = if (g2 > r2) max(r2, g2 - 0.006f) else r2
        fun hpCol(r: Float) = if (r > 0.5f) 0xFF22C55E.toInt() else if (r > 0.25f) 0xFFFACC15.toInt() else 0xFFEF4444.toInt()
        text(cv, a.def.name, 10f, 20f, 15f, Color.WHITE, Paint.Align.LEFT, sw = 3f)
        bar(cv, 10f, 25f, bw, 14f, g1, 0xFFFFFFFF.toInt()); bar(cv, 10f, 25f, bw, 14f, r1, hpCol(r1))
        val full1 = a.meter >= 100f; bar(cv, 10f, 42f, bw * 0.7f, 8f, a.meter / 100f, if (full1 && (now / 200_000_000L) % 2 == 0L) 0xFFFFFFFF.toInt() else 0xFFFBBF24.toInt())
        text(cv, if (full1) "SUPER READY!" else "SUPER", 14f + bw * 0.7f, 50f, 9f, if (full1) 0xFFFFE066.toInt() else Color.WHITE, Paint.Align.LEFT, sw = 2f)
        text(cv, b.def.name, DW - 10f, 20f, 15f, Color.WHITE, Paint.Align.RIGHT, sw = 3f)
        bar(cv, DW - 10f - bw, 25f, bw, 14f, g2, 0xFFFFFFFF.toInt(), true); bar(cv, DW - 10f - bw, 25f, bw, 14f, r2, hpCol(r2), true)
        bar(cv, DW - 10f - bw * 0.7f, 42f, bw * 0.7f, 8f, b.meter / 100f, 0xFFFBBF24.toInt(), true)
        for (i in 0 until 2) { fun pip(cx: Float, on: Boolean) { p.style = Paint.Style.FILL; p.color = if (on) 0xFFFFC83D.toInt() else 0x66000000; cv.drawCircle(cx, 62f, 5f, p); p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f; p.color = 0xFF1A0D06.toInt(); cv.drawCircle(cx, 62f, 5f, p) }
            pip(16f + i * 15f, i < a.wins); pip(DW - 16f - i * 15f, i < b.wins) }
        // timer
        val secs = max(0, (engine.timer + 59) / 60)
        p.style = Paint.Style.FILL; p.color = 0xDD1A0D06.toInt(); cv.drawCircle(DW / 2, 30f, 22f, p); p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFFFBBF24.toInt(); cv.drawCircle(DW / 2, 30f, 22f, p)
        text(cv, secs.toString(), DW / 2, 40f, 26f, if (secs <= 10) 0xFFEF4444.toInt() else Color.WHITE, sw = 3.5f)
        if (screen == Screen.FIGHT) {
            hits.add(Hit(DW / 2 - 18f, 56f, DW / 2 + 18f, 80f, A.PAUSE, 0))
            rect.set(DW / 2 - 16f, 56f, DW / 2 + 16f, 78f); p.style = Paint.Style.FILL; p.color = 0xAA1A0D06.toInt(); cv.drawRoundRect(rect, 6f, 6f, p)
            p.color = Color.WHITE; cv.drawRect(DW / 2 - 7f, 61f, DW / 2 - 2f, 73f, p); cv.drawRect(DW / 2 + 2f, 61f, DW / 2 + 7f, 73f, p)
        }
        val inf = info
        if (story && inf != null) text(cv, "LEVEL ${inf.n} · ${inf.tier}" + (if (inf.modNames.isNotEmpty()) " · " + inf.modNames.joinToString(" + ") else ""), DW / 2, 94f, 10f, 0xFFFFE066.toInt(), sw = 2f)
        if (a.combo >= 2) text(cv, "${a.combo} HITS!", 14f, 100f, 26f, 0xFFFFC83D.toInt(), Paint.Align.LEFT, stroke = 0xFFB3261E.toInt(), sw = 5f)
        if (b.combo >= 2) text(cv, "${b.combo} HITS!", DW - 14f, 100f, 26f, 0xFFFFC83D.toInt(), Paint.Align.RIGHT, stroke = 0xFFB3261E.toInt(), sw = 5f)
        drawFx(cv)
    }

    private fun drawFx(cv: Canvas) {
        val t = now
        // banner
        if (bannerText.isNotEmpty()) {
            var age = (t - bannerStart) / 1e9f
            if (bannerText.startsWith("ROUND") && age > bannerDur && engine.scr == Scr.INTRO) { bannerText = "FIGHT!"; bannerStart = t; bannerDur = 0.9f; age = 0f }
            if (age < bannerDur) {
                val k = max(0f, 0.28f - age) / 0.28f; val s = 1f + k * 1.6f; val al = if (age > bannerDur - 0.3f) (bannerDur - age) / 0.3f else 1f
                cv.save(); cv.translate(DW / 2, DH * 0.42f); cv.scale(s, s); cv.rotate(-4f)
                p.alpha = 255
                text(cv, bannerText, 0f, 0f, 64f, (0xFFFFE066.toInt() and 0x00FFFFFF) or ((al * 255).toInt() shl 24), stroke = ((al * 255).toInt() shl 24) or 0xB3261E, sw = 12f)
                cv.restore()
            }
        }
        // pops
        val it = pops.iterator()
        while (it.hasNext()) {
            val q = it.next(); val age = (t - q.t0) / 1e9f
            if (age > 0.9f) { it.remove(); continue }
            val al = (1f - max(0f, age - 0.5f) / 0.4f).coerceIn(0f, 1f); val sc = 0.6f + min(1f, age * 8f) * 0.5f
            cv.save(); cv.translate(q.x * DW, q.y * DH - age * 34f); cv.scale(sc, sc); cv.rotate(-6f)
            text(cv, q.text, 0f, 0f, 26f, (q.color and 0x00FFFFFF) or ((al * 255).toInt() shl 24), stroke = ((al * 255).toInt() shl 24) or 0x1A0D06, sw = 5f)
            cv.restore()
        }
        // super flash
        val fa = (t - flashAt) / 1e9f
        if (flashAt != 0L && fa < 0.4f) { p.style = Paint.Style.FILL; p.color = (flashColor and 0x00FFFFFF) or (((1f - fa / 0.4f) * 120).toInt() shl 24); cv.drawRect(0f, 0f, DW, DH, p) }
    }

    private fun drawPad(cv: Canvas) {
        val cx = 74f; val cy = DH - 74f; val s = 22f; val off = 44f
        fun dbtn(key: Int, x: Float, y: Float, rot: Float) {
            pad.add(PadBtn(key, x, y, s, "", 0)); val on = padHeld[key]
            p.style = Paint.Style.FILL; p.color = if (on) 0xCCFBBF24.toInt() else 0x88000000.toInt(); cv.drawCircle(x, y, s, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = 0xCCFFFFFF.toInt(); cv.drawCircle(x, y, s, p)
            cv.save(); cv.translate(x, y); cv.rotate(rot); path.reset(); path.moveTo(0f, -10f); path.lineTo(9f, 7f); path.lineTo(-9f, 7f); path.close()
            p.style = Paint.Style.FILL; p.color = 0xEEFFFFFF.toInt(); cv.drawPath(path, p); cv.restore()
        }
        dbtn(0, cx, cy - off, 0f); dbtn(1, cx, cy + off, 180f); dbtn(2, cx - off, cy, -90f); dbtn(3, cx + off, cy, 90f)
        val ax = DW - 74f; val ay = DH - 70f
        fun abtn(key: Int, x: Float, y: Float, r: Float, label: String, col: Int, glow: Boolean = false) {
            pad.add(PadBtn(key, x, y, r, label, col)); val on = padHeld[key]
            p.style = Paint.Style.FILL; p.color = (col and 0x00FFFFFF) or (if (on) 0xFF000000.toInt() else 0xB0000000.toInt()); cv.drawCircle(x, y, r + (if (glow) 3f else 0f), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = if (glow) 4f else 2.5f; p.color = if (glow) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt(); cv.drawCircle(x, y, r + (if (glow) 3f else 0f), p)
            text(cv, label, x, y + r * 0.22f, r * 0.62f, Color.WHITE, sw = r * 0.12f)
        }
        val ready = (engine.p1?.meter ?: 0f) >= 100f
        abtn(4, ax, ay, 34f, "PUNCH", 0xE53935); abtn(5, ax - 62f, ay + 10f, 28f, "KICK", 0x1E88E5)
        abtn(6, ax + 2f, ay - 66f, 25f, "GRAB", 0xF57C00); abtn(7, ax - 64f, ay - 52f, 27f, "SUPER", if (ready) 0xFFB300 else 0x7A5A10, ready && (now / 250_000_000L) % 2 == 0L)
    }

    private fun drawPause(cv: Canvas) {
        p.style = Paint.Style.FILL; p.color = 0x99000000.toInt(); cv.drawRect(0f, 0f, DW, DH, p)
        val l = DW / 2 - 130f; val t = DH / 2 - 110f
        panel(cv, l, t, l + 260f, t + 220f)
        text(cv, "CHAI BREAK", DW / 2, t + 36f, 30f, 0xFFFBBF24.toInt(), stroke = 0xFFB3261E.toInt(), sw = 6f)
        text(cv, "Sansad adjourned till further notice.", DW / 2, t + 54f, 10f, Color.WHITE, sw = 2f)
        btn(cv, A.RESUME, 0, l + 30f, t + 70f, l + 230f, t + 108f, "RESUME", 0xFFD62839.toInt(), 18f)
        btn(cv, A.MUSIC, 0, l + 30f, t + 118f, l + 230f, t + 156f, "MUSIC: " + (if (audio.musicOn) "ON" else "OFF"), 0xFF2563EB.toInt(), 18f)
        btn(cv, A.QUIT, 0, l + 30f, t + 166f, l + 230f, t + 204f, if (story) "QUIT TO LEVELS" else "QUIT TO SELECT", 0xFF555555.toInt(), 18f)
    }

    private fun drawResult(cv: Canvas) {
        p.style = Paint.Style.FILL; p.color = 0x99000000.toInt(); cv.drawRect(0f, 0f, DW, DH, p)
        val l = DW / 2 - 190f; val t = DH / 2 - 128f
        panel(cv, l, t, l + 380f, t + 256f)
        val win = resultWin
        text(cv, if (win) (if (story) "LEVEL $lv CLEAR!" else "YOU WIN!") else "YOU LOSE!", DW / 2, t + 48f, 44f, if (win) 0xFFFBBF24.toInt() else 0xFFEF4444.toInt(), stroke = 0xFF1A0D06.toInt(), sw = 8f)
        if (story && win) for (s in 0 until 3) star(cv, DW / 2 + (s - 1) * 34f, t + 80f, 14f, s < resStars)
        rect.set(l + 20f, t + 98f, l + 360f, t + 150f); p.style = Paint.Style.FILL; p.color = 0xFFF5ECD3.toInt(); cv.drawRoundRect(rect, 4f, 4f, p)
        text(cv, "THE DAILY JUMLA", DW / 2, t + 112f, 12f, 0xFF1A0D06.toInt(), sw = 0f)
        var y = t + 126f; for (s in wrap(resHead, 10f, 330f).take(2)) { p.typeface = Typeface.DEFAULT_BOLD; text(cv, s, DW / 2, y, 10f, 0xFF3A2A1A.toInt(), sw = 0f); p.typeface = face; y += 12f }
        val showNext = story && win && lv < LEVEL_COUNT
        val labels = ArrayList<Triple<A, String, Int>>()
        if (showNext) labels.add(Triple(A.NEXT, "NEXT LEVEL ›", 0xFFB7791F.toInt()))
        labels.add(Triple(A.REMATCH, if (story) "RETRY" else "REMATCH", 0xFFD62839.toInt()))
        labels.add(Triple(A.CHAR, if (story) "LEVELS" else "CHARACTERS", 0xFF2563EB.toInt()))
        val bw = (340f - 8f * (labels.size - 1)) / labels.size
        for ((i, x) in labels.withIndex()) btn(cv, x.first, 0, l + 20f + i * (bw + 8f), t + 176f, l + 20f + i * (bw + 8f) + bw, t + 220f, x.second, x.third, 16f)
    }
}
