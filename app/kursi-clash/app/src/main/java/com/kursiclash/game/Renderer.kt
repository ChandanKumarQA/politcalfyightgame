package com.kursiclash.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Draws the complete Indian Political Satire Fighting Game with Canvas and Caricature Drawables. */
class Renderer(private val ctx: Context, private val g: Game) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSkewX = -0.15f
    }
    private val path = Path()
    private val rect = RectF()

    private val white = Color.WHITE
    private val gold = 0xFFFFC83D.toInt()
    private val orange = 0xFFFF5A36.toInt()

    private val bmpCache = HashMap<String, Bitmap?>()
    private fun getBmp(resName: String): Bitmap? {
        if (bmpCache.containsKey(resName)) return bmpCache[resName]
        val bmp = try {
            val id = ctx.resources.getIdentifier(resName, "drawable", ctx.packageName)
            if (id != 0) BitmapFactory.decodeResource(ctx.resources, id) else null
        } catch (_: Exception) { null }
        bmpCache[resName] = bmp
        return bmp
    }

    fun draw(c: Canvas) {
        val vw = c.width
        val vh = c.height
        c.drawColor(Color.BLACK)
        val s = min(vw / W, vh / H)
        val ox = (vw - W * s) / 2f
        val oy = (vh - H * s) / 2f
        c.save()
        c.translate(ox, oy)
        c.scale(s, s)
        c.clipRect(0f, 0f, W, H)
        if (g.shake > 0f) {
            c.translate((g.rnd.nextFloat() - 0.5f) * g.shake, (g.rnd.nextFloat() - 0.5f) * g.shake)
        }
        when (g.scr) {
            Scr.TITLE -> drawTitle(c)
            Scr.SELECT -> drawSelect(c)
            Scr.PLAY_WITH_FRIEND -> drawPlayWithFriend(c)
            Scr.HOST_ROOM -> drawHostRoom(c)
            else -> {
                drawFightScene(c)
                if (g.paused) drawPauseMenu(c)
            }
        }
        c.restore()
    }

    // ------------------------------------------------------------------ helpers

    private fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        align: Paint.Align = Paint.Align.CENTER, outline: Boolean = true
    ) {
        tp.textSize = size
        tp.textAlign = align
        if (outline) {
            tp.style = Paint.Style.STROKE
            tp.strokeWidth = size * 0.16f
            tp.strokeJoin = Paint.Join.ROUND
            tp.color = Color.BLACK
            c.drawText(s, x, y, tp)
        }
        tp.style = Paint.Style.FILL
        tp.color = color
        c.drawText(s, x, y, tp)
    }

    private fun limb(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, w: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = w
        p.color = color
        c.drawLine(x1, y1, x2, y2, p)
        p.style = Paint.Style.FILL
    }

    private fun grad(c: Canvas, top: Int, bot: Int, y0: Float, y1: Float) {
        p.shader = LinearGradient(0f, y0, 0f, y1, top, bot, Shader.TileMode.CLAMP)
        c.drawRect(0f, y0, W, y1, p)
        p.shader = null
    }

    private fun button(c: Canvas, r: RectF, label: String, fill: Int, size: Float = 34f, strokeCol: Int = gold) {
        p.color = fill
        c.drawRoundRect(r, 18f, 18f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        p.color = strokeCol
        c.drawRoundRect(r, 18f, 18f, p)
        p.style = Paint.Style.FILL
        text(c, label, r.centerX(), r.centerY() + size * 0.35f, size, white)
    }

    // ------------------------------------------------------------------ stages

    private fun drawStage(c: Canvas) {
        when (g.stage) {
            0 -> stageHall(c)
            1 -> stageRally(c)
            else -> stageRoad(c)
        }
    }

    private fun stageHall(c: Canvas) {
        grad(c, 0xFF1C0A08.toInt(), 0xFF5B2A16.toInt(), 0f, GROUND)
        p.color = 0xFF7C4A2A.toInt()
        c.drawArc(RectF(390f, 40f, 890f, 540f), 180f, 180f, true, p)
        p.color = 0xFF9A6338.toInt()
        c.drawArc(RectF(440f, 90f, 840f, 490f), 180f, 180f, true, p)
        p.color = 0xFFF2C14E.toInt()
        c.drawCircle(640f, 300f, 46f, p)
        p.color = 0xFF7C4A2A.toInt()
        c.drawCircle(640f, 300f, 34f, p)
        for (i in 0 until 7) {
            val x = 70f + i * 190f
            p.color = 0xFF8C5A34.toInt()
            c.drawRect(x - 28f, 150f, x + 28f, GROUND, p)
            p.color = 0xFFB07A4A.toInt()
            c.drawRect(x - 28f, 150f, x - 18f, GROUND, p)
            p.color = 0xFF6A3E22.toInt()
            c.drawRect(x - 36f, 140f, x + 36f, 160f, p)
        }
        for (r in 0 until 3) {
            val y = 420f + r * 55f
            p.color = 0xFF3A1D10.toInt()
            c.drawRect(0f, y, W, y + 30f, p)
            for (k in 0 until 14) {
                val bx = 40f + k * 92f + (r % 2) * 46f
                p.color = 0xFFB9855A.toInt()
                c.drawCircle(bx, y - 6f, 15f, p)
            }
        }
        p.color = 0xFF2B170C.toInt()
        c.drawRect(0f, GROUND, W, H, p)
        p.color = 0xFF4A2A18.toInt()
        c.drawRect(0f, GROUND, W, GROUND + 6f, p)
    }

    private fun stageRally(c: Canvas) {
        grad(c, 0xFF2B1055.toInt(), 0xFFFF8A3D.toInt(), 0f, GROUND)
        p.color = 0xFFFFD27A.toInt()
        c.drawCircle(640f, 430f, 110f, p)
        p.color = 0xFFD62839.toInt()
        c.drawRect(380f, 150f, 900f, 230f, p)
        text(c, "CHAI · SAMOSA · ZINDABAD", 640f, 205f, 38f, white)
        for (i in 0 until 9) {
            val x = 60f + i * 150f
            p.color = 0xFF3A2A20.toInt()
            c.drawRect(x - 3f, 260f, x + 3f, GROUND - 60f, p)
            val w = sin((g.frame + i * 9) * 0.12f) * 8f
            path.reset()
            path.moveTo(x + 3f, 265f)
            path.lineTo(x + 60f, 280f + w)
            path.lineTo(x + 3f, 305f)
            path.close()
            p.color = if (i % 2 == 0) 0xFFFFC83D.toInt() else 0xFF22C55E.toInt()
            c.drawPath(path, p)
        }
        for (r in 0 until 3) {
            val y = 430f + r * 50f
            for (k in 0 until 16) {
                val bx = 30f + k * 80f + (r % 2) * 40f
                p.color = 0xFFC68642.toInt()
                c.drawCircle(bx, y, 16f, p)
            }
        }
        p.color = 0xFF1B1B1B.toInt()
        c.drawRect(0f, GROUND, W, H, p)
    }

    private fun stageRoad(c: Canvas) {
        grad(c, 0xFF0D1B2A.toInt(), 0xFF415A77.toInt(), 0f, GROUND)
        for (i in 0 until 8) {
            val x = 50f + i * 160f
            val h = 160f + ((i * 37) % 110)
            p.color = 0xFF1B263B.toInt()
            c.drawRect(x, GROUND - h, x + 120f, GROUND, p)
            p.color = 0xFFFFD166.toInt()
            for (wy in (GROUND - h + 20f).toInt()..GROUND.toInt() - 30 step 45) {
                for (wx in (x + 15f).toInt()..(x + 90f).toInt() step 30) {
                    if ((wx + wy + g.frame / 60) % 3 != 0) {
                        c.drawRect(wx.toFloat(), wy.toFloat(), wx + 16f, wy + 24f, p)
                    }
                }
            }
        }
        p.color = 0xFF262626.toInt()
        c.drawRect(0f, GROUND, W, H, p)
        p.color = 0xFFFFFFFF.toInt()
        for (i in 0 until 14) {
            c.drawRect(20f + i * 95f, GROUND + 40f, 70f + i * 95f, GROUND + 48f, p)
        }
    }

    // ------------------------------------------------------------------ fighters

    private fun drawHead(c: Canvas, look: Look, hx: Float, hy: Float, r: Float, expr: Int) {
        p.color = look.skin
        c.drawCircle(hx, hy, r, p)
        if (look.beard) {
            p.color = look.hair
            c.drawArc(RectF(hx - r, hy - r * 0.2f, hx + r, hy + r * 1.1f), 0f, 180f, true, p)
            p.color = look.skin
            c.drawCircle(hx + r * 0.35f, hy + r * 0.15f, r * 0.55f, p)
        }
        when (look.hat) {
            Hat.TOPI -> {
                p.color = 0xFFF4EBD0.toInt()
                path.reset()
                path.moveTo(hx - r * 1.0f, hy - r * 0.4f)
                path.lineTo(hx - r * 0.7f, hy - r * 1.2f)
                path.lineTo(hx + r * 0.8f, hy - r * 1.2f)
                path.lineTo(hx + r * 1.0f, hy - r * 0.4f)
                path.close()
                c.drawPath(path, p)
            }
            Hat.TURBAN -> {
                p.color = look.hair
                rect.set(hx - r * 1.15f, hy - r * 1.5f, hx + r * 1.15f, hy - r * 0.25f)
                c.drawOval(rect, p)
            }
            Hat.HAIR, Hat.BUN -> {
                p.color = look.hair
                c.drawArc(RectF(hx - r, hy - r, hx + r, hy + r), 180f, 180f, true, p)
            }
            Hat.CAP -> {
                p.color = look.hair
                c.drawArc(RectF(hx - r, hy - r, hx + r, hy + r), 180f, 180f, true, p)
                c.drawRect(hx, hy - r * 0.45f, hx + r * 1.5f, hy - r * 0.25f, p)
            }
            Hat.BALD -> {}
        }
        val ex = hx + r * 0.25f
        val ey = hy - r * 0.1f
        val gap = r * 0.55f
        p.color = Color.WHITE
        c.drawCircle(ex, ey, r * 0.2f, p)
        c.drawCircle(ex + gap, ey, r * 0.2f, p)
        p.color = Color.BLACK
        c.drawCircle(ex + 2f, ey, r * 0.1f, p)
        c.drawCircle(ex + gap + 2f, ey, r * 0.1f, p)
        if (look.glasses) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = Color.BLACK
            c.drawCircle(ex, ey, r * 0.3f, p)
            c.drawCircle(ex + gap, ey, r * 0.3f, p)
            c.drawLine(ex + r * 0.3f, ey, ex + gap - r * 0.3f, ey, p)
            p.style = Paint.Style.FILL
        }
    }

    private fun arm(c: Canvas, sx: Float, sy: Float, hx: Float, hy: Float, look: Look) {
        val mx = (sx + hx) / 2f
        val my = (sy + hy) / 2f
        limb(c, sx, sy, mx, my, 17f, look.outfit)
        limb(c, mx, my, hx, hy, 13f, look.skin)
        p.color = look.skin
        c.drawCircle(hx, hy, 9f, p)
    }

    private fun drawFighter(c: Canvas, f: Fighter) {
        val look = f.def.look
        val sh = 1f - min(0.5f, (GROUND - f.y) / 400f)
        p.color = 0x55000000
        c.drawOval(RectF(f.x - 48f * sh, GROUND - 7f, f.x + 48f * sh, GROUND + 9f), p)

        c.save()
        c.translate(f.x, f.y)
        c.scale(f.facing.toFloat(), 1f)
        if (f.flash > 3) p.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        if (f.st == St.KO) {
            c.translate(0f, -14f)
            c.rotate(-82f)
        } else if (f.st == St.HIT) {
            c.rotate(-7f)
        }

        val crouch = f.crouching && f.onGround
        val hipY = if (crouch) -52f else -92f
        val shY = hipY - (if (crouch) 60f else 70f)
        val m = f.move
        val atk = f.st == St.ATTACK && m != null
        var prog = 0f
        if (atk && m != null) {
            prog = if (m.total > 0) f.moveT.toFloat() / m.total else 0f
        }

        // Legs
        val legExt = if (atk && (m == Moves.KICK || m == Moves.KICK2 || m == Moves.SWEEP)) {
            sin(prog * 3.14159f) * 75f
        } else 0f
        limb(c, -12f, hipY, -18f, 0f, 18f, look.pants)
        limb(c, 12f, hipY, 18f + legExt, if (crouch) -10f else 0f, 18f, look.pants)

        // Torso
        val lean = if (crouch) 12f else if (f.st == St.WALK) 6f else 0f
        limb(c, 0f, hipY, lean, shY, 44f, look.outfit)

        // Arms
        val armExt = if (atk && (m == Moves.P1 || m == Moves.P2 || m == Moves.P3 || m == Moves.UPPER)) {
            sin(prog * 3.14159f) * 65f
        } else 0f
        arm(c, lean - 10f, shY + 10f, lean - 20f, shY + 40f, look)
        arm(c, lean + 10f, shY + 10f, lean + 30f + armExt, if (m == Moves.UPPER) shY - 40f else shY + 20f, look)

        // Cartoon Caricature Head
        drawHead(c, look, lean + 6f, shY - 30f, 27f, if (f.st == St.HIT) 1 else 0)

        if (atk && m == Moves.SUPER && f.moveT > 8) {
            p.color = 0x88FF9F1C.toInt()
            c.drawCircle(lean + 120f, shY + 4f, 14f + 26f * prog, p)
            p.color = 0xCCFFF3B0.toInt()
            c.drawCircle(lean + 120f, shY + 4f, 8f + 12f * prog, p)
        }
        p.colorFilter = null
        c.restore()
    }

    private fun drawProj(c: Canvas, pr: Proj) {
        val col = pr.owner.def.look.outfit
        p.color = (col and 0xFFFFFF) or 0x55000000
        c.drawCircle(pr.x, pr.y, 50f, p)
        p.color = (col and 0xFFFFFF) or 0xAA000000.toInt()
        c.drawCircle(pr.x, pr.y, 34f, p)
        p.color = 0xFFFFF3B0.toInt()
        c.drawCircle(pr.x, pr.y, 18f, p)
        for (i in 1..4) {
            p.color = (col and 0xFFFFFF) or 0x44000000
            c.drawCircle(pr.x - pr.vx * i * 1.6f, pr.y, 30f - i * 5f, p)
        }
    }

    // ------------------------------------------------------------------ fight scene + HUD

    private fun drawFightScene(c: Canvas) {
        drawStage(c)
        drawFighter(c, g.p1)
        drawFighter(c, g.p2)
        for (pr in g.projs) drawProj(c, pr)
        for (pop in g.pops) text(c, pop.text, pop.x, pop.y, 34f, pop.color)
        drawHud(c)
        if (g.scr != Scr.RESULT) drawPad(c)

        when (g.scr) {
            Scr.INTRO -> {
                val t = g.introT
                val label = if (t > 55) "ROUND ${g.round}" else "FIGHT!"
                val pulse = 1f + 0.08f * sin(g.frame * 0.4f)
                text(c, label, 640f, 330f, 120f * pulse, if (t > 55) white else gold)
            }
            Scr.ROUNDEND -> text(c, g.banner, 640f, 330f, 130f, orange)
            Scr.RESULT -> drawResult(c)
            else -> {}
        }
    }

    private fun bar(c: Canvas, x: Float, y: Float, w: Float, h: Float, ratio: Float, anchorRight: Boolean, color: Int) {
        p.color = 0xEE1C110C.toInt()
        c.drawRoundRect(RectF(x - 4f, y - 4f, x + w + 4f, y + h + 4f), 10f, 10f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF5D4037.toInt()
        c.drawRoundRect(RectF(x - 4f, y - 4f, x + w + 4f, y + h + 4f), 10f, 10f, p)
        p.style = Paint.Style.FILL

        val rw = w * ratio.coerceIn(0f, 1f)
        p.color = color
        if (anchorRight) c.drawRoundRect(RectF(x + w - rw, y, x + w, y + h), 8f, 8f, p)
        else c.drawRoundRect(RectF(x, y, x + rw, y + h), 8f, 8f, p)
    }

    private fun hpColor(r: Float) = when {
        r > 0.5f -> 0xFF43A047.toInt()
        r > 0.25f -> 0xFFFFC83D.toInt()
        else -> 0xFFFF3838.toInt()
    }

    private fun drawHud(c: Canvas) {
        val a = g.p1
        val b = g.p2
        val ra = a.hp / a.def.maxHp
        val rb = b.hp / b.def.maxHp

        // P1 Avatar Card (Left)
        val p1Bmp = getBmp(a.def.iconRes)
        val p1Rect = RectF(35f, 20f, 99f, 84f)
        p.color = 0xFF221510.toInt(); c.drawRoundRect(p1Rect, 10f, 10f, p)
        if (p1Bmp != null) c.drawBitmap(p1Bmp, null, p1Rect, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = gold; c.drawRoundRect(p1Rect, 10f, 10f, p); p.style = Paint.Style.FILL

        // P2 Avatar Card (Right)
        val p2Bmp = getBmp(b.def.iconRes)
        val p2Rect = RectF(1181f, 20f, 1245f, 84f)
        p.color = 0xFF15222D.toInt(); c.drawRoundRect(p2Rect, 10f, 10f, p)
        if (p2Bmp != null) c.drawBitmap(p2Bmp, null, p2Rect, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF00E5FF.toInt(); c.drawRoundRect(p2Rect, 10f, 10f, p); p.style = Paint.Style.FILL

        // Health Bars
        bar(c, 110f, 44f, 470f, 28f, ra, false, hpColor(ra))
        bar(c, 700f, 44f, 470f, 28f, rb, true, hpColor(rb))

        // Fighter Names
        val p1Name = if (g.isMultiplayer) "P1 · ${a.def.name}" else "${a.def.name} (YOU)"
        val p2Name = if (g.isMultiplayer) "P2 · ${b.def.name}" else "${b.def.name} (CPU)"
        text(c, p1Name, 110f, 34f, 22f, gold, Paint.Align.LEFT)
        text(c, p2Name, 1170f, 34f, 22f, 0xFF00E5FF.toInt(), Paint.Align.RIGHT)

        // Round Win Dots (Best of 3)
        for (i in 0 until 2) {
            p.color = if (i < a.wins) gold else 0x55000000
            c.drawCircle(116f + i * 22f, 85f, 7f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = gold
            c.drawCircle(116f + i * 22f, 85f, 7f, p); p.style = Paint.Style.FILL

            p.color = if (i < b.wins) gold else 0x55000000
            c.drawCircle(1164f - i * 22f, 85f, 7f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = gold
            c.drawCircle(1164f - i * 22f, 85f, 7f, p); p.style = Paint.Style.FILL
        }

        // Timer in Center
        p.color = 0xDD000000.toInt()
        c.drawRoundRect(RectF(600f, 18f, 680f, 68f), 12f, 12f, p)
        val secs = ((g.timer + 59) / 60).coerceAtLeast(0)
        text(c, secs.toString(), 640f, 56f, 40f, gold)

        // Pause Button [ || ]
        p.color = 0xCC2A1B14.toInt()
        c.drawRoundRect(g.btnPause, 8f, 8f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = gold
        c.drawRoundRect(g.btnPause, 8f, 8f, p); p.style = Paint.Style.FILL
        text(c, "II", g.btnPause.centerX(), g.btnPause.centerY() + 7f, 20f, white)

        // SHAKTI Meter Bars
        bar(c, 40f, 650f, 260f, 14f, a.meter / 100f, false, gold)
        text(c, "SHAKTI (SPECIAL READY)", 40f, 642f, 15f, gold, Paint.Align.LEFT)

        bar(c, 980f, 650f, 260f, 14f, b.meter / 100f, true, gold)
        text(c, "SHAKTI", 1240f, 642f, 15f, gold, Paint.Align.RIGHT)

        // Bottom Newsdesk Live Banner
        val deskR = RectF(340f, 638f, 940f, 672f)
        p.color = 0xDD1B0F1C.toInt(); c.drawRoundRect(deskR, 8f, 8f, p)
        p.color = 0xFFE53935.toInt(); c.drawCircle(360f, 655f, 5f, p)
        text(c, "LIVE", 380f, 660f, 14f, 0xFFFF5A36.toInt(), Paint.Align.LEFT)
        text(c, "BREAKING BAKWAS: SHOR SHARMA vs TROLL ARMY", 650f, 660f, 15f, white)

        // Scrolling News Ticker at bottom
        val tickerY = 705f
        p.color = 0xFFD84315.toInt(); c.drawRect(0f, 685f, W, 720f, p)
        val tickerMsg = "BREAKING: SANSAD ADJOURNED OVER CHAI PRICE ● MITRAMAN DEMANDS JPC PROBE INTO JAB ● YATRA BOY CLAIMS 4000 KM COMBO RECORD ● CHAI WALA RAISES SAMOSA TAX ●"
        val offset = (g.frame * 2.2f) % 1600f
        text(c, tickerMsg, W - offset, tickerY, 16f, white, Paint.Align.LEFT, outline = false)
        text(c, tickerMsg, W - offset + 1600f, tickerY, 16f, white, Paint.Align.LEFT, outline = false)

        if (a.combo >= 2) text(c, "${a.combo} HITS!", 200f, 200f, 46f, gold)
        if (b.combo >= 2) text(c, "${b.combo} HITS!", 1080f, 200f, 46f, gold)
    }

    private fun drawPad(c: Canvas) {
        for (bi in 0 until 8) {
            val x = Pad.x[bi]
            val y = Pad.y[bi]
            val r = Pad.r[bi]
            val held = g.heldTouch[bi]
            p.color = if (held) 0x99FFFFFF.toInt() else 0x44FFFFFF

            // SPECIAL ATTACK BUTTON (Index 7): Always highlighted and pulsing!
            if (bi == 7) {
                p.color = if ((g.frame / 8) % 2 == 0) 0xCCFFB300.toInt() else 0x88FFB300.toInt()
            }
            c.drawCircle(x, y, r, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = if (bi == 7) 5f else 3f
            p.color = if (bi == 7) gold else 0x99FFFFFF.toInt()
            c.drawCircle(x, y, r, p)
            p.style = Paint.Style.FILL

            if (bi < 4) {
                p.color = 0xDDFFFFFF.toInt()
                path.reset()
                when (bi) {
                    0 -> { path.moveTo(x - 20f, y); path.lineTo(x + 14f, y - 20f); path.lineTo(x + 14f, y + 20f) }
                    1 -> { path.moveTo(x + 20f, y); path.lineTo(x - 14f, y - 20f); path.lineTo(x - 14f, y + 20f) }
                    2 -> { path.moveTo(x, y - 20f); path.lineTo(x - 20f, y + 14f); path.lineTo(x + 20f, y + 14f) }
                    else -> { path.moveTo(x, y + 18f); path.lineTo(x - 18f, y - 12f); path.lineTo(x + 18f, y - 12f) }
                }
                path.close()
                c.drawPath(path, p)
            } else {
                text(c, Pad.label[bi], x, y + 8f, if (bi == 7) 20f else if (bi == 4) 24f else 20f, white)
            }
        }
    }

    private fun drawPauseMenu(c: Canvas) {
        p.color = 0xCC000000.toInt()
        c.drawRect(0f, 0f, W, H, p)
        val box = RectF(430f, 180f, 850f, 540f)
        p.color = 0xEE2A1B14.toInt(); c.drawRoundRect(box, 20f, 20f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = gold
        c.drawRoundRect(box, 20f, 20f, p); p.style = Paint.Style.FILL

        text(c, "GAME PAUSED", 640f, 235f, 44f, gold)
        button(c, g.btnResume, "RESUME", 0xFF43A047.toInt(), 28f)
        button(c, g.btnRestart, "RESTART", 0xFFE65100.toInt(), 28f)
        button(c, g.btnQuitSelect, "CHARACTER SELECT", 0xFF1565C0.toInt(), 24f)
    }

    private fun drawResult(c: Canvas) {
        p.color = 0xCC000000.toInt()
        c.drawRect(0f, 0f, W, H, p)
        text(c, if (g.resultWin) "YOU WIN!" else "YOU LOSE!", 640f, 220f, 120f,
            if (g.resultWin) gold else orange)
        text(c, "THE DAILY JUMLA", 640f, 310f, 34f, 0xFF9AD1FF.toInt())
        text(c, g.headline, 640f, 370f, 26f, white, outline = false)
        button(c, RectF(250f, 550f, 600f, 640f), "REMATCH", 0xFFE65100.toInt(), 34f)
        button(c, RectF(680f, 550f, 1030f, 640f), "CHARACTER SELECT", 0xFF1565C0.toInt(), 28f)
    }

    // ------------------------------------------------------------------ menus

    private fun drawTitle(c: Canvas) {
        stageHall(c)
        p.color = 0x88000000.toInt()
        c.drawRect(0f, 0f, W, H, p)
        val pulse = 1f + 0.04f * sin(g.frame * 0.1f)
        text(c, "KURSI CLASH", 640f, 200f, 130f * pulse, gold)
        text(c, "THE GREAT POWER BATTLE", 640f, 265f, 38f, white)

        val b1 = getBmp(Roster.all[0].iconRes)
        if (b1 != null) c.drawBitmap(b1, null, RectF(300f, 350f, 460f, 530f), p)
        val b2 = getBmp(Roster.all[1].iconRes)
        if (b2 != null) c.drawBitmap(b2, null, RectF(820f, 350f, 980f, 530f), p)

        text(c, "VS", 640f, 450f, 70f, orange)
        if ((g.frame / 30) % 2 == 0) text(c, "TAP TO START", 640f, 590f, 48f, white)
        text(c, "Indian Political Satire Game · All characters are original & fictional", 640f, 670f, 22f, 0xFFCCCCCC.toInt(), outline = false)
    }

    private fun drawSelect(c: Canvas) {
        grad(c, 0xFF1A0E28.toInt(), 0xFF351910.toInt(), 0f, H)
        text(c, "PICK YOUR NETA", 640f, 75f, 48f, gold)

        // 13 Character Cards in a single responsive row
        for (i in Roster.all.indices) {
            val d = Roster.all[i]
            val r = g.cardRect(i)
            val selP1 = (i == g.sel1)
            val selCPU = (i == g.sel2)

            p.color = if (selP1) 0xFF331E15.toInt() else if (selCPU) 0xFF152638.toInt() else 0xFF1F1824.toInt()
            c.drawRoundRect(r, 12f, 12f, p)

            // Card outline
            p.style = Paint.Style.STROKE
            p.strokeWidth = if (selP1 || selCPU) 4f else 2f
            p.color = if (selP1) 0xFFFF9800.toInt() else if (selCPU) 0xFF00E5FF.toInt() else 0xFF4A384D.toInt()
            c.drawRoundRect(r, 12f, 12f, p)
            p.style = Paint.Style.FILL

            // Top Badges ("1P", "CPU", "BOSS", "CH. 2", "?")
            when {
                selP1 -> {
                    val br = RectF(r.left + 4f, r.top - 8f, r.left + 36f, r.top + 14f)
                    p.color = 0xFFE65100.toInt(); c.drawRoundRect(br, 6f, 6f, p)
                    text(c, "1P", br.centerX(), br.centerY() + 5f, 13f, white)
                }
                selCPU -> {
                    val br = RectF(r.left + 4f, r.top - 8f, r.left + 44f, r.top + 14f)
                    p.color = 0xFF0288D1.toInt(); c.drawRoundRect(br, 6f, 6f, p)
                    text(c, "CPU", br.centerX(), br.centerY() + 5f, 12f, white)
                }
                d.badge == "BOSS" -> {
                    val br = RectF(r.left + 4f, r.top - 8f, r.left + 48f, r.top + 14f)
                    p.color = 0xFFFFB300.toInt(); c.drawRoundRect(br, 6f, 6f, p)
                    text(c, "BOSS", br.centerX(), br.centerY() + 5f, 11f, 0xFF212121.toInt())
                }
                d.badge == "CH. 2" -> {
                    val br = RectF(r.left + 4f, r.top - 8f, r.left + 50f, r.top + 14f)
                    p.color = 0xFF00ACC1.toInt(); c.drawRoundRect(br, 6f, 6f, p)
                    text(c, "CH. 2", br.centerX(), br.centerY() + 5f, 11f, white)
                }
                d.badge == "?" -> {
                    val br = RectF(r.left + 4f, r.top - 8f, r.left + 28f, r.top + 14f)
                    p.color = 0xFFE53935.toInt(); c.drawRoundRect(br, 6f, 6f, p)
                    text(c, "?", br.centerX(), br.centerY() + 5f, 13f, white)
                }
            }

            // Draw Caricature Photo Bitmap
            val bmp = getBmp(d.iconRes)
            if (bmp != null) {
                val picRect = RectF(r.left + 4f, r.top + 16f, r.right - 4f, r.bottom - 26f)
                c.drawBitmap(bmp, null, picRect, p)
            } else {
                drawHead(c, d.look, r.centerX(), r.top + 55f, 26f, 0)
            }

            // Name bar at bottom of card
            val nameBg = RectF(r.left + 2f, r.bottom - 24f, r.right - 2f, r.bottom - 2f)
            p.color = 0xEE120E16.toInt(); c.drawRoundRect(nameBg, 6f, 6f, p)
            text(c, d.name, r.centerX(), r.bottom - 8f, if (d.name.length > 8) 10f else 12f, if (selP1) gold else white)
        }

        // STAGE Selection Row: [ SANSAD (DIWALI EDITION) ] [ JANTAR MANTAR ] [ DESI SADAK ]
        text(c, "STAGE", 260f, 325f, 26f, gold, Paint.Align.RIGHT)
        button(c, g.btnStage0, "SANSAD", if (g.stage == 0) 0xFFE65100.toInt() else 0xFF2A1C22.toInt(), 22f,
            if (g.stage == 0) gold else 0xFF6D4C41.toInt())
        // Cute "DIWALI EDITION" badge floating over Sansad!
        val diwaliBadge = RectF(g.btnStage0.left + 20f, g.btnStage0.top - 14f, g.btnStage0.right - 20f, g.btnStage0.top + 10f)
        p.color = 0xFFD81B60.toInt(); c.drawRoundRect(diwaliBadge, 8f, 8f, p)
        text(c, "🪔 DIWALI EDITION", diwaliBadge.centerX(), diwaliBadge.centerY() + 4f, 11f, white, outline = false)

        button(c, g.btnStage1, "JANTAR MANTAR", if (g.stage == 1) 0xFFE65100.toInt() else 0xFF2A1C22.toInt(), 20f,
            if (g.stage == 1) gold else 0xFF6D4C41.toInt())
        button(c, g.btnStage2, "DESI SADAK", if (g.stage == 2) 0xFFE65100.toInt() else 0xFF2A1C22.toInt(), 22f,
            if (g.stage == 2) gold else 0xFF6D4C41.toInt())

        // CPU Difficulty Row: [ EASY ] [ NORMAL ] [ HARD ]
        text(c, "CPU", 410f, 408f, 26f, gold, Paint.Align.RIGHT)
        button(c, g.btnCpu0, "EASY", if (g.cpuLevel == 0) 0xFFE65100.toInt() else 0xFF1C222A.toInt(), 22f,
            if (g.cpuLevel == 0) gold else 0xFF455A64.toInt())
        button(c, g.btnCpu1, "NORMAL", if (g.cpuLevel == 1) 0xFFE65100.toInt() else 0xFF1C222A.toInt(), 22f,
            if (g.cpuLevel == 1) gold else 0xFF455A64.toInt())
        button(c, g.btnCpu2, "HARD", if (g.cpuLevel == 2) 0xFFE65100.toInt() else 0xFF1C222A.toInt(), 22f,
            if (g.cpuLevel == 2) gold else 0xFF455A64.toInt())

        // Bottom Action Buttons: [ 🌐 PLAY WITH A FRIEND ] and [ FIGHT! ]
        button(c, g.btnFriend, "🌐 PLAY WITH A FRIEND", 0xFF1976D2.toInt(), 30f, 0xFF64B5F6.toInt())
        button(c, g.btnFight, "FIGHT!", 0xFFE65100.toInt(), 44f, gold)
    }

    private fun drawPlayWithFriend(c: Canvas) {
        stageHall(c)
        p.color = 0xAA000000.toInt(); c.drawRect(0f, 0f, W, H, p)

        text(c, "PLAY WITH A FRIEND", 640f, 160f, 56f, gold)

        // Center Dark Card
        val card = RectF(380f, 210f, 900f, 520f)
        p.color = 0xEE1E1528.toInt(); c.drawRoundRect(card, 20f, 20f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF5D4037.toInt()
        c.drawRoundRect(card, 20f, 20f, p); p.style = Paint.Style.FILL

        // Button: [ HOST A ROOM ]
        button(c, g.btnHostRoom, "HOST A ROOM", 0xFFE65100.toInt(), 34f, gold)

        text(c, "or enter your friend's room code", 640f, 375f, 22f, 0xFFDDDDDD.toInt(), outline = false)

        // Row: [ CODE ] and [ JOIN ]
        p.color = 0xFF120E18.toInt(); c.drawRoundRect(g.btnCodeInput, 14f, 14f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = white
        c.drawRoundRect(g.btnCodeInput, 14f, 14f, p); p.style = Paint.Style.FILL
        val codeText = if (g.enteredCode.isEmpty()) "CODE" else g.enteredCode
        text(c, codeText, g.btnCodeInput.centerX(), g.btnCodeInput.centerY() + 10f, 28f, if (g.enteredCode.isEmpty()) 0xFF888888.toInt() else gold)

        button(c, g.btnJoinRoom, "JOIN", 0xFFE65100.toInt(), 30f, gold)

        // Back Button
        button(c, g.btnBackPwf, "BACK", 0xFF372A42.toInt(), 24f, 0xFF8D6E63.toInt())
    }

    private fun drawHostRoom(c: Canvas) {
        stageHall(c)
        p.color = 0xAA000000.toInt(); c.drawRect(0f, 0f, W, H, p)

        text(c, "PLAY WITH A FRIEND", 640f, 150f, 56f, gold)

        // Center Dark Card
        val card = RectF(380f, 195f, 900f, 530f)
        p.color = 0xEE1E1528.toInt(); c.drawRoundRect(card, 20f, 20f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF5D4037.toInt()
        c.drawRoundRect(card, 20f, 20f, p); p.style = Paint.Style.FILL

        text(c, "ROOM CODE", 640f, 245f, 20f, gold, outline = false)

        // Big Room Code Display
        text(c, g.hostRoomCode, 640f, 345f, 96f, white)

        // Action Buttons Row: [ Share on WhatsApp ] [ Copy link ]
        button(c, g.btnShareWhatsApp, "Share on WhatsApp", 0xFF25D366.toInt(), 20f, 0xFF69F0AE.toInt())
        button(c, g.btnCopyLink, "Copy link", 0xFF261D30.toInt(), 20f, white)

        // Pulsing Waiting Indicator
        val dotAlpha = if ((g.frame / 15) % 2 == 0) 0xFFFFC83D.toInt() else 0x66FFC83D
        p.color = dotAlpha; c.drawCircle(445f, 480f, 6f, p)
        text(c, "Waiting for your friend to join...", 655f, 486f, 21f, white, Paint.Align.CENTER, outline = false)

        // Back Button
        button(c, g.btnBackHost, "BACK", 0xFF372A42.toInt(), 24f, 0xFF8D6E63.toInt())
    }
}
