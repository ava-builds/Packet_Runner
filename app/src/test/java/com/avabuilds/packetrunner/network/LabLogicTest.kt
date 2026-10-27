package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabLogicTest {

    // ---- CRC ----

    @Test fun theRemainderOfAKnownDivisionIsRight() {
        // 10110010 000 divided by 1101, step by step on paper: XOR at bits 0, 1, 4, 5 and 7 leaves 001
        assertEquals("001", Crc.remainder("10110010000"))
    }

    @Test fun aFrameBuiltBySenderPassesTheReceiversCheck() {
        listOf("10110010", "00000000", "11111111", "01010101", "10000001").forEach {
            val frame = Crc.frame(it)
            assertEquals(it.length + 3, frame.length)
            assertTrue(it, Crc.isIntact(frame))
        }
    }

    @Test fun everySingleBitErrorIsCaught() {
        val frame = Crc.frame("10110010")
        frame.indices.forEach { assertFalse("bit $it", Crc.isIntact(Crc.corrupt(frame, it))) }
    }

    @Test fun everyShortBurstOfThreeBitsIsCaught() {
        val frame = Crc.frame("11001010")
        for (start in 0..frame.length - 3) {
            assertFalse("burst at $start", Crc.isIntact(Crc.corrupt(frame, start, start + 1, start + 2)))
        }
    }

    @Test fun theTrainerFollowsTheSameDivisionAsTheFormula() {
        val trainer = CrcTrainer("10110010")
        while (!trainer.done) assertTrue(trainer.apply(trainer.mustXor))
        assertEquals(Crc.remainder("10110010000"), trainer.remainder)
    }

    @Test fun aWrongChoiceChangesNothing() {
        val trainer = CrcTrainer("10110010")
        val before = trainer.register
        assertFalse(trainer.apply(!trainer.mustXor))
        assertEquals(before, trainer.register)
        assertEquals(0, trainer.step)
    }

    @Test fun afterTheLastStepNothingMoreHappens() {
        val trainer = CrcTrainer("1011")
        while (!trainer.done) trainer.apply(trainer.mustXor)
        assertFalse(trainer.apply(true))
        assertFalse(trainer.mustXor)
    }

    // ---- line codes ----

    @Test fun nrzIsHighForOneAndLowForZero() {
        assertEquals(listOf(true, true, false, false, true, true), LineCodes.encode(LineCode.NRZ, "101"))
    }

    @Test fun nrziChangesLevelOnEveryOneAndKeepsItOnZero() {
        // 1 -> high, 1 -> low, 0 -> low, 1 -> high
        assertEquals(
            listOf(true, true, false, false, false, false, true, true),
            LineCodes.encode(LineCode.NRZI, "1101"),
        )
    }

    @Test fun nrziKeepsTheLineLowForARunOfZeros() {
        assertTrue(LineCodes.encode(LineCode.NRZI, "0000").none { it })
    }

    @Test fun manchesterAlwaysChangesInTheMiddleOfABit() {
        val wave = LineCodes.encode(LineCode.MANCHESTER, "1001110")
        for (i in wave.indices step 2) assertTrue(wave[i] != wave[i + 1])
    }

    @Test fun manchesterZeroGoesLowToHighAndOneGoesHighToLow() {
        assertEquals(listOf(false, true, true, false), LineCodes.encode(LineCode.MANCHESTER, "01"))
    }

    @Test fun everyBitHasTwoLevels() {
        LineCode.entries.forEach { assertEquals(16, LineCodes.encode(it, "10110010").size) }
    }

    @Test fun mistakesPointAtTheWrongHalfBits() {
        val right = LineCodes.encode(LineCode.NRZ, "10")
        val drawn = right.toMutableList().also { it[1] = !it[1]; it[2] = !it[2] }
        assertEquals(listOf(1, 2), LineCodes.mistakes(LineCode.NRZ, "10", drawn))
        assertTrue(LineCodes.mistakes(LineCode.NRZ, "10", right).isEmpty())
        assertEquals(listOf(0, 1, 2, 3), LineCodes.mistakes(LineCode.NRZ, "10", emptyList()))
    }

    // ---- sliding window ----

    @Test fun stopAndWaitSendsOneFrameThenWaitsForItsAck() {
        val run = SlidingWindow.run(frames = 3, window = 1, rtt = 4)
        assertEquals(listOf(0, 4, 8), run.traces.map { it.sentAt })
        assertEquals(12, run.finishTick)
    }

    @Test fun aWindowAsBigAsTheRttKeepsTheLinkBusyEveryTick() {
        val run = SlidingWindow.run(frames = 10, window = 6, rtt = 6)
        assertEquals((0 until 10).toList(), run.traces.map { it.sentAt })
        assertEquals(SlidingWindow.idealFinish(10, 6), run.finishTick)
    }

    @Test fun aBiggerWindowNeverFinishesLater() {
        for (rtt in listOf(3, 5, 8)) {
            var previous = Int.MAX_VALUE
            for (w in 1..12) {
                val finish = SlidingWindow.run(12, w, rtt).finishTick
                assertTrue(finish <= previous)
                previous = finish
            }
        }
    }

    @Test fun theSmallestFullWindowEqualsTheRttWhenThereAreEnoughFrames() {
        listOf(4, 6, 8).forEach { assertEquals(it, SlidingWindow.smallestFullWindow(frames = 12, rtt = it)) }
    }

    @Test fun neverMoreFramesInFlightThanTheWindowAllows() {
        val run = SlidingWindow.run(frames = 12, window = 4, rtt = 7)
        run.traces.forEach { t ->
            val inFlight = run.traces.count { it.sentAt <= t.sentAt && it.ackedAt > t.sentAt }
            assertTrue(inFlight <= 4)
        }
    }

    // ---- the menu ----

    @Test fun everyGameHasAnIdARewardAndConcepts() {
        assertEquals(LabGame.entries.size, LabGame.entries.map { it.id }.toSet().size)
        LabGame.entries.forEach { g ->
            assertTrue(g.reward > 0)
            assertTrue(g.concepts.isNotEmpty())
            g.concepts.forEach { assertTrue(it.en.isNotBlank() && it.ko.isNotBlank() && it.book.isNotBlank()) }
        }
    }
}

class LabDataTest {
    @Test fun theInboxHasTwoDamagedFramesAndTheCheckFindsExactlyThose() {
        val frames = CrcInbox.frames()
        assertEquals(CrcInbox.messages.size, frames.size)
        assertEquals(2, frames.count { it.damaged })
        frames.forEach { assertEquals(it.damaged, !Crc.isIntact(it.frame)) }
    }

    @Test fun everyRoundIsPlayable() {
        EncodingRounds.all.forEach { assertTrue(it.bits.all { c -> c == '0' || c == '1' }) }
        assertEquals(LineCode.entries.toSet(), EncodingRounds.all.map { it.code }.toSet())
        WindowRounds.all.forEach { r ->
            val need = SlidingWindow.smallestFullWindow(r.frames, r.rtt)
            assertTrue(need in 2..r.frames)
            assertTrue(SlidingWindow.run(r.frames, need - 1, r.rtt).finishTick > SlidingWindow.idealFinish(r.frames, r.rtt))
        }
    }
}
