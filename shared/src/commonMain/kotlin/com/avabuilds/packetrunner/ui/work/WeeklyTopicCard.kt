package com.avabuilds.packetrunner.ui.work

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton
import com.avabuilds.packetrunner.work.Practice
import com.avabuilds.packetrunner.work.WeeklyTopic

/**
 * The Weekly topic: this week's idea from the course, in English and Korean with three key points and where the book
 * covers it, and a button to practise it in the game. Reading it for the first time each day gives a little XP.
 */
@Composable
fun WeeklyTopicScreen(
    topic: WeeklyTopic,
    weekNumber: Long,
    claimedToday: Boolean,
    onPractice: (Practice) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📚 Weekly topic", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                    Text("Computer Communication · week ${weekNumber % 100}", fontSize = 13.sp, color = PR.Muted)
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette5.PersianBlue, fontSize = 14.sp) }
            }
            Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                Text(topic.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                Text(topic.korean, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Palette5.PersianBlue)
                Text(topic.summary, fontSize = 13.sp, lineHeight = 19.sp, color = PR.Ink)
            }
            Panel(color = PR.White, borderColor = PR.Line) {
                Text("Key points", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                topic.points.forEachIndexed { i, p -> Text("${i + 1}. $p", fontSize = 13.sp, lineHeight = 19.sp, color = PR.Ink) }
                Text("📖 ${topic.book}", fontSize = 11.sp, color = PR.Muted)
            }
            Panel(color = PR.White, borderColor = PR.Amber) {
                Text("Practise it", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                when (val p = topic.practice) {
                    is Practice.InLab -> {
                        Text("The Lab has a game for this: “${p.game.title}”.", fontSize = 13.sp, color = PR.Muted)
                        PrimaryButton("Open the game") { onPractice(p) }
                    }
                    Practice.OnWorkbench -> {
                        Text("The Workbench calculates masks, networks and broadcast addresses for you.", fontSize = 13.sp, color = PR.Muted)
                        PrimaryButton("Open the Workbench") { onPractice(p) }
                    }
                    is Practice.OnTheJob -> Text(p.hint, fontSize = 13.sp, color = PR.Muted)
                }
            }
            if (claimedToday) Pill("✓ Read today", PR.Green50, PR.Green800) else Text("You earn a little XP the first time you read it today.", fontSize = 12.sp, color = PR.Muted)
            SecondaryButton("Done", onClick = onClose)
        }
    }
}
