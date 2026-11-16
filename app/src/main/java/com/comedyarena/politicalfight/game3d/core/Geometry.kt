package com.comedyarena.politicalfight.game3d.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Indexed triangle mesh (CCW = front face). GL handles are filled lazily by the renderer. */
class MeshData(val pos: FloatArray, val nor: FloatArray, val uv: FloatArray, val idx: ShortArray) {
    @JvmField var vbo = 0
    @JvmField var ibo = 0
    val triCount get() = idx.size / 3
}

private class MeshBuilder {
    val p = ArrayList<Float>(); val n = ArrayList<Float>(); val t = ArrayList<Float>(); val i = ArrayList<Short>()
    fun v(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, vv: Float): Int {
        p.add(x); p.add(y); p.add(z); n.add(nx); n.add(ny); n.add(nz); t.add(u); t.add(vv)
        return p.size / 3 - 1
    }
    fun tri(a: Int, b: Int, c: Int) { i.add(a.toShort()); i.add(b.toShort()); i.add(c.toShort()) }
    fun build() = MeshData(p.toFloatArray(), n.toFloatArray(), t.toFloatArray(), i.toShortArray())
}

object Geo {
    private val cache = HashMap<String, MeshData>()
    fun resetGl() { for (m in cache.values) { m.vbo = 0; m.ibo = 0 } }
    private inline fun get(key: String, make: () -> MeshData): MeshData = cache.getOrPut(key, make)

    /** three.js-style sphere. theta is measured from +Y. */
    fun sphere(r: Float, ws: Int = 20, hs: Int = 14, thetaStart: Float = 0f, thetaLen: Float = PIf): MeshData =
        get("s$r,$ws,$hs,$thetaStart,$thetaLen") {
            val b = MeshBuilder(); val grid = Array(hs + 1) { IntArray(ws + 1) }
            for (iy in 0..hs) {
                val vv = iy.toFloat() / hs
                for (ix in 0..ws) {
                    val u = ix.toFloat() / ws; val phi = u * 2f * PIf; val th = thetaStart + vv * thetaLen
                    val x = -cos(phi) * sin(th); val y = cos(th); val z = sin(phi) * sin(th)
                    grid[iy][ix] = b.v(r * x, r * y, r * z, x, y, z, u, 1f - vv)
                }
            }
            for (iy in 0 until hs) for (ix in 0 until ws) {
                val a = grid[iy][ix + 1]; val bb = grid[iy][ix]; val c = grid[iy + 1][ix]; val d = grid[iy + 1][ix + 1]
                if (iy != 0 || thetaStart > 0f) b.tri(a, bb, d)
                if (iy != hs - 1 || thetaStart + thetaLen < PIf - 1e-4f) b.tri(bb, c, d)
            }
            b.build()
        }

    /** Surface of revolution around Y. profile rows: radius, y, normalRadial, normalY (bottom -> top). */
    private fun lathe(profile: Array<FloatArray>, seg: Int): MeshData {
        val b = MeshBuilder(); val rows = Array(profile.size) { IntArray(seg + 1) }
        for (k in profile.indices) {
            val r = profile[k][0]; val y = profile[k][1]; val nr = profile[k][2]; val ny = profile[k][3]
            for (j in 0..seg) {
                val ph = j.toFloat() / seg * 2f * PIf; val c = cos(ph); val s = -sin(ph)
                rows[k][j] = b.v(r * c, y, r * s, nr * c, ny, nr * s, j.toFloat() / seg, k.toFloat() / (profile.size - 1))
            }
        }
        for (k in 0 until profile.size - 1) for (j in 0 until seg) {
            val p00 = rows[k][j]; val p01 = rows[k][j + 1]; val p10 = rows[k + 1][j]; val p11 = rows[k + 1][j + 1]
            b.tri(p00, p01, p11); b.tri(p00, p11, p10)
        }
        return b.build()
    }

    /** Capsule along Y: total height = len + 2r. */
    fun cap(r: Float, len: Float, seg: Int = 14, rings: Int = 5): MeshData = get("c$r,$len,$seg,$rings") {
        val rows = ArrayList<FloatArray>(); val h = len / 2f
        for (k in 0..rings) { val a = -PIf / 2f + (PIf / 2f) * k / rings; rows.add(floatArrayOf(r * cos(a), -h + r * sin(a), cos(a), sin(a))) }
        for (k in 0..rings) { val a = (PIf / 2f) * k / rings; rows.add(floatArrayOf(r * cos(a), h + r * sin(a), cos(a), sin(a))) }
        lathe(rows.toTypedArray(), seg)
    }

    /** Cylinder / cone (rt = 0). Closed caps. */
    fun cyl(rt: Float, rb: Float, h: Float, seg: Int = 20): MeshData = get("y$rt,$rb,$h,$seg") {
        val dr = rb - rt; val l = sqrt(dr * dr + h * h); val nr = h / l; val ny = dr / l
        lathe(arrayOf(
            floatArrayOf(0f, -h / 2, 0f, -1f), floatArrayOf(rb, -h / 2, 0f, -1f),
            floatArrayOf(rb, -h / 2, nr, ny), floatArrayOf(rt, h / 2, nr, ny),
            floatArrayOf(rt, h / 2, 0f, 1f), floatArrayOf(0f, h / 2, 0f, 1f)), seg)
    }

    fun cone(r: Float, h: Float, seg: Int = 14): MeshData = cyl(0f, r, h, seg)

    fun box(w: Float, h: Float, d: Float): MeshData = get("b$w,$h,$d") {
        val b = MeshBuilder(); val x = w / 2; val y = h / 2; val z = d / 2
        fun face(nx: Float, ny: Float, nz: Float, ax: FloatArray, bx: FloatArray, c: FloatArray) {
            // c = centre, ax / bx = half-extent vectors (right, up when viewed from outside)
            val v0 = b.v(c[0] - ax[0] - bx[0], c[1] - ax[1] - bx[1], c[2] - ax[2] - bx[2], nx, ny, nz, 0f, 0f)
            val v1 = b.v(c[0] + ax[0] - bx[0], c[1] + ax[1] - bx[1], c[2] + ax[2] - bx[2], nx, ny, nz, 1f, 0f)
            val v2 = b.v(c[0] + ax[0] + bx[0], c[1] + ax[1] + bx[1], c[2] + ax[2] + bx[2], nx, ny, nz, 1f, 1f)
            val v3 = b.v(c[0] - ax[0] + bx[0], c[1] - ax[1] + bx[1], c[2] - ax[2] + bx[2], nx, ny, nz, 0f, 1f)
            b.tri(v0, v1, v2); b.tri(v0, v2, v3)
        }
        face(0f, 0f, 1f, floatArrayOf(x, 0f, 0f), floatArrayOf(0f, y, 0f), floatArrayOf(0f, 0f, z))
        face(0f, 0f, -1f, floatArrayOf(-x, 0f, 0f), floatArrayOf(0f, y, 0f), floatArrayOf(0f, 0f, -z))
        face(1f, 0f, 0f, floatArrayOf(0f, 0f, -z), floatArrayOf(0f, y, 0f), floatArrayOf(x, 0f, 0f))
        face(-1f, 0f, 0f, floatArrayOf(0f, 0f, z), floatArrayOf(0f, y, 0f), floatArrayOf(-x, 0f, 0f))
        face(0f, 1f, 0f, floatArrayOf(x, 0f, 0f), floatArrayOf(0f, 0f, -z), floatArrayOf(0f, y, 0f))
        face(0f, -1f, 0f, floatArrayOf(x, 0f, 0f), floatArrayOf(0f, 0f, z), floatArrayOf(0f, -y, 0f))
        b.build()
    }

    fun torus(R: Float, tube: Float, rs: Int = 8, ts: Int = 24): MeshData = get("t$R,$tube,$rs,$ts") {
        val b = MeshBuilder()
        for (j in 0..rs) for (i in 0..ts) {
            val u = i.toFloat() / ts * 2f * PIf; val v = j.toFloat() / rs * 2f * PIf
            val x = (R + tube * cos(v)) * cos(u); val y = (R + tube * cos(v)) * sin(u); val z = tube * sin(v)
            val cx = R * cos(u); val cy = R * sin(u)
            var nx = x - cx; var ny = y - cy; var nz = z; val l = sqrt(nx * nx + ny * ny + nz * nz); nx /= l; ny /= l; nz /= l
            b.v(x, y, z, nx, ny, nz, i.toFloat() / ts, j.toFloat() / rs)
        }
        for (j in 1..rs) for (i in 1..ts) {
            val a = (ts + 1) * j + i - 1; val bb = (ts + 1) * (j - 1) + i - 1; val c = (ts + 1) * (j - 1) + i; val d = (ts + 1) * j + i
            b.tri(a, bb, d); b.tri(bb, c, d)
        }
        b.build()
    }


    /** Open tube with INWARD normals (draw with CULL_FRONT to see the inside). */
    fun tube(r: Float, h: Float, seg: Int = 48): MeshData = get("u$r,$h,$seg") {
        lathe(arrayOf(floatArrayOf(r, -h / 2, -1f, 0f), floatArrayOf(r, h / 2, -1f, 0f)), seg)
    }

    /** Flat ring in XY facing +Z. */
    fun ring(ri: Float, ro: Float, seg: Int = 32): MeshData = get("r$ri,$ro,$seg") {
        val b = MeshBuilder()
        for (i in 0..seg) { val a = i.toFloat() / seg * 2f * PIf; val c = cos(a); val s = sin(a)
            b.v(c * ri, s * ri, 0f, 0f, 0f, 1f, i.toFloat() / seg, 0f); b.v(c * ro, s * ro, 0f, 0f, 0f, 1f, i.toFloat() / seg, 1f) }
        for (i in 0 until seg) { val a = i * 2; b.tri(a, a + 1, a + 3); b.tri(a, a + 3, a + 2) }
        b.build()
    }

    /** Flat star in XY facing +Z. */
    fun star(ri: Float, ro: Float, pts: Int = 8): MeshData = get("a$ri,$ro,$pts") {
        val b = MeshBuilder(); val c0 = b.v(0f, 0f, 0f, 0f, 0f, 1f, .5f, .5f); val n = pts * 2
        for (i in 0 until n) { val a = i.toFloat() / n * 2f * PIf; val r = if (i % 2 == 1) ri else ro; b.v(cos(a) * r, sin(a) * r, 0f, 0f, 0f, 1f, 0f, 0f) }
        for (i in 0 until n) b.tri(c0, 1 + i, 1 + (i + 1) % n)
        b.build()
    }

    /** Plane in XY facing +Z. */
    fun plane(w: Float, h: Float): MeshData = get("p$w,$h") {
        val b = MeshBuilder(); val x = w / 2; val y = h / 2
        val a = b.v(-x, -y, 0f, 0f, 0f, 1f, 0f, 0f); val bb = b.v(x, -y, 0f, 0f, 0f, 1f, 1f, 0f)
        val c = b.v(x, y, 0f, 0f, 0f, 1f, 1f, 1f); val d = b.v(-x, y, 0f, 0f, 0f, 1f, 0f, 1f)
        b.tri(a, bb, c); b.tri(a, c, d); b.build()
    }
}
