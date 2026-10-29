package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerCasesTest {
    @Test fun aFrameOnTheHubReachesEveryoneAndOnlyOneWasMeantToGetIt() {
        val scan = TowerCases.hub.scans[0]
        assertEquals(4, scan.rows.size)
        assertEquals(listOf("B", "yes", "no ✗ wasted"), scan.rows[0])
        assertEquals(listOf("C", "yes", "yes ✓"), scan.rows[1])
        assertEquals(3, scan.rows.count { it[2].contains("wasted") })
    }

    @Test fun theCollisionScanKeepsGoingUntilTheTwoSendersDrawDifferentWaits() {
        val rows = TowerCases.hub.scans[1].rows
        assertTrue(rows.size >= 3) // at least two clashes, so the doubling range shows
        assertTrue(rows.dropLast(1).all { it[2].endsWith("clash again") })
        assertTrue(!rows.last()[2].endsWith("clash again") || rows.size == Csma.MAX_ATTEMPTS)
    }

    @Test fun theSwitchFloodsAnUnknownDestinationAndThenForwardsToOnePort() {
        val rows = TowerCases.hub.afterFix.rows
        assertEquals("C → A (first frame)", rows[0][0])
        assertEquals("flooded to ports 1, 2, 4, 5", rows[0][1])
        assertEquals(listOf("A → C", "forwarded to port 3"), rows[1])
    }

    @Test fun aSwitchKeepsSendingToThePortWhereItLastSawTheDeviceUntilTheDeviceSpeaks() {
        val bridge = LearningBridge(6)
        bridge.receive("Printer", "PC 1", 3)
        assertEquals(BridgeAction.Forward(3), bridge.receive("PC 1", "Printer", 1))
        assertEquals(BridgeAction.Forward(3), bridge.receive("PC 1", "Printer", 1)) // the printer moved, but it has not spoken
        bridge.receive("Printer", "PC 1", 5)
        assertEquals(BridgeAction.Forward(5), bridge.receive("PC 1", "Printer", 1))
    }

    @Test fun theMovedPrinterScansShowTheStaleEntryAndTheFix() {
        val moved = TowerCases.moved
        assertTrue(moved.scans[0].rows.contains(listOf("Printer", "3")))
        assertEquals("forwarded to port 3 (the old socket)", moved.scans[1].rows[0][1])
        assertTrue(moved.afterFix.rows.contains(listOf("Printer", "5")))
        assertEquals(listOf("next job: PC 1 → Printer", "forwarded to port 5"), moved.afterFix.rows.last())
    }

    @Test fun theNewCasesJoinTheRotation() {
        assertTrue(CaseFiles.all.map { it.id }.containsAll(listOf("tower_hub", "tower_moved")))
        assertEquals(CaseFiles.all.size, CaseFiles.all.map { it.id }.toSet().size)
    }
}
