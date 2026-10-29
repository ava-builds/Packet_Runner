package com.avabuilds.packetrunner.network

/** A van's registration at its home agent: while it lasts, packets for [home] are tunneled to [careOf]. Times are in seconds. */
data class Binding(val home: Ipv4, val careOf: Ipv4, val registeredAt: Int, val lifetimeSec: Int) {
    fun isActive(now: Int): Boolean = now < registeredAt + lifetimeSec
    fun ageSec(now: Int): Int = now - registeredAt
}

/** What the home agent did with a packet addressed to a mobile device that lives on its network. */
sealed interface MobileDelivery {
    /** The home agent wrapped the packet in a new one addressed to the care-of address. */
    data class Tunneled(val outer: IpPacket) : MobileDelivery

    /** There is no active binding: the device looks as if it were at home, but it is away, so the packet is lost. */
    data class Lost(val reason: LostReason) : MobileDelivery
}

enum class LostReason { NEVER_REGISTERED, REGISTRATION_EXPIRED }

/** The router on the van's home network. It answers for the van's home address and tunnels packets to wherever the van is. */
class HomeAgent(val address: Ipv4) {
    private val table = linkedMapOf<Ipv4, Binding>()

    val bindings: List<Binding> get() = table.values.toList()

    fun register(binding: Binding) {
        table[binding.home] = binding
    }

    fun deregister(home: Ipv4) {
        table.remove(home)
    }

    fun deliver(packet: IpPacket, now: Int): MobileDelivery {
        val binding = table[packet.dst] ?: return MobileDelivery.Lost(LostReason.NEVER_REGISTERED)
        if (!binding.isActive(now)) return MobileDelivery.Lost(LostReason.REGISTRATION_EXPIRED)
        return MobileDelivery.Tunneled(MobileIp.encapsulate(packet, address, binding.careOf))
    }
}

/**
 * Ingress filtering: a router only lets packets leave its network when their source address belongs to that network,
 * so nobody can send packets pretending to be somebody else.
 */
object IngressFilter {
    fun allowsLeaving(packet: IpPacket, network: SubnetInfo): Boolean = network.contains(packet.src)
}

/** How the van answers: straight out (triangle routing) or back through the home agent (reverse tunnel). */
sealed interface ReplyRoute {
    /** The reply leaves the foreign network with the home address as its source. */
    data class Direct(val packet: IpPacket, val passed: Boolean) : ReplyRoute

    /** The reply is wrapped with the care-of address as source, so it passes, and the home agent unwraps and forwards it. */
    data class ReverseTunnel(val outer: IpPacket, val inner: IpPacket, val passed: Boolean) : ReplyRoute
}

object MobileReply {
    /** The van's reply to [peer], sent from the foreign network [foreign] while it uses [careOf]. */
    fun send(home: Ipv4, careOf: Ipv4, peer: Ipv4, homeAgent: Ipv4, foreign: SubnetInfo, reverseTunnel: Boolean): ReplyRoute {
        val reply = IpPacket(home, peer)
        return if (reverseTunnel) {
            val outer = MobileIp.encapsulate(reply, careOf, homeAgent) // src = care-of, dst = home agent
            ReplyRoute.ReverseTunnel(outer, reply, IngressFilter.allowsLeaving(outer, foreign))
        } else {
            ReplyRoute.Direct(reply, IngressFilter.allowsLeaving(reply, foreign))
        }
    }
}
