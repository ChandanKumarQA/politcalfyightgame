package com.comedyarena.politicalfight

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object K { const val W = 1600f; const val H = 900f; const val GROUND = 690f }

enum class S { IDLE, WALK, JUMP, CROUCH, PUNCH, KICK, GRAB, SWEEP, UPPER, SPECIAL, BLOCK, HIT, WIN, KO }

/**
 * held[] indices:
 * 0 Left, 1 Right, 2 Jump/Up, 3 Punch, 4 Kick, 5 Special, 6 Block, 7 Crouch/Down, 8 Grab
 */
class Fighter(val hero: Hero, var x: Float, val isAI: Boolean, val diff: Float = 0.5f, hpMul: Float = 1f) {
    val maxHp = 100f * hpMul
    var hp = maxHp
    var meter = 100f // Always fully charged / enabled for Special moves
    var y = K.GROUND
    var vx = 0f
    var vy = 0f
    var face = 1
    var state = S.IDLE
    var t = 0
    var hitDone = false
    var stun = 0
    var spawnFlag = false
    var portrait = false
    var anim = 0f
    var bubble = ""
    var bubbleT = 0
    var stageW = 1600f
    var headBmp: Bitmap? = null
    var punchChain = 0
    var wins = 0
    val held = BooleanArray(9)

    val aiLevel = when { diff < 0.4f -> 0; diff > 0.7f -> 2; else -> 1 }
    private val ai = Ai(aiLevel)
    private val rnd = java.util.Random()

    val onGround get() = y >= K.GROUND
    val crouching get() = state == S.CROUCH || state == S.SWEEP || state == S.UPPER
    val acting get() = state == S.PUNCH || state == S.KICK || state == S.GRAB ||
            state == S.SWEEP || state == S.UPPER || state == S.SPECIAL

    private val speed get() = (3.5f + hero.speed * 0.55f) * (if (isAI) 0.8f + 0.2f * diff else 1f)
    val dmgMul get() = (0.75f + hero.power * 0.07f) * (if (hero.ability == 1) 1.1f else 1f) *
            (if (hero.ability == 3 && hp < maxHp * 0.3f) 1.3f else 1f)

    fun say(s: String, frames: Int) { bubble = s; bubbleT = frames }

    private fun durOf(): Int {
        val b = when (state) {
            S.PUNCH -> 16
            S.KICK -> 24
            S.SWEEP -> 26
            S.UPPER -> 22
            S.GRAB -> 30
            S.SPECIAL -> 44
            else -> 0
        }
        return if (hero.ability == 0 && state != S.SPECIAL) (b * 0.8f).toInt() else b
    }

    fun activeNow(): Boolean = when (state) {
        S.PUNCH -> t in 4..8
        S.KICK -> t in 6..12
        S.SWEEP -> t in 7..14
        S.UPPER -> t in 5..10
        S.GRAB -> t in 6..11
        else -> false
    }

    private fun reach(): Float = when (state) {
        S.PUNCH -> 95f + (punchChain * 12f)
        S.KICK -> 135f
        S.SWEEP -> 140f
        S.UPPER -> 85f
        S.GRAB -> 75f
        else -> 90f
    } + (if (hero.ability == 6) 25f else 0f)

    fun atkRect(): RectF {
        val top = when (state) {
            S.SWEEP -> y - 80f
            S.UPPER -> y - 240f
            S.KICK -> y - 130f
            S.GRAB -> y - 160f
            else -> y - 210f
        }
        val bot = when (state) {
            S.SWEEP -> y
            S.UPPER -> y - 90f
            S.KICK -> y - 20f
            else -> y - 100f
        }
        val r = reach()
        return if (face > 0) RectF(x + 20f, top, x + 20f + r, bot) else RectF(x - 20f - r, top, x - 20f, bot)
    }

    fun hurtRect(): RectF {
        val top = if (crouching) y - 140f else y - 230f
        return RectF(x - 42f, top, x + 42f, y)
    }

    private fun startAtk(s: S) {
        state = s
        t = 0
        hitDone = false
        vx = 0f
    }

    /** Returns true if the hit was blocked. */
    fun takeHit(d: Float, kb: Float, dir: Int, heavy: Boolean, isGrab: Boolean = false): Boolean {
        if (state == S.KO || state == S.WIN) return false
        var dmg = d * (if (hero.ability == 5) 0.85f else 1f)
        val blocked = !isGrab && state == S.BLOCK && face == -dir

        if (blocked) {
            hp -= dmg * 0.12f
            vx = dir * kb * 0.4f
            meter = 100f
        } else {
            hp -= dmg
            vx = dir * kb
            stun = if (heavy || isGrab) 28 else 16
            state = S.HIT
            t = 0
            if (isGrab) vy = -8f
            meter = 100f
        }
        if (hp <= 0f) { hp = 0f; state = S.KO; vy = -10f; vx = dir * 6f }
        return blocked
    }

    fun think(opp: Fighter) {
        ai.think(this, opp)
    }

    fun update(opp: Fighter) {
        anim += 0.25f
        if (bubbleT > 0) bubbleT--
        if (state == S.KO || state == S.WIN) { vx *= 0.9f; physics(); return }

        // Special mode is ALWAYS enabled and can be activated at any time!
        if (held[5] && state != S.SPECIAL) {
            startAtk(S.SPECIAL)
        }

        if (stun > 0) {
            stun--
            vx *= 0.9f
            if (stun == 0 && state == S.HIT) state = S.IDLE
        } else if (acting) {
            t++
            vx *= 0.8f
            if (state == S.SPECIAL && t == 14) spawnFlag = true

            // Combo cancels: PUNCH can chain up to 3 times on hit
            if (state == S.PUNCH && hitDone && t in 6..14) {
                if (held[3] && punchChain < 2) {
                    punchChain++
                    startAtk(S.PUNCH)
                    return
                }
                if (held[4]) {
                    startAtk(S.KICK)
                    return
                }
            }

            if (t >= durOf()) {
                state = S.IDLE
                t = 0
                punchChain = 0
            }
        } else {
            if (onGround) face = if (opp.x >= x) 1 else -1

            if (held[6] && onGround) {
                state = S.BLOCK
                vx = 0f
            } else if (held[7] && onGround) {
                // CROUCH
                if (held[3]) {
                    startAtk(S.UPPER)
                } else if (held[4]) {
                    startAtk(S.SWEEP)
                } else {
                    state = S.CROUCH
                    vx = 0f
                }
            } else {
                if (state == S.BLOCK || state == S.CROUCH) state = S.IDLE
                var mv = 0f
                if (held[0]) mv -= 1f
                if (held[1]) mv += 1f
                vx = mv * speed * (if (!onGround && hero.ability == 7) 1.2f else 1f)

                if (held[2] && onGround) {
                    vy = -(17f + hero.jump * 0.8f) * (if (hero.ability == 7) 1.15f else 1f)
                }

                if (held[8] && onGround) {
                    startAtk(S.GRAB)
                } else if (held[3]) {
                    startAtk(S.PUNCH)
                } else if (held[4]) {
                    startAtk(S.KICK)
                }

                if (!acting) state = if (!onGround) S.JUMP else if (mv != 0f) S.WALK else S.IDLE
            }
        }
        physics()
    }

    private fun physics() {
        x += vx
        if (y < K.GROUND || vy < 0f) { vy += 1f; y += vy }
        if (y >= K.GROUND) { y = K.GROUND; vy = 0f }
        x = x.coerceIn(60f, stageW - 60f)
    }

    private fun ext(a: Int, b: Int, d: Int): Float =
        if (t < a) t / a.toFloat() else if (t <= b) 1f else max(0f, 1f - (t - b) / max(1, d - b).toFloat())

    fun draw(c: Canvas, p: Paint) {
        val OUT = 0xFF222222.toInt()
        c.save()
        c.translate(x, y)
        if (!portrait) {
            p.style = Paint.Style.FILL; p.color = 0x44000000
            val sy = K.GROUND - y
            val sw = 55f - sy * 0.05f
            c.drawOval(-sw, sy - 9f, sw, sy + 9f, p)
        }
        c.scale(face.toFloat(), 1f)
        if (state == S.KO) { c.translate(0f, -10f); c.rotate(-80f) }

        var bob = 0f; var lean = 0f
        // 3D Martial Arts Guard Stance (Hands raised to chest/chin, ready to strike!)
        var fHx = 26f; var fHy = -165f; var bHx = -10f; var bHy = -175f
        var fFx = 26f; var fFy = 0f; var bFx = -26f; var bFy = 0f
        var mouth = false; var dead = false

        when (state) {
            S.IDLE -> {
                bob = sin(anim * 1.2f) * 4f
                fHy += sin(anim * 1.2f) * 3f
                bHy -= sin(anim * 1.2f) * 3f
            }
            S.WALK -> {
                val s = sin(anim * 1.8f) * 26f
                fFx = s + 10f; bFx = -s - 10f
                bob = abs(sin(anim * 1.8f)) * -5f
                fHx = 30f - s * 0.5f; bHx = -30f + s * 0.5f
            }
            S.JUMP -> { fFx = 26f; fFy = -45f; bFx = -18f; bFy = -30f; fHy = -150f; bHy = -140f; fHx = 45f }
            S.CROUCH -> {
                bob = 35f // lower body down
                fFx = 35f; fFy = 0f; bFx = -30f; bFy = 0f
                fHy += 20f; bHy += 20f
            }
            S.PUNCH -> {
                val e = ext(4, 8, durOf())
                fHx = 36f + (85f + punchChain * 20f) * e
                fHy = -150f - punchChain * 10f
                lean = (8f + punchChain * 4f) * e
            }
            S.UPPER -> {
                val e = ext(4, 9, durOf())
                bob = 20f * (1f - e)
                fHx = 30f + 30f * e; fHy = -120f - 110f * e
                lean = -8f * e
            }
            S.KICK -> {
                val e = ext(6, 12, durOf())
                fFx = 22f + 110f * e; fFy = -70f * e
                lean = -12f * e; fHx = 20f; fHy = -140f; bHx = -50f; bHy = -130f
            }
            S.SWEEP -> {
                val e = ext(6, 14, durOf())
                bob = 40f
                fFx = 24f + 120f * e; fFy = 0f
                lean = -14f * e
            }
            S.GRAB -> {
                val e = ext(5, 10, durOf())
                fHx = 30f + 65f * e; fHy = -140f
                bHx = -20f + 55f * e; bHy = -140f
                lean = 10f * e
            }
            S.SPECIAL -> {
                val e = min(1f, t / 14f)
                fHx = 40f + 40f * e; fHy = -175f - 25f * e
                bHx = -10f + 40f * e; bHy = -190f - 10f * e
                mouth = true; bob = sin(anim * 3f) * 3f
            }
            S.BLOCK -> { fHx = 34f; fHy = -195f; bHx = 18f; bHy = -180f; bob = 6f }
            S.HIT -> { lean = -14f; fHx = -10f; fHy = -120f; bHx = -50f; bHy = -110f; dead = true }
            S.WIN -> {
                bob = -abs(sin(anim * 0.9f)) * 30f
                fHx = 38f; fHy = -250f + sin(anim * 2f) * 15f
                bHx = -30f; bHy = -250f - sin(anim * 2f) * 15f
                mouth = true
            }
            S.KO -> { fHx = -30f; fHy = -90f; bHx = -40f; bHy = -80f; dead = true }
        }

        fun limb(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, col: Int) {
            p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = w + 8f; p.color = OUT; c.drawLine(x1, y1, x2, y2, p)
            p.strokeWidth = w; p.color = col; c.drawLine(x1, y1, x2, y2, p)
        }
        fun circ(cx: Float, cy: Float, r: Float, col: Int) {
            p.style = Paint.Style.FILL; p.color = col; c.drawCircle(cx, cy, r, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = OUT; c.drawCircle(cx, cy, r, p)
        }
        fun rr(l: Float, t2: Float, r: Float, b: Float, rad: Float, col: Int) {
            p.style = Paint.Style.FILL; p.color = col; c.drawRoundRect(l, t2, r, b, rad, rad, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = OUT; c.drawRoundRect(l, t2, r, b, rad, rad, p)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, col: Int) {
            p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = w; p.color = col
            c.drawLine(x1, y1, x2, y2, p)
        }

        val hipY = -95f + bob
        val pants = 0xFFF5F5F5.toInt()
        // back leg
        limb(-8f, hipY, bFx, bFy, 24f, pants); circ(bFx + 6f, bFy - 4f, 13f, 0xFF4E342E.toInt())

        c.save()
        c.translate(0f, hipY); c.rotate(lean); c.translate(0f, -hipY)
        // back arm
        limb(-18f, -160f + bob, bHx, bHy, 20f, hero.primaryColor); circ(bHx, bHy, 13f, hero.skinColor)
        // 3D Shaded Torso
        rr(-38f, -178f + bob, 38f, -88f + bob, 28f, hero.primaryColor)
        p.style = Paint.Style.FILL; p.color = 0x33FFFFFF; c.drawRoundRect(RectF(-32f, -174f + bob, 8f, -92f + bob), 18f, 18f, p)
        p.color = 0x55000000; c.drawRect(-36f, -138f + bob, 36f, -124f + bob, p)

        // cartoon caricature head
        val cx = 4f; val cy = -212f + bob
        drawCartoonHead(c, p, cx, cy, dead, mouth, OUT, ::circ, ::line, ::rr)
        // front arm
        limb(18f, -160f + bob, fHx, fHy, 20f, hero.primaryColor); circ(fHx, fHy, 14f, hero.skinColor)
        c.restore()
        // front leg
        limb(8f, hipY, fFx, fFy, 24f, pants); circ(fFx + 6f, fFy - 4f, 13f, 0xFF4E342E.toInt())
        c.restore()
        p.strokeCap = Paint.Cap.BUTT
    }

    /**
     * Draw cartoon caricature head inside a HUD box or portrait frame.
     */
    fun drawHeadInBox(c: Canvas, p: Paint, cx: Float, cy: Float, scale: Float = 1f, facingLeft: Boolean = false) {
        c.save()
        c.translate(cx, cy)
        c.scale(if (facingLeft) -scale else scale, scale)
        val OUT = 0xFF1B1B1B.toInt()
        fun circ(x: Float, y: Float, r: Float, col: Int) {
            p.style = Paint.Style.FILL; p.color = col; c.drawCircle(x, y, r, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT; c.drawCircle(x, y, r, p)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, col: Int) {
            p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = w; p.color = col
            c.drawLine(x1, y1, x2, y2, p)
        }
        fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float, col: Int) {
            p.style = Paint.Style.FILL; p.color = col; c.drawRoundRect(RectF(l, t, r, b), rad, rad, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT; c.drawRoundRect(RectF(l, t, r, b), rad, rad, p)
        }
        drawCartoonHead(c, p, 0f, 0f, false, false, OUT, ::circ, ::line, ::rr)
        c.restore()
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun drawCartoonHead(
        c: Canvas, p: Paint, cx: Float, cy: Float,
        dead: Boolean, mouth: Boolean, OUT: Int,
        circ: (Float, Float, Float, Int) -> Unit,
        line: (Float, Float, Float, Float, Float, Int) -> Unit,
        rr: (Float, Float, Float, Float, Float, Int) -> Unit
    ) {
        // Base Cartoon Head & Ears
        if (hero.id == 10) {
            circ(cx - 36f, cy + 2f, 15f, hero.skinColor) // Mbapu big ears
        } else {
            circ(cx - 30f, cy + 2f, 10f, hero.skinColor)
        }
        circ(cx, cy, 38f, hero.skinColor)
        circ(cx + 34f, cy + 8f, 7f, hero.skinColor) // nose

        // Politician-Specific Facial Caricature Features
        when (hero.id) {
            0 -> { // MITRAMAN: White side hair, trimmed white beard & mustache, rimless glasses
                p.style = Paint.Style.FILL; p.color = 0xFFEEEEEE.toInt()
                c.drawArc(RectF(cx - 40f, cy - 38f, cx + 8f, cy + 18f), 100f, 160f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
                c.drawArc(RectF(cx - 40f, cy - 38f, cx + 8f, cy + 18f), 100f, 160f, true, p)

                p.style = Paint.Style.FILL; p.color = 0xFFF5F5F5.toInt()
                val bp = Path().apply {
                    moveTo(cx - 16f, cy + 12f)
                    quadTo(cx + 10f, cy + 42f, cx + 36f, cy + 22f)
                    quadTo(cx + 26f, cy + 10f, cx + 10f, cy + 12f)
                    close()
                }
                c.drawPath(bp, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT; c.drawPath(bp, p)
                rr(cx + 8f, cy + 10f, cx + 35f, cy + 19f, 5f, 0xFFEEEEEE.toInt())

                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF222222.toInt()
                    c.drawRoundRect(RectF(cx + 6f, cy - 10f, cx + 19f, cy + 2f), 3f, 3f, p)
                    c.drawRoundRect(RectF(cx + 23f, cy - 10f, cx + 36f, cy + 2f), 3f, 3f, p)
                    c.drawLine(cx + 19f, cy - 4f, cx + 23f, cy - 4f, p)
                }
            }
            1 -> { // YATRA BOY: Wavy dark hair, scruffy full beard
                p.style = Paint.Style.FILL; p.color = 0xFF2B2522.toInt()
                c.drawArc(RectF(cx - 38f, cy - 42f, cx + 38f, cy + 10f), 160f, 200f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
                c.drawArc(RectF(cx - 38f, cy - 42f, cx + 38f, cy + 10f), 160f, 200f, true, p)

                p.style = Paint.Style.FILL; p.color = 0xFF352E2B.toInt()
                val bp = Path().apply {
                    moveTo(cx - 24f, cy + 8f)
                    quadTo(cx + 8f, cy + 45f, cx + 38f, cy + 26f)
                    quadTo(cx + 24f, cy + 14f, cx + 4f, cy + 15f)
                    close()
                }
                c.drawPath(bp, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT; c.drawPath(bp, p)
                rr(cx + 6f, cy + 9f, cx + 36f, cy + 18f, 5f, 0xFF352E2B.toInt())
            }
            2 -> { // DEEDEE: Saree pallu border, chignon bun, bindi, glasses
                circ(cx - 36f, cy + 8f, 15f, 0xFF1A1A1A.toInt())
                p.style = Paint.Style.FILL; p.color = 0xFFFAFAFA.toInt()
                c.drawArc(RectF(cx - 40f, cy - 40f, cx + 34f, cy + 15f), 140f, 190f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = 0xFF1976D2.toInt()
                c.drawArc(RectF(cx - 40f, cy - 40f, cx + 34f, cy + 15f), 140f, 190f, false, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
                c.drawArc(RectF(cx - 40f, cy - 40f, cx + 34f, cy + 15f), 140f, 190f, false, p)

                p.style = Paint.Style.FILL; p.color = 0xFFD32F2F.toInt()
                c.drawCircle(cx + 18f, cy - 14f, 4f, p)

                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF212121.toInt()
                    c.drawCircle(cx + 12f, cy - 4f, 8f, p)
                    c.drawCircle(cx + 28f, cy - 4f, 8f, p)
                    c.drawLine(cx + 20f, cy - 4f, cx + 22f, cy - 4f, p)
                }
            }
            3 -> { // TARIFF TAAU: Golden swept-forward quiff
                p.style = Paint.Style.FILL; p.color = 0xFFFFD54F.toInt()
                val hp = Path().apply {
                    moveTo(cx - 38f, cy - 20f)
                    cubicTo(cx - 40f, cy - 54f, cx + 10f, cy - 60f, cx + 46f, cy - 38f)
                    cubicTo(cx + 48f, cy - 24f, cx + 30f, cy - 20f, cx + 16f, cy - 28f)
                    close()
                }
                c.drawPath(hp, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawPath(hp, p)
                rr(-5f, -145f, 5f, -100f, 3f, 0xFFD50000.toInt()) // Red tie
            }
            4 -> { // DOZER DADA: Bald saffron dome, tilak, gold earring
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFFE65100.toInt()
                c.drawLine(cx + 8f, cy - 22f, cx + 28f, cy - 22f, p)
                c.drawLine(cx + 6f, cy - 17f, cx + 30f, cy - 17f, p)
                p.color = 0xFFD50000.toInt(); c.drawCircle(cx + 18f, cy - 17f, 3f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFFFFD700.toInt()
                c.drawCircle(cx - 30f, cy + 9f, 5f, p)
            }
            5 -> { // TOLL MAN: Groomed mustache & side-parted hair
                p.style = Paint.Style.FILL; p.color = 0xFF212121.toInt()
                c.drawArc(RectF(cx - 38f, cy - 40f, cx + 38f, cy + 4f), 180f, 180f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
                c.drawArc(RectF(cx - 38f, cy - 40f, cx + 38f, cy + 4f), 180f, 180f, true, p)
                rr(cx + 8f, cy + 12f, cx + 36f, cy + 20f, 5f, 0xFF212121.toInt())
            }
            6 -> { // SUTTI BABU: Voluminous puff hair, designer dark glasses
                p.style = Paint.Style.FILL; p.color = 0xFF1C1917.toInt()
                c.drawArc(RectF(cx - 32f, cy - 50f, cx + 36f, cy), 170f, 190f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
                c.drawArc(RectF(cx - 32f, cy - 50f, cx + 36f, cy), 170f, 190f, true, p)
                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF111111.toInt()
                    c.drawRoundRect(RectF(cx + 6f, cy - 10f, cx + 19f, cy + 3f), 4f, 4f, p)
                    c.drawRoundRect(RectF(cx + 23f, cy - 10f, cx + 36f, cy + 3f), 4f, 4f, p)
                    c.drawLine(cx + 19f, cy - 4f, cx + 23f, cy - 4f, p)
                }
            }
            7 -> { // RAIL RAJA: White side puffs, round spectacles
                circ(cx - 32f, cy + 6f, 14f, 0xFFEEEEEE.toInt())
                circ(cx + 8f, cy - 28f, 10f, 0xFFEEEEEE.toInt())
                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
                    c.drawCircle(cx + 12f, cy - 4f, 9f, p)
                    c.drawCircle(cx + 28f, cy - 4f, 9f, p)
                    c.drawLine(cx + 20f, cy - 4f, cx + 22f, cy - 4f, p)
                }
            }
            8 -> { // GHAR MANTRI: Full white beard & mustache, dark-rim glasses
                p.style = Paint.Style.FILL; p.color = 0xFFF0F0F0.toInt()
                val bp = Path().apply {
                    moveTo(cx - 18f, cy + 10f)
                    quadTo(cx + 10f, cy + 45f, cx + 38f, cy + 24f)
                    quadTo(cx + 28f, cy + 12f, cx + 12f, cy + 14f)
                    close()
                }
                c.drawPath(bp, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT; c.drawPath(bp, p)
                rr(cx + 6f, cy + 10f, cx + 36f, cy + 20f, 6f, 0xFFF0F0F0.toInt())
                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
                    c.drawRoundRect(RectF(cx + 6f, cy - 10f, cx + 19f, cy + 2f), 3f, 3f, p)
                    c.drawRoundRect(RectF(cx + 23f, cy - 10f, cx + 36f, cy + 2f), 3f, 3f, p)
                    c.drawLine(cx + 19f, cy - 4f, cx + 23f, cy - 4f, p)
                }
            }
            9 -> { // FACT BHAI: Modern clean styled hair & stubble
                p.style = Paint.Style.FILL; p.color = 0xFF212121.toInt()
                c.drawArc(RectF(cx - 36f, cy - 44f, cx + 34f, cy + 4f), 170f, 190f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
                c.drawArc(RectF(cx - 36f, cy - 44f, cx + 34f, cy + 4f), 170f, 190f, true, p)
                p.style = Paint.Style.FILL; p.color = 0x44000000
                c.drawRoundRect(RectF(cx - 10f, cy + 16f, cx + 34f, cy + 34f), 8f, 8f, p)
                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF37474F.toInt()
                    c.drawRoundRect(RectF(cx + 8f, cy - 10f, cx + 20f, cy + 2f), 3f, 3f, p)
                    c.drawRoundRect(RectF(cx + 23f, cy - 10f, cx + 35f, cy + 2f), 3f, 3f, p)
                }
            }
            10 -> { // MBAPU (BOSS): Round spectacles, white drooping mustache
                if (!dead) {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
                    c.drawCircle(cx + 12f, cy - 4f, 10f, p)
                    c.drawCircle(cx + 28f, cy - 4f, 10f, p)
                    c.drawLine(cx + 20f, cy - 4f, cx + 22f, cy - 4f, p)
                    c.drawLine(cx + 4f, cy - 4f, cx - 18f, cy - 2f, p)
                }
                rr(cx + 8f, cy + 12f, cx + 36f, cy + 22f, 6f, 0xFFEEEEEE.toInt())
            }
            else -> { // Default / Random / Trailer: Clean hairstyle
                p.style = Paint.Style.FILL; p.color = 0xFF212121.toInt()
                c.drawArc(RectF(cx - 36f, cy - 42f, cx + 36f, cy + 4f), 180f, 180f, true, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
                c.drawArc(RectF(cx - 36f, cy - 42f, cx + 36f, cy + 4f), 180f, 180f, true, p)
            }
        }

        // Animated Expressive Eyes or KO state
        if (dead) {
            line(cx + 4f, cy - 10f, cx + 18f, cy + 4f, 4f, OUT)
            line(cx + 18f, cy - 10f, cx + 4f, cy + 4f, 4f, OUT)
            line(cx + 22f, cy - 10f, cx + 36f, cy + 4f, 4f, OUT)
            line(cx + 36f, cy - 10f, cx + 22f, cy + 4f, 4f, OUT)
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = 0xFFFFD700.toInt()
            c.drawOval(RectF(cx - 16f, cy - 56f, cx + 26f, cy - 44f), p)
        } else {
            circ(cx + 11f, cy - 4f, 8f, 0xFFFFFFFF.toInt())
            circ(cx + 28f, cy - 4f, 8f, 0xFFFFFFFF.toInt())
            circ(cx + 14f, cy - 4f, 4f, OUT)
            circ(cx + 31f, cy - 4f, 4f, OUT)
            line(cx + 4f, cy - 16f, cx + 18f, cy - 13f, 4f, OUT)
            line(cx + 22f, cy - 13f, cx + 35f, cy - 16f, 4f, OUT)
        }

        // Mouth (Shouting on attack or expressive grin)
        if (mouth || dead) {
            p.style = Paint.Style.FILL; p.color = 0xFF8B0000.toInt()
            c.drawOval(cx + 8f, cy + 16f, cx + 28f, cy + 30f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
            c.drawOval(cx + 8f, cy + 16f, cx + 28f, cy + 30f, p)
        } else if (hero.id != 0 && hero.id != 1 && hero.id != 8 && hero.id != 10) {
            val pa = Path().apply {
                moveTo(cx + 8f, cy + 20f)
                quadTo(cx + 20f, cy + 30f, cx + 30f, cy + 20f)
            }
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT
            c.drawPath(pa, p)
        }
    }
}
