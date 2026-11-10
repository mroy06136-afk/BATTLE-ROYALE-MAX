package com.example.ui.battle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamePreferences
import com.example.engine3d.Renderer3D
import com.example.game.GameEngine
import com.example.model.PlayerState
import com.example.model.ScopeType
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun BattleScreen(
    gameEngine: GameEngine,
    prefs: GamePreferences,
    onReturnToLobby: () -> Unit
) {
    val renderer = remember { Renderer3D() }
    var frameTick by remember { mutableStateOf(0L) }

    // Dialog toggles
    var showMapDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showRadioMenu by remember { mutableStateOf(false) }

    // Hitmarker flash state
    var hitmarkerState by remember { mutableStateOf<Boolean?>(null) }
    val hitmarkerTrigger by gameEngine.hitmarkerTrigger.collectAsState()

    LaunchedEffect(hitmarkerTrigger) {
        if (hitmarkerTrigger != null) {
            hitmarkerState = hitmarkerTrigger
            delay(180)
            hitmarkerState = null
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(gameEngine.isMatchActive) {
        var lastTime = System.nanoTime()
        while (gameEngine.isMatchActive) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now
                gameEngine.update(dt)
                frameTick = now
            }
        }
    }

    // When game ends, record stats
    LaunchedEffect(gameEngine.isGameOver) {
        if (gameEngine.isGameOver) {
            prefs.recordMatchResult(
                kills = gameEngine.localPlayer.kills,
                isWin = gameEngine.isBooyah,
                headshotCount = gameEngine.localPlayer.headshots,
                damage = gameEngine.localPlayer.damageDealt
            )
        }
    }

    val localPlayer = gameEngine.localPlayer
    val activeWeapon = localPlayer.getEquippedWeapon()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. 3D World Scene Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick to trigger recomposition every frame
            val unused = frameTick

            renderer.renderScene(
                drawScope = this,
                camera = gameEngine.camera,
                structures = gameEngine.structures,
                trees = gameEngine.trees,
                rocks = gameEngine.rocks,
                vehicles = gameEngine.vehicles,
                players = gameEngine.players,
                airdrop = gameEngine.airdropCrate,
                dropshipPos = gameEngine.dropshipPos,
                safeZone = gameEngine.safeZone,
                bulletTracers = gameEngine.bulletTracers,
                particles = gameEngine.particles,
                groundLoot = gameEngine.groundLoot,
                localPlayer = localPlayer
            )
        }

        // 2. Right-side Aim & Look Pan Gesture Area
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val sens = prefs.sensitivity * 0.16f
                        localPlayer.yaw = (localPlayer.yaw + dragAmount.x * sens) % 360f
                        localPlayer.pitch = (localPlayer.pitch - dragAmount.y * sens).coerceIn(-65f, 65f)
                    }
                }
        )

        // 3. ADS Scope Overlay (if aiming)
        if (localPlayer.isAiming && activeWeapon != null) {
            ScopeOverlay(scopeType = activeWeapon.equippedScope)
        }

        // 4. Center Crosshair (when not scoped)
        if (!localPlayer.isAiming && localPlayer.state == PlayerState.ALIVE) {
            CenterCrosshair(modifier = Modifier.align(Alignment.Center))
        }

        // 5. Hitmarker Flash
        if (hitmarkerState != null) {
            HitmarkerView(isHeadshot = hitmarkerState == true, modifier = Modifier.align(Alignment.Center))
        }

        // 6. Floating Damage Numbers
        for (dmg in gameEngine.damageNumbers) {
            val proj = gameEngine.camera.project(
                com.example.engine3d.Vector3D(dmg.x, dmg.y, dmg.z),
                1920f,
                1080f
            )
            if (proj.isVisible) {
                Text(
                    text = "${dmg.damage}",
                    color = if (dmg.isHeadshot) BR_Red else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = if (dmg.isHeadshot) 20.sp else 16.sp,
                    modifier = Modifier.offset(
                        x = (proj.screenX * 0.4f).dp,
                        y = (proj.screenY * 0.4f - (dmg.ageMs * 0.05f)).dp
                    )
                )
            }
        }

        // 7. Top HUD Bar (Alive count, kills, safe zone timer, minimap, admin button)
        TopBattleBar(
            gameEngine = gameEngine,
            onOpenMap = { showMapDialog = true },
            onOpenAdmin = { showAdminDialog = true },
            onOpenRadio = { showRadioMenu = !showRadioMenu },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 8. Kill Feed (Top Left)
        KillFeedView(
            killFeed = gameEngine.killFeed,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 64.dp, start = 12.dp)
        )

        // 9. Quick Tactical Radio Comms Menu
        AnimatedVisibility(
            visible = showRadioMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            TacticalRadioMenu(
                onSelectCommand = { cmd ->
                    showRadioMenu = false
                    gameEngine.killFeed.add(0, com.example.game.KillFeedEntry(localPlayer.name, "Squad", cmd, false, true))
                    gameEngine.soundManager.playReload()
                }
            )
        }

        // 10. Dropship / Parachute Phase Overlay
        if (localPlayer.state == PlayerState.DROPSHIP || localPlayer.state == PlayerState.SKYDIVING || localPlayer.state == PlayerState.PARACHUTING) {
            FlightControlsOverlay(
                player = localPlayer,
                onEject = { gameEngine.ejectFromDropship() },
                onDeployParachute = { gameEngine.deployParachute() },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 11. Vehicle Driving Controls Overlay
        if (localPlayer.state == PlayerState.DRIVING) {
            val currentVehicle = gameEngine.vehicles.firstOrNull { it.id == localPlayer.vehicleId }
            if (currentVehicle != null) {
                VehicleDrivingOverlay(
                    vehicle = currentVehicle,
                    onExit = { gameEngine.toggleVehicle() },
                    onHorn = { gameEngine.soundManager.playVehicleHorn() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // 12. Bottom Left Controls: Virtual Analog Joystick, Ability & Healing
        if (localPlayer.state == PlayerState.ALIVE) {
            BottomLeftControls(
                player = localPlayer,
                gameEngine = gameEngine,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        // 13. Bottom Center: Weapon slots & Health/Armor gauges
        if (localPlayer.state == PlayerState.ALIVE) {
            BottomCenterHUD(
                player = localPlayer,
                onSelectWeapon = { idx ->
                    localPlayer.equippedWeaponIndex = idx
                    gameEngine.soundManager.playReload()
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
            )
        }

        // 14. Bottom Right: Combat Action Buttons (Fire, ADS, Jump, Crouch, Prone, Reload)
        if (localPlayer.state == PlayerState.ALIVE) {
            BottomRightCombatControls(
                player = localPlayer,
                gameEngine = gameEngine,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // 15. Vehicle "ENTER" button prompt (when near vehicle on foot)
        if (localPlayer.state == PlayerState.ALIVE) {
            val nearestVehicle = gameEngine.vehicles.minByOrNull {
                sqrt((it.x - localPlayer.x).pow(2) + (it.z - localPlayer.z).pow(2))
            }
            if (nearestVehicle != null) {
                val dist = sqrt((nearestVehicle.x - localPlayer.x).pow(2) + (nearestVehicle.z - localPlayer.z).pow(2))
                if (dist < 4.5f) {
                    Button(
                        onClick = { gameEngine.toggleVehicle(nearestVehicle) },
                        colors = ButtonDefaults.buttonColors(containerColor = BR_Orange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 80.dp)
                            .testTag("enter_vehicle_button")
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Drive", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DRIVE ${nearestVehicle.type.displayName.uppercase()}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 16. Tactical Map Dialog
        if (showMapDialog) {
            TacticalMapDialog(
                gameEngine = gameEngine,
                onDismiss = { showMapDialog = false }
            )
        }

        // 17. Admin Control Panel Dialog
        if (showAdminDialog) {
            AdminPanelDialog(
                gameEngine = gameEngine,
                prefs = prefs,
                onDismiss = { showAdminDialog = false }
            )
        }

        // 18. Game Over / Booyah Dialog
        if (gameEngine.isGameOver) {
            GameOverDialog(
                gameEngine = gameEngine,
                onReturnToLobby = onReturnToLobby,
                onPlayAgain = {
                    gameEngine.startMatch(localPlayer.character, gameEngine.gameMode)
                }
            )
        }
    }
}

@Composable
private fun TopBattleBar(
    gameEngine: GameEngine,
    onOpenMap: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Alive & Kills count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0x990A0C14), RoundedCornerShape(8.dp))
                .border(1.dp, BR_Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.People, contentDescription = "Alive", tint = BR_Cyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${gameEngine.alivePlayersCount}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.GpsFixed, contentDescription = "Kills", tint = BR_Red, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${gameEngine.localPlayer.kills}",
                color = BR_Orange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Center: Safe Zone Shrink Timer
        val sz = gameEngine.safeZone
        val mins = (sz.timeRemainingSec / 60).toInt()
        val secs = (sz.timeRemainingSec % 60).toInt()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(if (sz.isShrinking) Color(0xAAFF1744) else Color(0x990A0C14), RoundedCornerShape(8.dp))
                .border(1.dp, if (sz.isShrinking) BR_Red else BR_Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Storm",
                tint = if (sz.isShrinking) Color.White else BR_Cyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (sz.isShrinking) "ZONE SHRINKING!" else String.format("%02d:%02d", mins, secs),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Right: Comms, Admin button & Minimap radar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenRadio,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x990A0C14), CircleShape)
                    .border(1.dp, BR_Border, CircleShape)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Radio", tint = BR_Cyan, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenAdmin,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x990A0C14), CircleShape)
                    .border(1.dp, BR_Orange, CircleShape)
                    .testTag("admin_panel_open_button")
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = BR_Orange, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive Minimap Radar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xCC080B12), CircleShape)
                    .border(1.5.dp, BR_Cyan, CircleShape)
                    .clickable { onOpenMap() }
                    .testTag("minimap_radar_button")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    // Grid rings
                    drawCircle(Color(0x4400E5FF), size.width * 0.35f, Offset(cx, cy), style = Stroke(width = 1f))
                    // Compass needle
                    val radYaw = Math.toRadians(gameEngine.localPlayer.yaw.toDouble())
                    drawLine(
                        Color(0xFF00E676),
                        Offset(cx, cy),
                        Offset(cx + sin(radYaw).toFloat() * 18f, cy - cos(radYaw).toFloat() * 18f),
                        strokeWidth = 2.5f
                    )
                    drawCircle(Color.White, 3f, Offset(cx, cy))
                }
            }
        }
    }
}

@Composable
private fun KillFeedView(killFeed: List<com.example.game.KillFeedEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        killFeed.take(4).forEach { entry ->
            Row(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .background(
                        if (entry.isLocalPlayerKiller) Color(0xCCFF5722) else Color(0x990A0C14),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.killerName, color = if (entry.isLocalPlayerKiller) Color.White else BR_Orange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("🔫 ${entry.weaponName}", color = Color.White, fontSize = 10.sp)
                if (entry.isHeadshot) {
                    Text(" 💥", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(entry.victimName, color = BR_TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TacticalRadioMenu(onSelectCommand: (String) -> Unit) {
    val commands = listOf(
        "Enemy Spotted! 🎯",
        "I Need Ammo! 📦",
        "Get in the Vehicle! 🚗",
        "Follow Me to Zone! 🏃",
        "Help / Cover Me! 🛡️"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = BR_DarkBg.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, BR_Cyan),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            commands.forEach { cmd ->
                TextButton(
                    onClick = { onSelectCommand(cmd) },
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Text(cmd, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FlightControlsOverlay(
    player: com.example.model.Player,
    onEject: () -> Unit,
    onDeployParachute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ALTITUDE: ${player.y.toInt()}m | AIRSPEED: 180 km/h",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (player.state == PlayerState.DROPSHIP) {
            Button(
                onClick = onEject,
                colors = ButtonDefaults.buttonColors(containerColor = BR_Orange),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(56.dp)
                    .width(180.dp)
                    .testTag("eject_dropship_button")
            ) {
                Icon(Icons.Default.FlightTakeoff, contentDescription = "Eject", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EJECT / JUMP", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        } else if (player.state == PlayerState.SKYDIVING) {
            Button(
                onClick = onDeployParachute,
                colors = ButtonDefaults.buttonColors(containerColor = BR_CyanDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(52.dp)
                    .width(200.dp)
                    .testTag("deploy_parachute_button")
            ) {
                Icon(Icons.Default.Paragliding, contentDescription = "Parachute", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("OPEN PARACHUTE", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun VehicleDrivingOverlay(
    vehicle: com.example.model.GameVehicle,
    onExit: () -> Unit,
    onHorn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left: Steer Left & Right
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vehicle.steeringInput = -1.0f },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC1F2436)),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Steer Left", tint = Color.White)
            }

            Button(
                onClick = { vehicle.steeringInput = 1.0f },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC1F2436)),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Steer Right", tint = Color.White)
            }
        }

        // Center: Speedometer & Horn
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${abs(vehicle.currentSpeed).toInt()} KM/H",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BR_Gold
            )
            Spacer(modifier = Modifier.height(4.dp))
            IconButton(
                onClick = onHorn,
                modifier = Modifier.size(44.dp).background(BR_Orange, CircleShape)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Horn", tint = Color.White)
            }
        }

        // Right: Gas, Reverse, Exit
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vehicle.throttleInput = 1.0f },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC2E7D32)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp).width(64.dp)
            ) {
                Text("GAS", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { vehicle.throttleInput = -0.6f },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCCD32F2F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp).width(64.dp)
            ) {
                Text("REV", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCCFF5722)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp).width(64.dp).testTag("exit_vehicle_button")
            ) {
                Text("EXIT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BottomLeftControls(
    player: com.example.model.Player,
    gameEngine: GameEngine,
    modifier: Modifier = Modifier
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = modifier.padding(start = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Virtual Analog Joystick
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0x550A0C14), CircleShape)
                .border(2.dp, Color(0x66FFFFFF), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            stickOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (stickOffset.x + dragAmount.x).coerceIn(-45f, 45f)
                            val newY = (stickOffset.y + dragAmount.y).coerceIn(-45f, 45f)
                            stickOffset = Offset(newX, newY)

                            // Translate joystick to player movement along camera yaw
                            val speedMult = if (player.isSprinting) 1.6f else 1.0f
                            val adminSpeed = if (gameEngine.adminSuperSpeed) 3.0f else 1.0f
                            val speed = (7.5f * speedMult * adminSpeed)

                            val radYaw = Math.toRadians(player.yaw.toDouble())
                            val forwardX = sin(radYaw).toFloat()
                            val forwardZ = cos(radYaw).toFloat()
                            val rightX = cos(radYaw).toFloat()
                            val rightZ = -sin(radYaw).toFloat()

                            val nx = (stickOffset.x / 45f)
                            val ny = (-stickOffset.y / 45f) // forward is negative Y

                            player.x += (forwardX * ny + rightX * nx) * speed * 0.016f
                            player.z += (forwardZ * ny + rightZ * nx) * speed * 0.016f
                        }
                    )
                }
        ) {
            // Stick thumb knob
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.Center)
                    .offset(x = stickOffset.x.dp, y = stickOffset.y.dp)
                    .background(BR_Cyan.copy(alpha = 0.85f), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Quick Sprint Lock, Ability & Heal Buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Sprint Toggle Button
            IconButton(
                onClick = { player.isSprinting = !player.isSprinting },
                modifier = Modifier
                    .size(42.dp)
                    .background(if (player.isSprinting) BR_Orange else Color(0x880A0C14), CircleShape)
                    .border(1.dp, BR_Border, CircleShape)
                    .testTag("sprint_toggle_button")
            ) {
                Icon(Icons.Default.DirectionsRun, contentDescription = "Sprint", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Character Special Ability Button (e.g. Alok Drop the Beat / Kelly Dash)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(BR_OrangeDark, CircleShape)
                    .border(1.5.dp, BR_Gold, CircleShape)
                    .clickable { gameEngine.activateCharacterAbility() }
                    .testTag("character_ability_button")
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = "Ability",
                    tint = BR_Gold,
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                )
                if (player.abilityCooldownTimerSec > 0f) {
                    Text(
                        text = "${player.abilityCooldownTimerSec.toInt()}s",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Quick Medkit button
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { gameEngine.useMedkit() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC2E7D32)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).testTag("quick_medkit_button")
                ) {
                    Text("MED: ${player.medkitsCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { gameEngine.useInhaler() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC0097A7)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp).testTag("quick_inhaler_button")
                ) {
                    Text("EP: ${player.inhalersCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BottomCenterHUD(
    player: com.example.model.Player,
    onSelectWeapon: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Weapon Slot Cards
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            player.weapons.forEachIndexed { idx, weapon ->
                val isSelected = (player.equippedWeaponIndex == idx)
                Card(
                    modifier = Modifier
                        .clickable { onSelectWeapon(idx) }
                        .border(
                            1.5.dp,
                            if (isSelected) BR_Cyan else BR_Border,
                            RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xCC1F2436) else Color(0x990A0C14)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(weapon.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                "${weapon.currentAmmo} / ${weapon.reserveAmmo}",
                                color = if (weapon.currentAmmo <= 5) BR_Red else BR_Cyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Health & Armor Gauges
        Row(
            modifier = Modifier
                .background(Color(0x990A0C14), RoundedCornerShape(8.dp))
                .border(1.dp, BR_Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HP Bar
            Column(modifier = Modifier.width(140.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("HP", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text("${player.hp.toInt()}/100", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                LinearProgressIndicator(
                    progress = { (player.hp / player.maxHp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF00E676),
                    trackColor = Color(0x44FFFFFF)
                )

                // EP Bar
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EP", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Text("${player.ep.toInt()}/100", color = Color.White, fontSize = 9.sp)
                }
                LinearProgressIndicator(
                    progress = { (player.ep / player.maxEp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFFFD54F),
                    trackColor = Color(0x44FFFFFF)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vest & Helmet badges
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "VEST L${player.vest?.level ?: 0}",
                        fontSize = 10.sp,
                        color = BR_Cyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "HELM L${player.helmet?.level ?: 0}",
                        fontSize = 10.sp,
                        color = BR_Gold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomRightCombatControls(
    player: com.example.model.Player,
    gameEngine: GameEngine,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Posture & Reload buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reload
            IconButton(
                onClick = { gameEngine.reloadWeapon() },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x990A0C14), CircleShape)
                    .border(1.5.dp, BR_Border, CircleShape)
                    .testTag("reload_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
            }

            // Jump
            IconButton(
                onClick = {
                    val jumpPower = if (gameEngine.adminSuperJump) 12f else 6.5f
                    player.y += jumpPower * 0.1f
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x990A0C14), CircleShape)
                    .border(1.5.dp, BR_Border, CircleShape)
                    .testTag("jump_button")
            ) {
                Icon(Icons.Default.North, contentDescription = "Jump", tint = Color.White)
            }

            // Crouch
            IconButton(
                onClick = {
                    player.isCrouching = !player.isCrouching
                    if (player.isCrouching) player.isProne = false
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(if (player.isCrouching) BR_Cyan else Color(0x990A0C14), CircleShape)
                    .border(1.5.dp, BR_Border, CircleShape)
                    .testTag("crouch_button")
            ) {
                Icon(Icons.Default.AirlineSeatReclineExtra, contentDescription = "Crouch", tint = if (player.isCrouching) Color.Black else Color.White)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Large Fire & ADS Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ADS Scope Toggle
            IconButton(
                onClick = { player.isAiming = !player.isAiming },
                modifier = Modifier
                    .size(52.dp)
                    .background(if (player.isAiming) BR_Cyan else Color(0xAA1F2436), CircleShape)
                    .border(2.dp, BR_Cyan, CircleShape)
                    .testTag("ads_scope_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Scope", tint = if (player.isAiming) Color.Black else Color.White, modifier = Modifier.size(26.dp))
            }

            // Big Fire Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFF7043), Color(0xFFD84315))),
                        CircleShape
                    )
                    .border(2.5.dp, Color.White, CircleShape)
                    .clickable { gameEngine.fireWeapon() }
                    .testTag("fire_button")
            ) {
                Icon(
                    Icons.Default.GpsFixed,
                    contentDescription = "Fire",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun CenterCrosshair(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(36.dp)) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val gap = 5f
        val len = 12f

        // Reticle lines
        drawLine(Color(0xCCFFFFFF), Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = 2f)
        drawLine(Color(0xCCFFFFFF), Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = 2f)
        drawLine(Color(0xCCFFFFFF), Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = 2f)
        drawLine(Color(0xCCFFFFFF), Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = 2f)
        drawCircle(Color(0xFFFF1744), 2f, Offset(cx, cy))
    }
}

@Composable
private fun HitmarkerView(isHeadshot: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(30.dp)) {
        val color = if (isHeadshot) BR_Red else Color.White
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val arm = 10f
        val gap = 3f

        drawLine(color, Offset(cx - gap - arm, cy - gap - arm), Offset(cx - gap, cy - gap), strokeWidth = 2.5f)
        drawLine(color, Offset(cx + gap, cy - gap), Offset(cx + gap + arm, cy - gap - arm), strokeWidth = 2.5f)
        drawLine(color, Offset(cx - gap - arm, cy + gap + arm), Offset(cx - gap, cy + gap), strokeWidth = 2.5f)
        drawLine(color, Offset(cx + gap, cy + gap), Offset(cx + gap + arm, cy + gap + arm), strokeWidth = 2.5f)
    }
}
