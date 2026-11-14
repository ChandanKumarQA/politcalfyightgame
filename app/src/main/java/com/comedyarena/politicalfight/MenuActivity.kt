package com.comedyarena.politicalfight

import android.app.Activity
import android.content.Intent
import android.content.Context
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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Animated or picture portrait of a hero */
class PortraitView(ctx: Context, val hero: Hero) : View(ctx) {
    private val f = Fighter(hero, 0f, false).also { it.portrait = true; it.y = 0f }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bmp: Bitmap? = null

    init {
        try {
            val resId = resources.getIdentifier(hero.drawableName, "drawable", context.packageName)
            if (resId != 0) {
                bmp = BitmapFactory.decodeResource(resources, resId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDraw(c: Canvas) {
        val b = bmp
        if (b != null) {
            val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
            c.drawBitmap(b, null, dst, p)
        } else {
            val s = height / 300f
            f.anim += 0.15f
            c.save(); c.translate(width / 2f, height - 10f); c.scale(s, s)
            f.draw(c, p)
            c.restore()
            postInvalidateOnAnimation()
        }
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
        fr.addView(PortraitView(this, heroes[a]), lp(dp(125), dp(160)))
        fr.addView(tv("VS", 24f, 0xFFFFEB3B.toInt()))
        fr.addView(PortraitView(this, heroes[(a + 1) % heroes.size]), lp(dp(125), dp(160)))
        left.addView(fr)
        left.addView(tv("Best score: ${Prefs.best}   |   Levels: ${Prefs.unlocked}/500", 13f, Color.WHITE, false))
        left.addView(tv("All characters and events are fictional political parody.\nNo real person is depicted or promoted.", 10f, 0xCCFFFFFF.toInt(), false))

        val sv = ScrollView(this)
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL
        fun add(t: String, c: Int, a: () -> Unit) = col.addView(btn(t, c, a), lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0f, 3))
        add("🥊  PLAY GAME (VS CPU)", 0xFF43A047.toInt()) {
            charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h, stg -> Prefs.hero = h; Prefs.stage = stg; start("quick", h, -1, 1, stg) }
        }
        add("👥  PLAY WITH FRIEND (2P)", 0xFF1E88E5.toInt()) {
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
        add("⚙️  SETTINGS", 0xFF546E7A.toInt()) { showSettings() }
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
            "🔥 SPECIAL ATTACK:\n" +
            "• Tap the glowing SPECIAL button at ANY time during the fight to unleash your character's ultimate Shakti power move!\n\n" +
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

    private fun stat(label: String, v: Int) = label + " " + "█".repeat(v) + "░".repeat(10 - v)

    private fun charSelect(title: String, init: Int, onPick: (heroId: Int, stageId: Int) -> Unit) {
        screen = "chars"
        var sel = init.coerceIn(0, heroes.size - 1)
        var selectedStage = Prefs.stage.coerceIn(0, 2)

        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(12), dp(6), dp(12), dp(6))

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
                    g.setStroke(dp(4), 0xFFFF9800.toInt()) // Gold glowing border
                } else {
                    g.setColor(0xFF221510.toInt())
                    g.setStroke(dp(2), 0xFF4A342A.toInt())
                }
                cd.background = g
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

            // Character Card Image / Portrait
            val iv = ImageView(this)
            val resId = resources.getIdentifier(h.drawableName, "drawable", packageName)
            if (resId != 0) {
                iv.setImageResource(resId)
                iv.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            cd.addView(iv, lp(dp(95), dp(115)))

            // Name bar at bottom of card
            val nameTv = tv(h.name, 12f, if (idx == sel) 0xFFFFEB3B.toInt() else Color.WHITE)
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
        val trIv = ImageView(this); val trRes = resources.getIdentifier("face_trailer", "drawable", packageName)
        if (trRes != 0) trIv.setImageResource(trRes)
        trailerCd.addView(trIv, lp(dp(95), dp(115)))
        trailerCd.addView(tv("TRAILER", 12f, 0xFF00E5FF.toInt()))
        val gTr = GradientDrawable(); gTr.cornerRadius = dp(14).toFloat(); gTr.setColor(0xFF1B2428.toInt()); gTr.setStroke(dp(2), 0xFF00ACC1.toInt())
        trailerCd.background = gTr
        trailerCd.setOnClickListener {
            Sfx.click()
            tv("Chapter 2 coming soon!", 14f)
        }
        rowL.addView(trailerCd, lp(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))

        // Add "RANDOM (?)" card
        val rndCd = LinearLayout(this); rndCd.orientation = LinearLayout.VERTICAL; rndCd.gravity = Gravity.CENTER_HORIZONTAL
        rndCd.setPadding(dp(6), dp(6), dp(6), dp(6))
        val rndBadge = TextView(this); rndBadge.textSize = 10f; rndBadge.typeface = Typeface.DEFAULT_BOLD; rndBadge.gravity = Gravity.CENTER
        rndBadge.text = "?"; rndBadge.setTextColor(Color.WHITE)
        val rndBg = GradientDrawable(); rndBg.cornerRadius = dp(6).toFloat(); rndBg.setColor(0xFFE53935.toInt()); rndBadge.background = rndBg
        rndBadge.setPadding(dp(8), dp(2), dp(8), dp(2)); rndCd.addView(rndBadge, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 2))
        val rndIv = ImageView(this); val rndRes = resources.getIdentifier("face_random", "drawable", packageName)
        if (rndRes != 0) rndIv.setImageResource(rndRes)
        rndCd.addView(rndIv, lp(dp(95), dp(115)))
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
        stageRow.setPadding(dp(4), dp(4), dp(4), dp(4))
        stageRow.addView(tv("STAGE:", 14f, 0xFFFFEB3B.toInt()), lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 4))

        val stageNames = listOf("SANSAD", "JANTAR MANTAR", "DESI SADAK")
        val stageBtns = ArrayList<Button>()
        fun updateStageBtns() {
            stageBtns.forEachIndexed { i, b ->
                val bg = GradientDrawable(); bg.cornerRadius = dp(14).toFloat()
                if (i == selectedStage) {
                    bg.setColor(0xFFFF9800.toInt()) // Highlighted orange like reference image
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
            val sb = Button(this); sb.text = stageNames[i]; sb.textSize = 13f; sb.typeface = Typeface.DEFAULT_BOLD; sb.isAllCaps = false
            sb.setPadding(dp(16), dp(6), dp(16), dp(6))
            sb.setOnClickListener {
                selectedStage = i
                Prefs.stage = i
                Sfx.click()
                updateStageBtns()
            }
            stageBtns.add(sb)
            stageRow.addView(sb, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38), 0f, 4))
        }
        updateStageBtns()
        col.addView(stageRow)

        // Bottom Action Buttons
        val bottom = LinearLayout(this); bottom.gravity = Gravity.CENTER
        bottom.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(150), dp(46), 0f, 6))
        bottom.addView(btn("SELECT & FIGHT 🥊", 0xFF43A047.toInt()) { onPick(sel, selectedStage) }, lp(dp(210), dp(46), 0f, 6))
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
        col.setPadding(dp(80), dp(10), dp(80), dp(10))
        col.addView(tv("SETTINGS", 26f, 0xFFFFD700.toInt()))
        val snd = btn("", 0xFF43A047.toInt()) {}
        val mus = btn("", 0xFF43A047.toInt()) {}
        fun upd() { snd.text = "SOUND EFFECTS: " + if (Prefs.sound) "ON" else "OFF"; mus.text = "MUSIC: " + if (Prefs.music) "ON" else "OFF" }
        snd.setOnClickListener { Prefs.sound = !Prefs.sound; upd() }
        mus.setOnClickListener { Prefs.music = !Prefs.music; if (Prefs.music) Sfx.startMusic() else Sfx.stopMusic(); upd() }
        upd()
        col.addView(snd, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 4))
        col.addView(mus, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 4))
        col.addView(btn("UNLOCK ALL 500 LEVELS", 0xFFEF6C00.toInt()) { Prefs.unlocked = 500; showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 4))
        col.addView(btn("RESET PROGRESS", 0xFFE53935.toInt()) { Prefs.reset(); showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 4))
        col.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 4))
        show(col)
    }
}
