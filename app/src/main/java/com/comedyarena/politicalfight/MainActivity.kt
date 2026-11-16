package com.comedyarena.politicalfight

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.comedyarena.politicalfight.game3d.Audio
import com.comedyarena.politicalfight.game3d.GlRenderer
import com.comedyarena.politicalfight.game3d.UiView
import com.comedyarena.politicalfight.game3d.core.Engine

/**
 * Pure native Kotlin 3D OpenGL ES 2.0 Activity for Neta Fight.
 * Uses GLSurfaceView (3D scene) + hardware-accelerated Canvas UiView + synthesized AudioTrack sound.
 * Zero WebView / HTML dependency.
 */
class MainActivity : Activity() {
    private lateinit var gl: GLSurfaceView
    private lateinit var ui: UiView
    private lateinit var engine: Engine
    private val audio = Audio()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Initialize Ads
        AdManager.init(this)
        AdManager.loadInterstitial(this)

        ui = UiView(this, audio)
        engine = Engine(ui)
        ui.attach(engine)

        // Hook AdMob Interstitial on match/level completion
        ui.onMatchEnd = {
            runOnUiThread {
                AdManager.showInterstitial(this@MainActivity) {}
            }
        }

        gl = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(GlRenderer(this@MainActivity, engine))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        val root = FrameLayout(this)
        root.addView(gl, FrameLayout.LayoutParams(-1, -1))
        root.addView(ui, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        hideBars()
    }

    private fun hideBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onResume() {
        super.onResume()
        gl.onResume()
        audio.resume()
    }

    override fun onPause() {
        ui.onHostPause()
        audio.pause()
        gl.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        audio.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!ui.back()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode != KeyEvent.KEYCODE_BACK && e.keyCode != KeyEvent.KEYCODE_VOLUME_UP && e.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (ui.key(e.keyCode, e.action == KeyEvent.ACTION_DOWN, e.repeatCount > 0)) return true
        }
        return super.dispatchKeyEvent(e)
    }
}
