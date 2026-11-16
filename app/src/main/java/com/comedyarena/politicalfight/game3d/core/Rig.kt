package com.comedyarena.politicalfight.game3d.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class St { IDLE, WALK, JUMP, CROUCH, ATTACK, HIT, BLOCK, KO, WIN }
enum class Mid { P1, P2, P3, KICK, KICK2, SWEEP, UPPER, AIRKICK, GRAB, SUPER }

class Move(
    val id: Mid, val total: Int, val from: Int, val to: Int, val dmg: Float, val reach: Float,
    val lvl: Char, val stun: Int, val kb: Float, val launch: Float = 0f, val grab: Boolean = false, val sup: Boolean = false
)

object Moves {
    val P1 = Move(Mid.P1, 16, 4, 8, 5f, 1.8f, 'H', 13, 0.045f)
    val P2 = Move(Mid.P2, 16, 4, 8, 5f, 1.8f, 'H', 13, 0.045f)
    val P3 = Move(Mid.P3, 24, 7, 11, 9f, 1.95f, 'H', 22, 0.16f)
    val KICK = Move(Mid.KICK, 24, 8, 13, 8f, 2.05f, 'M', 18, 0.10f)
    val KICK2 = Move(Mid.KICK2, 26, 8, 13, 9f, 2.15f, 'M', 22, 0.15f)
    val SWEEP = Move(Mid.SWEEP, 30, 9, 15, 7f, 2.2f, 'L', 30, 0.09f)
    val UPPER = Move(Mid.UPPER, 24, 6, 12, 7f, 1.6f, 'M', 26, 0.05f, launch = 0.13f)
    val AIRKICK = Move(Mid.AIRKICK, 999, 0, 999, 7f, 1.9f, 'M', 18, 0.10f)
    val GRAB = Move(Mid.GRAB, 34, 7, 9, 12f, 1.5f, 'M', 36, 0.2f, grab = true)
    val SUPER = Move(Mid.SUPER, 54, 20, 21, 0f, 0f, 'M', 0, 0f, sup = true)
    val PUNCH_CHAIN = arrayOf(P1, P2, P3)
}

class Fighter(val def: CharDef, val isP1: Boolean) {
    val rig = Rig(def)
    val phase = (Math.random() * 6).toFloat()
    @JvmField var x = 0f; @JvmField var y = 0f; @JvmField var vx = 0f; @JvmField var vy = 0f
    @JvmField var facing = 1
    @JvmField var hp = def.hp; @JvmField var meter = 0f
    @JvmField var st = St.IDLE
    @JvmField var move: Move? = null
    @JvmField var moveT = 0; @JvmField var hitDone = false; @JvmField var chain = 0
    @JvmField var stun = 0; @JvmField var blocking = false; @JvmField var flash = 0
    @JvmField var combo = 0; @JvmField var comboT = 0; @JvmField var wins = 0
    @JvmField var walkPhase = 0f; @JvmField var koT = 0; @JvmField var superReady = false
    val onGround get() = y <= 0.0001f
    val crouching get() = st == St.CROUCH || (move != null && (move!!.id == Mid.SWEEP || move!!.id == Mid.UPPER))

    init { reset(0f, 1) }

    fun reset(nx: Float, face: Int) {
        x = nx; y = 0f; vx = 0f; vy = 0f; facing = face; hp = def.hp; meter = 0f; st = St.IDLE
        move = null; moveT = 0; hitDone = false; chain = 0; stun = 0; blocking = false; flash = 0
        combo = 0; comboT = 0; walkPhase = 0f; koT = 0; superReady = false
        rig.yaw = if (face > 0) -0.42f else PIf + 0.42f
        rig.setFlash(false)
    }
}

/** Pose channel indices. Limb angles: + = swing forward. lean/tilt: + = forward. */
private const val LEAN = 0; private const val TILT = 1; private const val SHN = 2; private const val ELN = 3
private const val SHF = 4; private const val ELF = 5; private const val HIPN = 6; private const val KNN = 7
private const val HIPF = 8; private const val KNF = 9; private const val YAW = 10; private const val BODYZ = 11
private const val BODYY = 12; private const val SQUASH = 13; private const val NCH = 15

/** Procedural 3D cartoon fighter built from toon primitives with outline hulls. */
class Rig(val def: CharDef) {
    val root = Node()
    private val body = Node()
    val hips = Node()
    private val torso = Node()
    private val head = Node()
    private val aNsh = Node(); private val aNel = Node(); private val aFsh = Node(); private val aFel = Node()
    private val lNhp = Node(); private val lNkn = Node(); private val lFhp = Node(); private val lFkn = Node()
    private val mouth: Node
    private val brows = ArrayList<Node>()
    private val eyes = Node(); private val exes = Node()
    private val aura: Node
    private val all = ArrayList<Node>()
    val cur = FloatArray(NCH)
    val tgt = FloatArray(NCH)
    var yaw = 0f
    private var flashOn = false
    private var expr = ""

    private fun part(parent: Node, mesh: MeshData, color: Int, px: Float = 0f, py: Float = 0f, pz: Float = 0f,
                     rx: Float = 0f, ry: Float = 0f, rz: Float = 0f, sx: Float = 1f, sy: Float = 1f, sz: Float = 1f, outline: Float = 1.09f): Node {
        val n = Node(mesh, color); n.px = px; n.py = py; n.pz = pz; n.rx = rx; n.ry = ry; n.rz = rz; n.sx = sx; n.sy = sy; n.sz = sz
        n.outline = outline; parent.add(n); all.add(n); return n
    }

    init {
        val S = def.scale
        root.add(body); body.sx = S; body.sy = S; body.sz = S
        body.add(hips); hips.py = 0.99f
        hips.add(torso)
        // torso
        part(torso, Geo.cap(0.34f, 0.2f), def.top, py = 0.42f, sx = 0.86f, sz = 1.08f)
        part(torso, Geo.cyl(0.33f, 0.5f, 0.5f), def.top, py = -0.1f, sx = 0.9f, sz = 1.1f, outline = 1.07f)
        part(torso, Geo.torus(0.37f, 0.055f), def.accent, py = 0.04f, rx = PIf / 2, sx = 0.9f, sy = 1.1f, outline = 0f)
        part(torso, Geo.torus(0.3f, 0.07f), def.accent, py = 0.84f, rx = PIf / 2, sx = 0.9f, sy = 1.1f, outline = 0f)
        // head
        torso.add(head); head.py = 1.3f
        val R = 0.58f
        part(head, Geo.sphere(R, 28, 20), def.skin, sx = 1f, sy = 0.96f, sz = 1.02f, outline = 1.06f)
        for (sd in intArrayOf(-1, 1)) part(head, Geo.sphere(0.12f, 12, 10), def.skin, -0.02f, -0.02f, sd * 0.57f, sx = 0.6f, sy = 1f, sz = 0.7f, outline = 1.12f)
        head.add(eyes); head.add(exes)
        for (sd in intArrayOf(-1, 1)) {
            part(eyes, Geo.sphere(0.135f, 14, 12), 0xffffff, 0.47f, 0.07f, sd * 0.2f, sx = 0.62f, sy = 1f, sz = 0.9f, outline = 1.08f)
            part(eyes, Geo.sphere(0.075f, 10, 8), 0x120a06, 0.54f, 0.06f, sd * 0.2f, sx = 0.5f, sy = 1f, sz = 0.9f, outline = 0f)
            brows.add(part(head, Geo.box(0.04f, 0.045f, 0.2f), def.hair, 0.5f, 0.25f, sd * 0.2f, rx = sd * -0.22f, outline = 0f))
            for (a in floatArrayOf(0.7f, -0.7f)) part(exes, Geo.box(0.03f, 0.2f, 0.04f), 0x120a06, 0.55f, 0.07f, sd * 0.2f, rx = a, outline = 0f)
        }
        exes.visible = false
        part(head, Geo.sphere(0.095f, 12, 10), def.skin, 0.58f, -0.08f, 0f, sx = 0.9f, sy = 0.9f, sz = 0.9f, outline = 1.12f)
        mouth = part(head, Geo.sphere(0.1f, 12, 8), 0x3a0d0d, 0.55f, -0.29f, 0f, sx = 0.25f, sy = 0.28f, sz = 1.5f, outline = 0f)
        if (def.glasses) {
            for (sd in intArrayOf(-1, 1)) part(head, Geo.torus(0.14f, 0.022f), 0x111111, 0.56f, 0.07f, sd * 0.2f, ry = PIf / 2, outline = 0f)
            part(head, Geo.box(0.03f, 0.03f, 0.14f), 0x111111, 0.57f, 0.1f, 0f, outline = 0f)
        }
        if (def.stache > 0) {
            val t = if (def.stache == 2) 0.075f else 0.05f
            for (sd in intArrayOf(-1, 1)) part(head, Geo.cap(t, 0.17f), def.hair, 0.56f, -0.17f, sd * 0.1f, rx = sd * 1.2f, outline = 0f)
        }
        if (def.beard) part(head, Geo.sphere(0.6f, 20, 12, PIf * 0.64f, PIf * 0.3f), def.hair, 0.03f, -0.02f, 0f, sz = 1.02f, outline = 0f)
        fun capHair(r: Float, tilt: Float, thetaEnd: Float = PIf * 0.55f) = part(head, Geo.sphere(r, 24, 14, 0f, thetaEnd), def.hair, rz = tilt, outline = 0f)
        when (def.hairStyle) {
            "short" -> capHair(R * 1.06f, 0.38f)
            "slick" -> { capHair(R * 1.07f, 0.5f); part(head, Geo.sphere(0.3f, 12, 10), def.hair, -0.35f, 0.3f, 0f, sx = 1f, sy = 0.9f, sz = 1.2f, outline = 0f) }
            "bun" -> { capHair(R * 1.06f, 0.42f); part(head, Geo.sphere(0.22f, 12, 10), def.hair, -0.32f, 0.62f, 0f) }
            "afro" -> capHair(R * 1.25f, 0.3f, PIf * 0.62f)
            "spiky" -> {
                capHair(R * 1.05f, 0.38f)
                for (i in 0 until 7) {
                    val a = i / 7f * 2f * PIf
                    part(head, Geo.cone(0.16f, 0.5f, 8), def.hair, cos(a) * 0.3f - 0.08f, 0.6f + sin(i * 1.7f) * 0.03f, sin(a) * 0.38f,
                        rx = sin(a) * 0.5f, rz = -cos(a) * 0.5f - 0.1f)
                }
            }
        }
        val hc = def.hatColor
        when (def.hat) {
            "topi" -> part(head, Geo.cyl(0.46f, 0.5f, 0.34f), hc, -0.02f, 0.56f, 0f, rz = 0.18f, sx = 0.82f, sy = 1f, sz = 1.12f)
            "turban" -> {
                part(head, Geo.sphere(R * 1.1f, 20, 12), hc, -0.04f, 0.16f, 0f, sx = 1f, sy = 0.9f, sz = 1.05f)
                for (i in 0 until 3) part(head, Geo.torus(0.5f - i * 0.04f, 0.11f), if (i % 2 == 1) 0xffffff else hc, -0.02f, 0.34f + i * 0.13f, 0f, rx = PIf / 2 + 0.2f, sy = 1.1f)
                part(head, Geo.sphere(0.2f, 12, 10), hc, -0.05f, 0.88f, 0f, sy = 0.8f)
            }
            "cap" -> {
                part(head, Geo.sphere(R * 1.08f, 22, 12, 0f, PIf * 0.5f), hc, rz = 0.2f, outline = 0f)
                part(head, Geo.cyl(0.4f, 0.4f, 0.06f, 16), hc, 0.56f, 0.28f, 0f, rz = 0.2f, sz = 1.1f)
            }
            "crown" -> {
                part(head, Geo.cyl(0.46f, 0.5f, 0.22f, 18), hc, -0.02f, 0.62f, 0f)
                for (i in 0 until 6) { val a = i / 6f * 2f * PIf; part(head, Geo.cone(0.09f, 0.26f, 6), 0xef4444, -0.02f + cos(a) * 0.42f, 0.85f, sin(a) * 0.42f, outline = 1.15f) }
            }
        }
        // arms (N = near camera side z+, F = far side z-)
        fun arm(side: Int, sh: Node, el: Node) {
            torso.add(sh); sh.py = 0.64f; sh.pz = side * 0.46f
            part(sh, Geo.cap(0.125f, 0.22f), def.top, py = -0.2f, outline = 1.14f)
            sh.add(el); el.py = -0.4f
            part(el, Geo.cap(0.105f, 0.2f), def.skin, py = -0.18f, outline = 1.14f)
            part(el, Geo.sphere(0.175f, 14, 12), def.skin, py = -0.42f, outline = 1.13f)
        }
        arm(1, aNsh, aNel); arm(-1, aFsh, aFel)
        // legs
        fun leg(side: Int, hp: Node, kn: Node) {
            hips.add(hp); hp.pz = side * 0.2f
            part(hp, Geo.cap(0.16f, 0.2f), def.bottom, py = -0.22f, outline = 1.12f)
            hp.add(kn); kn.py = -0.42f
            part(kn, Geo.cap(0.14f, 0.2f), def.bottom, py = -0.21f, outline = 1.12f)
            part(kn, Geo.sphere(0.19f, 14, 10), def.shoe, 0.07f, -0.44f, 0f, sx = 1.55f, sy = 0.7f, sz = 1.1f, outline = 1.1f)
        }
        leg(1, lNhp, lNkn); leg(-1, lFhp, lFkn)
        // super-charge aura
        aura = Node(Geo.sphere(1.5f, 20, 14), def.sup.color).also { it.mat = MAT_GLOW; it.alpha = 0.35f; it.py = 1.5f; it.visible = false }
        body.add(aura)
        cur[LEAN] = 0.1f; cur[SHN] = 0.9f; cur[ELN] = 1.7f; cur[SHF] = 0.5f; cur[ELF] = 1.9f; cur[HIPN] = 0.28f; cur[KNN] = -0.4f
        cur[HIPF] = -0.32f; cur[KNF] = -0.45f; cur[SQUASH] = 1f
        setExpr("normal")
    }

    fun bigHead() { head.scale(1.55f); head.py += 0.3f }

    fun setFlash(v: Boolean) {
        if (v == flashOn) return
        flashOn = v; val e = if (v) 1f else 0f
        for (n in all) n.emissive = e
    }

    fun setExpr(e: String) {
        if (e == expr) return
        expr = e
        val hit = e == "hit"; val ko = e == "ko"; val win = e == "win"; val ang = e == "angry"
        eyes.visible = !ko; exes.visible = ko
        mouth.sx = if (hit || ko) 0.4f else 0.25f
        mouth.sy = if (hit || ko) 1.0f else if (win) 0.6f else 0.28f
        mouth.sz = if (win) 2.4f else if (hit || ko) 1.1f else 1.5f
        mouth.py = if (win) -0.27f else -0.29f
        brows[0].rx = -(if (hit) 0.35f else if (ang) 0.5f else -0.22f)
        brows[1].rx = -(if (hit) -0.35f else if (ang) -0.5f else 0.22f)
    }

    /** Applies the fighter's logic state to the node transforms. */
    fun animate(f: Fighter, t: Int) {
        pose(f, t)
        val k = tgt[NCH - 1]
        for (i in 0 until NCH - 1) cur[i] += (tgt[i] - cur[i]) * k
        var d = (if (f.facing > 0) -0.42f else PIf + 0.42f) - yaw
        while (d > PIf) d -= 2f * PIf
        while (d < -PIf) d += 2f * PIf
        yaw += d * 0.35f
        root.ry = yaw + cur[YAW]; root.px = f.x; root.py = f.y; root.pz = 0f
        torso.rz = -cur[LEAN]; head.rz = -cur[TILT]
        aNsh.rz = cur[SHN]; aNel.rz = cur[ELN]; aFsh.rz = cur[SHF]; aFel.rz = cur[ELF]
        lNhp.rz = cur[HIPN]; lNkn.rz = cur[KNN]; lFhp.rz = cur[HIPF]; lFkn.rz = cur[KNF]
        body.rz = cur[BODYZ]; body.py = cur[BODYY]
        if (f.y <= 0.001f && f.st != St.KO) {
            fun ext(a: Float, kk: Float) = 0.42f * cos(a) + 0.44f * cos(a + kk) + 0.13f
            hips.py = max(0.45f, max(ext(cur[HIPN], cur[KNN]), ext(cur[HIPF], cur[KNF])))
        } else hips.py = 0.99f
        hips.sy = cur[SQUASH]
        val charging = f.st == St.ATTACK && f.move?.id == Mid.SUPER && f.moveT < 32
        aura.visible = charging || f.meter >= 100f
        if (aura.visible) {
            val pulse = 1f + sin(t * 0.5f) * 0.08f
            aura.scale(pulse * (if (charging) 1.15f else 0.82f))
            aura.alpha = if (charging) 0.42f else 0.07f + sin(t * 0.3f) * 0.025f
        }
        setFlash(f.flash > 3)
        setExpr(when (f.st) { St.KO -> "ko"; St.HIT -> "hit"; St.WIN -> "win"; St.ATTACK -> "angry"; else -> "normal" })
    }

    private fun progOf(f: Fighter): Float {
        val m = f.move ?: return 0f
        if (m.id == Mid.AIRKICK) return 1f
        if (f.moveT < m.from) return f.moveT / m.from.toFloat()
        if (f.moveT <= m.to) return 1f
        return max(0f, 1f - (f.moveT - m.to) / (m.total - m.to).toFloat())
    }

    private fun pose(f: Fighter, t: Int) {
        val P = tgt
        P[LEAN] = 0.12f; P[TILT] = 0f; P[SHN] = 0.95f; P[ELN] = 1.7f; P[SHF] = 0.55f; P[ELF] = 1.95f
        P[HIPN] = 0.3f; P[KNN] = -0.45f; P[HIPF] = -0.32f; P[KNF] = -0.5f; P[YAW] = 0f; P[BODYZ] = 0f; P[BODYY] = 0f; P[SQUASH] = 1f
        P[NCH - 1] = 0.35f
        val bob = sin(t * 0.11f + f.phase)
        fun ease(x: Float) = x * x * (3f - 2f * x)
        when (f.st) {
            St.IDLE -> { P[LEAN] += bob * 0.03f; P[HIPN] += bob * 0.05f; P[HIPF] -= bob * 0.03f; P[SHN] += bob * 0.06f; P[SHF] += bob * 0.05f }
            St.WALK -> {
                val w = f.walkPhase; val dir = if (f.vx * f.facing >= 0f) 1f else -1f
                P[HIPN] = 0.15f + sin(w) * 0.75f * dir; P[KNN] = -0.35f - max(0f, -sin(w) * dir) * 0.9f
                P[HIPF] = 0.15f - sin(w) * 0.75f * dir; P[KNF] = -0.35f - max(0f, sin(w) * dir) * 0.9f
                P[LEAN] = 0.2f; P[SHN] += sin(w) * 0.2f; P[SHF] -= sin(w) * 0.2f
            }
            St.JUMP -> { P[HIPN] = 0.9f; P[KNN] = -1.5f; P[HIPF] = 0.25f; P[KNF] = -1.1f; P[SHN] = 1.5f; P[ELN] = 0.5f; P[SHF] = 1.3f; P[ELF] = 0.6f; P[LEAN] = 0.05f }
            St.CROUCH -> { P[HIPN] = 1.15f; P[KNN] = -1.25f; P[HIPF] = 0.95f; P[KNF] = -1.15f; P[LEAN] = 0.35f; P[SHN] = 1.0f; P[SHF] = 0.7f; P[ELN] = 1.9f; P[ELF] = 2.0f }
            St.BLOCK -> { P[SHN] = 1.25f; P[ELN] = 2.35f; P[SHF] = 1.35f; P[ELF] = 2.4f; P[LEAN] = 0f; P[HIPN] = 0.4f; P[KNN] = -0.7f; P[HIPF] = -0.4f; P[KNF] = -0.7f; P[TILT] = 0.15f }
            St.HIT -> { P[LEAN] = -0.4f; P[TILT] = -0.35f; P[SHN] = -0.5f; P[ELN] = 0.6f; P[SHF] = -0.8f; P[ELF] = 0.5f; P[HIPN] = 0.45f; P[KNN] = -0.6f; P[HIPF] = -0.5f; P[KNF] = -0.4f; P[NCH - 1] = 0.6f }
            St.KO -> {
                val a = ease(minOf(1f, f.koT / 22f))
                P[BODYZ] = a * 1.5f; P[BODYY] = a * 0.5f; P[NCH - 1] = 0.5f
                P[SHN] = 2.2f; P[ELN] = 0.4f; P[SHF] = 1.8f; P[ELF] = 0.3f; P[HIPN] = 0.2f; P[KNN] = -0.2f; P[HIPF] = -0.1f; P[KNF] = -0.3f; P[LEAN] = 0f; P[TILT] = -0.3f
            }
            St.WIN -> {
                P[SHN] = 2.9f + sin(t * 0.3f) * 0.15f; P[ELN] = 0.2f; P[SHF] = 2.7f - sin(t * 0.3f) * 0.15f; P[ELF] = 0.3f
                P[LEAN] = -0.05f; P[HIPN] = 0.2f; P[KNN] = -0.3f; P[HIPF] = -0.2f; P[KNF] = -0.3f
                P[BODYY] = abs(sin(t * 0.15f)) * 0.35f; P[NCH - 1] = 0.4f
            }
            St.ATTACK -> {
                val m = f.move ?: return
                val e = ease(progOf(f)); P[NCH - 1] = 0.75f
                when (m.id) {
                    Mid.P1 -> { P[SHN] = 0.7f + 0.9f * e; P[ELN] = 1.9f - 1.85f * e; P[LEAN] = 0.12f + 0.28f * e; P[YAW] = -0.35f * e; P[HIPN] = 0.5f; P[KNN] = -0.7f; P[HIPF] = -0.6f; P[KNF] = -0.5f }
                    Mid.P2 -> { P[SHF] = 0.7f + 0.9f * e; P[ELF] = 1.9f - 1.85f * e; P[LEAN] = 0.12f + 0.28f * e; P[YAW] = 0.35f * e; P[HIPN] = 0.5f; P[KNN] = -0.7f; P[HIPF] = -0.6f; P[KNF] = -0.5f }
                    Mid.P3 -> { P[SHN] = 2.9f - 1.6f * e; P[ELN] = 0.25f; P[SHF] = 2.5f - 1.5f * e; P[ELF] = 0.4f; P[LEAN] = -0.15f + 0.7f * e; P[YAW] = -0.5f * e; P[HIPN] = 0.7f; P[KNN] = -0.8f; P[HIPF] = -0.7f; P[KNF] = -0.5f }
                    Mid.KICK -> { P[HIPN] = 0.2f + 1.3f * e; P[KNN] = -0.7f + 0.6f * e; P[LEAN] = -0.25f * e; P[HIPF] = -0.1f; P[KNF] = -0.3f; P[SHN] = 0.6f; P[SHF] = 1.0f }
                    Mid.KICK2 -> { P[HIPN] = 0.2f + 1.65f * e; P[KNN] = -0.8f + 0.7f * e; P[LEAN] = -0.5f * e; P[YAW] = 0.6f * e; P[HIPF] = -0.1f; P[KNF] = -0.3f; P[SHN] = 0.3f; P[SHF] = 1.6f }
                    Mid.SWEEP -> { P[HIPN] = 0.6f + 0.9f * e; P[KNN] = -0.9f + 0.85f * e; P[HIPF] = 1.15f; P[KNF] = -1.4f; P[LEAN] = -0.25f * e + 0.25f; P[SHN] = 0.7f; P[SHF] = 1.0f }
                    Mid.UPPER -> { P[SHN] = 0.4f + 2.55f * e; P[ELN] = 1.6f - 1.4f * e; P[LEAN] = -0.25f * e; P[HIPN] = 0.6f; P[KNN] = -0.8f; P[HIPF] = -0.5f; P[KNF] = -0.6f; P[BODYY] = 0.12f * e }
                    Mid.AIRKICK -> { P[HIPN] = 1.35f; P[KNN] = -0.15f; P[HIPF] = 0.3f; P[KNF] = -1.3f; P[LEAN] = -0.35f; P[SHN] = 1.5f; P[SHF] = -0.3f; P[ELN] = 0.5f }
                    Mid.GRAB -> { P[SHN] = 0.8f + 0.75f * e; P[SHF] = 0.8f + 0.75f * e; P[ELN] = 0.3f; P[ELF] = 0.3f; P[LEAN] = 0.25f + 0.2f * e; P[HIPN] = 0.6f; P[KNN] = -0.7f; P[HIPF] = -0.6f; P[KNF] = -0.5f }
                    Mid.SUPER -> {
                        if (f.moveT < m.from) {
                            val ch = minOf(1f, f.moveT / m.from.toFloat())
                            P[SHN] = -0.4f; P[ELN] = 1.2f; P[SHF] = -0.6f; P[ELF] = 1.4f; P[LEAN] = -0.3f * ch; P[HIPN] = 0.5f; P[KNN] = -0.7f; P[HIPF] = -0.5f; P[KNF] = -0.6f; P[SQUASH] = 1f - 0.06f * ch
                        } else {
                            val r = minOf(1f, (f.moveT - m.from) / 4f)
                            P[SHN] = 1.55f * r - 0.4f * (1 - r); P[SHF] = 1.45f * r - 0.6f * (1 - r); P[ELN] = 0.1f; P[ELF] = 0.15f
                            P[LEAN] = 0.45f * r - 0.3f * (1 - r); P[HIPN] = 0.6f; P[KNN] = -0.7f; P[HIPF] = -0.6f; P[KNF] = -0.5f
                        }
                    }
                }
            }
        }
    }

    /** Super projectile visuals. */
    companion object {
        fun superMesh(sup: SuperDef): Node {
            val g = Node(); val c = sup.color
            fun add(mesh: MeshData, col: Int, x: Float = 0f, y: Float = 0f, z: Float = 0f, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f): Node {
                val n = Node(mesh, col); n.px = x; n.py = y; n.pz = z; n.rx = rx; n.ry = ry; n.rz = rz; n.outline = 1.1f; g.add(n); return n
            }
            when (sup.shape) {
                "chair" -> {
                    add(Geo.box(1.1f, 0.2f, 1.0f), 0xfbbf24); add(Geo.box(1.1f, 1.1f, 0.18f), 0xfbbf24, 0f, 0.6f, -0.45f)
                    for (p in arrayOf(floatArrayOf(-.45f, -.4f), floatArrayOf(.45f, -.4f), floatArrayOf(-.45f, .4f), floatArrayOf(.45f, .4f))) add(Geo.box(0.14f, 0.7f, 0.14f), 0xd97706, p[0], -0.4f, p[1])
                    g.scale(1.15f)
                }
                "cup" -> { add(Geo.cyl(0.55f, 0.38f, 0.9f, 16), 0xc2410c); add(Geo.cyl(0.52f, 0.52f, 0.06f, 16), 0xb45309, 0f, 0.42f, 0f); add(Geo.torus(0.3f, 0.07f), 0xc2410c, 0.6f, 0f, 0f) }
                "scroll" -> for (i in 0 until 3) {
                    add(Geo.cyl(0.18f, 0.18f, 1.0f, 12), 0xfffaf0, 0f, (i - 1) * 0.5f, (i - 1) * 0.2f, 0f, 0f, PIf / 2)
                    add(Geo.cyl(0.2f, 0.2f, 0.1f, 12), 0xef4444, 0.5f, (i - 1) * 0.5f, (i - 1) * 0.2f, 0f, 0f, PIf / 2)
                }
                "mega" -> { add(Geo.cone(0.8f, 1.4f, 16), 0xff6b35, 0f, 0f, 0f, 0f, 0f, -PIf / 2); add(Geo.cyl(0.25f, 0.25f, 0.7f, 10), 0x333333, -0.9f, 0f, 0f, 0f, 0f, PIf / 2) }
                "barrier" -> {
                    add(Geo.box(2.2f, 0.28f, 0.28f), 0xffffff)
                    for (i in 0 until 4) add(Geo.box(0.3f, 0.3f, 0.3f), 0xef4444, -0.8f + i * 0.52f, 0f, 0f)
                    add(Geo.cyl(0.18f, 0.18f, 1.4f, 10), 0x555555, -1.1f, -0.5f, 0f)
                }
                "phone" -> {
                    add(Geo.box(0.7f, 1.3f, 0.14f), 0x1f2937)
                    val sc = Node(Geo.box(0.58f, 1.1f, 0.04f), c); sc.mat = MAT_BASIC; sc.pz = 0.09f; g.add(sc); g.scale(1.3f)
                }
                "laddoo" -> for (i in 0 until 4) add(Geo.sphere(0.34f, 14, 10), 0xf59e0b, -i * 0.5f, sin(i * 2f) * 0.35f, cos(i * 2f) * 0.3f)
                else -> add(Geo.sphere(0.6f, 16, 12), c)
            }
            val halo = Node(Geo.sphere(1.2f, 14, 10), c); halo.mat = MAT_GLOW; halo.alpha = 0.4f; g.add(halo)
            return g
        }
    }
}
