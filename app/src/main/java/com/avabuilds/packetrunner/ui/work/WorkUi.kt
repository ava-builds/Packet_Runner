package com.avabuilds.packetrunner.ui.work

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import com.avabuilds.packetrunner.ui.hud.SunMoonIconAt
import com.avabuilds.packetrunner.work.ScaledTimeSource
import com.avabuilds.packetrunner.work.Weekday
import com.avabuilds.packetrunner.work.WorkSchedule
import com.avabuilds.packetrunner.work.WorkState
import com.avabuilds.packetrunner.work.formatMinuteOfDay

private fun WorkState.colors(): Pair<Color, Color> = when (this) {
    WorkState.WORKING -> PR.Green50 to PR.Green800
    WorkState.LUNCH -> PR.Amber50 to PR.Amber800
    WorkState.OFF_DUTY -> PR.Blue50 to PR.Blue800
    WorkState.DAY_OFF -> PR.Purple50 to PR.Purple800
}

/** A small card under the top bar: sun or moon, the real clock, what you are doing now and what comes next. */
@Composable
fun WorkChip(life: WorkLife, modifier: Modifier = Modifier) {
    val (bg, fg) = life.state.colors()
    Row(
        modifier.background(bg, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SunMoonIconAt(life.now.minuteOfDay, Modifier.padding(0.dp), size = 22.dp)
        Column {
            Text(
                "${life.state.label} · ${life.now.weekday.short} ${formatMinuteOfDay(life.now.minuteOfDay)}" + if (life.demoMode) " · DEMO ×${ScaledTimeSource.DEMO_FACTOR}" else "",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg,
            )
            Text(life.nextUp, fontSize = 11.sp, color = fg.copy(alpha = 0.8f))
        }
    }
}

/** The clock in / clock out button shown at the office door. */
@Composable
fun ClockButton(life: WorkLife, modifier: Modifier = Modifier) {
    when {
        life.canClockOut -> SecondaryButton("⏱ Clock out", modifier) { life.clockOut() }
        life.canClockIn -> PrimaryButton("⏱ Clock in", modifier) { life.clockIn() }
    }
}

private fun roundTo30(minute: Int) = (minute / 30) * 30

/**
 * Where the player picks their work week: when work starts and ends, when lunch starts (it lasts one hour),
 * which 2 days are free, and whether Demo mode is on. On first launch it is the setup screen.
 */
@Composable
fun WorkSettingsScreen(life: WorkLife, firstTime: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier, onRingNow: () -> Unit = {}) {
    var schedule by remember { mutableStateOf(life.schedule) }
    val problem = schedule.problem()
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (firstTime) "Your work week" else "Work settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            Text(
                if (firstTime) "Network Town follows your real clock. Choose when you work, and neighbors will only phone you then."
                else "Neighbors only phone during your work hours.",
                fontSize = 13.sp, color = PR.Muted,
            )

            Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                TimeRow("Work starts", schedule.startMinute) { schedule = schedule.copy(startMinute = it) }
                TimeRow("Work ends", schedule.endMinute) { schedule = schedule.copy(endMinute = it) }
                TimeRow("Lunch starts (1 hour)", schedule.lunchStartMinute) { schedule = schedule.copy(lunchStartMinute = it) }
            }

            Panel(color = PR.White, borderColor = PR.Line) {
                Text("Days off (choose 2)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Weekday.entries.forEach { day ->
                        val off = day in schedule.daysOff
                        Box(Modifier.clickable { schedule = schedule.copy(daysOff = if (off) schedule.daysOff - day else schedule.daysOff + day) }) {
                            Pill(day.short, if (off) Palette5.PersianBlue else PR.Blue50, if (off) PR.White else PR.Blue800)
                        }
                    }
                }
            }

            Panel(color = PR.White, borderColor = PR.Amber) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo mode", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("For presentations: game time runs ${ScaledTimeSource.DEMO_FACTOR} times faster, so a whole work day passes in 9 minutes.", fontSize = 11.sp, color = PR.Muted)
                    }
                    Switch(checked = life.demoMode, onCheckedChange = { life.setDemo(it) })
                }
                if (life.demoMode && !firstTime) {
                    SecondaryButton("📞 Ring a call now (demo)") { onRingNow(); onClose() }
                }
            }

            problem?.let { Text(it, fontSize = 13.sp, color = PR.Red800) }
            PrimaryButton(if (firstTime) "Start working" else "Save") {
                if (problem == null) { life.saveSchedule(schedule); onClose() }
            }
            if (!firstTime) SecondaryButton("Cancel", onClick = onClose)
        }
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.clickable { onChange((roundTo30(minute) - 30).coerceAtLeast(0)) }) { Pill("−") }
        Text(formatMinuteOfDay(minute), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        Box(Modifier.clickable { onChange((roundTo30(minute) + 30).coerceAtMost(23 * 60 + 30)) }) { Pill("+") }
    }
}
