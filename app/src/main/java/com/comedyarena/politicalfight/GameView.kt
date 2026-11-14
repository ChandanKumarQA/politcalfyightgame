package com.comedyarena.politicalfight

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** mode: story | quick | party | tourney | two */
class Cfg(val mode: String, val p1: Int, val p2: Int, val level: Int)

private class Proj(var x: Float, var y: Float, val vx: Float, val owner: Fighter, val dmg: Float,
                   val r: Float, val label: String, val col: Int, val wob: Boolean) { var age = 0 }
private class FT(var x: Float, var y: Float, val text: String, var life: Int, val col: Int)
private class Btn(val r: RectF, val label: String, val player: Int, val code: Int, val col: Int)
private class OB(val r: RectF, val label: String, val col: Int, val act: () -> Unit)

class GameView(ctx: Context, private val cfg: Cfg, private val onExit: () -> Unit) :
    SurfaceView(ctx), SurfaceHolder.Callback, Runnable {

    private val W = K.W
    private val H = K.H
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    @Volatile private var running = false
    private var thread: Thread? = null
    private var sc = 1f; private var ox = 0f; private var oy = 0f
    private val heroes = Roster.heroes
    private val rnd = java.util.Random()

    private lateinit var p1: Fighter
    private lateinit var p2: Fighter
    private val projs = ArrayList<Proj>()
    private val floats = ArrayList<FT>()
    private var arena = 0
    private var phase = 0          // 0 intro, 1 play, 2 end
    private var phaseT = 0
    private var timeLeft = 3600
    private var score = 0
    private var shake = 0f
    @Volatile private var paused = false
    private var winnerP1 = false
    private var isDraw = false
    private var tRound = 0
    private var tOpps = IntArray(4)
    private var curLevel = cfg.level
    private var tm = 0f
    private var banner = ""
    private var resultTitle = ""
    private var comboCount = 0
    private var comboTimer = 0
    private val tickerHeadlines = listOf(
        "PARLIAMENT ADJOURNED: Ring-side samosa crisis reported · OPPOSITION CALLS FOR BAN ON DROPKICKS ·",
        "FAKE POLL: 1P 54% · CPU 62% · OTHERS 12% · TOTAL 128% (Margin of error ±100%) ·",
        "BREAKING: Flying dropkick declared 100% locally manufactured and organic ·",
        "EXCLUSIVE: Ring-side tea stall reports historic drop in chai prices during timeout ·",
        "ASTROLOGY ALERT: Special moves affected by retrograde Saturn in third round ·"
    )
    private val held = Array(2) { BooleanArray(7) }
    private val btns = ArrayList<Btn>()
    private var endButtons = listOf<OB>()
    private var pauseButtons = listOf<OB>()
    @Volatile private var pending: (() -> Unit)? = null
    private val pauseRect = RectF(750f, 150f, 850f, 205f)

    init {
        holder.addCallback(this)
        isFocusable = true
        buildControls()
        pauseButtons = listOf(
            OB(RectF(560f, 380f, 1040f, 470f), "RESUME", 0xFF43A047.toInt()) { paused = false },
            OB(RectF(560f, 500f, 1040f, 590f), "MAIN MENU", 0xFFE53935.toInt()) { exitNow() })
        initTournament()
        setupMatch()
    }

    private fun exitNow() { post { onExit() } }

    // ---------------------------------------------------------------- setup
    private fun initTournament() {
        val pool = (0 until 8).filter { it != cfg.p1 }.shuffled(rnd)
        tOpps = IntArray(4) { pool[it] }
    }

    private fun setupMatch() {
        projs.clear(); floats.clear()
        phase = 0; phaseT = 0; timeLeft = 3600; shake = 0f; paused = false; endButtons = listOf()
        val h1 = heroes[cfg.p1]
        when (cfg.mode) {
            "story" -> {
                val lv = Levels.list[curLevel - 1]
                var o = if (lv.opp < 0) (cfg.p1 + 2 + rnd.nextInt(5)) % 8 else lv.opp
                if (o == cfg.p1) o = (o + 1) % 8
                arena = lv.arena
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[o], 1220f, true, lv.diff, lv.hpMul)
                banner = "LEVEL $curLevel: ${lv.title.uppercase()}"
            }
            "tourney" -> {
                val o = tOpps[tRound]
                arena = (tRound * 2 + 1) % 7
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[o], 1220f, true, 0.35f + 0.17f * tRound, 1f + 0.1f * tRound)
                banner = "TOURNAMENT ROUND ${tRound + 1}/4"
            }
            "party" -> {
                arena = rnd.nextInt(7)
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[cfg.p2], 1220f, true, 0.55f, 1f)
                banner = "${h1.party.uppercase()} VS ${heroes[cfg.p2].party.uppercase()}"
            }
            "two" -> {
                arena = rnd.nextInt(7)
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[cfg.p2], 1220f, false)
                banner = "TWO PLAYER BATTLE"
            }
            else -> {
                var o = rnd.nextInt(8); if (o == cfg.p1) o = (o + 1) % 8
                arena = rnd.nextInt(7)
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[o], 1220f, true, 0.5f, 1f)
                banner = "QUICK FIGHT"
            }
        }
        p2.face = -1
    }

    private fun buildControls() {
        btns.clear()
        fun add(pl: Int, code: Int, label: String, x: Float, y: Float, s: Float, col: Int) {
            btns.add(Btn(RectF(x, y, x + s, y + s), label, pl, code, col))
        }
        if (cfg.mode != "two") {
            val s = 150f
            add(0, 0, "◀", 30f, 720f, s, 0xFF42A5F5.toInt()); add(0, 1, "▶", 200f, 720f, s, 0xFF42A5F5.toInt())
            add(0, 2, "JUMP", 115f, 550f, s, 0xFF66BB6A.toInt())
            add(0, 6, "BLOCK", 1060f, 720f, s, 0xFF78909C.toInt()); add(0, 3, "PUNCH", 1250f, 720f, s, 0xFFEF5350.toInt())
            add(0, 4, "KICK", 1420f, 720f, s, 0xFFFFA726.toInt()); add(0, 5, "SPECIAL", 1335f, 500f, s, 0xFFAB47BC.toInt())
        } else {
            val s = 112f; val st = 122f
            for (pl in 0..1) {
                val bx = if (pl == 0) 24f else W - 24f - (3 * st + s)
                val y1 = H - s - 16f; val y2 = y1 - st
                add(pl, 0, "◀", bx, y1, s, 0xFF42A5F5.toInt()); add(pl, 1, "▶", bx + st, y1, s, 0xFF42A5F5.toInt())
                add(pl, 2, "JUMP", bx + 2 * st, y1, s, 0xFF66BB6A.toInt()); add(pl, 6, "BLOCK", bx + 3 * st, y1, s, 0xFF78909C.toInt())
                add(pl, 3, "PUNCH", bx, y2, s, 0xFFEF5350.toInt()); add(pl, 4, "KICK", bx + st, y2, s, 0xFFFFA726.toInt())
                add(pl, 5, "SPECIAL", bx + 2 * st, y2, s, 0xFFAB47BC.toInt())
            }
        }
    }

    // ---------------------------------------------------------------- lifecycle
    override fun surfaceCreated(h: SurfaceHolder) { running = true; thread = Thread(this).also { it.start() } }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
        sc = max(w / W, hh / H); ox = (w - W * sc) / 2f; oy = (hh - H * sc) / 2f
    }
    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        try { thread?.join(500) } catch (e: Exception) { }
    }
    fun autoPause() { if (phase == 1) paused = true }
    fun togglePause() { if (phase == 1) paused = !paused else if (phase == 2) onExit() }

    override fun run() {
        var last = System.nanoTime()
        var acc = 0.0
        val step = 1e9 / 60.0
        while (running) {
            val now = System.nanoTime()
            acc += (now - last); last = now
            if (acc > step * 5) acc = step * 5
            pending?.let { pending = null; it() }
            while (acc >= step) { if (!paused) update(); acc -= step }
            val c = try { holder.lockCanvas() } catch (e: Exception) { null }
            if (c != null) {
                try { render(c) } finally { try { holder.unlockCanvasAndPost(c) } catch (e: Exception) { } }
            }
            val sleepMs = ((step - acc) / 1e6).toLong() - 1
            if (sleepMs > 0) try { Thread.sleep(sleepMs) } catch (e: Exception) { }
        }
    }

    // ---------------------------------------------------------------- update
    private fun update() {
        tm++
        phaseT++
        if (shake > 0) shake *= 0.85f
        when (phase) {
            0 -> {
                if (phaseT == 1) {
                    p1.say(p1.hero.intro, 110); p2.say(p2.hero.intro, 110)
                }
                if (phaseT == 100) Sfx.fight()
                java.util.Arrays.fill(p1.held, false); java.util.Arrays.fill(p2.held, false)
                p1.update(p2); p2.update(p1)
                if (phaseT > 130) { phase = 1; phaseT = 0 }
            }
            1 -> playUpdate()
            2 -> {
                java.util.Arrays.fill(p1.held, false); java.util.Arrays.fill(p2.held, false)
                p1.update(p2); p2.update(p1)
                updateProjs()
            }
        }
        val it = floats.iterator()
        while (it.hasNext()) { val f = it.next(); f.life--; f.y -= 1.5f; if (f.life <= 0) it.remove() }
    }

    private fun playUpdate() {
        if (comboTimer > 0) {
            comboTimer--
            if (comboTimer == 0) comboCount = 0
        }
        System.arraycopy(held[0], 0, p1.held, 0, 7)
        if (cfg.mode == "two") System.arraycopy(held[1], 0, p2.held, 0, 7) else p2.think(p1)
        p1.update(p2); p2.update(p1)
        // keep fighters from overlapping
        val dx = p2.x - p1.x
        if (abs(dx) < 70f && p1.onGround && p2.onGround) {
            val push = (70f - abs(dx)) / 2f; val s = if (dx >= 0) 1f else -1f
            p1.x -= s * push; p2.x += s * push
        }
        for (f in listOf(p1, p2)) if (f.spawnFlag) { f.spawnFlag = false; spawnProj(f) }
        melee(p1, p2); melee(p2, p1)
        updateProjs()
        timeLeft--
        if (p1.hp <= 0f || p2.hp <= 0f || timeLeft <= 0) endMatch()
    }

    private fun spawnProj(f: Fighter) {
        val st = f.hero.id % 3
        val sp = when (st) { 0 -> 13f; 1 -> 9f; else -> 16f }
        val r = when (st) { 0 -> 38f; 1 -> 52f; else -> 30f }
        // Meter boosts damage: 0.5x at empty → 1.5x at full meter
        val meterBonus = 0.5f + (f.meter / 100f).coerceIn(0f, 1f)
        val dmg = when (st) { 0 -> 20f; 1 -> 24f; else -> 16f } * f.dmgMul * meterBonus
        // Consume whatever meter is available
        f.meter = 0f
        projs.add(Proj(f.x + f.face * 70f, f.y - 140f, f.face * sp, f, dmg, r, f.hero.specialLabel, f.hero.specialColor, f.hero.id % 2 == 1))
        f.say(f.hero.shout, 70)
        Sfx.special()
    }

    private fun updateProjs() {
        val it = projs.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx; p.age++
            val yy = p.y + if (p.wob) sin(p.age * 0.2f) * 40f else 0f
            val target = if (p.owner === p1) p2 else p1
            if (phase == 1 && RectF.intersects(RectF(p.x - p.r, yy - p.r, p.x + p.r, yy + p.r), target.hurtRect())) {
                val blocked = target.takeHit(p.dmg, 14f, if (p.vx > 0) 1 else -1, true)
                onHit(p.owner, target, p.dmg, blocked, "KABOOM!")
                it.remove()
            } else if (p.x < -100f || p.x > W + 100f) it.remove()
        }
    }

    private fun melee(a: Fighter, b: Fighter) {
        if (!a.activeNow() || a.hitDone) return
        if (RectF.intersects(a.atkRect(), b.hurtRect())) {
            a.hitDone = true
            val kick = a.state == S.KICK
            val dmg = (if (kick) 10f else 6f) * a.dmgMul
            val kb = (if (kick) 11f else 6f) * (if (a.hero.ability == 1) 1.5f else 1f)
            val blocked = b.takeHit(dmg, kb, a.face, kick)
            onHit(a, b, dmg, blocked, if (kick) "WHAM!" else "BONK!")
            if (kick) Sfx.kick() else Sfx.punch()
        }
    }

    private fun onHit(a: Fighter, b: Fighter, dmg: Float, blocked: Boolean, word: String) {
        shake = if (blocked) 3f else 9f
        if (blocked) {
            floats.add(FT(b.x, b.y - 260f, "BLOCKED!", 40, 0xFF90CAF9.toInt()))
            Sfx.block()
        } else {
            if (a === p1) {
                comboTimer = 55
                comboCount++
                if (comboCount >= 2) {
                    floats.add(FT(b.x, b.y - 300f, "COMBO x$comboCount!", 46, 0xFFFF9800.toInt()))
                }
            }
            val hitWord = if (word == "KABOOM!") "SUPER HIT!" else word
            val col = if (word == "KABOOM!") 0xFFFFD700.toInt() else 0xFFFFEB3B.toInt()
            floats.add(FT(b.x, b.y - 240f, hitWord, 40, col))
        }
        a.meter = min(100f, a.meter + 8f * (if (a.hero.ability == 4) 1.6f else 1f))
        if (a.hero.ability == 2 && !blocked) a.hp = min(a.maxHp, a.hp + a.maxHp * 0.04f)
        if (a === p1 && !blocked) score += (dmg * 10f).toInt()
    }

    private fun endMatch() {
        phase = 2; phaseT = 0
        val d = p1.hp - p2.hp
        isDraw = abs(d) < 0.01f
        winnerP1 = d > 0f
        projs.clear()
        if (!isDraw) {
            val w = if (winnerP1) p1 else p2
            val l = if (winnerP1) p2 else p1
            w.state = S.WIN; w.say(w.hero.win, 260)
            if (l.state != S.KO) { l.state = S.KO; l.vy = -8f; l.vx = (if (w.x < l.x) 1 else -1) * 5f }
            l.say(l.hero.lose, 260)
        }
        val humanWon = winnerP1 && !isDraw
        if (cfg.mode != "two") { if (humanWon) Sfx.win() else Sfx.lose() } else Sfx.win()
        if (humanWon) score += (timeLeft / 60) * 50 + (p1.hp * 5f).toInt()
        if (score > Prefs.best) Prefs.best = score
        if (cfg.mode == "story" && humanWon) Prefs.unlocked = max(Prefs.unlocked, min(10, curLevel + 1))

        resultTitle = when {
            isDraw -> "DRAW!"
            cfg.mode == "two" -> if (winnerP1) "PLAYER 1 WINS!" else "PLAYER 2 WINS!"
            humanWon && cfg.mode == "story" && curLevel == 10 -> "CHAMPION OF THE ARENA!"
            humanWon && cfg.mode == "tourney" && tRound == 3 -> "TOURNAMENT CHAMPION!"
            humanWon -> "YOU WIN!"
            else -> "YOU LOSE!"
        }
        val opts = ArrayList<Pair<String, () -> Unit>>()
        when (cfg.mode) {
            "story" -> if (humanWon) { if (curLevel < 10) opts.add("NEXT LEVEL" to { curLevel++; setupMatch() }) }
            else opts.add("RETRY" to { setupMatch() })
            "tourney" -> if (humanWon) { if (tRound < 3) opts.add("NEXT ROUND" to { tRound++; setupMatch() }) }
            else opts.add("RESTART TOURNAMENT" to { tRound = 0; score = 0; initTournament(); setupMatch() })
            else -> opts.add("REMATCH" to { setupMatch() })
        }
        opts.add("MAIN MENU" to { exitNow() })
        endButtons = opts.mapIndexed { i, o ->
            OB(RectF(560f, 560f + i * 105f, 1040f, 650f + i * 105f), o.first, if (i == 0) 0xFF43A047.toInt() else 0xFFE53935.toInt(), o.second)
        }
    }

    // ---------------------------------------------------------------- input
    private fun toWorld(px: Float, py: Float) = Pair((px - ox) / sc, (py - oy) / sc)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val a = e.actionMasked
        val down = a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN
        if (paused || phase == 2) {
            if (down) {
                val i = e.actionIndex
                val (x, y) = toWorld(e.getX(i), e.getY(i))
                val list = if (paused) pauseButtons else endButtons
                for (b in list) if (b.r.contains(x, y)) { Sfx.click(); pending = b.act; break }
            }
            return true
        }
        if (down) {
            val i = e.actionIndex
            val (x, y) = toWorld(e.getX(i), e.getY(i))
            if (pauseRect.contains(x, y) && phase == 1) { paused = true; return true }
        }
        for (pl in held) java.util.Arrays.fill(pl, false)
        if (a != MotionEvent.ACTION_UP && a != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until e.pointerCount) {
                if (a == MotionEvent.ACTION_POINTER_UP && i == e.actionIndex) continue
                val (x, y) = toWorld(e.getX(i), e.getY(i))
                for (b in btns) if (b.r.contains(x, y)) held[b.player][b.code] = true
            }
        }
        return true
    }

    // ---------------------------------------------------------------- render
    private fun render(c: Canvas) {
        c.drawColor(Color.BLACK)
        c.save()
        c.translate(ox, oy); c.scale(sc, sc)
        c.clipRect(0f, 0f, W, H)
        c.save()
        if (shake > 0.5f) c.translate((rnd.nextFloat() - 0.5f) * shake * 2f, (rnd.nextFloat() - 0.5f) * shake * 2f)
        Arenas.draw(c, paint, arena, tm)
        val (back, front) = if (p1.y >= p2.y) Pair(p2, p1) else Pair(p1, p2)
        back.draw(c, paint); front.draw(c, paint)
        for (p in projs) drawProj(c, p)
        drawBubble(c, p1); drawBubble(c, p2)
        for (f in floats) drawOutlined(c, f.text, f.x, f.y, 56f, f.col)
        c.restore()
        drawHud(c)
        drawControls(c)
        drawNewsdesk(c)
        if (phase == 0) {
            drawOutlined(c, banner, W / 2, 260f, 52f, Color.WHITE)
            drawOutlined(c, if (phaseT < 95) "GET READY!" else "FIGHT!", W / 2, 360f, 96f, if (phaseT < 95) 0xFFFFEB3B.toInt() else 0xFFFF5252.toInt())
        }
        if (phase == 2 && phaseT > 40) drawEnd(c)
        if (paused) drawPause(c)
        c.restore()
    }

    private fun drawOutlined(c: Canvas, text: String, x: Float, y: Float, size: Float, col: Int) {
        paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = size
        paint.style = Paint.Style.STROKE; paint.strokeWidth = size / 7f; paint.color = 0xFF222222.toInt(); paint.strokeJoin = Paint.Join.ROUND
        c.drawText(text, x, y, paint)
        paint.style = Paint.Style.FILL; paint.color = col
        c.drawText(text, x, y, paint)
    }

    private fun drawProj(c: Canvas, p: Proj) {
        val yy = p.y + if (p.wob) sin(p.age * 0.2f) * 40f else 0f
        for (i in 1..3) {
            paint.style = Paint.Style.FILL; paint.color = (p.col and 0x00FFFFFF) or (0x55 / i shl 24)
            c.drawCircle(p.x - p.vx * i * 2.2f, yy, p.r * (1f - i * 0.18f), paint)
        }
        paint.style = Paint.Style.FILL; paint.color = p.col; c.drawCircle(p.x, yy, p.r, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f; paint.color = 0xFF222222.toInt(); c.drawCircle(p.x, yy, p.r, paint)
        val size = min(30f, p.r * 1.7f / (p.label.length * 0.6f))
        drawOutlined(c, p.label, p.x, yy + size / 3f, size, Color.WHITE)
    }

    private fun wrap(s: String, maxW: Float): List<String> {
        val out = ArrayList<String>(); var cur = ""
        for (w in s.split(" ")) {
            val t = if (cur.isEmpty()) w else "$cur $w"
            if (paint.measureText(t) > maxW && cur.isNotEmpty()) { out.add(cur); cur = w } else cur = t
        }
        if (cur.isNotEmpty()) out.add(cur)
        return out
    }

    private fun drawBubble(c: Canvas, f: Fighter) {
        if (f.bubbleT <= 0 || f.bubble.isEmpty()) return
        paint.textSize = 28f; paint.textAlign = Paint.Align.LEFT; paint.typeface = Typeface.DEFAULT_BOLD
        val lines = wrap(f.bubble, 320f)
        val lh = 34f
        val w = lines.maxOf { paint.measureText(it) } + 30f
        val h = lines.size * lh + 20f
        val bx = (f.x - w / 2f).coerceIn(10f, W - w - 10f)
        val by = max(220f, min(f.y, K.GROUND) - 290f - h)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        c.drawRoundRect(bx, by, bx + w, by + h, 22f, 22f, paint)
        val tail = Path(); val tx = f.x.coerceIn(bx + 25f, bx + w - 25f)
        tail.moveTo(tx - 14f, by + h - 2f); tail.lineTo(tx + 6f, by + h - 2f); tail.lineTo(tx, by + h + 24f); tail.close()
        c.drawPath(tail, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.color = 0xFF222222.toInt()
        c.drawRoundRect(bx, by, bx + w, by + h, 22f, 22f, paint)
        paint.style = Paint.Style.FILL; paint.color = 0xFF212121.toInt()
        for ((i, l) in lines.withIndex()) c.drawText(l, bx + 15f, by + 36f + i * lh, paint)
    }

    private fun bar(c: Canvas, x: Float, y: Float, w: Float, h: Float, frac: Float, col: Int, rightAlign: Boolean) {
        paint.style = Paint.Style.FILL; paint.color = 0xFF37474F.toInt(); c.drawRoundRect(x, y, x + w, y + h, 14f, 14f, paint)
        val fw = w * frac.coerceIn(0f, 1f)
        paint.color = col
        if (rightAlign) c.drawRoundRect(x + w - fw, y, x + w, y + h, 14f, 14f, paint) else c.drawRoundRect(x, y, x + fw, y + h, 14f, 14f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.color = 0xFF111111.toInt(); c.drawRoundRect(x, y, x + w, y + h, 14f, 14f, paint)
    }

    private fun hpColor(f: Float) = if (f > 0.5f) 0xFF66BB6A.toInt() else if (f > 0.25f) 0xFFFFCA28.toInt() else 0xFFEF5350.toInt()

    private fun drawHud(c: Canvas) {
        val f1 = p1.hp / p1.maxHp; val f2 = p2.hp / p2.maxHp
        bar(c, 40f, 40f, 620f, 46f, f1, hpColor(f1), false)
        bar(c, 940f, 40f, 620f, 46f, f2, hpColor(f2), true)
        val m1 = p1.meter / 100f; val m2 = p2.meter / 100f
        // SHAKTI meter bars - always visible, glow when full
        bar(c, 40f, 94f, 420f, 24f, m1, if (m1 >= 1f && (tm.toInt() / 6) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFFFD600.toInt(), false)
        bar(c, 1140f, 94f, 420f, 24f, m2, if (m2 >= 1f && (tm.toInt() / 6) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFFFD600.toInt(), true)
        // SHAKTI labels
        paint.textSize = 18f; paint.typeface = Typeface.DEFAULT_BOLD; paint.style = Paint.Style.FILL
        paint.setShadowLayer(3f, 0f, 0f, Color.BLACK)
        paint.textAlign = Paint.Align.LEFT; paint.color = 0xFFFFD600.toInt()
        c.drawText("SHAKTI", 44f, 108f, paint)
        paint.textAlign = Paint.Align.RIGHT
        c.drawText("SHAKTI", 1556f, 108f, paint)
        paint.textSize = 28f; paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        c.drawText((if (cfg.mode == "two") "P1 · " else "") + p1.hero.name + " (" + p1.hero.party + ")", 44f, 150f, paint)
        paint.textAlign = Paint.Align.RIGHT
        c.drawText((if (cfg.mode == "two") "P2 · " else "") + p2.hero.name + " (" + p2.hero.party + ")", 1556f, 150f, paint)
        // Show power level hint instead of "SPECIAL READY"
        if (m1 >= 1f) { paint.textAlign = Paint.Align.LEFT; paint.color = 0xFFFFEB3B.toInt(); c.drawText("MAX POWER! ${p1.hero.specialName}", 44f, 185f, paint) }
        if (m2 >= 1f && cfg.mode == "two") { paint.textAlign = Paint.Align.RIGHT; paint.color = 0xFFFFEB3B.toInt(); c.drawText("MAX POWER! ${p2.hero.specialName}", 1556f, 185f, paint) }
        paint.clearShadowLayer()
        drawOutlined(c, ceil(timeLeft / 60f).toInt().toString(), W / 2, 100f, 70f, Color.WHITE)
        if (cfg.mode != "two") drawOutlined(c, "SCORE $score", W / 2, 135f, 28f, 0xFFFFEB3B.toInt())
        if (phase == 1) {
            paint.style = Paint.Style.FILL; paint.color = 0x88000000.toInt(); c.drawRoundRect(pauseRect, 14f, 14f, paint)
            drawOutlined(c, "II", pauseRect.centerX(), pauseRect.centerY() + 12f, 34f, Color.WHITE)
        }
    }

    private fun drawControls(c: Canvas) {
        for (b in btns) {
            val pr = held[b.player][b.code]
            // Special button glows when meter is full
            val isSpecial = b.code == 5
            val alpha = if (pr) 0xE6 else if (isSpecial) 0xCC else 0x8C
            val col = if (isSpecial && p1.meter >= 100f && (tm.toInt() / 4) % 2 == 0) 0xFFFFD600.toInt() else b.col
            paint.style = Paint.Style.FILL; paint.color = (col and 0x00FFFFFF) or (alpha shl 24)
            c.drawRoundRect(b.r, 30f, 30f, paint)
            val borderCol = if (isSpecial && p1.meter >= 100f) 0xFFFFEB3B.toInt() else 0xCCFFFFFF.toInt()
            paint.style = Paint.Style.STROKE; paint.strokeWidth = if (isSpecial) 7f else 5f; paint.color = borderCol; c.drawRoundRect(b.r, 30f, 30f, paint)
            val s = b.r.width()
            val size = if (b.label.length > 3) s * 0.2f else s * 0.4f
            drawOutlined(c, b.label, b.r.centerX(), b.r.centerY() + size / 3f, size, Color.WHITE)
        }
    }

    private fun drawNewsdesk(c: Canvas) {
        val ny = H - 36f
        paint.style = Paint.Style.FILL; paint.color = 0xEE120A06.toInt()
        c.drawRect(0f, ny, W, H, paint)
        paint.color = 0xFFD50000.toInt()
        c.drawRect(0f, ny - 3f, W, ny, paint)

        // Live Badge
        paint.color = 0xFFD50000.toInt()
        c.drawRoundRect(RectF(12f, ny + 4f, 96f, H - 4f), 6f, 6f, paint)
        paint.textSize = 18f; paint.typeface = Typeface.DEFAULT_BOLD; paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        c.drawText("● LIVE", 54f, ny + 20f, paint)

        // Anchor Channels
        paint.textSize = 15f; paint.textAlign = Paint.Align.LEFT; paint.color = 0xFFFF9800.toInt()
        c.drawText("BREAKING BAKWAS · SHOR SHARMA", 112f, ny + 20f, paint)
        paint.textAlign = Paint.Align.RIGHT; paint.color = 0xFF42A5F5.toInt()
        c.drawText("TROLL ARMY · TWEETY TROLLWALA", W - 14f, ny + 20f, paint)

        // Satirical Scrolling Ticker
        val scroll = (tm * 2.8f) % (W + 1400f)
        val hIdx = ((tm / 400).toInt()) % tickerHeadlines.size
        paint.textSize = 18f; paint.color = 0xFFFFEB3B.toInt(); paint.textAlign = Paint.Align.LEFT
        c.drawText(tickerHeadlines[hIdx], W - scroll, ny - 8f, paint)
    }

    private fun drawButtons(c: Canvas, list: List<OB>) {
        for (b in list) {
            paint.style = Paint.Style.FILL; paint.color = b.col; c.drawRoundRect(b.r, 40f, 40f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 6f; paint.color = 0xFF222222.toInt(); c.drawRoundRect(b.r, 40f, 40f, paint)
            drawOutlined(c, b.label, b.r.centerX(), b.r.centerY() + 14f, 44f, Color.WHITE)
        }
    }

    private fun drawEnd(c: Canvas) {
        paint.style = Paint.Style.FILL; paint.color = 0xAA000000.toInt(); c.drawRect(0f, 0f, W, H, paint)
        drawOutlined(c, resultTitle, W / 2, 300f, 100f, 0xFFFFEB3B.toInt())
        if (cfg.mode != "two") {
            drawOutlined(c, "SCORE: $score     BEST: ${Prefs.best}", W / 2, 390f, 44f, Color.WHITE)
            if (cfg.mode == "story" && winnerP1 && !isDraw && curLevel < 10) drawOutlined(c, "LEVEL ${curLevel + 1} UNLOCKED!", W / 2, 460f, 40f, 0xFF69F0AE.toInt())
        }
        drawButtons(c, endButtons)
    }

    private fun drawPause(c: Canvas) {
        paint.style = Paint.Style.FILL; paint.color = 0xCC000000.toInt(); c.drawRect(0f, 0f, W, H, paint)
        drawOutlined(c, "PAUSED", W / 2, 300f, 100f, Color.WHITE)
        drawButtons(c, pauseButtons)
    }
}
