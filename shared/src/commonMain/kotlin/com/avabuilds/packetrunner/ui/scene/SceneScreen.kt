package com.avabuilds.packetrunner.ui.scene

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.network.CellPos
import com.avabuilds.packetrunner.network.ClientReport
import com.avabuilds.packetrunner.network.ClientStatus
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.network.DhcpPool
import com.avabuilds.packetrunner.network.Device
import com.avabuilds.packetrunner.network.DeviceSettings
import com.avabuilds.packetrunner.network.DeviceType
import com.avabuilds.packetrunner.network.EditResult
import com.avabuilds.packetrunner.network.Goal
import com.avabuilds.packetrunner.network.Verdict
import com.avabuilds.packetrunner.network.shortText
import com.avabuilds.packetrunner.network.NetworkReport
import com.avabuilds.packetrunner.network.NetworkSimulator
import com.avabuilds.packetrunner.network.Scenario
import com.avabuilds.packetrunner.network.SceneEditor
import com.avabuilds.packetrunner.ui.Glitch
import com.avabuilds.packetrunner.ui.NanaSays
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.Pip
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Tool(val label: String, val emoji: String) {
    CABLE("Cable", "🔌"),
    DEVICE("Device", "📦"),
    SETTINGS("Settings", "⚙️"),
}

/** How fast Pip runs along a cable during a scan, in cells per second. */
private const val PIP_SPEED = 5f

/** If the player does nothing for this long, Nana calls on the radio. */
private const val IDLE_HINT_MS = 35_000L

/**
 * The inside of a building: a room with devices and cables that the player edits with four tools.
 *  - Device: pick something from the tray and tap an empty spot (tap a placed device to remove it)
 *  - Cable: tap one device, then another
 *  - Settings: tap a device to change its settings
 *  - Scan: Pip runs along the cables and reports who is online
 * All the rules live in the `network` package; this screen shows the room and sends the edits.
 */
@Composable
fun SceneScreen(
    scenario: Scenario,
    neighborName: String,
    reward: Int,
    onFinish: (success: Boolean, payout: Int, stars: Int) -> Unit,
    modifier: Modifier = Modifier,
    /** When given, the concept card offers to save what the player learned. */
    onSaveToNotebook: ((title: String, concepts: List<Concept>) -> Unit)? = null,
) {
    var scene by remember { mutableStateOf(scenario.scene) }
    var spent by remember { mutableIntStateOf(0) }
    var tool by remember { mutableStateOf(Tool.DEVICE) }
    var trayChoice by remember { mutableStateOf<DeviceType?>(null) }
    var cableFrom by remember { mutableStateOf<String?>(null) }
    var settingsFor by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>("Goal: ${scenario.goal}") }

    var scanResult by remember { mutableStateOf<NetworkReport?>(null) }
    var revealed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var scanDone by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var pipAt by remember { mutableStateOf<Offset?>(null) }
    var cellPx by remember { mutableFloatStateOf(0f) }

    var hintIndex by remember { mutableIntStateOf(-1) }
    var nana by remember { mutableStateOf<String?>(null) }
    var failedScans by remember { mutableIntStateOf(0) }
    var edits by remember { mutableIntStateOf(0) }
    var showConcepts by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val solved = scanDone && scanResult?.let(scenario::isSolved) == true
    val stars = scenario.stars(spent)
    val tip = scenario.tip(stars)
    val payout = (reward - spent + tip).coerceAtLeast(0)

    fun showNextHint() {
        if (hintIndex + 1 < scenario.hints.size) {
            hintIndex += 1
            nana = scenario.hints[hintIndex]
        }
    }

    // Nana only calls when you are stuck: after a quiet while, or after failed scans
    LaunchedEffect(edits, failedScans) {
        delay(IDLE_HINT_MS)
        if (!solved) showNextHint()
    }

    fun applyEdit(result: EditResult, note: String? = null) {
        if (!result.ok) {
            message = result.error
            return
        }
        scene = result.scene
        spent = (spent + result.cost).coerceAtLeast(0)
        scanResult = null
        revealed = emptySet()
        scanDone = false
        edits += 1
        message = note
    }

    fun onTapCell(cell: CellPos) {
        if (scanning || showConcepts) return
        val device = scene.deviceAt(cell)
        when (tool) {
            Tool.DEVICE -> {
                val choice = trayChoice
                when {
                    device != null && device.placedByPlayer -> {
                        val price = scenario.allowed[device.type]?.price ?: 0
                        applyEdit(SceneEditor.remove(scene, device.id, price, scenario.cablePricePerCell), "${device.type.label} removed. Half of its price came back.")
                    }
                    device != null -> message = "The ${device.type.label.lowercase()} was here before you."
                    choice == null -> message = "Pick a device from the tray first."
                    else -> {
                        val allowance = scenario.allowed.getValue(choice)
                        applyEdit(SceneEditor.place(scene, choice, cell, allowance.price, allowance.maxCount), "${choice.label} placed.")
                        if (scene.devices.count { it.type == choice && it.placedByPlayer } >= allowance.maxCount) trayChoice = null
                    }
                }
            }

            Tool.CABLE -> {
                val from = cableFrom
                when {
                    device == null -> cableFrom = null
                    from == null ->
                        if (device.type.canCable) {
                            cableFrom = device.id
                            message = "Now tap the device to connect it to."
                        } else {
                            message = "A ${device.type.label.lowercase()} has no cable socket."
                        }
                    from == device.id -> cableFrom = null
                    else -> {
                        applyEdit(SceneEditor.toggleCable(scene, from, device.id, scenario.cablePricePerCell), "Done. Tap the same two devices again to take the cable away.")
                        cableFrom = null
                    }
                }
            }

            Tool.SETTINGS -> {
                if (device != null && device.type.isWifiSource) {
                    settingsFor = device.id
                    message = null
                } else {
                    message = "Tap a router, access point or repeater to see its settings."
                }
            }
        }
    }

    suspend fun movePip(points: List<Offset>) {
        pipAt = points.first()
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val distance = (b - a).getDistance()
            if (distance < 1f) continue
            val durationMs = distance / (cellPx * PIP_SPEED) * 1000f
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val f = ((now - start) / 1_000_000f / durationMs).coerceIn(0f, 1f)
                pipAt = a + (b - a) * f
                if (f >= 1f) break
            }
        }
    }

    fun startScan() {
        if (scanning || showConcepts) return
        scope.launch {
            scanning = true
            scanDone = false
            revealed = emptySet()
            message = "Pip is scanning the network…"
            val snapshot = scene
            val result = NetworkSimulator.evaluate(snapshot)
            scanResult = result
            for (client in result.clients) {
                val points = client.path.mapNotNull { snapshot.device(it)?.at }
                    .map { Offset((it.x + 0.5f) * cellPx, (it.y + 0.5f) * cellPx) }
                if (points.isNotEmpty()) movePip(points)
                revealed = revealed + client.deviceId
                delay(350)
            }
            pipAt = null
            scanDone = true
            scanning = false
            val problem = scenario.firstProblem(result, snapshot)
            if (problem == null) {
                nana = null // no need for the radio any more
                message = if (scenario.hasGlitch) "The Glitch can't jam the air any more! Tap Finish job when you are ready."
                else "Everyone is online and happy! Tap Finish job when you are ready."
            } else {
                message = problem
                failedScans += 1
                if (failedScans % 2 == 0) showNextHint() // stuck after two failed scans
            }
        }
    }

    Box(modifier.fillMaxSize().background(PR.Bg)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(scenario.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("$neighborName · pays ₩$reward", fontSize = 12.sp, color = PR.Muted)
                }
                Pill("Materials ₩$spent", PR.Amber50, PR.Amber800)
                TextButton(onClick = { onFinish(false, 0, 0) }) { Text("Give up", color = PR.Red800, fontSize = 13.sp) }
            }

            // The room
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val cell = min(constraints.maxWidth.toFloat() / scene.width, constraints.maxHeight.toFloat() / scene.height)
                SideEffect { cellPx = cell }
                val density = LocalDensity.current
                val widthDp = with(density) { (cell * scene.width).toDp() }
                val heightDp = with(density) { (cell * scene.height).toDp() }
                val textMeasurer = rememberTextMeasurer()

                Box(Modifier.size(widthDp, heightDp)) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .clipToBounds() // the Wi-Fi circles must stay inside the room
                            .pointerInput(scene.width, scene.height, cell) {
                                detectTapGestures { tap ->
                                    val cellPos = CellPos((tap.x / cell).toInt(), (tap.y / cell).toInt())
                                    if (scene.contains(cellPos)) onTapCell(cellPos)
                                }
                            },
                    ) {
                        fun centerOf(d: Device) = Offset((d.at.x + 0.5f) * cell, (d.at.y + 0.5f) * cell)

                        // Floor and walls
                        for (y in 0 until scene.height) for (x in 0 until scene.width) {
                            drawRect(
                                if ((x + y) % 2 == 0) RoomColors.FloorLight else RoomColors.FloorDark,
                                Offset(x * cell, y * cell), Size(cell, cell),
                            )
                        }
                        drawRoundRect(Palette5.DuskBlue, Offset.Zero, size, CornerRadius(cell * 0.12f), style = Stroke(cell * 0.12f))

                        // Wi-Fi coverage, shown once a scan has finished
                        if (scanDone) {
                            val result = scanResult
                            scene.devices.filter { it.type.isWifiSource && result != null && it.id in result.onlineSources }.forEach { d ->
                                val radius = d.type.wifiRange * cell
                                drawCircle(Palette5.IcyBlue.copy(alpha = 0.22f), radius, centerOf(d))
                                drawCircle(
                                    Palette5.PersianBlue.copy(alpha = 0.5f), radius, centerOf(d),
                                    style = Stroke(cell * 0.03f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(cell * 0.15f, cell * 0.1f))),
                                )
                            }
                        }

                        // Cables
                        scene.cables.forEach { cable ->
                            val a = scene.device(cable.a)
                            val b = scene.device(cable.b)
                            if (a != null && b != null) {
                                drawLine(Palette5.DeepNavy, centerOf(a), centerOf(b), cell * 0.1f)
                                drawLine(PR.Amber, centerOf(a), centerOf(b), cell * 0.04f)
                            }
                        }

                        // Devices with their names
                        val label = TextStyle(fontSize = (cell * 0.17f).toSp(), fontWeight = FontWeight.Bold, color = PR.Ink)
                        val noteStyle = TextStyle(fontSize = (cell * 0.15f).toSp(), fontWeight = FontWeight.Medium, color = Palette5.PersianBlue)
                        scene.devices.forEach { d ->
                            val c = centerOf(d)
                            if (d.id == cableFrom) drawCircle(PR.Amber.copy(alpha = 0.5f), cell * 0.46f, c)
                            drawDevice(d.type, c, cell)
                            val text = textMeasurer.measure(d.displayName, label)
                            drawText(text, topLeft = Offset(c.x - text.size.width / 2f, c.y + cell * 0.26f))
                            scenario.goals[d.id]?.let { goal ->
                                val note = textMeasurer.measure(goal.shortText, noteStyle)
                                drawText(note, topLeft = Offset(c.x - note.size.width / 2f, c.y + cell * 0.26f + text.size.height))
                            }
                        }
                    }

                    // What the scan found, next to each client
                    scanResult?.clients?.filter { it.deviceId in revealed }?.forEach { r ->
                        val d = scene.device(r.deviceId) ?: return@forEach
                        val anchorX = (d.at.x + 0.5f) * cell
                        ResultBadge(
                            r,
                            scenario.verdict(r),
                            scenario.goals[r.deviceId],
                            anchorX = anchorX,
                            roomWidth = cell * scene.width,
                            modifier = Modifier.offset { IntOffset(anchorX.roundToInt(), (d.at.y * cell).roundToInt()) },
                        )
                    }

                    // Pip running along the cables
                    pipAt?.let { p ->
                        val pipPx = cell * 0.7f
                        Box(Modifier.offset { IntOffset((p.x - pipPx / 2).roundToInt(), (p.y - pipPx * 0.75f).roundToInt()) }) {
                            Pip(size = with(density) { pipPx.toDp() })
                        }
                    }

                    // The Glitch jams the air while devices collide, and is gone once they ask permission first
                    val jammed = if (scanDone && scenario.hasGlitch) {
                        scanResult?.clients?.filter { it.status == ClientStatus.COLLISIONS }.orEmpty()
                    } else {
                        emptyList()
                    }
                    if (jammed.isNotEmpty()) {
                        val spots = jammed.mapNotNull { scene.device(it.deviceId)?.at }
                        val gx = spots.map { (it.x + 0.5f) * cell }.average().toFloat()
                        val gy = spots.map { (it.y + 0.5f) * cell }.average().toFloat()
                        val glitchPx = cell * 1.1f
                        Column(
                            Modifier.offset { IntOffset((gx - glitchPx / 2).roundToInt(), (gy - glitchPx / 2).roundToInt()) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Glitch(size = with(density) { glitchPx.toDp() })
                            Text(
                                "👾 jamming the air!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PR.Red800,
                                modifier = Modifier.background(PR.Red50, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // These three areas keep a fixed height, so the room does not jump when the tool changes
            Box(Modifier.fillMaxWidth().height(40.dp)) {
                message?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = PR.Ink, modifier = Modifier.padding(horizontal = 4.dp)) }
            }

            Box(Modifier.fillMaxWidth().height(232.dp).verticalScroll(rememberScrollState())) {
                when (tool) {
                    Tool.DEVICE -> Tray(scenario, scene, trayChoice) { trayChoice = if (trayChoice == it) null else it }
                    Tool.CABLE -> Hint("Tap one device, then another to lay a cable (₩${scenario.cablePricePerCell} per cell, at most ${SceneEditor.MAX_CABLE_CELLS.toInt()} cells). Tap the same two again to remove it.")
                    Tool.SETTINGS -> {
                        val device = settingsFor?.let { scene.device(it) }
                        if (device != null) {
                            SettingsPanel(device) { applyEdit(SceneEditor.setSettings(scene, device.id, it)) }
                        } else {
                            Hint("Tap a router, access point or repeater to change its settings.")
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(54.dp)) {
                if (solved) PrimaryButton("Finish job · ${"★".repeat(stars)} · you get ₩$payout") { showConcepts = true }
            }

            // The four tools
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolButton("🔍", "Scan", selected = scanning, enabled = !scanning, modifier = Modifier.weight(1f)) { startScan() }
                Tool.entries.forEach { t ->
                    ToolButton(t.emoji, t.label, selected = tool == t, enabled = !scanning, modifier = Modifier.weight(1f)) {
                        tool = t
                        cableFrom = null
                        message = null
                    }
                }
            }
        }

        // Nana's radio floats over the empty space above the room, so the room never moves. Tap to dismiss.
        nana?.let { hint ->
            Box(Modifier.align(Alignment.TopCenter).padding(start = 12.dp, end = 12.dp, top = 70.dp).clickable { nana = null }) {
                NanaSays("📻 $hint")
            }
        }

        if (showConcepts) {
            ConceptCardView(
                concepts = scenario.conceptsUsed(scene),
                reward = reward,
                spent = spent,
                stars = stars,
                tip = tip,
                payout = payout,
                onSaveToNotebook = onSaveToNotebook?.let { save -> { save(scenario.title, scenario.conceptsUsed(scene)) } },
                onContinue = { onFinish(true, payout, stars) },
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 17.sp, color = PR.Muted, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
}

/** A small result next to a client: online with its speed and delay, a warning if it needs more, or why it is offline. */
@Composable
private fun ResultBadge(report: ClientReport, verdict: Verdict, goal: Goal?, anchorX: Float, roomWidth: Float, modifier: Modifier) {
    val text = when {
        verdict == Verdict.OK -> "✓ ${report.speedMbps} Mbps · ${report.latencyMs} ms"
        verdict == Verdict.TOO_SLOW -> "⚠ ${report.speedMbps} Mbps · needs ${goal?.minSpeedMbps}"
        verdict == Verdict.TOO_LAGGY -> "⚠ ${report.latencyMs} ms · needs ≤${goal?.maxLatencyMs}"
        report.status == ClientStatus.COLLISIONS -> "✗ collisions"
        report.status == ClientStatus.NO_IP -> "✗ no IP address"
        report.status == ClientStatus.NO_UPLINK -> "✗ no internet"
        else -> "✗ no signal"
    }
    val (fill, ink, edge) = when (verdict) {
        Verdict.OK -> Triple(PR.Green50, PR.Green800, PR.Green600)
        Verdict.TOO_SLOW, Verdict.TOO_LAGGY -> Triple(PR.Amber50, PR.Amber800, PR.Amber)
        Verdict.OFFLINE -> Triple(PR.Red50, PR.Red800, PR.Red)
    }
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = ink,
        textAlign = TextAlign.Center,
        modifier = modifier
            .graphicsLayer {
                // centered above the device, but never past the walls of the room
                val centered = -size.width / 2f
                val leftMost = -anchorX
                val rightMost = roomWidth - anchorX - size.width
                translationX = if (rightMost < leftMost) leftMost else centered.coerceIn(leftMost, rightMost)
                translationY = -size.height
            }
            .background(fill, RoundedCornerShape(8.dp))
            .border(1.dp, edge, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** The devices the player may buy. */
@Composable
private fun Tray(scenario: Scenario, scene: com.avabuilds.packetrunner.network.NetworkScene, choice: DeviceType?, onPick: (DeviceType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        scenario.allowed.forEach { (type, allowance) ->
            val left = allowance.maxCount - scene.devices.count { it.type == type && it.placedByPlayer }
            val selected = choice == type
            Column(
                Modifier
                    .weight(1f)
                    .background(if (selected) Palette5.PersianBlue else PR.White, RoundedCornerShape(12.dp))
                    .border(1.dp, if (selected) Palette5.PersianBlue else PR.Line, RoundedCornerShape(12.dp))
                    .clickable(enabled = left > 0) { onPick(type) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ink = if (selected) Color.White else if (left > 0) PR.Ink else PR.Gray
                Text(type.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ink, textAlign = TextAlign.Center)
                Text("₩${allowance.price} · ×$left", fontSize = 10.sp, color = ink)
                Text(if (type.isWifiSource) "reach ${type.wifiRange.toInt()}" else "wired", fontSize = 10.sp, color = ink)
            }
        }
    }
}

@Composable
private fun SettingsPanel(device: Device, onChange: (DeviceSettings) -> Unit) {
    Panel(color = PR.White, borderColor = PR.Line) {
        Text("${device.type.label} settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RTS/CTS", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Ask permission before sending (avoids collisions)", fontSize = 11.sp, color = PR.Muted)
            }
            Switch(checked = device.settings.rtsCts, onCheckedChange = { onChange(device.settings.copy(rtsCts = it)) })
        }
        if (device.type == DeviceType.ROUTER) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("DHCP pool size", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("How many IP addresses it can hand out", fontSize = 11.sp, color = PR.Muted)
                }
                val size = device.settings.dhcpPoolSize
                Box(Modifier.clickable(enabled = size > 1) { onChange(device.settings.copy(dhcpPoolSize = size - 1)) }) { Pill("−") }
                Text("$size", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.clickable(enabled = size < 16) { onChange(device.settings.copy(dhcpPoolSize = size + 1)) }) { Pill("+") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("DHCP lease time", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("How long a device keeps its address", fontSize = 11.sp, color = PR.Muted)
                }
                val choices = DhcpPool.LEASE_CHOICES
                val index = choices.indexOf(device.settings.leaseMinutes).coerceAtLeast(0)
                Box(Modifier.clickable(enabled = index > 0) { onChange(device.settings.copy(leaseMinutes = choices[index - 1])) }) { Pill("−") }
                Text("${choices[index]} min", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.clickable(enabled = index < choices.lastIndex) { onChange(device.settings.copy(leaseMinutes = choices[index + 1])) }) { Pill("+") }
            }
        }
    }
}

@Composable
private fun ToolButton(emoji: String, label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(if (selected) Palette5.PersianBlue else PR.White, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) Palette5.PersianBlue else PR.Line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else PR.Ink)
    }
}

private object RoomColors {
    val FloorLight = Color(0xFFF6EAD2)
    val FloorDark = Color(0xFFF0E0C2)
}
