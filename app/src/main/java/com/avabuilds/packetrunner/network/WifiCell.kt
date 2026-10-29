package com.avabuilds.packetrunner.network

/** A Wi-Fi device standing at position [x] (in meters on a line) and tuned to a [channel]. */
data class AirNode(val name: String, val x: Int, val channel: Int = 1)

/** One conversation: [from] has data for [to] in every time slot. */
data class Flow(val from: String, val to: String) {
    override fun toString() = "$from→$to"
}

enum class SlotOutcome { SENT, DEFERRED, COLLIDED }

/** What happened to each flow in one time slot. */
data class SlotResult(val slot: Int, val outcomes: Map<Flow, SlotOutcome>)

/** How two conversations relate to each other. */
enum class Interplay { INDEPENDENT, EXPOSED, HIDDEN }

/**
 * A small Wi-Fi network on a line. A device hears another when they are on the same channel and no farther apart than [range].
 * Devices use carrier sense (CSMA/CA): before sending, a device that hears someone else sending waits.
 * With [rtsCts] on, a device also waits when it hears the receiver of a conversation (the receiver announces "clear to send").
 */
class WifiCell(nodes: List<AirNode>, val range: Int) {
    private val byName = nodes.associateBy { it.name }

    private fun node(name: String) = byName.getValue(name)

    fun hears(a: String, b: String): Boolean {
        if (a == b) return false
        val na = node(a)
        val nb = node(b)
        return na.channel == nb.channel && Math.abs(na.x - nb.x) <= range
    }

    /** Both conversations are heard by the other's sender: the sender must wait although nothing would collide. */
    fun interplay(a: Flow, b: Flow): Interplay {
        val sendersHearEachOther = hears(a.from, b.from)
        val aDisturbsB = hears(a.from, b.to)
        val bDisturbsA = hears(b.from, a.to)
        return when {
            sendersHearEachOther && !aDisturbsB && !bDisturbsA -> Interplay.EXPOSED
            !sendersHearEachOther && (aDisturbsB || bDisturbsA) -> Interplay.HIDDEN
            else -> Interplay.INDEPENDENT
        }
    }

    /**
     * Runs [slots] time slots. In slot i the conversations try in a rotating order, so everybody gets a turn.
     * A conversation waits if its sender hears one that is already sending (or, with [rtsCts], hears its receiver).
     * Those that send collide when their receiver also hears another sender.
     */
    fun simulate(flows: List<Flow>, slots: Int, rtsCts: Boolean = false): List<SlotResult> =
        (0 until slots).map { slot ->
            val order = flows.indices.map { flows[(it + slot) % flows.size] }
            val sending = mutableListOf<Flow>()
            val deferred = mutableSetOf<Flow>()
            for (flow in order) {
                val mustWait = sending.any { other -> hears(flow.from, other.from) || (rtsCts && hears(flow.from, other.to)) }
                if (mustWait) deferred += flow else sending += flow
            }
            val outcomes = flows.associateWith { flow ->
                when {
                    flow in deferred -> SlotOutcome.DEFERRED
                    sending.any { other -> other != flow && hears(flow.to, other.from) } -> SlotOutcome.COLLIDED
                    else -> SlotOutcome.SENT
                }
            }
            SlotResult(slot + 1, outcomes)
        }

    companion object {
        /** The share of all chances to send (in percent) that really delivered something. */
        fun utilizationPercent(results: List<SlotResult>): Int {
            val total = results.sumOf { it.outcomes.size }
            if (total == 0) return 0
            val sent = results.sumOf { r -> r.outcomes.values.count { it == SlotOutcome.SENT } }
            return Math.round(sent * 100.0 / total).toInt()
        }
    }
}
