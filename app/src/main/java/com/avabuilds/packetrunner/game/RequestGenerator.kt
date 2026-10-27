package com.avabuilds.packetrunner.game

import kotlin.random.Random

/**
 * Plans the phone calls of each day. It is seeded, so the same seed and the same town
 * always give the same calls (easy to test, and a saved game can replay a day).
 *
 * Rules:
 *  - only neighbors who have already moved in call;
 *  - a neighbor calls at most once a day, and new neighbors always call on their first day;
 *  - a place without internet asks for an INSTALL, a place with internet asks for a FIX or HELP
 *    (day 1 is scripted: installs for new homes, a fix for the others);
 *  - the number of calls grows with the day: 2, 2, 3, 3, 4 (never more than 4);
 *  - calls are spread between 09:00 and 16:00.
 */
class RequestGenerator(private val seed: Long) {

    fun callsFor(day: Int, neighbors: List<Neighbor>, connected: Set<String>): List<Request> {
        val rnd = Random(seed * 1_000_003L + day)
        val movedIn = neighbors.filter { it.movesInDay <= day }
        if (movedIn.isEmpty()) return emptyList()

        val count = minOf(movedIn.size, (2 + (day - 1) / 2).coerceAtMost(MAX_CALLS))
        val newcomers = movedIn.filter { it.movesInDay == day }.shuffled(rnd)
        val others = (movedIn - newcomers.toSet()).shuffled(rnd)
        val callers = (newcomers + others).take(count)

        val window = Clock.CALLS_UNTIL - Clock.CALLS_FROM
        val step = window / count
        return callers.mapIndexed { i, neighbor ->
            val type = when {
                neighbor.id !in connected -> RequestType.INSTALL
                day == 1 -> RequestType.FIX
                rnd.nextInt(10) < 7 -> RequestType.FIX
                else -> RequestType.HELP
            }
            val minute = Clock.CALLS_FROM + i * step + rnd.nextInt(step / 2)
            Request(
                id = day * 100 + i,
                type = type,
                neighborId = neighbor.id,
                buildingId = neighbor.buildingId,
                message = messageFor(type, neighbor, rnd),
                reward = REWARDS.getValue(type) + 10 * (day - 1),
                callMinute = minute.toFloat(),
            )
        }
    }

    private fun messageFor(type: RequestType, neighbor: Neighbor, rnd: Random): String {
        val isShop = neighbor.kind == BuildingType.CAFE
        val options = when (type) {
            RequestType.INSTALL -> listOf(
                "Hi, it's ${neighbor.name}! We just moved in. Can you set up our internet?",
                "Hello! ${neighbor.name} here. We have no internet yet. Could you install a router for us?",
            )
            RequestType.FIX ->
                if (isShop) listOf("The cafe Wi-Fi keeps dropping by the window!", "Customers by the window can't stay connected. Please help!")
                else listOf("The Wi-Fi in our back room is so slow. Can you take a look?", "Our internet keeps cutting out. Can you fix it?")
            RequestType.HELP ->
                if (isShop) listOf("Customers say they can't get an IP address on our Wi-Fi!")
                else listOf("My new phone can't get internet!", "Our guests can't connect. It says there is no IP address.")
        }
        return options[rnd.nextInt(options.size)]
    }

    companion object {
        const val MAX_CALLS = 4
        val REWARDS = mapOf(RequestType.INSTALL to 200, RequestType.FIX to 150, RequestType.HELP to 100)
    }
}
