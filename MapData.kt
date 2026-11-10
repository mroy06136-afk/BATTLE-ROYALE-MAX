package com.example.model

enum class StructureType {
    HOUSE_1_STORY,
    HOUSE_2_STORY,
    WATCHTOWER,
    WAREHOUSE_HANGAR,
    CONTAINER_CRATE,
    MILITARY_BUNKER
}

data class MapStructure(
    val id: String,
    val type: StructureType,
    val x: Float,
    val y: Float = 0f,
    val z: Float,
    val width: Float,
    val height: Float,
    val depth: Float,
    val yaw: Float = 0f,
    val colorHex: Long = 0xFF78909C,
    val roofColorHex: Long = 0xFFD84315
)

data class MapTree(
    val id: String,
    val x: Float,
    val y: Float = 0f,
    val z: Float,
    val height: Float = 8f,
    val trunkRadius: Float = 0.6f,
    val foliageRadius: Float = 3.2f,
    val isPalm: Boolean = false
)

data class MapRock(
    val id: String,
    val x: Float,
    val y: Float = 0f,
    val z: Float,
    val size: Float = 2.5f
)

data class PointOfInterest(
    val name: String,
    val x: Float,
    val z: Float,
    val tier: String,
    val description: String
)

data class AirdropCrate(
    val id: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var isLanding: Boolean = true,
    var hasLanded: Boolean = false,
    val weapon: Weapon = Weapon.createAWM(),
    val vest: GroundLoot = GroundLoot("drop_vest", ItemType.VEST, "Level 3 Military Vest", level = 3, x = 0f, z = 0f),
    val helmet: GroundLoot = GroundLoot("drop_helm", ItemType.HELMET, "Level 3 Spetsnaz Helmet", level = 3, x = 0f, z = 0f),
    var isLooted: Boolean = false
)
