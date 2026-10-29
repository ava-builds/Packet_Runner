package com.avabuilds.packetrunner.network

/** One network card: its IP address and its MAC address. */
data class Iface(val ip: String, val mac: String)

/** A frame on the wire for one hop of a packet's trip. The IP addresses are the packet's; the MAC addresses are this hop's. */
data class Frame(val hop: Int, val srcIp: String, val dstIp: String, val srcMac: String, val dstMac: String)

/** What happened when a host looked for the MAC address of an IP address on its own network. */
sealed interface ArpOutcome {
    /** The answer was already in the cache. */
    data class Cached(val mac: String) : ArpOutcome

    /** The host asked everyone ("who has this IP?") and the owner answered. */
    data class Learned(val mac: String) : ArpOutcome

    /** Nobody on the network owns that IP, so nobody answered. */
    data object NoAnswer : ArpOutcome
}

/**
 * ARP: a host that knows a neighbor's IP address asks the whole network for its MAC address
 * (a broadcast), remembers the answer in a cache, and uses it as the destination MAC of the frame.
 */
class ArpCache(initial: Map<String, String> = emptyMap()) {
    private val entries = linkedMapOf<String, String>().apply { putAll(initial) }

    val table: Map<String, String> get() = entries.toMap()

    /** Looks [ip] up in the cache; on a miss asks the [lan] (the other cards on the network). */
    fun resolve(ip: String, lan: List<Iface>): ArpOutcome {
        entries[ip]?.let { return ArpOutcome.Cached(it) }
        val owner = lan.firstOrNull { it.ip == ip } ?: return ArpOutcome.NoAnswer
        entries[ip] = owner.mac
        return ArpOutcome.Learned(owner.mac)
    }

    /** Forgets everything (like `arp -d`), so the next send asks again. */
    fun clear() = entries.clear()
}

object Arp {
    const val BROADCAST = "ff:ff:ff:ff:ff:ff"

    /**
     * The frames of one packet from [sender] to [receiver] through [routers] (each router is its card on the side
     * the packet comes in, then its card on the side it leaves). The IP addresses never change; every hop
     * has its own pair of MAC addresses.
     */
    fun frames(sender: Iface, routers: List<Pair<Iface, Iface>>, receiver: Iface): List<Frame> {
        val outgoing = listOf(sender) + routers.map { it.second }
        val incoming = routers.map { it.first } + receiver
        return outgoing.zip(incoming).mapIndexed { i, (from, to) ->
            Frame(i + 1, sender.ip, receiver.ip, from.mac, to.mac)
        }
    }

    /** True when the packet's IP addresses are the same on every hop while the MAC addresses change on every hop. */
    fun macChangesIpStays(frames: List<Frame>): Boolean =
        frames.map { it.srcIp to it.dstIp }.distinct().size == 1 &&
            frames.zipWithNext().all { (a, b) -> a.srcMac != b.srcMac && a.dstMac != b.dstMac }
}

/** The ARP jobs: a laptop that cannot leave its network, for two different reasons. */
object ArpCases {
    private val laptop = Iface("192.168.1.10", "aa:11:22:33:44:01")
    private val router = Iface("192.168.1.1", "aa:11:22:33:44:99")
    private val routerWan = Iface("10.0.0.1", "aa:11:22:33:55:02")
    private val server = Iface("10.0.0.50", "bb:77:66:55:44:33")
    private const val oldRouterMac = "aa:11:22:33:44:07"

    private val concepts = listOf(
        Concept(
            "ARP (Address Resolution Protocol)", "ARP (주소 결정 프로토콜)",
            "To send a frame to a neighbor on the same network, a host needs its MAC address. It asks everyone \"who has this IP?\", the owner answers, and the host keeps the answer in its ARP cache.",
            "Internetworking · Address translation (ARP)",
        ),
        Concept(
            "MAC changes at every hop, IP stays", "홉마다 MAC 주소는 바뀌고 IP 주소는 그대로",
            "The IP addresses name the two ends of the trip and never change. The MAC addresses only name the two ends of one link, so every router rewrites them.",
            "Internetworking · IP forwarding and address translation",
        ),
        Concept(
            "Default gateway", "기본 게이트웨이",
            "A host sends everything that is not on its own network to its default gateway, the router's address on that network. If that address is wrong, nothing leaves.",
            "Internetworking · Host configuration and forwarding",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. Open the scans first: one shows what the laptop remembers, one shows what it really sends.",
        "A frame goes to the MAC address in the ARP cache. If that address belongs to nobody, the frame is lost, whatever the IP says.",
        "Compare the destination MAC in the frame with the router's real MAC on its label.",
    )

    /** One line per frame: the IP pair (the same on every hop) above the MAC pair (new on every hop). */
    private fun frameRow(f: Frame) = listOf("${f.hop}", "${f.srcIp}\n→ ${f.dstIp}", "${f.srcMac}\n→ ${f.dstMac}")
    private fun frameRows(frames: List<Frame>) = frames.map(::frameRow)
    private val frameColumns = listOf("Hop", "IP: from → to", "MAC: from → to")

    /** The frames of a healthy trip, shown after the fix. */
    val healthyFrames: List<Frame> = Arp.frames(laptop, listOf(router to routerWan), server)

    private val healthy = Scan(
        "The trip now", "The IP addresses stay the same on both hops. The MAC addresses change at the router.",
        frameColumns, frameRows(healthyFrames),
    )

    /** The router was swapped, but the laptop still remembers the old one's MAC address. */
    val staleCache = CaseFile(
        id = "arp_stale", title = "Nothing leaves the laptop",
        story = "The office swapped the router over the weekend. Since then the laptop can open nothing on the Internet, although the Wi-Fi looks fine.",
        symptoms = listOf("The Internet does not load.", "Other laptops on the same Wi-Fi work.", "Pinging the router's IP address gets no answer."),
        scans = listOf(
            Scan(
                "ARP cache of the laptop", "What the laptop remembers about its neighbors.",
                listOf("IP address", "MAC address", "Age"),
                listOf(listOf(router.ip, oldRouterMac, "3 days"), listOf("192.168.1.20", "aa:11:22:33:44:05", "2 min")),
            ),
            Scan(
                "Frames the laptop sends", "A packet for the server, as frames on the wire. The router's real MAC is ${router.mac}.",
                frameColumns,
                listOf(frameRow(Frame(1, laptop.ip, server.ip, laptop.mac, oldRouterMac))),
            ),
        ),
        actions = listOf(
            CaseAction("Replace the Ethernet cable", false, "The cable was fine: the other laptops use the same one. The laptop still sends to the old MAC address.", cost = 30),
            CaseAction("Give the laptop a new IP address", false, "The IP address was never the problem. The frame still goes to a MAC address nobody owns.", cost = 20),
            CaseAction("Clear the laptop's ARP cache", true, "The laptop asked \"who has 192.168.1.1?\" and the new router answered. Everything loads again."),
            CaseAction("Reboot the new router", false, "The router was fine. The laptop still remembers the old address for hours.", cost = 20),
        ),
        afterFix = healthy,
        hints = hints, concepts = concepts,
    )

    /** The laptop's default gateway is set to an address no router owns, so its ARP question is never answered. */
    val wrongGateway = CaseFile(
        id = "arp_gateway", title = "The gateway that does not answer",
        story = "A new tenant set up a laptop by hand. It talks to the printer next door but cannot leave the building.",
        symptoms = listOf("The printer on the same network works.", "No website loads.", "The laptop says \"gateway not reachable\"."),
        scans = listOf(
            Scan(
                "ARP requests of the laptop", "Who the laptop asked for, and who answered.",
                listOf("Who has?", "Tell", "Answer"),
                listOf(
                    listOf("192.168.1.30 (printer)", laptop.ip, "aa:11:22:33:44:30"),
                    listOf("192.168.1.254 (gateway)", laptop.ip, "no answer ×3"),
                ),
            ),
            Scan(
                "Network settings and devices", "The gateway the laptop was given, and the router on the wall.",
                listOf("Item", "Address"),
                listOf(listOf("Laptop IP", laptop.ip), listOf("Default gateway (set by hand)", "192.168.1.254"), listOf("Router, LAN side", router.ip)),
            ),
        ),
        actions = listOf(
            CaseAction("Clear the laptop's ARP cache", false, "The laptop asked again for 192.168.1.254 and again nobody answered. The address is wrong, not the memory.", cost = 10),
            CaseAction("Replace the router", false, "The router was working: the other devices use it. Its address is 192.168.1.1, not .254.", cost = 60),
            CaseAction("Set the default gateway to 192.168.1.1", true, "The laptop asked \"who has 192.168.1.1?\", the router answered, and the Internet loads."),
            CaseAction("Move the laptop to the cable", false, "A cable does not change the gateway setting. The laptop still asks for an address nobody owns.", cost = 20),
        ),
        afterFix = healthy,
        hints = hints, concepts = concepts,
    )

    val all = listOf(staleCache, wrongGateway)
}
