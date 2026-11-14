package com.comedyarena.politicalfight

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

/** Seven colourful, fully procedural cartoon arenas (fictional, no real-place or party branding). */
object Arenas {
    val names = listOf("Sansad Arena (Parliament)", "Jantar Arena (Protest Rally)", "Desi Street (Bazaar)")
    private val sky = arrayOf(
        intArrayOf(0xFF1A237E.toInt(), 0xFFFFF8E1.toInt()), intArrayOf(0xFFBF360C.toInt(), 0xFFFFCC80.toInt()),
        intArrayOf(0xFF263238.toInt(), 0xFFFFAB91.toInt()), intArrayOf(0xFF7986CB.toInt(), 0xFFFFCC80.toInt()),
        intArrayOf(0xFF81D4FA.toInt(), 0xFFFFF59D.toInt()), intArrayOf(0xFFCE93D8.toInt(), 0xFFFFE0B2.toInt()),
        intArrayOf(0xFF311B92.toInt(), 0xFFEC407A.toInt()))
    private val ground = intArrayOf(0xFF8B0000.toInt(), 0xFF616161.toInt(), 0xFF37474F.toInt(), 0xFF8D6E63.toInt(),
        0xFFC0A040.toInt(), 0xFFD7B98E.toInt(), 0xFF6A1B9A.toInt())
    private val pal = intArrayOf(0xFFE53935.toInt(), 0xFFFFB300.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())
    private val pastel = intArrayOf(0xFFFFCDD2.toInt(), 0xFFFFE0B2.toInt(), 0xFFC8E6C9.toInt(), 0xFFBBDEFB.toInt(), 0xFFE1BEE7.toInt())
    private val shaders = HashMap<Int, LinearGradient>()

    private fun rect(c: Canvas, p: Paint, l: Float, t: Float, r: Float, b: Float, col: Int) {
        p.style = Paint.Style.FILL; p.color = col; c.drawRect(l, t, r, b, p)
    }
    private fun circle(c: Canvas, p: Paint, x: Float, y: Float, r: Float, col: Int) {
        p.style = Paint.Style.FILL; p.color = col; c.drawCircle(x, y, r, p)
    }
    private fun bunting(c: Canvas, p: Paint, y: Float, tm: Float) {
        for (i in 0 until 16) {
            val x = i * 100f
            val sag = sin(i * 0.4f) * 8f
            val tri = Path(); tri.moveTo(x, y + sag); tri.lineTo(x + 80f, y + sag); tri.lineTo(x + 40f, y + 60f + sin(tm * 0.1f + i) * 4f + sag); tri.close()
            p.style = Paint.Style.FILL; p.color = pal[i % 5]; c.drawPath(tri, p)
        }
        rect(c, p, 0f, y - 4f, K.W, y + 4f, 0xFF4E342E.toInt())
    }
    private fun crowd(c: Canvas, p: Paint, tm: Float, baseY: Float) {
        for (i in 0 until 32) {
            val x = i * 52f + (i % 3) * 7f
            val y = baseY + sin(tm * 0.12f + i * 1.7f) * 7f
            rect(c, p, x - 18f, y + 18f, x + 18f, y + 70f, pal[(i + 2) % 5])
            circle(c, p, x, y, 20f, 0xFFD7A074.toInt() - (i % 4) * 0x101010)
            p.style = Paint.Style.FILL
        }
    }

    fun draw(c: Canvas, p: Paint, id: Int, tm: Float) {
        val g = shaders.getOrPut(id) { LinearGradient(0f, 0f, 0f, K.GROUND, sky[id][0], sky[id][1], Shader.TileMode.CLAMP) }
        p.style = Paint.Style.FILL; p.shader = g; c.drawRect(0f, 0f, K.W, K.GROUND, p); p.shader = null
        val gy = K.GROUND
        when (id) {
            0 -> { // Sansad Arena (Parliament Hall)
                // Grand Central Dome & Circular Tier
                p.color = 0xFFFFF8E1.toInt(); c.drawOval(RectF(500f, 60f, 1100f, 500f), p)
                p.color = 0xFFFF9800.toInt(); c.drawArc(RectF(500f, 60f, 1100f, 500f), 180f, 60f, true, p)
                p.color = 0xFF4CAF50.toInt(); c.drawArc(RectF(500f, 60f, 1100f, 500f), 300f, 60f, true, p)
                rect(c, p, 200f, 280f, 1400f, gy, 0xFFFFF3E0.toInt())
                // Classical Ivory Columns
                for (i in 0 until 11) {
                    val cx = 240f + i * 112f
                    rect(c, p, cx, 280f, cx + 45f, gy - 20f, 0xFFECEFF1.toInt())
                    rect(c, p, cx - 8f, 260f, cx + 53f, 285f, 0xFFCFD8DC.toInt()) // capital
                }
                // Central Wooden Speaker's Podium & Chandelier
                rect(c, p, 730f, gy - 120f, 870f, gy, 0xFF3E2723.toInt())
                circle(c, p, 800f, 220f, 45f, 0xFFFFD700.toInt())
                // Tiered Audience Gallery
                crowd(c, p, tm, gy - 80f)
                // Floor Red Carpet
                rect(c, p, 0f, gy, K.W, K.H, 0xFF8B0000.toInt())
                rect(c, p, 300f, gy + 10f, 1300f, gy + 30f, 0xFFFFD700.toInt())
            }
            1 -> { // Jantar Arena (Protest Rally Ground)
                circle(c, p, 1300f, 150f, 80f, 0xFFFF7043.toInt()) // Sunset Sun
                // Loudspeaker Trusses
                rect(c, p, 100f, 150f, 130f, gy, 0xFF424242.toInt())
                rect(c, p, 1470f, 150f, 1500f, gy, 0xFF424242.toInt())
                // Stage & Banners
                rect(c, p, 250f, 320f, 1350f, 400f, 0xFFD32F2F.toInt())
                p.color = Color.WHITE; p.textSize = 34f; p.textAlign = Paint.Align.CENTER
                c.drawText("★ MAHA PARODY RALLY · JANTAR MANITAR ★", 800f, 375f, p)
                // Yellow-Black Police Barricades
                for (i in 0 until 6) {
                    val bx = 200f + i * 210f
                    rect(c, p, bx, gy - 70f, bx + 180f, gy, 0xFFFBC02D.toInt())
                    rect(c, p, bx, gy - 40f, bx + 180f, gy - 25f, 0xFF212121.toInt())
                }
                crowd(c, p, tm, gy - 110f)
                bunting(c, p, 60f, tm)
                rect(c, p, 0f, gy, K.W, K.H, 0xFF616161.toInt())
            }
            2 -> { // Desi Street (Indian Market & Chai Tapri)
                // Colorful Shopfronts
                val shopColors = intArrayOf(0xFFD84315.toInt(), 0xFFF57F17.toInt(), 0xFF1976D2.toInt(), 0xFF2E7D32.toInt(), 0xFF6A1B9A.toInt())
                val shopNames = listOf("CHAI & SAMOSA", "GUPTA SWEETS", "SUPER TAILOR", "PAAN BHANDAR", "DESI DHABA")
                for (i in 0 until 5) {
                    val sx = 80f + i * 300f
                    rect(c, p, sx, gy - 360f, sx + 280f, gy, shopColors[i])
                    // Awning
                    rect(c, p, sx - 10f, gy - 375f, sx + 290f, gy - 330f, 0xFFFFFFFF.toInt())
                    p.color = Color.WHITE; p.textSize = 24f; p.textAlign = Paint.Align.CENTER
                    c.drawText(shopNames[i], sx + 140f, gy - 290f, p)
                }
                // Hanging Festive Lights
                bunting(c, p, 100f, tm)
                // Auto Rickshaw on right
                rect(c, p, 1320f, gy - 160f, 1540f, gy - 40f, 0xFF2E7D32.toInt())
                rect(c, p, 1320f, gy - 230f, 1540f, gy - 160f, 0xFFFBC02D.toInt()) // yellow top
                circle(c, p, 1360f, gy - 30f, 30f, 0xFF111111.toInt())
                circle(c, p, 1500f, gy - 30f, 30f, 0xFF111111.toInt())
                // Road with white dashed divider
                rect(c, p, 0f, gy, K.W, K.H, 0xFF37474F.toInt())
                for (i in 0 until 8) {
                    rect(c, p, 50f + i * 200f, gy + 90f, 150f + i * 200f, gy + 105f, Color.WHITE)
                }
            }
            3 -> { // Riverside city
                circle(c, p, 300f, 200f, 90f, 0xFFFFE082.toInt())
                for (i in 0 until 10) rect(c, p, i * 170f, gy - 300f - (i % 3) * 60f, i * 170f + 140f, gy - 160f, 0xFF5C6BC0.toInt())
                rect(c, p, 0f, gy - 160f, K.W, gy - 60f, 0xFF29B6F6.toInt())
                for (i in 0 until 30) rect(c, p, i * 56f + sin(tm * 0.08f + i) * 10f, gy - 140f + (i % 3) * 24f, i * 56f + 40f + sin(tm * 0.08f + i) * 10f, gy - 132f + (i % 3) * 24f, 0x88FFFFFF.toInt())
                p.style = Paint.Style.STROKE; p.strokeWidth = 10f; p.color = 0xFF37474F.toInt()
                for (i in 0 until 4) c.drawArc(RectF(100f + i * 380f, gy - 260f, 480f + i * 380f, gy - 60f), 180f, 180f, false, p)
            }
            4 -> { // Mustard fields
                circle(c, p, 1250f, 170f, 110f, 0xFFFFB300.toInt())
                rect(c, p, 0f, gy - 200f, K.W, gy, 0xFFFDD835.toInt())
                for (i in 0 until 40) rect(c, p, i * 42f, gy - 230f + (i % 4) * 14f + sin(tm * 0.1f + i) * 4f, i * 42f + 18f, gy - 140f, 0xFFFFEB3B.toInt())
                for (i in 0 until 3) {
                    val x = 200f + i * 500f
                    rect(c, p, x, gy - 190f, x + 150f, gy - 80f, 0xFFA1887F.toInt())
                    val roof = Path(); roof.moveTo(x - 20f, gy - 190f); roof.lineTo(x + 75f, gy - 270f); roof.lineTo(x + 170f, gy - 190f); roof.close()
                    p.style = Paint.Style.FILL; p.color = 0xFF6D4C41.toInt(); c.drawPath(roof, p)
                }
            }
            5 -> { // Old town
                for (i in 0 until 5) {
                    val x = 100f + i * 330f
                    rect(c, p, x, 300f + (i % 2) * 60f, x + 120f, gy, 0xFFFFCCBC.toInt())
                    circle(c, p, x + 60f, 300f + (i % 2) * 60f, 60f, 0xFFFFAB91.toInt())
                    rect(c, p, x + 56f, 200f + (i % 2) * 60f, x + 64f, 250f + (i % 2) * 60f, 0xFF5D4037.toInt())
                    rect(c, p, x + 40f, 420f, x + 80f, gy, 0xFF8D6E63.toInt())
                }
                bunting(c, p, 70f, tm)
            }
            6 -> { // Campaign stage
                rect(c, p, 0f, 0f, 220f, gy, 0xFFC62828.toInt()); rect(c, p, K.W - 220f, 0f, K.W, gy, 0xFFC62828.toInt())
                for (i in 0 until 5) {
                    val b = Path(); val x = 260f + i * 270f
                    b.moveTo(x, 0f); b.lineTo(x - 90f, gy); b.lineTo(x + 90f, gy); b.close()
                    p.style = Paint.Style.FILL; p.color = 0x22FFFFFF; c.drawPath(b, p)
                }
                p.color = 0xFFFFFFFF.toInt(); p.textSize = 64f; p.textAlign = Paint.Align.CENTER; p.isFakeBoldText = true
                c.drawText("COMEDY CHAMPIONSHIP", 800f, 380f, p); p.isFakeBoldText = false
                for (i in 0 until 40) circle(c, p, (i * 97f + tm * 1.5f) % K.W, (i * 53f + tm * 2f) % gy, 6f, pal[i % 5])
            }
        }
        rect(c, p, 0f, gy - 40f, K.W, K.H, ground[id])
        rect(c, p, 0f, gy - 40f, K.W, gy - 34f, 0x33000000)
        for (i in 0 until 20) rect(c, p, i * 90f, gy + 40f, i * 90f + 50f, gy + 46f, 0x22000000)
    }
}
