package com.avabuilds.packetrunner.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LevelUpTest {
    @Test fun reachingALevelGivesOneEventWithABonusThatGrowsWithTheLevel() {
        val events = LevelUp.events(fromXp = 99, toXp = 100)
        assertEquals(listOf(2), events.map { it.level })
        assertEquals(50, events[0].bonus)
        assertEquals(75, LevelUp.bonus(3))
        assertEquals(175, LevelUp.bonus(7))
    }

    @Test fun noEventWhenTheLevelStaysTheSameOrXpGoesDown() {
        assertTrue(LevelUp.events(0, 99).isEmpty())
        assertTrue(LevelUp.events(150, 150).isEmpty())
        assertTrue(LevelUp.events(400, 100).isEmpty())
        assertTrue(LevelUp.events(-20, 50).isEmpty())
    }

    @Test fun aBigJumpGivesOneEventForEveryLevelReachedInOrder() {
        val events = LevelUp.events(0, Progress.xpToReach(4))
        assertEquals(listOf(2, 3, 4), events.map { it.level })
        assertEquals(50 + 75 + 100, LevelUp.totalBonus(events))
    }

    @Test fun everyCourseLevelHasNanasMessageAndWhatsNew() {
        for (level in 2..Curriculum.maxLevel) {
            val e = LevelUp.event(level)
            assertEquals(Curriculum.stage(level).nana, e.message)
            assertTrue(e.message.contains("Level $level"))
            assertTrue(e.whatsNew.isNotEmpty())
        }
    }

    @Test fun theListNamesTheLabGamesThatOpen() {
        assertTrue(LevelUp.event(2).whatsNew.contains("Lab game: Find the corrupted frame"))
        assertTrue(LevelUp.event(3).whatsNew.containsAll(listOf("Lab game: Fill the pipe", "Lab game: From hub to switch")))
        assertTrue(LevelUp.event(4).whatsNew.contains("The Workbench"))
        assertTrue(LevelUp.event(5).whatsNew.none { it.startsWith("Lab game") })
        assertTrue(LevelUp.event(2).whatsNew.none { it.contains("crc") })
    }

    @Test fun nanaMentionsTheNewPlaceAtTheLevelsThatOpenOne() {
        assertTrue(LevelUp.event(2).message.contains("cafe", ignoreCase = true))
        assertTrue(LevelUp.event(3).message.contains("tower", ignoreCase = true))
        assertTrue(LevelUp.event(4).message.contains("City Hall") && LevelUp.event(4).message.contains("Workbench"))
        assertTrue(LevelUp.event(5).message.contains("Apartments"))
        assertTrue(LevelUp.event(7).message.contains("Lab", ignoreCase = true))
    }

    @Test fun pastTheLastLevelThereIsStillACallAndABonus() {
        val e = LevelUp.event(Curriculum.maxLevel + 1)
        assertEquals(8, e.level)
        assertEquals(200, e.bonus)
        assertTrue(e.message.contains("every topic"))
    }
}

class ChimeTest {
    @Test fun theLevelUpTuneHasTheLengthOfItsNotes() {
        val samples = Chime.samples(Chime.levelUp)
        assertEquals(Chime.levelUp.sumOf { it.ms * Chime.SAMPLE_RATE / 1000 }, samples.size) // each note is a whole number of samples
    }

    @Test fun theVolumeStaysInsideWhatWasAsked() {
        val loud = Chime.samples(Chime.levelUp, volume = 0.4)
        assertTrue(loud.all { abs(it.toInt()) <= (0.4 * Short.MAX_VALUE).toInt() + 1 })
        assertTrue(loud.any { abs(it.toInt()) > 5_000 }) // it is not silent
        assertTrue(Chime.samples(Chime.levelUp, volume = 0.0).all { it.toInt() == 0 })
    }

    @Test fun everyNoteStartsAndEndsSoftlySoThereIsNoClick() {
        val notes = listOf(ChimeNote(440.0, 100), ChimeNote(660.0, 100))
        val s = Chime.samples(notes)
        val rate = Chime.SAMPLE_RATE
        assertEquals(0, s[0].toInt())
        assertTrue(abs(s[100 * rate / 1000 - 1].toInt()) < 800) // the end of the first note
        assertTrue(abs(s.last().toInt()) < 800)
    }

    @Test fun aNoteHasThePitchThatWasAsked() {
        val rate = Chime.SAMPLE_RATE
        val s = Chime.samples(listOf(ChimeNote(523.25, 100)))
        var crossings = 0
        for (i in 1 until s.size) if ((s[i - 1] < 0) != (s[i] < 0)) crossings++
        val hz = crossings / 2.0 / (s.size.toDouble() / rate)
        assertTrue("measured $hz Hz", abs(hz - 523.25) < 25)
    }

    @Test fun noNotesMeansNoSound() {
        assertEquals(0, Chime.samples(emptyList()).size)
    }
}
