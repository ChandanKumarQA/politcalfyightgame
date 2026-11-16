package com.comedyarena.politicalfight.game3d.core

import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

enum class Sfx { BELL, SUPER_READY, JUMP, LAND, WHOOSH, SUPER, HIT_LIGHT, HIT_HEAVY, BLOCK, KO, BOOM }
enum class Scr { BOOT, PREVIEW, INTRO, FIGHT, ROUNDEND, RESULT }

/** Callbacks fired from the engine (GL) thread. Implementations must be thread-safe. */
interface Events {
    fun onRound(n: Int)
    fun onBanner(text: String)
    fun onResult(p1Won: Boolean)
    /** sx, sy normalised 0..1 (top-left origin) */
    fun pop(text: String, sx: Float, sy: Float, color: Int)
    fun flash(color: Int)
    fun vibe(ms: Int)
    fun sfx(s: Sfx)
}

class Inp {
    @JvmField var left = false; @JvmField var right = false; @JvmField var up = false; @JvmField var down = false
    @JvmField var punch = false; @JvmField var kick = false; @JvmField var grab = false; @JvmField var sup = false
    fun clear() { left = false; right = false; up = false; down = false; punch = false; kick = false; grab = false; sup = false }
}

private const val MINX = -6.4f
private const val MAXX = 6.4f
private const val JUMPV = 0.19f
private const val WALK = 0.052f

class Ai(@JvmField var level: Float) {
    @JvmField val inp = Inp()
    private val r = Random()
    private var plan = 0; private var planT = 0; private var blockT = 0; private var atkCd = 40; private var prevOp = false

    fun think(me: Fighter, op: Fighter) {
        val i = inp; i.clear(); val lv = level
        if (me.st == St.KO || op.st == St.KO) return
        val dx = op.x - me.x; val d = abs(dx); val tw = if (dx > 0) 1 else -1
        fun toward() { if (tw > 0) i.right = true else i.left = true }
        fun away() { if (tw > 0) i.left = true else i.right = true }
        if (atkCd > 0) atkCd--
        val opAtk = op.st == St.ATTACK
        if (opAtk && !prevOp && d < 2.9f && r.nextFloat() < min(0.88f, 0.18f + 0.27f * lv)) blockT = 18 + (r.nextFloat() * 10).toInt()
        prevOp = opAtk
        if (blockT > 0) { blockT--; if (me.onGround && me.st != St.ATTACK) { away(); return } }
        if (me.st == St.HIT || me.st == St.BLOCK) return
        if (planT-- <= 0) { plan = (r.nextFloat() * 5).toInt(); planT = max(5, 14 + (r.nextFloat() * 30).toInt() - (lv * 7).toInt()) }
        val free = me.st != St.ATTACK && me.onGround
        when (plan) {
            0, 4 -> if (d > 1.7f) toward() else if (free) attack(d)
            1 -> if (d < 3.6f) away() else toward()
            2 -> { if (free && d > 2.6f && d < 6.5f) i.up = true; toward() }
            else -> if (d > 2.2f && r.nextFloat() < 0.05f) toward()
        }
        if (!me.onGround && d < 2.4f && r.nextFloat() < 0.12f) i.kick = true
        if (me.meter >= 100f && d > 3.4f && free && r.nextFloat() < 0.04f + 0.03f * lv) i.sup = true
        if (me.st == St.ATTACK && me.hitDone && r.nextFloat() < min(0.9f, 0.25f + 0.2f * lv)) { if (r.nextFloat() < 0.6f) i.punch = true else i.kick = true }
    }

    private fun attack(d: Float) {
        if (atkCd > 0) return
        atkCd = max(5, (26 - level * 6).toInt() + (r.nextFloat() * max(4f, 20 - level * 4)).toInt())
        val i = inp; val x = r.nextFloat() * 100
        when {
            x < 38 -> i.punch = true
            x < 62 -> i.kick = true
            x < 74 -> { i.down = true; i.kick = true }
            x < 84 -> { i.down = true; i.punch = true }
            x < 94 && d < 1.7f -> i.grab = true
            else -> i.punch = true
        }
    }
}

private class Proj(val owner: Fighter, var x: Float, val y: Float, val vx: Float, val node: Node, val dmg: Float, var life: Int, val shape: String, val color: Int)
private class Part(val n: Node) { var vx = 0f; var vy = 0f; var vz = 0f; var life = 0f; var max = 1f; var grav = 0f }
private class StarFx(val n: Node, val fill: Node, val edge: Node) { var life = 0; var size = 1f }
private class RingFx(val n: Node) { var life = 0; var max = 20; var big = 6f }

class Engine(private val ev: Events) {
    // ---- scene / render state (read by the GL renderer)
    val scene = Node()
    val light = Light()
    @Volatile var bg = 0x000000
    @Volatile var fogColor = 0x000000; @Volatile var fogNear = 30f; @Volatile var fogFar = 90f
    val view = FloatArray(16); val proj = FloatArray(16)
    @Volatile var aspect = 16f / 9f

    // ---- game state (read by the UI thread)
    @Volatile var scr = Scr.BOOT
    @Volatile var p1: Fighter? = null
    @Volatile var p2: Fighter? = null
    @Volatile var timer = 0
    @Volatile var round = 1
    @Volatile var introT = 0
    @Volatile var endT = 0
    @Volatile var paused = false
    @Volatile var roundWinner: Fighter? = null
    @Volatile var banner = ""
    @Volatile var stage = 0
    @Volatile var aiLevel = 0f
    @Volatile var previewWide = false
    var mods = Mods()
    var frame = 0; var t = 0f
    private var slow = 1f; private var slowT = 0f; private var hitstop = 0; private var shake = 0f
    private var acc = 0f; private var bell = false; private var camJump = true

    // ---- input (written by the UI thread)
    @Volatile var kLeft = false; @Volatile var kRight = false; @Volatile var kUp = false; @Volatile var kDown = false
    @Volatile var ePunch = false; @Volatile var eKick = false; @Volatile var eGrab = false; @Volatile var eSup = false

    private val cmds = ConcurrentLinkedQueue<() -> Unit>()
    fun post(f: () -> Unit) { cmds.add(f) }

    private val rnd = Random()
    private var ai = Ai(0f)
    private var arena: Arena? = null
    private val projs = ArrayList<Proj>()
    private val parts = ArrayList<Part>(); private val stars = ArrayList<StarFx>(); private val rings = ArrayList<RingFx>()
    private val fxRoot = Node()
    private val blobs = arrayOfNulls<Node>(2)
    private var cx = 0f; private var cy = 2.6f; private var cz = 10f; private var lx = 0f; private var ly = 1f
    private val tmp = FloatArray(16); private val vp = FloatArray(16); private val tv = FloatArray(4)
    private val in1 = Inp()

    init {
        scene.add(fxRoot)
        for (i in 0 until 2) { val b = Node(Geo.plane(2.4f, 1.3f), 0x000000); b.mat = MAT_BASIC; b.pattern = PAT_RADIAL; b.rx = -PIf / 2; b.py = 0.02f; b.alpha = 0.55f; blobs[i] = b; scene.add(b) }
        for (i in 0 until 46) { val n = Node(Geo.sphere(0.1f, 4, 2), 0xffe34d); n.mat = MAT_BASIC; n.visible = false; fxRoot.add(n); parts.add(Part(n)) }
        for (i in 0 until 4) {
            val s = Node(); s.visible = false
            val e = Node(Geo.star(0.5f, 1.1f, 8), 0xe63b2e); e.mat = MAT_BASIC; e.noDepth = true; e.cull = CULL_NONE
            val f = Node(Geo.star(0.36f, 0.88f, 8), 0xffe34d); f.mat = MAT_BASIC; f.noDepth = true; f.cull = CULL_NONE; f.pz = 0.01f
            s.add(e); s.add(f); fxRoot.add(s); stars.add(StarFx(s, f, e))
        }
        for (i in 0 until 4) { val n = Node(Geo.ring(0.85f, 1f, 40), 0xffffff); n.mat = MAT_BASIC; n.cull = CULL_NONE; n.visible = false; fxRoot.add(n); rings.add(RingFx(n)) }
        applyStage(0)
    }

    fun setViewport(w: Int, h: Int) { aspect = w.toFloat() / max(1, h) }

    fun applyStage(i: Int) {
        stage = i
        arena?.let { scene.remove(it.group) }
        val a = Arenas.get(i); arena = a; scene.children.add(0, a.group); a.group.parent = scene
        bg = a.bg; fogColor = a.fogColor; fogNear = a.fogNear; fogFar = a.fogFar
        val l = a.light; light.sky = l.sky; light.ground = l.ground; light.hemi = l.hemi; light.dir = l.dir; light.dirI = l.dirI; light.dx = l.dx; light.dy = l.dy; light.dz = l.dz
    }

    // ------------------------------------------------------------ fighters / matches
    private fun setFighters(d1: CharDef, d2: CharDef) {
        p1?.let { scene.remove(it.rig.root) }; p2?.let { scene.remove(it.rig.root) }
        val a = Fighter(d1, true); val b = Fighter(d2, false)
        scene.add(a.rig.root); scene.add(b.rig.root)
        p1 = a; p2 = b
        if (mods.bighead) { a.rig.bigHead(); b.rig.bigHead() }
    }
    private fun clearProjs() { for (p in projs) scene.remove(p.node); projs.clear() }

    fun preview(i1: Int, i2: Int, x: Float = 2.7f) {
        mods = Mods(); setFighters(Roster.all[i1], Roster.all[i2])
        p1!!.reset(-x, 1); p2!!.reset(x, -1); clearProjs(); scr = Scr.PREVIEW
    }

    fun swapPreview(side: Int, idx: Int) {
        val old = (if (side == 1) p1 else p2) ?: return
        scene.remove(old.rig.root)
        val nf = Fighter(Roster.all[idx], side == 1); nf.reset(old.x, old.facing); scene.add(nf.rig.root)
        if (side == 1) p1 = nf else p2 = nf
    }

    fun startMatch(i1: Int, i2: Int, level: Float, stg: Int) {
        mods = Mods(); aiLevel = level; applyStage(stg); setFighters(Roster.all[i1], Roster.all[i2])
        ai = Ai(level); round = 1; p1!!.wins = 0; p2!!.wins = 0; startRound()
    }

    fun startLevel(info: LevelInfo, i1: Int) {
        val m = info.mods
        mods = Mods(m.grav, m.speed, m.dmg, m.enemyDmg, m.time, m.m1, m.m2, m.bighead, m.mini)
        var d1 = Roster.all[i1]
        if (mods.mini) d1 = d1.copy(scale = d1.scale * 0.75f)
        aiLevel = info.ai; applyStage(info.stage); setFighters(d1, info.def)
        ai = Ai(info.ai); round = 1; p1!!.wins = 0; p2!!.wins = 0; startRound()
    }

    fun rematch() { p1!!.wins = 0; p2!!.wins = 0; round = 1; startRound() }

    private fun startRound() {
        val a = p1!!; val b = p2!!
        a.reset(-3.2f, 1); b.reset(3.2f, -1); a.meter = mods.m1; b.meter = mods.m2
        clearProjs(); timer = (mods.time * 60); hitstop = 0; slow = 1f; slowT = 0f
        introT = 125; clearInput(); scr = Scr.INTRO; bell = false; camJump = true
        ev.onRound(round)
    }

    private fun endRound() {
        val a = p1!!; val b = p2!!
        val ko = a.hp <= 0 || b.hp <= 0
        banner = if (ko) "K.O.!" else "TIME UP!"
        var w: Fighter? = null
        if (a.hp > b.hp) w = a else if (b.hp > a.hp) w = b
        w?.let { it.wins++ }
        roundWinner = w; endT = 190; scr = Scr.ROUNDEND
        if (!ko) slow = 1f
        ev.onBanner(banner)
    }

    private fun afterRound() {
        if (p1!!.wins >= 2 || p2!!.wins >= 2) { scr = Scr.RESULT; ev.onResult(p1!!.wins >= 2) }
        else { round++; startRound() }
    }

    fun clearInput() { kLeft = false; kRight = false; kUp = false; kDown = false; ePunch = false; eKick = false; eGrab = false; eSup = false }

    // ------------------------------------------------------------ main loop
    /** Called once per rendered frame from the GL thread. */
    fun advance(dtIn: Float) {
        while (true) { val c = cmds.poll() ?: break; c() }
        val dt = min(0.1f, dtIn)
        if (slowT > 0f) { slowT -= dt; if (slowT <= 0f) slow = 1f }
        if (!paused) {
            acc += dt * 60f * slow * mods.speed
            var n = 0
            while (acc >= 1f && n < 5) { update(); acc -= 1f; n++ }
            if (n == 5) acc = 0f
        }
        updateMatrices()
    }

    private fun update() {
        frame++; t = frame / 60f
        if (shake > 0f) shake = max(0f, shake - 0.025f)
        arena?.update?.invoke(t)
        updateFx()
        val a = p1 ?: return; val b = p2 ?: return
        when (scr) {
            Scr.FIGHT -> updateFight()
            Scr.INTRO -> {
                introT--
                if (introT == 55 && !bell) { bell = true; ev.sfx(Sfx.BELL) }
                if (introT <= 0) { scr = Scr.FIGHT; clearInput() }
                for (f in arrayOf(a, b)) { f.st = St.IDLE; physics(f) }
            }
            Scr.ROUNDEND -> {
                endT--
                val w = roundWinner
                for (f in arrayOf(a, b)) {
                    if (f.st == St.KO) { f.koT++; f.vx *= 0.92f; physics(f) }
                    else if (w === f && endT < 150) f.st = St.WIN
                    else { f.vx = 0f; physics(f) }
                }
                if (endT == 150 && slow < 1f) { slow = 1f; slowT = 0f }
                if (endT <= 0) afterRound()
            }
            Scr.RESULT -> for (f in arrayOf(a, b)) if (f.st == St.KO) { f.koT++; physics(f) }
            else -> { a.st = St.IDLE; b.st = St.IDLE }
        }
        for (i in 0 until 2) {
            val f = if (i == 0) a else b
            f.rig.animate(f, frame)
            val bl = blobs[i]!!; bl.px = f.x; bl.pz = 0f
            val sc = max(0.3f, 1f - f.y * 0.25f); bl.sx = sc; bl.sy = sc; bl.alpha = 0.55f * max(0.25f, 1f - f.y * 0.3f)
        }
        updateCamera()
    }

    private fun updateFight() {
        if (hitstop > 0) { hitstop--; return }
        val a = p1!!; val b = p2!!
        timer--
        in1.left = kLeft; in1.right = kRight; in1.up = kUp; in1.down = kDown
        in1.punch = ePunch; in1.kick = eKick; in1.grab = eGrab; in1.sup = eSup
        ePunch = false; eKick = false; eGrab = false; eSup = false
        ai.think(b, a)
        step(a, b, in1); step(b, a, ai.inp)
        updateProj(); pushApart()
        for (f in arrayOf(a, b)) { val rdy = f.meter >= 100f; if (rdy && !f.superReady && f.isP1) ev.sfx(Sfx.SUPER_READY); f.superReady = rdy }
        if (a.hp <= 0 || b.hp <= 0 || timer <= 0) endRound()
    }

    // ------------------------------------------------------------ fighter state machine
    private fun step(f: Fighter, o: Fighter, inp: Inp) {
        val sp = WALK * f.def.spd
        if (f.flash > 0) f.flash--
        if (f.comboT > 0 && --f.comboT == 0) f.combo = 0
        if (f.st == St.KO) { f.koT++; f.vx *= 0.92f; physics(f); return }
        if (f.st == St.HIT || f.st == St.BLOCK) {
            f.stun--; f.vx *= 0.86f; f.blocking = false
            if (f.stun <= 0 && f.onGround) { f.st = St.IDLE; f.vx = 0f } else if (f.stun <= 0) f.st = St.IDLE
            physics(f); return
        }
        if (f.st == St.ATTACK) { f.blocking = false; updateAttack(f, o, inp); physics(f); return }
        if (f.onGround && f.vy == 0f) f.facing = if (o.x > f.x) 1 else -1
        if (f.onGround) {
            val back = (f.facing == 1 && inp.left) || (f.facing == -1 && inp.right)
            val dir = (if (inp.right) 1 else 0) - (if (inp.left) 1 else 0)
            f.blocking = back
            var m: Move? = null
            if (inp.sup && f.meter >= 100f) m = Moves.SUPER
            else if (inp.grab || (inp.punch && inp.kick)) m = Moves.GRAB
            else if (inp.punch) m = if (inp.down) Moves.UPPER else Moves.P1
            else if (inp.kick) m = if (inp.down) Moves.SWEEP else Moves.KICK
            if (m != null) begin(f, m, true)
            else if (inp.up) { f.vy = JUMPV; f.vx = dir * sp; f.st = St.JUMP; f.blocking = false; if (f.isP1) ev.sfx(Sfx.JUMP) }
            else if (inp.down) { f.st = St.CROUCH; f.vx = 0f }
            else {
                f.vx = dir * sp * (if (back) 0.65f else 1f)
                f.st = if (dir != 0) St.WALK else St.IDLE
                if (dir != 0) f.walkPhase += 0.2f * f.def.spd + 0.08f
            }
        } else {
            f.st = St.JUMP; f.blocking = false
            if (inp.kick || inp.punch) begin(f, Moves.AIRKICK, true)
        }
        physics(f)
    }

    private fun begin(f: Fighter, m: Move, resetChain: Boolean) {
        f.move = m; f.moveT = 0; f.hitDone = false; f.st = St.ATTACK; f.blocking = false
        if (resetChain) f.chain = 0
        if (m.id != Mid.AIRKICK) f.vx = 0f
        if (m.sup) { f.meter = 0f; ev.flash(f.def.sup.color); ev.sfx(Sfx.SUPER) } else ev.sfx(Sfx.WHOOSH)
    }

    private fun updateAttack(f: Fighter, o: Fighter, inp: Inp) {
        val m = f.move; if (m == null) { f.st = St.IDLE; return }
        f.moveT++
        if (m.sup) {
            if (f.moveT == m.from) spawnSuper(f)
            if (f.moveT < m.from) f.vx = 0f
        } else if (!f.hitDone && f.moveT >= m.from && f.moveT <= m.to && inRange(f, o, m)) { f.hitDone = true; resolve(f, o, m) }
        if (f.hitDone && f.chain < 2 && (m.id == Mid.P1 || m.id == Mid.P2 || m.id == Mid.KICK)) {
            if (inp.punch) { f.chain++; begin(f, Moves.PUNCH_CHAIN[min(f.chain, 2)], false); return }
            if (inp.kick) { f.chain++; begin(f, Moves.KICK2, false); return }
        }
        if (m.id != Mid.AIRKICK && f.moveT >= m.total) { f.st = St.IDLE; f.move = null }
    }

    private fun inRange(f: Fighter, o: Fighter, m: Move): Boolean {
        if (o.st == St.KO) return false
        val dx = (o.x - f.x) * f.facing
        if (dx < -0.4f || dx > m.reach) return false
        if (abs(f.y - o.y) > 1.5f) return false
        if (m.lvl == 'H' && o.crouching && o.onGround) return false
        if (m.lvl == 'L' && !o.onGround) return false
        if (m.grab && !o.onGround) return false
        return true
    }

    private fun resolve(f: Fighter, o: Fighter, m: Move) {
        val dmg = m.dmg * f.def.pwr * max(0.5f, 1f - 0.08f * f.combo) * mods.dmg * (if (f.isP1) 1f else mods.enemyDmg)
        val hx = o.x; val hy = o.y + 1.9f * o.def.scale * (if (o.crouching) 0.65f else 1f); val hz = 0.4f
        if (m.grab) {
            o.hp -= dmg * o.def.def; o.st = St.HIT; o.stun = m.stun; o.move = null
            o.vx = f.facing * 0.14f; o.vy = 0.15f; f.meter = min(100f, f.meter + 14f); o.meter = min(100f, o.meter + 8f)
            f.combo++; f.comboT = 70; pop("PATAK!", hx, hy, hz, 0xff5a36); burst(hx, hy, hz, 0xff8a3d, 18); starAt(hx, hy, hz, 2.4f)
            shake = 0.5f; hitstop = 9; ev.sfx(Sfx.HIT_HEAVY); ev.vibe(30)
            if (o.hp <= 0) ko(o, f.facing)
            return
        }
        if (o.blocking && o.onGround) {
            o.hp = max(1f, o.hp - dmg * 0.12f); o.st = St.BLOCK; o.stun = 10
            o.vx = f.facing * (m.kb * 5f + 0.03f); f.meter = min(100f, f.meter + 3f)
            pop("BLOCK", hx, hy, hz, 0x9ad1ff); burst(hx, hy, hz, 0x9ad1ff, 6); hitstop = 3; ev.sfx(Sfx.BLOCK)
            return
        }
        val heavy = m.id == Mid.P3 || m.id == Mid.KICK2 || m.id == Mid.SWEEP || m.id == Mid.UPPER
        o.hp -= dmg * o.def.def; f.meter = min(100f, f.meter + dmg * 1.3f); o.meter = min(100f, o.meter + dmg * 0.8f)
        o.st = St.HIT; o.stun = m.stun; o.move = null; o.vx = f.facing * m.kb
        if (m.id == Mid.SWEEP) o.vy = 0.1f
        if (m.launch > 0f) o.vy = m.launch
        o.flash = 6; f.combo++; f.comboT = 70
        pop(HIT_WORDS[rnd.nextInt(HIT_WORDS.size)], hx, hy, hz, 0xffe066)
        burst(hx, hy, hz, if (heavy) 0xffa23a else 0xffe34d, if (heavy) 16 else 9); starAt(hx, hy, hz, if (heavy) 2.4f else 1.6f)
        hitstop = if (heavy) 9 else 5; shake = if (heavy) 0.45f else 0.2f; ev.sfx(if (heavy) Sfx.HIT_HEAVY else Sfx.HIT_LIGHT)
        if (heavy) ev.vibe(18)
        if (o.hp <= 0) ko(o, f.facing)
    }

    private fun ko(o: Fighter, dir: Int) {
        o.hp = 0f; o.st = St.KO; o.move = null; o.koT = 0; o.vx = dir * 0.2f; o.vy = 0.17f; hitstop = 22; shake = 0.9f
        slow = 0.35f; slowT = 1.2f; ringAt(o.x, 1.5f, 0.5f, 0xffffff, 8f); ev.sfx(Sfx.KO)
    }

    private fun physics(f: Fighter) {
        f.x += f.vx
        if (f.x < MINX) f.x = MINX
        if (f.x > MAXX) f.x = MAXX
        if (f.y > 0f || f.vy > 0f) {
            f.vy -= mods.grav; f.y += f.vy
            if (f.y <= 0f) {
                f.y = 0f
                if (f.st == St.KO && f.vy < -0.05f) { f.vy = -f.vy * 0.3f; f.y = 0.001f; dust(f.x); ev.sfx(Sfx.LAND) }
                else {
                    f.vy = 0f
                    if (f.st == St.JUMP) { f.st = St.IDLE; f.vx = 0f; dust(f.x); if (f.isP1) ev.sfx(Sfx.LAND) }
                    if (f.move?.id == Mid.AIRKICK) { f.move = null; f.st = St.IDLE; f.vx = 0f }
                    if (f.st == St.HIT && f.stun <= 0) f.st = St.IDLE
                }
            }
        }
    }

    private fun pushApart() {
        val a = p1!!; val b = p2!!
        if (!a.onGround || !b.onGround) return
        val d = b.x - a.x; val minD = 1.05f
        if (abs(d) < minD) {
            val push = (minD - abs(d)) / 2f; val s = if (d >= 0f) 1f else -1f
            a.x = max(MINX, min(MAXX, a.x - s * push)); b.x = max(MINX, min(MAXX, b.x + s * push))
        }
    }

    // ------------------------------------------------------------ super projectiles
    private fun spawnSuper(f: Fighter) {
        val sup = f.def.sup; val node = Rig.superMesh(sup); val dir = f.facing
        node.pos(f.x + dir * 1.4f, 1.9f, 0.3f); scene.add(node)
        projs.add(Proj(f, f.x + dir * 1.4f, 1.9f, dir * 0.2f, node, sup.dmg * f.def.pwr * mods.dmg * (if (f.isP1) 1f else mods.enemyDmg), 110, sup.shape, sup.color))
        ringAt(f.x + dir * 1.2f, 1.9f, 0.3f, sup.color, 5f)
        shake = 0.6f; pop(sup.name + "!", f.x, 3.8f, 0.5f, 0xffc83d)
    }

    private fun updateProj() {
        for (i in projs.size - 1 downTo 0) {
            val pr = projs[i]; pr.x += pr.vx; pr.life--
            pr.node.px = pr.x; pr.node.rx += 0.18f; if (pr.shape != "mega") pr.node.rz += 0.04f
            if (frame % 2 == 0) spark(pr.x, pr.y + (rnd.nextFloat() - 0.5f), 0.3f, pr.color)
            val tgt = if (pr.owner === p1) p2!! else p1!!
            var kill = pr.life <= 0 || abs(pr.x) > 16f
            if (!kill && tgt.st != St.KO && abs(pr.x - tgt.x) < 1.0f && tgt.y < 2.6f) {
                kill = true; val dir = if (pr.vx > 0f) 1 else -1
                val hx = tgt.x; val hy = tgt.y + 1.9f * tgt.def.scale * (if (tgt.crouching) 0.65f else 1f)
                if (tgt.blocking && tgt.onGround) {
                    tgt.hp = max(1f, tgt.hp - pr.dmg * 0.2f); tgt.st = St.BLOCK; tgt.stun = 14; tgt.vx = dir * 0.12f
                    pop("BLOCK", hx, hy, 0.4f, 0x9ad1ff); ev.sfx(Sfx.BLOCK)
                } else {
                    tgt.hp -= pr.dmg * tgt.def.def; tgt.meter = min(100f, tgt.meter + 10f); tgt.st = St.HIT; tgt.stun = 34; tgt.move = null
                    tgt.vx = dir * 0.2f; tgt.vy = 0.16f; tgt.flash = 8; pr.owner.combo++; pr.owner.comboT = 70
                    pop("BOOM!", hx, hy, 0.4f, 0xff7a3d); burst(hx, hy, 0.4f, pr.color, 26); starAt(hx, hy, 0.4f, 3.2f); ringAt(hx, hy, 0.4f, pr.color, 7f)
                    shake = 0.8f; hitstop = 10; ev.sfx(Sfx.BOOM); ev.vibe(40)
                    if (tgt.hp <= 0) ko(tgt, dir)
                }
            }
            if (kill) { scene.remove(pr.node); projs.removeAt(i) }
        }
    }

    // ------------------------------------------------------------ effects
    private fun pop(text: String, x: Float, y: Float, z: Float, color: Int) {
        tv[0] = 0f
        M4.mul(vp, proj, view); M4.transform(vp, x, y, z, tv)
        if (tv[3] <= 0.01f) return
        ev.pop(text, tv[0] / tv[3] * 0.5f + 0.5f, -tv[1] / tv[3] * 0.5f + 0.5f, color)
    }
    private fun freePart(): Part? { for (p in parts) if (!p.n.visible) return p; return null }
    private fun spark(x: Float, y: Float, z: Float, color: Int) {
        val q = freePart() ?: return
        q.n.visible = true; q.n.pos(x, y, z); q.n.color = color; q.n.alpha = 1f; q.n.scale(1.3f)
        q.vx = (rnd.nextFloat() - 0.5f) * 0.02f; q.vy = (rnd.nextFloat() - 0.5f) * 0.02f; q.vz = 0f; q.life = 24f; q.max = 24f; q.grav = 0f
    }
    private fun burst(x: Float, y: Float, z: Float, color: Int, n: Int) {
        for (i in 0 until n) {
            val q = freePart() ?: return
            q.n.visible = true; q.n.pos(x, y, z); q.n.color = if (i % 3 != 0) color else 0xffffff; q.n.alpha = 1f; q.n.scale(0.9f + rnd.nextFloat())
            val a = rnd.nextFloat() * 6.2832f; val s = 0.06f + rnd.nextFloat() * 0.12f
            q.vx = cos(a) * s; q.vy = sin(a) * s + 0.03f; q.vz = (rnd.nextFloat() - 0.3f) * 0.08f; q.life = 22f + rnd.nextFloat() * 14f; q.max = q.life; q.grav = 0.004f
        }
    }
    private fun dust(x: Float) {
        for (i in 0 until 5) {
            val q = freePart() ?: return
            q.n.visible = true; q.n.pos(x, 0.1f, 0.4f); q.n.color = 0xd9c7a0; q.n.alpha = 1f; q.n.scale(1.6f)
            q.vx = (rnd.nextFloat() - 0.5f) * 0.09f; q.vy = 0.02f + rnd.nextFloat() * 0.03f; q.vz = rnd.nextFloat() * 0.03f; q.life = 18f; q.max = 18f; q.grav = 0f
        }
    }
    private fun starAt(x: Float, y: Float, z: Float, size: Float) {
        for (s in stars) if (s.life <= 0) { s.life = 14; s.size = size; s.n.visible = true; s.n.pos(x, y, z + 0.6f); s.n.rz = rnd.nextFloat() * 6f; return }
    }
    private fun ringAt(x: Float, y: Float, z: Float, color: Int, big: Float) {
        for (r in rings) if (r.life <= 0) { r.life = 22; r.max = 22; r.big = big; r.n.visible = true; r.n.pos(x, y, z); r.n.color = color; return }
    }
    private fun updateFx() {
        for (q in parts) {
            if (!q.n.visible) continue
            q.life--; q.n.px += q.vx; q.n.py += q.vy; q.n.pz += q.vz; q.vy -= q.grav
            q.n.alpha = max(0f, q.life / q.max); q.n.rx += 0.2f; q.n.ry += 0.15f
            if (q.life <= 0f) q.n.visible = false
        }
        for (s in stars) {
            if (s.life <= 0) continue; s.life--
            val k = 1f - s.life / 14f; s.n.scale(s.size * (0.5f + k * 1.2f)); val al = 1f - k * k; s.fill.alpha = al; s.edge.alpha = al
            if (s.life <= 0) s.n.visible = false
        }
        for (r in rings) {
            if (r.life <= 0) continue; r.life--
            val k = 1f - r.life.toFloat() / r.max; r.n.scale(0.3f + k * r.big); r.n.alpha = 1f - k
            if (r.life <= 0) r.n.visible = false
        }
    }

    // ------------------------------------------------------------ camera
    private fun updateCamera() {
        val a = p1 ?: return; val b = p2 ?: return
        val th = tan(38f * PIf / 360f) * aspect
        var tx: Float; var ty: Float; var tz: Float; val lyT: Float
        if (scr == Scr.PREVIEW || scr == Scr.BOOT) {
            val sw = sin(t * 0.35f) * 0.9f; val wide = previewWide
            tx = sw; ty = if (wide) 2.8f else 2.35f
            tz = max(if (wide) 10.5f else 9.5f, ((abs(a.x - b.x) / 2f) + 3.2f) / th); lyT = if (wide) 1.55f else 0.95f
            cx += (tx - cx) * 0.06f; cy += (ty - cy) * 0.06f; cz += (tz - cz) * 0.06f
            lx += (sw * 0.3f - lx) * 0.06f; ly += (lyT - ly) * 0.06f
            return
        }
        val mid = (a.x + b.x) / 2f; val dx = abs(a.x - b.x)
        tz = min(max(8.6f, (dx / 2f + 3.4f) / th), 19f)
        ty = 2.5f + (tz - 8.6f) * 0.06f; tx = mid * 0.9f; lyT = 1.55f + max(a.y, b.y) * 0.2f
        val w = roundWinner
        if (scr == Scr.ROUNDEND && w != null) { tx = w.x; tz = 7.5f; ty = 2.6f }
        val k = if (camJump) 1f else 0.1f; camJump = false
        cx += (tx - cx) * k; cy += (ty - cy) * k; cz += (tz - cz) * k
        lx += (tx - lx) * k; ly += (lyT - ly) * k
    }

    private fun updateMatrices() {
        var sx = 0f; var sy = 0f
        if (shake > 0f && (scr == Scr.FIGHT || scr == Scr.ROUNDEND || scr == Scr.INTRO)) { sx = (rnd.nextFloat() - 0.5f) * shake * 0.5f; sy = (rnd.nextFloat() - 0.5f) * shake * 0.4f }
        M4.lookAt(view, cx + sx, cy + sy, cz, lx + sx, ly + sy, 0f, 0f, 1f, 0f)
        M4.perspective(proj, 38f * PIf / 180f, aspect, 0.1f, 200f)
        scene.updateWorld(null)
    }
}
