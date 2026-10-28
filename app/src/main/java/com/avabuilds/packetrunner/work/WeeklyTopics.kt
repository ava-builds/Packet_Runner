package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.network.LabGame

/** How the player can practise a topic in the game. */
sealed interface Practice {
    /** Play a Lab mini-game. */
    data class InLab(val game: LabGame) : Practice

    /** Use the Workbench subnet calculator. */
    data object OnWorkbench : Practice

    /** Wait for the right kind of customer job; [hint] says which. */
    data class OnTheJob(val hint: String) : Practice
}

/** One topic of the Computer Communication course, as a small card. Book names only the topic, never a chapter number. */
data class WeeklyTopic(
    val title: String,
    val korean: String,
    val summary: String,
    val points: List<String>,
    val book: String,
    val practice: Practice,
)

/**
 * The "Weekly topic": one topic per calendar week, in the order of the course, starting again after the last. The list
 * follows the topics of *Computer Networks: A Systems Approach*; it can be replaced by the real syllabus.
 */
object WeeklyTopics {
    const val CLAIM_XP = 15

    val all = listOf(
        WeeklyTopic(
            "Layers, bandwidth and latency", "계층 구조, 대역폭과 지연",
            "A network is built in layers, and its quality is measured by how much it carries and how long it takes.",
            listOf(
                "Each layer uses the one below it and hides its details from the one above.",
                "Bandwidth is how much data fits through per second. Latency is how long one packet takes.",
                "A full pipe holds delay × bandwidth bits.",
            ),
            "Foundation · Layering and performance", Practice.InLab(LabGame.WINDOW),
        ),
        WeeklyTopic(
            "Encoding and framing", "부호화와 프레이밍",
            "Bits must become signals on a wire, and a stream of bits must be cut into frames.",
            listOf(
                "NRZ is simple, but a long run of equal bits loses the clock.",
                "NRZI and Manchester add changes so the receiver can keep in step.",
                "Framing marks where one frame ends and the next begins.",
            ),
            "Getting Connected · Encoding and framing", Practice.InLab(LabGame.ENCODING),
        ),
        WeeklyTopic(
            "Error detection with CRC", "CRC 오류 검출",
            "The sender adds a few check bits so the receiver can tell whether a frame was damaged.",
            listOf(
                "A CRC is the remainder of dividing the data by an agreed generator.",
                "The receiver divides the whole frame: a remainder of zero means no error was found.",
                "A good CRC catches every burst of errors shorter than the check.",
            ),
            "Getting Connected · Error detection", Practice.InLab(LabGame.CRC),
        ),
        WeeklyTopic(
            "Reliable transmission and the sliding window", "신뢰성 있는 전송과 슬라이딩 윈도우",
            "Acknowledgements and timeouts make an unreliable link reliable.",
            listOf(
                "Stop-and-wait wastes the link while it waits for each acknowledgement.",
                "A sliding window keeps several frames in flight at once.",
                "A window as large as delay × bandwidth keeps the pipe full.",
            ),
            "Getting Connected · Reliable transmission", Practice.InLab(LabGame.WINDOW),
        ),
        WeeklyTopic(
            "Ethernet, hubs and switches", "이더넷, 허브와 스위치",
            "Ethernet shares a cable, so senders must handle collisions; switches avoid most of them.",
            listOf(
                "CSMA/CD: listen while sending, stop on a collision, wait a random time.",
                "A hub repeats every frame to everyone.",
                "A switch learns which port each address is on and forwards frames only where needed.",
            ),
            "Getting Connected · Ethernet; Internetworking · Switches", Practice.InLab(LabGame.LAN),
        ),
        WeeklyTopic(
            "Wireless links and the hidden node", "무선 링크와 숨은 단말 문제",
            "A radio cannot listen while it talks, so Wi-Fi avoids collisions instead of detecting them.",
            listOf(
                "Two devices that both reach the router may not hear each other: the hidden node problem.",
                "RTS/CTS lets a device ask permission first, and everyone who hears it waits.",
                "The farther from the router, the weaker the signal and the lower the speed.",
            ),
            "Getting Connected · Wireless links (802.11)", Practice.OnTheJob("Fix the cafe Wi-Fi when Hana calls."),
        ),
        WeeklyTopic(
            "IP addresses and subnets", "IP 주소와 서브넷",
            "An address has a network part and a host part; the mask says where one ends and the other begins.",
            listOf(
                "The network number is the address AND the mask.",
                "The broadcast address has every host bit set to 1.",
                "A /24 leaves 8 host bits, so 254 usable hosts.",
            ),
            "Internetworking · IP addressing and subnetting", Practice.OnWorkbench,
        ),
        WeeklyTopic(
            "Forwarding tables and routing", "포워딩 테이블과 라우팅",
            "A router looks up each packet's destination in its forwarding table to find the next hop.",
            listOf(
                "A row matches when (destination AND mask) equals its network number.",
                "When several rows match, the longest prefix wins.",
                "The default route 0.0.0.0/0 catches everything else.",
            ),
            "Internetworking · IP forwarding and routing", Practice.OnTheJob("Fix the city router when Dae calls from City Hall."),
        ),
        WeeklyTopic(
            "DHCP, NAT and mobility", "DHCP, NAT와 이동성",
            "Devices get addresses automatically, share one public address, and can move between networks.",
            listOf(
                "DHCP lends addresses from a pool for a lease time.",
                "NAT rewrites private addresses to one public address and remembers each connection in a table.",
                "Mobile IP keeps the home address and tunnels packets to the care-of address.",
            ),
            "Internetworking · Host configuration, NAT and mobility", Practice.InLab(LabGame.MOBILE),
        ),
        WeeklyTopic(
            "UDP and TCP", "UDP와 TCP",
            "The transport layer decides whether data is delivered fast or delivered completely and in order.",
            listOf(
                "UDP just sends: fast, but lost data stays lost.",
                "TCP resends lost data and delivers in order, but one loss makes everything behind it wait.",
                "Live video and calls like UDP; banks and file transfers need TCP.",
            ),
            "End-to-End Protocols · UDP and TCP", Practice.InLab(LabGame.TRANSPORT),
        ),
    )

    /** The week number of [day] (weeks start on Monday, counted from the week of 1970-01-01). */
    fun weekOf(day: WallTime): Long = Math.floorDiv(day.epochDay + 3, 7L)

    /** The topic of the week containing [day]. Every day of the same week gives the same topic. */
    fun forWeek(day: WallTime): WeeklyTopic = all[Math.floorMod(weekOf(day), all.size.toLong()).toInt()]
}
