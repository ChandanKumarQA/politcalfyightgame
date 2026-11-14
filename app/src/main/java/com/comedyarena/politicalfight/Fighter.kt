package com.comedyarena.politicalfight

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    private val legPath = Path()
    private val armPath = Path()
    private val kurtaPath = Path()
    private val shoePath = Path()
    private val fistRect = RectF()

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
            val sy = K.GROUND - y
            // Soft diffuse ambient ground shadow
            p.style = Paint.Style.FILL; p.color = 0x2A000000
            val sw = 75f - sy * 0.05f
            c.drawOval(-sw, sy - 8f, sw, sy + 8f, p)
        }
        c.scale(face.toFloat(), 1f)
        if (state == S.KO) { c.translate(0f, -10f); c.rotate(-80f) }

        var bob = 0f; var lean = 0f
        // 3D Martial Arts Guard Stance (Hands raised to chest/chin, ready to strike!)
        var fHx = 28f; var fHy = -168f; var bHx = -12f; var bHy = -178f
        var fFx = 22f; var fFy = 0f; var bFx = -24f; var bFy = 0f
        var mouth = false; var dead = false

        when (state) {
            S.IDLE -> {
                bob = sin(anim * 1.4f) * 4f
                fHy += sin(anim * 1.4f) * 3f
                bHy -= sin(anim * 1.4f) * 2f
            }
            S.WALK -> {
                val s = sin(anim * 1.8f) * 28f
                fFx = s + 12f; bFx = -s - 12f
                bob = abs(sin(anim * 1.8f)) * -5f
                fHx = 32f - s * 0.5f; bHx = -28f + s * 0.5f
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

        // Contact shadows directly under the feet
        if (!portrait && onGround) {
            val sy = K.GROUND - y
            p.style = Paint.Style.FILL; p.color = 0x55000000
            c.drawOval(bFx - 14f, sy + bFy - 4f, bFx + 20f, sy + bFy + 4f, p)
            c.drawOval(fFx - 14f, sy + fFy - 4f, fFx + 22f, sy + fFy + 4f, p)
        }

        val hipY = -95f + bob

        // --- Volumetric 3D Shoe Drawing ---
        fun draw3DShoe(fx: Float, fy: Float, isBack: Boolean) {
            val shoeColor = when (hero.id) {
                0 -> 0xFFFAFAFA.toInt() // Mitraman: pristine royal white jutti
                1 -> 0xFF5D4037.toInt() // Yatra Boy: brown leather sandal/chappal
                3 -> 0xFF212121.toInt() // Tariff Taau: black formal shoes
                else -> 0xFF4E342E.toInt() // Traditional tan/brown peshawari
            }
            val col = if (isBack) 0xFF3E2723.toInt() else shoeColor

            // Sole strip along the ground
            p.style = Paint.Style.FILL; p.color = 0xFF1B1B1B.toInt()
            c.drawRoundRect(RectF(fx - 14f, fy - 2f, fx + 26f, fy + 4f), 2f, 2f, p)

            // Upper shoe body with pointed/upturned toe
            shoePath.reset()
            shoePath.moveTo(fx - 14f, fy - 2f)
            shoePath.quadTo(fx - 2f, fy - 14f, fx + 8f, fy - 11f)
            shoePath.quadTo(fx + 20f, fy - 10f, fx + 28f, fy - 7f)
            shoePath.quadTo(fx + 30f, fy - 3f, fx + 25f, fy - 1f)
            shoePath.lineTo(fx - 14f, fy - 1f)
            shoePath.close()

            p.style = Paint.Style.FILL; p.color = col; c.drawPath(shoePath, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT; c.drawPath(shoePath, p)

            // Leather highlight on top of shoe
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x40FFFFFF
            c.drawLine(fx - 4f, fy - 8f, fx + 16f, fy - 7f, p)

            if (hero.id == 1) {
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
                c.drawLine(fx - 6f, fy - 9f, fx + 6f, fy - 2f, p)
                c.drawLine(fx + 4f, fy - 10f, fx + 14f, fy - 2f, p)
            }
        }

        // --- Volumetric 3D Leg with Churidar Folds ---
        fun draw3DLeg(hx: Float, hy: Float, fx: Float, fy: Float, isBack: Boolean) {
            val knX = (hx + fx) / 2f + (if (isBack) 7f else 13f)
            val knY = (hy + fy) / 2f + 4f
            val legCol = if (isBack) 0xFFE0E0E0.toInt() else 0xFFF5F5F5.toInt()

            // Thigh: volumetric quad from hip to knee
            legPath.reset()
            legPath.moveTo(hx - 14f, hy); legPath.lineTo(hx + 14f, hy)
            legPath.lineTo(knX + 11f, knY); legPath.lineTo(knX - 11f, knY)
            legPath.close()
            p.style = Paint.Style.FILL; p.color = legCol; c.drawPath(legPath, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawPath(legPath, p)

            // Thigh cloth fold highlight
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0x50FFFFFF
            c.drawLine(hx, hy + 4f, knX, knY - 4f, p)

            // Knee cap contour
            p.style = Paint.Style.FILL; p.color = legCol
            c.drawCircle(knX, knY, 11f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
            c.drawArc(RectF(knX - 11f, knY - 11f, knX + 11f, knY + 11f), -40f, 160f, false, p)

            // Calf: from knee down to ankle
            legPath.reset()
            legPath.moveTo(knX - 11f, knY); legPath.lineTo(knX + 11f, knY)
            legPath.lineTo(fx + 8f, fy - 10f); legPath.lineTo(fx - 9f, fy - 10f)
            legPath.close()
            p.style = Paint.Style.FILL; p.color = legCol; c.drawPath(legPath, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawPath(legPath, p)

            // Churidar horizontal wrinkle rings (authentic Indian clothing detail)
            val rings = floatArrayOf(fy - 14f, fy - 21f, fy - 28f)
            for (ry in rings) {
                val rw = 9f
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0x33000000
                c.drawLine(fx - rw, ry, fx + rw, ry + 1f, p)
                p.color = 0x66FFFFFF
                c.drawLine(fx - rw + 1f, ry - 1f, fx + rw - 1f, ry, p)
            }

            draw3DShoe(fx, fy, isBack)
        }

        // --- Volumetric 3D Arm with Clenched Martial Arts Fist ---
        fun draw3DArm(sx: Float, sy: Float, hx: Float, hy: Float, isBack: Boolean) {
            val midX = (sx + hx) / 2f
            val midY = (sy + hy) / 2f
            val isPunching = state == S.PUNCH || state == S.SPECIAL
            val elX = if (isPunching) midX else midX + (if (isBack) -12f else -8f)
            val elY = if (isPunching) midY else max(midY + 14f, sy + 18f)

            val armColor = hero.primaryColor
            val sleeveCol = if (isBack) {
                val r = (Color.red(armColor) * 0.8f).toInt()
                val g = (Color.green(armColor) * 0.8f).toInt()
                val b = (Color.blue(armColor) * 0.8f).toInt()
                Color.rgb(r, g, b)
            } else armColor

            // Shoulder sphere
            p.style = Paint.Style.FILL; p.color = sleeveCol; c.drawCircle(sx, sy, 13f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawCircle(sx, sy, 13f, p)

            // Upper arm sleeve: quad from shoulder to elbow
            armPath.reset()
            armPath.moveTo(sx - 12f, sy); armPath.lineTo(sx + 12f, sy)
            armPath.lineTo(elX + 9f, elY); armPath.lineTo(elX - 9f, elY)
            armPath.close()
            p.style = Paint.Style.FILL; p.color = sleeveCol; c.drawPath(armPath, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawPath(armPath, p)

            // Highlight along upper arm
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0x40FFFFFF
            c.drawLine(sx, sy - 2f, elX, elY - 2f, p)

            // Elbow cuff
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
            c.drawLine(elX - 9f, elY, elX + 9f, elY, p)

            // Forearm: from elbow to wrist
            armPath.reset()
            armPath.moveTo(elX - 9f, elY); armPath.lineTo(elX + 9f, elY)
            armPath.lineTo(hx + 7f, hy); armPath.lineTo(hx - 7f, hy)
            armPath.close()
            p.style = Paint.Style.FILL; p.color = sleeveCol; c.drawPath(armPath, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawPath(armPath, p)

            // Wrist cuff
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = OUT
            c.drawLine(hx - 7f, hy, hx + 7f, hy, p)

            // Clenched 3D Martial Arts Fist
            val skin = if (isBack) {
                val r = (Color.red(hero.skinColor) * 0.85f).toInt()
                val g = (Color.green(hero.skinColor) * 0.85f).toInt()
                val b = (Color.blue(hero.skinColor) * 0.85f).toInt()
                Color.rgb(r, g, b)
            } else hero.skinColor

            fistRect.set(hx - 9f, hy - 8f, hx + 12f, hy + 9f)
            p.style = Paint.Style.FILL; p.color = skin; c.drawRoundRect(fistRect, 5f, 5f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3.5f; p.color = OUT; c.drawRoundRect(fistRect, 5f, 5f, p)

            // 4 Defined Knuckles on striking front
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x55000000
            c.drawLine(hx + 8f, hy - 6f, hx + 8f, hy + 7f, p)
            c.drawLine(hx + 1f, hy - 4f, hx + 8f, hy - 4f, p)
            c.drawLine(hx + 1f, hy, hx + 8f, hy, p)
            c.drawLine(hx + 1f, hy + 4f, hx + 8f, hy + 4f, p)

            // Wrapped Thumb flap across front
            p.style = Paint.Style.FILL; p.color = skin
            c.drawRoundRect(RectF(hx - 6f, hy - 1f, hx + 5f, hy + 6f), 3f, 3f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = OUT
            c.drawRoundRect(RectF(hx - 6f, hy - 1f, hx + 5f, hy + 6f), 3f, 3f, p)

            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x50FFFFFF
            c.drawLine(hx - 5f, hy - 6f, hx + 9f, hy - 6f, p)
        }

        // --- Volumetric 3D Torso: Kurta Lower Skirt, Nehru Collar, Modi Waistcoat ---
        fun draw3DTorso() {
            val kurtaCol = 0xFFF5F5F5.toInt()
            val vestCol = hero.primaryColor

            // 1. Kurta Lower Drape / Skirt (hangs below waist to mid-thigh)
            kurtaPath.reset()
            kurtaPath.moveTo(-26f, hipY - 6f)
            kurtaPath.lineTo(26f, hipY - 6f)
            kurtaPath.lineTo(34f, hipY + 46f)
            kurtaPath.lineTo(-32f, hipY + 46f)
            kurtaPath.close()

            p.style = Paint.Style.FILL; p.color = kurtaCol; c.drawPath(kurtaPath, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawPath(kurtaPath, p)

            // Vertical cloth pleats & side slits
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = 0x33000000
            c.drawLine(-12f, hipY + 4f, -14f, hipY + 44f, p)
            c.drawLine(2f, hipY + 4f, 4f, hipY + 44f, p)
            c.drawLine(16f, hipY + 4f, 20f, hipY + 44f, p)
            p.color = 0x40FFFFFF
            c.drawLine(-10f, hipY + 4f, -12f, hipY + 44f, p)
            c.drawLine(4f, hipY + 4f, 6f, hipY + 44f, p)

            // 2. Nehru Collar Band around neck base
            val collarRect = RectF(-14f, -196f + bob, 14f, -182f + bob)
            p.style = Paint.Style.FILL; p.color = vestCol; c.drawRoundRect(collarRect, 4f, 4f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3.5f; p.color = OUT; c.drawRoundRect(collarRect, 4f, 4f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = OUT
            c.drawLine(0f, -196f + bob, 0f, -182f + bob, p)

            // 3. Nehru Waistcoat / Modi Vest (Chest & Abdomen)
            val vestRect = RectF(-34f, -182f + bob, 34f, hipY)
            p.style = Paint.Style.FILL; p.color = vestCol; c.drawRoundRect(vestRect, 18f, 18f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4.5f; p.color = OUT; c.drawRoundRect(vestRect, 18f, 18f, p)

            // Center vertical button placket band
            p.style = Paint.Style.FILL; p.color = 0x22000000
            c.drawRect(-5f, -182f + bob, 5f, hipY, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x33000000
            c.drawLine(-5f, -182f + bob, -5f, hipY, p)
            c.drawLine(5f, -182f + bob, 5f, hipY, p)

            // 4 Brass / Golden Buttons with 3D specular highlight
            val btnYs = floatArrayOf(-166f, -146f, -126f, -106f)
            for (by in btnYs) {
                val yPos = by + bob
                p.style = Paint.Style.FILL; p.color = 0xFFFFD700.toInt()
                c.drawCircle(0f, yPos, 3.5f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = OUT
                c.drawCircle(0f, yPos, 3.5f, p)
                p.style = Paint.Style.FILL; p.color = Color.WHITE
                c.drawCircle(-1f, yPos - 1f, 1.2f, p)
            }

            // Left Breast Welt Pocket with pocket square
            val pocketRect = RectF(-26f, -154f + bob, -10f, -144f + bob)
            p.style = Paint.Style.FILL; p.color = 0xFFFFFFFF.toInt()
            c.drawRoundRect(RectF(-24f, -158f + bob, -12f, -153f + bob), 2f, 2f, p)
            p.style = Paint.Style.FILL; p.color = vestCol; c.drawRoundRect(pocketRect, 3f, 3f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = OUT; c.drawRoundRect(pocketRect, 3f, 3f, p)

            // 3D Rounded Chest Volume Lighting
            p.style = Paint.Style.FILL; p.color = 0x25FFFFFF
            c.drawRoundRect(RectF(-28f, -178f + bob, 10f, hipY - 6f), 12f, 12f, p)
            p.color = 0x22000000
            c.drawRoundRect(RectF(-33f, -178f + bob, -24f, hipY - 6f), 6f, 6f, p)
        }

        fun circ(cx: Float, cy: Float, r: Float, col: Int) {
            p.style = Paint.Style.FILL; p.color = col; c.drawCircle(cx, cy, r, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawCircle(cx, cy, r, p)
        }
        fun rr(l: Float, t2: Float, r: Float, b: Float, rad: Float, col: Int) {
            p.style = Paint.Style.FILL; p.color = col; c.drawRoundRect(l, t2, r, b, rad, rad, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = OUT; c.drawRoundRect(l, t2, r, b, rad, rad, p)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, col: Int) {
            p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = w; p.color = col
            c.drawLine(x1, y1, x2, y2, p)
        }

        // Layer 1: Back Leg + Back Mojari
        draw3DLeg(-10f, hipY, bFx, bFy, isBack = true)

        c.save()
        c.translate(0f, hipY); c.rotate(lean); c.translate(0f, -hipY)

        // Layer 2: Back Arm + Clenched Fist
        draw3DArm(-20f, -162f + bob, bHx, bHy, isBack = true)

        // Layer 3: Volumetric Torso, Kurta Drape & Modi Vest
        draw3DTorso()

        // Layer 4: Expressive Caricature Head
        val cx = 4f; val cy = -212f + bob
        drawCartoonHead(c, p, cx, cy, dead, mouth, OUT, ::circ, ::line, ::rr)

        // Layer 5: Front Arm in Martial Arts Guard
        draw3DArm(20f, -162f + bob, fHx, fHy, isBack = false)

        c.restore()

        // Layer 6: Front Leg + Churidar Folds + Front Mojari
        draw3DLeg(10f, hipY, fFx, fFy, isBack = false)

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
