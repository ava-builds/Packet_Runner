package com.avabuilds.packetrunner

/** One frame arriving at the switch. */
data class SwitchFrame(val src: String, val dst: String)

/** World 2 · Switch city. Content for the hands-on switch puzzle. */
object SwitchCityLevel {
    const val WORLD = 2
    const val PORT_COUNT = 3

    /** Hosts behind each port. A and B share port 1 through a hub. */
    val hosts: Map<Int, List<String>> = mapOf(
        1 to listOf("A", "B"),
        2 to listOf("C"),
        3 to listOf("D"),
    )

    fun portOf(host: String): Int = hosts.entries.first { host in it.value }.key

    val frames = listOf(
        SwitchFrame("A", "C"),
        SwitchFrame("C", "A"),
        SwitchFrame("B", "A"),
        SwitchFrame("D", "C"),
        SwitchFrame("A", "D"),
        SwitchFrame("C", "B"),
    )

    val briefing = Briefing(
        chapter = "World 2 · Switch city",
        title = "Learn the streets",
        nanaSays = "Pip, a switch is a city guide who learns who lives where just by " +
            "watching who sends frames. You run the switch now. Tap the ports to send each frame!",
        why = "A learning bridge (브리지) fills its MAC table automatically. When a frame arrives " +
            "it remembers the source on that port, then decides what to do with the destination.",
        howToThink = "The table already shows who the switch has learned. Destination not in it? " +
            "Send to every other port (flood). On the same port it came from? Send nowhere (filter). " +
            "On another port? Send only there (forward).",
        keywords = listOf(
            "Learning bridge · 학습 브리지",
            "Flooding · 플러딩",
            "Filtering · 필터링",
            "MAC table",
        ),
    )

    const val WHAT_REALLY_HAPPENED =
        "A bridge learns by looking at the SOURCE address of every frame, and decides by looking at " +
            "the DESTINATION. Unknown destination: flood. Destination on the same segment: filter. " +
            "Known destination elsewhere: forward. Over time most frames are forwarded to one port " +
            "instead of flooded, so the network stays efficient."
}
