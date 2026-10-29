package com.avabuilds.packetrunner.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.WeekSnapshot
import com.avabuilds.packetrunner.work.WeekTracker
import com.avabuilds.packetrunner.work.Weekday
import com.avabuilds.packetrunner.work.WorkLog
import com.avabuilds.packetrunner.work.WorkSchedule
import kotlinx.coroutines.flow.first

private val Context.workDataStore by preferencesDataStore(name = "work_life")

/** Keeps the work schedule, today's clock-in log and the demo setting on the phone, with DataStore. */
class WorkStore(private val context: Context) : WorkDataStore {
    private object Keys {
        val start = intPreferencesKey("start")
        val end = intPreferencesKey("end")
        val lunchStart = intPreferencesKey("lunch_start")
        val lunchMinutes = intPreferencesKey("lunch_minutes")
        val daysOff = intPreferencesKey("days_off")
        val configured = booleanPreferencesKey("configured")
        val demo = booleanPreferencesKey("demo")
        val demoReal = longPreferencesKey("demo_real")
        val demoGame = longPreferencesKey("demo_game")
        val logDay = longPreferencesKey("log_day")
        val clockIn = intPreferencesKey("clock_in")
        val clockOut = intPreferencesKey("clock_out")
        val calls = androidx.datastore.preferences.core.stringPreferencesKey("calls")
        val nextCalls = androidx.datastore.preferences.core.stringPreferencesKey("next_calls")
        val notifAsked = booleanPreferencesKey("notif_asked")
        val weekIndex = longPreferencesKey("week_index")
        val weekPaid = longPreferencesKey("week_paid")
        val weekSolved = intPreferencesKey("week_solved")
        val weekHearts = intPreferencesKey("week_hearts")
        val weekXp = intPreferencesKey("week_xp")
    }

    override suspend fun load(): WorkData {
        val p: Preferences = context.workDataStore.data.first()
        val defaults = WorkSchedule()
        val schedule = WorkSchedule(
            startMinute = p[Keys.start] ?: defaults.startMinute,
            endMinute = p[Keys.end] ?: defaults.endMinute,
            lunchStartMinute = p[Keys.lunchStart] ?: defaults.lunchStartMinute,
            lunchMinutes = p[Keys.lunchMinutes] ?: defaults.lunchMinutes,
            daysOff = p[Keys.daysOff]?.let(::bitsToDaysOff) ?: defaults.daysOff,
        )
        return WorkData(
            // a damaged or old value never breaks the app: an invalid schedule falls back to the default
            schedule = if (schedule.problem() == null) schedule else defaults,
            configured = p[Keys.configured] ?: false,
            demoMode = p[Keys.demo] ?: false,
            demoRealSec = p[Keys.demoReal] ?: 0,
            demoGameSec = p[Keys.demoGame] ?: 0,
            log = WorkLog(p[Keys.logDay] ?: -1, p[Keys.clockIn], p[Keys.clockOut]),
            calls = CallBook.decode(p[Keys.calls]) ?: CallBook(-1),
            nextCalls = CallBook.decode(p[Keys.nextCalls]) ?: CallBook(-1),
            notificationsAsked = p[Keys.notifAsked] ?: false,
            week = WeekTracker(
                p[Keys.weekIndex] ?: -1,
                WeekSnapshot(p[Keys.weekSolved] ?: 0, p[Keys.weekHearts] ?: 0, p[Keys.weekXp] ?: 0),
                p[Keys.weekPaid] ?: -1,
            ),
        )
    }

    override suspend fun save(data: WorkData) {
        context.workDataStore.edit { p ->
            p[Keys.start] = data.schedule.startMinute
            p[Keys.end] = data.schedule.endMinute
            p[Keys.lunchStart] = data.schedule.lunchStartMinute
            p[Keys.lunchMinutes] = data.schedule.lunchMinutes
            p[Keys.daysOff] = daysOffToBits(data.schedule.daysOff)
            p[Keys.configured] = data.configured
            p[Keys.demo] = data.demoMode
            p[Keys.demoReal] = data.demoRealSec
            p[Keys.demoGame] = data.demoGameSec
            p[Keys.logDay] = data.log.epochDay
            p[Keys.calls] = data.calls.encode()
            p[Keys.nextCalls] = data.nextCalls.encode()
            p[Keys.notifAsked] = data.notificationsAsked
            p[Keys.weekIndex] = data.week.week
            p[Keys.weekPaid] = data.week.paidWeek
            p[Keys.weekSolved] = data.week.start.solvedCount
            p[Keys.weekHearts] = data.week.start.heartsTotal
            p[Keys.weekXp] = data.week.start.xp
            data.log.clockInMinute?.let { p[Keys.clockIn] = it } ?: p.remove(Keys.clockIn)
            data.log.clockOutMinute?.let { p[Keys.clockOut] = it } ?: p.remove(Keys.clockOut)
        }
    }
}
