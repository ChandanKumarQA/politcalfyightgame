package com.comedyarena.politicalfight

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during fights
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Notch / Display Cutout Full Screen support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Initialize Ads
        AdManager.init(this)
        AdManager.loadInterstitial(this)

        // Setup Hardware-Accelerated WebView
        webView = WebView(this).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    applyImmersiveMode()
                }
            }

            addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")
            loadUrl("file:///android_asset/game/index.html")
        }

        setContentView(webView)
        applyImmersiveMode()
    }

    private fun applyImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Send pause or back event to web game
        webView.evaluateJavascript("if (window.dispatchEvent) { window.dispatchEvent(new KeyboardEvent('keydown', {'key': 'Escape'})); }", null)
    }

    // --- JavaScript to Native Android Bridge ---
    class WebAppInterface(private val activity: MainActivity) {

        @JavascriptInterface
        fun vibrate(type: String) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val vib = vm.defaultVibrator
                    val ms = if (type == "heavy") 120L else 35L
                    vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vib = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    val ms = if (type == "heavy") 120L else 35L
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(ms)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JavascriptInterface
        fun showAd() {
            activity.runOnUiThread {
                AdManager.showInterstitial(activity) {}
            }
        }

        @JavascriptInterface
        fun shareRoomCode(code: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Battle me in Neta Fight!")
                putExtra(Intent.EXTRA_TEXT, "Join my 3D arena match in Neta Fight using Room Code: $code\nGet ready for the great power battle!")
            }
            activity.startActivity(Intent.createChooser(intent, "Share Room Code"))
        }

        @JavascriptInterface
        fun exitApp() {
            activity.runOnUiThread {
                activity.finish()
            }
        }
    }
}
