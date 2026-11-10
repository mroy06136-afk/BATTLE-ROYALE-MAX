package com.example.model

enum class ItemType {
    HELMET,
    VEST,
    BACKPACK,
    MEDKIT,
    INHALER,
    AMMO,
    WEAPON,
    SCOPE,
    ATTACHMENT
}

data class GroundLoot(
    val id: String,
    val type: ItemType,
    val name: String,
    val level: Int = 1,
    var x: Float,
    var y: Float = 0f,
    var z: Float,
    val weapon: Weapon? = null,
    val ammoType: AmmoType? = null,
    val ammoCount: Int = 0,
    val scopeType: ScopeType? = null,
    val healAmount: Float = 0f,
    val isAirdropExclusive: Boolean = false
) {
    fun getDamageReduction(): Float {
        return when (type) {
            ItemType.HELMET -> when (level) {
                1 -> 0.30f
                2 -> 0.45f
                3 -> 0.60f
                else -> 0f
            }
            ItemType.VEST -> when (level) {
                1 -> 0.35f
                2 -> 0.50f
                3 -> 0.65f
                else -> 0f
            }
            else -> 0f
        }
    }
}
