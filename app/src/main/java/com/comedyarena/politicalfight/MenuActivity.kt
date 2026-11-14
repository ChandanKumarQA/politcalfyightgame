package com.comedyarena.politicalfight

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

/** Animated cartoon caricature portrait of a hero */
class PortraitView(ctx: Context, val hero: Hero, private val bustOnly: Boolean = false) : View(ctx) {
    private val f = Fighter(hero, 0f, false).also { it.portrait = true; it.y = 0f }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        f.anim += 0.12f
        c.save()
        c.clipRect(0f, 0f, w, h)
        if (bustOnly) {
            // Focus on expressive caricature head & upper chest for selection cards
            val s = (h * 0.90f) / 160f
            c.translate(w / 2f, h / 2f + 175f * s)
            c.scale(s, s)
            f.draw(c, p)
        } else {
            // Full standing animated fighter for main menu
            val s = (h - 20f) / 260f
            c.translate(w / 2f, h - 10f)
            c.scale(s, s)
            f.draw(c, p)
        }
        c.restore()
        postInvalidateOnAnimation()
    }
}

class MenuActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var screenContainer: FrameLayout
    private var bannerAdView: com.google.android.gms.ads.AdView? = null
    private var screen = "home"
    private val heroes = Roster.heroes
    private var levelPage = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Prefs.init(this)
        AdManager.init(this)
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)

        // Render into cutout/notch area to avoid black bars on sides
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        root = FrameLayout(this)
        screenContainer = FrameLayout(this)
        screenContainer.setPadding(0, 0, 0, dp(50))
        root.addView(screenContainer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        try {
            val banner = AdManager.createBannerAd(this)
            bannerAdView = banner
            val bannerLp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            root.addView(banner, bannerLp)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContentView(root)
        fullscreen()
        showHome()
    }

    override fun onResume() {
        super.onResume(); fullscreen(); Sfx.startMusic()
        bannerAdView?.resume()
        AdManager.loadInterstitial(this)
        AdManager.loadRewarded(this)
        if (screen == "levels") showLevels()
    }
    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
        Sfx.stopMusic()
    }
    override fun onDestroy() {
        FirebaseMultiplayer.stopPolling()
        bannerAdView?.destroy()
        super.onDestroy()
    }
    @Suppress("DEPRECATION")
    override fun onBackPressed() { if (screen == "home") super.onBackPressed() else showHome() }

    private fun fullscreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // ------------------------------------------------------------ helpers
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun lp(w: Int, h: Int, weight: Float = 0f, m: Int = 0): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(w, h, weight); l.setMargins(dp(m), dp(m), dp(m), dp(m)); return l
    }
    private fun bgGrad() = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        intArrayOf(0xFF3E1228.toInt(), 0xFF6A1B4D.toInt(), 0xFFE65100.toInt()))

    private fun tv(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = true): TextView {
        val t = TextView(this)
        t.text = text; t.textSize = size; t.setTextColor(color); t.gravity = Gravity.CENTER
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun btn(text: String, color: Int, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text; b.setTextColor(Color.WHITE); b.textSize = 16f; b.isAllCaps = false; b.typeface = Typeface.DEFAULT_BOLD
        val gd = GradientDrawable(); gd.setColor(color); gd.cornerRadius = dp(20).toFloat(); gd.setStroke(dp(2), 0xFF1B1B1B.toInt())
        b.background = gd
        b.setOnClickListener { Sfx.click(); onClick() }
        return b
    }

    private fun show(v: View) {
        v.background = bgGrad()
        screenContainer.removeAllViews()
        screenContainer.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun start(mode: String, p1: Int, p2: Int, level: Int, arena: Int = Prefs.stage) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("mode", mode); i.putExtra("p1", p1); i.putExtra("p2", p2); i.putExtra("level", level); i.putExtra("arena", arena)
        startActivity(i)
    }

    private fun startOnlineMatch(code: String, p1Hero: Int, p2Hero: Int, stage: Int, isHost: Boolean) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("mode", "online")
        i.putExtra("p1", p1Hero)
        i.putExtra("p2", p2Hero)
        i.putExtra("level", 1)
        i.putExtra("arena", stage)
        i.putExtra("isHost", isHost)
        i.putExtra("roomCode", code)
        startActivity(i)
    }

    // ------------------------------------------------------------ screens
    private fun showHome() {
        screen = "home"
        val row = LinearLayout(this); row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.CENTER
        row.setPadding(dp(16), dp(8), dp(16), dp(8))
        val left = LinearLayout(this); left.orientation = LinearLayout.VERTICAL; left.gravity = Gravity.CENTER
        left.addView(tv("NETA FIGHT", 36f, 0xFFFFD700.toInt()))
        left.addView(tv("★ THE GREAT POWER BATTLE ★", 17f, 0xFFFFAB00.toInt()))
        val fr = LinearLayout(this); fr.gravity = Gravity.CENTER
        val a = Prefs.hero

        fun makeFighterCard(h: Hero, badgeText: String, badgeColor: Int, strokeColor: Int, onClick: () -> Unit): LinearLayout {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8), dp(6), dp(8), dp(8))
                val bg = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(0xFF221510.toInt())
                    setStroke(dp(3), strokeColor)
                }
                background = bg
                setOnClickListener { Sfx.click(); onClick() }
            }
            val b = TextView(this).apply {
                text = badgeText; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(badgeColor) }
                setPadding(dp(8), dp(2), dp(8), dp(2))
            }
            card.addView(b, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))
            card.addView(PortraitView(this, h, bustOnly = false), lp(dp(110), dp(135)))
            val name = tv(h.name, 12f, 0xFFFFEB3B.toInt())
            name.typeface = Typeface.DEFAULT_BOLD
            card.addView(name)
            return card
        }

        val p1Card = makeFighterCard(heroes[a], "1P", 0xFFE65100.toInt(), 0xFFFF9800.toInt()) {
            charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h, stg -> Prefs.hero = h; Prefs.stage = stg; showHome() }
        }
        val p2Card = makeFighterCard(heroes[(a + 1) % heroes.size], "CPU", 0xFF0288D1.toInt(), 0xFF03A9F4.toInt()) {
            charSelect("CHOOSE OPPONENT", (a + 1) % heroes.size) { _, _ -> showHome() }
        }
        fr.addView(p1Card, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6))
        fr.addView(tv("VS", 24f, 0xFFFFEB3B.toInt()))
        fr.addView(p2Card, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6))
        left.addView(fr)
        left.addView(tv("Best score: ${Prefs.best}   |   Levels: ${Prefs.unlocked}/500", 13f, Color.WHITE, false))
        left.addView(tv("All characters and events are fictional political parody.\nNo real person is depicted or promoted.", 10f, 0xCCFFFFFF.toInt(), false))

        val sv = ScrollView(this)
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL
        fun add(t: String, c: Int, a: () -> Unit) = col.addView(btn(t, c, a), lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(44), 0f, 3))
        add("🥊  PLAY GAME (VS CPU)", 0xFF43A047.toInt()) {
            charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h, stg -> Prefs.hero = h; Prefs.stage = stg; start("quick", h, -1, 1, stg) }
        }
        add("🌐  PLAY WITH A FRIEND (ONLINE)", 0xFF1976D2.toInt()) {
            showPlayWithFriend(Prefs.hero, Prefs.stage)
        }
        add("👥  LOCAL 2 PLAYER (SPLIT)", 0xFF1E88E5.toInt()) {
            charSelect("PLAYER 1: PICK A FIGHTER", 0) { h1, stg ->
                charSelect("PLAYER 2: PICK A FIGHTER", 1) { h2, _ -> start("two", h1, h2, 1, stg) }
            }
        }
        add("🏆  STORY MODE (500 LEVELS)", 0xFFEF6C00.toInt()) {
            charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h, stg -> Prefs.hero = h; Prefs.stage = stg; showLevels() }
        }
        add("🥋  CHARACTER COLLECTION", 0xFF6D4C41.toInt()) {
            charSelect("CHARACTER COLLECTION", Prefs.hero) { h, _ -> Prefs.hero = h; showHome() }
        }
        add("📖  HOW TO PLAY & COMBOS", 0xFF00897B.toInt()) { showHowToPlay() }
        add("⚙️  SETTINGS", 0xFF455A64.toInt()) { showSettings() }
        add("🚪  EXIT GAME", 0xFFD32F2F.toInt()) { finish() }
        sv.addView(col)

        row.addView(left, lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        row.addView(sv, lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        show(row)
    }

    private fun showHowToPlay() {
        screen = "how"
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(20), dp(10), dp(20), dp(10))
        col.addView(tv("HOW TO PLAY & COMBOS", 24f, 0xFFFFD700.toInt()))
        val tvControls = tv(
            "🕹️ CONTROLS:\n" +
            "• ◀ / ▶ : Move Left / Right across full arena\n" +
            "• JUMP : Jump in air\n" +
            "• BLOCK : Defend (takes 85% less damage)\n" +
            "• PUNCH : Rapid jab strike\n" +
            "• KICK : Heavy kick\n" +
            "• SPECIAL : Super power blast (ALWAYS ENABLED & READY!)\n\n" +
            "🌐 ONLINE MULTIPLAYER:\n" +
            "• Tap 'PLAY WITH A FRIEND' to host or join a room using a unique 4-letter room code (Firebase Realtime Database)!\n" +
            "• Share room code on WhatsApp with one click.\n\n" +
            "⚡ COMBOS:\n" +
            "• PUNCH + PUNCH + PUNCH : Triple Rapid Jab (Combo x3)\n" +
            "• PUNCH + PUNCH + KICK : Heavy Knockback Finish\n" +
            "• PUNCH + KICK + KICK : Flying Spin Combo\n" +
            "• KICK + KICK : Desi Double Kick\n" +
            "• JUMP + KICK : Flying Dropkick\n" +
            "• SPECIAL ATTACK : Maximum Cinematic Damage!",
            12f, Color.WHITE, false
        )
        tvControls.gravity = Gravity.LEFT
        col.addView(tvControls, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 6))
        col.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(160), dp(48), 0f, 4))
        show(col)
    }

    // ------------------------------------------------------------ Play With A Friend (Firebase)
    private fun showPlayWithFriend(heroId: Int, stageId: Int) {
        screen = "multiplayer"
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(20), dp(10), dp(20), dp(10))

        // Title: PLAY WITH A FRIEND in bold italicized gold font
        val title = tv("PLAY WITH A FRIEND", 28f, 0xFFFFB300.toInt())
        title.paint.isFakeBoldText = true
        col.addView(title, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 10))

        // Center Card (matching user screenshot media_1794657082692_4887fad4.jpg)
        val card = LinearLayout(this); card.orientation = LinearLayout.VERTICAL; card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(26), dp(22), dp(26), dp(22))
        val cardBg = GradientDrawable()
        cardBg.cornerRadius = dp(18).toFloat()
        cardBg.setColor(0xEE1E140F.toInt())
        cardBg.setStroke(dp(2), 0xFF5D4037.toInt())
        card.background = cardBg

        // Button: HOST A ROOM
        val hostBtn = Button(this)
        hostBtn.text = "HOST A ROOM"
        hostBtn.textSize = 17f
        hostBtn.setTextColor(Color.WHITE)
        hostBtn.typeface = Typeface.DEFAULT_BOLD
        val hostBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFFFB300.toInt(), 0xFFE65100.toInt()))
        hostBg.cornerRadius = dp(14).toFloat()
        hostBg.setStroke(dp(2), Color.WHITE)
        hostBtn.background = hostBg
        hostBtn.setOnClickListener {
            Sfx.click()
            FirebaseMultiplayer.hostRoom(heroId, stageId) { code, _ ->
                showHostRoomScreen(code ?: FirebaseMultiplayer.generateRoomCode(), heroId, stageId)
            }
        }
        card.addView(hostBtn, lp(dp(290), dp(52), 0f, 8))

        // Subtext: "or enter your friend's room code"
        val sub = tv("or enter your friend's room code", 13f, 0xFFD7CCC8.toInt(), false)
        card.addView(sub, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6))

        // Row: [ CODE ]  [ JOIN ]
        val joinRow = LinearLayout(this); joinRow.orientation = LinearLayout.HORIZONTAL; joinRow.gravity = Gravity.CENTER
        val etCode = EditText(this)
        etCode.hint = "CODE"
        etCode.setHintTextColor(0x88FFFFFF.toInt())
        etCode.setTextColor(Color.WHITE)
        etCode.textSize = 18f
        etCode.typeface = Typeface.DEFAULT_BOLD
        etCode.gravity = Gravity.CENTER
        etCode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        etCode.filters = arrayOf(InputFilter.LengthFilter(4), InputFilter.AllCaps())
        val etBg = GradientDrawable()
        etBg.setColor(0xFF140C08.toInt())
        etBg.cornerRadius = dp(12).toFloat()
        etBg.setStroke(dp(2), 0xFFBCAAA4.toInt())
        etCode.background = etBg
        joinRow.addView(etCode, lp(dp(130), dp(48), 0f, 4))

        val joinBtn = Button(this)
        joinBtn.text = "JOIN"
        joinBtn.textSize = 16f
        joinBtn.setTextColor(Color.WHITE)
        joinBtn.typeface = Typeface.DEFAULT_BOLD
        val joinBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFFF9800.toInt(), 0xFFE65100.toInt()))
        joinBg.cornerRadius = dp(12).toFloat()
        joinBg.setStroke(dp(2), Color.WHITE)
        joinBtn.background = joinBg
        joinBtn.setOnClickListener {
            val code = etCode.text.toString().trim().uppercase()
            if (code.length < 4) {
                Toast.makeText(this, "Please enter 4-letter room code!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Sfx.click()
            Toast.makeText(this, "Connecting to room $code...", Toast.LENGTH_SHORT).show()
            FirebaseMultiplayer.joinRoom(code, heroId) { success, hostHero, stg, _ ->
                startOnlineMatch(code, hostHero, heroId, stg, isHost = false)
            }
        }
        joinRow.addView(joinBtn, lp(dp(110), dp(48), 0f, 4))

        card.addView(joinRow, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6))

        col.addView(card, lp(dp(380), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 10))

        // Back Button
        val backBtn = Button(this)
        backBtn.text = "BACK"
        backBtn.textSize = 14f
        backBtn.setTextColor(Color.WHITE)
        backBtn.typeface = Typeface.DEFAULT_BOLD
        val bBg = GradientDrawable()
        bBg.setColor(0xFF2E1C14.toInt())
        bBg.cornerRadius = dp(12).toFloat()
        bBg.setStroke(dp(2), 0xFFFFB300.toInt())
        backBtn.background = bBg
        backBtn.setOnClickListener { Sfx.click(); showHome() }
        col.addView(backBtn, lp(dp(130), dp(40), 0f, 6))

        show(col)
    }

    private fun showHostRoomScreen(code: String, hostHero: Int, stageId: Int) {
        screen = "host_room"
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(20), dp(10), dp(20), dp(10))

        // Title: PLAY WITH A FRIEND
        val title = tv("PLAY WITH A FRIEND", 28f, 0xFFFFB300.toInt())
        title.paint.isFakeBoldText = true
        col.addView(title, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 8))

        // Center Card (matching user screenshot media_1794657089565_428fcf5e.jpg)
        val card = LinearLayout(this); card.orientation = LinearLayout.VERTICAL; card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(28), dp(20), dp(28), dp(20))
        val cardBg = GradientDrawable()
        cardBg.cornerRadius = dp(18).toFloat()
        cardBg.setColor(0xEE1E140F.toInt())
        cardBg.setStroke(dp(2), 0xFF5D4037.toInt())
        card.background = cardBg

        // Header: ROOM CODE
        val rc = tv("ROOM CODE", 13f, 0xFFB0BEC5.toInt())
        card.addView(rc, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

        // Large Room Code: F 5 V Q
        val spacedCode = code.toCharArray().joinToString(" ")
        val codeTv = tv(spacedCode, 42f, Color.WHITE)
        codeTv.letterSpacing = 0.2f
        codeTv.paint.isFakeBoldText = true
        card.addView(codeTv, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 8))

        // Action Buttons Row: [ Share on WhatsApp ]  [ Copy link ]
        val btnRow = LinearLayout(this); btnRow.orientation = LinearLayout.HORIZONTAL; btnRow.gravity = Gravity.CENTER

        // Share on WhatsApp Button (Green)
        val waBtn = Button(this)
        waBtn.text = "Share on WhatsApp"
        waBtn.textSize = 14f
        waBtn.setTextColor(Color.WHITE)
        waBtn.typeface = Typeface.DEFAULT_BOLD
        val waBg = GradientDrawable()
        waBg.setColor(0xFF25D366.toInt()) // WhatsApp Brand Green
        waBg.cornerRadius = dp(10).toFloat()
        waBtn.background = waBg
        waBtn.setOnClickListener {
            Sfx.click()
            val shareMsg = "Hey! Challenge me in NETA FIGHT: The Great Power Battle! My Room Code is: $code. Open the app, tap 'Play with a Friend', and join!"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareMsg)
                setPackage("com.whatsapp")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                }, "Share Room Code")
                startActivity(chooser)
            }
        }
        btnRow.addView(waBtn, lp(dp(170), dp(44), 0f, 4))

        // Copy Link / Code Button
        val cpBtn = Button(this)
        cpBtn.text = "Copy link"
        cpBtn.textSize = 14f
        cpBtn.setTextColor(Color.WHITE)
        cpBtn.typeface = Typeface.DEFAULT_BOLD
        val cpBg = GradientDrawable()
        cpBg.setColor(0xFF1E140F.toInt())
        cpBg.cornerRadius = dp(10).toFloat()
        cpBg.setStroke(dp(2), 0xFFFFB300.toInt())
        cpBtn.background = cpBg
        cpBtn.setOnClickListener {
            Sfx.click()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Neta Fight Room Code", code))
            Toast.makeText(this, "Room Code $code copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
        btnRow.addView(cpBtn, lp(dp(110), dp(44), 0f, 4))

        card.addView(btnRow, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 8))

        // Status Text: ● Waiting for your friend to join...
        val statusTv = tv("● Waiting for your friend to join...", 13f, 0xFFFFB300.toInt(), false)
        card.addView(statusTv, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))

        // Quick simulation test button for instant single-device test
        val testBtn = tv("⚡ Test Friend Join (Simulate)", 11f, 0xFF81D4FA.toInt(), false)
        testBtn.setPadding(dp(4), dp(4), dp(4), dp(4))
        testBtn.setOnClickListener {
            Sfx.click()
            FirebaseMultiplayer.stopPolling()
            val clientHero = (hostHero + 1) % heroes.size
            startOnlineMatch(code, hostHero, clientHero, stageId, isHost = true)
        }
        card.addView(testBtn, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

        col.addView(card, lp(dp(380), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 10))

        // Back Button
        val backBtn = Button(this)
        backBtn.text = "BACK"
        backBtn.textSize = 14f
        backBtn.setTextColor(Color.WHITE)
        backBtn.typeface = Typeface.DEFAULT_BOLD
        val bBg = GradientDrawable()
        bBg.setColor(0xFF2E1C14.toInt())
        bBg.cornerRadius = dp(12).toFloat()
        bBg.setStroke(dp(2), 0xFFFFB300.toInt())
        backBtn.background = bBg
        backBtn.setOnClickListener {
            Sfx.click()
            FirebaseMultiplayer.stopPolling()
            showPlayWithFriend(hostHero, stageId)
        }
        col.addView(backBtn, lp(dp(130), dp(40), 0f, 6))

        show(col)

        // Start Firebase real-time listener waiting for friend to join
        FirebaseMultiplayer.waitForGuest(code) { clientHero ->
            runOnUiThread {
                Toast.makeText(this, "Friend joined! Starting battle...", Toast.LENGTH_SHORT).show()
                startOnlineMatch(code, hostHero, clientHero, stageId, isHost = true)
            }
        }
    }

    // ------------------------------------------------------------ Character Select (with CPU & Play with Friend buttons)
    private fun charSelect(title: String, init: Int, onPick: (heroId: Int, stageId: Int) -> Unit) {
        screen = "chars"
        var sel = init.coerceIn(0, heroes.size - 1)
        var selectedStage = Prefs.stage.coerceIn(0, 2)
        var cpuDiffIndex = when (Prefs.cpuDiff) {
            0.3f -> 0
            0.85f -> 2
            else -> 1
        }

        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(12), dp(4), dp(12), dp(4))

        // Title: PICK YOUR NETA
        col.addView(tv("PICK YOUR NETA", 22f, 0xFFFFB300.toInt()))

        val hs = HorizontalScrollView(this)
        val rowL = LinearLayout(this); rowL.orientation = LinearLayout.HORIZONTAL; hs.addView(rowL)
        val cards = ArrayList<LinearLayout>()

        fun refreshCards() {
            cards.forEachIndexed { i, cd ->
                val g = GradientDrawable(); g.cornerRadius = dp(14).toFloat()
                if (i == sel) {
                    g.setColor(0xFF381F19.toInt())
                    g.setStroke(dp(3), 0xFFFF9800.toInt()) // Gold glowing border
                } else {
                    g.setColor(0xFF221510.toInt())
                    g.setStroke(dp(2), 0xFF4A342A.toInt())
                }
                cd.background = g
                if (cd.childCount >= 3 && cd.getChildAt(2) is TextView) {
                    val tv = cd.getChildAt(2) as TextView
                    tv.setTextColor(if (i == sel) 0xFFFFEB3B.toInt() else Color.WHITE)
                    tv.typeface = if (i == sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                }
            }
        }

        // Render Cards for each Neta
        for ((idx, h) in heroes.withIndex()) {
            val cd = LinearLayout(this); cd.orientation = LinearLayout.VERTICAL; cd.gravity = Gravity.CENTER_HORIZONTAL
            cd.setPadding(dp(6), dp(6), dp(6), dp(6))

            // Badge on top ("1P", "CPU", "BOSS", etc.)
            val badge = TextView(this)
            badge.textSize = 10f; badge.typeface = Typeface.DEFAULT_BOLD; badge.gravity = Gravity.CENTER
            badge.setTextColor(Color.WHITE)
            val badgeBg = GradientDrawable(); badgeBg.cornerRadius = dp(6).toFloat()
            when {
                idx == 0 -> { badge.text = "1P"; badgeBg.setColor(0xFFE65100.toInt()) }
                idx == 1 -> { badge.text = "CPU"; badgeBg.setColor(0xFF0288D1.toInt()) }
                idx == 10 -> { badge.text = "BOSS"; badgeBg.setColor(0xFFFFB300.toInt()); badge.setTextColor(0xFF212121.toInt()) }
                else -> { badge.text = "NETA"; badgeBg.setColor(0xFF4527A0.toInt()) }
            }
            badge.background = badgeBg
            badge.setPadding(dp(8), dp(2), dp(8), dp(2))
            cd.addView(badge, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

            // Animated Cartoon Caricature Portrait (Vector, Not Photo!)
            val pv = PortraitView(this, h, bustOnly = true)
            cd.addView(pv, lp(dp(95), dp(115)))

            // Name bar at bottom of card
            val nameTv = tv(h.name, 12f, if (idx == sel) 0xFFFFEB3B.toInt() else Color.WHITE)
            nameTv.typeface = if (idx == sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            cd.addView(nameTv)

            cd.setOnClickListener {
                sel = idx
                Sfx.click()
                refreshCards()
            }
            cards.add(cd)
            rowL.addView(cd, lp(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))
        }

        // Add "TRAILER (CH. 2)" teaser card
        val trailerCd = LinearLayout(this); trailerCd.orientation = LinearLayout.VERTICAL; trailerCd.gravity = Gravity.CENTER_HORIZONTAL
        trailerCd.setPadding(dp(6), dp(6), dp(6), dp(6))
        val trBadge = TextView(this); trBadge.textSize = 10f; trBadge.typeface = Typeface.DEFAULT_BOLD; trBadge.gravity = Gravity.CENTER
        trBadge.text = "CH. 2"; trBadge.setTextColor(Color.WHITE)
        val trBg = GradientDrawable(); trBg.cornerRadius = dp(6).toFloat(); trBg.setColor(0xFF00ACC1.toInt()); trBadge.background = trBg
        trBadge.setPadding(dp(8), dp(2), dp(8), dp(2)); trailerCd.addView(trBadge, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

        val trIcon = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(tv("🎬", 34f))
            addView(tv("COMING SOON", 10f, 0xFF00E5FF.toInt()))
        }
        trailerCd.addView(trIcon, lp(dp(95), dp(115)))
        trailerCd.addView(tv("TRAILER", 12f, 0xFF00E5FF.toInt()))
        val gTr = GradientDrawable(); gTr.cornerRadius = dp(14).toFloat(); gTr.setColor(0xFF1B2428.toInt()); gTr.setStroke(dp(2), 0xFF00ACC1.toInt())
        trailerCd.background = gTr
        trailerCd.setOnClickListener {
            Sfx.click()
            Toast.makeText(this, "Chapter 2 coming soon in next update!", Toast.LENGTH_SHORT).show()
        }
        rowL.addView(trailerCd, lp(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))

        // Add "RANDOM (?)" card
        val rndCd = LinearLayout(this); rndCd.orientation = LinearLayout.VERTICAL; rndCd.gravity = Gravity.CENTER_HORIZONTAL
        rndCd.setPadding(dp(6), dp(6), dp(6), dp(6))
        val rndBadge = TextView(this); rndBadge.textSize = 10f; rndBadge.typeface = Typeface.DEFAULT_BOLD; rndBadge.gravity = Gravity.CENTER
        rndBadge.text = "?"; rndBadge.setTextColor(Color.WHITE)
        val rndBg = GradientDrawable(); rndBg.cornerRadius = dp(6).toFloat(); rndBg.setColor(0xFFE53935.toInt()); rndBadge.background = rndBg
        rndBadge.setPadding(dp(8), dp(2), dp(8), dp(2)); rndCd.addView(rndBadge, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

        val rndIcon = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(tv("🎲", 36f))
            addView(tv("SURPRISE", 10f, 0xFFFFCA28.toInt()))
        }
        rndCd.addView(rndIcon, lp(dp(95), dp(115)))
        rndCd.addView(tv("RANDOM", 12f, 0xFFFFCA28.toInt()))
        val gRnd = GradientDrawable(); gRnd.cornerRadius = dp(14).toFloat(); gRnd.setColor(0xFF281E1B.toInt()); gRnd.setStroke(dp(2), 0xFFE53935.toInt())
        rndCd.background = gRnd
        rndCd.setOnClickListener {
            sel = (0 until heroes.size).random()
            Sfx.click()
            refreshCards()
        }
        rowL.addView(rndCd, lp(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))

        refreshCards()
        col.addView(hs, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // STAGE Selection Row: [ SANSAD ] [ JANTAR MANTAR ] [ DESI SADAK ]
        val stageRow = LinearLayout(this); stageRow.orientation = LinearLayout.HORIZONTAL; stageRow.gravity = Gravity.CENTER
        stageRow.setPadding(dp(4), dp(2), dp(4), dp(2))
        stageRow.addView(tv("STAGE:", 13f, 0xFFFFEB3B.toInt()), lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))

        val stageNames = listOf("SANSAD", "JANTAR MANTAR", "DESI SADAK")
        val stageBtns = ArrayList<Button>()
        fun updateStageBtns() {
            stageBtns.forEachIndexed { i, b ->
                val bg = GradientDrawable(); bg.cornerRadius = dp(12).toFloat()
                if (i == selectedStage) {
                    bg.setColor(0xFFFF9800.toInt())
                    bg.setStroke(dp(2), Color.WHITE)
                    b.setTextColor(Color.WHITE)
                } else {
                    bg.setColor(0xFF241712.toInt())
                    bg.setStroke(dp(2), 0xFF5D4037.toInt())
                    b.setTextColor(0xFFBCAAA4.toInt())
                }
                b.background = bg
            }
        }
        for (i in stageNames.indices) {
            val sb = Button(this); sb.text = stageNames[i]; sb.textSize = 12f; sb.typeface = Typeface.DEFAULT_BOLD; sb.isAllCaps = false
            sb.setPadding(dp(14), dp(4), dp(14), dp(4))
            sb.setOnClickListener {
                selectedStage = i
                Prefs.stage = i
                Sfx.click()
                updateStageBtns()
            }
            stageBtns.add(sb)
            stageRow.addView(sb, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34), 0f, 4))
        }
        updateStageBtns()
        col.addView(stageRow)

        // CPU Difficulty Selector Row (matching user screenshot media_1794656265411_ac757dae.jpg)
        val cpuRow = LinearLayout(this); cpuRow.orientation = LinearLayout.HORIZONTAL; cpuRow.gravity = Gravity.CENTER
        cpuRow.setPadding(dp(4), dp(2), dp(4), dp(2))
        cpuRow.addView(tv("CPU", 13f, 0xFFFFB300.toInt()), lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6))

        val diffNames = listOf("EASY", "NORMAL", "HARD")
        val diffVals = listOf(0.3f, 0.5f, 0.85f)
        val diffBtns = ArrayList<Button>()
        fun updateDiffBtns() {
            diffBtns.forEachIndexed { i, b ->
                val bg = GradientDrawable(); bg.cornerRadius = dp(10).toFloat()
                if (i == cpuDiffIndex) {
                    bg.setColor(0xFFFF9800.toInt())
                    bg.setStroke(dp(2), Color.WHITE)
                    b.setTextColor(Color.WHITE)
                } else {
                    bg.setColor(0xFF1E140F.toInt())
                    bg.setStroke(dp(2), 0xFF5D4037.toInt())
                    b.setTextColor(0xFFBCAAA4.toInt())
                }
                b.background = bg
            }
        }
        for (i in diffNames.indices) {
            val db = Button(this); db.text = diffNames[i]; db.textSize = 11f; db.typeface = Typeface.DEFAULT_BOLD; db.isAllCaps = false
            db.setPadding(dp(12), dp(2), dp(12), dp(2))
            db.setOnClickListener {
                cpuDiffIndex = i
                Prefs.cpuDiff = diffVals[i]
                Sfx.click()
                updateDiffBtns()
            }
            diffBtns.add(db)
            cpuRow.addView(db, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30), 0f, 3))
        }
        updateDiffBtns()
        col.addView(cpuRow)

        // Bottom Action Buttons (matching user screenshot media_1794656265411_ac757dae.jpg)
        // [ 🌐 PLAY WITH A FRIEND ]   and   [ FIGHT! ]
        val bottom = LinearLayout(this); bottom.orientation = LinearLayout.HORIZONTAL; bottom.gravity = Gravity.CENTER
        bottom.setPadding(dp(4), dp(2), dp(4), dp(2))

        // Play With A Friend (Blue Button)
        val friendBtn = Button(this)
        friendBtn.text = "🌐 PLAY WITH A FRIEND"
        friendBtn.textSize = 15f
        friendBtn.setTextColor(Color.WHITE)
        friendBtn.typeface = Typeface.DEFAULT_BOLD
        val frBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF1E88E5.toInt(), 0xFF1565C0.toInt()))
        frBg.cornerRadius = dp(14).toFloat()
        frBg.setStroke(dp(2), Color.WHITE)
        friendBtn.background = frBg
        friendBtn.setOnClickListener {
            Sfx.click()
            showPlayWithFriend(sel, selectedStage)
        }
        bottom.addView(friendBtn, lp(dp(220), dp(48), 0f, 6))

        // FIGHT! (Orange Gradient Button)
        val fightBtn = Button(this)
        fightBtn.text = "FIGHT!"
        fightBtn.textSize = 18f
        fightBtn.setTextColor(Color.WHITE)
        fightBtn.typeface = Typeface.DEFAULT_BOLD
        val fBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFFFB300.toInt(), 0xFFE65100.toInt()))
        fBg.cornerRadius = dp(14).toFloat()
        fBg.setStroke(dp(2), Color.WHITE)
        fightBtn.background = fBg
        fightBtn.setOnClickListener {
            Sfx.click()
            onPick(sel, selectedStage)
        }
        bottom.addView(fightBtn, lp(dp(200), dp(48), 0f, 6))

        col.addView(bottom)

        show(col)
    }

    private fun showLevels() {
        screen = "levels"
        val hero = heroes[Prefs.hero]
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(14), dp(6), dp(14), dp(6))

        col.addView(tv("STORY MODE · ${hero.name}", 22f, 0xFFFFD700.toInt()))

        // Level Navigation: 500 Levels supported!
        val navRow = LinearLayout(this); navRow.gravity = Gravity.CENTER
        val prevBtn = btn("◀ PREV 10", 0xFF546E7A.toInt()) {
            if (levelPage > 0) { levelPage--; showLevels() }
        }
        val startLvl = levelPage * 10 + 1
        val endLvl = min(500, (levelPage + 1) * 10)
        val pageText = tv("LEVELS $startLvl - $endLvl (OF 500)", 14f, 0xFFFFEB3B.toInt())
        val nextBtn = btn("NEXT 10 ▶", 0xFF546E7A.toInt()) {
            if (levelPage < 49) { levelPage++; showLevels() }
        }
        navRow.addView(prevBtn, lp(dp(110), dp(36), 0f, 4))
        navRow.addView(pageText, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 8))
        navRow.addView(nextBtn, lp(dp(110), dp(36), 0f, 4))
        col.addView(navRow, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

        // Quick Jump Row: Level 1, 100, 250, 500
        val jumpRow = LinearLayout(this); jumpRow.gravity = Gravity.CENTER
        fun addJump(label: String, targetPage: Int) {
            val jb = btn(label, 0xFF4527A0.toInt()) { levelPage = targetPage; showLevels() }
            jb.textSize = 11f
            jumpRow.addView(jb, lp(dp(85), dp(32), 0f, 3))
        }
        addJump("LVL 1", 0)
        addJump("LVL 100", 9)
        addJump("LVL 250", 24)
        addJump("LVL 500 🚀", 49)
        col.addView(jumpRow, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))

        // Display 10 level buttons on current page in 2 rows of 5
        for (r in 0 until 2) {
            val row = LinearLayout(this); row.gravity = Gravity.CENTER
            for (k in 0 until 5) {
                val n = levelPage * 10 + r * 5 + k + 1
                if (n > 500) break
                val lv = Levels.getLevel(n)
                val open = n <= Prefs.unlocked
                val label = if (open) "$n\n${lv.title}\n${lv.sub}" else "🔒 $n\nLocked"
                val b = btn(label, if (open) 0xFF43A047.toInt() else 0xFF607D8B.toInt()) {
                    if (open) start("story", hero.id, -1, n, lv.arena)
                }
                b.textSize = 10f
                b.setPadding(dp(4), dp(4), dp(4), dp(4))
                b.maxLines = 4
                row.addView(b, lp(0, dp(80), 1f, 3))
            }
            col.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        // Bottom Controls
        val bottom = LinearLayout(this); bottom.gravity = Gravity.CENTER
        bottom.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(120), dp(44), 0f, 4))
        bottom.addView(btn("CHANGE FIGHTER", 0xFFEF6C00.toInt()) {
            charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h, stg -> Prefs.hero = h; Prefs.stage = stg; showLevels() }
        }, lp(dp(160), dp(44), 0f, 4))

        // Unlock All 500 Levels button
        bottom.addView(btn("🔓 UNLOCK ALL (500)", 0xFFE65100.toInt()) {
            Prefs.unlocked = 500
            showLevels()
        }, lp(dp(190), dp(44), 0f, 4))
        col.addView(bottom)

        val sv = ScrollView(this)
        sv.addView(col)
        show(sv)
    }

    private fun showSettings() {
        screen = "settings"
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(60), dp(10), dp(60), dp(10))
        col.addView(tv("SETTINGS", 26f, 0xFFFFD700.toInt()))
        val snd = btn("", 0xFF43A047.toInt()) {}
        val mus = btn("", 0xFF43A047.toInt()) {}
        fun upd() { snd.text = "SOUND EFFECTS: " + if (Prefs.sound) "ON" else "OFF"; mus.text = "MUSIC: " + if (Prefs.music) "ON" else "OFF" }
        snd.setOnClickListener { Prefs.sound = !Prefs.sound; upd() }
        mus.setOnClickListener { Prefs.music = !Prefs.music; if (Prefs.music) Sfx.startMusic() else Sfx.stopMusic(); upd() }
        upd()
        col.addView(snd, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 4))
        col.addView(mus, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 4))
        col.addView(btn("FIREBASE URL: " + Prefs.firebaseUrl, 0xFF1976D2.toInt()) {
            Toast.makeText(this, "Firebase RTDB: ${Prefs.firebaseUrl}", Toast.LENGTH_LONG).show()
        }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 4))
        col.addView(btn("UNLOCK ALL 500 LEVELS", 0xFFEF6C00.toInt()) { Prefs.unlocked = 500; showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 4))
        col.addView(btn("RESET PROGRESS", 0xFFE53935.toInt()) { Prefs.reset(); showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 4))
        col.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 4))
        show(col)
    }
}
