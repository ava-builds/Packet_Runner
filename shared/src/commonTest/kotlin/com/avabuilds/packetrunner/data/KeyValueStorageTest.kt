package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.game.SolvedRecord
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.testing.assertEquals
import com.avabuilds.packetrunner.testing.assertFalse
import com.avabuilds.packetrunner.testing.assertNotNull
import com.avabuilds.packetrunner.testing.assertNull
import com.avabuilds.packetrunner.testing.assertTrue
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.CallStatus
import com.avabuilds.packetrunner.work.PlannedCall
import com.avabuilds.packetrunner.game.Request
import com.avabuilds.packetrunner.work.WeekSnapshot
import com.avabuilds.packetrunner.work.WeekTracker
import com.avabuilds.packetrunner.work.Weekday
import com.avabuilds.packetrunner.work.WorkLog
import com.avabuilds.packetrunner.work.WorkSchedule
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class KeyValueStorageTest {
    private val save = SaveData(
        day = 9, money = 777, hearts = mapOf("mina" to 3), connected = setOf("hana", "yuna"),
        solved = listOf(SolvedRecord(2, RequestType.CASE, "tower", 190)), upgrades = mapOf("scooter" to 1), seed = -123456789012L,
        labDone = setOf("crc"), rungToday = setOf(4), caught = setOf(5), xp = 640,
    )

    @Test fun theSavedGameComesBackAndAnEmptyStoreMeansANewTown() {
        val repo = KvSaveRepository(InMemoryKeyValueStore())
        var first: LoadResult? = null
        repo.load("local") { first = it }
        assertEquals(LoadResult.Found(null), first)
        var saved = false
        repo.save("local", save) { saved = it }
        assertTrue(saved)
        var second: LoadResult? = null
        repo.load("local") { second = it }
        assertEquals(LoadResult.Found(save), second)
        repo.delete("local") { }
        var third: LoadResult? = null
        repo.load("local") { third = it }
        assertEquals(LoadResult.Found(null), third)
    }

    @Test fun damagedSaveTextStartsANewTownInsteadOfCrashing() {
        val kv = InMemoryKeyValueStore().also { it.set("pr_save", "{oops") }
        var result: LoadResult? = null
        KvSaveRepository(kv).load("local") { result = it }
        assertEquals(LoadResult.Found(null), result)
    }

    @Test fun aStoreThatRefusesMakesSavingReportFailure() {
        val full = object : KeyValueStore {
            override fun get(key: String): String? = null
            override fun set(key: String, value: String) = false
            override fun remove(key: String) = Unit
        }
        var ok = true
        KvSaveRepository(full).save("local", save) { ok = it }
        assertFalse(ok)
    }

    @Test fun theNotebookCreatesReadsUpdatesAndDeletesNotes() {
        val repo = KvNotebookRepository(InMemoryKeyValueStore())
        var a: Note? = null
        repo.add("l", NoteDraft(" Routing ", "body one", "Routing"), 100L) { a = it }
        var b: Note? = null
        repo.add("l", NoteDraft("TCP", "body two"), 100L) { b = it }
        assertNotNull(a)
        assertNotNull(b)
        assertTrue(a!!.id != b!!.id) // two notes made in the same millisecond still get different ids
        assertEquals("Routing", a!!.title) // the title is trimmed
        var listed: List<Note> = emptyList()
        repo.list("l") { listed = it.getOrThrow() }
        assertEquals(listOf(a, b), listed)

        var updated = false
        repo.update("l", a!!.copy(body = "changed", updatedAt = 200L)) { updated = it }
        assertTrue(updated)
        var missing = true
        repo.update("l", Note("nope", "", "", "", 0, 0)) { missing = it }
        assertFalse(missing)

        var deleted = false
        repo.delete("l", b!!.id) { deleted = it }
        assertTrue(deleted)
        repo.list("l") { listed = it.getOrThrow() }
        assertEquals(1, listed.size)
        assertEquals("changed", listed[0].body)

        repo.replaceAll("l", listOf(b!!)) { }
        repo.list("l") { listed = it.getOrThrow() }
        assertEquals(listOf(b), listed)
        repo.deleteAll("l") { }
        repo.list("l") { listed = it.getOrThrow() }
        assertTrue(listed.isEmpty())
    }

    @Test fun workDataSurvivesTheTripToTextAndBack() = runTest {
        val call = PlannedCall(Request(7, RequestType.FIX, "hana", "cafe", "Wi-Fi drops \"again\"", 160, 600f), status = CallStatus.RANG)
        val data = WorkData(
            schedule = WorkSchedule(startMinute = 8 * 60, endMinute = 17 * 60, lunchStartMinute = 12 * 60 + 30, lunchMinutes = 45, daysOff = setOf(Weekday.WED, Weekday.SUN)),
            configured = true, demoMode = true, demoRealSec = 1_700_000_000L, demoGameSec = 1_700_001_000L,
            log = WorkLog(20_000L, 540, null),
            calls = CallBook(20_000L, listOf(call)), nextCalls = CallBook(20_001L),
            notificationsAsked = true,
            week = WeekTracker(2_857L, WeekSnapshot(3, 12, 400), 2_856L),
        )
        val store = KvWorkDataStore(InMemoryKeyValueStore())
        store.save(data)
        assertEquals(data, store.load())
    }

    @Test fun missingOrDamagedWorkDataGivesTheDefaults() = runTest {
        assertEquals(WorkData(), KvWorkDataStore(InMemoryKeyValueStore()).load())
        assertEquals(WorkData(), WorkDataCodec.decode("{not json"))
        assertEquals(WorkData(), WorkDataCodec.decode(null))
        // an impossible schedule falls back to the default one but keeps the rest
        val broken = WorkDataCodec.decode("""{"start": 1200, "end": 100, "configured": true}""")
        assertEquals(WorkSchedule(), broken.schedule)
        assertTrue(broken.configured)
    }

    @Test fun clockInAndOutAreStoredAsNullWhenAbsent() = runTest {
        val store = KvWorkDataStore(InMemoryKeyValueStore())
        store.save(WorkData(log = WorkLog(5L, null, null)))
        assertNull(store.load().log.clockInMinute)
        store.save(WorkData(log = WorkLog(5L, 600, 1000)))
        assertEquals(WorkLog(5L, 600, 1000), store.load().log)
    }
}
