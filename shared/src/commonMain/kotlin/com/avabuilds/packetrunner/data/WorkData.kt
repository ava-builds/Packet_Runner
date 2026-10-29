package com.avabuilds.packetrunner.data

import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.ScaledTimeSource
import com.avabuilds.packetrunner.work.TimeSource
import com.avabuilds.packetrunner.work.WallTime
import com.avabuilds.packetrunner.work.WeekTracker
import com.avabuilds.packetrunner.work.Weekday
import com.avabuilds.packetrunner.work.WorkLog
import com.avabuilds.packetrunner.work.WorkSchedule

/** Where the work data is kept: DataStore on Android, the browser's local storage on the web. */
interface WorkDataStore {
    suspend fun load(): WorkData
    suspend fun save(data: WorkData)
}

/** Everything about the work life that has to survive closing the app. */
data class WorkData(
    val schedule: WorkSchedule = WorkSchedule(),
    /** False until the player has chosen their work week on first launch. */
    val configured: Boolean = false,
    val demoMode: Boolean = false,
    /** While demo mode is on: the real time (total seconds) and the game time it started from. */
    val demoRealSec: Long = 0,
    val demoGameSec: Long = 0,
    val log: WorkLog = WorkLog(-1),
    /** Today's customer calls, so they survive closing the app. */
    val calls: CallBook = CallBook(-1),
    /** The plan for the next workday, made ahead so notifications can be set up while the app is closed. */
    val nextCalls: CallBook = CallBook(-1),
    /** True once the player has been asked about notifications, so we ask only once. */
    val notificationsAsked: Boolean = false,
    /** The week in progress, for the Friday payday. */
    val week: WeekTracker = WeekTracker(),
)

/** The clock this data describes: the real one, or the faster demo clock when demo mode is on. */
fun WorkData.timeSource(real: TimeSource): TimeSource =
    if (demoMode) ScaledTimeSource(
        real,
        WallTime(demoRealSec.floorDiv(86_400L), demoRealSec.mod(86_400L).toInt()),
        WallTime(demoGameSec.floorDiv(86_400L), demoGameSec.mod(86_400L).toInt()),
    ) else real

/** Turns a set of days off into a number (bit 0 = Monday) and back, for storing. */
fun daysOffToBits(days: Set<Weekday>): Int = days.fold(0) { acc, d -> acc or (1 shl d.ordinal) }
fun bitsToDaysOff(bits: Int): Set<Weekday> = Weekday.entries.filter { bits and (1 shl it.ordinal) != 0 }.toSet()
