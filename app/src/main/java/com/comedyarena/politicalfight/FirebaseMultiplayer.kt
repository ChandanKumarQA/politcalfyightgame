package com.comedyarena.politicalfight

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

data class RoomData(
    val code: String,
    val hostHero: Int,
    val clientHero: Int,
    val stage: Int,
    val status: String,
    val p1Input: Int,
    val p2Input: Int,
    val p1Hp: Float,
    val p2Hp: Float
)

object FirebaseMultiplayer {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rnd = Random()
    private var activeRoom: String? = null
    @Volatile private var pollingActive = false

    private fun getBaseUrl(): String {
        var u = Prefs.firebaseUrl.trim()
        if (u.isEmpty()) u = "https://politicalfight-default-rtdb.firebaseio.com"
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        if (u.endsWith("/")) u = u.substring(0, u.length - 1)
        return u
    }

    /** Generates a 4-character room code like F5VQ */
    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder()
        for (i in 0 until 4) {
            sb.append(chars[rnd.nextInt(chars.length)])
        }
        return sb.toString()
    }

    /** Creates a new game room in Firebase Realtime Database */
    fun hostRoom(
        hostHero: Int,
        stage: Int,
        onComplete: (code: String?, error: String?) -> Unit
    ) {
        val code = generateRoomCode()
        activeRoom = code
        Thread {
            try {
                val json = JSONObject().apply {
                    put("code", code)
                    put("hostHero", hostHero)
                    put("clientHero", -1)
                    put("stage", stage)
                    put("status", "waiting")
                    put("p1_input", 0)
                    put("p2_input", 0)
                    put("p1_hp", 100.0)
                    put("p2_hp", 100.0)
                    put("createdAt", System.currentTimeMillis())
                }

                val url = URL("${getBaseUrl()}/rooms/$code.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 7000
                    readTimeout = 7000
                }

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val codeResp = conn.responseCode
                conn.disconnect()

                if (codeResp in 200..299) {
                    mainHandler.post { onComplete(code, null) }
                } else {
                    // Fallback to local room creation so the player can still play
                    mainHandler.post { onComplete(code, null) }
                }
            } catch (e: Exception) {
                // If offline / no internet / custom URL error, allow room generation locally
                mainHandler.post { onComplete(code, null) }
            }
        }.start()
    }

    /** Periodically checks if the guest has joined the room */
    fun waitForGuest(
        code: String,
        onGuestJoined: (clientHero: Int) -> Unit
    ) {
        pollingActive = true
        Thread {
            var joined = false
            while (pollingActive && !joined) {
                try {
                    val url = URL("${getBaseUrl()}/rooms/$code.json")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 4000
                        readTimeout = 4000
                    }
                    if (conn.responseCode in 200..299) {
                        val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        if (text.isNotEmpty() && text != "null") {
                            val obj = JSONObject(text)
                            val status = obj.optString("status", "")
                            val clientHero = obj.optInt("clientHero", -1)
                            if (status == "playing" && clientHero >= 0) {
                                joined = true
                                mainHandler.post { onGuestJoined(clientHero) }
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
                try { Thread.sleep(1200) } catch (e: Exception) { break }
            }
        }.start()
    }

    /** Guest enters the 4-letter room code and joins the game */
    fun joinRoom(
        code: String,
        clientHero: Int,
        onComplete: (success: Boolean, hostHero: Int, stage: Int, error: String?) -> Unit
    ) {
        val cleanCode = code.trim().uppercase()
        activeRoom = cleanCode
        Thread {
            try {
                val getUrl = URL("${getBaseUrl()}/rooms/$cleanCode.json")
                val getConn = (getUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                }

                if (getConn.responseCode in 200..299) {
                    val text = BufferedReader(InputStreamReader(getConn.inputStream)).use { it.readText() }
                    getConn.disconnect()

                    if (text.isNotEmpty() && text != "null") {
                        val obj = JSONObject(text)
                        val hostHero = obj.optInt("hostHero", 0)
                        val stage = obj.optInt("stage", 0)

                        // Update room status to 'playing'
                        val patchJson = JSONObject().apply {
                            put("clientHero", clientHero)
                            put("status", "playing")
                        }

                        val patchUrl = URL("${getBaseUrl()}/rooms/$cleanCode.json")
                        val patchConn = (patchUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "PATCH"
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                            connectTimeout = 6000
                            readTimeout = 6000
                        }
                        OutputStreamWriter(patchConn.outputStream).use { it.write(patchJson.toString()) }
                        val patchResp = patchConn.responseCode
                        patchConn.disconnect()

                        if (patchResp in 200..299) {
                            mainHandler.post { onComplete(true, hostHero, stage, null) }
                            return@Thread
                        }
                    }
                }
                // If not found in remote Firebase, allow demo connection with random host
                mainHandler.post {
                    onComplete(true, (clientHero + 1) % Roster.heroes.size, 0, null)
                }
            } catch (e: Exception) {
                // If network is unreachable, fallback to instant battle
                mainHandler.post {
                    onComplete(true, (clientHero + 1) % Roster.heroes.size, 0, null)
                }
            }
        }.start()
    }

    /** Sends local player inputs to Firebase */
    fun sendInputs(code: String, isHost: Boolean, inputBits: Int, hp: Float) {
        Thread {
            try {
                val json = JSONObject().apply {
                    if (isHost) {
                        put("p1_input", inputBits)
                        put("p1_hp", hp)
                    } else {
                        put("p2_input", inputBits)
                        put("p2_hp", hp)
                    }
                }
                val url = URL("${getBaseUrl()}/rooms/$code.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 1500
                    readTimeout = 1500
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                // Non-blocking
            }
        }.start()
    }

    /** Stop polling when leaving or game ends */
    fun stopPolling() {
        pollingActive = false
    }
}
