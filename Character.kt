package com.example.model

data class GameCharacter(
    val id: String,
    val name: String,
    val title: String,
    val abilityName: String,
    val abilityDescription: String,
    val abilityCooldownSec: Int,
    val roleColorHex: Long,
    val iconEmoji: String,
    val speedMultiplier: Float = 1.0f,
    val unlockedByDefault: Boolean = false,
    val unlockCostGold: Int = 0
) {
    companion object {
        fun getAllCharacters(): List<GameCharacter> = listOf(
            GameCharacter(
                id = "alok",
                name = "Vanguard Alok",
                title = "Battlefield Maestro",
                abilityName = "Drop the Beat",
                abilityDescription = "Creates a 5m aura that restores 5 HP/sec and boosts movement speed by 15% for 6 seconds.",
                abilityCooldownSec = 35,
                roleColorHex = 0xFFFF7043,
                iconEmoji = "🎵",
                speedMultiplier = 1.05f,
                unlockedByDefault = true
            ),
            GameCharacter(
                id = "kelly",
                name = "Kelly Swift",
                title = "Speed Sprinter",
                abilityName = "Dash Sprint",
                abilityDescription = "Passive 12% faster sprinting speed, and active burst dash granting 35% speed for 4 seconds.",
                abilityCooldownSec = 25,
                roleColorHex = 0xFFFFEB3B,
                iconEmoji = "⚡",
                speedMultiplier = 1.15f,
                unlockedByDefault = true
            ),
            GameCharacter(
                id = "chrono",
                name = "Chrono Shield",
                title = "Forcefield Specialist",
                abilityName = "Time Turner",
                abilityDescription = "Deploys a stationary energy forcefield that blocks 200 incoming bullet damage for 5 seconds.",
                abilityCooldownSec = 45,
                roleColorHex = 0xFF00E5FF,
                iconEmoji = "🛡️",
                speedMultiplier = 1.0f,
                unlockedByDefault = false,
                unlockCostGold = 2500
            ),
            GameCharacter(
                id = "hayato",
                name = "Hayato Ronin",
                title = "Bushido Warrior",
                abilityName = "Bushido Art",
                abilityDescription = "When maximum HP decreases by 10%, bullet armor penetration increases by 10%.",
                abilityCooldownSec = 30,
                roleColorHex = 0xFFFF1744,
                iconEmoji = "⚔️",
                speedMultiplier = 1.0f,
                unlockedByDefault = false,
                unlockCostGold = 3500
            ),
            GameCharacter(
                id = "moco",
                name = "Moco Hacker",
                title = "Cyber Infiltrator",
                abilityName = "Hacker's Eye",
                abilityDescription = "Tags shot enemies for 5 seconds, exposing their exact position on minimap and through walls.",
                abilityCooldownSec = 20,
                roleColorHex = 0xFF76FF03,
                iconEmoji = "👁️",
                speedMultiplier = 1.0f,
                unlockedByDefault = false,
                unlockCostGold = 4000
            )
        )
    }
}

data class OutfitSkin(
    val id: String,
    val name: String,
    val characterId: String,
    val tier: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
)
