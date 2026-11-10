package com.example.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GamePreferences
import com.example.game.GameEngine
import com.example.model.ItemType
import com.example.model.Player
import com.example.model.VehicleType
import com.example.model.Weapon
import com.example.ui.theme.*

@Composable
fun AdminPanelDialog(
    gameEngine: GameEngine,
    prefs: GamePreferences,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Players", "Weapons", "Map & Events", "Rewards & System")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = BR_DarkBg.copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(2.dp, BR_Orange)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = BR_Orange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BATTLE ROYALE MAX - ADMIN CONTROL PANEL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("admin_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BR_Surface,
                    contentColor = BR_Orange,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // Content based on tab
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (selectedTab) {
                        0 -> AdminPlayersTab(gameEngine)
                        1 -> AdminWeaponsTab(gameEngine)
                        2 -> AdminMapTab(gameEngine)
                        3 -> AdminRewardsTab(gameEngine, prefs)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPlayersTab(engine: GameEngine) {
    var godMode by remember { mutableStateOf(engine.adminGodMode) }
    var infiniteAmmo by remember { mutableStateOf(engine.adminInfiniteAmmo) }
    var superSpeed by remember { mutableStateOf(engine.adminSuperSpeed) }
    var botDifficulty by remember { mutableStateOf(engine.adminBotDifficulty) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BR_Surface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("SUPER SURVIVOR CHEATS", fontWeight = FontWeight.Bold, color = BR_Gold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = godMode,
                            onCheckedChange = {
                                godMode = it
                                engine.adminGodMode = it
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("God Mode (Invincible)", color = Color.White, fontSize = 13.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = infiniteAmmo,
                            onCheckedChange = {
                                infiniteAmmo = it
                                engine.adminInfiniteAmmo = it
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Infinite Ammo", color = Color.White, fontSize = 13.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = superSpeed,
                            onCheckedChange = {
                                superSpeed = it
                                engine.adminSuperSpeed = it
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Super Speed x3", color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bot AI Difficulty: ${(botDifficulty * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = botDifficulty,
                        onValueChange = {
                            botDifficulty = it
                            engine.adminBotDifficulty = it
                        },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.width(200.dp)
                    )

                    Button(
                        onClick = {
                            // Spawn 5 bots near player
                            val lp = engine.localPlayer
                            for (i in 1..5) {
                                val b = Player(
                                    id = "bot_admin_${System.currentTimeMillis()}_$i",
                                    name = "Special_Agent_$i",
                                    isBot = true,
                                    x = lp.x + (i * 4f),
                                    y = 0f,
                                    z = lp.z + (i * 4f),
                                    state = com.example.model.PlayerState.ALIVE,
                                    teamColorHex = 0xFFFF1744
                                )
                                b.weapons.add(Weapon.createAK47())
                                engine.players.add(b)
                                engine.alivePlayersCount++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BR_Orange)
                    ) {
                        Text("Spawn 5 Bots", fontSize = 12.sp)
                    }
                }
            }
        }

        Text("ACTIVE COMBATANTS (${engine.players.size} Total):", fontWeight = FontWeight.Bold, color = BR_TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(engine.players) { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(BR_Surface, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(p.name + if (p.id == engine.localPlayer.id) " (YOU)" else "", color = if (p.id == engine.localPlayer.id) BR_Cyan else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("HP: ${p.hp.toInt()}/100 | Kills: ${p.kills} | State: ${p.state}", color = BR_TextSecondary, fontSize = 11.sp)
                    }

                    if (p.id != engine.localPlayer.id && p.state != com.example.model.PlayerState.DEAD) {
                        Row {
                            Button(
                                onClick = {
                                    p.state = com.example.model.PlayerState.DEAD
                                    p.hp = 0f
                                    engine.alivePlayersCount = (engine.alivePlayersCount - 1).coerceAtLeast(1)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BR_Red),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Kick/Eliminate", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWeaponsTab(engine: GameEngine) {
    val weaponsList = listOf(
        Pair("AK-47 Assault Rifle", Weapon.createAK47()),
        Pair("M4A1 Tactical Rifle", Weapon.createM4A1()),
        Pair("MP40 Submachine Gun", Weapon.createMP40()),
        Pair("AWM Sniper (.300 Mag)", Weapon.createAWM()),
        Pair("SPAS-12 Shotgun", Weapon.createSPAS12()),
        Pair("Desert Eagle Pistol", Weapon.createDesertEagle()),
        Pair("Frag Grenade (3x)", Weapon.createGrenade())
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("EQUIP WEAPONS & GEAR INSTANTLY:", fontWeight = FontWeight.Bold, color = BR_Gold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    engine.localPlayer.medkitsCount += 5
                    engine.localPlayer.inhalersCount += 5
                    engine.soundManager.playReload()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("+5 Medkits & +5 Inhalers", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    engine.localPlayer.vest = com.example.model.GroundLoot("admin_vest", ItemType.VEST, "Level 3 Military Vest", level = 3, x = 0f, z = 0f)
                    engine.localPlayer.helmet = com.example.model.GroundLoot("admin_helm", ItemType.HELMET, "Level 3 Spetsnaz Helmet", level = 3, x = 0f, z = 0f)
                    engine.soundManager.playReload()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BR_CyanDark)
            ) {
                Text("Equip Max Level 3 Armor", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(weaponsList) { (label, w) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(BR_Surface, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Damage: ${w.baseDamage.toInt()} | Mag: ${w.magazineSize} | Range: ${w.range.toInt()}m", color = BR_TextSecondary, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val lp = engine.localPlayer
                            lp.weapons.add(w)
                            lp.equippedWeaponIndex = lp.weapons.size - 1
                            engine.soundManager.playReload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BR_Orange),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Equip Gun", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMapTab(engine: GameEngine) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("MAP, SAFE ZONE & WORLD EVENTS:", fontWeight = FontWeight.Bold, color = BR_Gold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    // Force immediate safe zone shrink
                    engine.safeZone.timeRemainingSec = 1f
                    engine.safeZone.isShrinking = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Shrink Safe Zone Now", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    engine.summonAirdrop()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BR_Red),
                modifier = Modifier.weight(1f)
            ) {
                Text("Summon Immediate Airdrop", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("SPAWN VEHICLES AT PLAYER POSITION:", fontWeight = FontWeight.Bold, color = BR_TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val lp = engine.localPlayer
                    engine.vehicles.add(com.example.model.GameVehicle("car_${System.currentTimeMillis()}", VehicleType.SPORTS_CAR, lp.x + 4f, 0f, lp.z + 4f, lp.yaw))
                    engine.soundManager.playVehicleHorn()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Spawn Supercar GT", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val lp = engine.localPlayer
                    engine.vehicles.add(com.example.model.GameVehicle("buggy_${System.currentTimeMillis()}", VehicleType.MILITARY_BUGGY, lp.x + 4f, 0f, lp.z + 4f, lp.yaw))
                    engine.soundManager.playVehicleHorn()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Spawn Desert Buggy", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val lp = engine.localPlayer
                    engine.vehicles.add(com.example.model.GameVehicle("bike_${System.currentTimeMillis()}", VehicleType.MOTORCYCLE, lp.x + 4f, 0f, lp.z + 4f, lp.yaw))
                    engine.soundManager.playVehicleHorn()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Spawn Cyber Bike", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Teleport local player to safe zone center
                engine.localPlayer.x = engine.safeZone.targetCenterX
                engine.localPlayer.z = engine.safeZone.targetCenterZ
                engine.soundManager.playReload()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Teleport Player to Safe Zone Center", fontSize = 13.sp)
        }
    }
}

@Composable
private fun AdminRewardsTab(engine: GameEngine, prefs: GamePreferences) {
    var gold by remember { mutableStateOf(prefs.gold) }
    var diamonds by remember { mutableStateOf(prefs.diamonds) }
    var soundOn by remember { mutableStateOf(prefs.soundEnabled) }
    var preset by remember { mutableStateOf(prefs.graphicsPreset) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("ADMIN ECONOMY & SETTINGS:", fontWeight = FontWeight.Bold, color = BR_Gold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    prefs.gold += 10000
                    gold = prefs.gold
                    engine.soundManager.playVictoryFanfare()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                modifier = Modifier.weight(1f)
            ) {
                Text("+10,000 Gold Coins", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    prefs.diamonds += 5000
                    diamonds = prefs.diamonds
                    engine.soundManager.playVictoryFanfare()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.weight(1f)
            ) {
                Text("+5,000 Diamonds", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Game Audio & Sound FX:", color = Color.White, fontSize = 13.sp)
            Switch(
                checked = soundOn,
                onCheckedChange = {
                    soundOn = it
                    prefs.soundEnabled = it
                    engine.soundManager.isSoundEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Graphics Fidelity Target: $preset", color = Color.White, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Medium", "High", "Ultra 60FPS").forEach { opt ->
                    FilterChip(
                        selected = preset == opt,
                        onClick = {
                            preset = opt
                            prefs.graphicsPreset = opt
                        },
                        label = { Text(opt, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}
