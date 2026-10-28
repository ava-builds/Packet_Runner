package com.avabuilds.packetrunner.network

// ---------------------------------------------------------------------------
// Mobile IP: following a driver who moves between networks
// ---------------------------------------------------------------------------

/** A network the driver visits. [careOf] is the temporary address the foreign agent gives; null for the home network. */
data class MobilePlace(val name: String, val careOf: Ipv4?)

/** What the home agent does with a packet addressed to the driver's home address. */
sealed interface HomeAgentAction {
    data object DeliverLocally : HomeAgentAction
    data class Tunnel(val careOf: Ipv4) : HomeAgentAction
}

/**
 * The driver keeps one permanent home address. The home agent remembers where the driver is now (the care-of
 * address) and tunnels packets there; at home it delivers them normally.
 */
class MobileIpSession(
    val homeAddress: Ipv4,
    val homeAgent: Ipv4,
    val places: List<MobilePlace>,
) {
    /** The care-of address the home agent knows about, or null when it thinks the driver is at home. */
    var binding: Ipv4? = null
        private set

    var current: MobilePlace? = null
        private set

    fun moveTo(placeIndex: Int) {
        current = places[placeIndex]
    }

    /** The driver tells the home agent where it is now. At home this removes the binding. */
    fun register() {
        binding = current?.careOf
    }

    /** What the home agent must do with the next packet for the driver. */
    fun correctAction(): HomeAgentAction = binding?.let { HomeAgentAction.Tunnel(it) } ?: HomeAgentAction.DeliverLocally

    fun isRight(choice: HomeAgentAction): Boolean = choice == correctAction()

    /** The packet the home agent sends into the tunnel for a packet from [friend], or null when it just delivers it. */
    fun tunnelled(friend: Ipv4): IpPacket? =
        binding?.let { MobileIp.encapsulate(IpPacket(friend, homeAddress), homeAgent, it) }

    /** What the foreign agent hands to the driver: the original packet, with the outer header removed. */
    fun delivered(friend: Ipv4): IpPacket = tunnelled(friend)?.let { MobileIp.decapsulate(it) } ?: IpPacket(friend, homeAddress)

    companion object {
        /** The story: a delivery driver goes from the cafe to the depot and then home. */
        fun driver() = MobileIpSession(
            homeAddress = Ipv4.parse("192.168.1.50"),
            homeAgent = Ipv4.parse("192.168.1.1"),
            places = listOf(
                MobilePlace("Cafe", Ipv4.parse("10.5.0.77")),
                MobilePlace("Depot", Ipv4.parse("172.20.3.21")),
                MobilePlace("Home", null),
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// UDP and TCP: who needs what from the network
// ---------------------------------------------------------------------------

enum class Transport(val label: String) { UDP("UDP"), TCP("TCP") }

/** What one segment went through. [deliveredAt] is null when it never arrived. */
data class SegmentFate(val index: Int, val sentAt: Int, val deliveredAt: Int?, val lost: Boolean, val retransmitted: Boolean) {
    val lag: Int? get() = deliveredAt?.minus(sentAt)
}

data class TransferRun(val fates: List<SegmentFate>) {
    val missing: Int get() = fates.count { it.deliveredAt == null }
    val worstLag: Int get() = fates.mapNotNull { it.lag }.maxOrNull() ?: 0
}

object TransportSim {
    /** One tick to cross the network. */
    const val TRAVEL = 1

    /** TCP notices a lost segment after this many ticks and sends it again. */
    const val RETRANSMIT_AFTER = 4

    /**
     * Sends [segments] segments, one per tick, over a network that loses the ones in [lost] the first time.
     * UDP just sends: lost segments never arrive, the others come on time.
     * TCP resends lost segments and hands data up in order, so every segment waits behind a lost one.
     */
    fun run(transport: Transport, segments: Int, lost: Set<Int>): TransferRun {
        val raw = (0 until segments).map { i ->
            if (i in lost) i + RETRANSMIT_AFTER + TRAVEL else i + TRAVEL // when it first (or again) arrives
        }
        val fates = (0 until segments).map { i ->
            when (transport) {
                Transport.UDP -> SegmentFate(i, i, if (i in lost) null else raw[i], lost = i in lost, retransmitted = false)
                Transport.TCP -> SegmentFate(i, i, (0..i).maxOf { raw[it] }, lost = i in lost, retransmitted = i in lost)
            }
        }
        return TransferRun(fates)
    }
}

/** An application with what it needs from the network, and the network it has to live with. */
data class TransportApp(
    val name: String,
    val story: String,
    val segments: Int,
    val lost: Set<Int>,
    /** How many missing segments are fine (a glitch in a video, a click in a call). */
    val missingAllowed: Int,
    /** The longest a segment may take; null means the app does not care how long it takes. */
    val maxLag: Int?,
    val bestTransport: Transport,
) {
    enum class Verdict { GOOD, STUTTERS, DATA_MISSING }

    fun judge(transport: Transport): Verdict {
        val run = TransportSim.run(transport, segments, lost)
        return when {
            run.missing > missingAllowed -> Verdict.DATA_MISSING
            maxLag != null && run.worstLag > maxLag -> Verdict.STUTTERS
            else -> Verdict.GOOD
        }
    }
}

object TransportApps {
    val all = listOf(
        TransportApp("Live stream", "Viewers watch a match live. A skipped frame is a tiny glitch; a long pause is a disaster.", 12, setOf(3, 8), missingAllowed = 2, maxLag = 2, bestTransport = Transport.UDP),
        TransportApp("Bank transfer", "Every byte of the amount and the account number must arrive, in order. Waiting a little is fine.", 8, setOf(2), missingAllowed = 0, maxLag = null, bestTransport = Transport.TCP),
        TransportApp("Voice call", "Two friends talk. A click in the sound is fine, but they cannot wait for old words.", 12, setOf(5), missingAllowed = 2, maxLag = 2, bestTransport = Transport.UDP),
        TransportApp("Photo upload", "A photo with a missing piece is broken. It may take a moment.", 10, setOf(1, 6), missingAllowed = 0, maxLag = null, bestTransport = Transport.TCP),
    )
}
