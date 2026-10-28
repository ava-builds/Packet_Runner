package com.avabuilds.packetrunner.game

/** How a link between the office and a building looks on the Network Monitor. */
enum class LinkStatus {
    /** The building has no network yet, so there is nothing to watch. */
    NO_LINK,
    HEALTHY,

    /** Packets are getting lost: a problem has started, but the neighbor has not phoned yet. */
    DEGRADED,

    /** The neighbor already called (or is about to), so the problem is no secret any more. */
    DOWN,
}

enum class FlagOutcome { EARLY_CATCH, ALREADY_REPORTED, TOO_LATE, FALSE_ALARM, NO_LINK }

data class FlagResult(val state: GameState, val outcome: FlagOutcome, val bonus: Int = 0)

/**
 * The office Network Monitor. A problem at a connected place shows up in packet loss
 * [WARNING_LEAD] game minutes before the neighbor phones. Reporting it in that window pays a bonus.
 * Everything here is derived from the day's calls, so it needs no extra randomness.
 */
object NetworkMonitor {
    const val WARNING_LEAD = 150f
    const val EARLY_BONUS = 40
    const val EXTRA_FOR_VERY_EARLY = 20
    const val FALSE_ALARM_COST = 15

    /** The calls that will (or did) come from a broken network at [neighbor]'s place. */
    private fun problems(state: GameState, neighbor: Neighbor): List<Request> =
        (state.upcoming + listOfNotNull(state.ringing, state.active))
            .filter { it.neighborId == neighbor.id && it.type != RequestType.INSTALL }

    fun status(state: GameState, neighbor: Neighbor): LinkStatus {
        if (neighbor.id !in state.connected || neighbor.movesInDay > state.day) return LinkStatus.NO_LINK
        val open = problems(state, neighbor)
        return when {
            open.any { it === state.ringing || it === state.active || it.callMinute <= state.minute } -> LinkStatus.DOWN
            open.any { state.minute >= it.callMinute - WARNING_LEAD } -> LinkStatus.DEGRADED
            else -> LinkStatus.HEALTHY
        }
    }

    /** True once the player has reported the problem that is currently showing at [neighbor]'s place. */
    fun isReported(state: GameState, neighbor: Neighbor): Boolean =
        problems(state, neighbor).any { it.id in state.caught }

    /** The player taps a building on the monitor to report a problem there. */
    fun report(state: GameState, neighbor: Neighbor): FlagResult {
        val status = status(state, neighbor)
        return when (status) {
            LinkStatus.NO_LINK -> FlagResult(state, FlagOutcome.NO_LINK)
            LinkStatus.DOWN -> FlagResult(state, FlagOutcome.TOO_LATE)
            LinkStatus.HEALTHY -> FlagResult(
                state.copy(
                    money = (state.money - FALSE_ALARM_COST).coerceAtLeast(0),
                    todayFalseAlarms = state.todayFalseAlarms + 1,
                ),
                FlagOutcome.FALSE_ALARM,
            )
            LinkStatus.DEGRADED -> {
                val call = problems(state, neighbor).first { state.minute >= it.callMinute - WARNING_LEAD }
                if (call.id in state.caught) return FlagResult(state, FlagOutcome.ALREADY_REPORTED)
                val earliness = ((call.callMinute - state.minute) / WARNING_LEAD).coerceIn(0f, 1f)
                val bonus = EARLY_BONUS + (earliness * EXTRA_FOR_VERY_EARLY).toInt()
                FlagResult(
                    state.copy(
                        money = state.money + bonus,
                        caught = state.caught + call.id,
                        todayCaught = state.todayCaught + 1,
                        todayBonus = state.todayBonus + bonus,
                        xp = state.xp + Progress.EARLY_CATCH_XP,
                    ),
                    FlagOutcome.EARLY_CATCH,
                    bonus,
                )
            }
        }
    }
}

/** One packet on a link: how far along it is (0 = office, 1 = building) and whether it was lost on the way. */
data class PacketDot(val progress: Float, val lost: Boolean)

/** Where the packets on a link are, for drawing. [phase] runs from 0 to 1 and then starts over. */
object PacketFlow {
    const val COUNT = 4

    /** Healthy links deliver everything; a degraded link loses every other packet; a down link loses them all. */
    fun dots(status: LinkStatus, phase: Float): List<PacketDot> {
        if (status == LinkStatus.NO_LINK) return emptyList()
        return (0 until COUNT).map { i ->
            val p = (phase + i.toFloat() / COUNT) % 1f
            val dropPoint = when (status) {
                LinkStatus.DEGRADED -> if (i % 2 == 1) 0.6f else 1f
                LinkStatus.DOWN -> 0.35f
                else -> 1f
            }
            if (p > dropPoint) PacketDot(dropPoint, lost = true) else PacketDot(p, lost = false)
        }
    }
}
