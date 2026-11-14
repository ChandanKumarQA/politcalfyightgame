package com.comedyarena.politicalfight

data class Hero(
    val id: Int, val name: String, val party: String, val partyId: Int,
    val primaryColor: Int, val skinColor: Int, val hairStyle: Int,
    val power: Int, val speed: Int, val jump: Int,
    val ability: Int, val abilityText: String,
    val specialName: String, val specialLabel: String, val specialColor: Int,
    val intro: String, val shout: String, val win: String, val lose: String,
    val drawableName: String
) {
    val color: Int get() = primaryColor
    val skin: Int get() = skinColor
}

object Roster {
    val parties = listOf(
        "Team Vaada", "Team Yatra", "Trina Sena", "MAGA Morcha", "Bhagwa Front",
        "Gati Morcha", "Sutti Dal", "Lal Paltan", "Chanakya Dal", "Digital Sabha", "Ahimsa Front"
    )

    val heroes = listOf(
        Hero(0, "MITRAMAN", parties[0], 0, 0xFFE65100.toInt(), 0xFFE0A070.toInt(), 0, 8, 7, 6,
            1, "Balanced: +10% power, spinning flaming chakra", "Mitra Chakra", "CHAKRA!", 0xFFFF9800.toInt(),
            "Mitrooon! The verdict of the arena is absolute!", "MITRA CHAKRA BLAST!", "Victory is guaranteed for the people!", "I demand a recount of the punches!",
            "face_mitraman"),

        Hero(1, "YATRA BOY", parties[1], 1, 0xFF1976D2.toInt(), 0xFFE8B88A.toInt(), 1, 7, 9, 8,
            4, "Speed Demon: charges Shakti 40% faster", "Aloo Se Sona", "SONA!", 0xFFFFD700.toInt(),
            "This is a 4,000 kilometer uninterrupted combo!", "ALOO SE SONA!", "Love wins over hate in this arena!", "The EVM was rigged!",
            "face_yatraboy"),

        Hero(2, "DEEDEE", parties[2], 2, 0xFF2E7D32.toInt(), 0xFFD99B66.toInt(), 2, 8, 8, 7,
            7, "High Kicker: extra reach on kicks & dropkick", "Khela Hobe Blast", "KHELA HOBE!", 0xFF00E676.toInt(),
            "Khela hobe, and the match is mine!", "KHELA HOBE BLAST!", "Clean sweep! Now go home!", "TMC will protest this decision!",
            "face_deedee"),

        Hero(3, "TARIFF TAAU", parties[3], 3, 0xFFD50000.toInt(), 0xFFF2B58D.toInt(), 3, 10, 4, 4,
            5, "Heavyweight: 20% higher damage, heavy armor", "Wall Slam", "WALL SLAM!", 0xFF6D4C41.toInt(),
            "Nobody builds combos better than me, believe me!", "TREMENDOUS WALL SLAM!", "Huge victory! The biggest in history!", "This fight was totally fraudulent!",
            "face_tarifftaau"),

        Hero(4, "DOZER DADA", parties[4], 4, 0xFFFF6D00.toInt(), 0xFFDEA070.toInt(), 4, 9, 5, 5,
            3, "Iron Tank: +30% defense & heavy knockback", "Bulldozer Charge", "BULLDOZER!", 0xFFFFEB3B.toInt(),
            "All illegal attacks will be demolished on sight!", "BULLDOZER CHARGE!", "Law and order restored in the ring!", "Encounter with defeat... temporary!",
            "face_dozerdada"),

        Hero(5, "TOLL MAN", parties[5], 5, 0xFF5D4037.toInt(), 0xFFE2A878.toInt(), 5, 7, 7, 6,
            2, "Fastag Rush: recovers health on clean hit", "Fastag Blitz", "FASTAG!", 0xFF00ACC1.toInt(),
            "No fighter passes through without paying the toll!", "SUPERSONIC FASTAG!", "Expressway to victory unlocked!", "Toll barrier malfunctioned...",
            "face_tollman"),

        Hero(6, "SUTTI BABU", parties[6], 6, 0xFF6A1B9A.toInt(), 0xFFD7A074.toInt(), 1, 7, 8, 7,
            0, "Chill Combo: unpredictable rapid strikes", "Vacation Strike", "SUTTI!", 0xFFAB47BC.toInt(),
            "Time for a long vacation... after I win this!", "SUTTI STRIKE!", "Off to London for victory party!", "Taking sick leave from fighting...",
            "face_suttibabu"),

        Hero(7, "RAIL RAJA", parties[7], 7, 0xFF33691E.toInt(), 0xFFBC7E4C.toInt(), 6, 8, 8, 5,
            6, "Acrobatic: super fast low sweeps & punches", "Choo Choo Express", "CHOO CHOO!", 0xFFE53935.toInt(),
            "Samast janta ka dhyan akarsheet ho!", "CHOO CHOO EXPRESS!", "Platform number one par victory train!", "Chain pulling kisne ki?!",
            "face_railraja"),

        Hero(8, "GHAR MANTRI", parties[8], 8, 0xFF37474F.toInt(), 0xFFDFA070.toInt(), 7, 8, 6, 6,
            5, "Iron Wall: blocks counter-damage to opponent", "Master Plan Shield", "MASTER PLAN!", 0xFF90A4AE.toInt(),
            "Everything is proceeding according to the master plan.", "MASTER PLAN SHIELD!", "Aap chronology samajhiye!", "Samay aane par badla lenge.",
            "face_gharmantri"),

        Hero(9, "FACT BHAI", parties[9], 9, 0xFF00838F.toInt(), 0xFFE8B88A.toInt(), 8, 7, 9, 8,
            0, "Combo Master: rapid strikes with zero delay", "Fact Check Laser", "FACT CHECK!", 0xFF22D3EE.toInt(),
            "Your claims have been fact-checked and debunked!", "TRUTH LASER BEAM!", "Zero misinformation, 100% win!", "The server went down!",
            "face_factbhai"),

        Hero(10, "MBAPU", parties[10], 10, 0xFFFFD700.toInt(), 0xFFDCA272.toInt(), 9, 10, 8, 9,
            3, "Ahimsa Force: supreme ancient combat shakti", "Satyagraha Shock", "SATYA!", 0xFFFFD700.toInt(),
            "An eye for an eye ends the match!", "SATYAGRAHA SHOCKWAVE!", "Truth triumphs over everything!", "Keep walking...",
            "face_mbapu")
    )
}

data class Level(val title: String, val sub: String, val opp: Int, val arena: Int, val diff: Float, val hpMul: Float)

object Levels {
    const val MAX_LEVELS = 500

    private val baseLevels = listOf(
        Level("Sansad Rumble", "vs YATRA BOY", 1, 0, 0.30f, 0.85f),
        Level("Didi's Challenge", "vs DEEDEE", 2, 1, 0.40f, 0.90f),
        Level("Tariff Showdown", "vs TARIFF TAAU", 3, 2, 0.48f, 1.0f),
        Level("Bulldozer Battle", "vs DOZER DADA", 4, 1, 0.55f, 1.05f),
        Level("Highway Clash", "vs TOLL MAN", 5, 2, 0.62f, 1.1f),
        Level("Sutti Faceoff", "vs SUTTI BABU", 6, 2, 0.66f, 1.15f),
        Level("Railway Rumble", "vs RAIL RAJA", 7, 0, 0.70f, 1.2f),
        Level("Chanakya Duel", "vs GHAR MANTRI", 8, 0, 0.78f, 1.3f),
        Level("Fact Check Arena", "vs FACT BHAI", 9, 2, 0.85f, 1.4f),
        Level("Ultimate Throne", "Final Boss: MBAPU", 10, 0, 1.0f, 2.0f)
    )

    fun getLevel(n: Int): Level {
        val num = n.coerceIn(1, MAX_LEVELS)
        if (num <= baseLevels.size) return baseLevels[num - 1]
        val heroCount = Roster.heroes.size
        val opp = (num - 1) % heroCount
        val arena = ((num - 1) / 3) % 3
        val oppHero = Roster.heroes[opp]
        val progress = num.toFloat() / MAX_LEVELS
        val diff = (0.45f + progress * 0.55f).coerceIn(0.45f, 1.0f)
        val hpMul = (1.0f + progress * 1.5f).coerceIn(1.0f, 2.5f)
        val isBoss = (num % 10 == 0)
        val title = if (isBoss) "BOSS LEVEL $num" else "LEVEL $num BATTLE"
        val sub = if (isBoss) "★ BOSS: ${oppHero.name} ★" else "vs ${oppHero.name}"
        return Level(title, sub, opp, arena, diff, if (isBoss) hpMul * 1.25f else hpMul)
    }
}
