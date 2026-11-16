package com.comedyarena.politicalfight.game3d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.comedyarena.politicalfight.game3d.core.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** OpenGL ES 2.0 toon renderer for the engine's scene graph. */
class GlRenderer(private val ctx: Context, private val engine: Engine) : GLSurfaceView.Renderer {
    private var prog = 0
    private val u = HashMap<String, Int>()
    private var aPos = 0; private var aNor = 0; private var aUV = 0
    private var bannerTex = 0
    private var last = 0L
    private val opq = ArrayList<Node>(900); private val trn = ArrayList<Node>(120)
    private val nm = FloatArray(9); private val tmpM = FloatArray(16)
    private val ids = IntArray(2)
    private var boundMesh: MeshData? = null
    private var lastKey = -1L

    companion object {
        private const val VS = """
uniform mat4 uModel; uniform mat4 uView; uniform mat4 uProj; uniform mat3 uNormM;
attribute vec3 aPos; attribute vec3 aNor; attribute vec2 aUV;
varying vec3 vN; varying vec2 vUV; varying float vDepth;
void main() {
  vec4 w = uModel * vec4(aPos, 1.0); vec4 v = uView * w;
  gl_Position = uProj * v; vDepth = -v.z; vN = uNormM * aNor; vUV = aUV;
}"""
        private const val FS = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform vec3 uColor; uniform vec3 uColor2; uniform vec3 uColor3;
uniform float uAlpha; uniform float uEmis; uniform float uMat; uniform float uPat; uniform float uOutline; uniform float uNoFog;
uniform vec4 uPatP;
uniform vec3 uSky; uniform vec3 uGround; uniform vec3 uDirC; uniform vec3 uLightDir; uniform vec2 uLightP;
uniform vec3 uFogC; uniform vec2 uFogP;
uniform sampler2D uTex;
varying vec3 vN; varying vec2 vUV; varying float vDepth;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
vec3 lin(vec3 c) { return pow(c, vec3(2.2)); }
void main() {
  vec3 col = uColor; float a = uAlpha;
  if (uPat > 0.5) {
    if (uPat < 1.5) { vec2 c = floor(vUV * uPatP.xy); col = mix(uColor, uColor2, mod(c.x + c.y, 2.0)); }
    else if (uPat < 2.5) { float x = vUV.x * uPatP.x; float id = floor(x); float f = fract(x);
      col = uColor * (0.8 + 0.3 * hash(vec2(id, uPatP.z))); if (f < 0.1) col *= 0.55; }
    else if (uPat < 3.5) { vec2 cell = vUV * uPatP.xy; vec2 id = floor(cell); vec2 f = fract(cell);
      if (f.x > 0.16 && f.x < 0.84 && f.y > 0.14 && f.y < 0.82) {
        col = hash(id + uPatP.z) > 0.7 ? uColor3 : uColor2;
        if (f.x < 0.22 || f.x > 0.78 || f.y < 0.2) col = vec3(0.23, 0.29, 0.35);
      } }
    else if (uPat < 4.5) { float v = vUV.y; col = v > 0.45 ? mix(uColor2, uColor, (v - 0.45) / 0.55) : mix(uColor3, uColor2, v / 0.45); }
    else if (uPat < 5.5) { vec4 t = texture2D(uTex, vec2(vUV.x, 1.0 - vUV.y)); col = t.rgb; a *= t.a; }
    else { float d = length(vUV - 0.5) * 2.0; a *= clamp(1.0 - d, 0.0, 1.0); }
  }
  vec3 c = lin(col); vec3 o;
  if (uOutline > 0.5) { o = lin(vec3(0.102, 0.051, 0.024)); }
  else if (uMat < 0.5) {
    vec3 n = normalize(vN); float nd = dot(n, normalize(uLightDir));
    float tone = nd < -0.333 ? 0.314 : (nd < 0.333 ? 0.627 : 1.0);
    vec3 hc = mix(lin(uGround), lin(uSky), n.y * 0.5 + 0.5) * uLightP.x;
    vec3 dc = lin(uDirC) * uLightP.y * tone;
    o = c * (hc * 0.55 + dc * 0.62);
  } else { o = c; }
  o = mix(o, vec3(1.0), uEmis);
  if (uNoFog < 0.5) { float f = clamp((vDepth - uFogP.x) / (uFogP.y - uFogP.x), 0.0, 1.0); o = mix(o, lin(uFogC), f); }
  gl_FragColor = vec4(pow(clamp(o, 0.0, 1.0), vec3(1.0 / 2.2)), a);
}"""
    }

    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
        val ok = IntArray(1); GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("shader: " + GLES20.glGetShaderInfoLog(s))
        return s
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, shader(GLES20.GL_VERTEX_SHADER, VS)); GLES20.glAttachShader(prog, shader(GLES20.GL_FRAGMENT_SHADER, FS))
        GLES20.glLinkProgram(prog); GLES20.glUseProgram(prog)
        for (n in arrayOf("uModel", "uView", "uProj", "uNormM", "uColor", "uColor2", "uColor3", "uAlpha", "uEmis", "uMat", "uPat", "uOutline", "uNoFog", "uPatP",
            "uSky", "uGround", "uDirC", "uLightDir", "uLightP", "uFogC", "uFogP", "uTex")) u[n] = GLES20.glGetUniformLocation(prog, n)
        aPos = GLES20.glGetAttribLocation(prog, "aPos"); aNor = GLES20.glGetAttribLocation(prog, "aNor"); aUV = GLES20.glGetAttribLocation(prog, "aUV")
        Geo.resetGl(); boundMesh = null; lastKey = -1L
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glEnable(GLES20.GL_CULL_FACE); GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        makeBanner(); last = System.nanoTime()
    }

    private fun makeBanner() {
        val bmp = Bitmap.createBitmap(1024, 256, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 1024f, 0f, Color.parseColor("#d62839"), Color.parseColor("#8c1d2a"), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, 1024f, 256f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 14f; p.color = Color.parseColor("#fbbf24"); c.drawRect(10f, 10f, 1014f, 246f, p)
        val tf = try { Typeface.createFromAsset(ctx.assets, "fonts/Bangers.ttf") } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        p.typeface = tf; p.textSize = 120f; p.textAlign = Paint.Align.CENTER; p.strokeJoin = Paint.Join.ROUND
        val txt = "CHAI · SAMOSA · ZINDABAD"; val y = 128f + 40f
        p.style = Paint.Style.STROKE; p.strokeWidth = 12f; p.color = Color.parseColor("#1a0d06"); c.drawText(txt, 512f, y, p)
        p.style = Paint.Style.FILL; p.color = Color.WHITE; c.drawText(txt, 512f, y, p)
        val t = IntArray(1); GLES20.glGenTextures(1, t, 0); bannerTex = t[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bannerTex)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0); bmp.recycle()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h); engine.setViewport(w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime(); val dt = (now - last) / 1e9f; last = now
        engine.advance(dt)
        val bg = engine.bg
        GLES20.glClearColor(((bg shr 16) and 255) / 255f, ((bg shr 8) and 255) / 255f, (bg and 255) / 255f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(prog)
        GLES20.glUniformMatrix4fv(u["uView"]!!, 1, false, engine.view, 0)
        GLES20.glUniformMatrix4fv(u["uProj"]!!, 1, false, engine.proj, 0)
        val L = engine.light
        rgb("uSky", L.sky); rgb("uGround", L.ground); rgb("uDirC", L.dir); rgb("uFogC", engine.fogColor)
        GLES20.glUniform3f(u["uLightDir"]!!, L.dx, L.dy, L.dz); GLES20.glUniform2f(u["uLightP"]!!, L.hemi, L.dirI)
        GLES20.glUniform2f(u["uFogP"]!!, engine.fogNear, engine.fogFar)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bannerTex); GLES20.glUniform1i(u["uTex"]!!, 0)
        opq.clear(); trn.clear(); collect(engine.scene)
        GLES20.glDisable(GLES20.GL_BLEND); GLES20.glDepthMask(true); GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        for (n in opq) {
            drawNode(n, false)
            if (n.outline > 0f && n.mat == MAT_TOON) drawNode(n, true)
        }
        GLES20.glEnable(GLES20.GL_BLEND); GLES20.glDepthMask(false)
        for (n in trn) {
            if (n.noDepth) GLES20.glDisable(GLES20.GL_DEPTH_TEST) else GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            if (n.mat == MAT_GLOW) GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE) else GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            drawNode(n, false)
        }
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA); GLES20.glDepthMask(true); GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun rgb(name: String, c: Int) = GLES20.glUniform3f(u[name]!!, ((c shr 16) and 255) / 255f, ((c shr 8) and 255) / 255f, (c and 255) / 255f)

    private fun collect(n: Node) {
        if (!n.visible) return
        if (n.mesh != null) { if (n.alpha < 0.999f || n.mat == MAT_GLOW) { if (n.alpha > 0.01f) trn.add(n) } else opq.add(n) }
        val ch = n.children
        for (i in 0 until ch.size) collect(ch[i])
    }

    private fun bind(m: MeshData) {
        if (m.vbo == 0) {
            val n = m.pos.size / 3; val f = FloatArray(n * 8)
            for (i in 0 until n) {
                f[i * 8] = m.pos[i * 3]; f[i * 8 + 1] = m.pos[i * 3 + 1]; f[i * 8 + 2] = m.pos[i * 3 + 2]
                f[i * 8 + 3] = m.nor[i * 3]; f[i * 8 + 4] = m.nor[i * 3 + 1]; f[i * 8 + 5] = m.nor[i * 3 + 2]
                f[i * 8 + 6] = m.uv[i * 2]; f[i * 8 + 7] = m.uv[i * 2 + 1]
            }
            GLES20.glGenBuffers(2, ids, 0); m.vbo = ids[0]; m.ibo = ids[1]
            val fb = ByteBuffer.allocateDirect(f.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(f); fb.position(0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.vbo); GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, f.size * 4, fb, GLES20.GL_STATIC_DRAW)
            val ib = ByteBuffer.allocateDirect(m.idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(m.idx); ib.position(0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, m.ibo); GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, m.idx.size * 2, ib, GLES20.GL_STATIC_DRAW)
            boundMesh = null
        }
        if (boundMesh !== m) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.vbo); GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, m.ibo)
            GLES20.glEnableVertexAttribArray(aPos); GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 32, 0)
            GLES20.glEnableVertexAttribArray(aNor); GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 32, 12)
            GLES20.glEnableVertexAttribArray(aUV); GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, 32, 24)
            boundMesh = m
        }
    }

    private var curCull = -1
    private fun cull(c: Int) {
        if (c == curCull) return; curCull = c
        when (c) {
            CULL_BACK -> { GLES20.glEnable(GLES20.GL_CULL_FACE); GLES20.glCullFace(GLES20.GL_BACK) }
            CULL_FRONT -> { GLES20.glEnable(GLES20.GL_CULL_FACE); GLES20.glCullFace(GLES20.GL_FRONT) }
            else -> GLES20.glDisable(GLES20.GL_CULL_FACE)
        }
    }

    private fun drawNode(n: Node, outline: Boolean) {
        val m = n.mesh ?: return
        bind(m)
        var w = n.world
        if (outline) { System.arraycopy(n.world, 0, tmpM, 0, 16); val k = n.outline; for (i in 0 until 12) tmpM[i] *= k; w = tmpM }
        GLES20.glUniformMatrix4fv(u["uModel"]!!, 1, false, w, 0)
        M4.normalMatrix(w, nm); GLES20.glUniformMatrix3fv(u["uNormM"]!!, 1, false, nm, 0)
        cull(if (outline) CULL_FRONT else n.cull)
        rgb("uColor", n.color)
        GLES20.glUniform1f(u["uAlpha"]!!, n.alpha); GLES20.glUniform1f(u["uEmis"]!!, n.emissive)
        GLES20.glUniform1f(u["uMat"]!!, n.mat.toFloat()); GLES20.glUniform1f(u["uOutline"]!!, if (outline) 1f else 0f)
        GLES20.glUniform1f(u["uNoFog"]!!, if (n.noFog) 1f else 0f)
        GLES20.glUniform1f(u["uPat"]!!, n.pattern.toFloat())
        if (n.pattern != PAT_NONE) {
            rgb("uColor2", n.color2); rgb("uColor3", n.color3); GLES20.glUniform4f(u["uPatP"]!!, n.patU, n.patV, n.patSeed, 0f)
        }
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, m.idx.size, GLES20.GL_UNSIGNED_SHORT, 0)
    }
}
