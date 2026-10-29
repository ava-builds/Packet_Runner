package com.avabuilds.packetrunner.network

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class WifiCellTest {
    // A B C D in a line, 10 m apart
    private val nodes = listOf(AirNode("A", 0), AirNode("B", 10), AirNode("C", 20), AirNode("D", 30))
    private val cell = WifiCell(nodes, range = 12)
    private val ba = Flow("B", "A")
    private val cd = Flow("C", "D")

    @Test fun devicesHearEachOtherOnlyOnTheSameChannelWithinRange() {
        assertTrue(cell.hears("A", "B"))
        assertFalse(cell.hears("A", "C"))
        assertFalse(cell.hears("A", "A"))
        val other = WifiCell(nodes.map { if (it.name == "B") it.copy(channel = 6) else it }, 12)
        assertFalse(other.hears("A", "B"))
    }

    @Test fun twoPairsBackToBackAreAnExposedNode() {
        assertEquals(Interplay.EXPOSED, cell.interplay(ba, cd))
        assertEquals(Interplay.EXPOSED, cell.interplay(cd, ba))
    }

    @Test fun exposedSendersTakeTurnsAndHalfTheAirtimeIsWasted() {
        val results = cell.simulate(listOf(ba, cd), slots = 6)
        assertTrue(results.all { r -> r.outcomes.values.count { it == SlotOutcome.SENT } == 1 && r.outcomes.values.count { it == SlotOutcome.DEFERRED } == 1 })
        assertEquals(50, WifiCell.utilizationPercent(results))
        // both take turns first
        assertEquals(setOf(ba, cd), results.map { r -> r.outcomes.entries.first { it.value == SlotOutcome.SENT }.key }.toSet())
    }

    @Test fun separateChannelsOrLowerPowerFreeTheExposedNode() {
        val channels = WifiCell(nodes.map { if (it.name == "C" || it.name == "D") it.copy(channel = 11) else it }, 12)
        assertEquals(Interplay.INDEPENDENT, channels.interplay(ba, cd))
        assertEquals(100, WifiCell.utilizationPercent(channels.simulate(listOf(ba, cd), 6)))
        val quieter = WifiCell(nodes, range = 8)
        assertEquals(100, WifiCell.utilizationPercent(quieter.simulate(listOf(ba, cd), 6)))
    }

    @Test fun moreRangeMakesItWorseNotBetter() {
        val loud = WifiCell(nodes, range = 40)
        assertTrue(WifiCell.utilizationPercent(loud.simulate(listOf(ba, cd), 6)) < 100)
        assertEquals(Interplay.INDEPENDENT, loud.interplay(ba, cd))
    }

    private val hiddenNodes = listOf(AirNode("A", 0), AirNode("R", 10), AirNode("C", 20))
    private val hiddenCell = WifiCell(hiddenNodes, 12)
    private val ar = Flow("A", "R")
    private val cr = Flow("C", "R")

    @Test fun twoSendersThatCannotHearEachOtherAreHiddenFromEachOther() {
        assertEquals(Interplay.HIDDEN, hiddenCell.interplay(ar, cr))
    }

    @Test fun hiddenSendersTalkAtTheSameTimeAndCollideAtTheReceiver() {
        val results = hiddenCell.simulate(listOf(ar, cr), 4)
        assertTrue(results.all { r -> r.outcomes.values.all { it == SlotOutcome.COLLIDED } })
        assertEquals(0, WifiCell.utilizationPercent(results))
    }

    @Test fun rtsCtsMakesTheHiddenSenderWaitForTheClearToSend() {
        val results = hiddenCell.simulate(listOf(ar, cr), 4, rtsCts = true)
        assertTrue(results.all { r -> r.outcomes.values.count { it == SlotOutcome.SENT } == 1 && r.outcomes.values.count { it == SlotOutcome.DEFERRED } == 1 })
        assertEquals(50, WifiCell.utilizationPercent(results))
    }

    @Test fun rtsCtsDoesNotHelpAnExposedNode() {
        val without = WifiCell.utilizationPercent(cell.simulate(listOf(ba, cd), 6))
        val with = WifiCell.utilizationPercent(cell.simulate(listOf(ba, cd), 6, rtsCts = true))
        assertEquals(without, with)
    }

    @Test fun aSingleConversationAlwaysSucceedsAndNoFlowsMeansNothingUsed() {
        assertEquals(100, WifiCell.utilizationPercent(cell.simulate(listOf(ba), 3)))
        assertEquals(0, WifiCell.utilizationPercent(emptyList()))
    }

    @Test fun theCaseScansMatchTheSimulation() {
        val e = WifiCases.exposed
        assertTrue(e.scans[1].note.contains("50%"))
        assertTrue(e.afterFix.note.contains("100%"))
        assertEquals("6 of 6", e.afterFix.rows.last()[1])
        assertEquals("3 of 6", e.scans[1].rows.last()[1])
        val h = WifiCases.hidden
        assertTrue(h.scans[1].note.contains("0%"))
        assertTrue(h.scans[1].rows.first().drop(1).all { it == "collision ✗" })
        assertTrue(h.afterFix.note.contains("50%"))
        val phoneA = h.scans[0].rows.first { it[0].startsWith("Phone A") }
        assertEquals("Router", phoneA[2]) // the hidden phone cannot hear the other phone
    }

    @Test fun theNewCasesJoinTheRotation() {
        assertTrue(CaseFiles.all.map { it.id }.containsAll(listOf("wifi_exposed", "wifi_hidden")))
        assertEquals(CaseFiles.all.size, CaseFiles.all.map { it.id }.toSet().size)
    }
}
