package com.avabuilds.packetrunner.network

import kotlin.math.hypot

/** A cell of the room grid. x grows to the right, y grows downward. */
data class CellPos(val x: Int, val y: Int)

/**
 * Everything that can stand in a room. [wifiRange] is how far its Wi-Fi reaches, in cells
 * (0 = no Wi-Fi). [canCable] says whether an Ethernet cable can be plugged in.
 */
enum class DeviceType(val label: String, val isClient: Boolean, val wifiRange: Float, val canCable: Boolean) {
    WALL_SOCKET("Wall socket", isClient = false, wifiRange = 0f, canCable = true),
    ROUTER("Router", isClient = false, wifiRange = 4f, canCable = true),
    ACCESS_POINT("Access point", isClient = false, wifiRange = 4f, canCable = true),
    REPEATER("Repeater", isClient = false, wifiRange = 3f, canCable = false),
    SWITCH("Switch", isClient = false, wifiRange = 0f, canCable = true),
    HUB("Hub", isClient = false, wifiRange = 0f, canCable = true),
    LAPTOP("Laptop", isClient = true, wifiRange = 0f, canCable = false),
    PHONE("Phone", isClient = true, wifiRange = 0f, canCable = false),
    TV("TV", isClient = true, wifiRange = 0f, canCable = false),
    POS("Card reader", isClient = true, wifiRange = 0f, canCable = false);

    val isWifiSource: Boolean get() = wifiRange > 0f
}

/** The settings of a device. Not every device uses all of them. */
data class DeviceSettings(
    /** Wi-Fi sources: ask permission before sending (RTS/CTS), which avoids hidden-node collisions. */
    val rtsCts: Boolean = false,
    /** Router: how many IP addresses the DHCP server can hand out. */
    val dhcpPoolSize: Int = 10,
    /** Router: how long a device keeps its IP address after it stops talking. */
    val leaseMinutes: Int = 120,
)

data class Device(
    val id: String,
    val type: DeviceType,
    val at: CellPos,
    val settings: DeviceSettings = DeviceSettings(),
    /** True for things the player put there; only those can be removed. */
    val placedByPlayer: Boolean = false,
    /** A name for this particular device, for example "Window laptop". */
    val name: String? = null,
) {
    val displayName: String get() = name ?: type.label
}

/** An Ethernet cable between two devices. Equal no matter which end is named first. */
class Cable private constructor(val a: String, val b: String) {
    fun connects(id: String) = id == a || id == b
    fun other(id: String) = if (id == a) b else a

    override fun equals(other: Any?) = other is Cable && other.a == a && other.b == b
    override fun hashCode() = 31 * a.hashCode() + b.hashCode()
    override fun toString() = "Cable($a, $b)"

    companion object {
        fun between(x: String, y: String) = if (x <= y) Cable(x, y) else Cable(y, x)
    }
}

/** A room with devices and cables. Immutable: every edit makes a new scene. */
data class NetworkScene(
    val width: Int,
    val height: Int,
    val devices: List<Device>,
    val cables: Set<Cable> = emptySet(),
    /** Cables that are part of the building: the player cannot take them away. */
    val fixedCables: Set<Cable> = emptySet(),
    /** Devices that left but still hold a DHCP lease: minutes since each was last seen. */
    val staleLeaseAges: List<Int> = emptyList(),
) {
    fun device(id: String): Device? = devices.firstOrNull { it.id == id }
    fun deviceAt(cell: CellPos): Device? = devices.firstOrNull { it.at == cell }
    fun contains(cell: CellPos) = cell.x in 0 until width && cell.y in 0 until height

    /** Distance between two devices in cells. */
    fun distance(aId: String, bId: String): Float {
        val a = device(aId)!!.at
        val b = device(bId)!!.at
        return hypot((a.x - b.x).toFloat(), (a.y - b.y).toFloat())
    }

    /** Devices plugged into [id] by a cable. */
    fun cabledNeighbors(id: String): List<String> = cables.filter { it.connects(id) }.map { it.other(id) }
}
