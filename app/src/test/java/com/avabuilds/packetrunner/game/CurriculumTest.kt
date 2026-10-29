package com.avabuilds.packetrunner.game

import com.avabuilds.packetrunner.network.CaseFiles
import com.avabuilds.packetrunner.network.LabGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {
    private val neighbors = Neighbors.default()

    @Test fun thereAreSevenLevelsInTheOrderOfTheCourse() {
        assertEquals(7, Curriculum.maxLevel)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), Curriculum.stages.map { it.level })
        assertEquals(
            listOf("Houses", "Cafe", "Office tower", "City Hall", "Apartments", "Troubleshooting", "On the move"),
            Curriculum.stages.map { it.title },
        )
    }

    @Test fun levelsComeFromXpAndStopAtTheLast() {
        assertEquals(1, Curriculum.levelOf(0))
        assertEquals(2, Curriculum.levelOf(100))
        assertEquals(3, Curriculum.levelOf(300))
        assertEquals(7, Curriculum.levelOf(Progress.xpToReach(7)))
        assertEquals(7, Curriculum.levelOf(1_000_000)) // nothing new beyond the last level
    }

    @Test fun buildingsOpenWithTheirLevelAndTheOfficeIsAlwaysOpen() {
        assertTrue(Curriculum.buildingOpen("office", 1))
        assertTrue(listOf("house1", "house2", "house3").all { Curriculum.buildingOpen(it, 1) })
        assertFalse(Curriculum.buildingOpen("cafe", 1))
        assertTrue(Curriculum.buildingOpen("cafe", 2))
        assertFalse(Curriculum.buildingOpen("tower", 2))
        assertTrue(Curriculum.buildingOpen("tower", 3))
        assertFalse(Curriculum.buildingOpen("cityhall", 3))
        assertTrue(Curriculum.buildingOpen("cityhall", 4))
        assertFalse(Curriculum.buildingOpen("apartments", 4))
        assertTrue(Curriculum.buildingOpen("apartments", 5))
    }

    @Test fun everyNeighborLivesInABuildingThatIsOpenFromTheirLevel() {
        neighbors.forEach { n ->
            assertTrue("${n.name} at level ${n.unlockLevel}", Curriculum.buildingOpen(n.buildingId, n.unlockLevel))
            if (n.unlockLevel > 1) assertFalse("${n.name} one level earlier", Curriculum.buildingOpen(n.buildingId, n.unlockLevel - 1))
        }
    }

    @Test fun labGamesOpenAtTheLevelOfTheirTopic() {
        val expected = mapOf("encoding" to 1, "crc" to 2, "window" to 3, "lan" to 3, "mobile" to 7, "transport" to 7)
        assertEquals(expected, LabGame.entries.associate { it.id to Curriculum.labLevel(it.id) })
        assertTrue(Curriculum.labOpen("encoding", 1))
        assertFalse(Curriculum.labOpen("crc", 1))
        assertTrue(Curriculum.labOpen("crc", 2))
        assertFalse(Curriculum.labOpen("lan", 2))
        assertTrue(Curriculum.labOpen("lan", 3) && Curriculum.labOpen("window", 3))
        assertFalse(Curriculum.labOpen("mobile", 6))
        assertTrue(Curriculum.labOpen("mobile", 7) && Curriculum.labOpen("transport", 7))
    }

    @Test fun everyLabGameBelongsToExactlyOneLevel() {
        LabGame.entries.forEach { game -> assertEquals(1, Curriculum.stages.count { game.id in it.labIds }) }
    }

    @Test fun theWorkbenchOpensAtLevelFour() {
        assertFalse(Curriculum.workbenchOpen(3))
        assertTrue(Curriculum.workbenchOpen(4))
        assertTrue(Curriculum.workbenchOpen(7))
    }

    @Test fun caseFilesAreSpreadOverTheLevelsAndEveryCaseHasALevel() {
        val all = Curriculum.stages.flatMap { it.caseIds }
        assertEquals(CaseFiles.all.map { it.id }.toSet(), all.toSet())
        assertEquals(all.size, all.toSet().size)
        assertTrue(Curriculum.caseIds(1).isEmpty())
        assertEquals(listOf("wifi_exposed", "wifi_hidden"), Curriculum.caseIds(2))
        assertEquals(15, Curriculum.caseIds(7).size)
        assertTrue(Curriculum.caseIds(5).none { it.startsWith("frag") || it.startsWith("icmp") || it.startsWith("van") || it.startsWith("transport") })
    }

    @Test fun theNewestTopicComesFirstThenEarlierOnesAreMixedIn() {
        assertTrue(Curriculum.caseRotation(1).isEmpty())
        assertNull(Curriculum.pickCase(1, 0))
        // level 2: only the cafe cases
        assertEquals(setOf("wifi_exposed", "wifi_hidden"), Curriculum.caseRotation(2).toSet())
        // level 3: the first case after the level-up is a tower case, the next one an earlier Wi-Fi case
        assertTrue(Curriculum.pickCase(3, 0)!!.startsWith("tower"))
        assertTrue(Curriculum.pickCase(3, 1)!!.startsWith("wifi"))
        // level 6 has no new building but new cases: they lead, and older topics keep coming
        val six = (0 until 20).map { Curriculum.pickCase(6, it)!! }
        assertTrue(six[0].startsWith("frag") || six[0].startsWith("icmp"))
        assertTrue(six.any { it.startsWith("arp") } && six.any { it.startsWith("wifi") } && six.any { it.startsWith("tower") })
    }

    @Test fun noFutureCaseEverComesEarly() {
        for (level in 1..7) {
            val open = Curriculum.caseIds(level).toSet()
            (0 until 40).forEach { i -> Curriculum.pickCase(level, i)?.let { assertTrue("$it at level $level", it in open) } }
        }
    }

    @Test fun everyOpenCaseIsMetWithinOneRotation() {
        for (level in 2..7) {
            val seen = Curriculum.caseRotation(level).toSet()
            assertEquals("level $level", Curriculum.caseIds(level).toSet(), seen)
        }
    }

    @Test fun theNextUnlockIsShownWithItsXp() {
        assertEquals("Level 2: Cafe opens at 100 XP", Curriculum.nextUnlockText(0))
        assertEquals("Level 3: Office tower opens at 300 XP", Curriculum.nextUnlockText(100))
        assertEquals("Level 7: On the move opens at 2100 XP", Curriculum.nextUnlockText(Progress.xpToReach(6)))
        assertNull(Curriculum.nextUnlockText(Progress.xpToReach(7)))
    }

    @Test fun anOldSaveGetsItsLevelFromItsXp() {
        val save = SaveData(day = 12, money = 900, hearts = emptyMap(), connected = setOf("mina"), solved = emptyList(), xp = 650)
        val restored = GameEngine.restore(save, neighbors, RequestGenerator(1L))
        assertEquals(4, Curriculum.levelOf(restored.xp))
        assertTrue(Curriculum.buildingOpen("cityhall", Curriculum.levelOf(restored.xp)))
        assertFalse(Curriculum.buildingOpen("apartments", Curriculum.levelOf(restored.xp)))
    }

    @Test fun theLevelUpMessageAndWhatsNewDescribeTheLevel() {
        Curriculum.stages.forEach { s ->
            assertTrue(s.nana.contains("Level ${s.level}") || s.level == 1)
            assertTrue(Curriculum.whatsNew(s.level).any { it.startsWith("Topics:") })
        }
        assertTrue(Curriculum.whatsNew(4).contains("The Workbench"))
        assertTrue(Curriculum.whatsNew(3).any { it.contains("Office tower") })
        assertTrue(Curriculum.whatsNew(2).contains("Lab game: crc")) // without names the id is shown
    }

    @Test fun theOfficeTowerLivesInTownFromLevelThreeAndItsTenantAsksForCases() {
        val yuna = neighbors.first { it.id == "yuna" }
        assertEquals(BuildingType.TOWER, yuna.kind)
        assertEquals(3, yuna.unlockLevel)
        assertEquals(3, Curriculum.buildingLevel("tower"))
        val generator = RequestGenerator(5L)
        val calls = (1..20).flatMap { generator.callsFor(it, 3, neighbors, neighbors.filter { n -> n.hasInternetAtStart }.map { n -> n.id }.toSet()) }
        val towerCalls = calls.filter { it.buildingId == "tower" }
        assertTrue(towerCalls.isNotEmpty())
        assertTrue(towerCalls.all { it.type == RequestType.CASE })
    }

    @Test fun theTowerTenantGetsTowerCasesAndNothingElse() {
        assertTrue((0 until 6).all { CaseFiles.forTower(it).id.startsWith("tower_") })
        assertTrue(CaseFiles.forTower(0).id != CaseFiles.forTower(1).id)
    }

    @Test fun aSaveFromBeforeTheTowerStillLoadsWithTheTowerConnected() {
        val old = SaveData(day = 8, money = 700, hearts = mapOf("mina" to 2), connected = setOf("mina", "hana", "dae", "rin"), solved = emptyList(), xp = 800)
        val restored = GameEngine.restore(old, neighbors, RequestGenerator(3L))
        assertTrue("yuna" in restored.connected) // it has internet from the start, so it never asks for an install
        assertTrue(restored.upcoming.none { it.neighborId == "yuna" && it.type == RequestType.INSTALL })
    }

    @Test fun everyLockedBuildingOfTheTownHasALevelAndASpriteForItsLockedLook() {
        val town = Town.default()
        town.buildings.filter { it.id != Curriculum.OFFICE }.forEach { b ->
            assertTrue("${b.id} needs a level", Curriculum.buildingLevel(b.id) != null)
        }
        // not open at level 1 means a lot or a site is drawn; open means the building
        assertEquals(listOf("cafe", "tower", "cityhall", "apartments"), town.buildings.map { it.id }.filter { !Curriculum.buildingOpen(it, 1) && it !in listOf("house1", "house2", "house3") }.sortedBy { Curriculum.buildingLevel(it) }.let { it.sortedBy { id -> Curriculum.buildingLevel(id) } })
    }

    @Test fun theRoadmapMarksTheLevelsDoneCurrentAndLocked() {
        val rows = Curriculum.roadmap(Progress.xpToReach(4) + 10)
        assertEquals(7, rows.size)
        assertEquals(listOf(StageStatus.DONE, StageStatus.DONE, StageStatus.DONE, StageStatus.CURRENT, StageStatus.LOCKED, StageStatus.LOCKED, StageStatus.LOCKED), rows.map { it.status })
        assertEquals(listOf(0, 100, 300, 600, 1000, 1500, 2100), rows.map { it.xpToReach })
        assertEquals(StageStatus.CURRENT, Curriculum.roadmap(0).first().status)
        assertTrue(Curriculum.roadmap(1_000_000).take(6).all { it.status == StageStatus.DONE } && Curriculum.roadmap(1_000_000).last().status == StageStatus.CURRENT)
    }

    @Test fun theNextUnlockSaysWhatOpensAndHowMuchXpIsMissing() {
        val n = Curriculum.nextUnlock(285, LevelUp.labTitles)!!
        assertEquals(3, n.stage.level)
        assertEquals(300, n.xpToReach)
        assertEquals(15, n.xpLeft)
        assertEquals("Level 3: Office tower opens at 300 XP", n.text)
        assertTrue(n.opens.contains("Lab game: From hub to switch"))
        assertEquals(2, Curriculum.nextUnlock(0)!!.stage.level)
        assertEquals(100, Curriculum.nextUnlock(0)!!.xpLeft)
        assertNull(Curriculum.nextUnlock(Progress.xpToReach(7)))
    }

    @Test fun theTopBarLineShowsTheXpAndTheNextPlace() {
        assertEquals("15 / 100 XP → Cafe", Curriculum.hudLine(15))
        assertEquals("0 / 200 XP → Office tower", Curriculum.hudLine(100))
        assertEquals("0 / 700 XP", Curriculum.hudLine(Progress.xpToReach(7))) // no next level
    }
}
