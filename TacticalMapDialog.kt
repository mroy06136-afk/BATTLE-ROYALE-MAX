package com.example.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.GameEngine
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TacticalMapDialog(
    gameEngine: GameEngine,
    onDismiss: () -> Unit
) {
    var waypoint by remember { mutableStateOf<Offset?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = BR_DarkBg.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(2.dp, BR_Cyan)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Map", tint = BR_Cyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TACTICAL ISLAND MAP - KALAHARI RIDGE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tap map to ping waypoint",
                            fontSize = 12.sp,
                            color = BR_TextSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Interactive 2D Map Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F1A24), RoundedCornerShape(8.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    waypoint = offset
                                    gameEngine.soundManager.playReload()
                                }
                            }
                    ) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        val mapScale = (size.height.coerceAtMost(size.width) * 0.42f) / 450f

                        fun worldToMap(wx: Float, wz: Float): Offset {
                            return Offset(cx + wx * mapScale, cy + wz * mapScale)
                        }

                        // Island background circle/polygon
                        drawCircle(
                            color = Color(0xFF1E3824),
                            radius = 450f * mapScale,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color(0xFF2E7D32),
                            radius = 400f * mapScale,
                            center = Offset(cx, cy)
                        )

                        // Named POIs
                        val pois = listOf(
                            Pair("Peak City", Offset(0f, 0f)),
                            Pair("Clock Tower", Offset(-180f, -160f)),
                            Pair("Bimakti Base", Offset(200f, 180f)),
                            Pair("Pochinok", Offset(160f, -180f)),
                            Pair("Shipyard", Offset(-190f, 190f))
                        )
                        for (poi in pois) {
                            val mapPos = worldToMap(poi.second.x, poi.second.y)
                            drawCircle(Color(0x88FFD54F), 10f, mapPos)
                            drawCircle(Color(0xFFFFD54F), 5f, mapPos)
                        }

                        // Current Safe Zone (Blue Ring)
                        val safeCenter = worldToMap(gameEngine.safeZone.currentCenterX, gameEngine.safeZone.currentCenterZ)
                        val safeRadius = gameEngine.safeZone.currentRadius * mapScale
                        drawCircle(Color(0x3300E5FF), safeRadius, safeCenter)
                        drawCircle(Color(0xFF00E5FF), safeRadius, safeCenter, style = Stroke(width = 3f))

                        // Target Next Safe Zone (White Ring)
                        val targetCenter = worldToMap(gameEngine.safeZone.targetCenterX, gameEngine.safeZone.targetCenterZ)
                        val targetRadius = gameEngine.safeZone.targetRadius * mapScale
                        drawCircle(Color(0xFFFFFFFF), targetRadius, targetCenter, style = Stroke(width = 2f))

                        // Vehicle markers
                        for (v in gameEngine.vehicles) {
                            val vPos = worldToMap(v.x, v.z)
                            drawCircle(Color(0xFFFF9800), 5f, vPos)
                        }

                        // Airdrop marker
                        val airdrop = gameEngine.airdropCrate
                        if (airdrop != null) {
                            val adPos = worldToMap(airdrop.x, airdrop.z)
                            drawCircle(Color(0xFFFF1744), 8f, adPos)
                            drawCircle(Color(0xFFFFD600), 4f, adPos)
                        }

                        // Other players / Teammates / Bots
                        for (p in gameEngine.players) {
                            if (p.id != gameEngine.localPlayer.id && p.state != com.example.model.PlayerState.DEAD) {
                                val pPos = worldToMap(p.x, p.z)
                                val color = if (p.squadId == gameEngine.localPlayer.squadId) Color(0xFF00E5FF) else Color(0x66FF1744)
                                drawCircle(color, 4f, pPos)
                            }
                        }

                        // Local player position & orientation
                        val lp = gameEngine.localPlayer
                        val lpPos = worldToMap(lp.x, lp.z)
                        val radYaw = Math.toRadians(lp.yaw.toDouble())

                        // Vision cone
                        val conePath = Path().apply {
                            moveTo(lpPos.x, lpPos.y)
                            lineTo(
                                lpPos.x + sin(radYaw - 0.4).toFloat() * 32f,
                                lpPos.y + cos(radYaw - 0.4).toFloat() * 32f
                            )
                            lineTo(
                                lpPos.x + sin(radYaw + 0.4).toFloat() * 32f,
                                lpPos.y + cos(radYaw + 0.4).toFloat() * 32f
                            )
                            close()
                        }
                        drawPath(conePath, Color(0x6600E676))

                        // Player dot
                        drawCircle(Color(0xFF00E676), 7f, lpPos)
                        drawCircle(Color.White, 3f, lpPos)

                        // Waypoint ping if placed
                        if (waypoint != null) {
                            drawCircle(Color(0xFFFFD600), 12f, waypoint!!, style = Stroke(width = 2.5f))
                            drawCircle(Color(0xFFFFD600), 4f, waypoint!!)
                            drawLine(Color(0xFFFFD600), lpPos, waypoint!!, strokeWidth = 1.8f)
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = Color(0xFF00E676), label = "You")
                    LegendItem(color = Color(0xFF00E5FF), label = "Safe Zone")
                    LegendItem(color = Color.White, label = "Next Zone")
                    LegendItem(color = Color(0xFFFF9800), label = "Vehicles")
                    LegendItem(color = Color(0xFFFF1744), label = "Airdrop")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = RoundedCornerShape(2.dp), color = color) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = BR_TextSecondary)
    }
}
