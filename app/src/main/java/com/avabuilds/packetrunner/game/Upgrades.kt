package com.avabuilds.packetrunner.game

/** One thing the player can buy at the Upgrade shop, in levels. [prices] has one price for each level. */
data class Upgrade(val id: String, val title: String, val description: String, val prices: List<Int>) {
    val maxLevel: Int get() = prices.size
}

/** The shop's goods and what each level does. Pure rules, so they are easy to test. */
object Upgrades {
    const val SCOOTER = "scooter"
    const val CABLE_SPOOL = "cable_spool"

    val all = listOf(
        Upgrade(SCOOTER, "Faster scooter", "The engineer walks faster between buildings.", listOf(250, 500)),
        Upgrade(CABLE_SPOOL, "Bulk cable spool", "Ethernet cable costs less per cell in every room.", listOf(200, 400)),
    )

    fun byId(id: String): Upgrade? = all.firstOrNull { it.id == id }

    /** Tiles per second the engineer walks at, for a scooter [level] (0 = on foot). */
    fun walkSpeed(level: Int): Float = 4f + 1.5f * level.coerceIn(0, 2)

    /** The price of one cell of cable after the spool upgrade: 5, 4, then 3 for the standard price of 5. */
    fun cablePrice(basePrice: Int, level: Int): Int = (basePrice - level.coerceIn(0, 2)).coerceAtLeast(1)

    /** Levels the player owns, cleaned: only known upgrades, never above the top level or below zero. */
    fun sanitized(levels: Map<String, Int>): Map<String, Int> =
        levels.mapNotNull { (id, level) -> byId(id)?.let { id to level.coerceIn(0, it.maxLevel) } }.filter { it.second > 0 }.toMap()
}
