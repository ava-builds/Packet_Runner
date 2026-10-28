package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.RequestType

/** A networking idea the player just used, in English and Korean, with where the book covers it. */
data class Concept(val en: String, val ko: String, val explanation: String, val book: String)

/** How many of a device the player may place, and what each costs. */
data class Allowance(val price: Int, val maxCount: Int)

/** What an app on a client needs from the network. [label] is shown next to the device in the room. */
data class Goal(val label: String, val minSpeedMbps: Int = 0, val maxLatencyMs: Int = Int.MAX_VALUE)

/** The short note shown under a device in the room, for example "needs ≥ 45 Mbps". */
val Goal.shortText: String
    get() = when {
        minSpeedMbps > 0 -> "needs ≥ $minSpeedMbps Mbps"
        maxLatencyMs != Int.MAX_VALUE -> "needs ≤ $maxLatencyMs ms"
        else -> ""
    }

/** How a client is doing compared with what it needs. */
enum class Verdict { OK, OFFLINE, TOO_SLOW, TOO_LAGGY }

/**
 * One job: the room as the customer left it, what the player may buy, what some devices need,
 * what Nana says when the player is stuck, and which concepts the job teaches.
 * [parCost] is the cheapest known way to solve it, which sets the star rating.
 */
class Scenario(
    val id: String,
    val title: String,
    val goal: String,
    val scene: NetworkScene,
    val allowed: Map<DeviceType, Allowance>,
    val cablePricePerCell: Int,
    val hints: List<String>,
    private val baseConcepts: List<Concept>,
    val parCost: Int,
    val goals: Map<String, Goal> = emptyMap(),
    /** Show the Glitch jamming the air while there are collisions. */
    val hasGlitch: Boolean = false,
    /** Whether the Scan message points to the router's Settings when devices collide. */
    val pointsToSettings: Boolean = true,
) {
    /** The same job with a different price for a cell of cable (the shop's cable spool makes it cheaper). */
    fun withCablePrice(price: Int): Scenario = Scenario(
        id, title, goal, scene, allowed, price, hints, baseConcepts, parCost, goals, hasGlitch, pointsToSettings,
    )

    fun verdict(client: ClientReport): Verdict {
        if (client.status != ClientStatus.CONNECTED) return Verdict.OFFLINE
        val need = goals[client.deviceId] ?: return Verdict.OK
        return when {
            client.speedMbps < need.minSpeedMbps -> Verdict.TOO_SLOW
            client.latencyMs > need.maxLatencyMs -> Verdict.TOO_LAGGY
            else -> Verdict.OK
        }
    }

    /** Solved when every client is online and gets what its app needs. */
    fun isSolved(report: NetworkReport) = report.clients.isNotEmpty() && report.clients.all { verdict(it) == Verdict.OK }

    /** One sentence about the first client that is not happy yet, or null when everybody is. */
    fun firstProblem(report: NetworkReport, scene: NetworkScene): String? {
        val client = report.clients.firstOrNull { verdict(it) != Verdict.OK } ?: return null
        val name = scene.device(client.deviceId)?.displayName ?: "Device"
        val need = goals[client.deviceId]
        return when (verdict(client)) {
            Verdict.OFFLINE ->
                if ((client.status == ClientStatus.COLLISIONS || client.status == ClientStatus.NO_IP) && pointsToSettings) "$name: ${client.reason} Have a look at the router's Settings."
                else "$name: ${client.reason}"
            Verdict.TOO_SLOW -> "$name: only ${client.speedMbps} Mbps, but ${need?.label}. A repeater halves the speed, so keep this device close to the router."
            Verdict.TOO_LAGGY -> "$name: ${client.latencyMs} ms is too laggy, because ${need?.label}. Every hop and every repeater adds delay."
            Verdict.OK -> null
        }
    }

    /**
     * 3 stars for spending close to the cheapest solution, 2 for a bit more, 1 otherwise.
     * Jobs that need no purchase at all still get a small allowance, so buying nothing is the only way to 3 stars.
     */
    fun stars(spent: Int): Int {
        val base = maxOf(parCost, MIN_PAR)
        return when {
            spent <= base * 115 / 100 -> 3
            spent <= base * 160 / 100 -> 2
            else -> 1
        }
    }

    /** A tip for good work, in won. */
    fun tip(stars: Int): Int = (stars - 1) * 10

    private companion object {
        const val MIN_PAR = 20
    }

    /** The concepts to show after solving: the core ones, plus one for every special device used. */
    fun conceptsUsed(finalScene: NetworkScene): List<Concept> {
        val extra = buildList {
            if (finalScene.devices.any { it.type == DeviceType.REPEATER }) add(Concepts.REPEATER)
            if (finalScene.devices.any { it.type == DeviceType.ACCESS_POINT }) add(Concepts.ACCESS_POINT)
        }
        return baseConcepts + extra
    }
}

object Concepts {
    val ROUTER = Concept(
        "Router · DHCP", "라우터 · DHCP (IP 주소 자동 할당)",
        "A router joins your home network to the internet and gives every device an IP address.",
        "Internetworking · IP, and host configuration with DHCP",
    )
    val CABLE = Concept(
        "Ethernet cable (wired link)", "이더넷 케이블 (유선 링크)",
        "A cable carries the signal from the wall socket to the router. A cable has a maximum length.",
        "Getting Connected · Ethernet links",
    )
    val WIFI_RANGE = Concept(
        "Wi-Fi range and signal strength", "무선 LAN 통신 범위와 신호 세기",
        "Wi-Fi reaches only so far. The farther a device is from the router, the weaker the signal.",
        "Getting Connected · Wireless links (802.11)",
    )
    val BANDWIDTH_LATENCY = Concept(
        "Bandwidth and latency", "대역폭과 지연",
        "Bandwidth is how much data fits through per second. Latency is how long one packet takes. A weak signal lowers the first and raises the second.",
        "Foundation · Performance: bandwidth and latency",
    )
    val HIDDEN_NODE = Concept(
        "Hidden node problem", "숨은 단말 문제",
        "Two devices can both reach the router but not each other, so they talk at the same time and their messages collide.",
        "Getting Connected · Wireless links (802.11)",
    )
    val RTS_CTS = Concept(
        "RTS/CTS", "RTS/CTS · 충돌 회피 (CSMA/CA)",
        "A device asks permission first (RTS), the router answers (CTS), and everyone who hears it waits. Collisions are avoided before they happen.",
        "Getting Connected · Wireless links (802.11)",
    )
    val COLLISION_AVOIDANCE = Concept(
        "Collision avoidance, not detection", "충돌 회피 (CSMA/CA)",
        "Ethernet can listen while it talks and detect collisions. A radio can't, so Wi-Fi listens first and avoids collisions instead.",
        "Getting Connected · Ethernet and wireless media access",
    )
    val APP_NEEDS = Concept(
        "Different apps need different things", "애플리케이션마다 다른 요구 (대역폭 vs 지연)",
        "Streaming video needs bandwidth. A video call needs low latency. The same network can be fine for one and bad for the other.",
        "Foundation · Performance and application requirements",
    )
    val REPEATER = Concept(
        "Repeater", "리피터 (중계기)",
        "A repeater passes the Wi-Fi signal on and extends the range, but it shares the air with the router, so speed halves and delay grows.",
        "Getting Connected · Links and the devices that extend them",
    )
    val DHCP_POOL = Concept(
        "DHCP address pool", "DHCP 주소 풀",
        "A DHCP server hands out IP addresses from a pool. When every address is taken, the next device joins the Wi-Fi but gets no IP address and cannot use the internet.",
        "Internetworking · Host configuration with DHCP",
    )
    val DHCP_LEASE = Concept(
        "DHCP lease time", "DHCP 임대 시간",
        "An address is only lent for a while. A short lease gives addresses back quickly when a device leaves. A long lease keeps them busy, so the pool can run dry.",
        "Internetworking · Host configuration with DHCP",
    )
    val FORWARDING_TABLE = Concept(
        "Forwarding table", "포워딩 테이블",
        "A router looks at the destination address of every packet and finds the matching row in its forwarding table. The row names the next hop.",
        "Internetworking · IP forwarding and routing",
    )
    val SUBNET_MASK = Concept(
        "Subnet mask (prefix length)", "서브넷 마스크 (프리픽스 길이)",
        "The mask says how many leading bits are the network number. A row matches when the destination AND the mask equals the row's network.",
        "Internetworking · Subnetting and classless addressing (CIDR)",
    )
    val LONGEST_PREFIX = Concept(
        "Longest prefix match", "최장 프리픽스 매칭",
        "When several rows match, the one with the longest prefix (the most specific network) wins. A small network can sit inside a bigger one.",
        "Internetworking · Subnetting and classless addressing (CIDR)",
    )
    val DEFAULT_ROUTE = Concept(
        "Default route", "기본 경로",
        "The row 0.0.0.0/0 matches every address. Packets that match nothing else leave through it, usually towards the Internet.",
        "Internetworking · IP forwarding and routing",
    )
    val CRC = Concept(
        "CRC (cyclic redundancy check)", "CRC (순환 중복 검사)",
        "The sender divides its data by an agreed generator and adds the remainder to the frame. The receiver divides the whole frame: a remainder of zero means no error was found.",
        "Getting Connected · Error detection (CRC)",
    )
    val LINE_ENCODING = Concept(
        "Line encoding (NRZ, NRZI, Manchester)", "선로 부호화 (NRZ, NRZI, 맨체스터)",
        "Bits must become signal levels. NRZ is simple but long runs of equal bits lose the clock. NRZI and Manchester add changes so the receiver can keep in step.",
        "Getting Connected · Encoding",
    )
    val SLIDING_WINDOW = Concept(
        "Sliding window", "슬라이딩 윈도우",
        "The sender may have a window of frames in flight before the first acknowledgement returns. A window as large as the delay times the bandwidth keeps the pipe full.",
        "Getting Connected · Reliable transmission (sliding window)",
    )
    val PRIVATE_ADDRESSES = Concept(
        "Private addresses", "사설 주소",
        "Addresses such as 192.168.x.x are used only inside a home or building. They are not routed on the Internet, so many places can reuse the same ones.",
        "Internetworking · Network address translation (NAT)",
    )
    val NAT_TABLE = Concept(
        "NAT translation table", "NAT 변환 테이블",
        "A NAT router rewrites each outgoing packet to use the one public address and a port of its own, and writes the pair into a table. Replies are matched against the table to find the right device. A packet nobody asked for has no row and is dropped.",
        "Internetworking · Network address translation (NAT)",
    )
    val PORT_FORWARDING = Concept(
        "Port forwarding", "포트 포워딩",
        "To let outsiders reach a server inside, you add a fixed row to the NAT table: this public port always goes to that device.",
        "Internetworking · Network address translation (NAT)",
    )
    val CSMA_CD = Concept(
        "Ethernet collisions (CSMA/CD)", "이더넷 충돌 (CSMA/CD)",
        "On a shared cable, two senders can talk at once. Ethernet listens while it sends, notices the collision, and each sender waits a random time (backoff) before trying again. The wait range doubles after every collision.",
        "Getting Connected · Ethernet (CSMA/CD)",
    )
    val LEARNING_BRIDGE = Concept(
        "Switch and learning bridge", "스위치와 학습 브리지",
        "A hub repeats every frame to everyone. A switch learns which port each address is on from the frames it sees, then forwards a frame only to the right port. It floods only when it does not know the destination yet.",
        "Internetworking · Switches and learning bridges",
    )
    val MOBILE_IP = Concept(
        "Mobile IP", "모바일 IP",
        "A moving device keeps its home address. At a new network it gets a temporary care-of address and tells its home agent. The home agent tunnels packets for the home address to the care-of address, where the outer header is removed.",
        "Internetworking · Mobility and Mobile IP",
    )
    val UDP_TCP = Concept(
        "UDP or TCP", "UDP와 TCP",
        "UDP just sends: fast, but lost data stays lost. TCP resends lost data and delivers it in order, so everything arrives, but a loss makes everything behind it wait. Live video and calls want UDP; banks and file transfers want TCP.",
        "End-to-End Protocols · UDP and TCP",
    )
    val ACCESS_POINT = Concept(
        "Access point", "액세스 포인트 (AP)",
        "An access point adds Wi-Fi to a wired network. It needs a cable to the router, and it does not hand out IP addresses itself.",
        "Getting Connected · Wireless links (802.11)",
    )
}

/** Every concept the game teaches, for the notebook's topic picker. */
val Concepts.all: List<Concept>
    get() = listOf(
        Concepts.ROUTER, Concepts.CABLE, Concepts.WIFI_RANGE, Concepts.BANDWIDTH_LATENCY, Concepts.HIDDEN_NODE,
        Concepts.RTS_CTS, Concepts.COLLISION_AVOIDANCE, Concepts.APP_NEEDS, Concepts.REPEATER, Concepts.DHCP_POOL,
        Concepts.DHCP_LEASE, Concepts.ACCESS_POINT, Concepts.FORWARDING_TABLE, Concepts.SUBNET_MASK,
        Concepts.LONGEST_PREFIX, Concepts.DEFAULT_ROUTE, Concepts.CRC, Concepts.LINE_ENCODING, Concepts.SLIDING_WINDOW, Concepts.PRIVATE_ADDRESSES, Concepts.NAT_TABLE, Concepts.PORT_FORWARDING, Concepts.CSMA_CD, Concepts.LEARNING_BRIDGE, Concepts.MOBILE_IP, Concepts.UDP_TCP,
    )

object Scenarios {
    private const val CABLE_PRICE = 5

    private val allowedForInstall = mapOf(
        DeviceType.ROUTER to Allowance(price = 80, maxCount = 1),
        DeviceType.ACCESS_POINT to Allowance(price = 50, maxCount = 1),
        DeviceType.REPEATER to Allowance(price = 40, maxCount = 1),
        DeviceType.SWITCH to Allowance(price = 30, maxCount = 1),
    )

    private val installHints = listOf(
        "Nana on the radio. The internet comes out of the wall socket, and a router needs a cable from it. Try the Cable tool.",
        "Try the Scan tool. Pip will walk along the cables and tell you who is online and who is not.",
        "Wi-Fi only reaches so far. Move the router closer to whoever is offline, or add a repeater in between.",
    )

    private val demandingHints = listOf(
        "Nana on the radio. The internet comes out of the wall socket, and a router needs a cable from it. Try the Cable tool.",
        "Try the Scan tool. Pip will walk along the cables and tell you who is online and who is not.",
        "Look at the little notes under the TV and the laptop. Some devices need more than just being online.",
        "Devices join the CLOSEST signal, not the best one. A repeater halves the speed and adds delay, so keep the TV and the video call close to the router.",
        "Devices that can't hear each other collide when they talk at the same time. In the router's Settings, RTS/CTS makes them ask permission first.",
    )

    private val installConcepts = listOf(Concepts.ROUTER, Concepts.CABLE, Concepts.WIFI_RANGE, Concepts.BANDWIDTH_LATENCY)
    private val demandingConcepts = installConcepts + Concepts.APP_NEEDS

    /** The INSTALL job for the neighbor's house. Each house has its own room. */
    fun install(buildingId: String): Scenario = when (buildingId) {
        "house1" -> Scenario(
            id = "install_house1", title = "Set up the internet", goal = "Get the laptop and the phone online.",
            scene = NetworkScene(
                width = 7, height = 5,
                devices = listOf(
                    Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2)),
                    Device("laptop", DeviceType.LAPTOP, CellPos(5, 1)),
                    Device("phone", DeviceType.PHONE, CellPos(4, 4)),
                ),
            ),
            allowed = allowedForInstall, cablePricePerCell = CABLE_PRICE, hints = installHints,
            baseConcepts = installConcepts, parCost = 90,
        )

        "house3" -> Scenario(
            id = "install_house3", title = "Set up the internet", goal = "Everyone online, and the TV and the video call happy.",
            scene = NetworkScene(
                width = 11, height = 5,
                devices = listOf(
                    Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2)),
                    Device("tv", DeviceType.TV, CellPos(3, 3)),
                    Device("laptop", DeviceType.LAPTOP, CellPos(5, 0)),
                    Device("phone", DeviceType.PHONE, CellPos(10, 2)),
                ),
            ),
            allowed = allowedForInstall, cablePricePerCell = CABLE_PRICE, hints = demandingHints,
            baseConcepts = demandingConcepts, parCost = 110,
            goals = mapOf(
                "tv" to Goal("streaming needs at least 45 Mbps", minSpeedMbps = 45),
                "laptop" to Goal("a video call needs 35 ms or less", maxLatencyMs = 35),
            ),
        )

        else -> Scenario(
            id = "install_$buildingId", title = "Set up the internet", goal = "Get the laptop and the phone online.",
            scene = NetworkScene(
                width = 9, height = 5,
                devices = listOf(
                    Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2)),
                    Device("laptop", DeviceType.LAPTOP, CellPos(4, 1)),
                    Device("phone", DeviceType.PHONE, CellPos(7, 3)),
                ),
            ),
            allowed = allowedForInstall, cablePricePerCell = CABLE_PRICE, hints = installHints,
            baseConcepts = installConcepts, parCost = 100,
        )
    }

    private val fixHints = listOf(
        "Nana on the radio. Don't buy anything yet! First use Scan and see who is having trouble.",
        "Collisions mean two devices talked at the same time. The router hears both of them, but they are too far apart to hear each other.",
        "That is the hidden node problem. The router can make devices ask permission before they talk. Look at its Settings.",
        "Switch on RTS/CTS. A device sends a short request (RTS), the router answers (CTS), and everybody else waits. No more collisions, and it costs nothing.",
    )

    private val fixConcepts = listOf(Concepts.HIDDEN_NODE, Concepts.RTS_CTS, Concepts.COLLISION_AVOIDANCE)

    private val allowedForFix = mapOf(
        DeviceType.REPEATER to Allowance(price = 40, maxCount = 1),
        DeviceType.ACCESS_POINT to Allowance(price = 50, maxCount = 1),
    )

    /**
     * The FIX job. The network already exists: a router cabled to the wall socket, and clients that
     * sit too far apart to hear each other, so they collide. The cure is a setting, not a purchase.
     */
    fun fix(buildingId: String): Scenario {
        val cafe = buildingId == "cafe"
        val router = Device("router_1", DeviceType.ROUTER, CellPos(4, 2), name = "Router")
        val socket = Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2))
        val devices = if (cafe) {
            listOf(
                socket, router,
                Device("window_laptop", DeviceType.LAPTOP, CellPos(1, 0), name = "Window laptop"),
                Device("card_reader", DeviceType.POS, CellPos(3, 3), name = "Card reader"),
                Device("patio_phone", DeviceType.PHONE, CellPos(8, 2), name = "Patio phone"),
            )
        } else {
            listOf(
                socket, router,
                Device("back_laptop", DeviceType.LAPTOP, CellPos(1, 1), name = "Back room laptop"),
                Device("living_phone", DeviceType.PHONE, CellPos(7, 3), name = "Living room phone"),
            )
        }
        val wall = setOf(Cable.between("socket", "router_1"))
        return Scenario(
            id = "fix_$buildingId",
            title = if (cafe) "Fix the cafe Wi-Fi" else "Fix the Wi-Fi",
            goal = if (cafe) "Find out why the Wi-Fi keeps dropping, and fix it." else "Find out why the internet keeps cutting out, and fix it.",
            scene = NetworkScene(width = 9, height = 5, devices = devices, cables = wall, fixedCables = wall),
            allowed = allowedForFix,
            cablePricePerCell = CABLE_PRICE,
            hints = fixHints,
            baseConcepts = fixConcepts,
            parCost = 0,
            goals = if (cafe) mapOf("card_reader" to Goal("a card payment needs 35 ms or less", maxLatencyMs = 35)) else emptyMap(),
            hasGlitch = true,
            pointsToSettings = false,
        )
    }

    private val helpHints = listOf(
        "Nana on the radio. Scan first. Everyone is in range, so look at what the Scan says.",
        "The Wi-Fi works, but some devices have no IP address. The router's DHCP server hands those out.",
        "The pool is the list of addresses the router can lend. Guests who already left may still be holding some. Look at the router's Settings.",
        "Two cures: make the pool bigger, or make the lease shorter so addresses come back sooner. Neither costs anything.",
    )

    private val helpConcepts = listOf(Concepts.ROUTER, Concepts.DHCP_POOL, Concepts.DHCP_LEASE)

    /**
     * The HELP job. The Wi-Fi is fine, but the router's DHCP pool is nearly used up by guests who
     * already left, so the last devices get no IP address. The cure is a setting, not a purchase.
     */
    fun help(buildingId: String): Scenario {
        val cafe = buildingId == "cafe"
        // RTS/CTS is already on and the devices are spread out, so collisions are not part of this job
        val devices = listOf(
            Device("socket", DeviceType.WALL_SOCKET, CellPos(0, 2)),
            Device("router_1", DeviceType.ROUTER, CellPos(4, 2), name = "Router"),
            Device("laptop_a", DeviceType.LAPTOP, CellPos(1, 0), name = if (cafe) "Customer laptop" else "Guest laptop"),
            Device("laptop_b", DeviceType.LAPTOP, CellPos(7, 0), name = if (cafe) "Student laptop" else "Dad's laptop"),
            Device("phone_a", DeviceType.PHONE, CellPos(1, 4), name = if (cafe) "Customer phone" else "Guest phone"),
            Device("phone_b", DeviceType.PHONE, CellPos(7, 4), name = if (cafe) "Barista phone" else "New phone"),
            Device("tv", DeviceType.TV, CellPos(4, 4), name = if (cafe) "Menu screen" else "TV"),
        )
        val wall = setOf(Cable.between("socket", "router_1"))
        return Scenario(
            id = "help_$buildingId",
            title = "Out of IP addresses",
            goal = "Find out why some devices have no IP address, and fix it.",
            scene = NetworkScene(
                width = 9, height = 5,
                devices = devices.map { if (it.id == "router_1") it.copy(settings = DeviceSettings(rtsCts = true, dhcpPoolSize = 8, leaseMinutes = 120)) else it },
                cables = wall, fixedCables = wall,
                // five devices have left, so only three of the eight addresses are free
                staleLeaseAges = listOf(20, 45, 70, 90, 110),
            ),
            allowed = emptyMap(),
            cablePricePerCell = CABLE_PRICE,
            hints = helpHints,
            baseConcepts = helpConcepts,
            parCost = 0,
        )
    }

    /** The room for a request, or null for jobs that have their own screen. */
    fun forRequest(request: Request): Scenario? = when (request.type) {
        RequestType.INSTALL -> install(request.buildingId)
        RequestType.FIX -> fix(request.buildingId)
        RequestType.HELP -> help(request.buildingId)
        RequestType.ROUTE, RequestType.NAT -> null // these jobs have their own screens (RoutingScreen, NatScreen)
    }
}
