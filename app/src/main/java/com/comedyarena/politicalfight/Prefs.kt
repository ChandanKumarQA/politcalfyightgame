package com.comedyarena.politicalfight

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) {
        if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("arena", Context.MODE_PRIVATE)
        Sfx.soundOn = sound
        Sfx.musicOn = music
    }
    var unlocked: Int
        get() = sp.getInt("unlocked", 1)
        set(v) = sp.edit().putInt("unlocked", v.coerceIn(1, 10)).apply()
    var hero: Int
        get() = sp.getInt("hero", 0)
        set(v) = sp.edit().putInt("hero", v).apply()
    var best: Int
        get() = sp.getInt("best", 0)
        set(v) = sp.edit().putInt("best", v).apply()
    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) { sp.edit().putBoolean("sound", v).apply(); Sfx.soundOn = v }
    var music: Boolean
        get() = sp.getBoolean("music", true)
        set(v) { sp.edit().putBoolean("music", v).apply(); Sfx.musicOn = v }
    fun reset() { sp.edit().putInt("unlocked", 1).putInt("best", 0).apply() }
}
