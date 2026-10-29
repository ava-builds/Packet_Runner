package com.avabuilds.packetrunner.network

/** The office tower jobs: a hub that shouts to every floor, and a switch that still remembers where a printer used to be. */
object TowerCases {
    private fun slots(n: Int) = if (n == 1) "1 slot" else "$n slots"

    private val concepts = listOf(
        Concept(
            "Hub, switch and collision domain", "허브, 스위치, 충돌 도메인",
            "A hub repeats every frame out of every port, so all computers share one wire and collide. A switch sends a frame only where it needs to go, so many pairs can talk at once.",
            "Getting Connected · Ethernet and switching",
        ),
        Concept(
            "Learning bridge: flood, forward, filter", "러닝 브리지: 플러딩, 포워딩, 필터링",
            "A switch learns which port each sender is on from the frames it sees. For an unknown destination it floods all other ports, for a known one it forwards to one port, and if the destination is on the same port it drops the frame.",
            "Getting Connected · Switching (bridges)",
        ),
        Concept(
            "Ethernet collisions and backoff", "이더넷 충돌과 백오프",
            "When two computers send on a shared wire at once, both notice the collision, wait a random time and try again. After every clash the random range doubles, so busy shared wires slow down a lot.",
            "Getting Connected · Ethernet (CSMA/CD)",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. Check the scans: who receives a frame, and what the switch has learned.",
        "A frame goes to the port the switch last saw its sender on. Where was the printer last seen?",
        "A hub copies every frame to everyone. A switch is the one that learns and sends each frame only where it belongs.",
    )

    private fun portText(action: BridgeAction) = when (action) {
        is BridgeAction.Flood -> "flooded to ports ${action.ports.joinToString(", ")}"
        is BridgeAction.Forward -> "forwarded to port ${action.port}"
        BridgeAction.Filter -> "filtered (same port)"
    }

    private fun tableScan(name: String, note: String, table: Map<String, Int>) = Scan(
        name, note, listOf("Computer", "Port"), table.map { (mac, port) -> listOf(mac, "$port") },
    )

    // Case 1: the hub
    private val hubSession = LanSession(computers = 5, seed = 7L)
    private val hubDelivery: HubDelivery = hubSession.hubSend(src = 1, dst = 3)
    /** The first of the seeded examples where the two computers clash at least twice, so the doubling range is visible. */
    private val hubClash: CollisionTrace = (1L..200L).map { LanSession(5, it).hubCollide(1, 2) }.first { it.attempts >= 3 }

    private val switchSession = LanSession(computers = 5, seed = 7L).also { it.installSwitch() }
    private val switchFirst = switchSession.switchSend(3, 1) // C speaks first, so the switch learns where C is (and floods: it does not know A yet)
    private val switchThen = switchSession.switchSend(1, 3) // now A to C goes only to C's port

    val hub = CaseFile(
        id = "tower_hub", title = "The floor where everyone hears everything",
        story = "The tower's 4th floor runs five computers on one old hub. People complain that the network crawls when the floor is busy, and the security guard is worried that every computer can read everyone's traffic.",
        symptoms = listOf("The network is fast at 7 am and slow at 10 am.", "A computer that is only a bystander sees other people's frames.", "The more people work, the worse it gets."),
        scans = listOf(
            Scan(
                "One frame from A to C", "On the hub every computer receives the frame, whoever it is for.",
                listOf("Computer", "Receives it", "Meant for it?"),
                (2..5).map { n ->
                    listOf(hubSession.name(n), if (n in hubDelivery.receivers) "yes" else "no", if (n in hubDelivery.wasted) "no ✗ wasted" else "yes ✓")
                },
            ),
            Scan(
                "A and B send at the same time", "Each clash makes both wait a random number of slots; the range doubles every time.",
                listOf("Attempt", "A waits", "B waits"),
                hubClash.rounds.mapIndexed { i, (a, b) -> listOf("${i + 1}", slots(a), slots(b) + if (a == b) " · clash again" else "") },
            ),
        ),
        actions = listOf(
            CaseAction("Add a second hub for new computers", false, "Now twice as many computers share one wire. More frames collide and the floor gets slower, not faster.", cost = 40),
            CaseAction("Buy faster cables", false, "The wire is not short of speed, it is shared. Everyone still waits for the same clash-prone cable.", cost = 30),
            CaseAction("Replace the hub with a switch", true, "The switch learns which computer is on which port and sends each frame only there. Many pairs talk at once, with no collisions and no bystanders."),
            CaseAction("Ask people not to use the network at the same time", false, "Nobody can plan that. It lasts until the next busy morning.", cost = 10),
        ),
        afterFix = Scan(
            "The same frames on the switch", "The first frame is flooded because C is new to the switch. After that it knows where each one is.",
            listOf("Frame", "What the switch does"),
            listOf(
                listOf("C → A (first frame)", portText(switchFirst)),
                listOf("A → C", portText(switchThen)),
            ),
        ),
        hints = hints, concepts = concepts,
    )

    // Case 2: the printer that moved
    /** What a switch holds while the printer already stands at its new socket: the table and where a print job goes. */
    private fun stale(): Pair<Map<String, Int>, BridgeAction> {
        val bridge = LearningBridge(portCount = 6)
        bridge.receive("Printer", "PC 1", 3)
        bridge.receive("PC 1", "Printer", 1) // the printer is learned on port 3, its old socket
        val toOldPort = bridge.receive("PC 1", "Printer", 1)
        return bridge.learned.toMap() to toOldPort
    }

    private val staleCase = stale()
    private val afterMove = LearningBridge(portCount = 6).also {
        it.receive("Printer", "PC 1", 3)
        it.receive("PC 1", "Printer", 1)
        it.receive("Printer", "PC 1", 5) // the printer now speaks from its new socket
    }
    private val afterForward = afterMove.receive("PC 1", "Printer", 1)

    val moved = CaseFile(
        id = "tower_moved", title = "The printer that moved",
        story = "On the 9th floor the printer was moved to another wall socket. Since then nobody can print to it, although its lights are on and the cable is fine.",
        symptoms = listOf("Jobs to the printer disappear.", "The printer's own screen says it is online.", "It worked before it was moved."),
        scans = listOf(
            tableScan("What the switch remembers", "Which port each device was last seen on. The printer moved to port 5.", staleCase.first),
            Scan(
                "A print job from PC 1", "The switch looks up the printer's port and sends the frame there.",
                listOf("Frame", "What the switch does"),
                listOf(listOf("PC 1 → Printer", portText(staleCase.second) + " (the old socket)")),
            ),
        ),
        actions = listOf(
            CaseAction("Replace the switch", false, "The switch was working exactly as designed: it sends frames to the port where it last saw the printer.", cost = 60),
            CaseAction("Put a hub in front of the printer", false, "A hub changes nothing about the old entry. The switch still sends frames to port 3.", cost = 30),
            CaseAction("Make the printer send a frame (print a test page from it)", true, "The switch sees the printer's frame come in on port 5 and learns the new place. The next job goes to port 5."),
            CaseAction("Reboot every computer on the floor", false, "The computers are not the problem. The switch still holds the printer's old port.", cost = 20),
        ),
        afterFix = tableScan("The switch learned again", "The printer is now on port 5. The next print job goes there.", afterMove.learned)
            .copy(rows = afterMove.learned.map { (mac, port) -> listOf(mac, "$port") } + listOf(listOf("next job: PC 1 → Printer", portText(afterForward)))),
        hints = hints, concepts = concepts,
    )

    val all = listOf(hub, moved)
}
