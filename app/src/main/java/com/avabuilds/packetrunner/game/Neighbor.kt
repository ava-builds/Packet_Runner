package com.avabuilds.packetrunner.game

/**
 * A person who lives or works in the town and phones the player for help.
 * They live in town from level [unlockLevel] on (see [Curriculum]) and only call then. [hasInternetAtStart] is true when their place
 * already has a network (so they ask for fixes, not an installation).
 */
data class Neighbor(
    val id: String,
    val name: String,
    val buildingId: String,
    val kind: BuildingType,
    val unlockLevel: Int,
    val hasInternetAtStart: Boolean,
) {
    fun hasMovedIn(level: Int): Boolean = level >= unlockLevel
}

object Neighbors {
    /** The neighbors of the town. New ones move in as the player reaches the level of their building. */
    fun default() = listOf(
        Neighbor("mina", "Mina", "house1", BuildingType.HOUSE, unlockLevel = 1, hasInternetAtStart = false),
        Neighbor("hana", "Hana", "cafe", BuildingType.CAFE, unlockLevel = 2, hasInternetAtStart = true),
        Neighbor("joon", "Joon", "house2", BuildingType.HOUSE, unlockLevel = 1, hasInternetAtStart = false),
        Neighbor("sora", "Sora", "house3", BuildingType.HOUSE, unlockLevel = 1, hasInternetAtStart = false),
        Neighbor("dae", "Dae", "cityhall", BuildingType.CITY_HALL, unlockLevel = 4, hasInternetAtStart = true),
        Neighbor("yuna", "Yuna", "tower", BuildingType.TOWER, unlockLevel = 3, hasInternetAtStart = true),
        Neighbor("rin", "Rin", "apartments", BuildingType.APARTMENTS, unlockLevel = 5, hasInternetAtStart = true),
    )
}
