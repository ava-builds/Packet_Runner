package com.avabuilds.packetrunner.network

/** The delivery van jobs: a driver on the move whose address must keep working, seen from two sides. */
object MobileIpCases {
    private fun ip(text: String) = Ipv4.parse(text)

    private val concepts = listOf(
        Concept(
            "Mobile IP: home address and care-of address", "모바일 IP: 홈 주소와 임시(Care-of) 주소",
            "A moving device keeps one permanent home address that everyone else uses. While it is away, it also has a care-of address on the network it visits, and its home agent forwards packets there.",
            "Internetworking · Mobile IP",
        ),
        Concept(
            "Home agent, registration and tunneling", "홈 에이전트, 등록, 터널링",
            "The device registers its care-of address with the home agent for a limited time. The home agent wraps each packet in a new packet addressed to the care-of address (IP-in-IP); at the other end it is unwrapped.",
            "Internetworking · Mobile IP (tunneling)",
        ),
        Concept(
            "Ingress filtering and the reverse tunnel", "입구 필터링과 역터널링",
            "A network drops packets that leave it with a source address from another network. So a visiting device that answers with its home address is blocked; a reverse tunnel wraps the reply in a packet from the care-of address.",
            "Internetworking · Mobile IP (routing and security issues)",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. Read the scans like a parcel's trip: who holds the van's address, and where does the packet go next?",
        "The home agent only tunnels while the registration is alive. A registration has a lifetime, and the van must renew it.",
        "A packet leaving a network must carry that network's own address as its source. Look at the source of the van's reply.",
    )

    private fun packetText(p: IpPacket) = "${p.src} → ${p.dst}"
    private val packetColumns = listOf("Step", "Packet")

    private val dispatcher = ip("203.0.113.10")
    private val home = ip("10.1.0.77")
    private val homeAgentAddress = ip("10.1.0.1")
    private val careOf = ip("192.0.2.45")
    private val foreign = SubnetInfo(ip("192.0.2.1"), 24)

    // Case 1: the registration expired
    private val agent = HomeAgent(homeAgentAddress).also { it.register(Binding(home, careOf, registeredAt = 0, lifetimeSec = 600)) }
    private const val lateNow = 900
    private val inner = IpPacket(dispatcher, home)
    private val lostPacket = agent.deliver(inner, lateNow) as MobileDelivery.Lost
    private val renewed = HomeAgent(homeAgentAddress).also { it.register(Binding(home, careOf, registeredAt = 880, lifetimeSec = 600)) }
    private val tunneled = (renewed.deliver(inner, lateNow) as MobileDelivery.Tunneled).outer

    val expired = CaseFile(
        id = "van_expired", title = "The driver who vanished",
        story = "The dispatcher can message the delivery van's tablet while it is parked at the depot, but since the van left for the other side of town, nothing gets through. The tablet itself has a good signal.",
        symptoms = listOf("Messages to the van's usual address get no answer.", "The tablet shows a strong signal in its new area.", "It worked right after the driver left."),
        scans = listOf(
            Scan(
                "Home agent's table", "Who the home agent forwards for, and until when. Now is ${lateNow} s.",
                listOf("Home address", "Care-of address", "Registered", "Status"),
                agent.bindings.map {
                    listOf("${it.home}", "${it.careOf}", "${it.ageSec(lateNow)} s ago · ${it.lifetimeSec} s", if (it.isActive(lateNow)) "active" else "expired")
                },
            ),
            Scan(
                "A message to the van", "The dispatcher writes to the van's home address.",
                packetColumns,
                listOf(
                    listOf("Dispatcher sends", packetText(inner)),
                    listOf("Home agent", "no active registration: ${if (lostPacket.reason == LostReason.REGISTRATION_EXPIRED) "expired" else "none"}, packet lost"),
                ),
            ),
        ),
        actions = listOf(
            CaseAction("Give the van a new permanent address", false, "Everybody who knows the old address now loses the van, and open connections break. The problem was the registration, not the address.", cost = 40),
            CaseAction("Restart the dispatcher's computer", false, "The dispatcher's packets leave fine. They are dropped at the home agent, because nobody registered the van there any more.", cost = 20),
            CaseAction("Make the van register its care-of address again", true, "The van tells its home agent where it is now. The home agent starts tunneling the dispatcher's packets to the van's care-of address again."),
            CaseAction("Ask the driver to drive back to the depot", false, "It works at home, but that defeats the purpose of a mobile device. It is not a fix.", cost = 10),
        ),
        afterFix = Scan(
            "The trip after the fix", "The inner packet never changes. The home agent adds an outer one for the middle of the trip.",
            packetColumns,
            listOf(
                listOf("Dispatcher → home agent", packetText(inner)),
                listOf("Home agent → foreign network", packetText(tunneled) + " (carries the message)"),
                listOf("Foreign agent → van", packetText(MobileIp.decapsulate(tunneled))),
            ),
        ),
        hints = hints, concepts = concepts,
    )

    // Case 2: the reply is blocked by ingress filtering
    private val direct = MobileReply.send(home, careOf, dispatcher, homeAgentAddress, foreign, reverseTunnel = false) as ReplyRoute.Direct
    private val reverse = MobileReply.send(home, careOf, dispatcher, homeAgentAddress, foreign, reverseTunnel = true) as ReplyRoute.ReverseTunnel

    val filtered = CaseFile(
        id = "van_filtered", title = "Messages in, silence out",
        story = "At the new depot the dispatcher's messages reach the van, but the van's replies never get back. The dispatcher sees the tablet as dead, while the driver sees every message arrive.",
        symptoms = listOf("The van receives, but nothing it sends arrives.", "It happens only at depots in other cities.", "At home the replies work."),
        scans = listOf(
            Scan(
                "The van's reply", "The van answers with its permanent home address as the source.",
                packetColumns + listOf("Network"),
                listOf(listOf("Van sends", packetText(direct.packet), "${foreign.network}/${foreign.prefix}")),
            ),
            Scan(
                "The depot router's rule", "A router lets a packet leave only if its source belongs to the network.",
                listOf("Source", "Belongs to ${foreign.network}/${foreign.prefix}?", "Result"),
                listOf(listOf("${direct.packet.src}", if (foreign.contains(direct.packet.src)) "yes" else "no", if (direct.passed) "passes" else "dropped")),
            ),
        ),
        actions = listOf(
            CaseAction("Send every reply from the care-of address only", false, "The reply passes, but the dispatcher's session is with the home address. It sees an unknown sender and the connection breaks.", cost = 30),
            CaseAction("Ask the depot to switch off its source check", false, "That is a security rule of someone else's network. They will not change it for one van, and it protects them from forged packets.", cost = 20),
            CaseAction("Turn on the reverse tunnel", true, "The reply is wrapped in a packet from the care-of address to the home agent, which passes the depot's check. The home agent unwraps it and forwards it as coming from the home address."),
            CaseAction("Move the home agent to the new city", false, "The home agent stays where the van's home network is. Moving it does not change the depot's source check.", cost = 60),
        ),
        afterFix = Scan(
            "The reply through the reverse tunnel", "The outer packet is what the depot router sees.",
            packetColumns + listOf("Depot router"),
            listOf(
                listOf("Van → home agent (outer)", packetText(reverse.outer), if (reverse.passed) "passes" else "dropped"),
                listOf("Home agent → dispatcher (inner)", packetText(reverse.inner), "—"),
            ),
        ),
        hints = hints, concepts = concepts,
    )

    val all = listOf(expired, filtered)
}
