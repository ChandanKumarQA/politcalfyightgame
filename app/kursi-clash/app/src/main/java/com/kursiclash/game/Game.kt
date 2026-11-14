package com.kursiclash.game

import android.graphics.RectF
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Scr { TITLE, SELECT, PLAY_WITH_FRIEND, HOST_ROOM, INTRO, FIGHT, ROUNDEND, RESULT }

/** All game rules. Not thread-safe: callers synchronize on the instance. */
class Game {
    var scr = Scr.TITLE
    var frame = 0
    val rnd = Random()

    // Selection
    var sel1 = 0
    var sel2 = 1
    var pickPhase = 0
    var stage = 0
    var cpuLevel = 1

    // Multiplayer & Room
    var hostRoomCode = "F5VQ"
    var enteredCode = ""
    var isMultiplayer = false
    var isHost = true

    // Pause
    var paused = false

    // Match
    lateinit var p1: Fighter
    lateinit var p2: Fighter
    var round = 1
    var timer = 0
    var introT = 0
    var endT = 0
    var hitstop = 0
    var shake = 0f
    var banner = ""
    var resultWin = false
    var headline = ""
    val projs = ArrayList<Proj>()
    val pops = ArrayList<Pop>()

    // Input
    val in1 = Input()
    val heldTouch = BooleanArray(8)
    val heldKey = BooleanArray(8)
    private fun held(b: Int) = heldTouch[b] || heldKey[b]
    private var ai = Ai(1)

    // Callbacks to Activity / System
    var onShareWhatsApp: ((code: String) -> Unit)? = null
    var onCopyLink: ((code: String) -> Unit)? = null
    var onInputCode: ((callback: (String) -> Unit) -> Unit)? = null
    var onToast: ((msg: String) -> Unit)? = null

    // Character Card Rectangles (13 cards in horizontal row)
    fun cardRect(i: Int): RectF {
        val cardW = 88f
        val cardH = 125f
        val gap = 8f
        val startX = 20f
        val startY = 135f
        val x = startX + i * (cardW + gap)
        return RectF(x, startY, x + cardW, startY + cardH)
    }

    // Stage Selection Buttons
    val btnStage0 = RectF(340f, 290f, 510f, 350f)
    val btnStage1 = RectF(530f, 290f, 750f, 350f)
    val btnStage2 = RectF(770f, 290f, 960f, 350f)

    // CPU Selection Buttons
    val btnCpu0 = RectF(470f, 375f, 590f, 430f)
    val btnCpu1 = RectF(610f, 375f, 740f, 430f)
    val btnCpu2 = RectF(760f, 375f, 880f, 430f)

    // Bottom Selection Buttons
    val btnFriend = RectF(120f, 620f, 580f, 695f)
    val btnFight = RectF(620f, 620f, 1160f, 695f)

    // Play With Friend Screen Rectangles
    val btnHostRoom = RectF(440f, 260f, 840f, 335f)
    val btnCodeInput = RectF(450f, 410f, 630f, 480f)
    val btnJoinRoom = RectF(650f, 410f, 830f, 480f)
    val btnBackPwf = RectF(550f, 550f, 730f, 610f)

    // Host Room Screen Rectangles
    val btnShareWhatsApp = RectF(410f, 385f, 660f, 445f)
    val btnCopyLink = RectF(680f, 385f, 870f, 445f)
    val btnBackHost = RectF(550f, 560f, 730f, 620f)

    // Pause Dialog Buttons
    val btnPause = RectF(605f, 80f, 675f, 120f)
    val btnResume = RectF(480f, 260f, 800f, 330f)
    val btnRestart = RectF(480f, 350f, 800f, 420f)
    val btnQuitSelect = RectF(480f, 440f, 800f, 510f)

    companion object {
        val stageNames = arrayOf("SANSAD", "JANTAR MANTAR", "DESI SADAK")
        val levelNames = arrayOf("EASY", "NORMAL", "HARD")
        val hitWords = arrayOf("THOK!", "DHISHUM!", "OYE!", "ARRE!", "BAM!", "WAH!")
        val headlines = arrayOf(
            "Sources say the chai vendor demanded compensation for broken cups.",
            "A stray cow paused the fight, then ate a placard.",
            "Exit polls, as usual, were completely wrong.",
            "Parliament adjourned after a heated debate over samosa size.",
            "Referee fined for blowing the whistle without a licence.",
            "Opposition walks out; ruling side walks out to follow them.",
            "Fact-check pending: forwarded as received.",
            "Committee formed to study why the committee was formed."
        )
    }

    // ---------------- input from the view ----------------

    fun padPressed(b: Int) {
        when (b) {
            4 -> in1.punch = true
            5 -> in1.kick = true
            6 -> in1.grab = true
            7 -> in1.sup = true
        }
    }

    fun onBack(): Boolean {
        return when (scr) {
            Scr.TITLE -> false
            Scr.SELECT -> { scr = Scr.TITLE; true }
            Scr.PLAY_WITH_FRIEND -> { scr = Scr.SELECT; true }
            Scr.HOST_ROOM -> { FirebaseMultiplayer.stop(); scr = Scr.PLAY_WITH_FRIEND; true }
            Scr.FIGHT -> {
                paused = !paused
                true
            }
            else -> { scr = Scr.SELECT; true }
        }
    }

    fun onTap(x: Float, y: Float) {
        when (scr) {
            Scr.TITLE -> scr = Scr.SELECT
            Scr.SELECT -> tapSelect(x, y)
            Scr.PLAY_WITH_FRIEND -> tapPlayWithFriend(x, y)
            Scr.HOST_ROOM -> tapHostRoom(x, y)
            Scr.FIGHT -> {
                if (paused) {
                    when {
                        btnResume.contains(x, y) -> paused = false
                        btnRestart.contains(x, y) -> { paused = false; startMatch() }
                        btnQuitSelect.contains(x, y) -> { paused = false; scr = Scr.SELECT }
                    }
                } else if (btnPause.contains(x, y)) {
                    paused = true
                }
            }
            Scr.RESULT -> if (y in 540f..660f) {
                if (x < 640f) startMatch() else scr = Scr.SELECT
            }
            else -> {}
        }
    }

    private fun tapSelect(x: Float, y: Float) {
        for (i in Roster.all.indices) {
            if (cardRect(i).contains(x, y)) {
                if (i == 11) {
                    onToast?.invoke("Chapter 2 coming soon!")
                    return
                }
                if (i == 12) {
                    val pick = rnd.nextInt(11)
                    if (pickPhase == 0) sel1 = pick else sel2 = pick
                    pickPhase = 1 - pickPhase
                    return
                }
                if (pickPhase == 0) sel1 = i else sel2 = i
                pickPhase = 1 - pickPhase
                return
            }
        }
        when {
            btnStage0.contains(x, y) -> stage = 0
            btnStage1.contains(x, y) -> stage = 1
            btnStage2.contains(x, y) -> stage = 2
            btnCpu0.contains(x, y) -> cpuLevel = 0
            btnCpu1.contains(x, y) -> cpuLevel = 1
            btnCpu2.contains(x, y) -> cpuLevel = 2
            btnFriend.contains(x, y) -> scr = Scr.PLAY_WITH_FRIEND
            btnFight.contains(x, y) -> {
                isMultiplayer = false
                startMatch()
            }
        }
    }

    private fun tapPlayWithFriend(x: Float, y: Float) {
        when {
            btnHostRoom.contains(x, y) -> {
                scr = Scr.HOST_ROOM
                isMultiplayer = true
                isHost = true
                hostRoomCode = FirebaseMultiplayer.generateCode()
                FirebaseMultiplayer.hostRoom(sel1.coerceIn(0, 10), stage) { code ->
                    hostRoomCode = code
                }
                FirebaseMultiplayer.waitForGuest(hostRoomCode) { clientHero ->
                    sel2 = clientHero.coerceIn(0, 10)
                    startMatch()
                }
            }
            btnCodeInput.contains(x, y) -> {
                onInputCode?.invoke { code ->
                    enteredCode = code.trim().uppercase()
                }
            }
            btnJoinRoom.contains(x, y) -> {
                if (enteredCode.isNotEmpty()) {
                    isMultiplayer = true
                    isHost = false
                    FirebaseMultiplayer.joinRoom(enteredCode, sel1.coerceIn(0, 10)) { ok, hostHero, stg ->
                        if (ok) {
                            sel2 = hostHero.coerceIn(0, 10)
                            stage = stg
                            startMatch()
                        } else {
                            onToast?.invoke("Room not found or expired!")
                        }
                    }
                } else {
                    onToast?.invoke("Please enter a room code first!")
                }
            }
            btnBackPwf.contains(x, y) -> scr = Scr.SELECT
        }
    }

    private fun tapHostRoom(x: Float, y: Float) {
        when {
            btnShareWhatsApp.contains(x, y) -> onShareWhatsApp?.invoke(hostRoomCode)
            btnCopyLink.contains(x, y) -> onCopyLink?.invoke(hostRoomCode)
            btnBackHost.contains(x, y) -> {
                FirebaseMultiplayer.stop()
                scr = Scr.PLAY_WITH_FRIEND
            }
        }
    }

    // ---------------- match flow ----------------

    fun startMatch() {
        if (sel1 < 0 || sel1 >= 11) sel1 = 0
        if (sel2 < 0 || sel2 >= 11) {
            do { sel2 = rnd.nextInt(11) } while (sel2 == sel1)
        }
        p1 = Fighter(Roster.all[sel1], 380f, 1)
        p2 = Fighter(Roster.all[sel2], 900f, -1)
        ai = Ai(cpuLevel)
        round = 1
        startRound()
    }

    private fun startRound() {
        p1.reset(380f, 1)
        p2.reset(900f, -1)
        projs.clear()
        pops.clear()
        timer = 60 * 60
        hitstop = 0
        introT = 120
        in1.clearAll()
        paused = false
        scr = Scr.INTRO
    }

    private fun endRound() {
        banner = if (p1.hp <= 0f || p2.hp <= 0f) "K.O.!" else "TIME UP!"
        if (p1.hp > p2.hp) p1.wins++ else if (p2.hp > p1.hp) p2.wins++
        endT = 170
        scr = Scr.ROUNDEND
    }

    private fun afterRound() {
        if (p1.wins >= 2 || p2.wins >= 2) {
            resultWin = p1.wins >= 2
            headline = headlines[rnd.nextInt(headlines.size)]
            scr = Scr.RESULT
        } else {
            round++
            startRound()
        }
    }

    // ---------------- per-frame update ----------------

    fun update() {
        frame++
        if (paused) return
        if (shake > 0f) shake = max(0f, shake - 0.8f)
        val iter = pops.iterator()
        while (iter.hasNext()) {
            val pop = iter.next()
            pop.t--
            pop.y -= 1.2f
            if (pop.t <= 0) iter.remove()
        }
        when (scr) {
            Scr.INTRO -> {
                introT--
                if (introT <= 0) { scr = Scr.FIGHT; in1.clearAll() }
            }
            Scr.FIGHT -> updateFight()
            Scr.ROUNDEND -> {
                endT--
                settle(p1)
                settle(p2)
                if (endT <= 0) afterRound()
            }
            else -> {}
        }
    }

    private fun updateFight() {
        if (hitstop > 0) { hitstop--; return }
        timer--
        in1.left = held(0)
        in1.right = held(1)
        in1.up = held(2)
        in1.down = held(3)
        ai.think(p2, p1)
        step(p1, p2, in1)
        in1.clearEdges()
        step(p2, p1, ai.inp)
        updateProj()
        pushApart(p1, p2)
        if (p1.hp <= 0f || p2.hp <= 0f || timer <= 0) endRound()
    }

    private fun settle(f: Fighter) {
        f.vx *= 0.9f
        moveAndGravity(f)
    }

    private fun step(f: Fighter, o: Fighter, inp: Input) {
        if (f.flash > 0) f.flash--
        if (f.comboT > 0) {
            f.comboT--
            if (f.comboT == 0) f.combo = 0
        }

        if (f.st == St.KO) {
            f.vx *= 0.92f
            moveAndGravity(f)
            return
        }
        if (f.st == St.HIT || f.st == St.BLOCK) {
            f.stun--
            f.vx *= 0.86f
            if (f.st == St.HIT) f.blocking = false
            if (f.stun <= 0) {
                f.st = St.IDLE
                f.vx = 0f
                f.blocking = false
            }
            moveAndGravity(f)
            return
        }
        if (f.st == St.ATTACK) {
            f.blocking = false
            updateAttack(f, o, inp)
            moveAndGravity(f)
            return
        }

        if (f.onGround && f.vy == 0f) f.facing = if (o.x > f.x) 1 else -1

        if (f.onGround) {
            val back = (f.facing == 1 && inp.left) || (f.facing == -1 && inp.right)
            val dir = (if (inp.right) 1 else 0) - (if (inp.left) 1 else 0)
            f.blocking = back
            val m: Move? = when {
                // SPECIAL ATTACK IS ALWAYS ENABLED as user requested!
                inp.sup -> Moves.SUPER
                inp.grab || (inp.punch && inp.kick) -> Moves.GRAB
                inp.punch -> if (inp.down) Moves.UPPER else Moves.P1
                inp.kick -> if (inp.down) Moves.SWEEP else Moves.KICK
                else -> null
            }
            if (m != null) {
                begin(f, m, true)
            } else if (inp.up) {
                f.vy = -18f
                f.vx = dir * f.def.speed
                f.st = St.JUMP
                f.blocking = false
            } else if (inp.down) {
                f.st = St.CROUCH
                f.vx = 0f
            } else {
                f.vx = dir * f.def.speed * (if (back) 0.65f else 1f)
                f.st = if (dir != 0) St.WALK else St.IDLE
                if (dir != 0) f.walkPhase += 0.28f
            }
        } else {
            f.st = St.JUMP
            f.blocking = false
            if (inp.kick || inp.punch) begin(f, Moves.AIRKICK, true)
        }
        moveAndGravity(f)
    }

    private fun begin(f: Fighter, m: Move, resetChain: Boolean) {
        f.move = m
        f.moveT = 0
        f.hitDone = false
        f.st = St.ATTACK
        f.blocking = false
        if (resetChain) f.chain = 0
        if (m != Moves.AIRKICK) f.vx = 0f
        if (m.isSuper) f.meter = 0f
    }

    private fun updateAttack(f: Fighter, o: Fighter, inp: Input) {
        val m = f.move
        if (m == null) { f.st = St.IDLE; return }
        f.moveT++
        if (m.isSuper) {
            if (f.moveT == m.hitFrom) {
                projs.add(Proj(f.x + f.facing * 70f, f.y - 105f, f.facing * 17f, f, 26f * f.def.power))
                pop(f.x, f.y - 200f, "SUPER!", 0xFFFFC83D.toInt())
                shake = 8f
            }
        } else if (!f.hitDone && f.moveT >= m.hitFrom && f.moveT <= m.hitTo && inRange(f, o, m)) {
            f.hitDone = true
            resolve(f, o, m)
        }
        // Combo cancels: P1/P2/KICK can chain into the next move once they connect.
        if (f.hitDone && f.chain < 2 && (m == Moves.P1 || m == Moves.P2 || m == Moves.KICK)) {
            if (inp.punch) {
                f.chain++
                begin(f, Moves.PUNCHES[min(f.chain, 2)], false)
                return
            }
            if (inp.kick) {
                f.chain++
                begin(f, Moves.KICK2, false)
                return
            }
        }
        if (m != Moves.AIRKICK && f.moveT >= m.total) {
            f.st = St.IDLE
            f.move = null
        }
    }

    private fun inRange(f: Fighter, o: Fighter, m: Move): Boolean {
        if (o.st == St.KO) return false
        val dx = (o.x - f.x) * f.facing
        if (dx < -20f || dx - 30f > m.reach) return false
        if (abs(f.y - o.y) > 140f) return false
        if (m.level == Level.HIGH && o.crouching && o.onGround) return false
        if (m.level == Level.LOW && !o.onGround) return false
        if (m.isGrab && !o.onGround) return false
        return true
    }

    private fun resolve(f: Fighter, o: Fighter, m: Move) {
        var dmg = m.dmg * f.def.power
        dmg *= max(0.5f, 1f - 0.08f * f.combo)

        if (m.isGrab) {
            o.hp -= dmg * o.def.defense
            o.st = St.HIT
            o.stun = m.stun
            o.move = null
            o.vx = f.facing * m.knock
            o.vy = -9f
            f.meter = min(100f, f.meter + 14f)
            o.meter = min(100f, o.meter + 8f)
            f.combo++; f.comboT = 70
            pop(o.x, o.y - 190f, "PATAK!", 0xFFFF5A36.toInt())
            shake = 10f; hitstop = 8
            if (o.hp <= 0f) ko(o, f.facing)
            return
        }

        if (o.blocking && o.onGround) {
            o.hp = max(1f, o.hp - dmg * 0.12f)
            o.st = St.BLOCK
            o.stun = 10
            o.vx = f.facing * (m.knock * 0.7f + 2f)
            f.meter = min(100f, f.meter + 3f)
            pop(o.x, o.y - 190f, "BLOCK", 0xFF9AD1FF.toInt())
            shake = 2f
            return
        }

        o.hp -= dmg * o.def.defense
        o.st = St.HIT
        o.stun = m.stun
        o.flash = 6
        o.move = null
        o.vx = f.facing * m.knock
        if (m == Moves.UPPER) o.vy = -14f
        else if (m == Moves.SWEEP) o.vy = -7f
        f.meter = min(100f, f.meter + 9f)
        o.meter = min(100f, o.meter + 6f)
        f.combo++; f.comboT = 70
        shake = if (m.isSuper) 10f else 5f
        hitstop = if (m.isSuper) 10 else 4
        val word = when (m) {
            Moves.UPPER -> "UPPERCUT!"
            Moves.SWEEP -> "SWEEP!"
            else -> hitWords[rnd.nextInt(hitWords.size)]
        }
        pop(o.x, o.y - 190f, word, 0xFFFFC83D.toInt())
        if (o.hp <= 0f) ko(o, f.facing)
    }

    private fun ko(f: Fighter, dir: Int) {
        f.hp = 0f
        f.st = St.KO
        f.vx = dir * 13f
        f.vy = -16f
        f.move = null
        shake = 14f
        hitstop = 14
        pop(f.x, f.y - 200f, "K.O.!", 0xFFFF3838.toInt())
    }

    private fun updateProj() {
        val iter = projs.iterator()
        while (iter.hasNext()) {
            val pr = iter.next()
            pr.x += pr.vx
            pr.life--
            val hit1 = pr.owner !== p1 && abs(pr.x - p1.x) < 45f && abs(pr.y - (p1.y - 90f)) < 70f
            val hit2 = pr.owner !== p2 && abs(pr.x - p2.x) < 45f && abs(pr.y - (p2.y - 90f)) < 70f
            if (hit1 || hit2) {
                val target = if (hit1) p1 else p2
                val dir = if (pr.vx > 0) 1 else -1
                if (target.blocking && target.onGround) {
                    target.hp = max(1f, target.hp - pr.dmg * 0.15f)
                    target.st = St.BLOCK; target.stun = 14; target.vx = dir * 7f
                    pop(target.x, target.y - 190f, "BLOCKED!", 0xFF9AD1FF.toInt())
                } else {
                    target.hp -= pr.dmg * target.def.defense
                    target.st = St.HIT; target.stun = 24; target.flash = 8
                    target.vx = dir * 14f; target.vy = -10f
                    shake = 10f; hitstop = 8
                    pop(target.x, target.y - 190f, "SUPER HIT!", 0xFFFFC83D.toInt())
                    if (target.hp <= 0f) ko(target, dir)
                }
                iter.remove()
                continue
            }
            if (pr.life <= 0 || pr.x < -100f || pr.x > W + 100f) iter.remove()
        }
    }

    private fun pushApart(a: Fighter, b: Fighter) {
        val dx = b.x - a.x
        val dist = abs(dx)
        if (dist < 70f) {
            val push = (70f - dist) / 2f
            val dir = if (dx >= 0) 1f else -1f
            a.x -= dir * push
            b.x += dir * push
            clamp(a); clamp(b)
        }
    }

    private fun moveAndGravity(f: Fighter) {
        f.x += f.vx
        f.y += f.vy
        if (!f.onGround) {
            f.vy += 1.0f
        } else {
            f.y = GROUND
            f.vy = 0f
            if (f.st == St.JUMP) f.st = St.IDLE
        }
        clamp(f)
    }

    private fun clamp(f: Fighter) {
        if (f.x < 60f) f.x = 60f
        if (f.x > W - 60f) f.x = W - 60f
    }

    private fun pop(x: Float, y: Float, text: String, color: Int) {
        pops.add(Pop(x, y, text, color))
    }
}
