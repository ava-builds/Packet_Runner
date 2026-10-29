package com.avabuilds.packetrunner.network

import kotlin.math.ceil

/**
 * The result of one edit. When [error] is set the edit was refused and [scene] is unchanged.
 * [cost] is what the edit costs in won (negative when the player gets money back).
 */
data class EditResult(val scene: NetworkScene, val cost: Int = 0, val error: String? = null) {
    val ok: Boolean get() = error == null
}

/** The rules for changing a room: placing and removing devices, laying cables, changing settings. */
object SceneEditor {
    /** The longest Ethernet cable, in cells. */
    const val MAX_CABLE_CELLS = 6f

    fun place(scene: NetworkScene, type: DeviceType, at: CellPos, price: Int, maxCount: Int): EditResult {
        if (!scene.contains(at)) return refused(scene, "That spot is outside the room.")
        if (type.isClient || type == DeviceType.WALL_SOCKET) return refused(scene, "You can't place a ${type.label.lowercase()}.")
        if (scene.deviceAt(at) != null) return refused(scene, "Something is already there.")
        val placed = scene.devices.count { it.type == type && it.placedByPlayer }
        if (placed >= maxCount) return refused(scene, "You have no more ${type.label.lowercase()}s to place.")

        val device = Device(nextId(scene, type), type, at, placedByPlayer = true)
        return EditResult(scene.copy(devices = scene.devices + device), cost = price)
    }

    /** Removes a device the player placed, with its cables. Half the device price and all of the cable money come back. */
    fun remove(scene: NetworkScene, id: String, price: Int, cablePricePerCell: Int): EditResult {
        val device = scene.device(id) ?: return refused(scene, "There is nothing to remove.")
        if (!device.placedByPlayer) return refused(scene, "That was here before you. You can't take it away.")
        val refundedCables = scene.cables.filter { it.connects(id) }.sumOf { cableCost(scene, it, cablePricePerCell) }
        return EditResult(
            scene.copy(devices = scene.devices - device, cables = scene.cables.filterNot { it.connects(id) }.toSet()),
            cost = -(price / 2) - refundedCables,
        )
    }

    /** Lays a cable between two devices, or takes it away again when one is already there. */
    fun toggleCable(scene: NetworkScene, aId: String, bId: String, pricePerCell: Int): EditResult {
        if (aId == bId) return refused(scene, "Pick two different devices.")
        val a = scene.device(aId)
        val b = scene.device(bId)
        if (a == null || b == null) return refused(scene, "There is nothing to connect.")
        val cable = Cable.between(aId, bId)
        if (cable in scene.cables) {
            if (cable in scene.fixedCables) return refused(scene, "That cable is part of the building. You can't take it away.")
            return EditResult(scene.copy(cables = scene.cables - cable), cost = -cableCost(scene, cable, pricePerCell))
        }
        if (!a.type.canCable || !b.type.canCable) {
            val culprit = if (!a.type.canCable) a else b
            return refused(scene, "A ${culprit.type.label.lowercase()} has no cable socket.")
        }
        if (scene.distance(aId, bId) > MAX_CABLE_CELLS) {
            return refused(scene, "Too far! A cable can be at most ${MAX_CABLE_CELLS.toInt()} cells long.")
        }
        return EditResult(scene.copy(cables = scene.cables + cable), cost = cableCost(scene, cable, pricePerCell))
    }

    fun setSettings(scene: NetworkScene, id: String, settings: DeviceSettings): EditResult {
        val device = scene.device(id) ?: return refused(scene, "There is nothing to configure.")
        return EditResult(scene.copy(devices = scene.devices.map { if (it.id == id) device.copy(settings = settings) else it }))
    }

    /** What a cable costs: its length rounded up to whole cells times the price per cell. */
    fun cableCost(scene: NetworkScene, cable: Cable, pricePerCell: Int): Int =
        ceil(scene.distance(cable.a, cable.b)).toInt() * pricePerCell

    private fun refused(scene: NetworkScene, why: String) = EditResult(scene, error = why)

    private fun nextId(scene: NetworkScene, type: DeviceType): String {
        val base = type.name.lowercase()
        var n = 1
        while (scene.device("${base}_$n") != null) n++
        return "${base}_$n"
    }
}
