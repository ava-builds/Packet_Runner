package com.avabuilds.packetrunner.game

/**
 * What is stored for one player: the parts of [GameState] that last from day to day.
 * It converts to and from plain maps, which is what Firestore stores, so this file needs no Firebase.
 */
data class SaveData(
    val day: Int,
    val money: Int,
    /** Friendship hearts per neighbor id. */
    val hearts: Map<String, Int>,
    /** The neighbors whose place has a working network: the town as the player built it. */
    val connected: Set<String>,
    val solved: List<SolvedRecord>,
    /** Upgrade levels bought at the shop. */
    val upgrades: Map<String, Int> = emptyMap(),
    /** The seed of this game's calls. */
    val seed: Long = RequestGenerator.DEFAULT_SEED,
    /** What already happened today, so loading the game cannot repeat it for pay. */
    val labDone: Set<String> = emptySet(),
    val rungToday: Set<Int> = emptySet(),
    val caught: Set<Int> = emptySet(),
) {
    /** Makes a loaded save safe: numbers in range, only neighbors that exist, only known job types. */
    fun sanitized(neighbors: List<Neighbor>): SaveData {
        val ids = neighbors.map { it.id }.toSet()
        return SaveData(
            day = day.coerceAtLeast(1),
            money = money.coerceAtLeast(0),
            hearts = hearts.filterKeys { it in ids }.mapValues { it.value.coerceIn(0, MAX_HEARTS) },
            connected = connected.filter { it in ids }.toSet(),
            solved = solved,
            upgrades = Upgrades.sanitized(upgrades),
            seed = seed,
            labDone = labDone,
            rungToday = rungToday,
            caught = caught,
        )
    }

    fun toMap(): Map<String, Any> = mapOf(
        "day" to day,
        "money" to money,
        "hearts" to hearts,
        "connected" to connected.sorted(),
        "upgrades" to upgrades,
        "seed" to seed,
        "labDone" to labDone.sorted(),
        "rungToday" to rungToday.sorted(),
        "caught" to caught.sorted(),
        "solved" to solved.map { mapOf("day" to it.day, "type" to it.type.name, "buildingId" to it.buildingId, "payout" to it.payout) },
    )

    companion object {
        /** The state to keep. A finished day is saved as the start of the next one. */
        fun from(state: GameState) = SaveData(
            day = if (state.dayOver) state.day + 1 else state.day,
            money = state.money,
            hearts = state.hearts,
            connected = state.connected,
            solved = state.solved,
            upgrades = state.upgrades,
            seed = state.seed,
            // a finished day is saved as the next morning, with nothing done yet
            labDone = if (state.dayOver) emptySet() else state.labDone,
            rungToday = if (state.dayOver) emptySet() else state.rungToday,
            caught = if (state.dayOver) emptySet() else state.caught,
        )

        /** Reads a stored map. Missing or odd values fall back to a fresh start instead of crashing. */
        fun fromMap(map: Map<String, Any?>): SaveData {
            fun int(v: Any?, default: Int) = (v as? Number)?.toInt() ?: default
            val hearts = (map["hearts"] as? Map<*, *>).orEmpty()
                .mapNotNull { (k, v) -> (k as? String)?.let { it to int(v, 0) } }.toMap()
            val connected = (map["connected"] as? List<*>).orEmpty().filterIsInstance<String>().toSet()
            val solved = (map["solved"] as? List<*>).orEmpty().mapNotNull { row ->
                val r = row as? Map<*, *> ?: return@mapNotNull null
                val type = RequestType.entries.firstOrNull { it.name == r["type"] } ?: return@mapNotNull null
                SolvedRecord(int(r["day"], 1), type, r["buildingId"] as? String ?: return@mapNotNull null, int(r["payout"], 0))
            }
            val upgrades = (map["upgrades"] as? Map<*, *>).orEmpty().mapNotNull { (k, v) -> (k as? String)?.let { it to int(v, 0) } }.toMap()
            val seed = (map["seed"] as? Number)?.toLong() ?: RequestGenerator.DEFAULT_SEED
            val labDone = (map["labDone"] as? List<*>).orEmpty().filterIsInstance<String>().toSet()
            val rung = (map["rungToday"] as? List<*>).orEmpty().filterIsInstance<Number>().map { it.toInt() }.toSet()
            val caught = (map["caught"] as? List<*>).orEmpty().filterIsInstance<Number>().map { it.toInt() }.toSet()
            return SaveData(int(map["day"], 1), int(map["money"], START_MONEY), hearts, connected, solved, upgrades, seed, labDone, rung, caught)
        }
    }
}
