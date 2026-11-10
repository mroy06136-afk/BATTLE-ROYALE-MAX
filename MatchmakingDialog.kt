package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.GameMode
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MatchmakingDialog(
    mode: GameMode,
    onMatchReady: () -> Unit,
    onCancel: () -> Unit
) {
    var playersFound by remember { mutableStateOf(1) }
    var countdown by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        // Rapid simulated matchmaking to 50 players
        while (playersFound < 50) {
            delay((60..140).random().toLong())
            playersFound = (playersFound + (2..6).random()).coerceAtMost(50)
        }
        // Match found countdown
        countdown = 3
        delay(700)
        countdown = 2
        delay(700)
        countdown = 1
        delay(700)
        onMatchReady()
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .wrapContentHeight()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = BR_DarkBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, BR_Orange)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MATCHMAKING LOBBY",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(BR_Surface, CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (playersFound / 50f) },
                        modifier = Modifier.fillMaxSize(),
                        color = if (playersFound == 50) BR_Gold else BR_Orange,
                        trackColor = Color(0x33FFFFFF)
                    )
                    if (countdown != null) {
                        Text(
                            "$countdown",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = BR_Gold
                        )
                    } else {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = "Radar",
                            tint = BR_Orange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (countdown != null) "MATCH READY! LAUNCHING DROPSHIP..." else "SEARCHING FOR COMBATANTS...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (countdown != null) BR_Gold else Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "SURVIVORS READY: $playersFound / 50",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BR_Cyan
                )

                Text(
                    text = "Mode: ${mode.name} | Region: ASIA (24ms Low Latency)",
                    fontSize = 11.sp,
                    color = BR_TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border)
                ) {
                    Text("CANCEL SEARCH", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
