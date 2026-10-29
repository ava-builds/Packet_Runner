package com.avabuilds.packetrunner.network

/** One scan view: a small table the player reads to find the problem (a cache, packets on the wire, a trace). */
data class Scan(val name: String, val note: String, val columns: List<String>, val rows: List<List<String>>)

/** Something the player can do about the problem. A wrong one costs [cost] won (parts, time) and teaches through [consequence]. */
data class CaseAction(val label: String, val correct: Boolean, val consequence: String, val cost: Int = 0)

/**
 * A job that is solved by looking, not building: the neighbor's story and symptoms, scan views that show what is
 * really happening, a list of actions (one of them right) and the concept cards that explain it.
 * [afterFix] is the scan shown once the right action is taken, so the player sees the healthy network.
 */
data class CaseFile(
    val id: String,
    val title: String,
    val story: String,
    val symptoms: List<String>,
    val scans: List<Scan>,
    val actions: List<CaseAction>,
    val afterFix: Scan,
    val hints: List<String>,
    val concepts: List<Concept>,
) {
    init {
        require(actions.count { it.correct } == 1) { "case $id needs exactly one right action" }
    }
}

/** What happened when the player picked an action. */
data class ActionResult(val action: CaseAction, val solved: Boolean)

/** One attempt at a [CaseFile]: which scans were opened, which wrong actions were tried, and what the job pays. */
class CaseSession(val case: CaseFile) {
    private val opened = mutableSetOf<Int>()
    private val wrongTries = mutableListOf<CaseAction>()
    var solved: Boolean = false
        private set

    val wrongActions: List<CaseAction> get() = wrongTries.toList()
    val scansOpened: Int get() = opened.size

    fun openScan(index: Int) {
        if (index in case.scans.indices) opened += index
    }

    fun choose(index: Int): ActionResult? {
        val action = case.actions.getOrNull(index) ?: return null
        if (solved) return ActionResult(action, solved = true)
        if (action.correct) solved = true else wrongTries += action
        return ActionResult(action, action.correct)
    }

    /** What the wrong tries cost so far. */
    val spent: Int get() = wrongTries.sumOf { it.cost }

    /** 3 stars for no wrong try, 2 for one, 1 for more. */
    fun stars(): Int = when (wrongTries.size) {
        0 -> 3
        1 -> 2
        else -> 1
    }

    fun tip(): Int = (stars() - 1) * 10

    /** The reward plus the tip, minus what the wrong tries cost; never below zero. */
    fun payout(reward: Int): Int = (reward + tip() - spent).coerceAtLeast(0)
}

object CaseFiles {
    val all: List<CaseFile> get() = ArpCases.all + FragmentCases.all + IcmpCases.all + WifiCases.all + TowerCases.all + MobileIpCases.all + TransportCases.all

    /** Each case job is a different case, so the same one does not come twice in a row. */
    fun forJob(jobsDoneBefore: Int): CaseFile = all[jobsDoneBefore % all.size]
}
