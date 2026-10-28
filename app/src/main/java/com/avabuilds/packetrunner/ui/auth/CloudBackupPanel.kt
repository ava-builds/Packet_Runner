package com.avabuilds.packetrunner.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.CloudBackup
import com.avabuilds.packetrunner.data.CloudCopy
import com.avabuilds.packetrunner.data.LOCAL_UID
import com.avabuilds.packetrunner.data.NotebookRepository
import com.avabuilds.packetrunner.data.RestoreResult
import com.avabuilds.packetrunner.data.UserProfile
import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

/**
 * The optional "Cloud backup" in the profile. The game is already saved on the phone; here the player can log in
 * to copy it to their account and bring it back on another phone. The caller shows this only when Firebase is set up.
 */
@Composable
fun CloudBackupPanel(
    cloud: CloudBackup,
    user: UserProfile?,
    currentSave: SaveData,
    notebook: NotebookRepository,
    onUserChanged: (UserProfile) -> Unit,
    onLoggedOut: () -> Unit,
    onRestore: (CloudCopy) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (user == null) {
        var loggingIn by remember { mutableStateOf(false) }
        Panel(modifier, color = PR.White, borderColor = PR.Line) {
            Text("☁ Cloud backup (optional)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Your town is already saved on this phone. Log in if you also want a copy online, to keep it safe or to play on another phone.",
                fontSize = 12.sp, color = PR.Muted,
            )
            if (loggingIn) AuthForm(cloud.auth, onSignedIn = { loggingIn = false; onUserChanged(it) })
            else SecondaryButton("Log in or sign up", onClick = { loggingIn = true })
        }
        return
    }

    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmingRestore by remember { mutableStateOf(false) }

    Panel(modifier, color = PR.White, borderColor = Palette5.PersianBlue) {
        Text("☁ Cloud backup", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Your town and notebook are copied to ${user.email}. The copy on this phone is the one you play.", fontSize = 12.sp, color = PR.Muted)
        PrimaryButton(if (busy) "Please wait…" else "Back up now") {
            if (!busy) {
                busy = true
                message = null
                notebook.list(LOCAL_UID) { notes ->
                    cloud.backUp(user.uid, currentSave, notes.getOrDefault(emptyList())) { ok ->
                        busy = false
                        message = if (ok) "Backed up." else "Could not back up. Check your internet connection."
                    }
                }
            }
        }
        if (confirmingRestore) {
            Text(
                "This replaces the town and notes on this phone with the backed-up copy. Continue?",
                fontSize = 13.sp, color = PR.Red800,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Cancel", Modifier.weight(1f)) { confirmingRestore = false }
                PrimaryButton("Restore", Modifier.weight(1f)) {
                    if (!busy) {
                        busy = true
                        confirmingRestore = false
                        cloud.restore(user.uid) { result ->
                            busy = false
                            when (result) {
                                is RestoreResult.Found -> result.copy.save?.let { onRestore(result.copy) } ?: run { message = "There is no backed-up town yet." }
                                RestoreResult.NothingBackedUp -> message = "Nothing is backed up yet."
                                is RestoreResult.Failed -> message = result.message
                            }
                        }
                    }
                }
            }
        } else {
            SecondaryButton("Restore from cloud") { confirmingRestore = true; message = null }
        }
        message?.let { Text(it, fontSize = 13.sp, color = PR.Ink) }
    }
    com.avabuilds.packetrunner.ui.auth.AccountPanel(user, cloud.auth, cloud::deleteBackup, onUserChanged, onLoggedOut, onClose, Modifier)
}
