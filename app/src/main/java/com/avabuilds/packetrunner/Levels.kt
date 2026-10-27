package com.avabuilds.packetrunner

/** Text Nana shows before a level starts. */
data class Briefing(
    val chapter: String,
    val title: String,
    val nanaSays: String,
    val why: String,
    val howToThink: String,
    val keywords: List<String>,
    /** Where the book covers this topic (topic names only: chapter numbers differ by edition). */
    val book: String,
)

/** A wrong answer, kept so the player can review it later (Mistake Notebook). */
data class Mistake(
    val question: String,
    val yourAnswer: String,
    val correctAnswer: String,
    val explanation: String,
)

data class LevelResult(
    val cleared: Boolean,
    val stars: Int,
    val mistakes: List<Mistake>,
)

/** One packet that arrives at the router. */
data class RouterRound(
    val dest: Ipv4,
    val oldMac: String,
    val newMac: String,
)

/** World 3 · Router station. Forwarding table from 4장 (router R2). */
object RouterStationLevel {
    const val START_TTL = 64

    val table = listOf(
        Route(Ipv4.parse("128.96.34.0"), 25, "if0"),
        Route(Ipv4.parse("128.96.34.128"), 25, "if1"),
        Route(Ipv4.parse("128.96.33.0"), 24, "R3"),
    )

    val rounds = listOf(
        RouterRound(Ipv4.parse("128.96.34.139"), "E6-E9-00-17-BB-4B", "1A-23-F9-CD-06-9B"),
        RouterRound(Ipv4.parse("128.96.34.15"), "1A-23-F9-CD-06-9B", "49-BD-D2-C7-56-2A"),
        RouterRound(Ipv4.parse("128.96.33.200"), "49-BD-D2-C7-56-2A", "88-B2-2F-54-1A-0F"),
    )

    val briefing = Briefing(
        chapter = "World 3 · Router station",
        title = "Find the exit",
        nanaSays = "Pip, you just arrived at router R2. Routers don't know the whole map. " +
            "They only decide where you go next!",
        why = "Every router has a forwarding table (포워딩 테이블). It compares your destination " +
            "IP with each row and sends you out of one interface: the next hop (다음 홉). " +
            "(A routing table is different: routing protocols build it, and it feeds the forwarding table.)",
        howToThink = "For each row, put the mask stencil on the destination: D AND mask. " +
            "If the result equals the row's network number, that row is your exit. " +
            "Then change only your MAC raincoat. Your IP coat never changes!",
        keywords = listOf(
            "Forwarding table · 포워딩 테이블",
            "Subnet mask · 서브넷 마스크",
            "AND 연산",
            "Next hop · 다음 홉",
        ),
        book = "Internetworking · IP datagram forwarding, subnetting and ARP. A forwarding table is " +
            "what a router uses for each packet; a routing table is built by routing protocols.",
    )

    const val WHAT_REALLY_HAPPENED =
        "At every router, the IP header keeps the same source and destination address " +
            "(only TTL goes down by 1). The Ethernet frame is rebuilt with new MAC addresses " +
            "for the next link, and the router uses ARP to find the next hop's MAC address. " +
            "The router chose the next hop by checking (D AND mask) == network for each row of " +
            "its forwarding table."

    fun starsFor(mistakeCount: Int): Int = when {
        mistakeCount == 0 -> 3
        mistakeCount <= 2 -> 2
        else -> 1
    }
}
