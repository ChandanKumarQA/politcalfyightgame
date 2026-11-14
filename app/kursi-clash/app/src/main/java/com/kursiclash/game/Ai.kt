package com.kursiclash.game

import java.util.Random
import kotlin.math.abs

/** Simple CPU opponent. level: 0 = easy, 1 = normal, 2 = hard. */
class Ai(private val level: Int) {
    val inp = Input()
    private var plan = 0
    private var planT = 0
    private var blockT = 0
    private var atkCd = 40
    private var prevOpAtk = false
    private val rnd = Random()

    private fun chance(p: Float) = rnd.nextFloat() < p

    fun think(me: Fighter, op: Fighter) {
        inp.clearAll()
        if (me.st == St.KO || op.st == St.KO) return

        val dx = op.x - me.x
        val d = abs(dx)
        val tw = if (dx > 0f) 1 else -1
        fun toward() { if (tw > 0) inp.right = true else inp.left = true }
        fun away() { if (tw > 0) inp.left = true else inp.right = true }

        if (atkCd > 0) atkCd--

        // React to the player's attack by blocking (holding away).
        val opAtk = op.st == St.ATTACK
        if (opAtk && !prevOpAtk && d < 170f && chance(0.2f + 0.25f * level)) {
            blockT = 18 + rnd.nextInt(10)
        }
        prevOpAtk = opAtk
        if (blockT > 0) {
            blockT--
            if (me.onGround && me.st != St.ATTACK) { away(); return }
        }
        if (me.st == St.HIT || me.st == St.BLOCK) return

        if (planT-- <= 0) {
            plan = rnd.nextInt(5)
            planT = 14 + rnd.nextInt(30) - level * 5
        }
        val free = me.st != St.ATTACK && me.onGround
        when (plan) {
            0, 4 -> if (d > 105f) toward() else if (free) attack(d)
            1 -> if (d < 220f) away() else toward()
            2 -> {
                if (free && d > 150f && d < 400f) inp.up = true
                toward()
            }
            else -> if (d > 130f && chance(0.05f)) toward()
        }

        // Air attack
        if (!me.onGround && d < 140f && chance(0.12f)) inp.kick = true
        // Super
        if (me.meter >= 100f && d > 200f && free && chance(0.04f + 0.03f * level)) inp.sup = true
        // Continue a combo after a hit lands
        if (me.st == St.ATTACK && me.hitDone && chance(0.25f + 0.2f * level)) {
            if (chance(0.6f)) inp.punch = true else inp.kick = true
        }
    }

    private fun attack(d: Float) {
        if (atkCd > 0) return
        atkCd = 26 - level * 5 + rnd.nextInt(20)
        val r = rnd.nextInt(100)
        when {
            r < 38 -> inp.punch = true
            r < 62 -> inp.kick = true
            r < 74 -> { inp.down = true; inp.kick = true }
            r < 84 -> { inp.down = true; inp.punch = true }
            r < 94 && d < 100f -> inp.grab = true
            else -> inp.punch = true
        }
    }
}
