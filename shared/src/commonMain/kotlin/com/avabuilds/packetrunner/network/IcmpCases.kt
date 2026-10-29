package com.avabuilds.packetrunner.network

/** The ping and traceroute jobs: reading TTL and ICMP answers to find where the trouble really is. */
object IcmpCases {
    private val concepts = listOf(
        Concept(
            "ICMP (Internet Control Message Protocol)", "ICMP (인터넷 제어 메시지 프로토콜)",
            "ICMP is how the network reports about itself: ping asks \"are you there?\" and routers send error messages such as \"time exceeded\" or \"destination unreachable\".",
            "Internetworking · Error reporting (ICMP)",
        ),
        Concept(
            "TTL (Time to Live)", "TTL (생존 시간)",
            "Every packet carries a TTL that each router lowers by one. A packet whose TTL reaches 0 is dropped, so a packet can never travel in circles forever.",
            "Internetworking · IP packet format",
        ),
        Concept(
            "Traceroute", "트레이스라우트 (경로 추적)",
            "Traceroute sends packets with TTL 1, 2, 3... Each router that drops one answers \"time exceeded\", so the answers list the routers along the path and how long each took.",
            "Internetworking · Error reporting (ICMP)",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. Read the traceroute from the top: each line is one router, in order.",
        "A big jump in the time between two lines is where the slow link is. The time includes every link before it.",
        "A line of stars only says that router did not answer. Look at the lines after it before you blame it.",
    )

    private val columns = listOf("TTL", "Router", "Round trip")

    private fun row(line: TraceLine): List<String> = when (val r = line.reply) {
        is ProbeReply.TimeExceeded -> listOf("${line.ttl}", "${r.from.name} ${r.from.ip}", "${r.rttMs} ms")
        is ProbeReply.EchoReply -> listOf("${line.ttl}", "${r.from.name} ${r.from.ip}", "${r.rttMs} ms · reached")
        ProbeReply.Timeout -> listOf("${line.ttl}", "* * *", "no answer")
    }

    private fun trace(path: Path, maxHops: Int = 30) = Icmp.traceroute(path, maxHops).map(::row)

    /** The round trip to [hop], read off a traceroute probe that ends there. */
    private fun rttTo(path: Path, hop: PathHop): Int =
        (Icmp.probe(path, path.route.indexOf(hop) + 1) as ProbeReply.TimeExceeded).rttMs

    private fun hop(name: String, ip: String, ms: Int, answers: Boolean = true) = PathHop(name, ip, ms, answersIcmp = answers)

    // Case 1: a slow link far away
    private val homeRouter = hop("Home router", "192.168.1.1", 1)
    private val ispEdge = hop("ISP gateway", "100.64.0.1", 4)
    private val isp2 = hop("ISP core", "203.0.113.1", 5)
    private val seaLink = hop("Overseas link", "198.51.100.9", 90)
    private val partner = hop("Partner router", "198.51.100.1", 3)
    private val shopServer = hop("Shop server", "198.51.100.80", 2)
    val slowPath = Path(listOf(homeRouter, ispEdge, isp2, seaLink, partner), shopServer)

    val slowHop = CaseFile(
        id = "icmp_slow", title = "Is it my router?",
        story = "Hana's webshop loads slowly and she is sure the new router is to blame. She wants a new one. Find out where the delay really comes from.",
        symptoms = listOf("Every page of the shop takes seconds.", "Other sites load fast.", "The router was replaced last week."),
        scans = listOf(
            Scan("Traceroute to the shop", "One probe per TTL. Each router that drops it answers; the time is the whole round trip to that router.", columns, trace(slowPath)),
            Scan(
                "Ping the router and the shop", "A ping uses TTL ${Icmp.DEFAULT_TTL} and travels all the way.",
                listOf("Target", "Round trip"),
                listOf(
                    listOf("Home router", "${(Icmp.probe(slowPath, 1) as ProbeReply.TimeExceeded).rttMs} ms"),
                    listOf("Shop server", "${(Icmp.ping(slowPath) as ProbeReply.EchoReply).rttMs} ms"),
                ),
            ),
        ),
        actions = listOf(
            CaseAction("Replace the home router again", false, "The router answers in 2 ms. The delay starts further away, so the new router changes nothing.", cost = 60),
            CaseAction("Reboot the modem", false, "The modem was fine. The big jump in the traceroute is a long link far behind it.", cost = 10),
            CaseAction("Tell Hana the slow link is overseas, not her router", true, "The time jumps by about 180 ms at the overseas link. That link belongs to the shop's host, so it is for them to fix, and the router stays."),
            CaseAction("Change the DNS server", false, "The name was found at once; the delay comes after that, on the path. DNS is not the slow part.", cost = 20),
        ),
        afterFix = Scan("Where the time goes", "Compare the round trip before and after the overseas link.", listOf("Hop", "Round trip"),
            listOf(homeRouter, isp2, seaLink).map { h -> listOf(h.name, "${rttTo(slowPath, h)} ms") }),
        hints = hints, concepts = concepts,
    )

    // Case 2: a routing loop
    private val loopA = hop("Office router", "10.0.0.2", 2)
    private val loopB = hop("Branch router", "10.0.0.3", 2)
    private val loopPath = Path(listOf(hop("Edge router", "10.0.0.1", 1)) + List(70) { if (it % 2 == 0) loopA else loopB }, destination = null)
    private val fixedPath = Path(listOf(hop("Edge router", "10.0.0.1", 1), loopA, hop("Branch gateway", "10.0.8.1", 3)), hop("Backup server", "10.0.8.50", 2))

    val loop = CaseFile(
        id = "icmp_loop", title = "Packets that go in circles",
        story = "The branch cannot reach the backup server. Pinging it does not simply fail: it says \"time to live exceeded\".",
        symptoms = listOf("Ping: \"TTL expired in transit\".", "The server itself is up.", "It broke after someone edited a route."),
        scans = listOf(
            Scan("Traceroute to the backup server", "The same two routers keep answering.", columns, trace(loopPath, maxHops = 7)),
            Scan(
                "Routes (static)", "Where each router sends packets for the backup network 10.0.8.0/24.",
                listOf("Router", "Next hop for 10.0.8.0/24"),
                listOf(listOf("Edge router", "Office router 10.0.0.2"), listOf("Office router", "Branch router 10.0.0.3"), listOf("Branch router", "Office router 10.0.0.2")),
            ),
        ),
        actions = listOf(
            CaseAction("Raise the TTL to 255", false, "The packets now go round more times before they die. They still never arrive.", cost = 20),
            CaseAction("Restart the backup server", false, "The server was never reached. The packets die in the loop between two routers.", cost = 30),
            CaseAction("Point the branch router's route at the real gateway", true, "The branch router now sends the packets on to 10.0.8.1 instead of back to the office router. The loop is gone."),
            CaseAction("Block ICMP so the error disappears", false, "The error message is gone, but the loop is still there. Now nobody will know why it fails.", cost = 20),
        ),
        afterFix = Scan("Traceroute now", "The path ends at the server.", columns, trace(fixedPath)),
        hints = hints, concepts = concepts,
    )

    // Case 3: a router that stays silent
    private val hiddenPath = Path(
        listOf(hop("Home router", "192.168.0.1", 1), hop("ISP gateway", "100.64.1.1", 4), hop("Hidden router", "100.64.9.9", 3, answers = false), hop("ISP core", "203.0.113.7", 5)),
        hop("Game server", "203.0.113.99", 4),
    )

    val silent = CaseFile(
        id = "icmp_silent", title = "The router with three stars",
        story = "Sora ran a traceroute for her game and saw a line of three stars in the middle. She is sure that router is broken and wants it reported.",
        symptoms = listOf("The game runs fine.", "The traceroute shows * * * on hop 3.", "The hops after it answer normally."),
        scans = listOf(
            Scan("Traceroute to the game server", "Hop 3 gives no answer, but the trace goes on past it.", columns, trace(hiddenPath)),
            Scan(
                "Ping the game server", "A ping travels through hop 3 to the end.",
                listOf("Target", "Result"),
                listOf(listOf("Game server", (Icmp.ping(hiddenPath) as ProbeReply.EchoReply).let { "${it.rttMs} ms · reply" })),
            ),
        ),
        actions = listOf(
            CaseAction("Report hop 3 to the ISP as broken", false, "The ISP checks and finds nothing wrong: packets pass through hop 3. It is only set not to answer TTL probes.", cost = 30),
            CaseAction("Replace the home router", false, "Hop 1 answers perfectly. The stars are three hops away and harmless.", cost = 60),
            CaseAction("Explain it: hop 3 is silent but forwarding fine", true, "A router can drop the packet whose TTL ran out without telling anyone. Since hops 4 and 5 answer and the ping works, the traffic passes hop 3."),
            CaseAction("Set the game's TTL to 1", false, "TTL 1 makes the very first router drop every packet. The game stops working altogether.", cost = 20),
        ),
        afterFix = Scan("What the stars mean", "A silent hop that forwards is not a fault.", listOf("Sign", "Meaning"),
            listOf(listOf("* * * then more hops", "silent router, traffic passes"), listOf("* * * to the end", "traffic is blocked or lost"))),
        hints = hints, concepts = concepts,
    )

    val all = listOf(slowHop, loop, silent)
}
