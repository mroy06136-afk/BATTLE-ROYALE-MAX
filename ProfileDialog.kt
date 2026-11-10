package com.example.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ProfileStats
import com.example.ui.theme.*

@Composable
fun ProfileDialog(
    stats: ProfileStats,
    onDismiss: () -> Unit
) {
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
                        "SURVIVOR CAREER DOSSIER",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Avatar & Rank Badge
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = BR_Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(BR_Orange, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.MilitaryTech,
                                    contentDescription = "Rank",
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center).size(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(stats.username, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(stats.title, fontSize = 12.sp, color = BR_Gold)

                            Spacer(modifier = Modifier.height(12.dp))

                            val rank = stats.getRank()
                            Surface(
                                color = Color(rank.badgeColorHex),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = rank.title.uppercase(),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${stats.rankPoints} Rank Points", fontSize = 12.sp, color = BR_TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right: Career Records Grid
                    Card(
                        modifier = Modifier.weight(1.5f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = BR_Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("ALL-TIME COMBAT PERFORMANCE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BR_TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("MATCHES", "${stats.matchesPlayed}")
                                StatItem("BOOYAH WINS", "${stats.totalWins}")
                                StatItem("WIN RATE", String.format("%.1f%%", stats.getWinRatePercent()))
                            }

                            Divider(color = BR_Border, modifier = Modifier.padding(vertical = 12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("TOTAL KILLS", "${stats.totalKills}")
                                StatItem("K/D RATIO", String.format("%.2f", stats.getKdRatio()))
                                StatItem("HEADSHOT %", String.format("%.1f%%", stats.getHeadshotRatePercent()))
                            }

                            Divider(color = BR_Border, modifier = Modifier.padding(vertical = 12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("MAX KILLS IN MATCH", "${stats.highestKillsInMatch}")
                                StatItem("CAREER DAMAGE", "${stats.damageDealtTotal}")
                                StatItem("BATTLE PASS TIER", "Tier ${stats.battlePassTier}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(label, fontSize = 10.sp, color = BR_TextSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}
