package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.math.Position
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.sin

private enum class Phase { PLAN, RUN, CAUGHT, DELIVERED }

private const val PIP_SPEED = 4.5f // map units per second
private const val PIP_Y = 0.45f

private fun nodePosition(id: String, y: Float = PIP_Y): Position {
    val n = RouterMap.map.node(id)
    return Position(n.x, y, n.z)
}

/**
 * World 3 in 3D. For each packet:
 * 1. Draw Pip's route on the 3D map by dragging from node to adjacent node.
 * 2. Press Run: Pip runs the route. Router R2 sends him out of the exit its forwarding table
 *    picks, so a wrong exit gets him caught by a Glitch at that hop.
 */
@Composable
fun RouterStation3DScreen(onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    val level = RouterStationLevel
    val run = rememberLevelRun(onFinish)
    var round by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.PLAN) }
    val route = remember { mutableStateListOf(RouterMap.SOURCE) }
    var pipPos by remember { mutableStateOf(nodePosition(RouterMap.SOURCE)) }
    var hop by remember { mutableIntStateOf(0) }
    var glitchAt by remember { mutableStateOf<String?>(null) }
    var runId by remember { mutableIntStateOf(0) }
    var peekRow by remember { mutableStateOf<Int?>(null) }
    var say by remember {
        mutableStateOf(Say("Drag from Seoul along the links to draw Pip's route. R2's table decides the exit!"))
    }

    val dest = RouterMap.destinations[round]
    val routersPassed = route.take(hop + 1).count { RouterMap.map.node(it).kind == NodeKind.ROUTER }
    val ttl = level.START_TTL - routersPassed
    val mac = RouterMap.macs[routersPassed.coerceAtMost(RouterMap.macs.lastIndex)]

    fun resetRoute() {
        route.clear()
        route.add(RouterMap.SOURCE)
        pipPos = nodePosition(RouterMap.SOURCE)
        hop = 0
        glitchAt = null
        phase = Phase.PLAN
    }

    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        val r = route.toList()
        val verdict = RouterMap.check(r, dest)
        val stop = if (verdict is RouteVerdict.Lost) verdict.atIndex else r.lastIndex
        hop = 0
        for (i in 0 until stop) {
            val a = RouterMap.map.node(r[i])
            val b = RouterMap.map.node(r[i + 1])
            val durationMs = hypot(b.x - a.x, b.z - a.z) / PIP_SPEED * 1000f
            val t0 = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val f = ((now - t0) / 1_000_000f / durationMs).coerceIn(0f, 1f)
                pipPos = Position(
                    a.x + (b.x - a.x) * f,
                    PIP_Y + 0.08f * sin(now / 150_000_000f),
                    a.z + (b.z - a.z) * f,
                )
                if (f >= 1f) break
            }
            hop = i + 1
        }
        pipPos = nodePosition(r[stop])
        when (verdict) {
            is RouteVerdict.Lost -> {
                glitchAt = r[stop]
                say = Say("👾 A Glitch caught Pip at ${RouterMap.map.node(r[stop]).label}! " + verdict.explanation, isError = true)
                delay(700)
                val expected = RouterMap.expectedRoute(dest)
                run.fail(
                    Mistake(
                        question = "Route Pip to $dest",
                        yourAnswer = r.joinToString(" → ") { RouterMap.map.node(it).label },
                        correctAnswer = expected.joinToString(" → ") { RouterMap.map.node(it).label },
                        explanation = verdict.explanation,
                    ),
                )
                phase = Phase.CAUGHT
            }

            RouteVerdict.Delivered -> {
                say = Say(
                    "Delivered! IP header: $dest the whole way. Only the MAC raincoat changed at each router, " +
                        "and TTL dropped by 1 each time.",
                )
                phase = Phase.DELIVERED
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        LevelHeader("World 3 · Router station", "Packet ${round + 1} of ${RouterMap.destinations.size}", run.lives, onQuit)

        Column(
            Modifier
                .fillMaxWidth()
                .background(PR.Blue50, RoundedCornerShape(14.dp))
                .padding(10.dp),
        ) {
            Text("Destination IP  $dest", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                "🧥 MAC $mac    ⏳ TTL $ttl",
                fontSize = 11.sp,
                color = PR.Blue800,
                fontFamily = FontFamily.Monospace,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
            NetworkScene(
                map = RouterMap.map,
                route = route,
                pipPosition = pipPos,
                glitchAt = glitchAt,
                goalNode = RouterMap.destinationNode(dest),
                followPip = phase == Phase.RUN,
                interactive = phase == Phase.PLAN,
                onExtend = { id ->
                    route.add(id)
                    pipPos = nodePosition(id)
                },
                onRetract = {
                    route.removeAt(route.lastIndex)
                    pipPos = nodePosition(route.last())
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        ForwardingBoard(dest = dest, peekRow = peekRow, onRow = { peekRow = if (peekRow == it) null else it })

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (phase) {
                Phase.PLAN -> {
                    SecondaryButton("Clear", Modifier.weight(1f)) { resetRoute() }
                    PrimaryButton("Run Pip!", Modifier.weight(2f)) {
                        if (route.size >= 2) {
                            phase = Phase.RUN
                            say = Say("Pip is running…")
                            runId += 1
                        } else {
                            say = Say("Draw a route first: drag from Seoul to the next node.")
                        }
                    }
                }

                Phase.RUN -> Text("Running…", fontSize = 14.sp, color = PR.Muted, modifier = Modifier.padding(14.dp))
                Phase.CAUGHT -> PrimaryButton("Try again") {
                    say = Say("Check R2's table, then redraw the route.")
                    resetRoute()
                }

                Phase.DELIVERED -> {
                    val last = round == RouterMap.destinations.lastIndex
                    PrimaryButton(if (last) "Finish mission" else "Next packet →") {
                        if (last) {
                            run.clear()
                        } else {
                            round += 1
                            say = Say("New packet! Draw its route.")
                            resetRoute()
                        }
                    }
                }
            }
        }

        Text(
            say.text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = if (say.isError) PR.Red800 else PR.Ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(if (say.isError) PR.Red50 else PR.White, RoundedCornerShape(12.dp))
                .padding(10.dp),
        )
    }
}

/** R2's forwarding table. Tap a row to lay the mask stencil over the destination. */
@Composable
private fun ForwardingBoard(dest: Ipv4, peekRow: Int?, onRow: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PR.Board, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("R2 forwarding table · tap a row for the mask stencil", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
        RouterStationLevel.table.forEachIndexed { i, route ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (i == peekRow) PR.BoardRowSelected else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { onRow(i) }
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${route.network}/${route.prefix}", Modifier.weight(2.2f), fontSize = 12.sp, color = PR.BoardText, fontFamily = FontFamily.Monospace)
                Text("→ ${route.nextHop}", Modifier.weight(1f), fontSize = 12.sp, color = PR.BoardText, fontFamily = FontFamily.Monospace)
            }
        }
        if (peekRow != null) {
            Text(
                stencilText(dest, RouterStationLevel.table[peekRow]),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = PR.Amber,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The AND the router does for one row, shown without saying whether the row matches. */
private fun stencilText(dest: Ipv4, row: Route): String {
    val masked = dest and row.mask
    val qi = (0..3).firstOrNull { masked.octets[it] != row.network.octets[it] } ?: row.mask.firstPartialOctet()
    val d = dest.octets[qi]
    val m = row.mask.octets[qi]
    return "D AND mask = $masked   (row network ${row.network})\n" +
        "octet ${qi + 1}: ${d.toBinary8()} AND ${m.toBinary8()} = ${(d and m).toBinary8()}"
}
