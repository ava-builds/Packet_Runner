package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.game.RequestGenerator
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.game.SolvedRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonTextTest {
    @Test fun mapsListsTextNumbersAndBooleansSurviveTheTrip() {
        val data = mapOf("a" to 1, "b" to listOf("x", "y"), "c" to mapOf("d" to true), "e" to "line\n\"quoted\" \\ é")
        val back = JsonText.readMap(JsonText.write(data))!!
        assertEquals(1L, back["a"])
        assertEquals(listOf("x", "y"), back["b"])
        assertEquals(mapOf("d" to true), back["c"])
        assertEquals("line\n\"quoted\" \\ é", back["e"])
    }

    @Test fun damagedOrEmptyTextGivesNullInsteadOfCrashing() {
        assertNull(JsonText.read(null))
        assertNull(JsonText.read(""))
        assertNull(JsonText.read("{\"a\":"))
        assertNull(JsonText.read("{\"a\":1} extra"))
        assertNull(JsonText.readMap("[1,2]"))
    }

    @Test fun aWholeSaveSurvivesTheTripThroughJson() {
        val neighbors = Neighbors.default()
        val save = SaveData(
            day = 7, money = 1234,
            hearts = mapOf("mina" to 3), connected = setOf("hana", "mina"),
            solved = listOf(SolvedRecord(2, RequestType.FIX, "cafe", 160)),
            upgrades = mapOf("scooter" to 1), seed = -987654321012L,
            labDone = setOf("crc"), rungToday = setOf(3, 4), caught = setOf(5), xp = 420,
        )
        assertEquals(save, SaveData.fromMap(JsonText.readMap(JsonText.write(save.toMap()))!!))
        val state = GameEngine.newGame(neighbors, RequestGenerator(5L))
        val fresh = SaveData.from(state)
        assertEquals(fresh, SaveData.fromMap(JsonText.readMap(JsonText.write(fresh.toMap()))!!))
    }
}

class NoteStorageTest {
    private val a = Note("n1", "Routing", "line one\nline two", "Routing", 10L, 20L)
    private val b = Note("n2", "TCP \"hello\"", "", "", 30L, 30L)

    @Test fun notesSurviveBeingStoredAsText() {
        assertEquals(listOf(a, b), NoteStorage.decode(NoteStorage.encode(listOf(a, b))))
    }

    @Test fun noStoredTextOrDamagedTextGivesAnEmptyNotebook() {
        assertEquals(emptyList<Note>(), NoteStorage.decode(null))
        assertEquals(emptyList<Note>(), NoteStorage.decode("{oops"))
    }

    @Test fun newIdsNeverClashWithExistingOnes() {
        assertEquals("n99", NoteStorage.nextId(listOf(a), 99L))
        assertEquals("n99-1", NoteStorage.nextId(listOf(Note("n99", "", "", "", 0, 0)), 99L))
    }
}
