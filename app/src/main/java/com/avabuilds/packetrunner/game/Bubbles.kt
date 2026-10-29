package com.avabuilds.packetrunner.game

/** How a neighbor looks at the moment: worried when the network is broken, otherwise more cheerful the more they like you. */
enum class Mood {
    WORRIED, NEUTRAL, HAPPY, DELIGHTED;

    companion object {
        fun of(hearts: Int, hasProblem: Boolean): Mood = when {
            hasProblem -> WORRIED
            hearts >= 4 -> DELIGHTED
            hearts >= 2 -> HAPPY
            else -> NEUTRAL
        }
    }
}

/** What stands out above a building: nothing, a job you have taken on, or a network problem waiting for you. */
enum class Alert { NONE, JOB, PROBLEM }

/**
 * Everything the little bubbles above one building need: who lives there, how they feel, whether their phone is
 * ringing right now, and whether there is something that needs attention.
 */
data class BuildingBubble(
    val buildingId: String,
    val hearts: Int,
    val mood: Mood,
    val ringing: Boolean,
    val alert: Alert,
)

/** Works out the bubbles for the town from the game state. Only neighbors who have already moved in get one. */
object Bubbles {
    fun of(state: GameState, neighbors: List<Neighbor>): List<BuildingBubble> =
        neighbors.filter { it.hasMovedIn(Curriculum.levelOf(state.xp)) }.map { n ->
            val problem = NetworkMonitor.status(state, n) == LinkStatus.DOWN
            val job = state.active?.neighborId == n.id
            val hearts = state.hearts[n.id] ?: 0
            BuildingBubble(
                buildingId = n.buildingId,
                hearts = hearts,
                mood = Mood.of(hearts, problem),
                ringing = state.ringing?.neighborId == n.id,
                alert = when {
                    problem -> Alert.PROBLEM
                    job -> Alert.JOB
                    else -> Alert.NONE
                },
            )
        }
}
