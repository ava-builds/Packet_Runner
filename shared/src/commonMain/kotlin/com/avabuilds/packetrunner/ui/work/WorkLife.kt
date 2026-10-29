package com.avabuilds.packetrunner.ui.work

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.avabuilds.packetrunner.data.WorkData
import com.avabuilds.packetrunner.data.WorkDataStore
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.ScaledTimeSource
import com.avabuilds.packetrunner.work.WeekTracker
import com.avabuilds.packetrunner.work.SystemTimeSource
import com.avabuilds.packetrunner.work.TimeSource
import com.avabuilds.packetrunner.work.WallTime
import com.avabuilds.packetrunner.work.WorkRules
import com.avabuilds.packetrunner.work.WorkSchedule
import com.avabuilds.packetrunner.work.WorkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The work life as the screens see it: the schedule, today's log, and the current time and state. It reads the time
 * from a [TimeSource] (the real clock, or the faster demo clock) and saves every change to the phone.
 */
@Stable
class WorkLife(private val store: WorkDataStore, private val scope: CoroutineScope, private val real: TimeSource = SystemTimeSource()) {
    var loaded by mutableStateOf(false)
        private set
    private var data by mutableStateOf(WorkData())
    var now by mutableStateOf(real.now())
        private set

    val schedule: WorkSchedule get() = data.schedule
    val callBook: CallBook get() = data.calls
    val nextBook: CallBook get() = data.nextCalls
    val week: WeekTracker get() = data.week
    val notificationsAsked: Boolean get() = data.notificationsAsked
    val demoFactor: Int get() = if (data.demoMode) ScaledTimeSource.DEMO_FACTOR else 1
    val needsSetup: Boolean get() = loaded && !data.configured
    val demoMode: Boolean get() = data.demoMode
    val state: WorkState get() = WorkRules.stateAt(data.schedule, data.log, now)
    val nextUp: String get() = WorkRules.nextUp(data.schedule, data.log, now)
    val canClockIn: Boolean get() = WorkRules.canClockIn(data.schedule, data.log, now)
    val canClockOut: Boolean get() = WorkRules.canClockOut(data.schedule, data.log, now)

    private var clock: TimeSource = real

    private fun rebuildClock() {
        clock = if (data.demoMode) {
            ScaledTimeSource(real, secToTime(data.demoRealSec), secToTime(data.demoGameSec))
        } else real
    }

    /** Reads the saved data and clocks in automatically when the app is opened during work hours. */
    suspend fun load() {
        data = store.load()
        rebuildClock()
        now = clock.now()
        data = data.copy(log = WorkRules.autoClockIn(data.schedule, data.log, now))
        loaded = true
        persist()
    }

    /** Moves the clock on. Called about once a second. Also clocks in when a new day's work hours begin while the app is open. */
    fun tick() {
        now = clock.now()
        val log = WorkRules.autoClockIn(data.schedule, data.log, now)
        if (log != data.log && data.configured) { data = data.copy(log = log); persist() }
    }

    fun saveSchedule(schedule: WorkSchedule) {
        data = data.copy(schedule = schedule, configured = true)
        data = data.copy(log = WorkRules.autoClockIn(schedule, data.log, now))
        persist()
    }

    /** Replaces today's call book (after planning, ringing or missing calls) and saves it. */
    fun updateCallBook(book: CallBook) { if (book != data.calls) { data = data.copy(calls = book); persist() } }

    fun updateNextBook(book: CallBook) { if (book != data.nextCalls) { data = data.copy(nextCalls = book); persist() } }

    fun updateWeek(tracker: WeekTracker) { if (tracker != data.week) { data = data.copy(week = tracker); persist() } }

    fun markNotificationsAsked() { if (!data.notificationsAsked) { data = data.copy(notificationsAsked = true); persist() } }

    fun clockIn() { data = data.copy(log = WorkRules.clockIn(data.schedule, data.log, now)); persist() }
    fun clockOut() { data = data.copy(log = WorkRules.clockOut(data.log, now)); persist() }

    /** Demo mode runs 60 times faster, starting from the current time. Switching it off goes back to the real clock. */
    fun setDemo(on: Boolean) {
        if (on == data.demoMode) return
        val current = clock.now()
        data = if (on) data.copy(demoMode = true, demoRealSec = timeToSec(real.now()), demoGameSec = timeToSec(current))
        else data.copy(demoMode = false)
        rebuildClock()
        tick()
        persist()
    }

    private fun persist() { val snapshot = data; scope.launch { store.save(snapshot) } }

    private fun secToTime(total: Long) = WallTime(total.floorDiv(86_400L), total.mod(86_400L).toInt())
    private fun timeToSec(t: WallTime) = t.epochDay * 86_400L + t.secondOfDay
}

/** Creates the [WorkLife] for this screen, loads it, and keeps its clock ticking. */
@Composable
fun rememberWorkLife(store: WorkDataStore): WorkLife {
    val scope = rememberCoroutineScope()
    val life = remember(store) { WorkLife(store, scope) }
    LaunchedEffect(life) {
        life.load()
        while (true) { delay(1000); life.tick() }
    }
    return life
}
