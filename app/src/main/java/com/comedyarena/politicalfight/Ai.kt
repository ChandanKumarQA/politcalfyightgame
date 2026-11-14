package com.comedyarena.politicalfight

import java.util.Random
import kotlin.math.abs

/**
 * Intelligent CPU AI Controller for Political Fight Comedy Arena.
 * Supports 3 difficulty levels:
 * 0: EASY (diff ~0.3f)   - Slower reactions, basic movement, occasional punches/kicks
 * 1: NORMAL (diff ~0.5f) - Balanced movement, blocks incoming hits, executes combos
 * 2: HARD (diff ~0.85f)  - Smart spacing, aggressive blocking, uppercuts, sweeps, grabs & specials
 */
class Ai(val level: Int = 1) {
    private var plan = 0
    private var planT = 0
    private var blockT = 0
    private var atkCd = 30
    private var prevOpAtk = false
    private val rnd = Random()

    private fun chance(p: Float) = rnd.nextFloat() < p

    fun think(me: Fighter, op: Fighter) {
        java.util.Arrays.fill(me.held, false)
        if (me.state == S.KO || me.state == S.WIN || op.state == S.KO) return

        val dx = op.x - me.x
        val dist = abs(dx)
        val dir = if (dx > 0f) 1 else -1

        val toward = if (dir > 0) 1 else 0 // index 1 is Right, 0 is Left
        val away = if (dir > 0) 0 else 1

        if (atkCd > 0) atkCd--

        // Reactive Blocking: Detect opponent starting an attack while in close/mid range
        val opAtk = op.acting
        if (opAtk && !prevOpAtk && dist < 180f && chance(0.25f + 0.35f * level)) {
            blockT = 16 + rnd.nextInt(12)
        }
        prevOpAtk = opAtk
        if (blockT > 0) {
            blockT--
            if (me.onGround && !me.acting) {
                me.held[6] = true // Block button
                return
            }
        }

        if (me.state == S.HIT || me.state == S.BLOCK) return

        if (planT-- <= 0) {
            plan = rnd.nextInt(6)
            planT = 14 + rnd.nextInt(26) - level * 4
        }

        val free = !me.acting && me.onGround
        when (plan) {
            0, 5 -> if (dist > 120f) me.held[toward] = true else if (free) attack(me, dist)
            1 -> if (dist < 200f) me.held[away] = true else me.held[toward] = true
            2 -> {
                // Jump in
                if (free && dist in 140f..380f && chance(0.6f)) me.held[2] = true // Jump
                me.held[toward] = true
            }
            3 -> {
                // Defensive crouch or block
                if (free && dist < 160f) {
                    if (chance(0.5f)) me.held[7] = true // Crouch
                    else me.held[6] = true // Block
                } else me.held[toward] = true
            }
            4 -> {
                // Special attack from distance (Always enabled!)
                if (free && dist in 220f..700f && chance(0.3f + 0.3f * level)) {
                    me.held[5] = true // Special attack
                } else me.held[toward] = true
            }
        }

        // Air attack (Kick in mid-air)
        if (!me.onGround && dist < 150f && chance(0.35f)) {
            me.held[4] = true // Kick
        }

        // Combo continuation: Chain into next punch or kick when previous hit connects
        if (me.acting && me.hitDone && chance(0.3f + 0.3f * level)) {
            if (chance(0.6f)) me.held[3] = true // Punch chain
            else me.held[4] = true // Kick chain
        }
    }

    private fun attack(me: Fighter, dist: Float) {
        if (atkCd > 0) return
        atkCd = (24 - level * 5 + rnd.nextInt(18)).coerceAtLeast(8)
        val r = rnd.nextInt(100)
        when {
            // Close Range Grab (Unblockable!)
            r < 18 && dist < 95f -> me.held[8] = true // Grab
            // Uppercut (Crouch + Punch)
            r < 34 && dist < 100f -> { me.held[7] = true; me.held[3] = true }
            // Low Sweep (Crouch + Kick)
            r < 48 && dist < 140f -> { me.held[7] = true; me.held[4] = true }
            // Basic Punch
            r < 74 -> me.held[3] = true
            // Basic Kick
            r < 92 -> me.held[4] = true
            // Special Attack
            else -> me.held[5] = true
        }
    }
}
