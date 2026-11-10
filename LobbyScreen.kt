package com.example.ui.lobby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GamePreferences
import com.example.game.GameMode
import com.example.model.GameCharacter
import com.example.ui.battle.AdminPanelDialog
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    prefs: GamePreferences,
    onStartMatch: (GameMode, GameCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(GameMode.SOLO) }
    val allCharacters = remember { GameCharacter.getAllCharacters() }
    var currentCharacter by remember {
        mutableStateOf(allCharacters.firstOrNull { it.id == prefs.selectedCharacterId } ?: allCharacters.first())
    }

    // Dialog states
    var showMatchmaking by remember { mutableStateOf(false) }
    var showCharacterSelect by remember { mutableStateOf(false) }
    var showArmory by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showMissions by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }

    val profileStats = remember(prefs.rankPoints, prefs.gold, prefs.diamonds) { prefs.getProfileStats() }

    Box(modifier = modifier.fillMaxSize().background(BR_DarkBg)) {
        // 1. Cinematic Island Wallpaper Background
        Image(
            painter = painterResource(id = R.drawable.banner_lobby),
            contentDescription = "Battle Royale Island Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.45f
        )

        // Dark gradient overlay to keep HUD ultra crisp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE0A0C14),
                            Color(0x990A0C14),
                            Color(0xF00A0C14)
                        )
                    )
                )
        )

        // 2. Top Header Bar: Profile, Currencies, Admin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Profile Pill
            Row(
                modifier = Modifier
                    .background(Color(0xCC141722), RoundedCornerShape(24.dp))
                    .border(1.dp, BR_Border, RoundedCornerShape(24.dp))
                    .clickable { showProfile = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BR_Orange, CircleShape)
                ) {
                    Text(
                        currentCharacter.iconEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(profileStats.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(profileStats.getRank().badgeColorHex), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                profileStats.getRank().title,
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lv.${profileStats.level}", color = BR_TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            // Currencies & Admin Panel Button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Gold
                Surface(
                    color = Color(0xCC141722),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${profileStats.gold}", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Diamonds
                Surface(
                    color = Color(0xCC141722),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Cyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💎", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${profileStats.diamonds}", color = BR_Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Admin Control Panel Button
                IconButton(
                    onClick = { showAdmin = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xCC141722), CircleShape)
                        .border(1.5.dp, BR_Orange, CircleShape)
                        .testTag("lobby_admin_button")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = BR_Orange, modifier = Modifier.size(20.dp))
                }
            }
        }

        // 3. Main Center Stage: Character Hero Podium & Squad
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Game Mode Selection & Team Lobby
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Game Mode Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC141722)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("BATTLE ROYALE MODES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BR_TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GameMode.values().forEach { mode ->
                                val isSel = (selectedMode == mode)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedMode = mode },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) BR_Orange else Color(0xFF1E2436)
                                ) {
                                    Text(
                                        text = mode.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else BR_TextSecondary,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Squad Lobbies
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC141722)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SQUAD TEAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("#MAX-8921", fontSize = 10.sp, color = BR_Cyan)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SquadSlot(name = "${profileStats.username} (Leader)", isLeader = true, isReady = true)
                        if (selectedMode != GameMode.SOLO) {
                            SquadSlot(name = "GhostRider_99", isLeader = false, isReady = true)
                        }
                        if (selectedMode == GameMode.SQUAD) {
                            SquadSlot(name = "+ Invite Friend", isLeader = false, isReady = false)
                            SquadSlot(name = "+ Invite Friend", isLeader = false, isReady = false)
                        }
                    }
                }

                // Quick Map Sector info
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC141722)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = "Map", tint = BR_Cyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("MAP: KALAHARI RIDGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("50 Survivors | Ranked Match", fontSize = 10.sp, color = BR_TextSecondary)
                        }
                    }
                }
            }

            // Center Hero Character Showcase Podium
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Character Badge
                Surface(
                    color = Color(currentCharacter.roleColorHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(currentCharacter.roleColorHex))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentCharacter.iconEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(currentCharacter.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                            Text(currentCharacter.title, fontSize = 11.sp, color = Color(currentCharacter.roleColorHex))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Model Graphic (Podium ring with glow)
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(currentCharacter.roleColorHex).copy(alpha = 0.45f), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentCharacter.iconEmoji,
                        fontSize = 80.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Equipped Weapon Tag
                Surface(
                    color = Color(0xCC1F2436),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔫", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EQUIPPED: M4A1 CYBER ASSAULT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BR_Cyan)
                    }
                }
            }

            // Right Column: Big START MATCH CTA & Match Info
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC141722)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = "Ping", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ASIA-PACIFIC: 24ms", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text("Auto-match with 50 live players", color = BR_TextSecondary, fontSize = 10.sp)
                    }
                }

                // Giant Neon Orange START MATCH Button
                Button(
                    onClick = { showMatchmaking = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BR_Orange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("start_match_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "START MATCH",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${selectedMode.name} BATTLE ROYALE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }
        }

        // 4. Bottom Navigation Menu (Characters, Armory, Missions, Profile)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LobbyNavButton(
                    label = "SPECIALISTS",
                    icon = Icons.Default.Person,
                    onClick = { showCharacterSelect = true },
                    testTag = "lobby_specialists_button"
                )

                LobbyNavButton(
                    label = "ARMORY",
                    icon = Icons.Default.Shield,
                    onClick = { showArmory = true },
                    testTag = "lobby_armory_button"
                )

                LobbyNavButton(
                    label = "OPERATIONS",
                    icon = Icons.Default.Assignment,
                    badge = "!",
                    onClick = { showMissions = true },
                    testTag = "lobby_missions_button"
                )

                LobbyNavButton(
                    label = "CAREER STATS",
                    icon = Icons.Default.Leaderboard,
                    onClick = { showProfile = true },
                    testTag = "lobby_profile_button"
                )
            }
        }

        // Dialogs
        if (showMatchmaking) {
            MatchmakingDialog(
                mode = selectedMode,
                onMatchReady = {
                    showMatchmaking = false
                    onStartMatch(selectedMode, currentCharacter)
                },
                onCancel = { showMatchmaking = false }
            )
        }

        if (showCharacterSelect) {
            CharacterSelectDialog(
                prefs = prefs,
                onSelectCharacter = { char ->
                    currentCharacter = char
                },
                onDismiss = { showCharacterSelect = false }
            )
        }

        if (showArmory) {
            WeaponArmoryDialog(
                onDismiss = { showArmory = false }
            )
        }

        if (showProfile) {
            ProfileDialog(
                stats = profileStats,
                onDismiss = { showProfile = false }
            )
        }

        if (showMissions) {
            MissionsDialog(
                prefs = prefs,
                onDismiss = { showMissions = false }
            )
        }

        if (showAdmin) {
            val dummyEngine = remember {
                val eng = com.example.game.GameEngine()
                eng.startMatch(currentCharacter, selectedMode)
                eng
            }
            AdminPanelDialog(
                gameEngine = dummyEngine,
                prefs = prefs,
                onDismiss = { showAdmin = false }
            )
        }
    }
}

@Composable
private fun SquadSlot(name: String, isLeader: Boolean, isReady: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(Color(0xFF1E2436), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isLeader) Icons.Default.Star else Icons.Default.Person,
                contentDescription = null,
                tint = if (isLeader) BR_Gold else BR_TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        if (isReady) {
            Text("READY", fontSize = 9.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LobbyNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xCC141722),
        border = androidx.compose.foundation.BorderStroke(1.dp, BR_Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = BR_Cyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (badge != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(color = BR_Red, shape = CircleShape) {
                    Text(
                        badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
