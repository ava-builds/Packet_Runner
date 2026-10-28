package com.avabuilds.packetrunner.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.CloudBackup
import com.avabuilds.packetrunner.data.CloudCopy
import com.avabuilds.packetrunner.data.NotebookRepository
import com.avabuilds.packetrunner.data.UserProfile
import com.avabuilds.packetrunner.game.GameState
import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.game.Neighbor
import com.avabuilds.packetrunner.game.Progress
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.game.Upgrades
import com.avabuilds.packetrunner.ui.FriendsPanel
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.auth.CloudBackupPanel

/**
 * The profile: who you are (face, nickname, level and XP), how the game is going (day, money, jobs done, upgrades),
 * your friends and their hearts, and the optional cloud backup.
 */
@Composable
fun ProfileScreen(
    state: GameState,
    neighbors: List<Neighbor>,
    user: UserProfile?,
    cloud: CloudBackup?,
    currentSave: SaveData,
    notebook: NotebookRepository,
    onUserChanged: (UserProfile) -> Unit,
    onLoggedOut: () -> Unit,
    onRestore: (CloudCopy) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onWorkSettings: () -> Unit = {},
) {
    val progress = Progress.progress(state.xp)
    Box(modifier.fillMaxSize().background(PR.Bg).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EngineerAvatar(size = 64.dp)
                    Column(Modifier.weight(1f)) {
                        Text(user?.nickname ?: "Network engineer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                        Text("Level ${progress.level}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Palette5.PersianBlue)
                        Text("${progress.xpIntoLevel} / ${progress.xpForLevel} XP to level ${progress.level + 1}", fontSize = 11.sp, color = PR.Muted)
                    }
                }
            }

            Panel(color = PR.White, borderColor = PR.Line) {
                Text("How it is going", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                StatRow("Day", "${state.day}")
                StatRow("Money", "₩${state.money}")
                StatRow("XP", "${state.xp}")
                StatRow("Jobs solved", "${state.solved.size}")
                RequestType.entries.forEach { type ->
                    val n = state.solved.count { it.type == type }
                    if (n > 0) StatRow("  ${type.label}", "$n")
                }
                Upgrades.all.forEach { u -> StatRow(u.title, "level ${state.upgrades[u.id] ?: 0} of ${u.maxLevel}") }
            }

            com.avabuilds.packetrunner.ui.SecondaryButton("⚙ Work settings and demo mode", onClick = onWorkSettings)

            FriendsPanel(neighbors, state)

            // The cloud backup is optional and only offered when Firebase is set up
            if (cloud != null) {
                CloudBackupPanel(cloud, user, currentSave, notebook, onUserChanged, onLoggedOut, onRestore, onClose)
            } else {
                Panel(color = PR.White, borderColor = PR.Line) {
                    Text("Saved on this phone", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Your town and notebook are kept on this phone, so you can play with no internet.", fontSize = 12.sp, color = PR.Muted)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = PR.Muted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
    }
}
