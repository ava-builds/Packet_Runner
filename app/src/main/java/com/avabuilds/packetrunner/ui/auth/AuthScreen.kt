package com.avabuilds.packetrunner.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.AuthOutcome
import com.avabuilds.packetrunner.data.AuthRepository
import com.avabuilds.packetrunner.data.AuthValidation
import com.avabuilds.packetrunner.data.UserProfile
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.PrimaryButton

/** Log in or sign up. Everything typed is checked here first; Firebase only gets valid text. */
@Composable
fun AuthScreen(auth: AuthRepository, onSignedIn: (UserProfile) -> Unit, modifier: Modifier = Modifier) {
    var signingUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        error = AuthValidation.email(email)
            ?: AuthValidation.password(password)
            ?: if (signingUp) AuthValidation.nickname(nickname) else null
        if (error != null) return
        busy = true
        val finished: (AuthOutcome) -> Unit = { outcome ->
            busy = false
            when (outcome) {
                is AuthOutcome.Success -> outcome.user?.let(onSignedIn)
                is AuthOutcome.Failure -> error = outcome.message
            }
        }
        if (signingUp) auth.signUp(email, password, nickname, finished) else auth.signIn(email, password, finished)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Network Town", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
        Text("Be the town's network engineer.", fontSize = 14.sp, color = PR.Muted, modifier = Modifier.padding(bottom = 16.dp))
        Panel(color = PR.White, borderColor = Palette5.PersianBlue) {
            Text(if (signingUp) "Create your account" else "Log in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (signingUp) {
                OutlinedTextField(nickname, { nickname = it }, Modifier.fillMaxWidth(), label = { Text("Nickname") }, singleLine = true)
            }
            OutlinedTextField(
                email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            OutlinedTextField(
                password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            error?.let { Text(it, fontSize = 13.sp, color = PR.Red800) }
            PrimaryButton(if (busy) "Please wait…" else if (signingUp) "Sign up" else "Log in", onClick = { if (!busy) submit() })
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (signingUp) "I already have an account · Log in" else "New here? Create an account",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Palette5.PersianBlue,
                    modifier = Modifier.clickable { signingUp = !signingUp; error = null }.padding(8.dp),
                )
            }
        }
    }
}

/** Shown when google-services.json is missing, with what to do and a way to keep testing. */
@Composable
fun FirebaseNotConfiguredScreen(onPlayWithoutAccount: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Panel(color = PR.White, borderColor = PR.Amber) {
            Text("Firebase is not set up yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Add google-services.json to the app folder (see the steps in the README), then run the app again. " +
                    "Until then you can play without an account, but nothing is saved.",
                fontSize = 14.sp, lineHeight = 20.sp,
            )
            PrimaryButton("Play without an account", onClick = onPlayWithoutAccount)
        }
    }
}
