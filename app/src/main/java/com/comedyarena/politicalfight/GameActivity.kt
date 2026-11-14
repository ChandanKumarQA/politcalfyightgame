package com.comedyarena.politicalfight

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class GameActivity : Activity() {
    private lateinit var gv: GameView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Prefs.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Render into cutout/notch area to avoid black bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val cfg = Cfg(
            intent.getStringExtra("mode") ?: "quick",
            intent.getIntExtra("p1", 0), intent.getIntExtra("p2", 1), intent.getIntExtra("level", 1)
        )
        AdManager.loadInterstitial(this)
        gv = GameView(this, cfg) {
            AdManager.showInterstitial(this) { finish() }
        }
        setContentView(gv)
        immersive()
    }

    private fun immersive() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onResume() { super.onResume(); immersive(); Sfx.startMusic() }
    override fun onPause() { super.onPause(); Sfx.stopMusic(); gv.autoPause() }
    @Suppress("DEPRECATION")
    override fun onBackPressed() { gv.togglePause() }
}
