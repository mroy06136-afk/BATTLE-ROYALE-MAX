package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GameCharacter
import com.example.model.ProfileStats

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("battle_royale_max_prefs", Context.MODE_PRIVATE)

    var gold: Int
        get() = prefs.getInt("gold", 12500)
        set(value) = prefs.edit().putInt("gold", value).apply()

    var diamonds: Int
        get() = prefs.getInt("diamonds", 450)
        set(value) = prefs.edit().putInt("diamonds", value).apply()

    var rankPoints: Int
        get() = prefs.getInt("rank_points", 1850)
        set(value) = prefs.edit().putInt("rank_points", value).apply()

    var selectedCharacterId: String
        get() = prefs.getString("selected_character", "alok") ?: "alok"
        set(value) = prefs.edit().putString("selected_character", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var sensitivity: Float
        get() = prefs.getFloat("sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("sensitivity", value).apply()

    var graphicsPreset: String
        get() = prefs.getString("graphics_preset", "Ultra 60FPS") ?: "Ultra 60FPS"
        set(value) = prefs.edit().putString("graphics_preset", value).apply()

    var totalMatches: Int
        get() = prefs.getInt("total_matches", 84)
        set(value) = prefs.edit().putInt("total_matches", value).apply()

    var totalWins: Int
        get() = prefs.getInt("total_wins", 22)
        set(value) = prefs.edit().putInt("total_wins", value).apply()

    var totalKills: Int
        get() = prefs.getInt("total_kills", 342)
        set(value) = prefs.edit().putInt("total_kills", value).apply()

    var headshots: Int
        get() = prefs.getInt("headshots", 98)
        set(value) = prefs.edit().putInt("headshots", value).apply()

    fun getProfileStats(): ProfileStats {
        return ProfileStats(
            username = "Phoenix_Max",
            title = "Apex Survivor",
            level = 24,
            currentExp = 3450,
            maxExp = 5000,
            rankPoints = rankPoints,
            gold = gold,
            diamonds = diamonds,
            battlePassTier = 18,
            matchesPlayed = totalMatches,
            totalWins = totalWins,
            totalKills = totalKills,
            headshots = headshots
        )
    }

    fun recordMatchResult(kills: Int, isWin: Boolean, headshotCount: Int, damage: Float) {
        totalMatches++
        if (isWin) totalWins++
        totalKills += kills
        headshots += headshotCount
        val rpDelta = if (isWin) 55 else (kills * 8 - 15).coerceAtLeast(5)
        rankPoints = (rankPoints + rpDelta).coerceAtLeast(0)
        gold += (kills * 120 + if (isWin) 800 else 250)
        diamonds += if (isWin) 25 else 5
    }
}
