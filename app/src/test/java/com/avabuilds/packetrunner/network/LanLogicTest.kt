package com.avabuilds.packetrunner.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanLogicTest {

    // ---- the hub ----

    @Test fun aHubRepeatsAFrameToEveryOtherComputer() {
        val d = Hub.deliver(src = 1, dst = 4, computers = 5)
        assertEquals(setOf(2, 3, 4, 5), d.receivers)
    }

    @Test fun onAHubEveryoneExceptTheDestinationGetsAUselessCopy() {
        val d = Hub.deliver(src = 1, dst = 4, computers = 5)
        assertEquals(setOf(2, 3, 5), d.wasted)
    }

    // ---- CSMA/CD ----

    @Test fun theBackoffRangeDoublesAfterEveryCollision() {
        assertEquals(2, Csma.backoffChoices(1))
        assertEquals(4, Csma.backoffChoices(2))
        assertEquals(8, Csma.backoffChoices(3))
    }

    @Test fun theBackoffRangeStopsGrowingAfterTenCollisions() {
        assertEquals(1024, Csma.backoffChoices(10))
        assertEquals(1024, Csma.backoffChoices(16))
    }

    @Test fun aCollisionIsResolvedWhenTheTwoSendersDrawDifferentWaits() {
        val s = LanSession()
        val trace = s.hubCollide(1, 3)
        assertTrue(trace.attempts >= 1)
        assertTrue(trace.rounds.last().first != trace.rounds.last().second)
        // every earlier round was a tie, which is why they had to try again
        trace.rounds.dropLast(1).forEach { assertEquals(it.first, it.second) }
    }

    @Test fun everyDrawStaysInsideTheBackoffRangeOfItsAttempt() {
        for (seed in 1L..40L) {
            LanSession(seed = seed).hubCollide(1, 2).rounds.forEachIndexed { i, (a, b) ->
                val limit = Csma.backoffChoices(i + 1)
                assertTrue(a in 0 until limit && b in 0 until limit)
            }
        }
    }

    // ---- the switch ----

    @Test fun aNewSwitchFloodsToUnknownDestinationsButLearnsTheSender() {
        val s = LanSession()
        s.installSwitch()
        val action = s.switchSend(src = 1, dst = 4)
        assertEquals(BridgeAction.Flood(listOf(2, 3, 4, 5)), action)
        assertEquals(mapOf("A" to 1), s.switchTable)
    }

    @Test fun onceBothAreKnownTheSwitchForwardsToOnePortOnly() {
        val s = LanSession()
        s.installSwitch()
        s.switchSend(1, 4)
        s.switchSend(4, 1) // learns D, and A is known: forwarded
        val action = s.switchSend(1, 4)
        assertEquals(BridgeAction.Forward(4), action)
        assertEquals(setOf(4), action.outPorts())
        assertEquals(mapOf("A" to 1, "D" to 4), s.switchTable)
    }

    @Test fun theSwitchSendsTheReplyOnlyToTheComputerThatSentFirst() {
        val s = LanSession()
        s.installSwitch()
        s.switchSend(1, 4)
        assertEquals(BridgeAction.Forward(1), s.switchSend(4, 1))
    }

    @Test fun aFloodNeverGoesBackOutOfTheInputPort() {
        val s = LanSession()
        s.installSwitch()
        for (src in 1..5) {
            val dst = if (src == 5) 1 else src + 1
            val action = s.switchSend(src, dst)
            assertFalse(src in action.outPorts())
        }
    }

    @Test fun installingTwiceKeepsWhatWasLearned() {
        val s = LanSession()
        s.installSwitch()
        s.switchSend(1, 2)
        s.installSwitch()
        assertEquals(mapOf("A" to 1), s.switchTable)
    }

    @Test fun theHubIsGoneAfterTheSwitchIsInstalled() {
        val s = LanSession()
        s.installSwitch()
        var failed = false
        try { s.hubSend(1, 2) } catch (e: IllegalStateException) { failed = true }
        assertTrue(failed)
    }

    @Test fun aSwitchCannotBeUsedBeforeItIsInstalled() {
        var failed = false
        try { LanSession().switchSend(1, 2) } catch (e: IllegalStateException) { failed = true }
        assertTrue(failed)
    }

    // ---- the lesson's missions ----

    @Test fun missionsAreDoneInTheirOwnTimeAndAllDoneEndsTheLesson() {
        val s = LanSession()
        assertTrue(LanMission.entries.none { s.isDone(it) })
        s.hubSend(1, 4); assertTrue(s.isDone(LanMission.HUB_SEND))
        s.hubCollide(1, 3); assertTrue(s.isDone(LanMission.HUB_COLLIDE))
        s.installSwitch(); assertTrue(s.isDone(LanMission.INSTALL))
        s.switchSend(1, 4); assertTrue(s.isDone(LanMission.SWITCH_FLOOD))
        assertFalse(s.allDone)
        s.switchSend(4, 1); assertTrue(s.isDone(LanMission.SWITCH_FORWARD))
        assertTrue(s.allDone)
    }

    @Test fun computersAreNamedFromA() {
        val s = LanSession()
        assertEquals(listOf("A", "B", "C", "D", "E"), (1..5).map { s.name(it) })
    }

    @Test fun theLanGameIsInTheLabMenuWithItsConcepts() {
        assertTrue(LabGame.LAN in LabGame.entries)
        assertTrue(Concepts.CSMA_CD in LabGame.LAN.concepts && Concepts.LEARNING_BRIDGE in LabGame.LAN.concepts)
    }
}
