package com.comedyarena.politicalfight

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

/** Three detailed, fully procedural Indian cartoon arenas for Neta Fight */
object Arenas {
    val names = listOf("Sansad Arena (Parliament)", "Jantar Arena (Protest Rally)", "Desi Street (Bazaar)")
    private val sky = arrayOf(
        intArrayOf(0xFF1A237E.toInt(), 0xFFFFF8E1.toInt()), intArrayOf(0xFFBF360C.toInt(), 0xFFFFCC80.toInt()),
        intArrayOf(0xFF263238.toInt(), 0xFFFFAB91.toInt()), intArrayOf(0xFF7986CB.toInt(), 0xFFFFCC80.toInt()),
        intArrayOf(0xFF81D4FA.toInt(), 0xFFFFF59D.toInt()), intArrayOf(0xFFCE93D8.toInt(), 0xFFFFE0B2.toInt()),
        intArrayOf(0xFF311B92.toInt(), 0xFFEC407A.toInt())
    )
    private val ground = intArrayOf(
        0xFF8B0000.toInt(), 0xFF616161.toInt(), 0xFF37474F.toInt(), 0xFF8D6E63.toInt(),
        0xFFC0A040.toInt(), 0xFFD7B98E.toInt(), 0xFF6A1B9A.toInt()
    )
    private val pal = intArrayOf(0xFFE53935.toInt(), 0xFFFFB300.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())
    private val shaders = HashMap<Int, LinearGradient>()

    private fun rect(c: Canvas, p: Paint, l: Float, t: Float, r: Float, b: Float, col: Int) {
        p.style = Paint.Style.FILL; p.color = col; c.drawRect(l, t, r, b, p)
    }
    private fun circle(c: Canvas, p: Paint, x: Float, y: Float, r: Float, col: Int) {
        p.style = Paint.Style.FILL; p.color = col; c.drawCircle(x, y, r, p)
    }
    private fun bunting(c: Canvas, p: Paint, y: Float, tm: Float, w: Float) {
        val count = (w / 90f).toInt() + 1
        for (i in 0 until count) {
            val x = i * 90f
            val sag = sin(i * 0.4f) * 8f
            val tri = Path(); tri.moveTo(x, y + sag); tri.lineTo(x + 75f, y + sag); tri.lineTo(x + 37f, y + 55f + sin(tm * 0.1f + i) * 4f + sag); tri.close()
            p.style = Paint.Style.FILL; p.color = pal[i % 5]; c.drawPath(tri, p)
        }
        rect(c, p, 0f, y - 4f, w, y + 4f, 0xFF4E342E.toInt())
    }
    private fun crowd(c: Canvas, p: Paint, tm: Float, baseY: Float, w: Float) {
        val count = (w / 48f).toInt() + 1
        for (i in 0 until count) {
            val x = i * 48f + (i % 3) * 6f
            val y = baseY + sin(tm * 0.12f + i * 1.7f) * 7f
            rect(c, p, x - 18f, y + 18f, x + 18f, y + 70f, pal[(i + 2) % 5])
            circle(c, p, x, y, 20f, 0xFFD7A074.toInt() - (i % 4) * 0x101010)
            p.style = Paint.Style.FILL
        }
    }

    fun draw(c: Canvas, p: Paint, id: Int, tm: Float, w: Float = 1600f) {
        val g = shaders.getOrPut(id) { LinearGradient(0f, 0f, 0f, K.GROUND, sky[id][0], sky[id][1], Shader.TileMode.CLAMP) }
        p.style = Paint.Style.FILL; p.shader = g; c.drawRect(0f, 0f, w, K.GROUND, p); p.shader = null
        val gy = K.GROUND
        val midX = w / 2f

        when (id) {
            0 -> { // Sansad Arena (Parliament Hall)
                // Grand Central Dome & Circular Tier
                p.color = 0xFFFFF8E1.toInt(); c.drawOval(RectF(midX - 300f, 60f, midX + 300f, 500f), p)
                p.color = 0xFFFF9800.toInt(); c.drawArc(RectF(midX - 300f, 60f, midX + 300f, 500f), 180f, 60f, true, p)
                p.color = 0xFF4CAF50.toInt(); c.drawArc(RectF(midX - 300f, 60f, midX + 300f, 500f), 300f, 60f, true, p)
                rect(c, p, 100f, 280f, w - 100f, gy, 0xFFFFF3E0.toInt())
                // Classical Ivory Columns
                val numCols = (w / 120f).toInt()
                for (i in 0 until numCols) {
                    val cx = 60f + i * 120f
                    rect(c, p, cx, 280f, cx + 45f, gy - 20f, 0xFFECEFF1.toInt())
                    rect(c, p, cx - 8f, 260f, cx + 53f, 285f, 0xFFCFD8DC.toInt())
                }
                // Central Wooden Speaker's Podium & Chandelier
                rect(c, p, midX - 80f, gy - 120f, midX + 80f, gy, 0xFF3E2723.toInt())
                circle(c, p, midX, 220f, 45f, 0xFFFFD700.toInt())
                // Tiered Audience Gallery
                crowd(c, p, tm, gy - 80f, w)
                // Floor Red Carpet
                rect(c, p, 0f, gy, w, K.H, 0xFF8B0000.toInt())
                rect(c, p, 150f, gy + 10f, w - 150f, gy + 30f, 0xFFFFD700.toInt())
            }
            1 -> { // Jantar Arena (Protest Rally Ground)
                circle(c, p, w - 240f, 150f, 80f, 0xFFFF7043.toInt()) // Sunset Sun
                // Loudspeaker Trusses
                rect(c, p, 80f, 150f, 110f, gy, 0xFF424242.toInt())
                rect(c, p, w - 110f, 150f, w - 80f, gy, 0xFF424242.toInt())
                // Stage & Banners
                rect(c, p, midX - 450f, 320f, midX + 450f, 400f, 0xFFD32F2F.toInt())
                p.color = Color.WHITE; p.textSize = 34f; p.textAlign = Paint.Align.CENTER
                c.drawText("★ MAHA PARODY RALLY · JANTAR MANTAR ★", midX, 375f, p)
                // Yellow-Black Police Barricades
                val numBarricades = (w / 230f).toInt()
                for (i in 0 until numBarricades) {
                    val bx = 50f + i * 230f
                    rect(c, p, bx, gy - 70f, bx + 190f, gy, 0xFFFBC02D.toInt())
                    rect(c, p, bx, gy - 40f, bx + 190f, gy - 25f, 0xFF212121.toInt())
                }
                crowd(c, p, tm, gy - 110f, w)
                bunting(c, p, 60f, tm, w)
                rect(c, p, 0f, gy, w, K.H, 0xFF616161.toInt())
            }
            2 -> { // Desi Street (Indian Market & Chai Tapri)
                // Colorful Shopfronts
                val shopColors = intArrayOf(0xFFD84315.toInt(), 0xFFF57F17.toInt(), 0xFF1976D2.toInt(), 0xFF2E7D32.toInt(), 0xFF6A1B9A.toInt())
                val shopNames = listOf("CHAI & SAMOSA", "GUPTA SWEETS", "SUPER TAILOR", "PAAN BHANDAR", "DESI DHABA")
                val numShops = (w / 310f).toInt() + 1
                for (i in 0 until numShops) {
                    val sx = 20f + i * 310f
                    rect(c, p, sx, gy - 360f, sx + 290f, gy, shopColors[i % shopColors.size])
                    // Awning
                    rect(c, p, sx - 10f, gy - 375f, sx + 300f, gy - 330f, 0xFFFFFFFF.toInt())
                    p.color = Color.WHITE; p.textSize = 24f; p.textAlign = Paint.Align.CENTER
                    c.drawText(shopNames[i % shopNames.size], sx + 145f, gy - 290f, p)
                }
                // Hanging Festive Lights
                bunting(c, p, 100f, tm, w)
                // Auto Rickshaw on right
                val rickX = w - 240f
                rect(c, p, rickX, gy - 160f, rickX + 200f, gy - 40f, 0xFF2E7D32.toInt())
                rect(c, p, rickX, gy - 230f, rickX + 200f, gy - 160f, 0xFFFBC02D.toInt()) // yellow top
                circle(c, p, rickX + 40f, gy - 30f, 30f, 0xFF111111.toInt())
                circle(c, p, rickX + 160f, gy - 30f, 30f, 0xFF111111.toInt())
                // Road with white dashed divider
                rect(c, p, 0f, gy, w, K.H, 0xFF37474F.toInt())
                val numDashes = (w / 200f).toInt()
                for (i in 0 until numDashes) {
                    rect(c, p, 50f + i * 200f, gy + 90f, 150f + i * 200f, gy + 105f, Color.WHITE)
                }
            }
            else -> {
                crowd(c, p, tm, gy - 90f, w)
                bunting(c, p, 80f, tm, w)
                rect(c, p, 0f, gy, w, K.H, ground[id % ground.size])
            }
        }
        rect(c, p, 0f, gy - 40f, w, K.H, ground[id % ground.size])
        rect(c, p, 0f, gy - 40f, w, gy - 34f, 0x33000000)
    }
}
