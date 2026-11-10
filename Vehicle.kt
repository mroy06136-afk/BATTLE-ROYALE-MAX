package com.example.model

enum class VehicleType(
    val displayName: String,
    val maxSpeed: Float,
    val acceleration: Float,
    val health: Float,
    val handling: Float,
    val bodyColorHex: Long,
    val length: Float,
    val width: Float
) {
    SPORTS_CAR("Supercar GT", 145f, 55f, 400f, 2.8f, 0xFFFF3D00, 4.2f, 2.0f),
    MILITARY_BUGGY("Desert Buggy", 110f, 45f, 600f, 2.2f, 0xFF795548, 3.8f, 2.2f),
    MOTORCYCLE("Cyber Bike", 135f, 65f, 280f, 3.5f, 0xFF00E5FF, 2.4f, 1.0f)
}

data class GameVehicle(
    val id: String,
    val type: VehicleType,
    var x: Float,
    var y: Float = 0f,
    var z: Float,
    var yaw: Float = 0f,
    var currentSpeed: Float = 0f,
    var health: Float = type.health,
    val maxHealth: Float = type.health,
    var driverId: String? = null,
    var isDestroyed: Boolean = false,
    var steeringInput: Float = 0f,
    var throttleInput: Float = 0f
) {
    fun isOccupied(): Boolean = driverId != null
}
