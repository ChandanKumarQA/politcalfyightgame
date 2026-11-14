package com.kursiclash.game

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.InputFilter
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.EditText
import android.widget.Toast
import kotlin.math.min

class GameView(private val ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {
    private val game = Game()
    private val renderer = Renderer(ctx, game)
    private var thread: Thread? = null
    @Volatile private var running = false
    private val prevHeld = BooleanArray(8)

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true

        // Wire up Game callbacks to Android Native APIs
        game.onShareWhatsApp = { code ->
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, "Battle me in Kursi Clash! Room code: $code\nJoin the fight and clash for the throne!")
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Battle me in Kursi Clash! Room code: $code")
                }
                ctx.startActivity(Intent.createChooser(share, "Share Room Code"))
            }
        }

        game.onCopyLink = { code ->
            try {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Kursi Clash Room Code", code))
                (ctx as? Activity)?.runOnUiThread {
                    Toast.makeText(ctx, "Room code $code copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        game.onInputCode = { callback ->
            (ctx as? Activity)?.runOnUiThread {
                val et = EditText(ctx).apply {
                    hint = "Enter 4-letter Room Code"
                    filters = arrayOf(InputFilter.LengthFilter(6), InputFilter.AllCaps())
                    gravity = Gravity.CENTER
                }
                AlertDialog.Builder(ctx)
                    .setTitle("Enter Friend's Room Code")
                    .setView(et)
                    .setPositiveButton("JOIN") { _, _ ->
                        val code = et.text.toString().trim().uppercase()
                        callback(code)
                    }
                    .setNegativeButton("CANCEL", null)
                    .show()
            }
        }

        game.onToast = { msg ->
            (ctx as? Activity)?.runOnUiThread {
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        running = true
        thread = Thread(this, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, ht: Int) {}

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        try { thread?.join() } catch (_: InterruptedException) {}
        thread = null
    }

    override fun run() {
        val step = 1_000_000_000.0 / 60.0
        var last = System.nanoTime()
        var acc = 0.0
        while (running) {
            val now = System.nanoTime()
            acc += (now - last).toDouble()
            last = now
            if (acc > step * 5) acc = step * 5
            while (acc >= step) {
                synchronized(game) { game.update() }
                acc -= step
            }
            val c = holder.lockCanvas()
            if (c == null) {
                try { Thread.sleep(8) } catch (_: InterruptedException) {}
                continue
            }
            try {
                synchronized(game) { renderer.draw(c) }
            } finally {
                holder.unlockCanvasAndPost(c)
            }
            try { Thread.sleep(4) } catch (_: InterruptedException) {}
        }
    }

    /** Returns true if the back press was handled inside the game. */
    fun onBack(): Boolean = synchronized(game) { game.onBack() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val vw = width.toFloat()
        val vh = height.toFloat()
        val sc = min(vw / W, vh / H)
        val ox = (vw - W * sc) / 2f
        val oy = (vh - H * sc) / 2f
        synchronized(game) {
            val inFight = (game.scr == Scr.FIGHT || game.scr == Scr.INTRO || game.scr == Scr.ROUNDEND) && !game.paused
            if (!inFight) {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    game.onTap((e.x - ox) / sc, (e.y - oy) / sc)
                }
                return true
            }

            // Check if pause button was tapped during fight
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                val px = (e.x - ox) / sc
                val py = (e.y - oy) / sc
                if (game.btnPause.contains(px, py)) {
                    game.paused = true
                    return true
                }
            }

            val held = BooleanArray(8)
            val ending = e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL
            val skip = if (e.actionMasked == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
            if (!ending) {
                for (i in 0 until e.pointerCount) {
                    if (i == skip) continue
                    val px = (e.getX(i) - ox) / sc
                    val py = (e.getY(i) - oy) / sc
                    for (b in 0 until 8) {
                        val dx = px - Pad.x[b]
                        val dy = py - Pad.y[b]
                        val rr = Pad.r[b] * 1.15f
                        if (dx * dx + dy * dy <= rr * rr) held[b] = true
                    }
                }
            }
            for (b in 0 until 8) {
                if (held[b] && !prevHeld[b]) game.padPressed(b)
                prevHeld[b] = held[b]
                game.heldTouch[b] = held[b]
            }
        }
        return true
    }

    // Keyboard support (handy on emulator, TV, or controller)
    private val keyHeld = BooleanArray(8)

    private fun keyIndex(k: Int): Int = when (k) {
        KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> 0
        KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> 1
        KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> 2
        KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> 3
        KeyEvent.KEYCODE_J -> 4
        KeyEvent.KEYCODE_K -> 5
        KeyEvent.KEYCODE_I -> 6
        KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_SPACE -> 7
        else -> -1
    }

    private fun key(k: Int, down: Boolean): Boolean {
        if (k == KeyEvent.KEYCODE_ESCAPE) {
            if (down) synchronized(game) { game.onBack() }
            return true
        }
        val b = keyIndex(k)
        if (b < 0) return false
        synchronized(game) {
            if (down && !keyHeld[b]) game.padPressed(b)
            keyHeld[b] = down
            game.heldKey[b] = down
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        key(keyCode, true) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        key(keyCode, false) || super.onKeyUp(keyCode, event)
}
