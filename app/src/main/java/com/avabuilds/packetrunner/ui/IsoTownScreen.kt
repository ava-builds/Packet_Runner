package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Building
import com.avabuilds.packetrunner.game.BuildingBubble
import com.avabuilds.packetrunner.game.Curriculum
import com.avabuilds.packetrunner.game.DayLight
import com.avabuilds.packetrunner.game.IsoCamera
import com.avabuilds.packetrunner.game.Juice
import com.avabuilds.packetrunner.game.IsoMath
import com.avabuilds.packetrunner.game.LinkStatus
import com.avabuilds.packetrunner.game.PathWalker
import com.avabuilds.packetrunner.game.Pathfinding
import com.avabuilds.packetrunner.game.TilePos
import com.avabuilds.packetrunner.game.TownLife
import com.avabuilds.packetrunner.game.Town
import com.avabuilds.packetrunner.game.Vec2
import com.avabuilds.packetrunner.game.pointInPolygon
import com.avabuilds.packetrunner.ui.sprites.CanvasSpriteProvider
import com.avabuilds.packetrunner.ui.sprites.Sprite
import com.avabuilds.packetrunner.ui.sprites.SpriteKey
import com.avabuilds.packetrunner.ui.sprites.SpriteProvider
import com.avabuilds.packetrunner.ui.sprites.buildingSilhouette
import com.avabuilds.packetrunner.ui.sprites.labelHeightInTiles
import com.avabuilds.packetrunner.ui.sprites.lockedLabelHeightInTiles
import com.avabuilds.packetrunner.ui.sprites.lockedSpriteKey
import com.avabuilds.packetrunner.ui.sprites.spriteKey
import kotlin.math.roundToInt

/** The 10 x 10 town is about this many tiles wide on screen at the start, so it fills the width. */
private const val FILL_TILES = 11.5f

/** Steps per second of the slow animation (swaying trees, a person's idle bob) in the layer with the buildings. */
private const val SWAY_STEPS = 15f

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
fun IsoTownScreen(
    controller: TownController,
    targetBuildingId: String?,
    modifier: Modifier = Modifier,
    /** How each building's link to the office is doing; drives the color of its cable and its Wi-Fi ring. */
    linkStatus: Map<String, LinkStatus> = emptyMap(),
    /** The time of day in game minutes (08:00 = 480). Read only while drawing, so the clock does not recompose the screen. */
    dayMinute: () -> Float = { 12 * 60f },
    /** Mood, hearts, ringing phones and alerts above the buildings. */
    bubbles: List<BuildingBubble> = emptyList(),
    /** Keeps the "Reset view" button clear of the system bars; the town itself draws under them. */
    controlsPadding: PaddingValues = PaddingValues(),
    /** Called when the player taps a building (for a light vibration). */
    onBuildingTap: () -> Unit = {},
    /** The buildings that are not open yet: they show as a building site or a "for rent" lot, and tapping them does not walk there. */
    lockedBuildings: Set<String> = emptySet(),
    /** Called instead of walking when the player taps a building that is not open yet. */
    onLockedBuildingTap: (Building) -> Unit = {},
) {
    val town = controller.town
    val grid = town.grid
    val sprites: SpriteProvider = remember { CanvasSpriteProvider() }
    val textMeasurer = rememberTextMeasurer()
    // The town fills most of the screen width at the start; pinch to zoom as before
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val tileWidth = screenWidthPx / FILL_TILES
    val scenery = remember(town) { Scenery.build(town) }
    // no cables run to a building that is not open yet
    val openCables = remember(scenery, lockedBuildings) { scenery.cables.filter { it.buildingId !in lockedBuildings } }
    val life = remember { TownLife.default() }
    val iso = remember(grid, tileWidth) { IsoMath.centeredOn(grid, tileWidth, tileWidth / 2f) }

    var camera by remember { mutableStateOf(IsoCamera()) }

    // The building that was just tapped bounces for a moment. The bounce time is only read while it lasts,
    // so the layer is redrawn every frame only then.
    var bounceId by remember { mutableStateOf<String?>(null) }
    var bounceStart by remember { mutableFloatStateOf(0f) }

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

    // One animation clock for everything that moves on its own (cables, packets, Wi-Fi rings).
    // Only the network layer reads it, so the ground and the buildings are not redrawn every frame.
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { clock.floatValue = it / 1_000_000_000f }
    }

    // Trees sway and people bob at this many steps a second, so the layer with the buildings is not redrawn at full speed
    val swayTime by remember { derivedStateOf { (clock.floatValue * SWAY_STEPS).toInt() / SWAY_STEPS } }

    LaunchedEffect(bounceId, bounceStart) {
        if (bounceId != null) {
            kotlinx.coroutines.delay((Juice.BOUNCE_SECONDS * 1000).toLong() + 50)
            bounceId = null
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(PR.Bg)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val viewCenter = Vec2(size.width / 2f, size.height * ViewTransform.VIEW_CENTER_Y)
                    camera = camera.zoomAbout(centroid.toVec2(), zoom, viewCenter).panBy(pan.toVec2())
                }
            }
            .pointerInput(town, iso) {
                detectTapGestures { tap ->
                    val viewCenter = Vec2(size.width / 2f, size.height * ViewTransform.VIEW_CENTER_Y)
                    val point = tap.toVec2()
                    val tw = tileWidth * camera.scale

                    // A tap on a building's drawn walls or roof walks to its door. Check the
                    // buildings in front first, because they cover the ones behind them.
                    val building = town.buildings.sortedByDescending { it.depth }.firstOrNull { b ->
                        val c = camera.worldToScreen(iso.tileCenter(b.center.x, b.center.y), viewCenter)
                        pointInPolygon(buildingSilhouette(b.type, c, tw, b.size), point)
                    }
                    if (building != null && building.id in lockedBuildings) {
                        onLockedBuildingTap(building)
                        return@detectTapGestures
                    }
                    if (building != null) {
                        bounceStart = clock.floatValue
                        bounceId = building.id
                        onBuildingTap()
                        controller.walkTo(building.door)
                        return@detectTapGestures
                    }
                    val tile = iso.tileAt(camera.screenToWorld(point, viewCenter))
                    if (grid.isWalkable(tile)) controller.walkTo(tile)
                }
            },
    ) {
        // Layer 1: the ground. It changes only when the camera moves.
        Canvas(Modifier.fillMaxSize()) {
            val view = ViewTransform(camera, iso, tileWidth, size)
            val scope = this
            // The world around the town: hills at the back, then faded grass, the river and the bridge
            scenery.hills.forEach { hill ->
                val at = view.screenOf(hill.x, hill.y)
                if (view.onScreen(at)) sprites.draw(scope, Sprite(SpriteKey.HILL, at, view.tw, variant = (hill.radius * 10).toInt(), alpha = 0.9f))
            }
            scenery.groundTiles.forEach { t ->
                val at = view.screenOf(t.x.toFloat(), t.y.toFloat())
                if (view.onScreen(at)) sprites.draw(scope, Sprite(t.key, at, view.tw, variant = t.variant, alpha = t.alpha))
            }
            // Ground of the town: grass in a checkerboard, and the roads
            for (y in 0 until grid.height) for (x in 0 until grid.width) {
                val key = when {
                    TilePos(x, y) in town.roads -> SpriteKey.ROAD
                    (x + y) % 2 == 0 -> SpriteKey.GRASS_LIGHT
                    else -> SpriteKey.GRASS_DARK
                }
                sprites.draw(scope, Sprite(key, view.screenOf(x.toFloat(), y.toFloat()), view.tw))
            }
        }

        // Layer 2: what lies on the ground and moves by itself: Wi-Fi rings, cables, packets, and the walking trail.
        Canvas(Modifier.fillMaxSize()) {
            val view = ViewTransform(camera, iso, tileWidth, size)
            drawNetwork(view, town, openCables, linkStatus, clock.floatValue)
            val trail = controller.trail
            trail.forEach { tile ->
                drawCircle(TownColors.Marker.copy(alpha = 0.6f), view.tw * 0.06f, view.screenOf(tile.x.toFloat(), tile.y.toFloat()))
            }
            trail.lastOrNull()?.let { goal ->
                drawDiamondOutline(view.screenOf(goal.x.toFloat(), goal.y.toFloat()), view.tw * 0.9f, view.th * 0.9f, TownColors.Marker, 3f)
            }
        }

        // Layer 3: things that stand on tiles, the player, and the name labels. They change when the player walks.
        Canvas(Modifier.fillMaxSize()) {
            val scope = this
            val view = ViewTransform(camera, iso, tileWidth, size)
            // Read here, not in the composable, so a walking player only redraws this layer
            val playerPos = controller.playerPos
            val arrivedAt = controller.arrivedAt
            val sway = swayTime
            val bouncing = bounceId
            val bounceT = if (bouncing != null) clock.floatValue - bounceStart else 0f
            val walking = controller.isWalking

            // Things that stand on tiles are drawn back to front: the bigger x + y is, the further down
            // the screen and the closer to the camera, so it is drawn later. The player is slotted in by depth.
            val playerDepth = playerPos.x + playerPos.y + 0.01f
            var playerDrawn = false
            fun drawPlayer() {
                sprites.draw(scope, Sprite(SpriteKey.PLAYER, view.screenOf(playerPos.x, playerPos.y), view.tw, time = sway, moving = walking))
                playerDrawn = true
            }
            scenery.standing.forEach { p ->
                if (!playerDrawn && p.depth > playerDepth) drawPlayer()
                val at = view.screenOf(p.x, p.y)
                if (view.onScreen(at)) {
                    val locked = p.buildingId != null && p.buildingId in lockedBuildings
                    val key = if (locked) town.building(p.buildingId!!).type.lockedSpriteKey() else p.key
                    val sprite = Sprite(key, at, view.tw, size = p.size, variant = p.variant, doorSide = p.doorSide, doorIndex = p.doorIndex, time = sway, phase = p.phase)
                    if (bouncing != null && p.buildingId == bouncing) {
                        // squash and stretch around the foot of the building
                        val sq = Juice.bounce(bounceT)
                        scale(sq.x, sq.y, pivot = at) { sprites.draw(scope, sprite) }
                    } else {
                        sprites.draw(scope, sprite)
                    }
                }
            }
            if (!playerDrawn) drawPlayer()

            // Name labels float above the buildings. The building you are on the way to is blue,
            // the one you are standing at is orange.
            town.buildings.forEach { b ->
                val at = view.screenOf(b.center.x, b.center.y)
                val isTarget = b.id == targetBuildingId
                val style = labelStyle(isTarget, camera.scale)
                val locked = b.id in lockedBuildings
                val label = if (locked) lockedLabel(b) else labelText(b.name, isTarget)
                val text = textMeasurer.measure(label, style)
                val padX = 8f * density
                val padY = 3f * density
                val pill = labelPillSize(textMeasurer, label, style)
                val topLeft = Offset(at.x - pill.width / 2f, at.y - (if (locked) b.type.lockedLabelHeightInTiles() else b.type.labelHeightInTiles()) * view.tw - pill.height)
                val fill = when {
                    isTarget -> Palette5.PersianBlue
                    arrivedAt?.id == b.id -> PR.Amber
                    else -> Color.White.copy(alpha = 0.92f)
                }
                drawRoundRect(fill, topLeft, pill, CornerRadius(pill.height / 2f))
                drawText(text, topLeft = topLeft + Offset(padX, padY))
            }
        }

        // Layer 4: the life of the town: people, cars, smoke, birds, cloud shadows and Pip. Follows the shared clock.
        Canvas(Modifier.fillMaxSize()) {
            val view = ViewTransform(camera, iso, tileWidth, size)
            drawLife(view, sprites, town, life, openCables, linkStatus, clock.floatValue)
        }

        // Layer 5: the color of the time of day over everything. Layer 6: lit windows and streetlights on top of the dark.
        Canvas(Modifier.fillMaxSize()) { drawDayTint(DayLight.at(dayMinute())) }
        Canvas(Modifier.fillMaxSize()) {
            drawNightLights(ViewTransform(camera, iso, tileWidth, size), sprites, town, DayLight.at(dayMinute()), lockedBuildings)
        }

        // Layer 7: the bubbles above the buildings, over the dark so they stay bright at night
        Canvas(Modifier.fillMaxSize()) {
            drawBubbles(ViewTransform(camera, iso, tileWidth, size), town, bubbles, targetBuildingId, textMeasurer, clock.floatValue)
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(controlsPadding)
                .padding(16.dp)
                .clickable { camera = IsoCamera() },
        ) { Pill("Reset view") }
    }
}

/** The label of a building that is not open yet: "For rent · Lv 2" for a lot, "Soon · Lv 3" for a building site. */
private fun lockedLabel(b: Building): String {
    val level = Curriculum.buildingLevel(b.id)?.let { " · Lv $it" }.orEmpty()
    return if (b.type.lockedSpriteKey() == SpriteKey.CONSTRUCTION) "Soon$level" else "For rent$level"
}
