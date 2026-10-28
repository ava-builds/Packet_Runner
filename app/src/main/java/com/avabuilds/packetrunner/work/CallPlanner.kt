package com.avabuilds.packetrunner.work

import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.game.RequestGenerator
import kotlin.random.Random

/** A call is PENDING until it rings, RANG once it has (answered or not), MISSED while it waits in the voicemail inbox, and EXPIRED once the shift ended without a call back. */
enum class CallStatus { PENDING, RANG, MISSED, EXPIRED }

/** One customer call planned for a work day, and what became of it. */
data class PlannedCall(val request: Request, val status: CallStatus = CallStatus.PENDING) {
    val minute: Int get() = request.callMinute.toInt()
}

/**
 * The calls of one work day. It remembers which day it is for, so a new day needs a new plan, and it is saved with
 * the phone's data so the calls survive closing the app.
 */
data class CallBook(val epochDay: Long, val calls: List<PlannedCall> = emptyList()) {

    /** The calls that have not rung yet, in time order. */
    fun pending(): List<Request> = calls.filter { it.status == CallStatus.PENDING }.map { it.request }.sortedBy { it.callMinute }

    fun markRang(id: Int) = copy(calls = calls.map { if (it.request.id == id && it.status == CallStatus.PENDING) it.copy(status = CallStatus.RANG) else it })

    /** Calls that stayed unrung for more than [CallPlanner.LATE_MINUTES] after their time become missed. */
    fun expire(nowMinute: Int): CallBook = copy(
        calls = calls.map { if (it.status == CallStatus.PENDING && nowMinute - it.minute > CallPlanner.LATE_MINUTES) it.copy(status = CallStatus.MISSED) else it },
    )

    /** The voicemails waiting in the inbox. */
    fun voicemails(): List<Request> = calls.filter { it.status == CallStatus.MISSED }.map { it.request }

    /**
     * Brings the saved statuses in line with the game's inbox [inboxIds]: calls that rang and are now in the inbox become
     * MISSED, and MISSED calls that left the inbox (returned or picked up from a notification) become RANG.
     */
    fun syncVoicemails(inboxIds: Set<Int>) = copy(
        calls = calls.map {
            when {
                it.status == CallStatus.RANG && it.request.id in inboxIds -> it.copy(status = CallStatus.MISSED)
                it.status == CallStatus.MISSED && it.request.id !in inboxIds -> it.copy(status = CallStatus.RANG)
                else -> it
            }
        },
    )

    /** The shift is over: voicemails that were not returned expire. */
    fun expireVoicemails() = copy(calls = calls.map { if (it.status == CallStatus.MISSED) it.copy(status = CallStatus.EXPIRED) else it })

    fun add(call: PlannedCall) = copy(calls = calls + call)

    /** One line per call, tab separated; the first line is the day. Messages never contain tabs or new lines. */
    fun encode(): String = buildString {
        append(epochDay)
        calls.forEach { c ->
            val r = c.request
            append('\n').append(listOf(r.id, c.status.name, r.type.name, r.neighborId, r.buildingId, r.reward, r.callMinute.toInt(), r.message.replace('\t', ' ').replace('\n', ' ')).joinToString("\t"))
        }
    }

    companion object {
        /** Reads what [encode] wrote. Lines that make no sense are skipped; nothing readable gives null. */
        fun decode(text: String?): CallBook? {
            if (text.isNullOrBlank()) return null
            val lines = text.lines()
            val day = lines.first().trim().toLongOrNull() ?: return null
            val calls = lines.drop(1).mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 8) return@mapNotNull null
                val type = com.avabuilds.packetrunner.game.RequestType.entries.firstOrNull { it.name == f[2] } ?: return@mapNotNull null
                val status = CallStatus.entries.firstOrNull { it.name == f[1] } ?: return@mapNotNull null
                PlannedCall(
                    Request(f[0].toIntOrNull() ?: return@mapNotNull null, type, f[3], f[4], f.drop(7).joinToString("\t"), f[5].toIntOrNull() ?: return@mapNotNull null, f[6].toFloatOrNull() ?: return@mapNotNull null),
                    status,
                )
            }
            return CallBook(day, calls)
        }
    }
}

/** Decides when and who calls on a work day. Calls only fall in working time, never at lunch or after work. */
object CallPlanner {
    const val MIN_CALLS = 3
    const val MAX_CALLS = 5

    /** No two calls closer than this, in minutes. */
    const val MIN_GAP = 20

    /** A call that could not ring (you were busy or off duty) is missed after this many minutes. */
    const val LATE_MINUTES = 15

    /** The minutes of the day when a call may ring: not right after clock-in, not at lunch, not near the end. */
    fun allowedMinutes(schedule: WorkSchedule): List<Int> =
        (schedule.startMinute + 10 until schedule.lunchStartMinute - 10).toList() +
            (schedule.lunchEndMinute + 5 until schedule.endMinute - 30).toList()

    /** The calls for [epochDay]: none on a day off, otherwise 3 to 5 (fewer when not enough neighbors live in town). */
    fun plan(
        schedule: WorkSchedule,
        day: WallTime,
        gameDay: Int,
        neighbors: List<Neighbor>,
        connected: Set<String>,
        generator: RequestGenerator,
        /** When the plan is made in the middle of the day, calls only fall from this minute on (not in the past). */
        fromMinute: Int = 0,
    ): CallBook {
        if (!schedule.isWorkday(day.weekday)) return CallBook(day.epochDay)
        val rnd = Random(generator.seed * 31L + day.epochDay)
        val movedIn = neighbors.filter { it.movesInDay <= gameDay }
        // a place without internet calls once (it asks for an install); a connected one may call twice
        val pool = movedIn.flatMap { n -> if (n.id in connected || n.movesInDay > gameDay) listOf(n, n) else listOf(n) }.shuffled(rnd)
        val allowed = allowedMinutes(schedule).filter { it >= fromMinute + 2 }
        val count = minOf(rnd.nextInt(MIN_CALLS, MAX_CALLS + 1), pool.size, allowed.size / MIN_GAP + 1)
        val minutes = pickMinutes(allowed, count, rnd)
        val calls = pool.take(minutes.size).zip(minutes).mapIndexed { i, (neighbor, minute) ->
            val id = (day.epochDay * 10 + i).toInt()
            PlannedCall(generator.requestFor(id, neighbor, connected, gameDay, minute.toFloat(), rnd))
        }
        return CallBook(day.epochDay, calls)
    }

    /** [count] minutes from [allowed], at least [MIN_GAP] apart, in order. */
    fun pickMinutes(allowed: List<Int>, count: Int, rnd: Random): List<Int> {
        if (count <= 0 || allowed.isEmpty()) return emptyList()
        repeat(300) {
            val picked = allowed.shuffled(rnd).take(count).sorted()
            if (picked.zipWithNext().all { (a, b) -> b - a >= MIN_GAP }) return picked
        }
        // very short days: spread them evenly instead
        return (0 until count).map { allowed[(allowed.size - 1) * it / maxOf(count - 1, 1)] }.distinct()
    }
}
