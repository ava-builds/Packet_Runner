package com.avabuilds.packetrunner.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Curriculum
import com.avabuilds.packetrunner.game.LevelUp
import com.avabuilds.packetrunner.game.Progress
import com.avabuilds.packetrunner.game.StageStatus
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel

/** What comes next: the level and place that opens, the XP still missing with a bar, and what it brings. */
@Composable
fun NextUnlockCard(xp: Int, modifier: Modifier = Modifier) {
    val next = Curriculum.nextUnlock(xp, LevelUp.labTitles)
    Panel(modifier, color = PR.White, borderColor = if (next == null) PR.Green600 else Palette5.PersianBlue) {
        if (next == null) {
            Text("🏆 Every level is open", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("You have reached every topic of the course. Keep practising: the cases come in every order.", fontSize = 12.sp, color = PR.Muted)
            return@Panel
        }
        val progress = Progress.progress(xp)
        Text("🔓 Next unlock", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Level ${next.stage.level} · ${next.stage.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette5.PersianBlue)
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(PR.GrayFill)) {
            Box(Modifier.fillMaxWidth(progress.fraction.coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette5.PersianBlue))
        }
        Text(next.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PR.Ink)
        Text("${next.xpLeft} XP to go", fontSize = 11.sp, color = PR.Muted)
        next.opens.forEach { Text("• $it", fontSize = 12.sp, color = PR.Ink) }
    }
}

/** The seven levels of the course in order: the ones reached with a tick, the current one marked, the rest locked with their XP. */
@Composable
fun CoursePath(xp: Int, modifier: Modifier = Modifier) {
    Panel(modifier, color = PR.White, borderColor = PR.Line) {
        Text("Course path", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Curriculum.roadmap(xp).forEach { row ->
            val (mark, color) = when (row.status) {
                StageStatus.DONE -> "✓" to PR.Green800
                StageStatus.CURRENT -> "▶" to Palette5.PersianBlue
                StageStatus.LOCKED -> "🔒" to PR.Gray
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(mark, Modifier.padding(top = 1.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                Column(Modifier.weight(1f)) {
                    Text("Level ${row.stage.level} · ${row.stage.title}", fontSize = 13.sp, fontWeight = if (row.status == StageStatus.CURRENT) FontWeight.Bold else FontWeight.Medium, color = if (row.status == StageStatus.LOCKED) PR.Gray else PR.Ink)
                    Text(row.stage.topics.joinToString(", "), fontSize = 11.sp, color = PR.Muted)
                }
                if (row.status == StageStatus.LOCKED) Text("${row.xpToReach} XP", fontSize = 11.sp, color = PR.Muted)
            }
        }
    }
}
