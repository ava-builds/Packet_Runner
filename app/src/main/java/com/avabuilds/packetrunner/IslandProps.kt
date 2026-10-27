package com.avabuilds.packetrunner

import androidx.compose.runtime.Composable
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import kotlin.math.hypot
import kotlin.random.Random

/** Where the grass surface of the island is. */
internal const val GROUND = 0.72f

private data class Lump(val x: Float, val z: Float, val r: Float, val yaw: Float)

/** The island is a union of overlapping stepped cylinders, like the cliffs in a toy diorama. */
private val Lumps = listOf(
    Lump(0f, 8f, 3.2f, 5f), Lump(-1f, 4.5f, 3.6f, 18f), Lump(1.2f, 1f, 3.8f, 30f),
    Lump(-1f, -2.5f, 3.6f, 12f), Lump(1f, -5.5f, 3.4f, 25f), Lump(-0.5f, -8.5f, 3f, 8f),
    Lump(-3.2f, 6.5f, 2f, 20f), Lump(3.4f, 3f, 2.2f, 35f), Lump(-3.4f, -0.5f, 2.2f, 10f),
    Lump(3.2f, -4f, 2f, 22f), Lump(-3f, -7f, 1.8f, 15f),
)

/** The five world landmarks, in world order, on the island's winding road. */
internal val LandmarkSpots = listOf(
    1 to Position(-1.4f, GROUND, 8.0f),
    2 to Position(1.8f, GROUND, 4.2f),
    3 to Position(-1.8f, GROUND, 0.6f),
    4 to Position(1.8f, GROUND, -3.2f),
    5 to Position(-1.0f, GROUND, -7.0f),
)

private val RoadControl = listOf(
    0f to 10.5f, -1.4f to 8f, 0.4f to 6.2f, 1.8f to 4.2f, 0.2f to 2.4f, -1.8f to 0.6f,
    0.4f to -1.4f, 1.8f to -3.2f, 0.4f to -5f, -1f to -7f,
)

/** Catmull-Rom spline through [RoadControl], sampled into a smooth winding road. */
internal val RoadPoints: List<Pair<Float, Float>> = run {
    val pts = listOf(RoadControl.first()) + RoadControl + listOf(RoadControl.last())
    buildList {
        for (i in 1 until pts.size - 2) {
            val (p0, p1, p2, p3) = listOf(pts[i - 1], pts[i], pts[i + 1], pts[i + 2])
            for (s in 0 until 8) {
                val t = s / 8f
                fun c(a: Float, b: Float, cc: Float, d: Float) = 0.5f * (
                    2 * b + (-a + cc) * t + (2 * a - 5 * b + 4 * cc - d) * t * t + (-a + 3 * b - 3 * cc + d) * t * t * t
                    )
                add(c(p0.first, p1.first, p2.first, p3.first) to c(p0.second, p1.second, p2.second, p3.second))
            }
        }
        add(RoadControl.last())
    }
}

private fun onIsland(x: Float, z: Float) = Lumps.any { hypot(x - it.x, z - it.z) < it.r * 0.78f }
private fun nearLandmark(x: Float, z: Float) = LandmarkSpots.any { hypot(x - it.second.x, z - it.second.z) < 1.9f }
private fun nearRoad(x: Float, z: Float) = RoadPoints.any { hypot(x - it.first, z - it.second) < 0.85f }

private val TreeSpots: List<Pair<Float, Float>> = run {
    val rnd = Random(7)
    val out = mutableListOf<Pair<Float, Float>>()
    var guard = 0
    while (out.size < 34 && guard++ < 4000) {
        val x = rnd.nextFloat() * 10f - 5f
        val z = rnd.nextFloat() * 20f - 10f
        if (onIsland(x, z) && !nearLandmark(x, z) && !nearRoad(x, z) && out.none { hypot(x - it.first, z - it.second) < 0.9f }) {
            out.add(x to z)
        }
    }
    out
}

private val HillSpots = listOf(-3.3f to 5.8f, 3.6f to 2.6f, -3.6f to -3.8f, 3.4f to -6.2f, 0.9f to 9.2f, -2.6f to -8.2f)

@Composable
internal fun SceneScope.IslandWorld(p: Palette) {
    // Sea
    CubeNode(size = Size(90f, 0.2f, 110f), materialInstance = p.sea, position = Position(0f, -0.6f, 0f))

    // Terrain: sand ring, brown cliff, green top, each lump a touch higher or lower than the last
    Lumps.forEachIndexed { i, l ->
        val j = (i % 3) * 0.03f
        CylinderNode(radius = l.r + 0.5f, height = 0.4f, sideCount = 9, materialInstance = p.sand,
            position = Position(l.x, -0.35f, l.z), rotation = Rotation(y = l.yaw))
        CylinderNode(radius = l.r, height = 1.0f, sideCount = 9, materialInstance = p.cliff,
            position = Position(l.x, j, l.z), rotation = Rotation(y = l.yaw))
        CylinderNode(radius = l.r, height = 0.2f, sideCount = 9, materialInstance = p.grass,
            position = Position(l.x, 0.6f + j, l.z), rotation = Rotation(y = l.yaw))
    }

    // The winding road is Pip's network cable
    TubeNode(
        points = RoadPoints.map { Position(it.first, GROUND + 0.05f, it.second) },
        radius = 0.2f,
        materialInstance = p.road,
    )

    // Trees
    TreeSpots.forEach { (x, z) ->
        CylinderNode(radius = 0.07f, height = 0.3f, materialInstance = p.trunk, position = Position(x, GROUND + 0.15f, z))
        ConeNode(radius = 0.32f, height = 0.8f, sideCount = 7, materialInstance = p.leaf, position = Position(x, GROUND + 0.65f, z))
    }

    // Soft hills
    HillSpots.forEach { (x, z) ->
        SphereNode(radius = 0.9f, materialInstance = p.grassDark, position = Position(x, GROUND, z), scale = io.github.sceneview.math.Scale(1.4f, 0.55f, 1.2f))
    }

    // Lake and mountain
    CylinderNode(radius = 1.1f, height = 0.06f, sideCount = 10, materialInstance = p.lake, position = Position(-3.3f, GROUND + 0.01f, -0.4f))
    ConeNode(radius = 1.3f, height = 2.0f, sideCount = 6, materialInstance = p.rock, position = Position(3.4f, GROUND + 1.0f, -5.4f))
    ConeNode(radius = 0.45f, height = 0.6f, sideCount = 6, materialInstance = p.snow, position = Position(3.4f, GROUND + 1.75f, -5.4f))

    // Sailboats
    listOf(-6.2f to 6f, 6.6f to -2f, -5.5f to -9f).forEach { (x, z) ->
        CubeNode(size = Size(0.9f, 0.18f, 0.35f), materialInstance = p.wood, position = Position(x, -0.4f, z))
        ConeNode(radius = 0.3f, height = 0.9f, sideCount = 3, materialInstance = p.sail, position = Position(x, 0.15f, z))
    }
}

// ---------------------------------------------------------------------------
// Landmarks: one network-themed building cluster per world.
// ---------------------------------------------------------------------------

/** World 1: a Wi-Fi tower inside a radio bubble and a bank of fog. */
@Composable
internal fun SceneScope.WifiFogLandmark(at: Position, p: Palette) {
    CylinderNode(radius = 0.1f, height = 1.7f, materialInstance = p.sail, position = Position(at.x, at.y + 0.85f, at.z))
    SphereNode(radius = 0.18f, materialInstance = p.roof, position = Position(at.x, at.y + 1.75f, at.z))
    SphereNode(radius = 0.8f, materialInstance = p.radio, position = Position(at.x, at.y + 1.7f, at.z))
    SphereNode(radius = 1.25f, materialInstance = p.radio, position = Position(at.x, at.y + 1.7f, at.z))
    listOf(-0.8f to 0.5f, 0.7f to 0.7f, 0.1f to -0.8f, -0.6f to -0.5f, 0.9f to -0.3f).forEach { (dx, dz) ->
        SphereNode(radius = 0.42f, materialInstance = p.fog, position = Position(at.x + dx, at.y + 0.3f, at.z + dz))
    }
}

/** World 2: a switch city skyline. */
@Composable
internal fun SceneScope.SwitchCityLandmark(at: Position, p: Palette) {
    listOf(
        Triple(-0.5f, 0.2f, Size(0.55f, 1.4f, 0.55f)), Triple(0.15f, -0.4f, Size(0.6f, 1.0f, 0.6f)),
        Triple(0.6f, 0.3f, Size(0.5f, 1.9f, 0.5f)), Triple(-0.3f, -0.6f, Size(0.5f, 0.7f, 0.5f)),
        Triple(0.1f, 0.6f, Size(0.6f, 0.9f, 0.5f)),
    ).forEachIndexed { i, (dx, dz, size) ->
        CubeNode(
            size = size,
            materialInstance = if (i % 2 == 0) p.building else p.buildingLight,
            position = Position(at.x + dx, at.y + size.y / 2, at.z + dz),
        )
    }
}

/** World 3: a router with antennas, a platform and a train. */
@Composable
internal fun SceneScope.RouterStationLandmark(at: Position, p: Palette) {
    CylinderNode(radius = 0.8f, height = 0.4f, materialInstance = p.building, position = Position(at.x, at.y + 0.2f, at.z))
    CubeNode(size = Size(0.8f, 0.4f, 0.8f), materialInstance = p.leaf, position = Position(at.x, at.y + 0.6f, at.z))
    CylinderNode(radius = 0.05f, height = 0.7f, materialInstance = p.leaf, position = Position(at.x - 0.25f, at.y + 1.15f, at.z))
    CylinderNode(radius = 0.05f, height = 0.7f, materialInstance = p.leaf, position = Position(at.x + 0.25f, at.y + 1.15f, at.z))
    CubeNode(size = Size(0.08f, 0.04f, 1.8f), materialInstance = p.rail, position = Position(at.x + 1.1f, at.y + 0.02f, at.z))
    CubeNode(size = Size(0.08f, 0.04f, 1.8f), materialInstance = p.rail, position = Position(at.x + 1.5f, at.y + 0.02f, at.z))
    CubeNode(size = Size(0.5f, 0.35f, 1.0f), materialInstance = p.train, position = Position(at.x + 1.3f, at.y + 0.25f, at.z - 0.1f))
}

/** World 4: Mom's old house and a moving van. */
@Composable
internal fun SceneScope.MomMovedLandmark(at: Position, p: Palette) {
    CubeNode(size = Size(1.1f, 0.65f, 0.9f), materialInstance = p.wall, position = Position(at.x, at.y + 0.33f, at.z))
    ConeNode(radius = 0.85f, height = 0.6f, sideCount = 4, materialInstance = p.roof,
        position = Position(at.x, at.y + 0.95f, at.z), rotation = Rotation(y = 45f))
    CubeNode(size = Size(0.22f, 0.4f, 0.05f), materialInstance = p.trunk, position = Position(at.x, at.y + 0.2f, at.z + 0.46f))
    CubeNode(size = Size(0.5f, 0.4f, 0.8f), materialInstance = p.sail, position = Position(at.x - 1.1f, at.y + 0.3f, at.z + 0.3f))
    CubeNode(size = Size(0.5f, 0.25f, 0.3f), materialInstance = p.building, position = Position(at.x - 1.1f, at.y + 0.2f, at.z + 0.85f))
}

/** World 5: a giant phone. */
@Composable
internal fun SceneScope.MomsPhoneLandmark(at: Position, p: Palette) {
    CubeNode(size = Size(0.95f, 1.7f, 0.16f), materialInstance = p.leaf, position = Position(at.x, at.y + 0.85f, at.z), rotation = Rotation(y = 15f))
    CubeNode(size = Size(0.8f, 1.45f, 0.03f), materialInstance = p.screen, position = Position(at.x + 0.02f, at.y + 0.85f, at.z + 0.09f), rotation = Rotation(y = 15f))
    SphereNode(radius = 0.9f, materialInstance = p.radio, position = Position(at.x, at.y + 1.0f, at.z))
}

/** Draws the landmark for [world] at its spot. */
@Composable
internal fun SceneScope.Landmark(world: Int, at: Position, p: Palette) {
    when (world) {
        1 -> WifiFogLandmark(at, p)
        2 -> SwitchCityLandmark(at, p)
        3 -> RouterStationLandmark(at, p)
        4 -> MomMovedLandmark(at, p)
        else -> MomsPhoneLandmark(at, p)
    }
}

