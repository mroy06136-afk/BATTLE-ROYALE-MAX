package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.example.data.GamePreferences
import com.example.model.DailyMission
import com.example.ui.theme.*

@Composable
fun MissionsDialog(
    prefs: GamePreferences,
    onDismiss: () -> Unit
) {
    val missions = remember {
        mutableStateListOf(
            DailyMission("m1", "Combat Eliminator", "Eliminate 5 enemies in Battle Royale", 5, 5, 500, 20),
            DailyMission("m2", "Island Survival", "Survive for more than 10 minutes", 1, 1, 350, 15),
            DailyMission("m3", "Road Warrior", "Travel 1,500m in any vehicle", 1500, 1500, 400, 15),
            DailyMission("m4", "Rifle Marksman", "Deal 1,000 damage with Assault Rifles", 1000, 750, 600, 25),
            DailyMission("m5", "Apex Champion", "Achieve a Booyah / #1 Victory finish", 1, 1, 1000, 50, rewardDiamonds = 50)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = BR_DarkBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, BR_Gold)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DAILY OPERATIONS & BOUNTIES",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(missions) { mission ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BR_Surface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mission.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(mission.description, color = BR_TextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            progress = { (mission.current.toFloat() / mission.target).coerceIn(0f, 1f) },
                                            modifier = Modifier.width(140.dp).height(5.dp),
                                            color = BR_Gold,
                                            trackColor = Color(0x33FFFFFF)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${mission.current}/${mission.target}", fontSize = 10.sp, color = BR_TextSecondary)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("+${mission.rewardGold} Gold", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        if (mission.rewardDiamonds > 0) {
                                            Text("+${mission.rewardDiamonds} Dia", color = BR_Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        onClick = {
                                            if (mission.isCompleted() && !mission.isClaimed) {
                                                mission.isClaimed = true
                                                prefs.gold += mission.rewardGold
                                                prefs.diamonds += mission.rewardDiamonds
                                            }
                                        },
                                        enabled = mission.isCompleted() && !mission.isClaimed,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (mission.isClaimed) Color(0xFF2E7D32) else BR_Orange
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (mission.isClaimed) "CLAIMED" else if (mission.isCompleted()) "CLAIM" else "IN PROGRESS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
