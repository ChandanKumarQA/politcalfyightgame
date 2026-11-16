package com.comedyarena.politicalfight.game3d.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/* Original, fictional cartoon archetypes. Not based on any real person. */

class SuperDef(val name: String, val desc: String, val color: Int, val shape: String, val dmg: Float)

data class CharDef(
    val id: String, val name: String, val title: String, val team: String,
    val spd: Float, val pwr: Float, val hp: Float, val def: Float,
    val skin: Int, val top: Int, val bottom: Int, val shoe: Int, val accent: Int,
    val hair: Int, val hairStyle: String = "none", val hat: String = "none", val hatColor: Int = 0xffffff,
    val glasses: Boolean = false, val stache: Int = 0, val beard: Boolean = false,
    val sup: SuperDef, val quote: String, val scale: Float = 1f, val boss: Boolean = false,
    val tag: String = ""
)

object Roster {
    val all: List<CharDef> = listOf(
        CharDef("kursi", "KURSI KUMAR", "Balanced Brawler", "Team Sinhasan", 1.0f, 1.0f, 100f, 1.0f,
            0xd9a066, 0xf28c28, 0xf5f0e6, 0x3b2a1a, 0xffd166, 0x222222, "none", "topi", 0xf6efd8, false, 1, false,
            SuperDef("KURSI THROW", "Hurls a giant golden chair across the arena.", 0xffc83d, "chair", 26f), "Kursi meri, fight meri!"),
        CharDef("jumla", "JUMLA JEE", "Speed Striker", "Team Vaada", 1.25f, 0.9f, 92f, 1.1f,
            0xc68642, 0x1fa3a3, 0xeeeeee, 0x1b1b1b, 0xffffff, 0x111111, "slick", "none", 0xffffff, true, 0, false,
            SuperDef("JUMLA JAAL", "Fires a flurry of rolled-up promises. Most of them explode.", 0xffffff, "scroll", 24f), "Achhe din aa gaye... bas aa hi rahe hain!"),
        CharDef("chai", "CHAI CHACHA", "Heavy Hitter", "Team Tapri", 0.82f, 1.25f, 120f, 0.9f,
            0xb87a4b, 0x8b5a2b, 0x4a3322, 0x2a1a10, 0xffffff, 0x2b1b10, "none", "none", 0xffffff, false, 2, false,
            SuperDef("GARAM CHAI", "Flings a scalding kulhad of extra-strong chai.", 0xc97b3a, "cup", 28f), "Pehle chai, phir ladai."),
        CharDef("rally", "RALLY RAJA", "Power Orator", "Team Jalsa", 0.98f, 1.15f, 102f, 1.0f,
            0xd9a066, 0xd62839, 0xf2e8cf, 0x3a1a1a, 0xffd166, 0x1a1a1a, "none", "turban", 0xe8b923, false, 2, true,
            SuperDef("LOUDSPEAKER BLAST", "Screams through a megaphone. Eardrums were harmed.", 0xff6b35, "mega", 27f), "Bhaiyon aur behno... zor se bolo!"),
        CharDef("toll", "TOLL TAU", "Iron Tank", "Team Highway", 0.75f, 1.1f, 132f, 0.78f,
            0xe0b48a, 0x4a5568, 0x2d3748, 0x111111, 0xfbbf24, 0x9a9a9a, "short", "none", 0xffffff, true, 1, false,
            SuperDef("TOLL BARRIER", "Drops a boom barrier. Pay up or get flattened.", 0xfbbf24, "barrier", 27f), "Rukiye. Toll bharna padega."),
        CharDef("netri", "NETRI NIMMI", "Rushdown Queen", "Team Dharna", 1.12f, 1.0f, 100f, 1.0f,
            0xe0ac83, 0x2563eb, 0x1e3a8a, 0xffffff, 0xf472b6, 0x1a1a1a, "bun", "none", 0xffffff, false, 0, false,
            SuperDef("DHARNA SHOCK", "A sit-in so intense it shakes the arena.", 0x60a5fa, "orb", 25f), "Hum yahin baithenge!"),
        CharDef("uncle", "WHATSAPP UNCLE", "Forward Trickster", "Team Forward", 1.0f, 1.05f, 105f, 1.0f,
            0xcb9462, 0x16a34a, 0xe5e7eb, 0x333333, 0xfacc15, 0x3b3b3b, "none", "cap", 0x16a34a, true, 1, false,
            SuperDef("GOOD MORNING SPAM", "Sends 400 flower pics. Direct hit.", 0x4ade80, "phone", 26f), "Share karo, warna bura hoga!"),
        CharDef("meme", "MEME MANTRI", "Agile Troll", "Team Viral", 1.18f, 1.0f, 96f, 1.0f,
            0xd2a679, 0x7c3aed, 0x1f2937, 0xfacc15, 0xf43f5e, 0x0f0f0f, "spiky", "none", 0xffffff, true, 0, false,
            SuperDef("VIRAL BLAST", "A cursed meme so spicy it hurts.", 0xa78bfa, "orb", 25f), "Ratio + L + fight kar."),
        CharDef("laddoo", "LADDOO LALA", "Grapple Master", "Team Mithai", 0.9f, 1.2f, 112f, 0.92f,
            0xd8a070, 0xfbbf24, 0xffffff, 0x7c2d12, 0xdc2626, 0x2a1a0a, "none", "none", 0xffffff, false, 2, false,
            SuperDef("LADDOO BARRAGE", "Rapid-fire motichoor laddoos. Sweet pain.", 0xf59e0b, "laddoo", 27f), "Mithai khao, mooh band rakho!"),
        CharDef("reel", "REEL RANI", "Influencer Menace", "Team Trending", 1.08f, 0.95f, 98f, 1.0f,
            0xe8b88a, 0xec4899, 0x831843, 0xffffff, 0x22d3ee, 0x5b2a86, "afro", "none", 0xffffff, true, 0, false,
            SuperDef("SELFIE FLASH", "Blinds the opponent with 12 filters at once.", 0xf9a8d4, "phone", 25f), "Like, share, subscribe... and lose."),
        CharDef("boss", "MEGA MANTRI", "BOSS · Final Form", "Team Kursi Forever", 0.95f, 1.3f, 150f, 0.75f,
            0xc9905c, 0x111827, 0x111827, 0xfbbf24, 0xfbbf24, 0xe5e7eb, "none", "crown", 0xfbbf24, false, 2, true,
            SuperDef("KURSI KA KHEL", "Rains golden chairs from the heavens.", 0xfbbf24, "chair", 32f), "Yeh kursi kabhi nahi chhodunga!", 1.18f, true)
    )
}

val STAGE_NAMES = arrayOf("SABHA HALL", "RALLY MAIDAN", "DESI SADAK")
val LEVEL_NAMES = arrayOf("EASY", "NORMAL", "HARD")
val HIT_WORDS = arrayOf("THOK!", "DHISHUM!", "OYE!", "ARRE!", "BAM!", "WAH!", "THAAP!")
val HEADLINES = arrayOf(
    "Sources say the chai vendor demanded compensation for broken cups.",
    "A stray cow paused the fight, then ate a placard.",
    "Exit polls, as usual, were completely wrong.",
    "Parliament adjourned after a heated debate over samosa size.",
    "Referee fined for blowing the whistle without a licence.",
    "Opposition walks out; ruling side walks out to follow them.",
    "Fact-check pending: forwarded as received.",
    "Committee formed to study why the committee was formed.",
    "Auto-rickshaw driver declares neutrality; meter still running."
)

/* ================= 500 generated levels ================= */
const val LEVEL_COUNT = 500
val TIERS = arrayOf("NEWBIE", "LOCAL", "MOHALLA", "TOWN", "DISTRICT", "STATE", "ZONAL", "REGIONAL", "NATIONAL", "LEGENDARY")
val MOD_KEYS = arrayOf("lowgrav", "bighead", "turbo", "glass", "fullsuper", "rage", "iron", "sprint", "giant", "mini", "slowmo")
val MOD_NAMES = arrayOf("LOW GRAVITY", "BIG HEADS", "TURBO SPEED", "GLASS CANNONS", "FULL SUPER START", "RAGE ENEMY", "IRON ENEMY",
    "SPRINT (30s)", "GIANT ENEMY", "MINI FIGHTERS", "SLOW-MO")

/** Gameplay modifiers for the current match. */
class Mods(
    var grav: Float = 0.0105f, var speed: Float = 1f, var dmg: Float = 1f, var enemyDmg: Float = 1f,
    var time: Int = 60, var m1: Float = 0f, var m2: Float = 0f, var bighead: Boolean = false, var mini: Boolean = false
)

class LevelInfo(val n: Int, val boss: Boolean, val def: CharDef, val stage: Int, val ai: Float, val mods: Mods, val modNames: List<String>, val tier: String)

private class Rng(seed: Int) {
    private var a = seed
    fun next(): Float {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xffffffffL).toDouble().div(4294967296.0).toFloat()
    }
}

fun shiftHue(hex: Int, dh: Float, ds: Float, dl: Float): Int {
    val r = ((hex shr 16) and 255) / 255f; val g = ((hex shr 8) and 255) / 255f; val b = (hex and 255) / 255f
    val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); var h = 0f; var s = 0f; val l = (mx + mn) / 2f
    if (mx != mn) {
        val d = mx - mn; s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        h = when (mx) { r -> (g - b) / d + (if (g < b) 6f else 0f); g -> (b - r) / d + 2f; else -> (r - g) / d + 4f } / 6f
    }
    h = ((h + dh) % 1f + 1f) % 1f; s = s + ds; s = if (s < 0f) 0f else if (s > 1f) 1f else s
    val ll = min(0.85f, max(0.1f, l + dl))
    fun hue(p: Float, q: Float, tt: Float): Float {
        var t = tt; if (t < 0f) t += 1f; if (t > 1f) t -= 1f
        return when { t < 1f / 6f -> p + (q - p) * 6f * t; t < 0.5f -> q; t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f; else -> p }
    }
    val q = if (ll < 0.5f) ll * (1f + s) else ll + s - ll * s; val p = 2f * ll - q
    val rr = (hue(p, q, h + 1f / 3f) * 255f + 0.5f).toInt(); val gg = (hue(p, q, h) * 255f + 0.5f).toInt(); val bb = (hue(p, q, h - 1f / 3f) * 255f + 0.5f).toInt()
    return (rr shl 16) or (gg shl 8) or bb
}

private val SKINS = intArrayOf(0xd9a066, 0xc68642, 0xb87a4b, 0xe0ac83, 0xcb9462, 0xe8b88a, 0x9c6b43)
private val HAIR_STYLES = arrayOf("none", "short", "spiky", "bun", "afro", "slick")

/** Deterministic: level n always yields the same enemy, arena, difficulty and twists. */
fun levelInfo(n: Int): LevelInfo {
    val R = Rng(n * 7919 + 13); val boss = n % 10 == 0; val tier = min(9, (n - 1) / 50)
    val base = if (boss) Roster.all[10] else Roster.all[(n * 3 + (R.next() * 3).toInt()) % 10]
    var d = base.copy(
        top = shiftHue(base.top, R.next(), (R.next() - 0.5f) * 0.3f, (R.next() - 0.5f) * 0.2f),
        bottom = shiftHue(base.bottom, R.next(), (R.next() - 0.5f) * 0.3f, (R.next() - 0.5f) * 0.2f),
        accent = shiftHue(base.accent, R.next(), 0f, 0f), shoe = shiftHue(base.shoe, R.next(), 0f, 0f))
    if (!boss) {
        d = d.copy(skin = SKINS[(R.next() * SKINS.size).toInt()], glasses = R.next() < 0.4f, stache = (R.next() * 3).toInt())
        if (base.hat == "none") d = d.copy(hairStyle = HAIR_STYLES[(R.next() * HAIR_STYLES.size).toInt()], hair = shiftHue(base.hair, R.next(), 0.1f, 0.1f))
        if (base.hat != "none") d = d.copy(hatColor = shiftHue(base.hatColor, R.next(), 0f, 0f))
    }
    var count = when { n < 5 -> 0; n < 100 -> if (R.next() < 0.6f) 1 else 0; n < 300 -> 1 + (if (R.next() < 0.5f) 1 else 0); else -> 1 + (R.next() * 3).toInt() }
    if (boss && count < 1) count = 1
    val mods = ArrayList<Int>()
    while (mods.size < count) { val m = (R.next() * MOD_KEYS.size).toInt(); if (!mods.contains(m)) mods.add(m) }
    val M = Mods()
    var hp = base.hp * (0.75f + n * 0.0045f) * (if (boss) 1.2f else 1f)
    var scale = if (boss) min(1.5f, 1.18f + n / 1500f) else base.scale
    mods.forEach { m ->
        when (MOD_KEYS[m]) {
            "lowgrav" -> M.grav = 0.0055f; "bighead" -> M.bighead = true; "turbo" -> M.speed = 1.3f
            "glass" -> M.dmg = 1.7f; "fullsuper" -> { M.m1 = 100f; M.m2 = 100f }; "rage" -> M.enemyDmg = 1.4f
            "iron" -> hp *= 1.5f; "sprint" -> M.time = 30; "giant" -> { scale *= 1.3f; hp *= 1.2f }
            "mini" -> { M.mini = true; scale *= 0.75f }; "slowmo" -> M.speed = 0.75f
        }
    }
    d = d.copy(pwr = base.pwr * (0.85f + n * 0.0014f), spd = base.spd * (0.92f + n * 0.0005f), def = base.def * (1f - n * 0.0005f),
        hp = Math.round(hp).toFloat(), scale = scale, tag = TIERS[tier] + " · LV " + n)
    val stage = (R.next() * 3).toInt()
    val ai = 3f * ((n - 1).toFloat() / (LEVEL_COUNT - 1)).pow(0.8f)
    return LevelInfo(n, boss, d, stage, ai, M, mods.map { MOD_NAMES[it] }, TIERS[tier])
}
