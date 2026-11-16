package com.comedyarena.politicalfight.game3d.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class Arena(
    val group: Node, val bg: Int, val fogColor: Int, val fogNear: Float, val fogFar: Float,
    val light: Light, val update: (Float) -> Unit
)

object Arenas {
    private val built = arrayOfNulls<Arena>(3)
    fun get(i: Int): Arena { if (built[i] == null) built[i] = when (i) { 0 -> hall(); 1 -> rally(); else -> road() }; return built[i]!! }

    private class Rnd(var s: Long) { fun next(): Float { s = (s * 16807L) % 2147483647L; return (s - 1).toFloat() / 2147483646f } }

    private fun box(parent: Node, w: Float, h: Float, d: Float, color: Int, x: Float = 0f, y: Float = 0f, z: Float = 0f, basic: Boolean = false): Node {
        val n = Node(Geo.box(w, h, d), color); n.px = x; n.py = y; n.pz = z; if (basic) n.mat = MAT_BASIC; parent.add(n); return n
    }
    private fun mesh(parent: Node, m: MeshData, color: Int, x: Float = 0f, y: Float = 0f, z: Float = 0f, basic: Boolean = false): Node {
        val n = Node(m, color); n.px = x; n.py = y; n.pz = z; if (basic) n.mat = MAT_BASIC; parent.add(n); return n
    }
    private fun floorPlane(parent: Node, w: Float, h: Float, color: Int, y: Float, z: Float): Node {
        val n = Node(Geo.plane(w, h), color); n.rx = -PIf / 2; n.py = y; n.pz = z; parent.add(n); return n
    }

    private fun skyDome(top: Int, mid: Int, bot: Int): Node {
        val n = Node(Geo.sphere(90f, 24, 16), top); n.mat = MAT_BASIC; n.pattern = PAT_GRADIENT; n.color2 = mid; n.color3 = bot
        n.cull = CULL_FRONT; n.noFog = true; return n
    }

    private class Crowd(parent: Node, val pts: List<FloatArray>, palette: IntArray, seed: Long, val sc: Float) {
        val heads = ArrayList<Node>(); val bodies = ArrayList<Node>()
        init {
            val r = Rnd(seed); val skins = intArrayOf(0xd9a066, 0xc68642, 0xb87a4b, 0xe0ac83, 0xcb9462, 0xe8b88a)
            for (p in pts) {
                val h = Node(Geo.sphere(0.36f, 10, 8), skins[(r.next() * skins.size).toInt()]); h.scale(sc)
                val b = Node(Geo.cap(0.34f, 0.55f, 10, 3), palette[(r.next() * palette.size).toInt()]); b.scale(sc)
                parent.add(h); parent.add(b); heads.add(h); bodies.add(b)
            }
        }
        fun update(t: Float) {
            for (i in pts.indices) {
                val p = pts[i]; val bob = max(0f, sin(t * 3.2f + p[3])) * 0.22f
                val b = bodies[i]; b.px = p[0]; b.py = p[1] + bob; b.pz = p[2]; b.rz = sin(t * 2f + p[3]) * 0.05f
                val h = heads[i]; h.px = p[0]; h.py = p[1] + bob + 0.78f * sc; h.pz = p[2]; h.rz = b.rz
            }
        }
    }

    // ------------------------------------------------------------------ SABHA HALL
    private fun hall(): Arena {
        val g = Node(); val R = Rnd(11)
        val floor = floorPlane(g, 50f, 36f, 0xe8d3a8, 0f, -2f); floor.pattern = PAT_CHECKER; floor.color2 = 0xa9764a; floor.patU = 36f; floor.patV = 26f
        val wall = Node(Geo.tube(19f, 12f, 64), 0xb0703c); wall.py = 6f; wall.pz = 3f; wall.cull = CULL_FRONT
        wall.pattern = PAT_STRIPES; wall.patU = 150f; wall.patSeed = 3f; g.add(wall)
        for (i in 0 until 15) {
            val a = (i / 14f - 0.5f) * 2.2f
            val p = Node(); p.px = sin(a) * 17f; p.pz = 4f - cos(a) * 17f; p.ry = -a; g.add(p)
            mesh(p, Geo.cyl(0.55f, 0.65f, 9f, 16), 0xc8935a, 0f, 4.5f, 0f)
            box(p, 1.6f, 0.5f, 1.6f, 0x7a4a26, 0f, 0.25f, 0f); box(p, 1.6f, 0.5f, 1.6f, 0x7a4a26, 0f, 9.1f, 0f)
        }
        val pts = ArrayList<FloatArray>()
        for (tier in 0 until 3) {
            val rad = 12.5f + tier * 2f; val y = 1f + tier * 1.35f; val segs = 22
            for (i in 0 until segs) {
                val a = (i / (segs - 1f) - 0.5f) * 2.4f
                val sb = box(g, 2.4f, y, 2.1f, if (tier % 2 == 1) 0x6b4428 else 0x5b3a22, sin(a) * rad, y / 2, 3f - cos(a) * rad); sb.ry = -a
            }
            val n = 30 + tier * 4
            for (i in 0 until n) { val a = (i / (n - 1f) - 0.5f) * 2.3f; pts.add(floatArrayOf(sin(a) * rad, y, 3f - cos(a) * rad, R.next() * 6f)) }
        }
        val crowd = Crowd(g, pts, intArrayOf(0xffffff, 0xf5f0e6, 0xf28c28, 0x16a34a, 0x2563eb, 0xd62839, 0xfbbf24), 5, 1f)
        // emblem
        val em = Node(); em.pos(0f, 6.8f, -12.7f); g.add(em)
        mesh(em, Geo.cyl(2.3f, 2.3f, 0.3f, 40), 0xfbbf24).rx = PIf / 2
        mesh(em, Geo.torus(2.3f, 0.14f), 0x7a4a26, 0f, 0f, 0.1f)
        val dk = 0x5a2d12
        box(em, 1.9f, 0.35f, 0.3f, dk, 0f, 0.1f, 0.3f); box(em, 1.5f, 0.3f, 0.3f, dk, 0f, -0.35f, 0.3f)
        box(em, 1.5f, 1.7f, 0.3f, dk, 0f, 1.0f, 0.3f)
        box(em, 0.22f, 1.2f, 0.3f, dk, -0.65f, -1.0f, 0.3f); box(em, 0.22f, 1.2f, 0.3f, dk, 0.65f, -1.0f, 0.3f)
        // lamps
        val lamps = ArrayList<Node>()
        for (i in 0 until 7) {
            val l = Node(); l.pos((i - 3) * 5.2f, 8.5f, -3f - abs(i - 3) * 0.6f); g.add(l)
            mesh(l, Geo.cyl(0.03f, 0.03f, 3f, 6), 0x222222, 0f, 1.5f, 0f)
            mesh(l, Geo.sphere(0.5f, 12, 8), 0xffe08a, basic = true)
            mesh(l, Geo.cone(0.9f, 0.7f, 14), 0xb45309, 0f, 0.55f, 0f)
            lamps.add(l)
        }
        for (i in 0 until 9) {
            val x = -9f + i * 2.25f; val z = 4f
            mesh(g, Geo.cyl(0.22f, 0.14f, 0.14f, 10), 0xb45309, x, 0.07f, z)
            mesh(g, Geo.cone(0.09f, 0.32f, 8), 0xffb020, x, 0.3f, z, basic = true)
        }
        return Arena(g, 0x1c0a08, 0x2a120a, 24f, 60f, Light(0xffe2b8, 0x6a3a1e, 0.95f, 0xfff1d6, 1.0f, -4f, 10f, 8f)) { t ->
            crowd.update(t); for (i in lamps.indices) lamps[i].rz = sin(t * 0.8f + i) * 0.04f
        }
    }

    // ------------------------------------------------------------------ RALLY MAIDAN
    private class Firework(val nodes: List<Node>, val vel: List<FloatArray>, val cx: Float, val cy: Float, val cz: Float, val off: Float)

    private fun rally(): Arena {
        val g = Node(); val R = Rnd(23)
        g.add(skyDome(0x1a0b3d, 0x7a2d6b, 0xff8a3d))
        for (i in 0 until 70) {
            val a = R.next() * 2f * PIf; val e = 0.25f + R.next() * 1.0f
            val s = Node(Geo.sphere(0.22f, 4, 3), 0xffffff); s.mat = MAT_BASIC; s.noFog = true
            s.pos(cos(a) * cos(e) * 85f, sin(e) * 85f, -abs(sin(a)) * cos(e) * 85f - 10f); g.add(s)
        }
        val moon = Node(Geo.sphere(3f, 18, 12), 0xfff1b8); moon.mat = MAT_BASIC; moon.noFog = true; moon.pos(22f, 22f, -70f); g.add(moon)
        val dirt = floorPlane(g, 70f, 44f, 0x6b4a2e, 0f, -4f); dirt.pattern = PAT_CHECKER; dirt.color2 = 0x634327; dirt.patU = 46f; dirt.patV = 30f
        floorPlane(g, 16.4f, 3.6f, 0xfbbf24, 0.005f, 0f)
        floorPlane(g, 16f, 3.2f, 0xb3261e, 0.01f, 0f)
        val banner = Node(Geo.plane(14f, 3.5f), 0xffffff); banner.mat = MAT_BASIC; banner.pattern = PAT_TEXTURE; banner.tex = "banner"; banner.pos(0f, 4.7f, -9.5f); g.add(banner)
        box(g, 20f, 1.2f, 5f, 0x7c4a2a, 0f, 0.6f, -11.5f)
        for (x in floatArrayOf(-9f, 9f)) box(g, 0.5f, 9f, 0.5f, 0x2b2b30, x, 4.5f, -9.5f)
        box(g, 19f, 0.5f, 0.5f, 0x2b2b30, 0f, 9.2f, -9.5f)
        for (x in floatArrayOf(-11f, 11f)) {
            box(g, 2.2f, 4.2f, 1.6f, 0x1c1c22, x, 2.1f, -9f)
            for (y in floatArrayOf(1.2f, 3f)) mesh(g, Geo.cyl(0.7f, 0.7f, 0.2f, 18), 0x3f3f48, x, y, -8.1f).rx = PIf / 2
        }
        val beams = ArrayList<Node>()
        for (i in 0 until 4) {
            val pv = Node(); pv.pos(-7.5f + i * 5f, 9.2f, -9.4f)
            val b = Node(Geo.cone(2.4f, 11f, 18), 0xfff1b8); b.mat = MAT_GLOW; b.alpha = 0.12f; b.cull = CULL_NONE; b.py = -5.5f
            pv.add(b); g.add(pv); beams.add(pv)
        }
        val flags = ArrayList<Node>(); val fc = intArrayOf(0xfbbf24, 0x22c55e, 0x38bdf8, 0xf472b6, 0xef4444)
        for (line in 0 until 2) for (i in 0 until 26) {
            val x = -16f + i * 1.28f; val y = 7.6f - line * 1.6f - sin(i / 25f * PIf) * 0.9f
            val f = Node(Geo.cone(0.34f, 0.7f, 3), fc[(i + line) % 5]); f.mat = MAT_BASIC; f.cull = CULL_NONE
            f.rz = PIf; f.pos(x, y, -6f - line * 1.5f); g.add(f); flags.add(f)
        }
        val pts = ArrayList<FloatArray>()
        for (row in 0 until 4) for (i in 0 until 34) pts.add(floatArrayOf(-17f + i * 1.05f + (row % 2) * 0.5f + R.next() * 0.2f, row * 0.12f, -3.6f - row * 1.15f, R.next() * 6f))
        val crowd = Crowd(g, pts, intArrayOf(0xffffff, 0xf2e8cf, 0xf28c28, 0x16a34a, 0x2563eb, 0x7c3aed, 0xfacc15, 0xef4444), 9, 1f)
        val sc = intArrayOf(0xfbbf24, 0x22c55e, 0x60a5fa, 0xf472b6)
        for (i in 0 until 12) {
            val x = -15f + i * 2.7f + R.next(); val z = -4.8f - R.next() * 2.2f
            val s = Node(); s.pos(x, 2.3f, z); g.add(s)
            box(s, 0.07f, 1.6f, 0.07f, 0x6b4a2e); box(s, 1.2f, 0.8f, 0.06f, sc[i % 4], 0f, 0f, 0.9f, basic = true)
            flags.add(s)
        }
        val fws = ArrayList<Firework>(); val fcol = intArrayOf(0xffd166, 0x7ee787, 0xff6b9d)
        for (k in 0 until 3) {
            val nodes = ArrayList<Node>(); val vel = ArrayList<FloatArray>()
            for (i in 0 until 30) {
                val a = R.next() * 2f * PIf; val b = acos(2f * R.next() - 1f); val s = 2f + R.next() * 3f
                vel.add(floatArrayOf(sin(b) * cos(a) * s, cos(b) * s, sin(b) * sin(a) * s))
                val n = Node(Geo.sphere(0.3f, 4, 3), fcol[k]); n.mat = MAT_GLOW; n.noFog = true; n.visible = false; g.add(n); nodes.add(n)
            }
            fws.add(Firework(nodes, vel, -10f + k * 10f, 13f + k * 2f, -35f, k * 2.1f))
        }
        return Arena(g, 0x2b1055, 0x7a2d6b, 30f, 90f, Light(0xffc2a0, 0x4a2a30, 0.9f, 0xffe1b0, 0.95f, -5f, 9f, 9f)) { t ->
            crowd.update(t)
            for (i in beams.indices) { beams[i].rz = sin(t * 0.7f + i * 1.3f) * 0.5f; beams[i].rx = cos(t * 0.5f + i) * 0.15f }
            for (i in flags.indices) flags[i].rx = sin(t * 3f + i) * 0.12f
            for (w in fws) {
                val lt = ((t + w.off) % 4.2f) / 4.2f
                val op = if (lt < 0.12f) 0f else max(0f, 1f - lt)
                for (i in w.nodes.indices) {
                    val n = w.nodes[i]; val v = w.vel[i]
                    n.visible = op > 0.02f; n.alpha = op
                    n.pos(w.cx + v[0] * lt * 4f, w.cy + v[1] * lt * 4f - lt * lt * 8f, w.cz + v[2] * lt * 4f)
                }
            }
        }
    }

    // ------------------------------------------------------------------ DESI SADAK
    private fun road(): Arena {
        val g = Node(); val R = Rnd(31)
        g.add(skyDome(0x4aa8ea, 0xa9d8f5, 0xfdeccb))
        val sun = Node(Geo.sphere(4f, 18, 12), 0xfff3b0); sun.mat = MAT_BASIC; sun.noFog = true; sun.pos(-30f, 28f, -70f); g.add(sun)
        val clouds = ArrayList<Node>()
        for (i in 0 until 7) {
            val c = Node(); c.pos(-40f + i * 14f, 20f + R.next() * 8f, -60f - R.next() * 15f)
            for (j in 0 until 4) { val s = Node(Geo.sphere(2f + R.next() * 2f, 10, 8), 0xffffff); s.mat = MAT_BASIC; s.noFog = true; s.pos(j * 2.6f, R.next(), 0f); s.sy = 0.7f; c.add(s) }
            g.add(c); clouds.add(c)
        }
        floorPlane(g, 70f, 30f, 0x4b4b52, 0f, 4f)
        for (i in -6..6) { val d = Node(Geo.plane(2.6f, 0.22f), 0xe8dc9a); d.rx = -PIf / 2; d.pos(i * 5.2f, 0.012f, 1.2f); g.add(d) }
        box(g, 60f, 0.35f, 5f, 0x9a8f86, 0f, 0.17f, -8.2f)
        box(g, 60f, 0.4f, 0.4f, 0xe6dccb, 0f, 0.2f, -5.6f)
        floorPlane(g, 80f, 30f, 0x7c746b, -0.01f, -20f)
        val pal = intArrayOf(0xf2b8a2, 0xf6e0a4, 0xa9d6c7, 0xb9c4f0, 0xf0a8b8, 0xe6c8f0)
        for (i in 0 until 9) {
            val h = 7f + R.next() * 9f; val w = 4.4f + R.next() * 1.4f; val x = -23f + i * 5.8f
            val bz = -12f - R.next() * 1.5f
            val b = box(g, w, h, 4f, pal[i % pal.size], x, h / 2, bz)
            b.pattern = PAT_WINDOWS; b.color2 = 0x6d8aa8; b.color3 = 0xffe9a0; b.patU = (w / 1.5f).toInt().toFloat(); b.patV = (h / 1.5f).toInt().toFloat(); b.patSeed = i * 3.7f
            if (R.next() > 0.5f) mesh(g, Geo.cyl(0.7f, 0.7f, 1f, 12), 0x2b6cb0, x, h + 0.5f, bz)
            box(g, w * 0.8f, 0.9f, 0.2f, intArrayOf(0xef4444, 0x22c55e, 0xf59e0b, 0x3b82f6)[i % 4], x, 3.6f, bz + 2.1f)
        }
        val poleX = floatArrayOf(-14f, -4f, 6f, 16f)
        for (x in poleX) { mesh(g, Geo.cyl(0.14f, 0.18f, 8.5f, 8), 0x5a5a60, x, 4.25f, -6.5f); box(g, 2.6f, 0.18f, 0.18f, 0x5a5a60, x, 8f, -6.5f) }
        for (w in 0 until 3) for (i in 0 until poleX.size - 1) {
            val segs = 6
            for (s in 0 until segs) {
                val u0 = s / segs.toFloat(); val u1 = (s + 1) / segs.toFloat()
                val x0 = poleX[i] + (poleX[i + 1] - poleX[i]) * u0 + (w - 1) * 0.9f; val x1 = poleX[i] + (poleX[i + 1] - poleX[i]) * u1 + (w - 1) * 0.9f
                val y0 = 8f - sin(u0 * PIf) * 0.9f; val y1 = 8f - sin(u1 * PIf) * 0.9f
                val len = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0))
                val n = box(g, len, 0.035f, 0.035f, 0x1a1a1a, (x0 + x1) / 2, (y0 + y1) / 2, -6.5f, basic = true); n.rz = atan2(y1 - y0, x1 - x0)
            }
        }
        // chai tapri
        val tp = Node(); tp.pos(-8f, 0.35f, -7.5f); g.add(tp)
        box(tp, 3f, 1.3f, 1.5f, 0x8b5a2b, 0f, 0.65f, 0f); box(tp, 3.6f, 0.15f, 2.2f, 0xd62839, 0f, 2.6f, 0.1f)
        for (x in floatArrayOf(-1.6f, 1.6f)) box(tp, 0.1f, 2f, 0.1f, 0x4a3322, x, 1.6f, 0.8f)
        mesh(tp, Geo.cyl(0.35f, 0.45f, 0.6f, 14), 0xb8bcc4, -0.6f, 1.6f, 0f)
        // cow
        val cow = Node(); cow.pos(8.5f, 0.35f, -7f); cow.ry = -0.5f; g.add(cow)
        box(cow, 2.2f, 1.1f, 1.0f, 0xf0e4cc, 0f, 1.4f, 0f); box(cow, 0.7f, 0.55f, 0.8f, 0x5a3a22, -0.3f, 1.5f, 0.1f)
        box(cow, 0.8f, 0.8f, 0.7f, 0xf0e4cc, 1.45f, 1.75f, 0f); box(cow, 0.35f, 0.3f, 0.5f, 0xe8a0a0, 1.9f, 1.55f, 0f)
        for (p in arrayOf(floatArrayOf(-0.8f, 0.35f), floatArrayOf(-0.8f, -0.35f), floatArrayOf(0.8f, 0.35f), floatArrayOf(0.8f, -0.35f))) box(cow, 0.22f, 0.9f, 0.22f, 0xf0e4cc, p[0], 0.45f, p[1])
        val tail = Node(); tail.pos(-1.1f, 1.8f, 0f); cow.add(tail); box(tail, 0.1f, 0.9f, 0.1f, 0x5a3a22, 0f, -0.45f, 0f)
        // auto rickshaw
        val auto = Node(); g.add(auto)
        box(auto, 2.4f, 0.9f, 1.3f, 0x1f8a3a, 0f, 0.8f, 0f); box(auto, 1.5f, 0.12f, 1.4f, 0x111111, -0.3f, 1.9f, 0f)
        box(auto, 1.0f, 0.9f, 1.2f, 0xfbbf24, 0.9f, 1.4f, 0f); box(auto, 0.1f, 1.0f, 1.3f, 0xfacc15, -1.3f, 1.4f, 0f)
        for (p in arrayOf(floatArrayOf(0.9f, 0.5f), floatArrayOf(-0.9f, 0.5f), floatArrayOf(-0.9f, -0.5f))) mesh(auto, Geo.cyl(0.35f, 0.35f, 0.22f, 14), 0x111111, p[0], 0.35f, p[1]).rx = PIf / 2
        val pts = ArrayList<FloatArray>()
        for (i in 0 until 28) pts.add(floatArrayOf(-20f + i * 1.5f + R.next(), 0.35f, -9.5f - R.next() * 1.2f, R.next() * 6f))
        val crowd = Crowd(g, pts, intArrayOf(0xffffff, 0xf28c28, 0x2563eb, 0x16a34a, 0xd62839, 0xfacc15), 3, 0.95f)
        return Arena(g, 0x8fc8f0, 0xcfe8f5, 35f, 95f, Light(0xdff1ff, 0x8a7a68, 1.05f, 0xfff3d0, 1.05f, -6f, 12f, 8f)) { t ->
            crowd.update(t)
            for (i in clouds.indices) { val c = clouds[i]; c.px += 0.004f * (1f + i * 0.1f); if (c.px > 50f) c.px = -50f }
            tail.rz = sin(t * 3f) * 0.4f
            auto.pos(((t * 2.2f) % 56f) - 28f, 0f, -4.3f)
        }
    }
}
