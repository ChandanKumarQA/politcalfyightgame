package com.comedyarena.politicalfight.game3d.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

const val PIf = 3.14159265f

/** Column-major 4x4 matrix helpers (OpenGL layout). Output arrays must not alias inputs. */
object M4 {
    fun identity(o: FloatArray) {
        o.fill(0f); o[0] = 1f; o[5] = 1f; o[10] = 1f; o[15] = 1f
    }

    fun mul(o: FloatArray, a: FloatArray, b: FloatArray) {
        for (c in 0..3) {
            val b0 = b[c * 4]; val b1 = b[c * 4 + 1]; val b2 = b[c * 4 + 2]; val b3 = b[c * 4 + 3]
            o[c * 4] = a[0] * b0 + a[4] * b1 + a[8] * b2 + a[12] * b3
            o[c * 4 + 1] = a[1] * b0 + a[5] * b1 + a[9] * b2 + a[13] * b3
            o[c * 4 + 2] = a[2] * b0 + a[6] * b1 + a[10] * b2 + a[14] * b3
            o[c * 4 + 3] = a[3] * b0 + a[7] * b1 + a[11] * b2 + a[15] * b3
        }
    }

    /** T * Rx * Ry * Rz * S  (same Euler order as three.js 'XYZ'). */
    fun compose(o: FloatArray, px: Float, py: Float, pz: Float, rx: Float, ry: Float, rz: Float, sx: Float, sy: Float, sz: Float) {
        val a = cos(rx); val b = sin(rx); val c = cos(ry); val d = sin(ry); val e = cos(rz); val f = sin(rz)
        val ae = a * e; val af = a * f; val be = b * e; val bf = b * f
        o[0] = c * e * sx; o[1] = (af + be * d) * sx; o[2] = (bf - ae * d) * sx; o[3] = 0f
        o[4] = -c * f * sy; o[5] = (ae - bf * d) * sy; o[6] = (be + af * d) * sy; o[7] = 0f
        o[8] = d * sz; o[9] = -b * c * sz; o[10] = a * c * sz; o[11] = 0f
        o[12] = px; o[13] = py; o[14] = pz; o[15] = 1f
    }

    fun perspective(o: FloatArray, fovY: Float, aspect: Float, n: Float, f: Float) {
        val t = 1f / tan(fovY / 2f)
        o.fill(0f)
        o[0] = t / aspect; o[5] = t; o[10] = (f + n) / (n - f); o[11] = -1f; o[14] = 2f * f * n / (n - f)
    }

    fun lookAt(o: FloatArray, ex: Float, ey: Float, ez: Float, tx: Float, ty: Float, tz: Float, ux: Float, uy: Float, uz: Float) {
        var zx = ex - tx; var zy = ey - ty; var zz = ez - tz
        var l = sqrt(zx * zx + zy * zy + zz * zz); zx /= l; zy /= l; zz /= l
        var xx = uy * zz - uz * zy; var xy = uz * zx - ux * zz; var xz = ux * zy - uy * zx
        l = sqrt(xx * xx + xy * xy + xz * xz); xx /= l; xy /= l; xz /= l
        val yx = zy * xz - zz * xy; val yy = zz * xx - zx * xz; val yz = zx * xy - zy * xx
        o[0] = xx; o[1] = yx; o[2] = zx; o[3] = 0f
        o[4] = xy; o[5] = yy; o[6] = zy; o[7] = 0f
        o[8] = xz; o[9] = yz; o[10] = zz; o[11] = 0f
        o[12] = -(xx * ex + xy * ey + xz * ez); o[13] = -(yx * ex + yy * ey + yz * ez); o[14] = -(zx * ex + zy * ey + zz * ez); o[15] = 1f
    }

    /** Inverse-transpose of the upper 3x3 of [m], column-major 3x3 in [o]. */
    fun normalMatrix(m: FloatArray, o: FloatArray) {
        val a00 = m[0]; val a01 = m[4]; val a02 = m[8]
        val a10 = m[1]; val a11 = m[5]; val a12 = m[9]
        val a20 = m[2]; val a21 = m[6]; val a22 = m[10]
        val c00 = a11 * a22 - a12 * a21; val c01 = -(a10 * a22 - a12 * a20); val c02 = a10 * a21 - a11 * a20
        val c10 = -(a01 * a22 - a02 * a21); val c11 = a00 * a22 - a02 * a20; val c12 = -(a00 * a21 - a01 * a20)
        val c20 = a01 * a12 - a02 * a11; val c21 = -(a00 * a12 - a02 * a10); val c22 = a00 * a11 - a01 * a10
        var d = a00 * c00 + a01 * c01 + a02 * c02
        if (d > -1e-12f && d < 1e-12f) d = 1e-12f
        val i = 1f / d
        o[0] = c00 * i; o[1] = c10 * i; o[2] = c20 * i
        o[3] = c01 * i; o[4] = c11 * i; o[5] = c21 * i
        o[6] = c02 * i; o[7] = c12 * i; o[8] = c22 * i
    }

    /** Transform point by [m]; result written to [o] (x,y,z,w). */
    fun transform(m: FloatArray, x: Float, y: Float, z: Float, o: FloatArray) {
        o[0] = m[0] * x + m[4] * y + m[8] * z + m[12]
        o[1] = m[1] * x + m[5] * y + m[9] * z + m[13]
        o[2] = m[2] * x + m[6] * y + m[10] * z + m[14]
        o[3] = m[3] * x + m[7] * y + m[11] * z + m[15]
    }
}

fun clampf(v: Float, a: Float, b: Float) = if (v < a) a else if (v > b) b else v
