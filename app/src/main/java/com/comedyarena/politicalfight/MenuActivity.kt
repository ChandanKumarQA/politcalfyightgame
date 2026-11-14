package com.comedyarena.politicalfight

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Animated cartoon portrait of a hero, drawn with the same code used in battle. */
class PortraitView(ctx: Context, hero: Hero) : View(ctx) {
    private val f = Fighter(hero, 0f, false).also { it.portrait = true; it.y = 0f }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val s = height / 300f
        f.anim += 0.15f
        c.save(); c.translate(width / 2f, height - 10f); c.scale(s, s)
        f.draw(c, p)
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
        intArrayOf(0xFF4A148C.toInt(), 0xFFE91E63.toInt(), 0xFFFF9800.toInt()))

    private fun tv(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = true): TextView {
        val t = TextView(this)
        t.text = text; t.textSize = size; t.setTextColor(color); t.gravity = Gravity.CENTER
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun btn(text: String, color: Int, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text; b.setTextColor(Color.WHITE); b.textSize = 17f; b.isAllCaps = false; b.typeface = Typeface.DEFAULT_BOLD
        val gd = GradientDrawable(); gd.setColor(color); gd.cornerRadius = dp(24).toFloat(); gd.setStroke(dp(3), 0xFF222222.toInt())
        b.background = gd
        b.setOnClickListener { Sfx.click(); onClick() }
        return b
    }

    private fun show(v: View) {
        v.background = bgGrad()
        screenContainer.removeAllViews()
        screenContainer.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun start(mode: String, p1: Int, p2: Int, level: Int) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("mode", mode); i.putExtra("p1", p1); i.putExtra("p2", p2); i.putExtra("level", level)
        startActivity(i)
    }

    // ------------------------------------------------------------ screens
    private fun showHome() {
        screen = "home"
        val row = LinearLayout(this); row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.CENTER
        row.setPadding(dp(16), dp(8), dp(16), dp(8))
        val left = LinearLayout(this); left.orientation = LinearLayout.VERTICAL; left.gravity = Gravity.CENTER
        left.addView(tv("NETA FIGHT", 36f))
        left.addView(tv("★ THE GREAT POWER BATTLE ★", 18f, 0xFFFFEB3B.toInt()))
        val fr = LinearLayout(this); fr.gravity = Gravity.CENTER
        val a = Prefs.hero
        fr.addView(PortraitView(this, heroes[a]), lp(dp(130), dp(170)))
        fr.addView(tv("VS", 26f, 0xFFFFEB3B.toInt()))
        fr.addView(PortraitView(this, heroes[(a + 1) % heroes.size]), lp(dp(130), dp(170)))
        left.addView(fr)
        left.addView(tv("Best score: ${Prefs.best}   |   Levels unlocked: ${Prefs.unlocked}/10", 13f, Color.WHITE, false))
        left.addView(tv("All characters, parties and events are fictional parody.\nNo real person or party is depicted or promoted.", 10f, 0xCCFFFFFF.toInt(), false))
        val sv = ScrollView(this)
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL
        fun add(t: String, c: Int, a: () -> Unit) = col.addView(btn(t, c, a), lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 3))
        add("🥊  PLAY GAME (VS CPU)", 0xFF43A047.toInt()) { charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h -> Prefs.hero = h; start("quick", h, -1, 1) } }
        add("👥  PLAY WITH FRIEND (2P)", 0xFF1E88E5.toInt()) {
            charSelect("PLAYER 1: PICK A FIGHTER", 0) { h1 -> charSelect("PLAYER 2: PICK A FIGHTER", 1) { h2 -> start("two", h1, h2, 1) } }
        }
        add("🏆  STORY MODE LEVELS", 0xFFEF6C00.toInt()) { charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h -> Prefs.hero = h; showLevels() } }
        add("🥋  CHARACTER COLLECTION", 0xFF6D4C41.toInt()) { charSelect("CHARACTER COLLECTION", Prefs.hero) { h -> Prefs.hero = h; showHome() } }
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
            "• ◀ / ▶ : Move Left / Right\n" +
            "• JUMP : Jump in air\n" +
            "• BLOCK : Defend (takes 85% less damage, charges Shakti)\n" +
            "• PUNCH : Rapid jab strike\n" +
            "• KICK : Heavy kick\n" +
            "• SPECIAL : Super power attack (Shakti blast!)\n\n" +
            "🔥 COMBOS:\n" +
            "• PUNCH + PUNCH + PUNCH : Triple Rapid Jab\n" +
            "• PUNCH + PUNCH + KICK : Heavy Knockback Finish\n" +
            "• PUNCH + KICK + KICK : Flying Spin Combo\n" +
            "• KICK + KICK : Desi Double Kick\n" +
            "• JUMP + KICK : Flying Dropkick\n" +
            "• SPECIAL AT 100% : Maximum Cinematic Damage!",
            12f, Color.WHITE, false
        )
        tvControls.gravity = Gravity.LEFT
        col.addView(tvControls, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f, 6))
        col.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(160), dp(48), 0f, 4))
        show(col)
    }

    private fun stat(label: String, v: Int) = label + " " + "█".repeat(v) + "░".repeat(10 - v)

    private fun charSelect(title: String, init: Int, onPick: (Int) -> Unit) {
        screen = "chars"
        var sel = init
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(12), dp(8), dp(12), dp(8))
        col.addView(tv(title, 22f))
        val hs = HorizontalScrollView(this)
        val rowL = LinearLayout(this); rowL.orientation = LinearLayout.HORIZONTAL; hs.addView(rowL)
        val cards = ArrayList<LinearLayout>()
        fun refresh() {
            cards.forEachIndexed { i, cd ->
                val g = GradientDrawable(); g.cornerRadius = dp(18).toFloat()
                g.setColor(if (i == sel) 0xFFFFF176.toInt() else 0xFFFFFDE7.toInt())
                g.setStroke(dp(if (i == sel) 5 else 2), if (i == sel) 0xFFFF6F00.toInt() else 0xFF333333.toInt())
                cd.background = g
            }
        }
        val dark = 0xFF212121.toInt()
        for (h in heroes) {
            val cd = LinearLayout(this); cd.orientation = LinearLayout.VERTICAL; cd.gravity = Gravity.CENTER_HORIZONTAL
            cd.setPadding(dp(10), dp(8), dp(10), dp(8))
            cd.addView(PortraitView(this, h), lp(dp(150), dp(150)))
            cd.addView(tv(h.name, 15f, dark))
            cd.addView(tv(h.party, 12f, 0xFF6A1B9A.toInt()))
            val st = tv(stat("POWER", h.power) + "\n" + stat("SPEED", h.speed) + "\n" + stat("JUMP ", h.jump), 10f, dark, false)
            st.typeface = Typeface.MONOSPACE; cd.addView(st)
            cd.addView(tv("★ " + h.specialName, 11f, 0xFFD84315.toInt()))
            val ab = tv(h.abilityText, 10f, 0xFF424242.toInt(), false); ab.maxWidth = dp(170); cd.addView(ab)
            cd.setOnClickListener { sel = h.id; Sfx.click(); refresh() }
            cards.add(cd)
            rowL.addView(cd, lp(dp(185), ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 5))
        }
        refresh()
        col.addView(hs, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val bottom = LinearLayout(this); bottom.gravity = Gravity.CENTER
        bottom.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(160), dp(50), 0f, 6))
        bottom.addView(btn("SELECT ✔", 0xFF43A047.toInt()) { onPick(sel) }, lp(dp(200), dp(50), 0f, 6))
        col.addView(bottom)
        show(col)
    }

    private fun partyPick() {
        screen = "party"
        fun pick(title: String, exclude: Int, done: (Int) -> Unit) {
            val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
            col.setPadding(dp(40), dp(10), dp(40), dp(10))
            col.addView(tv(title, 24f))
            val colors = intArrayOf(0xFFD32F2F.toInt(), 0xFFF9A825.toInt(), 0xFF039BE5.toInt(), 0xFF7E57C2.toInt())
            for (i in Roster.parties.indices) if (i != exclude) {
                val names = heroes.filter { it.partyId == i }.joinToString(" & ") { it.name }
                col.addView(btn("${Roster.parties[i]}\n$names", colors[i]) { done(i) }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(64), 0f, 4))
            }
            col.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(160), dp(48), 0f, 4))
            show(col)
        }
        pick("YOUR PARTY", -1) { mine ->
            pick("OPPONENT PARTY", mine) { theirs ->
                val a = heroes.filter { it.partyId == mine }.random()
                val b = heroes.filter { it.partyId == theirs }.random()
                Prefs.hero = a.id
                start("party", a.id, b.id, 1)
                showHome()
            }
        }
    }

    private fun showLevels() {
        screen = "levels"
        val hero = heroes[Prefs.hero]
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(16), dp(8), dp(16), dp(8))
        col.addView(tv("STORY MODE · ${hero.name}", 22f))
        for (r in 0 until 2) {
            val row = LinearLayout(this); row.gravity = Gravity.CENTER
            for (k in 0 until 5) {
                val n = r * 5 + k + 1
                val lv = Levels.list[n - 1]
                val open = n <= Prefs.unlocked
                val b = btn(if (open) "$n\n${lv.title}\n${lv.sub}" else "🔒 $n\nLocked", if (open) 0xFF43A047.toInt() else 0xFF607D8B.toInt()) {
                    if (open) start("story", hero.id, -1, n)
                }
                b.textSize = 10f
                b.setPadding(dp(4), dp(4), dp(4), dp(4))
                b.ellipsize = null
                b.maxLines = 5
                row.addView(b, lp(0, dp(140), 1f, 4))
            }
            col.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val bottom = LinearLayout(this); bottom.gravity = Gravity.CENTER
        bottom.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(dp(130), dp(48), 0f, 4))
        bottom.addView(btn("CHANGE FIGHTER", 0xFFEF6C00.toInt()) {
            charSelect("CHOOSE YOUR FIGHTER", Prefs.hero) { h -> Prefs.hero = h; showLevels() }
        }, lp(dp(180), dp(48), 0f, 4))
        if (Prefs.unlocked < 10) {
            bottom.addView(btn("🎁 UNLOCK NEXT LEVEL (AD)", 0xFFE65100.toInt()) {
                AdManager.showRewarded(this, onRewardEarned = {
                    Prefs.unlocked += 1
                    showLevels()
                })
            }, lp(dp(220), dp(48), 0f, 4))
        }
        col.addView(bottom)
        val sv = ScrollView(this)
        sv.addView(col)
        show(sv)
    }

    private fun showSettings() {
        screen = "settings"
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER
        col.setPadding(dp(80), dp(10), dp(80), dp(10))
        col.addView(tv("SETTINGS", 26f))
        val snd = btn("", 0xFF43A047.toInt()) {}
        val mus = btn("", 0xFF43A047.toInt()) {}
        fun upd() { snd.text = "SOUND EFFECTS: " + if (Prefs.sound) "ON" else "OFF"; mus.text = "MUSIC: " + if (Prefs.music) "ON" else "OFF" }
        snd.setOnClickListener { Prefs.sound = !Prefs.sound; upd() }
        mus.setOnClickListener { Prefs.music = !Prefs.music; if (Prefs.music) Sfx.startMusic() else Sfx.stopMusic(); upd() }
        upd()
        col.addView(snd, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0f, 5))
        col.addView(mus, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0f, 5))
        col.addView(btn("RESET PROGRESS", 0xFFE53935.toInt()) { Prefs.reset(); showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0f, 5))
        col.addView(btn("◀ BACK", 0xFF546E7A.toInt()) { showHome() }, lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0f, 5))
        show(col)
    }
}
