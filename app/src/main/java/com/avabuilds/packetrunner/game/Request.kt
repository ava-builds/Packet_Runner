package com.avabuilds.packetrunner.game

/** What the neighbor needs: a new network, a broken one repaired, help with a setting, or a router's forwarding table put right. */
enum class RequestType(val label: String) {
    INSTALL("Install"),
    FIX("Fix"),
    HELP("Help"),
    ROUTE("Route"),
}

/** A phone call from a neighbor. [callMinute] is the time of day when the phone rings. */
data class Request(
    val id: Int,
    val type: RequestType,
    val neighborId: String,
    val buildingId: String,
    val message: String,
    val reward: Int,
    val callMinute: Float,
)
