package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMonitorTest {
    private val hana = Neighbors.default().first { it.id == "hana" }
    private val mina = Neighbors.default().first { it.id == "mina" }

    /** Day 1 at the last level, so everyone lives in town: Hana's cafe is connected and will call with a FIX at [callMinute]. */
    private fun dayWithCall(minute: Float, callMinute: Float = 600f): GameState {
        val call = Request(1, RequestType.FIX, "hana", "cafe", "help", 150, callMinute)
        return GameEngine.newGame(Neighbors.default(), RequestGenerator(1L)).copy(upcoming = listOf(call), minute = minute, xp = Progress.xpToReach(Curriculum.maxLevel))
    }

    @Test fun aPlaceWithoutNetworkHasNoLink() {
        assertEquals(LinkStatus.NO_LINK, NetworkMonitor.status(dayWithCall(480f), mina))
    }

    @Test fun theLinkIsHealthyUntilTheWarningWindowStarts() {
        val before = 600f - NetworkMonitor.WARNING_LEAD - 1f
        assertEquals(LinkStatus.HEALTHY, NetworkMonitor.status(dayWithCall(before), hana))
    }

    @Test fun packetsStartToGetLostBeforeTheNeighborCalls() {
        val inWindow = 600f - NetworkMonitor.WARNING_LEAD + 1f
        assertEquals(LinkStatus.DEGRADED, NetworkMonitor.status(dayWithCall(inWindow), hana))
    }

    @Test fun onceTheCallIsDueOrRingingTheLinkIsDown() {
        assertEquals(LinkStatus.DOWN, NetworkMonitor.status(dayWithCall(600f), hana))
        val s = dayWithCall(590f)
        val ringing = s.copy(upcoming = emptyList(), ringing = s.upcoming.first())
        assertEquals(LinkStatus.DOWN, NetworkMonitor.status(ringing, hana))
    }

    @Test fun reportingInTheWindowPaysABonusOnce() {
        val s = dayWithCall(600f - NetworkMonitor.WARNING_LEAD + 1f)
        val first = NetworkMonitor.report(s, hana)
        assertEquals(FlagOutcome.EARLY_CATCH, first.outcome)
        assertTrue(first.bonus >= NetworkMonitor.EARLY_BONUS)
        assertEquals(s.money + first.bonus, first.state.money)
        assertEquals(1, first.state.todayCaught)
        assertTrue(NetworkMonitor.isReported(first.state, hana))
        assertEquals(FlagOutcome.ALREADY_REPORTED, NetworkMonitor.report(first.state, hana).outcome)
    }

    @Test fun reportingEarlierPaysMore() {
        val early = NetworkMonitor.report(dayWithCall(600f - NetworkMonitor.WARNING_LEAD + 1f), hana).bonus
        val late = NetworkMonitor.report(dayWithCall(590f), hana).bonus
        assertTrue(early > late)
        assertTrue(late >= NetworkMonitor.EARLY_BONUS)
    }

    @Test fun reportingAfterTheCallIsTooLateAndFree() {
        val s = dayWithCall(610f)
        val r = NetworkMonitor.report(s, hana)
        assertEquals(FlagOutcome.TOO_LATE, r.outcome)
        assertEquals(s, r.state)
    }

    @Test fun reportingAHealthyLinkIsAFalseAlarmThatCostsMoney() {
        val s = dayWithCall(480f, callMinute = 900f)
        val r = NetworkMonitor.report(s, hana)
        assertEquals(FlagOutcome.FALSE_ALARM, r.outcome)
        assertEquals(s.money - NetworkMonitor.FALSE_ALARM_COST, r.state.money)
        assertEquals(1, r.state.todayFalseAlarms)
    }

    @Test fun aFalseAlarmNeverTakesMoneyBelowZero() {
        val r = NetworkMonitor.report(dayWithCall(480f, callMinute = 900f).copy(money = 5), hana)
        assertEquals(0, r.state.money)
    }

    @Test fun reportingAPlaceWithoutNetworkDoesNothing() {
        val s = dayWithCall(480f)
        val r = NetworkMonitor.report(s, mina)
        assertEquals(FlagOutcome.NO_LINK, r.outcome)
        assertEquals(s, r.state)
    }

    @Test fun installCallsAreNotNetworkProblems() {
        val s = dayWithCall(480f)
        val install = s.upcoming.first().copy(type = RequestType.INSTALL)
        assertEquals(LinkStatus.HEALTHY, NetworkMonitor.status(s.copy(upcoming = listOf(install), minute = 590f), hana))
    }

    @Test fun theNextDayStartsWithCleanMonitorCounters() {
        val neighbors = Neighbors.default()
        val s = dayWithCall(600f - NetworkMonitor.WARNING_LEAD + 1f)
        val reported = NetworkMonitor.report(s, hana).state
        val next = GameEngine.nextDay(reported, neighbors, RequestGenerator(1L))
        assertEquals(0, next.todayCaught)
        assertEquals(0, next.todayBonus)
        assertTrue(next.caught.isEmpty())
    }

    // ---- packets ----

    @Test fun aHealthyLinkDeliversEveryPacket() {
        listOf(0f, 0.3f, 0.8f).forEach { phase ->
            assertTrue(PacketFlow.dots(LinkStatus.HEALTHY, phase).none { it.lost })
        }
    }

    @Test fun aDownLinkLosesEveryPacketBeforeTheMiddle() {
        val dots = PacketFlow.dots(LinkStatus.DOWN, 0.9f)
        assertTrue(dots.all { it.progress <= 0.35f })
        assertTrue(dots.any { it.lost })
    }

    @Test fun aDegradedLinkLosesSomePacketsButNotAll() {
        val seen = (0 until 20).flatMap { PacketFlow.dots(LinkStatus.DEGRADED, it / 20f) }
        assertTrue(seen.any { it.lost })
        assertTrue(seen.any { !it.lost && it.progress > 0.7f })
    }

    @Test fun aPlaceWithoutNetworkShowsNoPackets() {
        assertFalse(PacketFlow.dots(LinkStatus.NO_LINK, 0.5f).isNotEmpty())
    }

    @Test fun aNeighborWhoHasNotMovedInYetHasNoLink() {
        val dae = Neighbors.default().first { it.id == "dae" }
        val s = dayWithCall(480f).copy(connected = setOf("hana", "dae"))
        assertEquals(LinkStatus.NO_LINK, NetworkMonitor.status(s.copy(xp = Progress.xpToReach(3)), dae)) // City Hall opens at level 4
        assertEquals(LinkStatus.HEALTHY, NetworkMonitor.status(s.copy(xp = Progress.xpToReach(4)), dae))
    }
}
