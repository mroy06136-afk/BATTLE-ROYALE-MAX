package com.example.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.ScopeType

@Composable
fun ScopeOverlay(scopeType: ScopeType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f

        when (scopeType) {
            ScopeType.SCOPE_8X -> {
                // Full sniper black vignette masking
                val scopeRadius = size.height * 0.42f
                
                // Draw dark outer mask
                val maskPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    addOval(androidx.compose.ui.geometry.Rect(cx - scopeRadius, cy - scopeRadius, cx + scopeRadius, cy + scopeRadius))
                }
                drawPath(maskPath, Color(0xEE05060A))

                // Scope ring
                drawCircle(Color(0xFF263238), scopeRadius, Offset(cx, cy), style = Stroke(width = 8f))
                drawCircle(Color(0xFF00E5FF), scopeRadius, Offset(cx, cy), style = Stroke(width = 2f))

                // Crosshair lines
                drawLine(Color(0xCC00E5FF), Offset(cx - scopeRadius, cy), Offset(cx + scopeRadius, cy), strokeWidth = 2f)
                drawLine(Color(0xCC00E5FF), Offset(cx, cy - scopeRadius), Offset(cx, cy + scopeRadius), strokeWidth = 2f)

                // Central red dot
                drawCircle(Color(0xFFFF1744), 3.5f, Offset(cx, cy))

                // Mil-dot hash marks
                val step = scopeRadius * 0.18f
                for (i in 1..4) {
                    // Vertical marks
                    drawLine(Color(0xAA00E5FF), Offset(cx - 8f, cy + i * step), Offset(cx + 8f, cy + i * step), strokeWidth = 1.5f)
                    drawLine(Color(0xAA00E5FF), Offset(cx - 8f, cy - i * step), Offset(cx + 8f, cy - i * step), strokeWidth = 1.5f)
                    // Horizontal marks
                    drawLine(Color(0xAA00E5FF), Offset(cx + i * step, cy - 8f), Offset(cx + i * step, cy + 8f), strokeWidth = 1.5f)
                    drawLine(Color(0xAA00E5FF), Offset(cx - i * step, cy - 8f), Offset(cx - i * step, cy + 8f), strokeWidth = 1.5f)
                }
            }
            ScopeType.SCOPE_4X, ScopeType.SCOPE_2X -> {
                val scopeRadius = size.height * 0.38f
                drawCircle(Color(0x8805060A), scopeRadius, Offset(cx, cy))
                drawCircle(Color(0xFFFF7043), scopeRadius, Offset(cx, cy), style = Stroke(width = 3f))

                // Crosshairs with gap in center
                val gap = 16f
                drawLine(Color(0xFFFF5722), Offset(cx - scopeRadius * 0.8f, cy), Offset(cx - gap, cy), strokeWidth = 2f)
                drawLine(Color(0xFFFF5722), Offset(cx + gap, cy), Offset(cx + scopeRadius * 0.8f, cy), strokeWidth = 2f)
                drawLine(Color(0xFFFF5722), Offset(cx, cy - scopeRadius * 0.8f), Offset(cx, cy - gap), strokeWidth = 2f)
                drawLine(Color(0xFFFF5722), Offset(cx, cy + gap), Offset(cx, cy + scopeRadius * 0.8f), strokeWidth = 2f)

                // Circle chevron
                drawCircle(Color(0xFFFF5722), 8f, Offset(cx, cy), style = Stroke(width = 2f))
                drawCircle(Color(0xFFFF1744), 3f, Offset(cx, cy))
            }
            ScopeType.RED_DOT, ScopeType.IRON_SIGHT -> {
                // Clean holographic sight reticle
                drawCircle(Color(0xFFFF1744), 4f, Offset(cx, cy))
                drawCircle(Color(0x88FF1744), 18f, Offset(cx, cy), style = Stroke(width = 1.8f))
                drawLine(Color(0xAAFF1744), Offset(cx - 26f, cy), Offset(cx - 18f, cy), strokeWidth = 2f)
                drawLine(Color(0xAAFF1744), Offset(cx + 18f, cy), Offset(cx + 26f, cy), strokeWidth = 2f)
                drawLine(Color(0xAAFF1744), Offset(cx, cy - 26f), Offset(cx, cy - 18f), strokeWidth = 2f)
                drawLine(Color(0xAAFF1744), Offset(cx, cy + 18f), Offset(cx, cy + 26f), strokeWidth = 2f)
            }
        }
    }
}
