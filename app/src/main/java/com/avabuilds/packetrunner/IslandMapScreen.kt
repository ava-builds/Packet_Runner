package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberView
import io.github.sceneview.utils.worldToScreen
import kotlin.math.abs
import kotlin.math.roundToInt

private val MapCameraPosition = Position(6.8f, 25f, 21f)
private val MapCameraTarget = Position(-1.2f, 0f, 1.5f)

/** Alternative world map: Pip's journey as a 3D island. Not wired in; see [WorldMapScreen]. */
@Composable
fun IslandMapScreen(worldStars: Map<Int, Int>, onPlayWorld: (Int) -> Unit) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val palette = remember(materialLoader) { Palette(materialLoader) }
    val camera = rememberCameraNode(engine) {
        position = MapCameraPosition
        lookAt(MapCameraTarget)
    }
    val labelAt = remember { mutableStateMapOf<Int, Offset>() }
    val pipWorld = Worlds.firstOrNull { (worldStars[it.number] ?: 0) == 0 }?.number

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pip's journey", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Seoul → Yangon · 5 worlds", fontSize = 13.sp, color = PR.Muted)
            }
            Pill("★ ${worldStars.values.sum()} / ${Worlds.size * 3}", PR.Amber50, PR.Amber800)
        }

        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
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
                    LandmarkSpots.forEach { (world, at) ->
                        val s = view.worldToScreen(Position(at.x, at.y + 3.4f, at.z))
                        if (s != null) {
                            val old = labelAt[world]
                            if (old == null || abs(old.x - s.x) > 1f || abs(old.y - s.y) > 1f) {
                                labelAt[world] = Offset(s.x, s.y)
                            }
                        }
                    }
                },
            ) {
                IslandWorld(palette)
                LandmarkSpots.forEach { (world, at) -> Landmark(world, at, palette) }
                pipWorld?.let { w ->
                    val at = LandmarkSpots.first { it.first == w }.second
                    PipModel(Position(at.x + 0.9f, at.y + 0.45f, at.z + 1.1f), palette)
                }
            }

            // Tappable world labels pinned above each landmark
            Worlds.forEach { world ->
                val at = labelAt[world.number] ?: return@forEach
                val stars = worldStars[world.number] ?: 0
                val here = world.number == pipWorld
                Column(
                    Modifier
                        .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
                        .graphicsLayer {
                            translationX = -size.width / 2f
                            translationY = -size.height
                        }
                        .background(if (stars > 0) PR.Teal50 else PR.White, RoundedCornerShape(12.dp))
                        .border(if (here) 2.dp else 1.dp, if (here) PR.Purple else if (stars > 0) PR.Teal else PR.Line, RoundedCornerShape(12.dp))
                        .clickable { if (world.playable) onPlayWorld(world.number) }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${world.emoji} ${world.number} · ${world.title}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    if (stars > 0 || here) {
                        Text(
                            if (stars > 0) "★".repeat(stars) + "☆".repeat(3 - stars) else "Play now",
                            fontSize = 10.sp,
                            color = if (stars > 0) PR.Amber else PR.Purple800,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
