package com.comedyarena.politicalfight.game3d.core

const val MAT_TOON = 0     // lit, 3-step toon shading
const val MAT_BASIC = 1    // unlit
const val MAT_GLOW = 2     // unlit, additive + transparent

const val PAT_NONE = 0
const val PAT_CHECKER = 1  // uv * (sx, sy), alternating color / color2
const val PAT_STRIPES = 2  // vertical wooden slats
const val PAT_WINDOWS = 3  // building facade
const val PAT_GRADIENT = 4 // sky: color (top) / color2 (middle) / color3 (bottom) by v
const val PAT_TEXTURE = 5  // named texture (see Node.tex)
const val PAT_RADIAL = 6   // soft radial alpha falloff (blob shadow)

const val CULL_BACK = 0
const val CULL_FRONT = 1   // draw inside faces
const val CULL_NONE = 2

/** Scene-graph node. Transform order: T * Rx * Ry * Rz * S (three.js 'XYZ'). */
class Node(var mesh: MeshData? = null, var color: Int = 0xffffff) {
    @JvmField var px = 0f; @JvmField var py = 0f; @JvmField var pz = 0f
    @JvmField var rx = 0f; @JvmField var ry = 0f; @JvmField var rz = 0f
    @JvmField var sx = 1f; @JvmField var sy = 1f; @JvmField var sz = 1f
    @JvmField var visible = true
    @JvmField var mat = MAT_TOON
    @JvmField var alpha = 1f
    @JvmField var outline = 0f          // >0: inverted-hull outline, scale factor (e.g. 1.09)
    @JvmField var emissive = 0f         // 0..1 white flash
    @JvmField var cull = CULL_BACK
    @JvmField var pattern = PAT_NONE
    @JvmField var color2 = 0xffffff
    @JvmField var color3 = 0xffffff
    @JvmField var patU = 1f
    @JvmField var patV = 1f
    @JvmField var patSeed = 0f
    @JvmField var tex: String? = null
    @JvmField var noFog = false
    @JvmField var noDepth = false
    val children = ArrayList<Node>()
    var parent: Node? = null
    val local = FloatArray(16)
    val world = FloatArray(16)

    fun add(c: Node): Node { c.parent?.children?.remove(c); c.parent = this; children.add(c); return c }
    fun remove(c: Node) { if (children.remove(c)) c.parent = null }

    fun pos(x: Float, y: Float, z: Float): Node { px = x; py = y; pz = z; return this }
    fun rot(x: Float, y: Float, z: Float): Node { rx = x; ry = y; rz = z; return this }
    fun scale(x: Float, y: Float, z: Float): Node { sx = x; sy = y; sz = z; return this }
    fun scale(s: Float): Node { sx = s; sy = s; sz = s; return this }

    fun updateWorld(parentWorld: FloatArray?) {
        M4.compose(local, px, py, pz, rx, ry, rz, sx, sy, sz)
        if (parentWorld == null) System.arraycopy(local, 0, world, 0, 16) else M4.mul(world, parentWorld, local)
        for (i in 0 until children.size) children[i].updateWorld(world)
    }
}

class Light(
    var sky: Int = 0xffffff, var ground: Int = 0x444444, var hemi: Float = 1f,
    var dir: Int = 0xffffff, var dirI: Float = 1f,
    var dx: Float = -4f, var dy: Float = 10f, var dz: Float = 8f
)
