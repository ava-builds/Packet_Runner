package com.avabuilds.packetrunner.game

/**
 * A person who lives or works in the town and phones the player for help.
 * They only start calling from day [movesInDay] on. [hasInternetAtStart] is true when their place
 * already has a network (so they ask for fixes, not an installation).
 */
data class Neighbor(
    val id: String,
    val name: String,
    val buildingId: String,
    val kind: BuildingType,
    val movesInDay: Int,
    val hasInternetAtStart: Boolean,
)

object Neighbors {
    /** The neighbors of the starter town. New ones move in as the days pass. */
    fun default() = listOf(
        Neighbor("mina", "Mina", "house1", BuildingType.HOUSE, movesInDay = 1, hasInternetAtStart = false),
        Neighbor("hana", "Hana", "cafe", BuildingType.CAFE, movesInDay = 1, hasInternetAtStart = true),
        Neighbor("joon", "Joon", "house2", BuildingType.HOUSE, movesInDay = 2, hasInternetAtStart = false),
        Neighbor("sora", "Sora", "house3", BuildingType.HOUSE, movesInDay = 3, hasInternetAtStart = false),
        Neighbor("dae", "Dae", "cityhall", BuildingType.CITY_HALL, movesInDay = 2, hasInternetAtStart = true),
        Neighbor("rin", "Rin", "apartments", BuildingType.APARTMENTS, movesInDay = 3, hasInternetAtStart = true),
    )
}
