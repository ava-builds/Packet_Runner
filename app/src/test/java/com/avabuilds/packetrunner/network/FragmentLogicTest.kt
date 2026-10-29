package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentLogicTest {
    @Test fun aFragmentCarriesWhatFitsAfterTheHeaderRoundedDownToEight() {
        assertEquals(1480, Fragmentation.maxData(1500))
        assertEquals(1376, Fragmentation.maxData(1400))
        assertEquals(576 - 20 - (576 - 20) % 8, Fragmentation.maxData(576))
    }

    @Test fun dataThatFitsGoesAsOneLastFragment() {
        val f = Fragmentation.fragment(1, 1480, 1500)
        assertEquals(listOf(IpFragment(1, 0, 1480, moreFragments = false)), f)
    }

    @Test fun fourThousandBytesOnAnMtuOf1500MakesThreeFragmentsWithTheRightOffsets() {
        val f = Fragmentation.fragment(7, 4000, 1500)
        assertEquals(listOf(0, 185, 370), f.map { it.offsetUnits })
        assertEquals(listOf(1480, 1480, 1040), f.map { it.dataLength })
        assertEquals(listOf(true, true, false), f.map { it.moreFragments })
        assertEquals(listOf(0, 1480, 2960), f.map { it.byteOffset })
        assertEquals(1500, f[0].totalLength)
        assertEquals(1060, f[2].totalLength)
    }

    @Test fun everyFragmentStartsWhereThePreviousOneEnded() {
        for (mtu in listOf(576, 1280, 1400, 1500, 9000)) for (len in listOf(1, 100, 1479, 1480, 1481, 4000, 65000)) {
            val f = Fragmentation.fragment(1, len, mtu)
            assertEquals(len, f.sumOf { it.dataLength })
            f.zipWithNext().forEach { (a, b) -> assertEquals(a.byteOffset + a.dataLength, b.byteOffset) }
            f.dropLast(1).forEach { assertEquals(0, it.dataLength % 8); assertTrue(it.totalLength <= mtu) }
            assertTrue(f.last().totalLength <= mtu)
            assertEquals(1, f.count { !it.moreFragments })
        }
    }

    @Test fun allTheFragmentsPutTheDatagramBackTogetherInAnyOrder() {
        val f = Fragmentation.fragment(7, 4000, 1500)
        assertEquals(Reassembly.Complete(4000), Fragmentation.reassemble(f))
        assertEquals(Reassembly.Complete(4000), Fragmentation.reassemble(f.reversed()))
    }

    @Test fun aMissingMiddleFragmentLeavesAGapAndAMissingLastOneLeavesNoEnd() {
        val f = Fragmentation.fragment(7, 4000, 1500)
        assertEquals(Reassembly.Incomplete(listOf(1480..2959), lastSeen = true), Fragmentation.reassemble(listOf(f[0], f[2])))
        assertEquals(Reassembly.Incomplete(emptyList(), lastSeen = false), Fragmentation.reassemble(f.take(2)))
        assertEquals(Reassembly.Incomplete(listOf(0..2959), lastSeen = true), Fragmentation.reassemble(listOf(f[2])))
    }

    @Test fun aFragmentThatArrivesTwiceIsCountedOnce() {
        val f = Fragmentation.fragment(7, 4000, 1500)
        assertEquals(Reassembly.Complete(4000), Fragmentation.reassemble(f + f[1]))
    }

    @Test fun moreFragmentsMeanALowerChanceThatTheDatagramSurvives() {
        assertEquals(100, Fragmentation.survivalPercent(1, 100))
        assertEquals(90, Fragmentation.survivalPercent(1, 90))
        assertEquals(73, Fragmentation.survivalPercent(3, 90))
        assertTrue(Fragmentation.survivalPercent(45, 90) < Fragmentation.survivalPercent(3, 90))
    }

    @Test fun aPingThatFitsIsDeliveredAndATooBigOneIsDroppedOrFragmented() {
        assertEquals(Probe.Delivered, Fragmentation.probe(1372, 1400, dontFragment = true))
        assertEquals(Probe.TooBigDropped, Fragmentation.probe(1400, 1400, dontFragment = true))
        assertEquals(Probe.Fragmented(2), Fragmentation.probe(1400, 1400, dontFragment = false))
    }

    @Test fun theCaseScansMatchTheLogic() {
        val camera = FragmentCases.camera
        assertEquals(3, FragmentCases.cameraFragments.size)
        assertEquals(listOf("185", "1480", "1 (more)"), camera.scans[0].rows[1])
        assertEquals(listOf("gap", "bytes 1480–2959", "missing"), camera.scans[1].rows.last())
        assertEquals(1, camera.afterFix.rows.size)
        val vpn = FragmentCases.vpn
        assertEquals(listOf("1372", "1392", "reply"), vpn.scans[0].rows[1])
        assertEquals(listOf("1400", "1420", "no reply"), vpn.scans[0].rows[2])
        assertFalse(vpn.afterFix.rows.isEmpty())
    }

    @Test fun theNewCasesJoinTheRotation() {
        assertTrue(CaseFiles.all.map { it.id }.containsAll(listOf("frag_camera", "frag_vpn")))
        assertEquals(CaseFiles.all.size, CaseFiles.all.map { it.id }.toSet().size)
    }
}
