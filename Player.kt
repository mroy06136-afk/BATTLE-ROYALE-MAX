package com.example.model

enum class PlayerState {
    DROPSHIP,
    SKYDIVING,
    PARACHUTING,
    ALIVE,
    DRIVING,
    KNOCKED,
    DEAD
}

data class Player(
    val id: String,
    val name: String,
    val isBot: Boolean = false,
    val squadId: Int = 1,
    val teamColorHex: Long = 0xFF00E5FF,
    var x: Float = 0f,
    var y: Float = 350f, // Dropship altitude
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var yaw: Float = 0f,   // Degrees
    var pitch: Float = 0f, // Degrees (-60 to +60)
    var state: PlayerState = PlayerState.DROPSHIP,
    var hp: Float = 100f,
    val maxHp: Float = 100f,
    var ep: Float = 0f, // Energy points for passive heal
    val maxEp: Float = 100f,
    var helmet: GroundLoot? = null,
    var vest: GroundLoot? = null,
    var backpackLevel: Int = 1,
    var medkitsCount: Int = 2,
    var inhalersCount: Int = 3,
    val weapons: MutableList<Weapon> = mutableListOf(),
    var equippedWeaponIndex: Int = 0,
    val character: GameCharacter = GameCharacter.getAllCharacters()[0],
    var kills: Int = 0,
    var damageDealt: Float = 0f,
    var headshots: Int = 0,
    var isShooting: Boolean = false,
    var isAiming: Boolean = false,
    var isCrouching: Boolean = false,
    var isProne: Boolean = false,
    var isSprinting: Boolean = false,
    var isReloading: Boolean = false,
    var reloadTimerMs: Long = 0L,
    var abilityCooldownTimerSec: Float = 0f,
    var activeAbilityTimerSec: Float = 0f,
    var vehicleId: String? = null,
    // Bot AI behaviors
    var botTargetPlayerId: String? = null,
    var botMoveTargetX: Float = 0f,
    var botMoveTargetZ: Float = 0f,
    var botFireCooldownMs: Long = 0L,
    var botReactionTimerMs: Long = 0L,
    var botDifficulty: Float = 1.0f // 0.6 = Easy, 1.0 = Medium, 1.4 = Pro
) {
    fun getEquippedWeapon(): Weapon? {
        if (equippedWeaponIndex in weapons.indices) {
            return weapons[equippedWeaponIndex]
        }
        return null
    }

    fun takeDamage(amount: Float, isHeadshot: Boolean): Float {
        if (state == PlayerState.DEAD) return 0f
        
        var damageAfterArmor = amount
        if (isHeadshot) {
            val helmetReduction = helmet?.getDamageReduction() ?: 0f
            damageAfterArmor *= (1.0f - helmetReduction)
        } else {
            val vestReduction = vest?.getDamageReduction() ?: 0f
            damageAfterArmor *= (1.0f - vestReduction)
        }
        
        hp -= damageAfterArmor
        if (hp <= 0f) {
            hp = 0f
            state = PlayerState.DEAD
        }
        return damageAfterArmor
    }
}
