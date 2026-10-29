package com.avabuilds.packetrunner.network

/** The Wi-Fi jobs: carrier sense gone wrong, seen from both sides (the exposed node and the hidden node). */
object WifiCases {
    private val concepts = listOf(
        Concept(
            "Carrier sense (CSMA/CA)", "반송파 감지 (CSMA/CA)",
            "Wi-Fi devices share the air. Before sending, a device listens: if it hears another device sending, it waits. That avoids most collisions without any cable.",
            "Getting Connected · Wireless links (CSMA/CA)",
        ),
        Concept(
            "Exposed node", "노출 단말 문제",
            "An exposed node hears a nearby sender and waits, although its own message would not disturb anyone. The air is free for it, but it cannot know, so capacity is wasted.",
            "Getting Connected · Wireless links (CSMA/CA)",
        ),
        Concept(
            "Hidden node and RTS/CTS", "숨겨진 단말 문제와 RTS/CTS",
            "Two devices that cannot hear each other may both send to a device between them, and their messages collide there. With RTS/CTS the receiver announces \"clear to send\", which even the hidden device hears.",
            "Getting Connected · Wireless links (CSMA/CA)",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. First the first scan: who hears whom. Then watch what each conversation does in every slot.",
        "A device waits when it hears someone sending. Ask: would its own message really disturb that someone's receiver?",
        "Two devices on different channels do not hear each other at all. A receiver that says \"clear to send\" is heard by devices the sender cannot reach.",
    )

    private val slotColumns = listOf("Slot")

    private fun outcomeText(o: SlotOutcome) = when (o) {
        SlotOutcome.SENT -> "sent ✓"
        SlotOutcome.DEFERRED -> "waits"
        SlotOutcome.COLLIDED -> "collision ✗"
    }

    private fun airtime(cell: WifiCell, flows: List<Flow>, slots: Int, rtsCts: Boolean = false): Scan {
        val results = cell.simulate(flows, slots, rtsCts)
        val rows = results.map { r -> listOf("${r.slot}") + flows.map { outcomeText(r.outcomes.getValue(it)) } } +
            listOf(listOf("sent") + flows.map { f -> "${results.count { it.outcomes[f] == SlotOutcome.SENT }} of $slots" })
        return Scan(
            "Airtime, $slots slots", "What each conversation did in each time slot. ${WifiCell.utilizationPercent(results)}% of the chances to send were used.",
            slotColumns + flows.map { it.toString() }, rows,
        )
    }

    private fun hearing(cell: WifiCell, nodes: List<AirNode>): Scan = Scan(
        "Who hears whom", "A device can hear another one on its channel within ${cell.range} m.",
        listOf("Device", "Spot", "Hears"),
        nodes.map { n ->
            val heard = nodes.filter { cell.hears(n.name, it.name) }.map { it.name }
            listOf("${n.name} (ch ${n.channel})", "${n.x} m", if (heard.isEmpty()) "nobody" else heard.joinToString(", "))
        },
    )

    // Case 1: the exposed node. Receipt printer pair and barista tablet pair stand in a line: A B . C D.
    private val exposedNodes = listOf(AirNode("Printer", 0), AirNode("Till", 10), AirNode("Tablet", 20), AirNode("Router", 30))
    private val exposedCell = WifiCell(exposedNodes, range = 12)
    private val exposedFlows = listOf(Flow("Till", "Printer"), Flow("Tablet", "Router"))
    private val separated = WifiCell(exposedNodes.map { if (it.name == "Tablet" || it.name == "Router") it.copy(channel = 11) else it }, range = 12)

    val exposed = CaseFile(
        id = "wifi_exposed", title = "The till that waits for nothing",
        story = "At the cafe the till sends receipts to the printer, while the barista's tablet talks to the router at the other end of the room. Both are slow, though neither pair is far apart and the Wi-Fi is not busy.",
        symptoms = listOf("Receipts print slowly while the tablet is in use.", "The tablet is slow while receipts print.", "Speed tests are fine when only one pair is used."),
        scans = listOf(
            hearing(exposedCell, exposedNodes),
            airtime(exposedCell, exposedFlows, slots = 6),
        ),
        actions = listOf(
            CaseAction("Turn on RTS/CTS", false, "RTS/CTS helps with hidden devices. Here the till still hears the tablet's \"request to send\" and still waits, and the extra messages slow things down more.", cost = 20),
            CaseAction("Raise the transmit power of all devices", false, "Every device now hears even more of the others, so even more devices wait for each other.", cost = 30),
            CaseAction("Put the tablet and the router on another channel", true, "Devices on different channels do not hear each other, so the till no longer waits for the tablet. Both pairs send at the same time."),
            CaseAction("Replace the receipt printer", false, "The printer is fine. It is the till that holds back, because it hears the tablet.", cost = 50),
        ),
        afterFix = airtime(separated, exposedFlows, slots = 6),
        hints = hints, concepts = concepts,
    )

    // Case 2: the hidden node. Two phones at the far ends of the room, the router in the middle: A B C.
    private val hiddenNodes = listOf(AirNode("Phone A", 0), AirNode("Router", 10), AirNode("Phone B", 20))
    private val hiddenCell = WifiCell(hiddenNodes, range = 12)
    private val hiddenFlows = listOf(Flow("Phone A", "Router"), Flow("Phone B", "Router"))

    val hidden = CaseFile(
        id = "wifi_hidden", title = "Two guests who cannot hear each other",
        story = "Two guests sit at opposite ends of the long cafe room, the router in the middle. When both upload a video, both uploads stall and keep restarting. One alone is fine.",
        symptoms = listOf("It only goes wrong when both are sending.", "Each phone shows a strong signal to the router.", "Retries and stalls, not a dropped connection."),
        scans = listOf(
            hearing(hiddenCell, hiddenNodes),
            airtime(hiddenCell, hiddenFlows, slots = 6),
        ),
        actions = listOf(
            CaseAction("Change the router's channel", false, "A new channel changes nothing: the two phones still cannot hear each other, so they still send at the same time.", cost = 20),
            CaseAction("Raise the router's transmit power", false, "The phones already hear the router. Their messages to it still collide, because the phones cannot hear each other.", cost = 30),
            CaseAction("Turn on RTS/CTS", true, "Now the router answers with \"clear to send\", and the other phone hears that although it cannot hear the first one. It waits its turn. No more collisions."),
            CaseAction("Move the guests to the cable", false, "Phones have no cable. Nothing changes for them.", cost = 10),
        ),
        afterFix = airtime(hiddenCell, hiddenFlows, slots = 6, rtsCts = true),
        hints = hints, concepts = concepts,
    )

    val all = listOf(exposed, hidden)
}
