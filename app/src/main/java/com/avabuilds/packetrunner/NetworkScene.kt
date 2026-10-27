package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberView
import io.github.sceneview.utils.worldToScreen
import kotlin.math.roundToInt

/** Colors for the 3D props, created once per scene. */
private class Palette(private val loader: MaterialLoader) {
    private fun mat(hex: Long, rough: Float = 0.5f) = loader.createColorInstance(Color(hex), roughness = rough)

    val ground: MaterialInstance = mat(0xFFE4E0F7, 0.9f)
    val link: MaterialInstance = mat(0xFF9A97AE, 0.8f)
    val route: MaterialInstance = mat(0xFF1D9E75, 0.3f)
    val router: MaterialInstance = mat(0xFF7F77DD)
    val routerTop: MaterialInstance = mat(0xFF3C3489)
    val network: MaterialInstance = mat(0xFF5DCAA5)
    val house: MaterialInstance = mat(0xFF085041)
    val source: MaterialInstance = mat(0xFF378ADD)
    val pip: MaterialInstance = mat(0xFF5DCAA5, 0.35f)
    val white: MaterialInstance = mat(0xFFFFFFFF, 0.3f)
    val dark: MaterialInstance = mat(0xFF0D2B22, 0.4f)
    val glitch: MaterialInstance = mat(0xFFE24B4A, 0.4f)
    val goal: MaterialInstance = mat(0xFFEF9F27, 0.3f)
}

private val OverviewPosition = Position(0f, 14f, 13f)
private val OverviewTarget = Position(0f, 0f, 1.3f)
private const val LINK_Y = 0.06f
private const val ROUTE_Y = 0.16f

/**
 * A 3D network map. In the draw phase ([interactive]) the player drags a finger from node to
 * adjacent node to build a route. In the run phase Pip is shown at [pipPosition] and the camera
 * can follow him.
 */
@Composable
fun NetworkScene(
    map: NetworkMap,
    route: List<String>,
    pipPosition: Position?,
    glitchAt: String?,
    goalNode: String?,
    followPip: Boolean,
    interactive: Boolean,
    onExtend: (String) -> Unit,
    onRetract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val palette = remember(materialLoader) { Palette(materialLoader) }
    val camera = rememberCameraNode(engine) {
        position = OverviewPosition
        lookAt(OverviewTarget)
    }
    val labelAt = remember { mutableStateMapOf<String, Offset>() }

    SideEffect {
        val p = pipPosition
        if (followPip && p != null) {
            camera.position = Position(p.x * 0.5f, 6.5f, p.z + 6.5f)
            camera.lookAt(Position(p.x * 0.5f, 0f, p.z - 1.5f))
        } else {
            camera.position = OverviewPosition
            camera.lookAt(OverviewTarget)
        }
    }

    val currentRoute by rememberUpdatedState(route)
    val currentInteractive by rememberUpdatedState(interactive)
    val currentExtend by rememberUpdatedState(onExtend)
    val currentRetract by rememberUpdatedState(onRetract)
    val hitRadius = with(LocalDensity.current) { 48.dp.toPx() }

    fun touch(p: Offset) {
        if (!currentInteractive) return
        val hit = map.nodes
            .mapNotNull { n ->
                view.worldToScreen(Position(n.x, 0.3f, n.z))?.let { s -> n.id to (Offset(s.x, s.y) - p).getDistance() }
            }
            .filter { it.second <= hitRadius }
            .minByOrNull { it.second }?.first ?: return
        val r = currentRoute
        val last = r.lastOrNull() ?: return
        when {
            r.size >= 2 && hit == r[r.size - 2] -> currentRetract()
            hit !in r && map.linked(last, hit) -> currentExtend(hit)
        }
    }

    Box(modifier.clipToBounds()) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            view = view,
            cameraNode = camera,
            cameraManipulator = null,
            onGestureListener = null,
            autoCenterContent = false,
            onFrame = {
                map.nodes.forEach { n ->
                    val s = view.worldToScreen(Position(n.x, 1.7f, n.z))
                    if (s != null) {
                        val old = labelAt[n.id]
                        if (old == null || kotlin.math.abs(old.x - s.x) > 1f || kotlin.math.abs(old.y - s.y) > 1f) {
                            labelAt[n.id] = Offset(s.x, s.y)
                        }
                    }
                }
            },
        ) {
            CubeNode(
                size = Size(40f, 0.2f, 60f),
                materialInstance = palette.ground,
                position = Position(0f, -0.1f, 0f),
            )
            map.links.forEach { (a, b) -> key(a, b) { Tube(map, a, b, LINK_Y, 0.07f, palette.link) } }
            route.zipWithNext().forEach { (a, b) -> key("route", a, b) { Tube(map, a, b, ROUTE_Y, 0.14f, palette.route) } }
            map.nodes.forEach { n -> key(n.id) { NodeProp(n, palette) } }
            goalNode?.let { GoalMarker(map.node(it), palette) }
            pipPosition?.let { PipModel(it, palette) }
            glitchAt?.let { GlitchModel(map.node(it), palette) }
        }

        // Touch layer: drag (or tap) onto an adjacent node to extend the route; back onto the
        // previous node to undo the last hop.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectDragGestures { change, _ -> touch(change.position) } }
                .pointerInput(Unit) { detectTapGestures { touch(it) } },
        )

        // Labels pinned on top of the 3D objects.
        map.nodes.forEach { n ->
            val at = labelAt[n.id] ?: return@forEach
            Text(
                (if (n.id == goalNode) "📱 " else "") + n.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PR.Ink,
                modifier = Modifier
                    .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
                    .graphicsLayer {
                        translationX = -size.width / 2f
                        translationY = -size.height
                    }
                    .background(PR.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Props, all built from simple primitives.
// ---------------------------------------------------------------------------

@Composable
private fun SceneScope.Tube(map: NetworkMap, a: String, b: String, y: Float, radius: Float, material: MaterialInstance) {
    val na = map.node(a)
    val nb = map.node(b)
    TubeNode(
        points = listOf(Position(na.x, y, na.z), Position(nb.x, y, nb.z)),
        radius = radius,
        materialInstance = material,
    )
}

@Composable
private fun SceneScope.NodeProp(n: MapNode, p: Palette) {
    val at = Position(n.x, 0f, n.z)
    when (n.kind) {
        NodeKind.SOURCE -> {
            CylinderNode(radius = 0.8f, height = 0.3f, materialInstance = p.source, position = Position(at.x, 0.15f, at.z))
            CubeNode(size = Size(0.7f, 0.5f, 0.5f), materialInstance = p.routerTop, position = Position(at.x, 0.55f, at.z))
        }

        NodeKind.ROUTER -> {
            CylinderNode(radius = 0.9f, height = 0.45f, materialInstance = p.router, position = Position(at.x, 0.22f, at.z))
            CubeNode(size = Size(0.8f, 0.4f, 0.8f), materialInstance = p.routerTop, position = Position(at.x, 0.65f, at.z))
            CylinderNode(radius = 0.05f, height = 0.7f, materialInstance = p.routerTop, position = Position(at.x - 0.25f, 1.2f, at.z))
            CylinderNode(radius = 0.05f, height = 0.7f, materialInstance = p.routerTop, position = Position(at.x + 0.25f, 1.2f, at.z))
        }

        NodeKind.NETWORK -> {
            CylinderNode(radius = 1.25f, height = 0.2f, materialInstance = p.network, position = Position(at.x, 0.1f, at.z))
            CubeNode(size = Size(0.5f, 0.45f, 0.5f), materialInstance = p.house, position = Position(at.x - 0.5f, 0.42f, at.z + 0.2f))
            CubeNode(size = Size(0.5f, 0.6f, 0.5f), materialInstance = p.house, position = Position(at.x + 0.1f, 0.5f, at.z - 0.3f))
            CubeNode(size = Size(0.5f, 0.4f, 0.5f), materialInstance = p.house, position = Position(at.x + 0.5f, 0.4f, at.z + 0.3f))
        }
    }
}

@Composable
private fun SceneScope.GoalMarker(n: MapNode, p: Palette) {
    CylinderNode(radius = 0.04f, height = 1.0f, materialInstance = p.goal, position = Position(n.x, 1.0f, n.z))
    SphereNode(radius = 0.28f, materialInstance = p.goal, position = Position(n.x, 1.65f, n.z))
}

@Composable
private fun SceneScope.PipModel(pos: Position, p: Palette) {
    Node(position = pos) {
        CubeNode(size = Size(0.85f, 0.6f, 0.85f), materialInstance = p.pip)
        CubeNode(size = Size(0.86f, 0.06f, 0.86f), materialInstance = p.routerTop, position = Position(0f, 0.3f, 0f))
        SphereNode(radius = 0.12f, materialInstance = p.white, position = Position(-0.2f, 0.05f, 0.43f))
        SphereNode(radius = 0.12f, materialInstance = p.white, position = Position(0.2f, 0.05f, 0.43f))
        SphereNode(radius = 0.06f, materialInstance = p.dark, position = Position(-0.2f, 0.05f, 0.54f))
        SphereNode(radius = 0.06f, materialInstance = p.dark, position = Position(0.2f, 0.05f, 0.54f))
    }
}

@Composable
private fun SceneScope.GlitchModel(n: MapNode, p: Palette) {
    Node(position = Position(n.x, 0.9f, n.z)) {
        CubeNode(size = Size(0.9f, 0.9f, 0.9f), materialInstance = p.glitch, rotation = Rotation(y = 45f))
        SphereNode(radius = 0.12f, materialInstance = p.goal, position = Position(-0.22f, 0.1f, 0.5f))
        SphereNode(radius = 0.12f, materialInstance = p.goal, position = Position(0.22f, 0.1f, 0.5f))
    }
}
