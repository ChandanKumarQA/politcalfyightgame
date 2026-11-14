package com.kursiclash.game

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

object FirebaseMultiplayer {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rnd = Random()
    private const val BASE_URL = "https://politicalfight-default-rtdb.firebaseio.com"
    @Volatile private var polling = false

    fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder()
        for (i in 0 until 4) sb.append(chars[rnd.nextInt(chars.length)])
        return sb.toString()
    }

    fun hostRoom(hero1: Int, stage: Int, callback: (code: String) -> Unit) {
        val code = generateCode()
        Thread {
            try {
                val json = JSONObject().apply {
                    put("code", code)
                    put("hostHero", hero1)
                    put("clientHero", -1)
                    put("stage", stage)
                    put("status", "waiting")
                    put("p1_input", 0)
                    put("p2_input", 0)
                    put("createdAt", System.currentTimeMillis())
                }
                val url = URL("$BASE_URL/rooms/$code.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {}
            mainHandler.post { callback(code) }
        }.start()
    }

    fun waitForGuest(code: String, onJoined: (clientHero: Int) -> Unit) {
        polling = true
        Thread {
            while (polling) {
                try {
                    val url = URL("$BASE_URL/rooms/$code.json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 3000
                        readTimeout = 3000
                    }
                    if (conn.responseCode in 200..299) {
                        val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        if (text.isNotEmpty() && text != "null") {
                            val obj = JSONObject(text)
                            val status = obj.optString("status", "")
                            val clientHero = obj.optInt("clientHero", -1)
                            if (status == "playing" && clientHero >= 0) {
                                polling = false
                                mainHandler.post { onJoined(clientHero) }
                                break
                            }
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
                try { Thread.sleep(1200) } catch (_: Exception) { break }
            }
        }.start()
    }

    fun joinRoom(code: String, clientHero: Int, callback: (success: Boolean, hostHero: Int, stage: Int) -> Unit) {
        Thread {
            var hostHero = 0
            var stage = 0
            try {
                val url = URL("$BASE_URL/rooms/$code.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                if (conn.responseCode in 200..299) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    if (text.isNotEmpty() && text != "null") {
                        val obj = JSONObject(text)
                        hostHero = obj.optInt("hostHero", 0)
                        stage = obj.optInt("stage", 0)

                        val patchUrl = URL("$BASE_URL/rooms/$code.json")
                        val patchConn = (patchUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "PATCH"
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                            connectTimeout = 4000
                            readTimeout = 4000
                        }
                        val patchJson = JSONObject().apply {
                            put("clientHero", clientHero)
                            put("status", "playing")
                        }
                        OutputStreamWriter(patchConn.outputStream).use { it.write(patchJson.toString()) }
                        patchConn.responseCode
                        patchConn.disconnect()
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}
            mainHandler.post { callback(true, hostHero, stage) }
        }.start()
    }

    fun stop() {
        polling = false
    }
}
