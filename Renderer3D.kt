package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.*
import kotlin.math.*

data class BulletTracer(
    val start: Vector3D,
    val end: Vector3D,
    val ageMs: Long,
    val maxAgeMs: Long = 180L,
    val color: Color = Color(0xFFFFEB3B)
)

data class Particle(
    var pos: Vector3D,
    var vel: Vector3D,
    var ageMs: Long,
    val maxAgeMs: Long,
    val size: Float,
    val color: Color
)

abstract class Renderable(val depth: Float) : Comparable<Renderable> {
    override fun compareTo(other: Renderable): Int = other.depth.compareTo(this.depth) // Back to front
    abstract fun render(drawScope: DrawScope)
}

class Renderer3D {

    fun renderScene(
        drawScope: DrawScope,
        camera: Camera3D,
        structures: List<MapStructure>,
        trees: List<MapTree>,
        rocks: List<MapRock>,
        vehicles: List<GameVehicle>,
        players: List<Player>,
        airdrop: AirdropCrate?,
        dropshipPos: Vector3D?,
        safeZone: SafeZone,
        bulletTracers: List<BulletTracer>,
        particles: List<Particle>,
        groundLoot: List<GroundLoot>,
        localPlayer: Player
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Draw Sky & Ground background
        drawSkyAndHorizon(drawScope, camera, width, height)

        // 2. Collect all 3D renderables
        val renderList = mutableListOf<Renderable>()

        // Safe zone ring segments
        addSafeZoneRenderables(renderList, camera, safeZone, width, height)

        // Terrain grid lines / ground landmarks
        addTerrainGrid(renderList, camera, width, height)

        // Dropship in sky
        if (dropshipPos != null) {
            addDropshipRenderable(renderList, camera, dropshipPos, width, height)
        }

        // Airdrop
        if (airdrop != null) {
            addAirdropRenderable(renderList, camera, airdrop, width, height)
        }

        // Structures
        for (struct in structures) {
            val dist = sqrt((struct.x - camera.x).pow(2) + (struct.z - camera.z).pow(2))
            if (dist < 380f) {
                addStructureRenderables(renderList, camera, struct, width, height)
            }
        }

        // Trees
        for (tree in trees) {
            val dist = sqrt((tree.x - camera.x).pow(2) + (tree.z - camera.z).pow(2))
            if (dist < 320f) {
                addTreeRenderables(renderList, camera, tree, width, height)
            }
        }

        // Rocks
        for (rock in rocks) {
            val dist = sqrt((rock.x - camera.x).pow(2) + (rock.z - camera.z).pow(2))
            if (dist < 260f) {
                addRockRenderables(renderList, camera, rock, width, height)
            }
        }

        // Ground Loot
        for (loot in groundLoot) {
            val dist = sqrt((loot.x - camera.x).pow(2) + (loot.z - camera.z).pow(2))
            if (dist < 90f) {
                addLootRenderables(renderList, camera, loot, width, height)
            }
        }

        // Vehicles
        for (veh in vehicles) {
            val dist = sqrt((veh.x - camera.x).pow(2) + (veh.z - camera.z).pow(2))
            if (dist < 350f) {
                addVehicleRenderables(renderList, camera, veh, width, height)
            }
        }

        // Players & Bots
        for (p in players) {
            if (p.id == localPlayer.id && localPlayer.state != PlayerState.DROPSHIP && localPlayer.state != PlayerState.PARACHUTING && localPlayer.state != PlayerState.DRIVING) {
                // In 1st person or third person close, skip local body or draw third person model
                continue
            }
            if (p.state == PlayerState.DEAD) continue
            val dist = sqrt((p.x - camera.x).pow(2) + (p.z - camera.z).pow(2))
            if (dist < 400f) {
                addPlayerRenderables(renderList, camera, p, width, height)
            }
        }

        // Bullet Tracers
        for (tracer in bulletTracers) {
            addTracerRenderable(renderList, camera, tracer, width, height)
        }

        // Particles / Explosions
        for (part in particles) {
            addParticleRenderable(renderList, camera, part, width, height)
        }

        // 3. Sort back to front (Painter's algorithm)
        renderList.sort()

        // 4. Render all items
        for (renderable in renderList) {
            renderable.render(drawScope)
        }
    }

    private fun drawSkyAndHorizon(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val horizonOffset = (camera.pitch / camera.fov) * height
        val horizonY = (height * 0.5f) + horizonOffset

        // Sky gradient
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0D1B2A), // Deep twilight space
                    Color(0xFF1B263B), // Midnight blue
                    Color(0xFF415A77), // Atmospheric haze
                    Color(0xFFE07A5F)  // Warm sunset horizon glow
                ),
                startY = 0f,
                endY = horizonY.coerceAtLeast(0f)
            ),
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY.coerceIn(0f, height))
        )

        // Distant mountains silhouette on horizon
        if (horizonY > -50f && horizonY < height + 100f) {
            val mountainPath = Path()
            mountainPath.moveTo(0f, horizonY)
            val step = width / 8f
            for (i in 0..8) {
                val peakHeight = if (i % 2 == 1) 35f else 12f
                mountainPath.lineTo(i * step, horizonY - peakHeight)
            }
            mountainPath.lineTo(width, horizonY)
            mountainPath.close()
            drawScope.drawPath(
                mountainPath,
                Color(0x772A344A)
            )
        }

        // Ground gradient
        if (horizonY < height) {
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF386641), // Lush island grass
                        Color(0xFF2C5135), // Darker terrain
                        Color(0xFF1E3824)  // Close ground shadow
                    ),
                    startY = horizonY.coerceAtLeast(0f),
                    endY = height
                ),
                topLeft = Offset(0f, horizonY.coerceAtLeast(0f)),
                size = Size(width, height - horizonY.coerceAtLeast(0f))
            )
        }
    }

    private fun addTerrainGrid(renderList: MutableList<Renderable>, camera: Camera3D, width: Float, height: Float) {
        // Subtle perspective ground grid to give high-speed motion feel
        val gridSize = 40f
        val startX = (floor(camera.x / gridSize) * gridSize) - 160f
        val startZ = (floor(camera.z / gridSize) * gridSize) - 160f

        for (ix in 0..8) {
            val gx = startX + (ix * gridSize)
            val p1 = camera.project(Vector3D(gx, 0f, startZ), width, height)
            val p2 = camera.project(Vector3D(gx, 0f, startZ + 320f), width, height)

            if (p1.isVisible && p2.isVisible) {
                val avgDepth = (p1.depth + p2.depth) * 0.5f
                renderList.add(object : Renderable(avgDepth) {
                    override fun render(drawScope: DrawScope) {
                        drawScope.drawLine(
                            color = Color(0x224ADE80),
                            start = Offset(p1.screenX, p1.screenY),
                            end = Offset(p2.screenX, p2.screenY),
                            strokeWidth = 1.2f
                        )
                    }
                })
            }
        }
    }

    private fun addSafeZoneRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        safeZone: SafeZone,
        width: Float,
        height: Float
    ) {
        val numSegments = 24
        val stepRad = (2 * PI / numSegments).toFloat()

        for (i in 0 until numSegments) {
            val angle1 = i * stepRad
            val angle2 = (i + 1) * stepRad

            val x1 = safeZone.currentCenterX + cos(angle1) * safeZone.currentRadius
            val z1 = safeZone.currentCenterZ + sin(angle1) * safeZone.currentRadius
            val x2 = safeZone.currentCenterX + cos(angle2) * safeZone.currentRadius
            val z2 = safeZone.currentCenterZ + sin(angle2) * safeZone.currentRadius

            val bot1 = camera.project(Vector3D(x1, 0f, z1), width, height)
            val bot2 = camera.project(Vector3D(x2, 0f, z2), width, height)
            val top1 = camera.project(Vector3D(x1, 35f, z1), width, height)
            val top2 = camera.project(Vector3D(x2, 35f, z2), width, height)

            if (bot1.isVisible || bot2.isVisible) {
                val avgDepth = (bot1.depth + bot2.depth) * 0.5f
                renderList.add(object : Renderable(avgDepth) {
                    override fun render(drawScope: DrawScope) {
                        val path = Path().apply {
                            moveTo(bot1.screenX, bot1.screenY)
                            lineTo(top1.screenX, top1.screenY)
                            lineTo(top2.screenX, top2.screenY)
                            lineTo(bot2.screenX, bot2.screenY)
                            close()
                        }
                        drawScope.drawPath(path, Color(0x3300B0FF), style = Fill)
                        drawScope.drawLine(Color(0xFF00E5FF), Offset(bot1.screenX, bot1.screenY), Offset(bot2.screenX, bot2.screenY), strokeWidth = 2.5f)
                        drawScope.drawLine(Color(0xAA00E5FF), Offset(top1.screenX, top1.screenY), Offset(top2.screenX, top2.screenY), strokeWidth = 1.5f)
                    }
                })
            }
        }
    }

    private fun addStructureRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        s: MapStructure,
        width: Float,
        height: Float
    ) {
        val halfW = s.width * 0.5f
        val halfD = s.depth * 0.5f
        val h = s.height

        // 8 corners of the 3D building box
        val c0 = camera.project(Vector3D(s.x - halfW, s.y, s.z - halfD), width, height)
        val c1 = camera.project(Vector3D(s.x + halfW, s.y, s.z - halfD), width, height)
        val c2 = camera.project(Vector3D(s.x + halfW, s.y, s.z + halfD), width, height)
        val c3 = camera.project(Vector3D(s.x - halfW, s.y, s.z + halfD), width, height)

        val t0 = camera.project(Vector3D(s.x - halfW, s.y + h, s.z - halfD), width, height)
        val t1 = camera.project(Vector3D(s.x + halfW, s.y + h, s.z - halfD), width, height)
        val t2 = camera.project(Vector3D(s.x + halfW, s.y + h, s.z + halfD), width, height)
        val t3 = camera.project(Vector3D(s.x - halfW, s.y + h, s.z + halfD), width, height)

        val avgDepth = (c0.depth + c1.depth + c2.depth + c3.depth) * 0.25f

        renderList.add(object : Renderable(avgDepth) {
            override fun render(drawScope: DrawScope) {
                val wallColor = Color(s.colorHex)
                val roofColor = Color(s.roofColorHex)
                val strokeColor = Color(0xFF1E293B)

                // Draw visible walls
                fun drawQuad(p1: ProjectedPoint, p2: ProjectedPoint, p3: ProjectedPoint, p4: ProjectedPoint, color: Color) {
                    if (p1.isVisible || p2.isVisible || p3.isVisible || p4.isVisible) {
                        val p = Path().apply {
                            moveTo(p1.screenX, p1.screenY)
                            lineTo(p2.screenX, p2.screenY)
                            lineTo(p3.screenX, p3.screenY)
                            lineTo(p4.screenX, p4.screenY)
                            close()
                        }
                        drawScope.drawPath(p, color)
                        drawScope.drawPath(p, strokeColor, style = Stroke(width = 1.5f))
                    }
                }

                // Front, sides, back, and roof
                drawQuad(c0, c1, t1, t0, wallColor.copy(alpha = 0.95f))
                drawQuad(c1, c2, t2, t1, wallColor.copy(alpha = 0.85f))
                drawQuad(c2, c3, t3, t2, wallColor.copy(alpha = 0.75f))
                drawQuad(c3, c0, t0, t3, wallColor.copy(alpha = 0.9f))
                drawQuad(t0, t1, t2, t3, roofColor)
            }
        })
    }

    private fun addTreeRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        t: MapTree,
        width: Float,
        height: Float
    ) {
        val base = camera.project(Vector3D(t.x, t.y, t.z), width, height)
        val top = camera.project(Vector3D(t.x, t.y + t.height, t.z), width, height)

        if (!base.isVisible && !top.isVisible) return

        renderList.add(object : Renderable(base.depth) {
            override fun render(drawScope: DrawScope) {
                val treeHeightPx = abs(base.screenY - top.screenY)
                val foliageWidthPx = treeHeightPx * 0.65f

                // Trunk
                drawScope.drawLine(
                    color = Color(0xFF5D4037),
                    start = Offset(base.screenX, base.screenY),
                    end = Offset(top.screenX, top.screenY + treeHeightPx * 0.4f),
                    strokeWidth = (treeHeightPx * 0.12f).coerceAtLeast(2f)
                )

                // Conical Foliage
                val foliagePath = Path().apply {
                    moveTo(top.screenX, top.screenY)
                    lineTo(top.screenX - foliageWidthPx * 0.5f, top.screenY + treeHeightPx * 0.75f)
                    lineTo(top.screenX + foliageWidthPx * 0.5f, top.screenY + treeHeightPx * 0.75f)
                    close()
                }
                drawScope.drawPath(foliagePath, Color(0xFF2E7D32))
                drawScope.drawPath(foliagePath, Color(0xFF1B5E20), style = Stroke(width = 1.5f))
            }
        })
    }

    private fun addRockRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        r: MapRock,
        width: Float,
        height: Float
    ) {
        val center = camera.project(Vector3D(r.x, r.y, r.z), width, height)
        if (!center.isVisible) return

        renderList.add(object : Renderable(center.depth) {
            override fun render(drawScope: DrawScope) {
                val radiusPx = (450f / center.depth.coerceAtLeast(1f)) * r.size
                drawScope.drawCircle(
                    color = Color(0xFF607D8B),
                    radius = radiusPx,
                    center = Offset(center.screenX, center.screenY)
                )
                drawScope.drawCircle(
                    color = Color(0xFF37474F),
                    radius = radiusPx,
                    center = Offset(center.screenX, center.screenY),
                    style = Stroke(width = 2f)
                )
            }
        })
    }

    private fun addLootRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        loot: GroundLoot,
        width: Float,
        height: Float
    ) {
        val proj = camera.project(Vector3D(loot.x, loot.y + 0.3f, loot.z), width, height)
        if (!proj.isVisible) return

        renderList.add(object : Renderable(proj.depth) {
            override fun render(drawScope: DrawScope) {
                val sizePx = (300f / proj.depth.coerceAtLeast(1f)).coerceIn(8f, 32f)
                val lootColor = when (loot.type) {
                    ItemType.WEAPON -> Color(0xFFFF9800)
                    ItemType.HELMET, ItemType.VEST -> Color(0xFF00E5FF)
                    ItemType.MEDKIT, ItemType.INHALER -> Color(0xFF4CAF50)
                    ItemType.AMMO -> Color(0xFFFFEB3B)
                    else -> Color(0xFFE0E0E0)
                }

                // Glowing beacon ring on ground
                drawScope.drawCircle(
                    color = lootColor.copy(alpha = 0.4f),
                    radius = sizePx * 1.5f,
                    center = Offset(proj.screenX, proj.screenY)
                )
                // Loot box / crate
                drawScope.drawRect(
                    color = lootColor,
                    topLeft = Offset(proj.screenX - sizePx * 0.5f, proj.screenY - sizePx * 0.5f),
                    size = Size(sizePx, sizePx)
                )
            }
        })
    }

    private fun addVehicleRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        v: GameVehicle,
        width: Float,
        height: Float
    ) {
        val center = camera.project(Vector3D(v.x, v.y + 0.7f, v.z), width, height)
        if (!center.isVisible) return

        renderList.add(object : Renderable(center.depth) {
            override fun render(drawScope: DrawScope) {
                val scale = (400f / center.depth.coerceAtLeast(1f)).coerceIn(10f, 90f)
                val carWidth = scale * (v.type.length * 0.5f)
                val carHeight = scale * 0.7f
                val color = Color(v.type.bodyColorHex)

                // Vehicle body
                val bodyPath = Path().apply {
                    moveTo(center.screenX - carWidth * 0.5f, center.screenY + carHeight * 0.3f)
                    lineTo(center.screenX - carWidth * 0.3f, center.screenY - carHeight * 0.6f)
                    lineTo(center.screenX + carWidth * 0.3f, center.screenY - carHeight * 0.6f)
                    lineTo(center.screenX + carWidth * 0.5f, center.screenY + carHeight * 0.3f)
                    close()
                }
                drawScope.drawPath(bodyPath, color)
                drawScope.drawPath(bodyPath, Color.Black, style = Stroke(width = 2f))

                // Wheels
                val wheelRadius = carHeight * 0.35f
                drawScope.drawCircle(
                    Color(0xFF212121),
                    wheelRadius,
                    Offset(center.screenX - carWidth * 0.35f, center.screenY + carHeight * 0.4f)
                )
                drawScope.drawCircle(
                    Color(0xFF212121),
                    wheelRadius,
                    Offset(center.screenX + carWidth * 0.35f, center.screenY + carHeight * 0.4f)
                )

                // Headlights / taillights
                drawScope.drawCircle(
                    Color(0xFFFFFF00),
                    wheelRadius * 0.4f,
                    Offset(center.screenX - carWidth * 0.45f, center.screenY)
                )
            }
        })
    }

    private fun addPlayerRenderables(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        p: Player,
        width: Float,
        height: Float
    ) {
        val base = camera.project(Vector3D(p.x, p.y, p.z), width, height)
        val head = camera.project(Vector3D(p.x, p.y + 1.8f, p.z), width, height)

        if (!base.isVisible && !head.isVisible) return

        renderList.add(object : Renderable(base.depth) {
            override fun render(drawScope: DrawScope) {
                val heightPx = abs(base.screenY - head.screenY).coerceIn(12f, 180f)
                val widthPx = heightPx * 0.45f

                // If player is skydiving or parachuting
                if (p.state == PlayerState.PARACHUTING) {
                    val canopyTop = Offset(head.screenX, head.screenY - heightPx * 0.8f)
                    val canopyWidth = widthPx * 2.5f

                    // Parachute Canopy
                    val canopyPath = Path().apply {
                        moveTo(canopyTop.x - canopyWidth * 0.5f, canopyTop.y)
                        quadraticTo(canopyTop.x, canopyTop.y - heightPx * 0.4f, canopyTop.x + canopyWidth * 0.5f, canopyTop.y)
                        close()
                    }
                    drawScope.drawPath(canopyPath, Color(0xFFFF5722))
                    drawScope.drawLine(Color.White, Offset(head.screenX, head.screenY), Offset(canopyTop.x - canopyWidth * 0.4f, canopyTop.y), strokeWidth = 1f)
                    drawScope.drawLine(Color.White, Offset(head.screenX, head.screenY), Offset(canopyTop.x + canopyWidth * 0.4f, canopyTop.y), strokeWidth = 1f)
                }

                // Head
                val headRadius = heightPx * 0.16f
                drawScope.drawCircle(
                    color = Color(0xFFFFCC80),
                    radius = headRadius,
                    center = Offset(head.screenX, head.screenY + headRadius)
                )
                // Combat Helmet
                drawScope.drawCircle(
                    color = Color(0xFF263238),
                    radius = headRadius * 1.05f,
                    center = Offset(head.screenX, head.screenY + headRadius * 0.7f)
                )

                // Torso / Tactical Vest
                val vestColor = if (p.isBot) Color(0xFFE53935) else Color(p.teamColorHex)
                drawScope.drawRect(
                    color = vestColor,
                    topLeft = Offset(head.screenX - widthPx * 0.4f, head.screenY + headRadius * 2f),
                    size = Size(widthPx * 0.8f, heightPx * 0.45f)
                )

                // Legs
                drawScope.drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(head.screenX - widthPx * 0.2f, head.screenY + headRadius * 2f + heightPx * 0.45f),
                    end = Offset(base.screenX - widthPx * 0.25f, base.screenY),
                    strokeWidth = widthPx * 0.25f
                )
                drawScope.drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(head.screenX + widthPx * 0.2f, head.screenY + headRadius * 2f + heightPx * 0.45f),
                    end = Offset(base.screenX + widthPx * 0.25f, base.screenY),
                    strokeWidth = widthPx * 0.25f
                )

                // Gun held in hands
                drawScope.drawLine(
                    color = Color(0xFF212121),
                    start = Offset(head.screenX, head.screenY + heightPx * 0.35f),
                    end = Offset(head.screenX + widthPx * 0.6f, head.screenY + heightPx * 0.32f),
                    strokeWidth = widthPx * 0.18f
                )

                // Overhead health bar & name tag
                val barWidth = widthPx * 1.2f
                val hpRatio = (p.hp / p.maxHp).coerceIn(0f, 1f)
                val barTop = head.screenY - 14f

                drawScope.drawRect(
                    color = Color(0x99000000),
                    topLeft = Offset(head.screenX - barWidth * 0.5f, barTop),
                    size = Size(barWidth, 5f)
                )
                drawScope.drawRect(
                    color = if (hpRatio > 0.5f) Color(0xFF00E676) else Color(0xFFFF1744),
                    topLeft = Offset(head.screenX - barWidth * 0.5f, barTop),
                    size = Size(barWidth * hpRatio, 5f)
                )
            }
        })
    }

    private fun addDropshipRenderable(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        pos: Vector3D,
        width: Float,
        height: Float
    ) {
        val proj = camera.project(pos, width, height)
        if (!proj.isVisible) return

        renderList.add(object : Renderable(proj.depth) {
            override fun render(drawScope: DrawScope) {
                val scale = (1200f / proj.depth.coerceAtLeast(1f)).coerceIn(15f, 160f)
                val shipPath = Path().apply {
                    moveTo(proj.screenX - scale * 0.8f, proj.screenY)
                    lineTo(proj.screenX + scale * 0.8f, proj.screenY)
                    lineTo(proj.screenX, proj.screenY - scale * 0.25f)
                    close()
                }
                drawScope.drawPath(shipPath, Color(0xFF455A64))
                // Jet engine exhausts
                drawScope.drawCircle(Color(0xFFFF9800), scale * 0.12f, Offset(proj.screenX - scale * 0.5f, proj.screenY + 4f))
                drawScope.drawCircle(Color(0xFFFF9800), scale * 0.12f, Offset(proj.screenX + scale * 0.5f, proj.screenY + 4f))
            }
        })
    }

    private fun addAirdropRenderable(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        airdrop: AirdropCrate,
        width: Float,
        height: Float
    ) {
        val proj = camera.project(Vector3D(airdrop.x, airdrop.y, airdrop.z), width, height)
        if (!proj.isVisible) return

        renderList.add(object : Renderable(proj.depth) {
            override fun render(drawScope: DrawScope) {
                val sizePx = (500f / proj.depth.coerceAtLeast(1f)).coerceIn(12f, 75f)

                // Vertical red smoke beam reaching skywards
                drawScope.drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x00FF1744), Color(0x88FF1744), Color(0xFFFF1744)),
                        startY = proj.screenY - sizePx * 8f,
                        endY = proj.screenY
                    ),
                    start = Offset(proj.screenX, proj.screenY - sizePx * 8f),
                    end = Offset(proj.screenX, proj.screenY),
                    strokeWidth = sizePx * 0.35f
                )

                // Crate
                drawScope.drawRect(
                    color = Color(0xFFD50000),
                    topLeft = Offset(proj.screenX - sizePx * 0.5f, proj.screenY - sizePx * 0.5f),
                    size = Size(sizePx, sizePx)
                )
                // Yellow cross bands
                drawScope.drawLine(
                    color = Color(0xFFFFD600),
                    start = Offset(proj.screenX, proj.screenY - sizePx * 0.5f),
                    end = Offset(proj.screenX, proj.screenY + sizePx * 0.5f),
                    strokeWidth = sizePx * 0.2f
                )
            }
        })
    }

    private fun addTracerRenderable(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        t: BulletTracer,
        width: Float,
        height: Float
    ) {
        val p1 = camera.project(t.start, width, height)
        val p2 = camera.project(t.end, width, height)

        if (!p1.isVisible && !p2.isVisible) return

        val avgDepth = (p1.depth + p2.depth) * 0.5f
        renderList.add(object : Renderable(avgDepth) {
            override fun render(drawScope: DrawScope) {
                val alpha = (1f - (t.ageMs.toFloat() / t.maxAgeMs)).coerceIn(0f, 1f)
                drawScope.drawLine(
                    color = t.color.copy(alpha = alpha),
                    start = Offset(p1.screenX, p1.screenY),
                    end = Offset(p2.screenX, p2.screenY),
                    strokeWidth = 3f
                )
            }
        })
    }

    private fun addParticleRenderable(
        renderList: MutableList<Renderable>,
        camera: Camera3D,
        p: Particle,
        width: Float,
        height: Float
    ) {
        val proj = camera.project(p.pos, width, height)
        if (!proj.isVisible) return

        renderList.add(object : Renderable(proj.depth) {
            override fun render(drawScope: DrawScope) {
                val progress = (p.ageMs.toFloat() / p.maxAgeMs).coerceIn(0f, 1f)
                val alpha = (1f - progress)
                val radius = (350f / proj.depth.coerceAtLeast(1f)) * p.size * (1f + progress)
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = radius.coerceAtLeast(1.5f),
                    center = Offset(proj.screenX, proj.screenY)
                )
            }
        })
    }
}
