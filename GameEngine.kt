package com.example.game

import com.example.engine3d.BulletTracer
import com.example.engine3d.Camera3D
import com.example.engine3d.Particle
import com.example.engine3d.Vector3D
import com.example.model.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Random
import kotlin.math.*

data class DamageNumber(
    val damage: Int,
    val isHeadshot: Boolean,
    val x: Float,
    val y: Float,
    val z: Float,
    var ageMs: Long = 0L,
    val maxAgeMs: Long = 900L
)

data class KillFeedEntry(
    val killerName: String,
    val victimName: String,
    val weaponName: String,
    val isHeadshot: Boolean,
    val isLocalPlayerKiller: Boolean
)

enum class GameMode {
    SOLO,
    DUO,
    SQUAD
}

class GameEngine(
    val soundManager: GameSoundManager = GameSoundManager()
) {
    private val random = Random()

    var gameMode: GameMode = GameMode.SOLO
    var isMatchActive: Boolean = false
    var isGameOver: Boolean = false
    var isBooyah: Boolean = false

    lateinit var localPlayer: Player
    val players = mutableListOf<Player>()
    val structures = mutableListOf<MapStructure>()
    val trees = mutableListOf<MapTree>()
    val rocks = mutableListOf<MapRock>()
    val vehicles = mutableListOf<GameVehicle>()
    val groundLoot = mutableListOf<GroundLoot>()
    var airdropCrate: AirdropCrate? = null

    val safeZone = SafeZone()
    val camera = Camera3D()

    var dropshipPos: Vector3D? = Vector3D(-420f, 260f, -420f)
    var dropshipDir = Vector3D(1f, 0f, 1f).normalized()
    var dropshipSpeed = 48f

    val bulletTracers = mutableListOf<BulletTracer>()
    val particles = mutableListOf<Particle>()
    val damageNumbers = mutableListOf<DamageNumber>()
    val killFeed = mutableListOf<KillFeedEntry>()

    // Admin & cheat toggles
    var adminGodMode: Boolean = false
    var adminInfiniteAmmo: Boolean = false
    var adminSuperSpeed: Boolean = false
    var adminSuperJump: Boolean = false
    var adminBotDifficulty: Float = 1.0f

    // Match statistics
    var matchRank: Int = 1
    var alivePlayersCount: Int = 50

    // Hitmarker state
    private val _hitmarkerTrigger = MutableStateFlow<Boolean?>(null)
    val hitmarkerTrigger: StateFlow<Boolean?> = _hitmarkerTrigger

    init {
        initMap()
    }

    fun startMatch(
        selectedCharacter: GameCharacter,
        mode: GameMode = GameMode.SOLO
    ) {
        gameMode = mode
        isMatchActive = true
        isGameOver = false
        isBooyah = false
        alivePlayersCount = 50
        matchRank = 1
        killFeed.clear()
        bulletTracers.clear()
        particles.clear()
        damageNumbers.clear()

        // Init dropship
        dropshipPos = Vector3D(-380f, 260f, -380f)

        // Init local player
        localPlayer = Player(
            id = "player_local",
            name = "Phoenix_Max",
            isBot = false,
            squadId = 1,
            teamColorHex = 0xFF00E5FF,
            x = dropshipPos!!.x,
            y = dropshipPos!!.y,
            z = dropshipPos!!.z,
            yaw = 45f,
            pitch = 0f,
            state = PlayerState.DROPSHIP,
            character = selectedCharacter
        )

        // Give basic starter weapons & gear
        localPlayer.weapons.clear()
        localPlayer.weapons.add(Weapon.createM4A1())
        localPlayer.weapons.add(Weapon.createMP40())
        localPlayer.weapons.add(Weapon.createDesertEagle())
        localPlayer.equippedWeaponIndex = 0

        players.clear()
        players.add(localPlayer)

        // Spawn 49 other players (bots & simulated online players)
        val botNames = listOf(
            "ShadowRider", "ViperX", "Kelly_Pro", "Hayato_Slash", "ChronoGod",
            "MocoGhost", "AlokBeats", "KalahariKing", "BermudaSniper", "Wolfrahh_99",
            "DashaQueen", "K_Master", "SkylerSonic", "SteffieArt", "LuquetaKick",
            "JaiSpecial", "CluTracker", "JotaHunter", "NotoraDrive", "AlvaroBoom",
            "A124_Cyborg", "RafaelSilent", "LauraAim", "HayatoFire", "MishaSpeed",
            "AndrewArmor", "FordWill", "OliviaMedic", "KlaFist", "MaximSpeed",
            "PalomaAmmo", "MiguelCrazy", "AntonioTough", "CarolineCute", "WukongBush",
            "NikitaSMG", "MishaRacer", "JosephDash", "ShirouDeliver", "MaroFalcon",
            "D-BeeMusic", "ThivaRescue", "DimitriAura", "OthoMemory", "NairiWall",
            "KentaShield", "IrisGloo", "TatsuyaDash", "OrionCrimson"
        )

        val squadSize = when (mode) {
            GameMode.SOLO -> 1
            GameMode.DUO -> 2
            GameMode.SQUAD -> 4
        }

        for (i in 0 until 49) {
            val name = botNames.getOrElse(i) { "Combatant_${i + 1}" }
            val squadId = if (mode == GameMode.SOLO) (i + 2) else ((i + 1) / squadSize) + 1
            val isTeammate = (squadId == localPlayer.squadId)

            val bot = Player(
                id = "bot_$i",
                name = name,
                isBot = true,
                squadId = squadId,
                teamColorHex = if (isTeammate) 0xFF00E5FF else 0xFFFF1744,
                x = dropshipPos!!.x + (random.nextFloat() * 20f - 10f),
                y = dropshipPos!!.y,
                z = dropshipPos!!.z + (random.nextFloat() * 20f - 10f),
                yaw = random.nextFloat() * 360f,
                state = PlayerState.DROPSHIP,
                botDifficulty = adminBotDifficulty
            )
            // Equip bots with random weapons
            val botWeapons = listOf(Weapon.createAK47(), Weapon.createM4A1(), Weapon.createMP40(), Weapon.createSPAS12())
            bot.weapons.add(botWeapons[random.nextInt(botWeapons.size)])
            bot.weapons.add(Weapon.createDesertEagle())
            players.add(bot)
        }

        // Reset safe zone
        safeZone.currentCenterX = 0f
        safeZone.currentCenterZ = 0f
        safeZone.currentRadius = 450f
        safeZone.targetCenterX = (random.nextFloat() * 160f) - 80f
        safeZone.targetCenterZ = (random.nextFloat() * 160f) - 80f
        safeZone.targetRadius = 260f
        safeZone.phase = 1
        safeZone.timeRemainingSec = 75f
        safeZone.isShrinking = false
        safeZone.damagePerSecond = 2.0f

        // Reset ground loot
        spawnGroundLoot()
    }

    private fun initMap() {
        structures.clear()
        trees.clear()
        rocks.clear()
        vehicles.clear()

        // 1. Peak City (Center high-loot city)
        structures.add(MapStructure("peak_hq", StructureType.HOUSE_2_STORY, 0f, 0f, 0f, 22f, 14f, 22f, colorHex = 0xFF546E7A, roofColorHex = 0xFFFF5722))
        structures.add(MapStructure("peak_apt1", StructureType.HOUSE_2_STORY, -40f, 0f, 20f, 16f, 11f, 16f, colorHex = 0xFF455A64, roofColorHex = 0xFF37474F))
        structures.add(MapStructure("peak_apt2", StructureType.HOUSE_1_STORY, 35f, 0f, -25f, 18f, 7f, 14f, colorHex = 0xFF78909C, roofColorHex = 0xFFD84315))
        structures.add(MapStructure("peak_store", StructureType.WAREHOUSE_HANGAR, -15f, 0f, -50f, 28f, 9f, 20f, colorHex = 0xFF607D8B, roofColorHex = 0xFF263238))

        // 2. Clock Tower Outpost (South West)
        structures.add(MapStructure("clock_tower", StructureType.WATCHTOWER, -180f, 0f, -160f, 12f, 26f, 12f, colorHex = 0xFF795548, roofColorHex = 0xFF8D6E63))
        structures.add(MapStructure("clock_house1", StructureType.HOUSE_1_STORY, -210f, 0f, -140f, 15f, 6.5f, 15f, colorHex = 0xFF8D6E63, roofColorHex = 0xFFBCAAA4))

        // 3. Bimakti Military Base (North East)
        structures.add(MapStructure("base_hangar1", StructureType.WAREHOUSE_HANGAR, 200f, 0f, 180f, 32f, 12f, 24f, colorHex = 0xFF37474F, roofColorHex = 0xFF212121))
        structures.add(MapStructure("base_hangar2", StructureType.WAREHOUSE_HANGAR, 240f, 0f, 140f, 30f, 11f, 22f, colorHex = 0xFF37474F, roofColorHex = 0xFF212121))
        structures.add(MapStructure("base_tower", StructureType.WATCHTOWER, 170f, 0f, 210f, 8f, 18f, 8f, colorHex = 0xFF546E7A, roofColorHex = 0xFF00E5FF))

        // 4. Pochinok Village (South East)
        structures.add(MapStructure("pochinok_1", StructureType.HOUSE_1_STORY, 160f, 0f, -180f, 16f, 6.5f, 16f, colorHex = 0xFF8D6E63, roofColorHex = 0xFFE64A19))
        structures.add(MapStructure("pochinok_2", StructureType.HOUSE_2_STORY, 195f, 0f, -160f, 18f, 11f, 18f, colorHex = 0xFF6D4C41, roofColorHex = 0xFFD84315))

        // 5. Shipyard Harbor (North West)
        structures.add(MapStructure("shipyard_crane", StructureType.WATCHTOWER, -220f, 0f, 180f, 14f, 24f, 14f, colorHex = 0xFFFF8F00, roofColorHex = 0xFFFFA000))
        structures.add(MapStructure("shipyard_con1", StructureType.CONTAINER_CRATE, -190f, 0f, 200f, 12f, 5f, 6f, colorHex = 0xFFD32F2F))
        structures.add(MapStructure("shipyard_con2", StructureType.CONTAINER_CRATE, -170f, 0f, 220f, 12f, 5f, 6f, colorHex = 0xFF1976D2))

        // Natural Trees & Rocks across the island
        for (i in 0..65) {
            val angle = (i * 25f) * (PI / 180f).toFloat()
            val radius = 50f + (i * 5.5f)
            val tx = cos(angle) * radius + (sin(i.toFloat()) * 25f)
            val tz = sin(angle) * radius + (cos(i.toFloat()) * 25f)
            trees.add(MapTree("tree_$i", tx, 0f, tz, height = 7f + (i % 4) * 2f))
        }

        for (i in 0..25) {
            val rx = ((i * 37) % 650) - 325f
            val rz = ((i * 59) % 650) - 325f
            rocks.add(MapRock("rock_$i", rx, 0f, rz, size = 2f + (i % 3) * 1.5f))
        }

        // Vehicles scattered at strategic crossroads
        vehicles.add(GameVehicle("car_peak", VehicleType.SPORTS_CAR, x = 20f, z = 35f, yaw = 90f))
        vehicles.add(GameVehicle("buggy_base", VehicleType.MILITARY_BUGGY, x = 160f, z = 160f, yaw = 180f))
        vehicles.add(GameVehicle("moto_clock", VehicleType.MOTORCYCLE, x = -160f, z = -140f, yaw = 45f))
        vehicles.add(GameVehicle("car_shipyard", VehicleType.SPORTS_CAR, x = -180f, z = 160f, yaw = 0f))
    }

    private fun spawnGroundLoot() {
        groundLoot.clear()
        // Spawn loot near buildings and crossroads
        for (struct in structures) {
            groundLoot.add(GroundLoot("loot_${struct.id}_1", ItemType.WEAPON, "AK-47 Assault Rifle", x = struct.x + 3f, z = struct.z + 3f, weapon = Weapon.createAK47()))
            groundLoot.add(GroundLoot("loot_${struct.id}_2", ItemType.AMMO, "7.62mm Ammo (60x)", x = struct.x - 3f, z = struct.z + 2f, ammoType = AmmoType.AMMO_762, ammoCount = 60))
            groundLoot.add(GroundLoot("loot_${struct.id}_3", ItemType.VEST, "Level 2 Tactical Vest", level = 2, x = struct.x + 2f, z = struct.z - 3f))
            groundLoot.add(GroundLoot("loot_${struct.id}_4", ItemType.MEDKIT, "First Aid Medkit", x = struct.x - 2f, z = struct.z - 2f, healAmount = 100f))
        }
        // Random field loot
        for (i in 0..35) {
            val lx = (random.nextFloat() * 600f) - 300f
            val lz = (random.nextFloat() * 600f) - 300f
            groundLoot.add(GroundLoot("loot_field_$i", ItemType.INHALER, "Energy Inhaler", x = lx, z = lz, healAmount = 40f))
        }
    }

    // Dropship ejection / parachute jump
    fun ejectFromDropship() {
        if (localPlayer.state == PlayerState.DROPSHIP) {
            localPlayer.state = PlayerState.SKYDIVING
            localPlayer.vy = -18f
            soundManager.playExplosion()
        }
    }

    fun deployParachute() {
        if (localPlayer.state == PlayerState.SKYDIVING) {
            localPlayer.state = PlayerState.PARACHUTING
            localPlayer.vy = -7f
            soundManager.playReload()
        }
    }

    // Game loop tick (invoked at 60 FPS)
    fun update(deltaSeconds: Float) {
        if (!isMatchActive) return

        // 1. Dropship Flight
        if (dropshipPos != null) {
            val currentPos = dropshipPos!!
            dropshipPos = Vector3D(
                currentPos.x + dropshipDir.x * dropshipSpeed * deltaSeconds,
                currentPos.y,
                currentPos.z + dropshipDir.z * dropshipSpeed * deltaSeconds
            )
            // If local player still on dropship, stick with it
            if (localPlayer.state == PlayerState.DROPSHIP) {
                localPlayer.x = dropshipPos!!.x
                localPlayer.y = dropshipPos!!.y
                localPlayer.z = dropshipPos!!.z

                // Auto-eject at end of route
                if (dropshipPos!!.x > 360f || dropshipPos!!.z > 360f) {
                    ejectFromDropship()
                }
            }
        }

        // 2. Update Bots AI & Drops
        updateBots(deltaSeconds)

        // 3. Update Local Player Physics
        updateLocalPlayerPhysics(deltaSeconds)

        // 4. Update Vehicles
        updateVehicles(deltaSeconds)

        // 5. Update Safe Zone
        updateSafeZone(deltaSeconds)

        // 6. Update Airdrop
        updateAirdrop(deltaSeconds)

        // 7. Update Particles & Tracers
        updateEffects(deltaSeconds)

        // 8. Update Camera to follow player or vehicle
        updateCamera()

        // 9. Check Victory / Defeat
        checkGameStatus()
    }

    private fun updateLocalPlayerPhysics(dt: Float) {
        // Skydiving / Parachuting
        when (localPlayer.state) {
            PlayerState.SKYDIVING -> {
                localPlayer.y += localPlayer.vy * dt
                // Forward glide based on camera yaw
                val radYaw = Math.toRadians(localPlayer.yaw.toDouble())
                localPlayer.x += sin(radYaw).toFloat() * 18f * dt
                localPlayer.z += cos(radYaw).toFloat() * 18f * dt

                // Auto-open parachute at low altitude
                if (localPlayer.y <= 60f) {
                    deployParachute()
                }
            }
            PlayerState.PARACHUTING -> {
                localPlayer.y += localPlayer.vy * dt
                val radYaw = Math.toRadians(localPlayer.yaw.toDouble())
                localPlayer.x += sin(radYaw).toFloat() * 10f * dt
                localPlayer.z += cos(radYaw).toFloat() * 10f * dt

                if (localPlayer.y <= 0.2f) {
                    localPlayer.y = 0f
                    localPlayer.state = PlayerState.ALIVE
                    soundManager.playReload()
                }
            }
            PlayerState.ALIVE -> {
                // Gravity & ground level
                localPlayer.y = 0f

                // Active ability timer & cooldown
                if (localPlayer.activeAbilityTimerSec > 0f) {
                    localPlayer.activeAbilityTimerSec -= dt
                    // Alok heal
                    if (localPlayer.character.id == "alok") {
                        localPlayer.hp = (localPlayer.hp + 5f * dt).coerceAtMost(localPlayer.maxHp)
                    }
                }
                if (localPlayer.abilityCooldownTimerSec > 0f) {
                    localPlayer.abilityCooldownTimerSec -= dt
                }

                // EP Passive Healing
                if (localPlayer.ep > 0f && localPlayer.hp < localPlayer.maxHp) {
                    val heal = min(localPlayer.ep, 3f * dt)
                    localPlayer.ep -= heal
                    localPlayer.hp = (localPlayer.hp + heal).coerceAtMost(localPlayer.maxHp)
                }

                // Auto loot pickup for ammo & medkits
                val iter = groundLoot.iterator()
                while (iter.hasNext()) {
                    val loot = iter.next()
                    val dist = sqrt((loot.x - localPlayer.x).pow(2) + (loot.z - localPlayer.z).pow(2))
                    if (dist < 2.5f) {
                        when (loot.type) {
                            ItemType.MEDKIT -> {
                                localPlayer.medkitsCount++
                                iter.remove()
                                soundManager.playReload()
                            }
                            ItemType.INHALER -> {
                                localPlayer.inhalersCount++
                                iter.remove()
                                soundManager.playReload()
                            }
                            ItemType.AMMO -> {
                                val eq = localPlayer.getEquippedWeapon()
                                if (eq != null && eq.ammoType == loot.ammoType) {
                                    eq.reserveAmmo += loot.ammoCount
                                    iter.remove()
                                    soundManager.playReload()
                                }
                            }
                            ItemType.VEST -> {
                                if (localPlayer.vest == null || loot.level > (localPlayer.vest?.level ?: 0)) {
                                    localPlayer.vest = loot
                                    iter.remove()
                                    soundManager.playReload()
                                }
                            }
                            ItemType.HELMET -> {
                                if (localPlayer.helmet == null || loot.level > (localPlayer.helmet?.level ?: 0)) {
                                    localPlayer.helmet = loot
                                    iter.remove()
                                    soundManager.playReload()
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // Storm damage
                if (!adminGodMode && !safeZone.isInsideSafeZone(localPlayer.x, localPlayer.z)) {
                    localPlayer.hp -= safeZone.damagePerSecond * dt
                    if (localPlayer.hp <= 0f) {
                        localPlayer.hp = 0f
                        localPlayer.state = PlayerState.DEAD
                        handlePlayerElimination(localPlayer, "Storm Barrier", "Electric Zone", false)
                    }
                }
            }
            PlayerState.DRIVING -> {
                // Handled in updateVehicles
            }
            else -> {}
        }
    }

    private fun updateBots(dt: Float) {
        for (bot in players) {
            if (!bot.isBot || bot.state == PlayerState.DEAD) continue

            // 1. Dropship & Skydiving for bots
            if (bot.state == PlayerState.DROPSHIP) {
                // Staggered bot jumps across map
                if (random.nextFloat() < 0.035f || dropshipPos!!.x > 320f) {
                    bot.state = PlayerState.SKYDIVING
                    bot.vy = -18f
                }
            } else if (bot.state == PlayerState.SKYDIVING) {
                bot.y += bot.vy * dt
                val radYaw = Math.toRadians(bot.yaw.toDouble())
                bot.x += sin(radYaw).toFloat() * 15f * dt
                bot.z += cos(radYaw).toFloat() * 15f * dt
                if (bot.y <= 60f) {
                    bot.state = PlayerState.PARACHUTING
                    bot.vy = -7f
                }
            } else if (bot.state == PlayerState.PARACHUTING) {
                bot.y += bot.vy * dt
                if (bot.y <= 0f) {
                    bot.y = 0f
                    bot.state = PlayerState.ALIVE
                }
            } else if (bot.state == PlayerState.ALIVE) {
                // 2. Bot Combat AI on ground
                // Storm movement: move towards safe zone center
                if (!safeZone.isInsideSafeZone(bot.x, bot.z)) {
                    bot.botMoveTargetX = safeZone.targetCenterX
                    bot.botMoveTargetZ = safeZone.targetCenterZ
                    bot.hp -= safeZone.damagePerSecond * dt
                    if (bot.hp <= 0f) {
                        bot.state = PlayerState.DEAD
                        alivePlayersCount = (alivePlayersCount - 1).coerceAtLeast(1)
                        killFeed.add(0, KillFeedEntry("Storm Barrier", bot.name, "Playzone", false, false))
                        continue
                    }
                }

                // Check distance to local player
                val distToPlayer = sqrt((bot.x - localPlayer.x).pow(2) + (bot.z - localPlayer.z).pow(2))
                val isEnemy = (bot.squadId != localPlayer.squadId)

                if (isEnemy && distToPlayer < 75f && localPlayer.state == PlayerState.ALIVE) {
                    // Turn to face local player
                    val dx = localPlayer.x - bot.x
                    val dz = localPlayer.z - bot.z
                    bot.yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()

                    // Bot firing logic
                    bot.botFireCooldownMs -= (dt * 1000).toLong()
                    if (bot.botFireCooldownMs <= 0L) {
                        bot.botFireCooldownMs = (600L / bot.botDifficulty).toLong()
                        // Bot fires at local player
                        val isHit = random.nextFloat() < (0.28f * bot.botDifficulty)
                        val tracerEnd = if (isHit) {
                            val isHeadshot = random.nextFloat() < 0.15f
                            val damage = (16f * bot.botDifficulty)
                            if (!adminGodMode) {
                                localPlayer.takeDamage(damage, isHeadshot)
                                soundManager.playHitMarker(isHeadshot)
                            }
                            Vector3D(localPlayer.x, localPlayer.y + 1.2f, localPlayer.z)
                        } else {
                            Vector3D(
                                localPlayer.x + (random.nextFloat() * 6f - 3f),
                                localPlayer.y + 1.2f,
                                localPlayer.z + (random.nextFloat() * 6f - 3f)
                            )
                        }
                        bulletTracers.add(
                            BulletTracer(
                                start = Vector3D(bot.x, bot.y + 1.2f, bot.z),
                                end = tracerEnd,
                                ageMs = 0L,
                                color = Color(0xFFFF5252)
                            )
                        )
                        soundManager.playGunshot("rifle")

                        if (localPlayer.state == PlayerState.DEAD) {
                            handlePlayerElimination(localPlayer, bot.name, "AK-47", false)
                        }
                    }

                    // Bot tactical movement: strafe or advance
                    val radYaw = Math.toRadians(bot.yaw.toDouble())
                    bot.x += sin(radYaw).toFloat() * 3.5f * dt
                    bot.z += cos(radYaw).toFloat() * 3.5f * dt
                } else {
                    // Bot wandering / simulated skirmish between bots
                    if (random.nextFloat() < 0.008f && alivePlayersCount > 2) {
                        // Background bot eliminated by another bot
                        val otherBot = players.filter { it.isBot && it.id != bot.id && it.state == PlayerState.ALIVE && it.squadId != bot.squadId }.randomOrNull()
                        if (otherBot != null) {
                            otherBot.state = PlayerState.DEAD
                            alivePlayersCount = (alivePlayersCount - 1).coerceAtLeast(1)
                            val weapons = listOf("M4A1", "AK-47", "MP40", "AWM", "SPAS-12")
                            val isHead = random.nextBoolean()
                            killFeed.add(0, KillFeedEntry(bot.name, otherBot.name, weapons.random(), isHead, false))
                            if (killFeed.size > 8) killFeed.removeAt(killFeed.size - 1)
                        }
                    }
                }
            }
        }
    }

    private fun updateVehicles(dt: Float) {
        for (v in vehicles) {
            if (v.isOccupied()) {
                // Apply steering & acceleration
                v.yaw += v.steeringInput * v.type.handling * 45f * dt
                val targetSpeed = v.throttleInput * v.type.maxSpeed
                v.currentSpeed += (targetSpeed - v.currentSpeed) * 4f * dt

                val radYaw = Math.toRadians(v.yaw.toDouble())
                val vx = sin(radYaw).toFloat() * v.currentSpeed * 0.28f
                val vz = cos(radYaw).toFloat() * v.currentSpeed * 0.28f

                v.x += vx * dt
                v.z += vz * dt

                // Update driver position
                if (v.driverId == localPlayer.id) {
                    localPlayer.x = v.x
                    localPlayer.z = v.z
                    localPlayer.yaw = v.yaw
                }

                // Roadkill detection: hitting bots with vehicle!
                if (abs(v.currentSpeed) > 25f) {
                    for (bot in players) {
                        if (bot.isBot && bot.state == PlayerState.ALIVE && bot.squadId != localPlayer.squadId) {
                            val dist = sqrt((bot.x - v.x).pow(2) + (bot.z - v.z).pow(2))
                            if (dist < 3.2f) {
                                bot.hp = 0f
                                bot.state = PlayerState.DEAD
                                localPlayer.kills++
                                alivePlayersCount = (alivePlayersCount - 1).coerceAtLeast(1)
                                killFeed.add(0, KillFeedEntry(localPlayer.name, bot.name, "Vehicular Impact", false, true))
                                soundManager.playExplosion()
                                particles.add(Particle(Vector3D(bot.x, 1f, bot.z), Vector3D(0f, 3f, 0f), 0L, 600L, 2.5f, Color(0xFFFF1744)))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateSafeZone(dt: Float) {
        safeZone.timeRemainingSec -= dt
        if (safeZone.timeRemainingSec <= 0f) {
            if (!safeZone.isShrinking) {
                // Start shrinking
                safeZone.isShrinking = true
                safeZone.timeRemainingSec = 35f // 35 seconds to collapse
            } else {
                // Shrink complete, start next phase
                safeZone.phase++
                safeZone.isShrinking = false
                safeZone.currentCenterX = safeZone.targetCenterX
                safeZone.currentCenterZ = safeZone.targetCenterZ
                safeZone.currentRadius = safeZone.targetRadius
                safeZone.timeRemainingSec = 55f

                // Pick next smaller circle
                safeZone.targetRadius = (safeZone.currentRadius * 0.6f).coerceAtLeast(30f)
                val maxOffset = (safeZone.currentRadius - safeZone.targetRadius) * 0.7f
                safeZone.targetCenterX += (random.nextFloat() * maxOffset * 2f) - maxOffset
                safeZone.targetCenterZ += (random.nextFloat() * maxOffset * 2f) - maxOffset
                safeZone.damagePerSecond += 1.5f
            }
        }

        // Gradually interpolate circle if shrinking
        if (safeZone.isShrinking) {
            val progress = dt / 35f
            safeZone.currentRadius = (safeZone.currentRadius + (safeZone.targetRadius - safeZone.currentRadius) * progress * 2.5f)
            safeZone.currentCenterX = (safeZone.currentCenterX + (safeZone.targetCenterX - safeZone.currentCenterX) * progress * 2.5f)
            safeZone.currentCenterZ = (safeZone.currentCenterZ + (safeZone.targetCenterZ - safeZone.currentCenterZ) * progress * 2.5f)
        }
    }

    private fun updateAirdrop(dt: Float) {
        val drop = airdropCrate ?: return
        if (drop.isLanding) {
            drop.y -= 12f * dt
            if (drop.y <= 0f) {
                drop.y = 0f
                drop.isLanding = false
                drop.hasLanded = true
                soundManager.playExplosion()
            }
        }
    }

    private fun updateEffects(dt: Float) {
        // Tracers
        val tracerIter = bulletTracers.iterator()
        while (tracerIter.hasNext()) {
            val t = tracerIter.next()
            val newAge = t.ageMs + (dt * 1000).toLong()
            if (newAge >= t.maxAgeMs) {
                tracerIter.remove()
            }
        }

        // Particles
        val partIter = particles.iterator()
        while (partIter.hasNext()) {
            val p = partIter.next()
            p.ageMs += (dt * 1000).toLong()
            p.pos = p.pos + (p.vel * dt)
            if (p.ageMs >= p.maxAgeMs) {
                partIter.remove()
            }
        }

        // Damage numbers
        val dmgIter = damageNumbers.iterator()
        while (dmgIter.hasNext()) {
            val d = dmgIter.next()
            d.ageMs += (dt * 1000).toLong()
            if (d.ageMs >= d.maxAgeMs) {
                dmgIter.remove()
            }
        }
    }

    private fun updateCamera() {
        if (localPlayer.state == PlayerState.DRIVING) {
            val v = vehicles.firstOrNull { it.id == localPlayer.vehicleId }
            if (v != null) {
                val radYaw = Math.toRadians(v.yaw.toDouble())
                camera.x = v.x - sin(radYaw).toFloat() * 6.5f
                camera.y = v.y + 3.2f
                camera.z = v.z - cos(radYaw).toFloat() * 6.5f
                camera.yaw = v.yaw
                camera.pitch = 12f
                camera.fov = 75f
                return
            }
        }

        // Player camera (First/Third person perspective)
        camera.x = localPlayer.x
        camera.y = when (localPlayer.state) {
            PlayerState.SKYDIVING, PlayerState.PARACHUTING -> localPlayer.y + 1.6f
            else -> if (localPlayer.isCrouching) 1.1f else if (localPlayer.isProne) 0.5f else 1.75f
        }
        camera.z = localPlayer.z
        camera.yaw = localPlayer.yaw
        camera.pitch = localPlayer.pitch

        // Scope FOV zoom
        val weapon = localPlayer.getEquippedWeapon()
        camera.fov = if (localPlayer.isAiming && weapon != null) {
            when (weapon.equippedScope) {
                ScopeType.RED_DOT -> 55f
                ScopeType.SCOPE_2X -> 42f
                ScopeType.SCOPE_4X -> 24f
                ScopeType.SCOPE_8X -> 12f
                else -> 65f
            }
        } else {
            75f
        }
    }

    // Local Player Shooting Action
    fun fireWeapon(): Boolean {
        if (localPlayer.state != PlayerState.ALIVE) return false
        val weapon = localPlayer.getEquippedWeapon() ?: return false

        if (weapon.currentAmmo <= 0 && !adminInfiniteAmmo) {
            reloadWeapon()
            return false
        }

        if (!adminInfiniteAmmo) {
            weapon.currentAmmo--
        }

        soundManager.playGunshot(
            when (weapon.category) {
                WeaponCategory.SNIPER -> "sniper"
                WeaponCategory.SHOTGUN -> "shotgun"
                WeaponCategory.SMG -> "smg"
                else -> "rifle"
            }
        )

        // Camera kick recoil
        val recoilMult = if (localPlayer.isCrouching) 0.7f else if (localPlayer.isProne) 0.5f else 1.0f
        localPlayer.pitch = (localPlayer.pitch + weapon.recoil * 35f * recoilMult).coerceAtMost(60f)

        // Raycast from camera along look vector
        val radYaw = Math.toRadians(camera.yaw.toDouble())
        val radPitch = Math.toRadians(camera.pitch.toDouble())
        val dirX = (sin(radYaw) * cos(radPitch)).toFloat()
        val dirY = -sin(radPitch).toFloat()
        val dirZ = (cos(radYaw) * cos(radPitch)).toFloat()
        val rayDir = Vector3D(dirX, dirY, dirZ).normalized()

        var hitTarget: Player? = null
        var hitDist = weapon.range
        var isHeadshot = false

        for (bot in players) {
            if (!bot.isBot || bot.state == PlayerState.DEAD || bot.squadId == localPlayer.squadId) continue
            // Simple ray-sphere collision test
            val toBot = Vector3D(bot.x - camera.x, (bot.y + 1.2f) - camera.y, bot.z - camera.z)
            val projDist = toBot.x * rayDir.x + toBot.y * rayDir.y + toBot.z * rayDir.z

            if (projDist > 0f && projDist < hitDist) {
                val closestPoint = Vector3D(camera.x + rayDir.x * projDist, camera.y + rayDir.y * projDist, camera.z + rayDir.z * projDist)
                val distToCenter = sqrt((closestPoint.x - bot.x).pow(2) + (closestPoint.z - bot.z).pow(2))
                val yDiff = closestPoint.y - bot.y

                if (distToCenter < 1.1f && yDiff in 0f..2.0f) {
                    hitDist = projDist
                    hitTarget = bot
                    isHeadshot = (yDiff >= 1.55f)
                }
            }
        }

        val startTracer = Vector3D(camera.x + rayDir.x * 0.8f, camera.y + rayDir.y * 0.8f - 0.2f, camera.z + rayDir.z * 0.8f)
        val endTracer = if (hitTarget != null) {
            Vector3D(hitTarget.x, hitTarget.y + (if (isHeadshot) 1.7f else 1.2f), hitTarget.z)
        } else {
            Vector3D(camera.x + rayDir.x * weapon.range, camera.y + rayDir.y * weapon.range, camera.z + rayDir.z * weapon.range)
        }

        bulletTracers.add(BulletTracer(startTracer, endTracer, 0L, 160L, Color(0xFFFFD54F)))

        // Process damage
        if (hitTarget != null) {
            val dmg = weapon.getEffectiveDamage(isHeadshot).toInt()
            hitTarget.takeDamage(dmg.toFloat(), isHeadshot)
            localPlayer.damageDealt += dmg
            if (isHeadshot) localPlayer.headshots++

            _hitmarkerTrigger.value = isHeadshot
            soundManager.playHitMarker(isHeadshot)

            damageNumbers.add(DamageNumber(dmg, isHeadshot, hitTarget.x, hitTarget.y + 1.8f, hitTarget.z))

            if (hitTarget.state == PlayerState.DEAD) {
                localPlayer.kills++
                alivePlayersCount = (alivePlayersCount - 1).coerceAtLeast(1)
                killFeed.add(0, KillFeedEntry(localPlayer.name, hitTarget.name, weapon.name, isHeadshot, true))
            }
        }

        return true
    }

    fun reloadWeapon() {
        val weapon = localPlayer.getEquippedWeapon() ?: return
        if (weapon.currentAmmo >= weapon.getMaxAmmo() || weapon.reserveAmmo <= 0) return
        localPlayer.isReloading = true
        soundManager.playReload()
        val needed = weapon.getMaxAmmo() - weapon.currentAmmo
        val toLoad = min(needed, weapon.reserveAmmo)
        weapon.currentAmmo += toLoad
        weapon.reserveAmmo -= toLoad
        localPlayer.isReloading = false
    }

    fun useMedkit(): Boolean {
        if (localPlayer.medkitsCount > 0 && localPlayer.hp < localPlayer.maxHp) {
            localPlayer.medkitsCount--
            localPlayer.hp = (localPlayer.hp + 75f).coerceAtMost(localPlayer.maxHp)
            soundManager.playReload()
            return true
        }
        return false
    }

    fun useInhaler(): Boolean {
        if (localPlayer.inhalersCount > 0 && localPlayer.ep < localPlayer.maxEp) {
            localPlayer.inhalersCount--
            localPlayer.ep = (localPlayer.ep + 50f).coerceAtMost(localPlayer.maxEp)
            soundManager.playReload()
            return true
        }
        return false
    }

    fun activateCharacterAbility(): Boolean {
        if (localPlayer.abilityCooldownTimerSec <= 0f) {
            localPlayer.abilityCooldownTimerSec = localPlayer.character.abilityCooldownSec.toFloat()
            localPlayer.activeAbilityTimerSec = 6.0f
            soundManager.playReload()
            return true
        }
        return false
    }

    fun toggleVehicle(vehicle: GameVehicle? = null) {
        if (localPlayer.state == PlayerState.DRIVING) {
            val v = vehicles.firstOrNull { it.id == localPlayer.vehicleId }
            v?.driverId = null
            localPlayer.vehicleId = null
            localPlayer.state = PlayerState.ALIVE
            localPlayer.x += 2.5f
        } else {
            val v = vehicle ?: vehicles.minByOrNull { sqrt((it.x - localPlayer.x).pow(2) + (it.z - localPlayer.z).pow(2)) }
            if (v != null) {
                val dist = sqrt((v.x - localPlayer.x).pow(2) + (v.z - localPlayer.z).pow(2))
                if (dist < 4.5f) {
                    v.driverId = localPlayer.id
                    localPlayer.vehicleId = v.id
                    localPlayer.state = PlayerState.DRIVING
                    soundManager.playVehicleHorn()
                }
            }
        }
    }

    fun summonAirdrop() {
        val dropX = localPlayer.x + (sin(Math.toRadians(localPlayer.yaw.toDouble())).toFloat() * 25f)
        val dropZ = localPlayer.z + (cos(Math.toRadians(localPlayer.yaw.toDouble())).toFloat() * 25f)
        airdropCrate = AirdropCrate(
            id = "airdrop_admin",
            x = dropX,
            y = 120f,
            z = dropZ,
            isLanding = true,
            hasLanded = false
        )
        soundManager.playExplosion()
    }

    private fun handlePlayerElimination(victim: Player, killer: String, weapon: String, isHead: Boolean) {
        alivePlayersCount = (alivePlayersCount - 1).coerceAtLeast(1)
        killFeed.add(0, KillFeedEntry(killer, victim.name, weapon, isHead, false))
        if (victim.id == localPlayer.id) {
            matchRank = alivePlayersCount + 1
            isGameOver = true
            isBooyah = false
        }
    }

    private fun checkGameStatus() {
        if (!isGameOver && isMatchActive) {
            if (localPlayer.state == PlayerState.DEAD) {
                isGameOver = true
                isBooyah = false
            } else if (alivePlayersCount <= 1 && localPlayer.state != PlayerState.DEAD) {
                isGameOver = true
                isBooyah = true
                matchRank = 1
                soundManager.playVictoryFanfare()
            }
        }
    }
}
