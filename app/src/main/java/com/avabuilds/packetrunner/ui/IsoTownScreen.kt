package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.IsoCamera
import com.avabuilds.packetrunner.game.IsoMath
import com.avabuilds.packetrunner.game.PathWalker
import com.avabuilds.packetrunner.game.Pathfinding
import com.avabuilds.packetrunner.game.TilePos
import com.avabuilds.packetrunner.game.Town
import com.avabuilds.packetrunner.game.Vec2
import com.avabuilds.packetrunner.game.pointInPolygon
import com.avabuilds.packetrunner.ui.sprites.CanvasSpriteProvider
import com.avabuilds.packetrunner.ui.sprites.Sprite
import com.avabuilds.packetrunner.ui.sprites.SpriteKey
import com.avabuilds.packetrunner.ui.sprites.SpriteProvider
import com.avabuilds.packetrunner.ui.sprites.buildingSilhouette
import com.avabuilds.packetrunner.ui.sprites.labelHeightInTiles
import com.avabuilds.packetrunner.ui.sprites.spriteKey
import kotlin.math.roundToInt

private fun Offset.toVec2() = Vec2(x, y)
private fun Vec2.toOffset() = Offset(x, y)

/**
 * The isometric town. Tap the ground to walk there, tap a building to walk to its door,
 * drag to pan, pinch to zoom. Everything that is not drawing (grid, math, pathfinding, walking)
 * lives in the `game` package, and every picture comes from the [SpriteProvider].
 * [controller] holds the player and carries out walk orders; [targetBuildingId] marks the
 * building the player is on the way to.
 */
@Composable
fun IsoTownScreen(controller: TownController, targetBuildingId: String?, modifier: Modifier = Modifier) {
    val town = controller.town
    val grid = town.grid
    val sprites: SpriteProvider = remember { CanvasSpriteProvider() }
    val textMeasurer = rememberTextMeasurer()
    val tileWidth = with(LocalDensity.current) { 36.dp.toPx() }
    val iso = remember(grid, tileWidth) { IsoMath.centeredOn(grid, tileWidth, tileWidth / 2f) }

    var camera by remember { mutableStateOf(IsoCamera()) }

    // Each walk order starts a new walk from wherever the player is right now.
    LaunchedEffect(controller.walkRequest) {
        val request = controller.walkRequest ?: return@LaunchedEffect
        val from = TilePos(controller.playerPos.x.roundToInt(), controller.playerPos.y.roundToInt())
        val route = Pathfinding.findPath(grid, from, request.tile)
        if (route.isEmpty()) return@LaunchedEffect

        controller.arrivedAt = null
        controller.trail = route
        val walker = PathWalker(controller.playerPos, route, controller.walkSpeed)
        var lastFrame = withFrameNanos { it }
        while (!walker.finished) {
            val now = withFrameNanos { it }
            walker.advance((now - lastFrame) / 1_000_000_000f)
            lastFrame = now
            controller.playerPos = walker.position
            controller.trail = walker.remaining
        }
        controller.trail = emptyList()
        controller.arrivedAt = town.buildings.firstOrNull { it.door == request.tile }
    }

    Box(modifier.fillMaxSize().background(PR.Bg)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val viewCenter = Vec2(size.width / 2f, size.height / 2f)
                        camera = camera.zoomAbout(centroid.toVec2(), zoom, viewCenter).panBy(pan.toVec2())
                    }
                }
                .pointerInput(town, iso) {
                    detectTapGestures { tap ->
                        val viewCenter = Vec2(size.width / 2f, size.height / 2f)
                        val point = tap.toVec2()
                        val tw = tileWidth * camera.scale

                        // A tap on a building's drawn walls or roof walks to its door. Check the
                        // buildings in front first, because they cover the ones behind them.
                        val building = town.buildings.sortedByDescending { it.depth }.firstOrNull { b ->
                            val c = camera.worldToScreen(iso.tileCenter(b.center.x, b.center.y), viewCenter)
                            pointInPolygon(buildingSilhouette(b.type, c, tw, b.size), point)
                        }
                        if (building != null) {
                            controller.walkTo(building.door)
                            return@detectTapGestures
                        }
                        val tile = iso.tileAt(camera.screenToWorld(point, viewCenter))
                        if (grid.isWalkable(tile)) controller.walkTo(tile)
                    }
                },
        ) {
            val scope = this
            // Read here, not in the composable, so a walking player only redraws the Canvas
            val playerPos = controller.playerPos
            val trail = controller.trail
            val arrivedAt = controller.arrivedAt
            val viewCenter = Vec2(size.width / 2f, size.height / 2f)
            val tw = tileWidth * camera.scale
            val th = tw / 2f
            fun screenOf(x: Float, y: Float) = camera.worldToScreen(iso.tileCenter(x, y), viewCenter).toOffset()

            // Ground: grass in a checkerboard, and the roads
            for (y in 0 until grid.height) for (x in 0 until grid.width) {
                val key = when {
                    TilePos(x, y) in town.roads -> SpriteKey.ROAD
                    (x + y) % 2 == 0 -> SpriteKey.GRASS_LIGHT
                    else -> SpriteKey.GRASS_DARK
                }
                sprites.draw(scope, Sprite(key, screenOf(x.toFloat(), y.toFloat()), tw))
            }

            // Where the player is heading, and the tiles on the way
            trail.forEach { tile ->
                drawCircle(TownColors.Marker.copy(alpha = 0.6f), tw * 0.06f, screenOf(tile.x.toFloat(), tile.y.toFloat()))
            }
            trail.lastOrNull()?.let { goal ->
                drawDiamondOutline(screenOf(goal.x.toFloat(), goal.y.toFloat()), tw * 0.9f, th * 0.9f, TownColors.Marker, 3f)
            }

            // Things that stand on tiles are drawn back to front: the bigger x + y is, the
            // further down the screen and the closer to the camera, so it is drawn later.
            val things = buildList<Pair<Float, () -> Unit>> {
                town.buildings.forEach { b ->
                    add(
                        b.depth to {
                            val sprite = Sprite(
                                key = b.type.spriteKey(),
                                anchor = screenOf(b.center.x, b.center.y),
                                tileWidth = tw,
                                size = b.size,
                                variant = b.style,
                                doorSide = b.doorSide,
                                doorIndex = b.doorIndex,
                            )
                            sprites.draw(scope, sprite)
                        },
                    )
                }
                town.trees.forEach { t ->
                    add(
                        (t.x + t.y).toFloat() to {
                            val variant = (t.x * 5 + t.y * 3) % 3
                            sprites.draw(scope, Sprite(SpriteKey.TREE, screenOf(t.x.toFloat(), t.y.toFloat()), tw, variant = variant))
                        },
                    )
                }
                add((playerPos.x + playerPos.y + 0.01f) to { sprites.draw(scope, Sprite(SpriteKey.PLAYER, screenOf(playerPos.x, playerPos.y), tw)) })
            }
            things.sortedBy { it.first }.forEach { it.second() }

            // Name labels float above the buildings. The building you are on the way to is blue,
            // the one you are standing at is orange.
            town.buildings.forEach { b ->
                val at = screenOf(b.center.x, b.center.y)
                val isTarget = b.id == targetBuildingId
                val style = TextStyle(
                    fontSize = (11f * camera.scale.coerceIn(0.8f, 1.5f)).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTarget) Color.White else PR.Ink,
                )
                val text = textMeasurer.measure(if (isTarget) "! ${b.name}" else b.name, style)
                val padX = 8f * density
                val padY = 3f * density
                val pill = Size(text.size.width + 2 * padX, text.size.height + 2 * padY)
                val topLeft = Offset(at.x - pill.width / 2f, at.y - b.type.labelHeightInTiles() * tw - pill.height)
                val fill = when {
                    isTarget -> Palette5.PersianBlue
                    arrivedAt?.id == b.id -> PR.Amber
                    else -> Color.White.copy(alpha = 0.92f)
                }
                drawRoundRect(fill, topLeft, pill, CornerRadius(pill.height / 2f))
                drawText(text, topLeft = topLeft + Offset(padX, padY))
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable { camera = IsoCamera() },
        ) { Pill("Reset view") }
    }
}
