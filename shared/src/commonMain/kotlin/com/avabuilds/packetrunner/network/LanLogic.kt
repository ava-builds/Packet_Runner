package com.avabuilds.packetrunner.network

import kotlin.math.min
import kotlin.random.Random

/** Ethernet's answer to collisions on a shared cable: listen while sending (CSMA/CD), then back off for a random time. */
object Csma {
    const val MAX_ATTEMPTS = 16

    /** After the n-th collision a sender waits 0 to 2^n - 1 slot times (the range stops growing after 10 collisions). */
    fun backoffChoices(attempt: Int): Int = 1 shl min(attempt, 10)
}

/** A hub repeats every frame out of every other port: it is one shared cable. */
object Hub {
    /** The computers that receive a frame sent from [src] (everyone else), and the ones it was not meant for. */
    fun deliver(src: Int, dst: Int, computers: Int): HubDelivery {
        val receivers = (1..computers).filter { it != src }.toSet()
        return HubDelivery(receivers, receivers - dst)
    }
}

data class HubDelivery(val receivers: Set<Int>, val wasted: Set<Int>)

/** What one collision looked like: the waiting times both senders drew in each attempt, until they differed. */
data class CollisionTrace(val rounds: List<Pair<Int, Int>>) {
    val attempts: Int get() = rounds.size
}

sealed interface LanEvent {
    data class HubSent(val src: Int, val dst: Int, val delivery: HubDelivery) : LanEvent
    data class HubCollision(val a: Int, val b: Int, val trace: CollisionTrace) : LanEvent
    data object SwitchInstalled : LanEvent
    data class SwitchSent(val src: Int, val dst: Int, val action: BridgeAction) : LanEvent
}

/** The steps of the hub-to-switch lesson. Each is done when a matching event has happened. */
enum class LanMission(val text: String) {
    HUB_SEND("On the hub, send a frame and see who hears it."),
    HUB_COLLIDE("Make two computers send at the same time."),
    INSTALL("Replace the hub with a switch."),
    SWITCH_FLOOD("Send to a computer the switch has not heard from yet."),
    SWITCH_FORWARD("Send to a computer the switch already knows.");
}

/**
 * The office LAN: five computers on one device, first a hub, then a switch with a learning bridge.
 * The player sends frames; the session works out who receives them and what the switch learns.
 */
class LanSession(val computers: Int = 5, seed: Long = 7L) {
    private val rnd = Random(seed)
    private var bridge: LearningBridge? = null
    private val log = mutableListOf<LanEvent>()

    val events: List<LanEvent> get() = log.toList()
    val usesSwitch: Boolean get() = bridge != null

    /** What the switch has learned so far: computer name -> port. */
    val switchTable: Map<String, Int> get() = bridge?.learned.orEmpty()

    fun name(computer: Int): String = ('A' + (computer - 1)).toString()

    fun hubSend(src: Int, dst: Int): HubDelivery {
        check(!usesSwitch) { "The hub is gone" }
        require(src != dst)
        return Hub.deliver(src, dst, computers).also { log += LanEvent.HubSent(src, dst, it) }
    }

    /** Two computers talk at once on the hub. They both notice the clash, and each backs off for a random number of slots. */
    fun hubCollide(a: Int, b: Int): CollisionTrace {
        check(!usesSwitch) { "The hub is gone" }
        val rounds = mutableListOf<Pair<Int, Int>>()
        do {
            val choices = Csma.backoffChoices(rounds.size + 1)
            rounds += rnd.nextInt(choices) to rnd.nextInt(choices)
        } while (rounds.last().first == rounds.last().second && rounds.size < Csma.MAX_ATTEMPTS)
        return CollisionTrace(rounds).also { log += LanEvent.HubCollision(a, b, it) }
    }

    fun installSwitch() {
        if (usesSwitch) return
        bridge = LearningBridge(computers)
        log += LanEvent.SwitchInstalled
    }

    /** The switch learns [src] on its port and then forwards, floods or filters. */
    fun switchSend(src: Int, dst: Int): BridgeAction {
        val b = checkNotNull(bridge) { "There is no switch yet" }
        require(src != dst)
        return b.receive(name(src), name(dst), src).also { log += LanEvent.SwitchSent(src, dst, it) }
    }

    fun isDone(mission: LanMission): Boolean = when (mission) {
        LanMission.HUB_SEND -> log.any { it is LanEvent.HubSent }
        LanMission.HUB_COLLIDE -> log.any { it is LanEvent.HubCollision }
        LanMission.INSTALL -> log.any { it is LanEvent.SwitchInstalled }
        LanMission.SWITCH_FLOOD -> log.any { it is LanEvent.SwitchSent && it.action is BridgeAction.Flood }
        LanMission.SWITCH_FORWARD -> log.any { it is LanEvent.SwitchSent && it.action is BridgeAction.Forward }
    }

    val allDone: Boolean get() = LanMission.entries.all { isDone(it) }
}
