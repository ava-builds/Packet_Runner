package com.avabuilds.packetrunner.game

const val MAX_HEARTS = 5
const val START_MONEY = 500
const val FAIL_PENALTY = 50

/** How long the phone rings (in game minutes) before the call is missed. */
const val RING_TIMEOUT_MINUTES = 120f

/**
 * Everything that changes while you play. It is immutable: [GameEngine] returns a new state
 * for every event, which keeps the rules easy to test and easy to save later.
 */
data class GameState(
    val day: Int,
    val minute: Float,
    val money: Int,
    /** Friendship hearts (0 to [MAX_HEARTS]) per neighbor id. */
    val hearts: Map<String, Int>,
    /** Ids of the neighbors whose place has a working network. */
    val connected: Set<String>,
    /** Today's calls that have not rung yet, earliest first. */
    val upcoming: List<Request>,
    val ringing: Request? = null,
    val ringingSince: Float = 0f,
    /** The request the player accepted and is working on. */
    val active: Request? = null,
    val todayEarned: Int = 0,
    val todaySolved: Int = 0,
    val todayMissed: Int = 0,
    val todayFailed: Int = 0,
    /** Ids of the calls whose problem the player already reported on the Network Monitor. */
    val caught: Set<Int> = emptySet(),
    val todayCaught: Int = 0,
    val todayFalseAlarms: Int = 0,
    val todayBonus: Int = 0,
) {
    val dayOver: Boolean get() = minute >= Clock.DAY_END
}

object GameEngine {

    fun newGame(neighbors: List<Neighbor>, generator: RequestGenerator): GameState {
        val connected = neighbors.filter { it.hasInternetAtStart }.map { it.id }.toSet()
        return GameState(
            day = 1,
            minute = Clock.DAY_START,
            money = START_MONEY,
            hearts = neighbors.associate { it.id to 0 },
            connected = connected,
            upcoming = generator.callsFor(1, neighbors, connected),
        )
    }

    /** Moves time forward. Rings the phone when a call is due and ends the day at 20:00. */
    fun advance(state: GameState, seconds: Float): GameState {
        if (state.dayOver) return state
        var s = state.copy(minute = (state.minute + seconds * Clock.MINUTES_PER_SECOND).coerceAtMost(Clock.DAY_END))

        // The phone only rings when the player is free
        if (s.ringing == null && s.active == null) {
            val next = s.upcoming.firstOrNull()
            if (next != null && next.callMinute <= s.minute) {
                s = s.copy(ringing = next, ringingSince = s.minute, upcoming = s.upcoming.drop(1))
            }
        }
        if (s.ringing != null && s.minute - s.ringingSince >= RING_TIMEOUT_MINUTES) s = ignore(s)

        // At 20:00 a ringing phone is missed and unfinished work counts as failed
        if (s.dayOver) {
            if (s.ringing != null) s = ignore(s)
            if (s.active != null) s = complete(s, solved = false)
        }
        return s
    }

    /** The player picks up the phone. */
    fun answer(state: GameState): GameState {
        val call = state.ringing ?: return state
        return state.copy(ringing = null, active = call)
    }

    /** The player lets it ring, or it rang too long: the neighbor is a little unhappy. */
    fun ignore(state: GameState): GameState {
        val call = state.ringing ?: return state
        return changeHearts(state, call.neighborId, -1).copy(ringing = null, todayMissed = state.todayMissed + 1)
    }

    /**
     * The player finished the request. A solved one pays and adds a heart; a failed one costs.
     * [payout] is what the customer really pays: the reward minus the materials the player used.
     * It defaults to the full reward. [bonusHearts] are extra hearts for great work.
     */
    fun complete(state: GameState, solved: Boolean, payout: Int? = null, bonusHearts: Int = 0): GameState {
        val job = state.active ?: return state
        return if (solved) {
            val pay = (payout ?: job.reward).coerceAtLeast(0)
            changeHearts(state, job.neighborId, 1 + bonusHearts).copy(
                active = null,
                money = state.money + pay,
                connected = if (job.type == RequestType.INSTALL) state.connected + job.neighborId else state.connected,
                todayEarned = state.todayEarned + pay,
                todaySolved = state.todaySolved + 1,
            )
        } else {
            changeHearts(state, job.neighborId, -1).copy(
                active = null,
                money = (state.money - FAIL_PENALTY).coerceAtLeast(0),
                todayFailed = state.todayFailed + 1,
            )
        }
    }

    /** Developer shortcut: jumps to 20:00 so the end of the day (and the next day) can be tried quickly. */
    fun skipToEvening(state: GameState): GameState =
        advance(state, (Clock.DAY_END - state.minute) / Clock.MINUTES_PER_SECOND + 1f)

    /** Starts the next morning at 08:00 with new calls. Money, hearts and networks carry over. */
    fun nextDay(state: GameState, neighbors: List<Neighbor>, generator: RequestGenerator): GameState {
        val day = state.day + 1
        return state.copy(
            day = day,
            minute = Clock.DAY_START,
            upcoming = generator.callsFor(day, neighbors, state.connected),
            ringing = null,
            active = null,
            todayEarned = 0,
            todaySolved = 0,
            todayMissed = 0,
            todayFailed = 0,
            caught = emptySet(),
            todayCaught = 0,
            todayFalseAlarms = 0,
            todayBonus = 0,
        )
    }

    private fun changeHearts(state: GameState, neighborId: String, delta: Int): GameState {
        val now = state.hearts[neighborId] ?: 0
        return state.copy(hearts = state.hearts + (neighborId to (now + delta).coerceIn(0, MAX_HEARTS)))
    }
}
