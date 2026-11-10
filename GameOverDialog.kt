package com.example.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.GameEngine
import com.example.ui.theme.*

@Composable
fun GameOverDialog(
    gameEngine: GameEngine,
    onReturnToLobby: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val isBooyah = gameEngine.isBooyah
    val rank = gameEngine.matchRank
    val kills = gameEngine.localPlayer.kills
    val damage = gameEngine.localPlayer.damageDealt.toInt()
    val headshots = gameEngine.localPlayer.headshots

    val goldEarned = kills * 120 + if (isBooyah) 800 else 250
    val rpEarned = if (isBooyah) 55 else (kills * 8 - 15).coerceAtLeast(5)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = BR_DarkBg,
            border = androidx.compose.foundation.BorderStroke(
                2.5.dp,
                if (isBooyah) BR_Gold else BR_Orange
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Banner
                if (isBooyah) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = BR_Gold,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BOOYAH!",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = BR_Gold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "#1 VICTORY ROYALE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "ELIMINATED",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = BR_Red,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "MATCH RANK #$rank / 50",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BR_Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            StatBox(label = "KILLS", value = "$kills", color = BR_Orange)
                            StatBox(label = "DAMAGE", value = "$damage", color = BR_Cyan)
                            StatBox(label = "HEADSHOTS", value = "$headshots", color = BR_Red)
                        }

                        Divider(color = BR_Border, modifier = Modifier.padding(vertical = 12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            StatBox(label = "RANK POINTS", value = "+$rpEarned RP", color = BR_Gold)
                            StatBox(label = "GOLD EARNED", value = "+$goldEarned G", color = Color(0xFFFFD54F))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onReturnToLobby,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("lobby_return_button"),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BR_Border),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Lobby", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LOBBY", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("play_again_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBooyah) BR_Gold else BR_Orange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Again", tint = if (isBooyah) Color.Black else Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "PLAY AGAIN",
                            color = if (isBooyah) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = BR_TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
