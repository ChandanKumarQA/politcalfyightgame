package com.comedyarena.politicalfight

/** All characters, parties and events are FICTIONAL parody creations. */
data class Hero(
    val id: Int, val name: String, val party: String, val partyId: Int,
    val color: Int, val skin: Int, val hat: Int,
    val power: Int, val speed: Int, val jump: Int,
    val ability: Int, val abilityText: String,
    val specialName: String, val specialLabel: String, val specialColor: Int,
    val intro: String, val shout: String, val win: String, val lose: String
)

object Roster {
    val parties = listOf("Team Vaada", "Team Yatra", "Trina Sena", "MAGA Morcha", "Bhagwa Front", "Gati Morcha", "Lal Paltan", "Chanakya Dal", "Digital Sabha", "Royal Morcha")
    val heroes = listOf(
        Hero(0, "Mitra Man", parties[0], 0, 0xFFE65100.toInt(), 0xFFE0A070.toInt(), 0, 8, 7, 6,
            1, "Balanced: +10% power, spinning flaming chakra", "Mitra Chakra", "CHAKRA!", 0xFFFF9800.toInt(),
            "Mitrooon! The verdict of the arena is absolute!", "MITRA CHAKRA BLAST!", "Victory is guaranteed for the people!", "I demand a recount of the punches!"),

        Hero(1, "Yatra Boy", parties[1], 1, 0xFF1976D2.toInt(), 0xFFE8B88A.toInt(), 1, 7, 9, 8,
            4, "Speed Demon: charges Shakti 40% faster", "Aloo Se Sona", "SONA!", 0xFFFFD700.toInt(),
            "This is a 4,000 kilometer uninterrupted combo!", "ALOO SE SONA!", "Love wins over hate in this arena!", "The EVM was rigged!"),

        Hero(2, "Deedee Queen", parties[2], 2, 0xFF2E7D32.toInt(), 0xFFD99B66.toInt(), 2, 8, 8, 7,
            7, "High Kicker: extra reach on kicks & dropkick", "Khela Hobe Blast", "KHELA HOBE!", 0xFF00E676.toInt(),
            "Khela hobe, and the match is mine!", "KHELA HOBE BLAST!", "Clean sweep! Now go home!", "TMC will protest this decision!"),

        Hero(3, "Tariff Tau", parties[3], 3, 0xFFD50000.toInt(), 0xFFF2B58D.toInt(), 3, 10, 4, 4,
            5, "Heavyweight: 20% higher damage, heavy armor", "Wall Slam", "WALL SLAM!", 0xFF6D4C41.toInt(),
            "Nobody builds combos better than me, believe me!", "TREMENDOUS WALL SLAM!", "Huge victory! The biggest in history!", "This fight was totally fraudulent!"),

        Hero(4, "Dozer Dada", parties[4], 4, 0xFFFF6D00.toInt(), 0xFFDEA070.toInt(), 4, 9, 5, 5,
            3, "Iron Tank: +30% defense & heavy knockback", "Bulldozer Charge", "BULLDOZER!", 0xFFFFEB3B.toInt(),
            "All illegal attacks will be demolished on sight!", "BULLDOZER CHARGE!", "Law and order restored in the ring!", "Encounter with defeat... temporary!"),

        Hero(5, "Toll Master", parties[5], 5, 0xFF5D4037.toInt(), 0xFFE2A878.toInt(), 5, 7, 7, 6,
            2, "Fastag Rush: recovers health on clean hit", "Fastag Blitz", "FASTAG!", 0xFF00ACC1.toInt(),
            "No fighter passes through without paying the toll!", "SUPERSONIC FASTAG!", "Expressway to victory unlocked!", "Toll barrier malfunctioned..."),

        Hero(6, "Rail Raja", parties[6], 6, 0xFF33691E.toInt(), 0xFFBC7E4C.toInt(), 6, 8, 8, 5,
            6, "Acrobatic: super fast low sweeps & punches", "Choo Choo Express", "CHOO CHOO!", 0xFFE53935.toInt(),
            "Samast janta ka dhyan akarsheet ho!", "CHOO CHOO EXPRESS!", "Platform number one par victory train!", "Chain pulling kisne ki?!"),

        Hero(7, "Ghar Mantri", parties[7], 7, 0xFF37474F.toInt(), 0xFFDFA070.toInt(), 7, 8, 6, 6,
            5, "Iron Wall: blocks counter-damage to opponent", "Master Plan Shield", "MASTER PLAN!", 0xFF90A4AE.toInt(),
            "Everything is proceeding according to the master plan.", "MASTER PLAN SHIELD!", "Aap chronology samajhiye!", "Samay aane par badla lenge."),

        Hero(8, "Fact Bhai", parties[8], 8, 0xFF00838F.toInt(), 0xFFE8B88A.toInt(), 8, 7, 9, 8,
            0, "Combo Master: rapid strikes with zero delay", "Fact Check Laser", "FACT CHECK!", 0xFF22D3EE.toInt(),
            "Your claims have been fact-checked and debunked!", "TRUTH LASER BEAM!", "Zero misinformation, 100% win!", "The server went down!"),

        Hero(9, "Chai Boss", parties[9], 9, 0xFFFF6F00.toInt(), 0xFFDCA272.toInt(), 9, 10, 8, 7,
            1, "Kadak King: giant boiling kettle blast", "Kadak Chai Splash", "KADAK CHAI!", 0xFFFFD700.toInt(),
            "Only the strongest can handle the asli kadak swaad!", "KADAK CHAI ERUPTION!", "Garam chai, thanda opponent!", "Chai gir gayi... rematch!")
    )
}

data class Level(val title: String, val sub: String, val opp: Int, val arena: Int, val diff: Float, val hpMul: Float)

object Levels {
    val list = listOf(
        Level("Sansad Rumble", "vs Yatra Boy", 1, 0, 0.30f, 0.85f),
        Level("Didi's Challenge", "vs Deedee Queen", 2, 1, 0.40f, 0.90f),
        Level("Tariff Showdown", "vs Tariff Tau", 3, 2, 0.48f, 1.0f),
        Level("Bulldozer Battle", "vs Dozer Dada", 4, 1, 0.55f, 1.05f),
        Level("Highway Clash", "vs Toll Master", 5, 2, 0.62f, 1.1f),
        Level("Railway Rumble", "vs Rail Raja", 6, 0, 0.68f, 1.15f),
        Level("Chanakya Duel", "vs Ghar Mantri", 7, 0, 0.75f, 1.25f),
        Level("Fact Check Arena", "vs Fact Bhai", 8, 2, 0.82f, 1.35f),
        Level("Mitra Man Showdown", "vs Mitra Man", 0, 0, 0.90f, 1.5f),
        Level("Ultimate Throne", "Final Boss: Chai Boss", 9, 0, 1.0f, 2.0f)
    )
}
