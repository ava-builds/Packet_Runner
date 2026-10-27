package com.avabuilds.packetrunner.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.AuthOutcome
import com.avabuilds.packetrunner.data.AuthRepository
import com.avabuilds.packetrunner.data.AuthValidation
import com.avabuilds.packetrunner.data.SaveRepository
import com.avabuilds.packetrunner.data.UserProfile
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton
import com.avabuilds.packetrunner.ui.SecondaryButton

/** Account settings: change the nickname, log out, or delete the account (after typing the password again). */
@Composable
fun AccountPanel(
    user: UserProfile,
    auth: AuthRepository,
    saves: SaveRepository?,
    onUserChanged: (UserProfile) -> Unit,
    onLoggedOut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nickname by remember { mutableStateOf(user.nickname) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Panel(modifier, color = PR.White, borderColor = Palette5.PersianBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("👤 Your account", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.clickable(onClick = onClose)) { Pill("Close", PR.Blue50, PR.Blue800) }
        }
        Text(user.email, fontSize = 13.sp, color = PR.Muted)

        OutlinedTextField(nickname, { nickname = it }, Modifier.fillMaxWidth(), label = { Text("Nickname") }, singleLine = true)
        PrimaryButton("Save nickname") {
            val problem = AuthValidation.nickname(nickname)
            if (problem != null) {
                message = problem
            } else if (!busy) {
                busy = true
                auth.updateNickname(nickname) { outcome ->
                    busy = false
                    when (outcome) {
                        is AuthOutcome.Success -> { outcome.user?.let(onUserChanged); message = "Nickname saved." }
                        is AuthOutcome.Failure -> message = outcome.message
                    }
                }
            }
        }

        if (confirmingDelete) {
            Text("This deletes your account and cannot be undone. Type your password to confirm.", fontSize = 13.sp, color = PR.Red800)
            OutlinedTextField(
                password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Cancel", Modifier.weight(1f)) { confirmingDelete = false; password = "" }
                PrimaryButton("Delete forever", Modifier.weight(1f)) {
                    if (!busy) {
                        busy = true
                        auth.deleteAccount(password, beforeDelete = { uid, proceed -> saves?.delete(uid, proceed) ?: proceed(true) }) { outcome ->
                            busy = false
                            when (outcome) {
                                is AuthOutcome.Success -> onLoggedOut()
                                is AuthOutcome.Failure -> message = outcome.message
                            }
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Log out", Modifier.weight(1f)) { auth.signOut(); onLoggedOut() }
                SecondaryButton("Delete account", Modifier.weight(1f)) { confirmingDelete = true; message = null }
            }
        }
        message?.let { Text(it, fontSize = 13.sp, color = PR.Ink) }
    }
}
