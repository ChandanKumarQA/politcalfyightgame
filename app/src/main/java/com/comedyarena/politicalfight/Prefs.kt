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
        get() = sp.getInt("unlocked", 500) // All 500 levels unlocked by default for instant play
        set(v) = sp.edit().putInt("unlocked", v.coerceIn(1, 500)).apply()
    var hero: Int
        get() = sp.getInt("hero", 0)
        set(v) = sp.edit().putInt("hero", v).apply()
    var stage: Int
        get() = sp.getInt("stage", 0)
        set(v) = sp.edit().putInt("stage", v.coerceIn(0, 2)).apply()
    var best: Int
        get() = sp.getInt("best", 0)
        set(v) = sp.edit().putInt("best", v).apply()
    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) { sp.edit().putBoolean("sound", v).apply(); Sfx.soundOn = v }
    var music: Boolean
        get() = sp.getBoolean("music", true)
        set(v) { sp.edit().putBoolean("music", v).apply(); Sfx.musicOn = v }
    fun reset() { sp.edit().putInt("unlocked", 500).putInt("best", 0).apply() }
}
