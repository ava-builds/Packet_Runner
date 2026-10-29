package com.avabuilds.packetrunner.network

/** The streamer and the bank: the same network, and two apps that need opposite things from it (UDP against TCP). */
object TransportCases {
    private val concepts = listOf(
        Concept(
            "UDP: fast, no promises", "UDP: 빠르지만 보장은 없음",
            "UDP just sends datagrams. Nothing is resent, so a lost one is simply gone, but nothing waits either. It suits live video and calls, where old data is worthless.",
            "End-to-End Protocols · UDP",
        ),
        Concept(
            "TCP: reliable and in order", "TCP: 신뢰성 있는 순서 보장 전송",
            "TCP acknowledges data and resends what was lost, and it hands data to the application in order. Nothing is missing at the end, but everything behind a lost segment has to wait for its resend.",
            "End-to-End Protocols · TCP",
        ),
        Concept(
            "Choose the transport for the app", "응용 프로그램에 맞는 전송 프로토콜 선택",
            "Ask what the app can live without. A bank cannot lose a single byte but can wait; a live stream can lose a frame but cannot wait. The first needs TCP, the second UDP.",
            "End-to-End Protocols · UDP and TCP",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. Look at when each segment arrives, and at which ones never arrive at all.",
        "TCP gives data to the app in order. One lost segment makes every segment after it wait for the resend.",
        "Ask what hurts more for this app: a missing piece, or a long wait?",
    )

    private val columns = listOf("Segment", "Sent", "Arrives", "What happened")

    private fun rows(run: TransferRun, maxLag: Int?): List<List<String>> = run.fates.map { f ->
        val note = when {
            f.deliveredAt == null -> "lost, never arrives"
            f.retransmitted -> "lost, sent again"
            maxLag != null && (f.lag ?: 0) > maxLag -> "waits behind a lost one"
            else -> "on time"
        }
        listOf("${f.index + 1}", "tick ${f.index}", f.deliveredAt?.let { "tick $it" } ?: "—", note)
    }

    private fun summaryRow(transport: Transport, run: TransferRun, maxLag: Int?): List<String> {
        val lagText = "${run.worstLag} ticks"
        val verdict = when {
            run.missing > 0 -> "${run.missing} missing"
            maxLag != null && run.worstLag > maxLag -> "freezes"
            else -> "smooth"
        }
        return listOf(transport.label, lagText, "${run.missing}", verdict)
    }

    private val summaryColumns = listOf("Transport", "Longest wait", "Missing", "Result")

    // Case 1: the streamer
    private val live = TransportApps.all.first { it.name == "Live stream" }
    private val liveTcp = TransportSim.run(Transport.TCP, live.segments, live.lost)
    private val liveUdp = TransportSim.run(Transport.UDP, live.segments, live.lost)

    val streamer = CaseFile(
        id = "transport_stream", title = "The match that keeps freezing",
        story = "A streamer broadcasts a match live. The viewers say the picture freezes for a moment every few seconds and then races to catch up, although every frame is eventually shown.",
        symptoms = listOf("Short freezes, then a burst of fast-forward.", "No frame is ever missing.", "The Internet connection tests fine."),
        scans = listOf(
            Scan("The stream over TCP", "Frames 4 and 9 are lost once. Everything after a lost one waits for its resend. Limit for a live picture: ${live.maxLag} ticks.", columns, rows(liveTcp, live.maxLag)),
            Scan("TCP against UDP", "The same frames and the same losses, sent both ways.", summaryColumns, listOf(summaryRow(Transport.TCP, liveTcp, live.maxLag), summaryRow(Transport.UDP, liveUdp, live.maxLag))),
        ),
        actions = listOf(
            CaseAction("Ask for a faster Internet plan", false, "The frames are not slow, they are held back. Waiting for a resent frame takes the same time on any plan.", cost = 60),
            CaseAction("Open several TCP connections", false, "Each connection still hands its data over in order, so each one freezes behind its own lost frame.", cost = 30),
            CaseAction("Send the live stream over UDP", true, "Lost frames are simply skipped, a tiny glitch. Nothing waits for a resend, so the picture stays smooth."),
            CaseAction("Send every frame twice over TCP", false, "Twice the traffic, and the order rule is the same: a lost frame still holds up the ones behind it.", cost = 20),
        ),
        afterFix = Scan("The stream over UDP", "Lost frames are skipped; every other frame arrives on time.", columns, rows(liveUdp, live.maxLag)),
        hints = hints, concepts = concepts,
    )

    // Case 2: the bank
    private val bank = TransportApps.all.first { it.name == "Bank transfer" }
    private val bankUdp = TransportSim.run(Transport.UDP, bank.segments, bank.lost)
    private val bankTcp = TransportSim.run(Transport.TCP, bank.segments, bank.lost)

    val bankCase = CaseFile(
        id = "transport_bank", title = "The transfer that lost a piece",
        story = "A bank's new app sends transfer instructions in small datagrams. Customers report that some transfers go through with a wrong amount or are rejected, but only now and then, and the bank's logs show no error.",
        symptoms = listOf("Now and then an instruction arrives incomplete.", "The network shows no errors.", "Customers must try again, and some are charged twice."),
        scans = listOf(
            Scan("The transfer over UDP", "Segment 3 is lost once. Nobody notices and nobody resends it.", columns, rows(bankUdp, null)),
            Scan("UDP against TCP", "The same transfer and the same loss, sent both ways.", summaryColumns, listOf(summaryRow(Transport.UDP, bankUdp, null), summaryRow(Transport.TCP, bankTcp, null))),
        ),
        actions = listOf(
            CaseAction("Send each instruction twice over UDP", false, "Both copies can be lost, and when both arrive the bank may carry out the transfer twice.", cost = 30),
            CaseAction("Add a checksum to the data", false, "UDP already has one. A checksum finds damaged data, it does not bring back data that never arrived.", cost = 20),
            CaseAction("Send the transfers over TCP", true, "TCP notices the missing segment, asks for it again and hands everything over in order. A little late, but complete."),
            CaseAction("Move to a faster network", false, "Speed does not stop a segment from being lost now and then. The data is still missing when it happens.", cost = 50),
        ),
        afterFix = Scan("The transfer over TCP", "The lost segment is sent again. Everything arrives, in order, a little late.", columns, rows(bankTcp, null)),
        hints = hints, concepts = concepts,
    )

    val all = listOf(streamer, bankCase)
}
