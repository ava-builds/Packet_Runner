package com.avabuilds.packetrunner.game

/**
 * One level of the course-shaped progression: the topics it teaches, what opens (buildings, Lab games, the Workbench,
 * case files) and what Nana says when the player gets there. Everything is named by id, so this file needs no screens.
 */
data class Stage(
    val level: Int,
    /** "Houses", "Cafe", ... : the name of the level. */
    val title: String,
    val topics: List<String>,
    val buildingIds: List<String> = emptyList(),
    val labIds: List<String> = emptyList(),
    val caseIds: List<String> = emptyList(),
    val workbench: Boolean = false,
    /** What Nana says on the phone when the level is reached. */
    val nana: String,
)

/** Where a level stands on the path of the course for the player. */
enum class StageStatus { DONE, CURRENT, LOCKED }

/** One row of the course path: a level, how far the player is with it, and the XP that reaches it. */
data class RoadmapRow(val stage: Stage, val status: StageStatus, val xpToReach: Int)

/** The next thing to unlock: its level, the XP it needs, how much is still missing, and what it opens. */
data class NextUnlock(val stage: Stage, val xpToReach: Int, val xpLeft: Int, val text: String, val opens: List<String>)

/**
 * The progression follows the order of the Computer Communication course. A level is reached with XP
 * (see [Progress]); the level, not the day, decides which neighbors live in town, which Lab games and tools are open
 * and which case files can come. Saved games need nothing extra: the level is worked out from the saved XP.
 */
object Curriculum {
    const val OFFICE = "office"

    val stages: List<Stage> = listOf(
        Stage(
            1, "Houses",
            topics = listOf("Cables", "Repeater", "Bandwidth vs latency"),
            buildingIds = listOf("house1", "house2", "house3"),
            labIds = listOf("encoding"),
            nana = "Welcome to Network Town! The three houses need internet. Cables, a repeater and a little patience will do. The Lab's signal game is open too.",
        ),
        Stage(
            2, "Cafe",
            topics = listOf("Wi-Fi hidden node and exposed node", "RTS/CTS"),
            buildingIds = listOf("cafe"),
            labIds = listOf("crc"),
            caseIds = listOf("wifi_exposed", "wifi_hidden"),
            nana = "Level 2! The cafe is open, and its Wi-Fi is busy. Watch out for hidden and exposed nodes. The Lab has a new game: finding corrupted frames.",
        ),
        Stage(
            3, "Office tower",
            topics = listOf("Hub collisions", "CSMA/CD", "Switch and learning bridge"),
            buildingIds = listOf("tower"),
            labIds = listOf("window", "lan"),
            caseIds = listOf("tower_hub", "tower_moved"),
            nana = "Level 3! The office tower is finished. Its old hub shouts to every floor. Show them what a switch can do. Two new Lab games are open.",
        ),
        Stage(
            4, "City Hall",
            topics = listOf("Forwarding table", "Subnet masks", "ARP"),
            buildingIds = listOf("cityhall"),
            caseIds = listOf("arp_stale", "arp_gateway"),
            workbench = true,
            nana = "Level 4! City Hall opens, with a router that needs its subnet masks. Your Workbench is ready too: it does the subnet maths for you.",
        ),
        Stage(
            5, "Apartments",
            topics = listOf("DHCP pool and lease", "NAT", "Port forwarding"),
            buildingIds = listOf("apartments"),
            nana = "Level 5! The Apartments share one Internet address, and addresses are running out. Time for DHCP, NAT and port forwarding.",
        ),
        Stage(
            6, "Troubleshooting",
            topics = listOf("MTU and fragmentation", "ICMP", "Ping", "Traceroute"),
            caseIds = listOf("frag_camera", "frag_vpn", "icmp_slow", "icmp_loop", "icmp_silent"),
            nana = "Level 6! Now you are a real troubleshooter. Big packets, slow hops, silent routers: read the scans carefully.",
        ),
        Stage(
            7, "On the move",
            topics = listOf("Mobile IP (delivery van)", "TCP vs UDP (streamer and bank)"),
            labIds = listOf("mobile", "transport"),
            caseIds = listOf("van_expired", "van_filtered", "transport_stream", "transport_bank"),
            nana = "Level 7! The last topics: a delivery van that moves between networks, and a streamer and a bank that need opposite things. Two more Lab games are open.",
        ),
    )

    val maxLevel: Int = stages.size

    fun stage(level: Int): Stage = stages[level.coerceIn(1, maxLevel) - 1]

    /** The level after [level], or null at the top. */
    fun next(level: Int): Stage? = stages.getOrNull(level)

    fun levelOf(xp: Int): Int = Progress.levelOf(xp).coerceAtMost(maxLevel)

    private fun upTo(level: Int) = stages.filter { it.level <= level }

    /** The office is always open (it is home base); every other building opens with its level. */
    fun buildingOpen(buildingId: String, level: Int): Boolean =
        buildingId == OFFICE || upTo(level).any { buildingId in it.buildingIds }

    /** The level a building opens at, or null for the office (always open). */
    fun buildingLevel(buildingId: String): Int? = stages.firstOrNull { buildingId in it.buildingIds }?.level

    fun labOpen(labId: String, level: Int): Boolean = upTo(level).any { labId in it.labIds }

    /** The level a Lab game opens at. */
    fun labLevel(labId: String): Int = stages.first { labId in it.labIds }.level

    fun workbenchOpen(level: Int): Boolean = upTo(level).any { it.workbench }

    /** Every case file that may come at [level]: all those of this level and the earlier ones. */
    fun caseIds(level: Int): List<String> = upTo(level).flatMap { it.caseIds }

    /**
     * The order in which case files come at [level]: the newest topics alternate with the earlier ones, so the player
     * meets the new topic first and keeps practising the old ones. Empty while no case file is open.
     */
    fun caseRotation(level: Int): List<String> {
        val newestStage = upTo(level).lastOrNull { it.caseIds.isNotEmpty() } ?: return emptyList()
        val newest = newestStage.caseIds
        val older = caseIds(level) - newest.toSet()
        if (older.isEmpty()) return newest
        val length = 2 * maxOf(newest.size, older.size)
        return List(length) { i -> if (i % 2 == 0) newest[i / 2 % newest.size] else older[i / 2 % older.size] }
    }

    /** The case file for the next case job, given how many case jobs were already done. Null while none is open. */
    fun pickCase(level: Int, doneBefore: Int): String? {
        val rotation = caseRotation(level)
        return if (rotation.isEmpty()) null else rotation[doneBefore % rotation.size]
    }

    /** All levels with their state for a player with [xp]: the ones reached, the one they are in, and the ones still ahead. */
    fun roadmap(xp: Int): List<RoadmapRow> {
        val level = levelOf(xp)
        return stages.map { s ->
            RoadmapRow(s, when { s.level < level -> StageStatus.DONE; s.level == level -> StageStatus.CURRENT; else -> StageStatus.LOCKED }, Progress.xpToReach(s.level))
        }
    }

    /** What comes next for a player with [xp], or null at the top level. [labTitles] names the Lab games it opens. */
    fun nextUnlock(xp: Int, labTitles: Map<String, String> = emptyMap()): NextUnlock? {
        val level = levelOf(xp)
        val next = next(level) ?: return null
        val needed = Progress.xpToReach(next.level)
        return NextUnlock(next, needed, (needed - xp.coerceAtLeast(0)).coerceAtLeast(0), nextUnlockText(xp) ?: "", whatsNew(next.level, labTitles))
    }

    /** A short line for the top bar: "15 / 100 XP → Cafe", or just the XP at the top level. */
    fun hudLine(xp: Int): String {
        val p = Progress.progress(xp)
        val next = next(levelOf(xp))
        return if (next == null) "${p.xpIntoLevel} / ${p.xpForLevel} XP" else "${p.xpIntoLevel} / ${p.xpForLevel} XP → ${next.title}"
    }

    /** "Level 3: Office tower opens at 300 XP", or null at the top level. */
    fun nextUnlockText(xp: Int): String? {
        val level = levelOf(xp)
        val next = next(level) ?: return null
        return "Level ${next.level}: ${next.title} opens at ${Progress.xpToReach(next.level)} XP"
    }

    /**
     * What a level opens, as short lines for a card ("A new place in town: Cafe", "Lab game: ..."). [labTitles] gives the names of the
     * Lab games by id; an id without a name is shown as it is.
     */
    fun whatsNew(level: Int, labTitles: Map<String, String> = emptyMap()): List<String> {
        val s = stage(level)
        return buildList {
            if (s.buildingIds.isNotEmpty()) add("A new place in town: ${s.title}")
            if (s.workbench) add("The Workbench")
            s.labIds.forEach { add("Lab game: ${labTitles[it] ?: it}") }
            if (s.caseIds.isNotEmpty()) add("${s.caseIds.size} new case jobs")
            add("Topics: ${s.topics.joinToString(", ")}")
        }
    }
}
