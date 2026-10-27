package com.avabuilds.packetrunner.network

/**
 * The logic behind the three Lab mini-games at the office. Plain Kotlin, so it can be unit-tested.
 * Bit strings are written as text such as "1011", most significant bit first.
 */

// ---------------------------------------------------------------------------
// CRC: error detection by polynomial division
// ---------------------------------------------------------------------------

object Crc {
    /** x^3 + x^2 + 1, the small generator used in the Lab (the same style of example as in the book). */
    const val GENERATOR = "1101"

    private fun xorAt(bits: CharArray, at: Int, generator: String) {
        generator.forEachIndexed { k, g -> if (g == '1') bits[at + k] = if (bits[at + k] == '1') '0' else '1' }
    }

    /** Divides [bits] by [generator] (division without carries, XOR instead of subtraction) and returns the remainder. */
    fun remainder(bits: String, generator: String = GENERATOR): String {
        val work = bits.toCharArray()
        for (i in 0..work.size - generator.length) if (work[i] == '1') xorAt(work, i, generator)
        return String(work).takeLast(generator.length - 1)
    }

    /** The data with its CRC bits added at the end: what the sender transmits. */
    fun frame(data: String, generator: String = GENERATOR): String =
        data + remainder(data + "0".repeat(generator.length - 1), generator)

    /** The receiver divides the whole frame; a remainder of zero means no error was found. */
    fun isIntact(frame: String, generator: String = GENERATOR): Boolean = remainder(frame, generator).all { it == '0' }

    /** [frame] with the bits at [positions] flipped, to simulate noise on the wire. */
    fun corrupt(frame: String, vararg positions: Int): String {
        val bits = frame.toCharArray()
        positions.forEach { bits[it] = if (bits[it] == '1') '0' else '1' }
        return String(bits)
    }
}

/** A frame in the CRC inbox: what arrived, and whether noise damaged it on the way. */
data class InboxFrame(val frame: String, val damaged: Boolean)

/** The frames of the CRC game's inbox, fixed so every run is the same. Two of five were damaged. */
object CrcInbox {
    val messages = listOf("10110010", "01101100", "11100001", "00111011", "10011101")

    fun frames(): List<InboxFrame> = messages.mapIndexed { i, m ->
        val sent = Crc.frame(m)
        when (i) {
            1 -> InboxFrame(Crc.corrupt(sent, 3), damaged = true) // a flipped data bit
            3 -> InboxFrame(Crc.corrupt(sent, 8, 9), damaged = true) // two flipped bits inside the CRC part
            else -> InboxFrame(sent, damaged = false)
        }
    }
}

/**
 * Long division, one step at a time, for the player to carry out. At each step the bit under the
 * arrow decides: a 1 means "XOR the generator in", a 0 means "skip". [apply] says whether the player was right.
 */
class CrcTrainer(val message: String, val generator: String = Crc.GENERATOR) {
    var register: String = message + "0".repeat(generator.length - 1)
        private set
    var step: Int = 0
        private set

    val done: Boolean get() = step >= message.length

    /** True when the bit under the arrow is 1, so the generator must be XORed in. */
    val mustXor: Boolean get() = !done && register[step] == '1'

    /** The remainder once every step is done; it becomes the CRC bits. */
    val remainder: String get() = register.takeLast(generator.length - 1)

    /** The player chose XOR or skip. Does the step and returns true when the choice was right; a wrong choice changes nothing. */
    fun apply(xor: Boolean): Boolean {
        if (done || xor != mustXor) return false
        if (xor) {
            val bits = register.toCharArray()
            generator.forEachIndexed { k, g -> if (g == '1') bits[step + k] = if (bits[step + k] == '1') '0' else '1' }
            register = String(bits)
        }
        step++
        return true
    }
}

// ---------------------------------------------------------------------------
// Line codes: turning bits into signal levels
// ---------------------------------------------------------------------------

enum class LineCode(val label: String, val rule: String) {
    NRZ("NRZ", "1 is a high signal, 0 is a low signal."),
    NRZI("NRZI", "A 1 makes the signal change level. A 0 keeps the level. The line starts low."),
    MANCHESTER("Manchester", "Every bit has a change in the middle: 0 goes low to high, 1 goes high to low."),
}

object LineCodes {
    /** The signal for [bits]: two levels per bit (first half, second half of the bit), true = high. */
    fun encode(code: LineCode, bits: String): List<Boolean> {
        val out = mutableListOf<Boolean>()
        var level = false // NRZI starts low
        bits.forEach { c ->
            val one = c == '1'
            when (code) {
                LineCode.NRZ -> { out += one; out += one }
                LineCode.NRZI -> { if (one) level = !level; out += level; out += level }
                LineCode.MANCHESTER -> { out += one; out += !one }
            }
        }
        return out
    }

    /** The positions of the half-bits where [drawn] differs from the right signal. */
    fun mistakes(code: LineCode, bits: String, drawn: List<Boolean>): List<Int> {
        val right = encode(code, bits)
        return right.indices.filter { drawn.getOrNull(it) != right[it] }
    }
}

// ---------------------------------------------------------------------------
// Sliding window: keeping the pipe full
// ---------------------------------------------------------------------------

/** One frame of a sliding-window run: when it was sent and when its acknowledgement came back (both in ticks). */
data class FrameTrace(val frame: Int, val sentAt: Int, val ackedAt: Int)

data class WindowRun(val traces: List<FrameTrace>, val window: Int, val rtt: Int) {
    /** The tick at which the last acknowledgement arrives. */
    val finishTick: Int get() = traces.maxOf { it.ackedAt }
}

object SlidingWindow {
    /**
     * Sends [frames] frames, one per tick at most, never more than [window] unacknowledged at a time.
     * An acknowledgement comes back [rtt] ticks after its frame was sent and frees its place in the window.
     */
    fun run(frames: Int, window: Int, rtt: Int): WindowRun {
        require(frames > 0 && window > 0 && rtt > 0)
        val traces = mutableListOf<FrameTrace>()
        var tick = 0
        for (f in 0 until frames) {
            // wait until fewer than [window] frames are still waiting for their acknowledgement
            while (traces.count { it.ackedAt > tick } >= window) tick++
            traces += FrameTrace(f, sentAt = tick, ackedAt = tick + rtt)
            tick++
        }
        return WindowRun(traces, window, rtt)
    }

    /** The fastest possible finish: one frame per tick, and then the last acknowledgement. */
    fun idealFinish(frames: Int, rtt: Int): Int = frames - 1 + rtt

    /** The smallest window that already reaches the ideal finish: the delay-bandwidth product, in frames. */
    fun smallestFullWindow(frames: Int, rtt: Int): Int =
        (1..frames).first { run(frames, it, rtt).finishTick == idealFinish(frames, rtt) }
}

// ---------------------------------------------------------------------------
// The three games, as the menu lists them
// ---------------------------------------------------------------------------

/** The rounds of the line-code game: which code to draw, for which bits. */
data class EncodingRound(val code: LineCode, val bits: String)

object EncodingRounds {
    val all = listOf(
        EncodingRound(LineCode.NRZ, "10110010"),
        EncodingRound(LineCode.NRZI, "11010010"),
        EncodingRound(LineCode.MANCHESTER, "10011010"),
    )
}

/** The rounds of the sliding-window game: how many frames to send, and how long an acknowledgement takes (in ticks). */
data class WindowRound(val frames: Int, val rtt: Int)

object WindowRounds {
    val all = listOf(WindowRound(10, 4), WindowRound(10, 6), WindowRound(12, 8))
}

enum class LabGame(val id: String, val title: String, val blurb: String, val reward: Int) {
    CRC("crc", "Find the corrupted frame", "Divide frames by the generator and catch the ones the wire damaged.", 60),
    ENCODING("encoding", "Draw the signal", "Turn bits into a wave with NRZ, NRZI and Manchester.", 50),
    WINDOW("window", "Fill the pipe", "Choose the window size that keeps the link busy.", 50);

    val concepts: List<Concept>
        get() = when (this) {
            CRC -> listOf(Concepts.CRC)
            ENCODING -> listOf(Concepts.LINE_ENCODING)
            WINDOW -> listOf(Concepts.SLIDING_WINDOW)
        }
}
