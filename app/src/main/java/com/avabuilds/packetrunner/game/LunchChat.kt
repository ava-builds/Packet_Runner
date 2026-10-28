package com.avabuilds.packetrunner.game

/** What the neighbor says over lunch, line by line. The player taps to hear the next one. */
object LunchChat {
    private val shop = listOf(
        "Hi! The usual? Today's special is bibimbap.",
        "Business is better since the Wi-Fi works. Customers stay for a second coffee!",
        "Did you know the card reader needs under 35 ms? Nobody likes waiting at the till.",
        "Thank you for taking care of the network. It is easy to forget until it breaks!",
    )
    private val other = listOf(
        "Hello! Mind if I sit with you?",
        "The internet at my place has been great lately.",
        "You work so hard. Enjoy your break!",
    )

    /** The lines for [neighbor]. The cafe owner has more to say. */
    fun linesFor(neighbor: Neighbor): List<String> = if (neighbor.kind == BuildingType.CAFE) shop else other

    /** The neighbor to have lunch with: the owner of the cafe. */
    fun partner(neighbors: List<Neighbor>): Neighbor? = neighbors.firstOrNull { it.kind == BuildingType.CAFE }
}
