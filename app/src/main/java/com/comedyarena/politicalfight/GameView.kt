package com.comedyarena.politicalfight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** mode: story | quick | party | tourney | two | online */
class Cfg(
    val mode: String,
    val p1: Int,
    val p2: Int,
    val level: Int,
    val arena: Int = 0,
    val isHost: Boolean = true,
    val roomCode: String = ""
)

private class Proj(var x: Float, var y: Float, val vx: Float, val owner: Fighter, val dmg: Float,
                   val r: Float, val label: String, val col: Int, val wob: Boolean) { var age = 0 }
private class FT(var x: Float, var y: Float, val text: String, var life: Int, val col: Int)
private class Btn(val r: RectF, val label: String, val player: Int, val code: Int, val col: Int)
private class OB(val r: RectF, val label: String, val col: Int, val act: () -> Unit)

class GameView(ctx: Context, private val cfg: Cfg, private val onExit: () -> Unit) :
    SurfaceView(ctx), SurfaceHolder.Callback, Runnable {

    private var W = 1600f
    private val H = 900f
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
    private var headline = ""
    private var comboCount = 0
    private var comboTimer = 0
    private var currentRound = 1

    private val hitWords = listOf("THOK!", "DHISHUM!", "OYE!", "ARRE!", "BAM!", "WAH!", "PATAK!")
    private val dailyHeadlines = listOf(
        "Sources say the chai vendor demanded compensation for broken cups during timeout.",
        "A stray cow paused the fight, then ate a political placard.",
        "Exit polls, as usual, were completely wrong by 200%.",
        "Parliament adjourned after a heated debate over samosa size.",
        "Opposition walks out; ruling side walks out to follow them.",
        "Referee fined for blowing whistle without Fastag approval.",
        "Breaking: Flying dropkick declared 100% locally manufactured and organic."
    )

    private val tickerHeadlines = listOf(
        "PARLIAMENT ADJOURNED: Ring-side samosa crisis reported · OPPOSITION CALLS FOR BAN ON DROPKICKS ·",
        "FAKE POLL: 1P 54% · CPU 62% · OTHERS 12% · TOTAL 128% (Margin of error ±100%) ·",
        "BREAKING: Flying dropkick declared 100% locally manufactured and organic ·",
        "EXCLUSIVE: Ring-side tea stall reports historic drop in chai prices during timeout ·",
        "ASTROLOGY ALERT: Special moves affected by retrograde Saturn in third round ·"
    )

    // held booleans: 0 Left, 1 Right, 2 Jump, 3 Punch, 4 Kick, 5 Special, 6 Block, 7 Crouch, 8 Grab
    private val held = Array(2) { BooleanArray(9) }
    private val btns = ArrayList<Btn>()
    private var endButtons = listOf<OB>()
    private var pauseButtons = listOf<OB>()
    @Volatile private var pending: (() -> Unit)? = null
    private var pauseRect = RectF(760f, 130f, 840f, 180f)

    private val avatarCache = HashMap<String, Bitmap?>()
    private fun getAvatar(name: String): Bitmap? {
        if (avatarCache.containsKey(name)) return avatarCache[name]
        val bmp = try {
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
        } catch (e: Exception) { null }
        avatarCache[name] = bmp
        return bmp
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        initTournament()
        setupMatch()
        buildControls()
        updatePauseButtons()
    }

    private fun updatePauseButtons() {
        val midX = W / 2f
        pauseButtons = listOf(
            OB(RectF(midX - 240f, 380f, midX + 240f, 470f), "RESUME", 0xFF43A047.toInt()) { paused = false },
            OB(RectF(midX - 240f, 500f, midX + 240f, 590f), "MAIN MENU", 0xFFE53935.toInt()) { exitNow() })
    }

    private fun exitNow() { post { onExit() } }

    // ---------------------------------------------------------------- setup
    private fun initTournament() {
        val pool = (0 until heroes.size).filter { it != cfg.p1 }.shuffled(rnd)
        tOpps = IntArray(4) { pool[it % pool.size] }
    }

    private fun setupMatch() {
        projs.clear(); floats.clear()
        phase = 0; phaseT = 0; timeLeft = 3600; shake = 0f; paused = false; endButtons = listOf()
        currentRound = 1
        val h1 = heroes[cfg.p1]
        arena = cfg.arena.coerceIn(0, 2)

        when (cfg.mode) {
            "story" -> {
                val lv = Levels.getLevel(curLevel)
                var o = if (lv.opp < 0) (cfg.p1 + 2 + rnd.nextInt(5)) % heroes.size else lv.opp
                if (o == cfg.p1) o = (o + 1) % heroes.size
                arena = lv.arena
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[o], W - 380f, true, lv.diff, lv.hpMul)
                banner = "LEVEL $curLevel: ${lv.title.uppercase()}"
            }
            "tourney" -> {
                val o = tOpps[tRound]
                arena = (tRound * 2 + 1) % 3
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[o], W - 380f, true, 0.35f + 0.17f * tRound, 1f + 0.1f * tRound)
                banner = "TOURNAMENT ROUND ${tRound + 1}/4"
            }
            "party" -> {
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[cfg.p2], W - 380f, true, 0.55f, 1f)
                banner = "${h1.party.uppercase()} VS ${heroes[cfg.p2].party.uppercase()}"
            }
            "two" -> {
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[cfg.p2], W - 380f, false)
                banner = "TWO PLAYER BATTLE"
            }
            "online" -> {
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[cfg.p2], W - 380f, false)
                banner = "ONLINE MATCH · ROOM ${cfg.roomCode}"
            }
            else -> {
                var o = rnd.nextInt(heroes.size); if (o == cfg.p1) o = (o + 1) % heroes.size
                p1 = Fighter(h1, 380f, false)
                p2 = Fighter(heroes[o], W - 380f, true, Prefs.cpuDiff, 1f)
                banner = "QUICK FIGHT"
            }
        }
        p2.face = -1
        p1.stageW = W
        p2.stageW = W

        // Special attack always 100% full & ready
        p1.meter = 100f
        p2.meter = 100f
    }

    private fun buildControls() {
        btns.clear()
        fun add(pl: Int, code: Int, label: String, r: RectF, col: Int) {
            btns.add(Btn(r, label, pl, code, col))
        }
        val midX = W / 2f
        pauseRect = RectF(midX - 45f, 125f, midX + 45f, 175f)

        if (cfg.mode != "two") {
            // --- LEFT SIDE: 2x2 D-PAD Movement (0% Overlap) ---
            // Row 1: y = 585f..700f; Row 2: y = 720f..835f (height = 115f, gap = 20f)
            // Col 1: x = 35f..160f; Col 2: x = 178f..303f (width = 125f, gap = 18f)
            add(0, 2, "JUMP",   RectF(35f, 585f, 160f, 700f), 0xFF66BB6A.toInt()) // Up
            add(0, 7, "CROUCH", RectF(178f, 585f, 303f, 700f), 0xFF5C6BC0.toInt()) // Down
            add(0, 0, "◀",      RectF(35f, 720f, 160f, 835f), 0xFF42A5F5.toInt()) // Left
            add(0, 1, "▶",      RectF(178f, 720f, 303f, 835f), 0xFF42A5F5.toInt()) // Right

            // --- RIGHT SIDE: Combat Actions (2 Rows, 3 Columns, 0% Overlap) ---
            // Row 1: y = 585f..700f; Row 2: y = 720f..835f (height = 115f, gap = 20f)
            // Col 1: W-385..W-275; Col 2: W-260..W-150; Col 3: W-135..W-25 (width = 110f, gap = 15f)
            // Col 1, Row 1 is kept empty so it never covers the fighter!
            add(0, 8, "GRAB",    RectF(W - 260f, 585f, W - 150f, 700f), 0xFF8D6E63.toInt())
            add(0, 5, "SPECIAL", RectF(W - 135f, 585f, W - 25f,  700f), 0xFFAB47BC.toInt())

            add(0, 6, "BLOCK",   RectF(W - 385f, 720f, W - 275f, 835f), 0xFF78909C.toInt())
            add(0, 3, "PUNCH",   RectF(W - 260f, 720f, W - 150f, 835f), 0xFFEF5350.toInt())
            add(0, 4, "KICK",    RectF(W - 135f, 720f, W - 25f,  835f), 0xFFFFA726.toInt())
        } else {
            val s = 95f; val st = 105f
            for (pl in 0..1) {
                val bx = if (pl == 0) 24f else W - 24f - (4 * st + s)
                val y1 = 730f; val y2 = 615f
                add(pl, 0, "◀", RectF(bx, y1, bx + s, y1 + s), 0xFF42A5F5.toInt())
                add(pl, 1, "▶", RectF(bx + st, y1, bx + st + s, y1 + s), 0xFF42A5F5.toInt())
                add(pl, 2, "JUMP", RectF(bx + 2 * st, y1, bx + 2 * st + s, y1 + s), 0xFF66BB6A.toInt())
                add(pl, 7, "DOWN", RectF(bx + 3 * st, y1, bx + 3 * st + s, y1 + s), 0xFF5C6BC0.toInt())
                add(pl, 6, "BLOCK", RectF(bx + 4 * st, y1, bx + 4 * st + s, y1 + s), 0xFF78909C.toInt())

                add(pl, 3, "PUNCH", RectF(bx, y2, bx + s, y2 + s), 0xFFEF5350.toInt())
                add(pl, 4, "KICK", RectF(bx + st, y2, bx + st + s, y2 + s), 0xFFFFA726.toInt())
                add(pl, 8, "GRAB", RectF(bx + 2 * st, y2, bx + 2 * st + s, y2 + s), 0xFF8D6E63.toInt())
                add(pl, 5, "SPECIAL", RectF(bx + 3 * st, y2, bx + 3 * st + s, y2 + s), 0xFFAB47BC.toInt())
            }
        }
    }

    // ---------------------------------------------------------------- lifecycle
    override fun surfaceCreated(h: SurfaceHolder) { running = true; thread = Thread(this).also { it.start() } }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
        if (hh > 0 && w > 0) {
            sc = hh / 900f
            W = max(1600f, w / sc)
            ox = 0f
            oy = 0f
            if (::p1.isInitialized) p1.stageW = W
            if (::p2.isInitialized) p2.stageW = W
            buildControls()
            updatePauseButtons()
        }
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

        if (cfg.mode == "online") {
            if (cfg.isHost) {
                System.arraycopy(held[0], 0, p1.held, 0, 9)
            } else {
                System.arraycopy(held[0], 0, p2.held, 0, 9)
            }
            // Send inputs to Firebase Realtime Database
            if (tm.toInt() % 6 == 0 && cfg.roomCode.isNotEmpty()) {
                val myHeld = if (cfg.isHost) p1.held else p2.held
                var bits = 0
                for (k in 0..8) if (myHeld[k]) bits = bits or (1 shl k)
                val myHp = if (cfg.isHost) p1.hp else p2.hp
                FirebaseMultiplayer.sendInputs(cfg.roomCode, cfg.isHost, bits, myHp)
            }
        } else if (cfg.mode == "two") {
            System.arraycopy(held[0], 0, p1.held, 0, 9)
            System.arraycopy(held[1], 0, p2.held, 0, 9)
        } else {
            System.arraycopy(held[0], 0, p1.held, 0, 9)
            p2.think(p1)
        }

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
        if (p1.hp <= 0f || p2.hp <= 0f || timeLeft <= 0) endRoundOrMatch()
    }

    private fun spawnProj(f: Fighter) {
        val st = f.hero.id % 3
        val sp = when (st) { 0 -> 14f; 1 -> 11f; else -> 17f }
        val r = when (st) { 0 -> 42f; 1 -> 56f; else -> 35f }
        val dmg = when (st) { 0 -> 22f; 1 -> 26f; else -> 18f } * f.dmgMul * 1.5f
        f.meter = 100f // Keep Special mode permanently enabled
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
            val isGrab = a.state == S.GRAB
            val isUpper = a.state == S.UPPER
            val isSweep = a.state == S.SWEEP
            val isKick = a.state == S.KICK

            val dmg = when {
                isGrab -> 14f
                isUpper -> 10f
                isSweep -> 8f
                isKick -> 11f
                else -> 6f + a.punchChain * 2.5f
            } * a.dmgMul

            val kb = when {
                isGrab -> 16f
                isUpper -> 6f
                isSweep -> 10f
                isKick -> 12f
                else -> 7f + a.punchChain * 3f
            } * (if (a.hero.ability == 1) 1.5f else 1f)

            val blocked = b.takeHit(dmg, kb, a.face, isGrab || isUpper || isSweep, isGrab)

            val word = when {
                isGrab -> "PATAK!"
                isUpper -> "UPPERCUT!"
                isSweep -> "SWEEP!"
                else -> hitWords[rnd.nextInt(hitWords.size)]
            }
            onHit(a, b, dmg, blocked, word)
            if (isKick || isSweep) Sfx.kick() else Sfx.punch()
        }
    }

    private fun onHit(a: Fighter, b: Fighter, dmg: Float, blocked: Boolean, word: String) {
        shake = if (blocked) 3f else 9f
        if (blocked) {
            floats.add(FT(b.x, b.y - 260f, "BLOCKED!", 40, 0xFF90CAF9.toInt()))
            Sfx.block()
        } else {
            if (a === p1) {
                comboTimer = 60
                comboCount++
                if (comboCount >= 2) {
                    floats.add(FT(b.x, b.y - 300f, "COMBO x$comboCount!", 46, 0xFFFF9800.toInt()))
                }
            }
            val hitWord = if (word == "KABOOM!") "SUPER HIT!" else word
            val col = if (word == "KABOOM!" || word == "PATAK!") 0xFFFFD700.toInt() else 0xFFFFEB3B.toInt()
            floats.add(FT(b.x, b.y - 240f, hitWord, 40, col))
        }
        a.meter = 100f // Keep Special fully enabled
        if (a.hero.ability == 2 && !blocked) a.hp = min(a.maxHp, a.hp + a.maxHp * 0.04f)
        if (a === p1 && !blocked) score += (dmg * 10f).toInt()
    }

    private fun endRoundOrMatch() {
        if (p1.hp > p2.hp) p1.wins++ else if (p2.hp > p1.hp) p2.wins++

        // Best of 3 Rounds: First to 2 wins takes the match (Story mode ends on 1 round)
        if (p1.wins >= 2 || p2.wins >= 2 || currentRound >= 3 || cfg.mode == "story") {
            endMatch()
        } else {
            currentRound++
            banner = if (currentRound == 2) "ROUND 2" else "FINAL ROUND"
            phase = 0
            phaseT = 0
            timeLeft = 3600
            p1.hp = p1.maxHp
            p2.hp = p2.maxHp
            p1.x = 380f
            p2.x = W - 380f
            p1.state = S.IDLE
            p2.state = S.IDLE
            p2.face = -1
        }
    }

    private fun endMatch() {
        phase = 2; phaseT = 0
        val d = p1.wins - p2.wins
        isDraw = d == 0
        winnerP1 = if (d != 0) d > 0 else p1.hp >= p2.hp
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
        if (cfg.mode == "story" && humanWon) Prefs.unlocked = max(Prefs.unlocked, min(500, curLevel + 1))

        headline = dailyHeadlines[rnd.nextInt(dailyHeadlines.size)]

        resultTitle = when {
            isDraw -> "DRAW!"
            cfg.mode == "online" -> if ((winnerP1 && cfg.isHost) || (!winnerP1 && !cfg.isHost)) "YOU WIN!" else "YOU LOSE!"
            cfg.mode == "two" -> if (winnerP1) "PLAYER 1 WINS!" else "PLAYER 2 WINS!"
            humanWon && cfg.mode == "story" && curLevel == 500 -> "THE ULTIMATE GRAND CHAMPION!"
            humanWon && cfg.mode == "tourney" && tRound == 3 -> "TOURNAMENT CHAMPION!"
            humanWon -> "YOU WIN!"
            else -> "YOU LOSE!"
        }
        val opts = ArrayList<Pair<String, () -> Unit>>()
        when (cfg.mode) {
            "story" -> if (humanWon) { if (curLevel < 500) opts.add("NEXT LEVEL" to { curLevel++; setupMatch() }) }
            else opts.add("RETRY" to { setupMatch() })
            "tourney" -> if (humanWon) { if (tRound < 3) opts.add("NEXT ROUND" to { tRound++; setupMatch() }) }
            else opts.add("RESTART TOURNAMENT" to { tRound = 0; score = 0; initTournament(); setupMatch() })
            else -> opts.add("REMATCH" to { setupMatch() })
        }
        opts.add("MAIN MENU" to { exitNow() })
        val midX = W / 2f
        endButtons = opts.mapIndexed { i, o ->
            OB(RectF(midX - 240f, 560f + i * 105f, midX + 240f, 650f + i * 105f), o.first, if (i == 0) 0xFF43A047.toInt() else 0xFFE53935.toInt(), o.second)
        }
    }

    // ---------------------------------------------------------------- input & keyboard
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return handleKey(keyCode, true) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return handleKey(keyCode, false) || super.onKeyUp(keyCode, event)
    }

    private fun handleKey(keyCode: Int, down: Boolean): Boolean {
        val code = when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> 0
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> 1
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> 2
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> 7
            KeyEvent.KEYCODE_J -> 3
            KeyEvent.KEYCODE_K -> 4
            KeyEvent.KEYCODE_I -> 8
            KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_SPACE -> 5
            KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> 6
            KeyEvent.KEYCODE_ESCAPE -> { if (down) togglePause(); return true }
            else -> -1
        }
        if (code >= 0) {
            held[0][code] = down
            return true
        }
        return false
    }

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
                for (b in btns) {
                    if (b.r.contains(x, y)) held[b.player][b.code] = true
                }
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
        Arenas.draw(c, paint, arena, tm, W)
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
            drawOutlined(c, banner, W / 2f, 260f, 52f, Color.WHITE)
            drawOutlined(c, if (phaseT < 95) "GET READY!" else "FIGHT!", W / 2f, 360f, 96f, if (phaseT < 95) 0xFFFFEB3B.toInt() else 0xFFFF5252.toInt())
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
        paint.style = Paint.Style.FILL; paint.color = 0xFF2B201A.toInt(); c.drawRoundRect(x, y, x + w, y + h, 10f, 10f, paint)
        val fw = w * frac.coerceIn(0f, 1f)
        paint.color = col
        if (rightAlign) c.drawRoundRect(x + w - fw, y, x + w, y + h, 10f, 10f, paint) else c.drawRoundRect(x, y, x + fw, y + h, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = 0xFF111111.toInt(); c.drawRoundRect(x, y, x + w, y + h, 10f, 10f, paint)
    }

    private fun hpColor(f: Float) = if (f > 0.5f) 0xFFFFB300.toInt() else if (f > 0.25f) 0xFFFF9800.toInt() else 0xFFEF5350.toInt()

    private fun drawHud(c: Canvas) {
        val f1 = p1.hp / p1.maxHp; val f2 = p2.hp / p2.maxHp

        // --- P1 Avatar Portrait Box (Left) ---
        val r1 = RectF(30f, 28f, 122f, 120f)
        paint.style = Paint.Style.FILL; paint.color = 0xFF201612.toInt(); c.drawRoundRect(r1, 14f, 14f, paint)
        c.save()
        val clipPath1 = Path().apply { addRoundRect(r1, 14f, 14f, Path.Direction.CW) }
        c.clipPath(clipPath1)
        p1.drawHeadInBox(c, paint, r1.centerX(), r1.centerY() + 4f, 0.82f, facingLeft = false)
        c.restore()
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.color = 0xFFFF9800.toInt()
        c.drawRoundRect(r1, 14f, 14f, paint)
        // 1P Badge
        paint.style = Paint.Style.FILL; paint.color = 0xFFE65100.toInt()
        c.drawRoundRect(RectF(26f, 22f, 64f, 44f), 6f, 6f, paint)
        paint.color = Color.WHITE; paint.textSize = 14f; paint.typeface = Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
        c.drawText(if (cfg.mode == "two") "1P" else "YOU", 45f, 38f, paint)

        // --- P2 Avatar Portrait Box (Right) ---
        val r2 = RectF(W - 122f, 28f, W - 30f, 120f)
        paint.style = Paint.Style.FILL; paint.color = 0xFF201612.toInt(); c.drawRoundRect(r2, 14f, 14f, paint)
        c.save()
        val clipPath2 = Path().apply { addRoundRect(r2, 14f, 14f, Path.Direction.CW) }
        c.clipPath(clipPath2)
        p2.drawHeadInBox(c, paint, r2.centerX(), r2.centerY() + 4f, 0.82f, facingLeft = true)
        c.restore()
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f; paint.color = 0xFF03A9F4.toInt()
        c.drawRoundRect(r2, 14f, 14f, paint)
        // CPU/2P Badge
        paint.style = Paint.Style.FILL; paint.color = 0xFF0288D1.toInt()
        c.drawRoundRect(RectF(W - 64f, 22f, W - 26f, 44f), 6f, 6f, paint)
        paint.color = Color.WHITE; paint.textSize = 14f; paint.typeface = Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
        c.drawText(if (cfg.mode == "two") "2P" else "CPU", W - 45f, 38f, paint)

        // --- Health Bars ---
        val barW = min(540f, W / 2f - 240f)
        bar(c, 134f, 34f, barW, 40f, f1, hpColor(f1), false)
        bar(c, W - 134f - barW, 34f, barW, 40f, f2, hpColor(f2), true)

        // --- Round Win Circles (Best of 3) ---
        for (i in 0 until 2) {
            paint.style = Paint.Style.FILL
            paint.color = if (i < p1.wins) 0xFFFFD700.toInt() else 0x55000000
            c.drawCircle(146f + i * 20f, 84f, 7f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = 0xFFFFD700.toInt()
            c.drawCircle(146f + i * 20f, 84f, 7f, paint)

            paint.style = Paint.Style.FILL
            paint.color = if (i < p2.wins) 0xFFFFD700.toInt() else 0x55000000
            c.drawCircle(W - 146f - i * 20f, 84f, 7f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = 0xFFFFD700.toInt()
            c.drawCircle(W - 146f - i * 20f, 84f, 7f, paint)
        }

        // --- Fighter Names ---
        paint.style = Paint.Style.FILL; paint.textSize = 22f; paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.LEFT; paint.color = 0xFFFFEB3B.toInt()
        val p1Name = when (cfg.mode) {
            "online" -> "${p1.hero.name} " + if (cfg.isHost) "(YOU)" else "(FRIEND)"
            "two" -> "P1 · ${p1.hero.name}"
            else -> "${p1.hero.name} (YOU)"
        }
        c.drawText(p1Name, 194f, 88f, paint)

        paint.textAlign = Paint.Align.RIGHT; paint.color = 0xFF81D4FA.toInt()
        val p2Name = when (cfg.mode) {
            "online" -> "${p2.hero.name} " + if (!cfg.isHost) "(YOU)" else "(FRIEND)"
            "two" -> "P2 · ${p2.hero.name}"
            else -> "${p2.hero.name} (CPU)"
        }
        c.drawText(p2Name, W - 194f, 88f, paint)

        // --- SHAKTI Meter Bars ---
        val meterW = min(360f, barW - 60f)
        val m1 = p1.meter / 100f; val m2 = p2.meter / 100f
        bar(c, 134f, 98f, meterW, 20f, m1, 0xFFFFD700.toInt(), false)
        bar(c, W - 134f - meterW, 98f, meterW, 20f, m2, 0xFFFFD700.toInt(), true)

        // --- SHAKTI Labels & Special Ready Glow ---
        paint.textSize = 15f; paint.textAlign = Paint.Align.LEFT; paint.color = 0xFFFFD600.toInt()
        c.drawText("SHAKTI", 138f, 114f, paint)
        paint.textAlign = Paint.Align.RIGHT
        c.drawText("SHAKTI", W - 138f, 114f, paint)

        // "SPECIAL READY" Always Glowing!
        val glowCol = if ((tm.toInt() / 6) % 2 == 0) 0xFFFFEB3B.toInt() else 0xFFFF9800.toInt()
        paint.textSize = 20f; paint.textAlign = Paint.Align.LEFT; paint.color = glowCol
        c.drawText("★ SPECIAL READY: ${p1.hero.specialLabel} ★", 134f, 142f, paint)

        if (cfg.mode == "two") {
            paint.textAlign = Paint.Align.RIGHT
            c.drawText("★ SPECIAL READY: ${p2.hero.specialLabel} ★", W - 134f, 142f, paint)
        }

        // --- Timer (Center) ---
        val midX = W / 2f
        drawOutlined(c, ceil(timeLeft / 60f).toInt().toString(), midX, 86f, 66f, Color.WHITE)
        if (cfg.mode != "two") drawOutlined(c, "SCORE $score", midX, 118f, 24f, 0xFFFFEB3B.toInt())

        // Pause Button
        if (phase == 1) {
            paint.style = Paint.Style.FILL; paint.color = 0xAA000000.toInt(); c.drawRoundRect(pauseRect, 12f, 12f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = 0xFFFFD700.toInt(); c.drawRoundRect(pauseRect, 12f, 12f, paint)
            drawOutlined(c, "II", pauseRect.centerX(), pauseRect.centerY() + 11f, 30f, Color.WHITE)
        }
    }

    private fun drawControls(c: Canvas) {
        for (b in btns) {
            val pr = held[b.player][b.code]
            val isSpecial = b.code == 5
            val alpha = if (pr) 0xF5 else if (isSpecial) 0xDD else 0xA0

            // SPECIAL button always glows gold and stands out
            val col = if (isSpecial) {
                if ((tm.toInt() / 6) % 2 == 0) 0xFFFFB300.toInt() else 0xFF9C27B0.toInt()
            } else b.col

            paint.style = Paint.Style.FILL; paint.color = (col and 0x00FFFFFF) or (alpha shl 24)
            c.drawRoundRect(b.r, 22f, 22f, paint)

            val borderCol = if (isSpecial) 0xFFFFD700.toInt() else if (pr) Color.WHITE else 0xCCFFFFFF.toInt()
            paint.style = Paint.Style.STROKE; paint.strokeWidth = if (isSpecial) 5f else if (pr) 4f else 3f; paint.color = borderCol
            c.drawRoundRect(b.r, 22f, 22f, paint)

            val s = b.r.width()
            val size = if (b.label.length > 5) s * 0.19f else if (b.label.length > 3) s * 0.24f else if (b.label.length > 1) s * 0.28f else s * 0.42f
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
        drawOutlined(c, resultTitle, W / 2f, 260f, 96f, 0xFFFFEB3B.toInt())

        // Satirical Newspaper Headline ("THE DAILY JUMLA")
        drawOutlined(c, "THE DAILY JUMLA", W / 2f, 330f, 30f, 0xFF9AD1FF.toInt())
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.textSize = 22f; paint.textAlign = Paint.Align.CENTER
        c.drawText(headline, W / 2f, 375f, paint)

        if (cfg.mode != "two") {
            drawOutlined(c, "SCORE: $score     BEST: ${Prefs.best}", W / 2f, 430f, 38f, Color.WHITE)
            if (cfg.mode == "story" && winnerP1 && !isDraw && curLevel < 500) {
                drawOutlined(c, "LEVEL ${curLevel + 1} UNLOCKED!", W / 2f, 480f, 34f, 0xFF69F0AE.toInt())
            }
        }
        drawButtons(c, endButtons)
    }

    private fun drawPause(c: Canvas) {
        paint.style = Paint.Style.FILL; paint.color = 0xCC000000.toInt(); c.drawRect(0f, 0f, W, H, paint)
        drawOutlined(c, "PAUSED", W / 2f, 300f, 96f, Color.WHITE)
        drawButtons(c, pauseButtons)
    }
}
