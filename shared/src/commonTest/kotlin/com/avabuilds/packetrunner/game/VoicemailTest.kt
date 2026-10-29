package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import kotlin.test.Test

class VoicemailTest {
    private val neighbors = Neighbors.default()
    private val gen = RequestGenerator(1L)
    private fun call(id: Int, who: String = "hana", minute: Float = 600f) = Request(id, RequestType.FIX, who, if (who == "hana") "cafe" else "house1", "hi", 150, minute)
    private fun day(vararg calls: Request) = GameEngine.startWorkDay(GameEngine.newGame(neighbors, gen), calls.toList(), newDay = false)
    private fun ringingAt(s: GameState, minute: Float) = GameEngine.syncTo(s, minute, canRing = true)

    // ---- quick answers ----
    @Test fun answeringQuicklyPaysABonusAndAnExtraHeart() {
        val ringing = ringingAt(day(call(1)), 600f)
        val before = ringing.copy(hearts = ringing.hearts + ("hana" to 1))
        val answered = GameEngine.answerCall(GameEngine.syncTo(before, 600f + GameEngine.QUICK_ANSWER_MINUTES, canRing = true))
        assertEquals(before.money + GameEngine.QUICK_BONUS, answered.money)
        assertEquals(2, answered.hearts.getValue("hana"))
        assertEquals(1, answered.todayQuickAnswers)
        assertEquals(1, answered.active?.id)
        assertNull(answered.ringing)
    }

    @Test fun answeringLateIsStillFineButEarnsNoBonus() {
        val ringing = ringingAt(day(call(1)), 600f)
        val late = GameEngine.answerCall(ringing.copy(minute = 600f + GameEngine.QUICK_ANSWER_MINUTES + 1))
        assertEquals(ringing.money, late.money)
        assertEquals(ringing.hearts, late.hearts)
        assertEquals(0, late.todayQuickAnswers)
        assertEquals(1, late.active?.id)
    }

    @Test fun answeringWithNoRingingPhoneDoesNothing() {
        val s = day(call(1))
        assertEquals(s, GameEngine.answerCall(s))
    }

    @Test fun theQuickBonusCannotPushHeartsPastTheMaximum() {
        val ringing = ringingAt(day(call(1)), 600f)
        val full = ringing.copy(hearts = ringing.hearts + ("hana" to MAX_HEARTS))
        assertEquals(MAX_HEARTS, GameEngine.answerCall(full).hearts.getValue("hana"))
    }

    // ---- voicemail ----
    @Test fun aMissedCallCanBeReturnedLaterAndBecomesTheActiveJob() {
        val vm = GameEngine.ignore(ringingAt(day(call(1)), 600f))
        val back = GameEngine.returnVoicemail(vm, 1)
        assertEquals(1, back.active?.id)
        assertTrue(back.voicemails.isEmpty())
    }

    @Test fun youCannotReturnACallWhileTheyPhoneIsBusy() {
        val s = ringingAt(day(call(1), call(2, "mina", 610f)), 600f)
        val busyRinging = GameEngine.ignore(s).copy(ringing = call(2, "mina"))
        assertEquals(busyRinging, GameEngine.returnVoicemail(busyRinging, 1))
        val busyWorking = GameEngine.returnVoicemail(GameEngine.ignore(s), 1)
        val another = busyWorking.copy(voicemails = listOf(call(3)))
        assertEquals(another, GameEngine.returnVoicemail(another, 3))
    }

    @Test fun returningACallThatIsNotInTheInboxChangesNothing() {
        val s = day(call(1))
        assertEquals(s, GameEngine.returnVoicemail(s, 99))
    }

    @Test fun aSolvedReturnedCallPaysLikeAnyJob() {
        val vm = GameEngine.ignore(ringingAt(day(call(1)), 600f))
        val done = GameEngine.complete(GameEngine.returnVoicemail(vm, 1), solved = true)
        assertEquals(vm.money + 150, done.money)
        assertEquals(1, done.todaySolved)
    }

    // ---- end of shift ----
    @Test fun unansweredVoicemailsMakeTheirNeighborsALittleSadAtClockOut() {
        val s = day().copy(
            hearts = mapOf("hana" to 3, "mina" to 0, "joon" to 0, "sora" to 0, "dae" to 0, "rin" to 0),
            voicemails = listOf(call(1), call(2, "mina"), call(3)),
        )
        val after = GameEngine.endShift(s)
        assertEquals(2, after.hearts.getValue("hana")) // two voicemails from the same neighbor cost one heart
        assertEquals(0, after.hearts.getValue("mina")) // never below zero
        assertEquals(3, after.todayMissed)
        assertTrue(after.voicemails.isEmpty())
    }

    @Test fun endingAShiftWithNoVoicemailsChangesNothing() {
        val s = day(call(1))
        assertEquals(s, GameEngine.endShift(s))
    }

    @Test fun answeredAndReturnedCallsNeverCostAnythingAtClockOut() {
        val vm = GameEngine.ignore(ringingAt(day(call(1)), 600f))
        val returned = GameEngine.complete(GameEngine.returnVoicemail(vm, 1), solved = true)
        val after = GameEngine.endShift(returned)
        assertEquals(returned.hearts, after.hearts)
        assertEquals(0, after.todayMissed)
    }

    @Test fun aNewWorkDayStartsWithAnEmptyInbox() {
        val s = day().copy(voicemails = listOf(call(1)), todayQuickAnswers = 2)
        val next = GameEngine.startWorkDay(s, emptyList(), newDay = true)
        assertTrue(next.voicemails.isEmpty())
        assertEquals(0, next.todayQuickAnswers)
    }

    @Test fun tappingTheNotificationOfAVoicemailRingsItAgain() {
        val vm = GameEngine.ignore(ringingAt(day(call(1)), 600f))
        val opened = GameEngine.bringCallForward(vm, 1)
        assertTrue(opened.voicemails.isEmpty())
        assertEquals(1, GameEngine.syncTo(opened, 640f, canRing = true).ringing?.id)
    }
}
