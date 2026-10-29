package com.avabuilds.packetrunner.network

/** The fragmentation jobs: big data on a path with a small MTU, seen from two sides. */
object FragmentCases {
    private val concepts = listOf(
        Concept(
            "MTU and fragmentation", "MTU와 단편화",
            "Every link has a maximum packet size, its MTU. A router cuts a bigger datagram into fragments that fit; only the receiver puts them back together.",
            "Internetworking · IP fragmentation and reassembly",
        ),
        Concept(
            "Fragment offset (in 8-byte units)", "단편 오프셋 (8바이트 단위)",
            "Each fragment says where its data starts in the original, counted in units of 8 bytes, and whether more fragments follow. That is how the receiver rebuilds the datagram in order.",
            "Internetworking · IP packet format",
        ),
        Concept(
            "One lost fragment loses the datagram", "단편 하나가 사라지면 데이터그램 전체가 손실",
            "The receiver can only rebuild the datagram when every fragment arrived. The more fragments, the smaller the chance, so it is best to send packets that fit the path.",
            "Internetworking · IP fragmentation and reassembly",
        ),
    )

    private val hints = listOf(
        "Nana on the radio. Open both scans: what was sent, and what really arrived.",
        "A fragment's offset times 8 is the byte where its data starts. Add its length: the next fragment should start right there.",
        "If a gap is left, the receiver throws the whole datagram away. Fewer fragments, fewer chances to lose one.",
    )

    private fun fragRow(f: IpFragment) = listOf("${f.byteOffset / 8}", "${f.dataLength}", if (f.moreFragments) "1 (more)" else "0 (last)")
    private val fragColumns = listOf("Offset (×8 bytes)", "Data length", "More fragments")

    // Case 1: the camera
    private const val cameraData = 4000
    private const val linkMtu = 1500
    val cameraFragments: List<IpFragment> = Fragmentation.fragment(7, cameraData, linkMtu)
    private val cameraArrived = cameraFragments.filterIndexed { i, _ -> i != 1 } // the middle one is lost on the long link
    private val cameraGap: IntRange = (Fragmentation.reassemble(cameraArrived) as Reassembly.Incomplete).gaps.first()
    private val shrunk = Fragmentation.fragment(8, 1400 - Fragmentation.HEADER, linkMtu)

    val camera = CaseFile(
        id = "frag_camera", title = "The camera that keeps freezing",
        story = "The cafe's menu-board camera sends its pictures to the server in big 4000-byte datagrams over a flaky link. The picture freezes again and again.",
        symptoms = listOf("Most pictures never show up.", "The link is up and fast.", "A speed test is fine."),
        scans = listOf(
            Scan(
                "What the camera sends", "One datagram of $cameraData bytes of data, cut for a link with an MTU of $linkMtu.",
                fragColumns, cameraFragments.map(::fragRow),
            ),
            Scan(
                "What the server received", "The middle fragment never arrived.",
                fragColumns, cameraArrived.map(::fragRow) + listOf(listOf("gap", "bytes ${cameraGap.first}–${cameraGap.last}", "missing")),
            ),
        ),
        actions = listOf(
            CaseAction("Buy a faster Internet plan", false, "The link was never short of speed. One fragment still goes missing now and then, and the whole picture goes with it.", cost = 60),
            CaseAction("Set the camera's MTU to 9000", false, "The link in the middle still has an MTU of $linkMtu, so the big frames are cut or dropped there anyway.", cost = 20),
            CaseAction("Make the camera send 1400-byte packets", true, "Every packet now fits in one piece. Nothing is cut up, so there is no fragment to lose and the picture is steady."),
            CaseAction("Ask the server to retry putting it together", false, "The server cannot rebuild what it never received. The missing bytes were lost on the way.", cost = 10),
        ),
        afterFix = Scan(
            "A packet now", "A 1400-byte packet is a single fragment: nothing to lose, nothing to rebuild.",
            fragColumns, shrunk.map(::fragRow),
        ),
        hints = hints, concepts = concepts,
    )

    // Case 2: the VPN
    private const val vpnMtu = 1400
    private val probeSizes = listOf(1000, 1372, 1400, 1472)

    private fun probeText(result: Probe) = when (result) {
        Probe.Delivered -> "reply"
        is Probe.Fragmented -> "reply (${result.pieces} pieces)"
        Probe.TooBigDropped -> "no reply"
    }

    private fun probeRows(df: Boolean, mtu: Int) = probeSizes.map { size ->
        listOf("$size", "${size + Fragmentation.HEADER}", probeText(Fragmentation.probe(size, mtu, df)))
    }

    private val probeColumns = listOf("Ping data (bytes)", "Packet size", "Result")
    private val afterFixFragments = Fragmentation.fragment(9, 3000, vpnMtu)

    val vpn = CaseFile(
        id = "frag_vpn", title = "Small pages load, big ones hang",
        story = "Since the office joined a VPN, chat and small pages work, but big uploads and some pages hang forever. The VPN link has a smaller MTU than the laptop thinks, and the \"too big\" warnings are blocked on the way.",
        symptoms = listOf("Small packets get through.", "Large uploads freeze at the start.", "Everything worked before the VPN."),
        scans = listOf(
            Scan(
                "Pings with \"don't fragment\"", "The laptop sends pings of growing size and cannot cut them up.",
                probeColumns, probeRows(df = true, mtu = vpnMtu),
            ),
            Scan(
                "MTU along the path", "The smallest MTU on the path decides the biggest packet that fits.",
                listOf("Link", "MTU"),
                listOf(listOf("Laptop Wi-Fi", "1500"), listOf("Office router", "1500"), listOf("VPN tunnel", "$vpnMtu"), listOf("Server", "1500")),
            ),
        ),
        actions = listOf(
            CaseAction("Turn the Wi-Fi off and on", false, "The connection came back, but the laptop still sends 1500-byte packets that the tunnel drops. Big uploads hang again.", cost = 10),
            CaseAction("Replace the office router", false, "The router was fine. The packets are too big for the VPN tunnel, not for the router.", cost = 60),
            CaseAction("Set the laptop's MTU to $vpnMtu", true, "Now every packet fits through the tunnel. Big uploads start and finish."),
            CaseAction("Set the laptop's MTU to 9000", false, "A bigger MTU makes it worse: the tunnel drops even more of the packets.", cost = 20),
        ),
        afterFix = Scan(
            "A 3000-byte send now", "The packets fit the tunnel. Offsets still count in units of 8 bytes.",
            fragColumns, afterFixFragments.map(::fragRow),
        ),
        hints = hints, concepts = concepts,
    )

    val all = listOf(camera, vpn)
}
