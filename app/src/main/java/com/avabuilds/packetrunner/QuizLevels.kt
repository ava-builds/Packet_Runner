package com.avabuilds.packetrunner

/** One multiple-choice puzzle: a scenario on the board, a question, one right answer. */
data class Question(
    val scenario: String,
    val prompt: String,
    val answer: String,
    val wrong: List<String>,
    val explanation: String,
)

/** A world played as a series of scenario puzzles (worlds 1, 2, 4 and 5). */
data class QuizLevel(
    val world: Int,
    val hudName: String,
    val briefing: Briefing,
    val whatReallyHappened: String,
    val questions: List<Question>,
)

private fun BridgeAction.text(): String = when (this) {
    is BridgeAction.Forward -> "Forward out of port $port only"
    is BridgeAction.Flood -> "Flood to ports ${ports.joinToString(", ")}"
    BridgeAction.Filter -> "Filter (drop it, same side)"
}

// ---------------------------------------------------------------------------
// World 1 · Wi-Fi fog
// ---------------------------------------------------------------------------

object WifiFogLevel {
    private const val RANGE = 100
    private val a = WifiNode("A", 0)
    private val b = WifiNode("B", 80)
    private val c = WifiNode("C", 160)
    private val a2 = WifiNode("A", 0)
    private val b2 = WifiNode("B", 50)
    private val c2 = WifiNode("C", 90)

    private fun line(nodes: List<WifiNode>) =
        nodes.joinToString("   ") { "${it.name}@${it.x}m" } + "   (radio range $RANGE m)"

    private val collide = "A senses an idle channel, sends, and both frames collide at B"
    private val waits = "A hears C and waits, so there is no collision"
    private val deaf = "B cannot hear anyone, so nothing is received"

    private val hidden1 = Wifi.isHiddenPair(a, c, b, RANGE)
    private val hidden2 = Wifi.isHiddenPair(a2, c2, b2, RANGE)

    private val navEnd = Wifi.navEnd(heardAtUs = 200, durationUs = 500)

    val level = QuizLevel(
        world = 1,
        hudName = "Wi-Fi fog",
        briefing = Briefing(
            chapter = "World 1 · Wi-Fi fog",
            title = "Cross the fog",
            nanaSays = "Pip, the Wi-Fi fog is thick. Radios can't always hear each other, " +
                "so two senders may shout at the same time without knowing!",
            why = "Wireless LANs can't detect collisions like Ethernet: a radio can't listen while " +
                "it talks. So Wi-Fi tries to avoid them (CSMA/CA, 충돌 회피) and uses RTS/CTS to " +
                "fix the hidden terminal problem (숨은 단말 문제).",
            howToThink = "Draw the nodes on a line and a circle of radio range around each one. " +
                "If two senders are both inside the receiver's circle but outside each other's, " +
                "they are hidden. RTS/CTS: ask (RTS), get permission (CTS), send DATA, get ACK.",
            keywords = listOf(
                "CSMA/CA · 충돌 회피",
                "Hidden terminal · 숨은 단말",
                "RTS / CTS",
                "NAV · 대기 시간",
            ),
        ),
        whatReallyHappened = "Wi-Fi radios can't transmit and listen at once, so collisions can't be " +
            "detected. A hidden terminal can't hear another sender that still reaches the same " +
            "receiver. RTS/CTS lets the receiver announce the transfer to everyone in its range. " +
            "Anyone who overhears the RTS or CTS sets its NAV and stays quiet for the stated duration.",
        questions = listOf(
            Question(
                scenario = line(listOf(a, b, c)),
                prompt = "A and C both want to send to B. Can A hear C? What happens?",
                answer = if (hidden1) collide else waits,
                wrong = if (hidden1) listOf(waits, deaf) else listOf(collide, deaf),
                explanation = "|A − C| = ${kotlin.math.abs(a.x - c.x)} m, range is $RANGE m → " +
                    if (hidden1) "A and C can't hear each other (hidden terminals), but both reach B."
                    else "they hear each other.",
            ),
            Question(
                scenario = line(listOf(a2, b2, c2)),
                prompt = "Now A, B, C are closer together. Is A hidden from C?",
                answer = if (hidden2) "Yes, hidden" else "No, they hear each other",
                wrong = listOf(if (hidden2) "No, they hear each other" else "Yes, hidden"),
                explanation = "|A − C| = ${kotlin.math.abs(a2.x - c2.x)} m ≤ $RANGE m, so carrier sensing works " +
                    "and C waits for A. No hidden terminal here.",
            ),
            Question(
                scenario = "A → B : RTS ...",
                prompt = "A sent an RTS to B. Which frame does B send back next?",
                answer = Wifi.rtsCtsSequence[1],
                wrong = listOf("ACK", "DATA"),
                explanation = "Order: ${Wifi.rtsCtsSequence.joinToString(" → ")}. " +
                    "The CTS tells everyone near B (even those hidden from A) to stay quiet.",
            ),
            Question(
                scenario = "C overhears B's CTS at t = 200 µs\nCTS duration field = 500 µs",
                prompt = "When can C try to send again?",
                answer = "t = $navEnd µs",
                wrong = listOf("t = 200 µs", "t = 500 µs"),
                explanation = "C sets its NAV: 200 + 500 = $navEnd µs. It stays quiet until then.",
            ),
            Question(
                scenario = "Ethernet: collision detection (CD)\nWi-Fi: collision avoidance (CA)",
                prompt = "Why can't Wi-Fi just use collision detection?",
                answer = "A radio can't listen while it transmits",
                wrong = listOf("Wi-Fi is faster than Ethernet", "Radio waves can't carry frames"),
                explanation = "A node's own signal drowns out others, and hidden terminals can't be " +
                    "heard anyway. So Wi-Fi avoids collisions before they happen.",
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// World 2 · Switch city
// ---------------------------------------------------------------------------

object SwitchCityLevel {
    private val ports = mapOf("A" to 1, "B" to 1, "C" to 2, "D" to 3)

    private data class Frame(val src: String, val dst: String)

    private val frames = listOf(
        Frame("A", "C"),
        Frame("C", "A"),
        Frame("B", "A"),
        Frame("D", "C"),
        Frame("A", "D"),
    )

    private fun buildQuestions(): List<Question> {
        val bridge = LearningBridge(portCount = 3)
        return frames.map { f ->
            val inPort = ports.getValue(f.src)
            val before = bridge.learned.entries.joinToString("  ") { "${it.key}→${it.value}" }
                .ifEmpty { "(empty)" }
            val action = bridge.receive(f.src, f.dst, inPort)
            val after = bridge.learned.entries.joinToString("  ") { "${it.key}→${it.value}" }
            val all = listOf(
                BridgeAction.Flood((1..3).filter { it != inPort }),
                BridgeAction.Filter,
                BridgeAction.Forward(1),
                BridgeAction.Forward(2),
                BridgeAction.Forward(3),
            ).map { it.text() }
            Question(
                scenario = "Ports: 1 = {A, B via hub}   2 = {C}   3 = {D}\n" +
                    "Table before: $before\nFrame ${f.src} → ${f.dst} arrives on port $inPort",
                prompt = "What does the switch do with this frame?",
                answer = action.text(),
                wrong = all.filter { it != action.text() }.take(2),
                explanation = "It first learns ${f.src} is on port $inPort (table: $after). " +
                    when (action) {
                        is BridgeAction.Flood -> "${f.dst} is not in the table, so it floods all other ports."
                        BridgeAction.Filter -> "${f.dst} is on the same port it came from, so nobody else needs it."
                        is BridgeAction.Forward -> "${f.dst} is known on port ${action.port}, so it forwards only there."
                    },
            )
        }
    }

    val level = QuizLevel(
        world = 2,
        hudName = "Switch city",
        briefing = Briefing(
            chapter = "World 2 · Switch city",
            title = "Learn the streets",
            nanaSays = "Pip, a switch is a city guide who learns who lives where just by " +
                "watching who sends frames. Watch the table grow!",
            why = "A learning bridge (브리지) fills its MAC table automatically. When a frame arrives " +
                "it remembers the source on that port, then decides what to do with the destination.",
            howToThink = "1) Write down: source MAC → incoming port. 2) Look up the destination. " +
                "Not in the table? Flood to every other port. On the same port it came from? Filter. " +
                "On another port? Forward only there.",
            keywords = listOf(
                "Learning bridge · 학습 브리지",
                "Flooding · 플러딩",
                "Filtering · 필터링",
                "MAC table",
            ),
        ),
        whatReallyHappened = "A bridge learns by looking at the SOURCE address of every frame, and " +
            "decides by looking at the DESTINATION. Unknown destination: flood. Destination on the " +
            "same segment: filter. Known destination elsewhere: forward. Over time most frames are " +
            "forwarded to one port instead of flooded, so the network stays efficient.",
        questions = buildQuestions(),
    )
}

// ---------------------------------------------------------------------------
// World 4 · Mom moved!
// ---------------------------------------------------------------------------

object MobileIpLevel {
    private val pip = Ipv4.parse("203.0.113.5")
    private val home = Ipv4.parse("128.96.5.9")
    private val homeAgent = Ipv4.parse("128.96.5.1")
    private val careOf = Ipv4.parse("45.64.20.77")

    private val original = IpPacket(src = pip, dst = home)
    private val tunneled = MobileIp.encapsulate(original, homeAgent, careOf)
    private val delivered = MobileIp.decapsulate(tunneled)

    private val scenario = "Mom's home address : $home\n" +
        "Home agent (home net): $homeAgent\n" +
        "Mom now in Yangon, care-of address: $careOf"

    val level = QuizLevel(
        world = 4,
        hudName = "Mom moved!",
        briefing = Briefing(
            chapter = "World 4 · Mom moved!",
            title = "Follow Mom",
            nanaSays = "Pip, Mom moved to Yangon but her phone keeps its home address. " +
                "Everyone still sends to the old address. Who forwards it?",
            why = "Mobile IP (모바일 IP) lets a host keep one permanent home address while it moves. " +
                "A home agent (홈 에이전트) receives packets for it and tunnels them to the care-of address.",
            howToThink = "Senders never change the destination. The home agent wraps the whole packet " +
                "inside a new IP header (source = home agent, destination = care-of address). " +
                "At the end of the tunnel the outer header is removed.",
            keywords = listOf(
                "Home agent · 홈 에이전트",
                "Care-of address · 임시 주소",
                "Tunneling · 터널링",
                "Encapsulation · 캡슐화",
            ),
        ),
        whatReallyHappened = "Pip's packet is addressed to Mom's permanent home address $home. " +
            "The home agent captures it and encapsulates it: outer source $homeAgent, outer " +
            "destination $careOf, original packet inside. At Mom's location the outer header is " +
            "removed, and the original packet (destination $home) is delivered to her phone.",
        questions = listOf(
            Question(
                scenario = scenario + "\nPip sends: ${original.src} → ${original.dst}",
                prompt = "Pip's packet reaches Mom's home network. Who picks it up?",
                answer = "The home agent",
                wrong = listOf("Mom's phone directly", "The foreign agent in Yangon"),
                explanation = "The packet is routed to the home network because $home belongs there. " +
                    "The home agent knows Mom has moved.",
            ),
            Question(
                scenario = scenario,
                prompt = "The tunnel packet's OUTER destination is…",
                answer = "Care-of address ${tunneled.dst}",
                wrong = listOf("Home address $home", "Pip's address $pip"),
                explanation = "Outer header: ${tunneled.src} → ${tunneled.dst}. " +
                    "Its job is to get the packet to Mom's current location.",
            ),
            Question(
                scenario = scenario,
                prompt = "The tunnel packet's OUTER source is…",
                answer = "Home agent ${tunneled.src}",
                wrong = listOf("Pip $pip", "Care-of address $careOf"),
                explanation = "The home agent creates the new header, so it is the source of the tunnel.",
            ),
            Question(
                scenario = "Tunnel packet arrives in Yangon:\n" +
                    "outer ${tunneled.src} → ${tunneled.dst}\n" +
                    "inner ${tunneled.inner!!.src} → ${tunneled.inner!!.dst}",
                prompt = "After the outer header is removed, the delivered packet's destination is…",
                answer = "${delivered.dst} (Mom's home address)",
                wrong = listOf("${tunneled.dst} (care-of address)", "${tunneled.src} (home agent)"),
                explanation = "Decapsulation restores the original packet unchanged: ${delivered.src} → ${delivered.dst}. " +
                    "Mom's phone still thinks she is at home.",
            ),
            Question(
                scenario = scenario + "\nMom replies to Pip.",
                prompt = "How does Mom's reply travel?",
                answer = "Straight to Pip, no tunnel",
                wrong = listOf("Back through the tunnel to the home agent first", "To the home agent by broadcast"),
                explanation = "Mom knows Pip's address, so she sends normally. The reply path differs " +
                    "from the forward path (triangle routing).",
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// World 5 · Mom's phone
// ---------------------------------------------------------------------------

object TransportLevel {
    private const val CLIENT_ISN = 1000L
    private const val SERVER_ISN = 5000L

    private val data1 = TcpSegment(seq = 1001, len = 500)
    private val data2 = TcpSegment(seq = 1501, len = 500)
    private val data3 = TcpSegment(seq = 2001, len = 500)
    private val ackAfterFirst = Tcp.ackFor(data1)
    private val ackWithGap = Tcp.cumulativeAck(1001, listOf(data1, data3))

    val level = QuizLevel(
        world = 5,
        hudName = "Mom's phone",
        briefing = Briefing(
            chapter = "World 5 · Mom's phone",
            title = "Ring ring!",
            nanaSays = "Pip, you finally reached Mom's phone! The last step is picking the " +
                "right app on the phone and making sure nothing gets lost.",
            why = "The transport layer (트랜스포트 계층) delivers data to the right application using " +
                "port numbers. TCP adds a handshake, sequence numbers and ACKs for reliability; " +
                "UDP just sends.",
            howToThink = "TCP: SYN(seq=x) → SYN+ACK(seq=y, ack=x+1) → ACK(ack=y+1). " +
                "An ACK number is the NEXT byte expected: seq + length. ACKs are cumulative: " +
                "they stop at the first missing byte.",
            keywords = listOf(
                "3-way handshake · 3-웨이 핸드셰이크",
                "Sequence / ACK number",
                "Port · 포트",
                "TCP vs UDP",
            ),
        ),
        whatReallyHappened = "TCP opens with a 3-way handshake: SYN seq=$CLIENT_ISN, SYN+ACK seq=$SERVER_ISN " +
            "ack=${Tcp.synAckAckNumber(CLIENT_ISN)}, ACK ack=${Tcp.finalAckNumber(SERVER_ISN)}. " +
            "The ACK number is always the next byte expected, and it is cumulative, so a lost segment " +
            "stops the ACK number until it is resent. UDP skips all of this for speed, " +
            "and port numbers pick the application.",
        questions = listOf(
            Question(
                scenario = "Pip → Mom : SYN, seq = $CLIENT_ISN",
                prompt = "Mom's SYN+ACK acknowledges with ack = ?",
                answer = Tcp.synAckAckNumber(CLIENT_ISN).toString(),
                wrong = listOf(CLIENT_ISN.toString(), (CLIENT_ISN + 2).toString()),
                explanation = "A SYN uses up one sequence number: $CLIENT_ISN + 1 = ${Tcp.synAckAckNumber(CLIENT_ISN)}.",
            ),
            Question(
                scenario = "Mom → Pip : SYN+ACK, seq = $SERVER_ISN, ack = ${Tcp.synAckAckNumber(CLIENT_ISN)}",
                prompt = "Pip's final ACK of the handshake has ack = ?",
                answer = Tcp.finalAckNumber(SERVER_ISN).toString(),
                wrong = listOf(SERVER_ISN.toString(), Tcp.synAckAckNumber(CLIENT_ISN).toString()),
                explanation = "Pip acknowledges Mom's SYN: $SERVER_ISN + 1 = ${Tcp.finalAckNumber(SERVER_ISN)}.",
            ),
            Question(
                scenario = "Pip → Mom : DATA seq = ${data1.seq}, ${data1.len} bytes",
                prompt = "What ACK number does Mom send back?",
                answer = ackAfterFirst.toString(),
                wrong = listOf(data1.seq.toString(), (ackAfterFirst + 1).toString()),
                explanation = "ACK = next byte expected = seq + len = ${data1.seq} + ${data1.len} = $ackAfterFirst.",
            ),
            Question(
                scenario = "Pip sent three 500-byte segments:\n" +
                    "seq ${data1.seq} ✓   seq ${data2.seq} ✗ (lost)   seq ${data3.seq} ✓ arrived",
                prompt = "What ACK number does Mom send after the third segment arrives?",
                answer = ackWithGap.toString(),
                wrong = listOf(Tcp.ackFor(data3).toString(), (data1.seq).toString()),
                explanation = "ACKs are cumulative. Bytes $ackWithGap onward are missing, so Mom keeps " +
                    "asking for $ackWithGap until it is resent.",
            ),
            Question(
                scenario = "Mom opens a video call with Pip.\nSpeed matters more than perfection.",
                prompt = "Which transport protocol fits best?",
                answer = "UDP",
                wrong = listOf("TCP"),
                explanation = "A late frame is useless in a live call, so resending it (TCP) just adds " +
                    "delay. UDP sends and moves on.",
            ),
            Question(
                scenario = "A packet arrives at Mom's phone (IP: 45.64.20.77).\n" +
                    "Her phone runs a browser, a chat app and a video app.",
                prompt = "What tells the phone which app gets this data?",
                answer = "The destination port number",
                wrong = listOf("The destination IP address", "The destination MAC address"),
                explanation = "IP gets the packet to the phone; the port number gets the data to the " +
                    "right application on it.",
            ),
        ),
    )
}

/** Worlds that are played as scenario puzzles, keyed by world number. */
val QuizLevels: Map<Int, QuizLevel> = listOf(
    WifiFogLevel.level,
    SwitchCityLevel.level,
    MobileIpLevel.level,
    TransportLevel.level,
).associateBy { it.world }
