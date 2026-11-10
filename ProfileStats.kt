package com.example.model

enum class RankTier(val title: String, val minPoints: Int, val badgeColorHex: Long) {
    BRONZE("Bronze III", 0, 0xFFCD7F32),
    SILVER("Silver II", 1000, 0xFFC0C0C0),
    GOLD("Gold IV", 1600, 0xFFFFD700),
    PLATINUM("Platinum III", 2200, 0xFF00E5FF),
    DIAMOND("Diamond II", 2800, 0xFFB388FF),
    HEROIC("Heroic", 3400, 0xFFFF1744),
    GRANDMASTER("Grandmaster", 4200, 0xFFFFD600)
}

data class ProfileStats(
    val username: String = "Phoenix_Max",
    val title: String = "Apex Survivor",
    var level: Int = 24,
    var currentExp: Int = 3450,
    var maxExp: Int = 5000,
    var rankPoints: Int = 1850,
    var gold: Int = 12500,
    var diamonds: Int = 450,
    var battlePassTier: Int = 18,
    var matchesPlayed: Int = 84,
    var totalWins: Int = 22,
    var totalKills: Int = 342,
    var headshots: Int = 98,
    var damageDealtTotal: Long = 45800L,
    var highestKillsInMatch: Int = 14
) {
    fun getRank(): RankTier {
        return RankTier.values().lastOrNull { rankPoints >= it.minPoints } ?: RankTier.BRONZE
    }

    fun getKdRatio(): Float {
        val deaths = (matchesPlayed - totalWins).coerceAtLeast(1)
        return totalKills.toFloat() / deaths
    }

    fun getWinRatePercent(): Float {
        if (matchesPlayed == 0) return 0f
        return (totalWins.toFloat() / matchesPlayed) * 100f
    }

    fun getHeadshotRatePercent(): Float {
        if (totalKills == 0) return 0f
        return (headshots.toFloat() / totalKills) * 100f
    }
}

data class DailyMission(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    var current: Int,
    val rewardGold: Int,
    val rewardExp: Int,
    val rewardDiamonds: Int = 0,
    var isClaimed: Boolean = false
) {
    fun isCompleted(): Boolean = current >= target
}
