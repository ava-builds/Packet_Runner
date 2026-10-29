package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.WeekSnapshot
import com.avabuilds.packetrunner.work.WeekTracker
import com.avabuilds.packetrunner.work.WorkLog
import com.avabuilds.packetrunner.work.WorkSchedule

/**
 * The smallest storage there is: text under a key. The browser's localStorage is one (see the web app); the classes below
 * keep the game, the notebook and the work data in any [KeyValueStore], so that code is the same everywhere and can be tested.
 */
interface KeyValueStore {
    fun get(key: String): String?

    /** Stores [value]; false when the store refused (full, or blocked by the browser). */
    fun set(key: String, value: String): Boolean
    fun remove(key: String)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val map = linkedMapOf<String, String>()
    override fun get(key: String) = map[key]
    override fun set(key: String, value: String): Boolean { map[key] = value; return true }
    override fun remove(key: String) { map.remove(key) }
}

/** The saved game as JSON text under one key. [uid] is ignored: there is one local game per device. */
class KvSaveRepository(private val kv: KeyValueStore, private val key: String = "pr_save") : SaveRepository {
    override fun load(uid: String, onDone: (LoadResult) -> Unit) {
        val map = JsonText.readMap(kv.get(key))
        onDone(LoadResult.Found(map?.let(SaveData::fromMap))) // nothing stored, or damaged text: a new town
    }

    override fun save(uid: String, data: SaveData, onDone: (Boolean) -> Unit) = onDone(kv.set(key, JsonText.write(data.toMap())))

    override fun delete(uid: String, onDone: (Boolean) -> Unit) {
        kv.remove(key)
        onDone(true)
    }
}

/** The notebook as one JSON list under one key. */
class KvNotebookRepository(private val kv: KeyValueStore, private val key: String = "pr_notes") : NotebookRepository {
    private fun read(): List<Note> = NoteStorage.decode(kv.get(key))
    private fun write(notes: List<Note>): Boolean = kv.set(key, NoteStorage.encode(notes))

    override fun list(uid: String, onDone: (Result<List<Note>>) -> Unit) = onDone(Result.success(read()))

    override fun add(uid: String, draft: NoteDraft, now: Long, onDone: (Note?) -> Unit) {
        val notes = read()
        val note = Note(NoteStorage.nextId(notes, now), draft.title.trim(), draft.body, draft.topic, createdAt = now, updatedAt = now)
        onDone(if (write(notes + note)) note else null)
    }

    override fun update(uid: String, note: Note, onDone: (Boolean) -> Unit) {
        val notes = read()
        onDone(note.id in notes.map { it.id } && write(notes.map { if (it.id == note.id) note else it }))
    }

    override fun delete(uid: String, noteId: String, onDone: (Boolean) -> Unit) {
        val notes = read()
        onDone(noteId in notes.map { it.id } && write(notes.filter { it.id != noteId }))
    }

    override fun deleteAll(uid: String, onDone: (Boolean) -> Unit) {
        kv.remove(key)
        onDone(true)
    }

    override fun replaceAll(uid: String, notes: List<Note>, onDone: (Boolean) -> Unit) = onDone(write(notes))
}

/** The work data as JSON under one key. */
class KvWorkDataStore(private val kv: KeyValueStore, private val key: String = "pr_work") : WorkDataStore {
    override suspend fun load(): WorkData = WorkDataCodec.decode(kv.get(key))
    override suspend fun save(data: WorkData) { kv.set(key, WorkDataCodec.encode(data)) }
}

/** Turns [WorkData] into text and back. Missing or damaged values fall back to the defaults instead of breaking the app. */
object WorkDataCodec {
    fun encode(d: WorkData): String = JsonText.write(
        mapOf(
            "start" to d.schedule.startMinute,
            "end" to d.schedule.endMinute,
            "lunchStart" to d.schedule.lunchStartMinute,
            "lunchMinutes" to d.schedule.lunchMinutes,
            "daysOff" to daysOffToBits(d.schedule.daysOff),
            "configured" to d.configured,
            "demo" to d.demoMode,
            "demoReal" to d.demoRealSec,
            "demoGame" to d.demoGameSec,
            "logDay" to d.log.epochDay,
            "clockIn" to d.log.clockInMinute,
            "clockOut" to d.log.clockOutMinute,
            "calls" to d.calls.encode(),
            "nextCalls" to d.nextCalls.encode(),
            "notifAsked" to d.notificationsAsked,
            "weekIndex" to d.week.week,
            "weekPaid" to d.week.paidWeek,
            "weekSolved" to d.week.start.solvedCount,
            "weekHearts" to d.week.start.heartsTotal,
            "weekXp" to d.week.start.xp,
        ),
    )

    fun decode(text: String?): WorkData {
        val m = JsonText.readMap(text) ?: return WorkData()
        fun int(k: String): Int? = (m[k] as? Number)?.toInt()
        fun long(k: String): Long? = (m[k] as? Number)?.toLong()
        val defaults = WorkSchedule()
        val schedule = WorkSchedule(
            startMinute = int("start") ?: defaults.startMinute,
            endMinute = int("end") ?: defaults.endMinute,
            lunchStartMinute = int("lunchStart") ?: defaults.lunchStartMinute,
            lunchMinutes = int("lunchMinutes") ?: defaults.lunchMinutes,
            daysOff = int("daysOff")?.let(::bitsToDaysOff) ?: defaults.daysOff,
        )
        return WorkData(
            // a damaged or old value never breaks the app: an invalid schedule falls back to the default
            schedule = if (schedule.problem() == null) schedule else defaults,
            configured = m["configured"] as? Boolean ?: false,
            demoMode = m["demo"] as? Boolean ?: false,
            demoRealSec = long("demoReal") ?: 0,
            demoGameSec = long("demoGame") ?: 0,
            log = WorkLog(long("logDay") ?: -1, int("clockIn"), int("clockOut")),
            calls = CallBook.decode(m["calls"] as? String) ?: CallBook(-1),
            nextCalls = CallBook.decode(m["nextCalls"] as? String) ?: CallBook(-1),
            notificationsAsked = m["notifAsked"] as? Boolean ?: false,
            week = WeekTracker(
                long("weekIndex") ?: -1,
                WeekSnapshot(int("weekSolved") ?: 0, int("weekHearts") ?: 0, int("weekXp") ?: 0),
                long("weekPaid") ?: -1,
            ),
        )
    }
}
