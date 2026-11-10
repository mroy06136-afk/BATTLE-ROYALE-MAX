package com.example.engine3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    
    fun length(): Float = sqrt(x * x + y * y + z * z)
    
    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.0001f) Vector3D(x / len, y / len, z / len) else Vector3D(0f, 0f, 0f)
    }

    fun distanceTo(other: Vector3D): Float = (this - other).length()
}

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val isVisible: Boolean
)

class Camera3D(
    var x: Float = 0f,
    var y: Float = 2f,
    var z: Float = 0f,
    var yaw: Float = 0f,   // Degrees horizontal
    var pitch: Float = 0f, // Degrees vertical
    var fov: Float = 75f   // Field of view in degrees
) {
    fun project(point: Vector3D, screenWidth: Float, screenHeight: Float): ProjectedPoint {
        val dx = point.x - x
        val dy = point.y - y
        val dz = point.z - z

        // 1. Rotate around Y axis (-yaw)
        val radYaw = Math.toRadians(-yaw.toDouble())
        val cosY = cos(radYaw).toFloat()
        val sinY = sin(radYaw).toFloat()

        val rx1 = dx * cosY - dz * sinY
        val rz1 = dx * sinY + dz * cosY
        val ry1 = dy

        // 2. Rotate around X axis (-pitch)
        val radPitch = Math.toRadians(-pitch.toDouble())
        val cosP = cos(radPitch).toFloat()
        val sinP = sin(radPitch).toFloat()

        val rx2 = rx1
        val ry2 = ry1 * cosP - rz1 * sinP
        val rz2 = ry1 * sinP + rz1 * cosP

        // Near plane clipping
        if (rz2 <= 0.8f) {
            return ProjectedPoint(0f, 0f, rz2, isVisible = false)
        }

        // Perspective division
        val fovRad = Math.toRadians(fov.toDouble() / 2.0)
        val fovScale = (screenWidth * 0.5f) / kotlin.math.tan(fovRad).toFloat()

        val screenX = (screenWidth * 0.5f) + (rx2 / rz2) * fovScale
        val screenY = (screenHeight * 0.5f) - (ry2 / rz2) * fovScale

        val inBounds = screenX >= -200f && screenX <= screenWidth + 200f &&
                       screenY >= -200f && screenY <= screenHeight + 200f

        return ProjectedPoint(screenX, screenY, rz2, isVisible = inBounds)
    }
}
