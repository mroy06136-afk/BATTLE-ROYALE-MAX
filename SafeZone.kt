package com.example.model

data class SafeZone(
    var currentCenterX: Float = 0f,
    var currentCenterZ: Float = 0f,
    var currentRadius: Float = 480f,
    var targetCenterX: Float = 0f,
    var targetCenterZ: Float = 0f,
    var targetRadius: Float = 320f,
    var phase: Int = 1,
    val maxPhases: Int = 5,
    var timeRemainingSec: Float = 60f,
    var isShrinking: Boolean = false,
    var damagePerSecond: Float = 2.5f
) {
    fun isInsideSafeZone(x: Float, z: Float): Boolean {
        val dx = x - currentCenterX
        val dz = z - currentCenterZ
        return (dx * dx + dz * dz) <= (currentRadius * currentRadius)
    }

    fun isInsideTargetZone(x: Float, z: Float): Boolean {
        val dx = x - targetCenterX
        val dz = z - targetCenterZ
        return (dx * dx + dz * dz) <= (targetRadius * targetRadius)
    }
}
