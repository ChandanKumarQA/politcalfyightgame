package com.kursiclash.game

// Virtual resolution; everything is drawn in this space and scaled to the screen.
const val W = 1280f
const val H = 720f
const val GROUND = 610f

enum class St { IDLE, WALK, JUMP, CROUCH, ATTACK, HIT, BLOCK, KO }
enum class Level { HIGH, MID, LOW }
enum class Hat { BALD, HAIR, TOPI, TURBAN, CAP, BUN }

class Move(
    val name: String,
    val total: Int,
    val hitFrom: Int,
    val hitTo: Int,
    val dmg: Float,
    val reach: Float,
    val level: Level,
    val stun: Int,
    val knock: Float,
    val isGrab: Boolean = false,
    val isSuper: Boolean = false
)

object Moves {
    val P1 = Move("P1", 16, 4, 8, 5f, 95f, Level.HIGH, 13, 3f)
    val P2 = Move("P2", 16, 4, 8, 5f, 95f, Level.HIGH, 13, 3f)
    val P3 = Move("P3", 24, 7, 11, 9f, 105f, Level.HIGH, 22, 11f)
    val KICK = Move("KICK", 24, 8, 13, 8f, 115f, Level.MID, 18, 7f)
    val KICK2 = Move("KICK2", 26, 8, 13, 9f, 120f, Level.MID, 22, 10f)
    val SWEEP = Move("SWEEP", 30, 9, 15, 7f, 125f, Level.LOW, 30, 6f)
    val UPPER = Move("UPPER", 24, 6, 12, 7f, 85f, Level.MID, 26, 3f)
    val AIRKICK = Move("AIRKICK", 999, 0, 999, 7f, 95f, Level.MID, 18, 6f)
    val GRAB = Move("GRAB", 34, 7, 9, 12f, 80f, Level.MID, 36, 16f, isGrab = true)
    val SUPER = Move("SUPER", 54, 20, 21, 0f, 0f, Level.MID, 0, 0f, isSuper = true)
    val PUNCHES = arrayOf(P1, P2, P3)
}

class Look(
    val skin: Int,
    val outfit: Int,
    val pants: Int,
    val hair: Int,
    val hat: Hat,
    val glasses: Boolean = false,
    val stache: Int = 0,
    val beard: Boolean = false
)

class CharDef(
    val name: String,
    val tag: String,
    val speed: Float,
    val power: Float,
    val maxHp: Float,
    val defense: Float,
    val look: Look,
    val iconRes: String,
    val badge: String = ""
)

// All 13 original Indian Political Satire Characters
object Roster {
    val all = listOf(
        CharDef("MITRAMAN", "Balanced", 5.0f, 1.05f, 100f, 1.0f,
            Look(0xFFD9A066.toInt(), 0xFFF28C28.toInt(), 0xFFF5F0E6.toInt(), 0xFFE0E0E0.toInt(), Hat.BALD, glasses = true, beard = true),
            "face_mitraman", "1P"),
        CharDef("YATRA BOY", "Fast", 5.8f, 0.95f, 95f, 1.05f,
            Look(0xFFC68642.toInt(), 0xFFEFEFEF.toInt(), 0xFFEFEFEF.toInt(), 0xFF333333.toInt(), Hat.HAIR, beard = true),
            "face_yatraboy", "CPU"),
        CharDef("DEEDEE", "Power", 5.4f, 1.15f, 100f, 1.0f,
            Look(0xFFE0AC83.toInt(), 0xFF2563EB.toInt(), 0xFF1E3A8A.toInt(), 0xFF1A1A1A.toInt(), Hat.BUN, glasses = true),
            "face_deedee"),
        CharDef("TARIFF TAAU", "Heavy", 4.4f, 1.25f, 115f, 0.95f,
            Look(0xFFD9A066.toInt(), 0xFF1E3A8A.toInt(), 0xFF1E293B.toInt(), 0xFFD4AF37.toInt(), Hat.HAIR),
            "face_tarifftaau"),
        CharDef("DOZER DADA", "Defense", 4.2f, 1.1f, 125f, 0.85f,
            Look(0xFFD9A066.toInt(), 0xFFF28C28.toInt(), 0xFFF28C28.toInt(), 0xFF222222.toInt(), Hat.BALD),
            "face_dozerdada"),
        CharDef("TOLL MAN", "Special", 4.8f, 1.1f, 110f, 0.95f,
            Look(0xFFE0B48A.toInt(), 0xFF4A5568.toInt(), 0xFF2D3748.toInt(), 0xFF2B1B10.toInt(), Hat.HAIR, stache = 1),
            "face_tollman"),
        CharDef("SUTTI BABU", "Rushdown", 5.6f, 1.0f, 100f, 1.0f,
            Look(0xFFD2A679.toInt(), 0xFF7C3AED.toInt(), 0xFF1F2937.toInt(), 0xFF0F0F0F.toInt(), Hat.HAIR),
            "face_suttibabu"),
        CharDef("RAIL RAJA", "Speed", 6.2f, 0.9f, 95f, 1.1f,
            Look(0xFFC68642.toInt(), 0xFFD62839.toInt(), 0xFFF2E8CF.toInt(), 0xFFE0E0E0.toInt(), Hat.BALD, glasses = true),
            "face_railraja"),
        CharDef("GHAR MANTRI", "Tank", 4.0f, 1.2f, 130f, 0.85f,
            Look(0xFFB87A4B.toInt(), 0xFF8B5A2B.toInt(), 0xFF3E2A1A.toInt(), 0xFFE0E0E0.toInt(), Hat.BALD, glasses = true, beard = true),
            "face_gharmantri"),
        CharDef("FACT BHAI", "Combos", 5.3f, 1.05f, 100f, 1.0f,
            Look(0xFFCB9462.toInt(), 0xFF16A34A.toInt(), 0xFFE5E7EB.toInt(), 0xFF222222.toInt(), Hat.HAIR, glasses = true),
            "face_factbhai"),
        CharDef("MBAPU", "BOSS", 5.2f, 1.3f, 120f, 0.9f,
            Look(0xFFB87A4B.toInt(), 0xFFF5F0E6.toInt(), 0xFFF5F0E6.toInt(), 0xFF222222.toInt(), Hat.BALD, glasses = true, stache = 1),
            "face_mbapu", "BOSS"),
        CharDef("TRAILER", "CH. 2", 5.0f, 1.0f, 100f, 1.0f,
            Look(0xFFC68642.toInt(), 0xFF00ACC1.toInt(), 0xFF006064.toInt(), 0xFF111111.toInt(), Hat.HAIR),
            "face_trailer", "CH. 2"),
        CharDef("RANDOM", "?", 5.0f, 1.0f, 100f, 1.0f,
            Look(0xFFD9A066.toInt(), 0xFFE53935.toInt(), 0xFFB71C1C.toInt(), 0xFF222222.toInt(), Hat.TOPI),
            "face_random", "?")
    )
}

class Fighter(val def: CharDef, var x: Float, var facing: Int) {
    var y = GROUND
    var vx = 0f
    var vy = 0f
    var hp = def.maxHp
    var meter = 0f
    var st = St.IDLE
    var move: Move? = null
    var moveT = 0
    var hitDone = false
    var chain = 0
    var stun = 0
    var blocking = false
    var flash = 0
    var combo = 0
    var comboT = 0
    var wins = 0
    var walkPhase = 0f

    val onGround: Boolean get() = y >= GROUND
    val crouching: Boolean get() = st == St.CROUCH || move == Moves.SWEEP || move == Moves.UPPER

    fun reset(nx: Float, face: Int) {
        x = nx; facing = face; y = GROUND; vx = 0f; vy = 0f
        hp = def.maxHp; st = St.IDLE; move = null; moveT = 0; hitDone = false
        chain = 0; stun = 0; blocking = false; flash = 0; combo = 0; comboT = 0
    }
}

class Input {
    var left = false
    var right = false
    var up = false
    var down = false
    // edge-triggered (pressed this frame)
    var punch = false
    var kick = false
    var grab = false
    var sup = false

    fun clearEdges() { punch = false; kick = false; grab = false; sup = false }
    fun clearAll() { left = false; right = false; up = false; down = false; clearEdges() }
}

class Proj(var x: Float, val y: Float, val vx: Float, val owner: Fighter, val dmg: Float) {
    var life = 120
}

class Pop(var x: Float, var y: Float, val text: String, val color: Int) {
    var t = 40
}

// On-screen touch buttons: 0 left, 1 right, 2 jump, 3 crouch, 4 punch, 5 kick, 6 grab, 7 super
object Pad {
    val x = floatArrayOf(85f, 265f, 175f, 175f, 1130f, 1010f, 990f, 1150f)
    val y = floatArrayOf(610f, 610f, 495f, 672f, 575f, 645f, 520f, 440f)
    val r = floatArrayOf(62f, 62f, 58f, 44f, 72f, 62f, 50f, 55f)
    val label = arrayOf("", "", "", "", "PUNCH", "KICK", "GRAB", "SUPER")
}
