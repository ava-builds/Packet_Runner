package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportCasesTest {
    @Test fun theStreamOverTcpFreezesBehindLostFramesAndNothingIsMissing() {
        val tcp = TransportCases.streamer.scans[0].rows
        assertEquals(12, tcp.size)
        assertTrue(tcp.any { it[3] == "lost, sent again" })
        assertTrue(tcp.any { it[3] == "waits behind a lost one" })
        assertTrue(tcp.none { it[3].startsWith("lost, never") })
    }

    @Test fun theStreamOverUdpSkipsLostFramesAndEverythingElseIsOnTime() {
        val udp = TransportCases.streamer.afterFix.rows
        assertEquals(2, udp.count { it[3] == "lost, never arrives" })
        assertEquals(10, udp.count { it[3] == "on time" })
    }

    @Test fun theSummaryShowsTcpFreezingAndUdpSmoothWithSomeMissing() {
        val summary = TransportCases.streamer.scans[1].rows
        assertEquals("freezes", summary[0][3])
        assertEquals("2 missing", summary[1][3])
    }

    @Test fun theBankOverUdpLosesASegmentAndOverTcpGetsEverythingALittleLate() {
        val udp = TransportCases.bankCase.scans[0].rows
        assertEquals(listOf("3", "tick 2", "—", "lost, never arrives"), udp[2])
        val summary = TransportCases.bankCase.scans[1].rows
        assertEquals("1 missing", summary[0][3])
        assertEquals("smooth", summary[1][3])
        val tcp = TransportCases.bankCase.afterFix.rows
        assertTrue(tcp.none { it[2] == "—" })
        assertEquals("lost, sent again", tcp[2][3])
    }

    @Test fun eachAppHasTheTransportItNeedsAccordingToTheLabRules() {
        val apps = TransportApps.all
        assertEquals(Transport.UDP, apps.first { it.name == "Live stream" }.bestTransport)
        assertEquals(Transport.TCP, apps.first { it.name == "Bank transfer" }.bestTransport)
        assertEquals(TransportApp.Verdict.STUTTERS, apps.first { it.name == "Live stream" }.judge(Transport.TCP))
        assertEquals(TransportApp.Verdict.DATA_MISSING, apps.first { it.name == "Bank transfer" }.judge(Transport.UDP))
    }

    @Test fun theNewCasesJoinTheRotation() {
        assertTrue(CaseFiles.all.map { it.id }.containsAll(listOf("transport_stream", "transport_bank")))
        assertEquals(CaseFiles.all.size, CaseFiles.all.map { it.id }.toSet().size)
    }
}
