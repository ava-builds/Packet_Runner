package com.avabuilds.packetrunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.avabuilds.packetrunner.data.AuthRepository
import com.avabuilds.packetrunner.data.FirebaseAuthRepository
import com.avabuilds.packetrunner.data.FirestoreSaveRepository
import com.avabuilds.packetrunner.data.SaveRepository
import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.data.UserProfile
import com.avabuilds.packetrunner.ui.GameScreen
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.SaveGate
import com.avabuilds.packetrunner.ui.auth.AuthScreen
import com.avabuilds.packetrunner.ui.auth.FirebaseNotConfiguredScreen
import com.avabuilds.packetrunner.ui.theme.PacketRunnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val firebaseReady = FirebaseAuthRepository.isConfigured(this)
        val auth: AuthRepository? = if (firebaseReady) FirebaseAuthRepository() else null
        val saves: SaveRepository? = if (firebaseReady) FirestoreSaveRepository() else null
        setContent {
            PacketRunnerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PR.Bg,
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    var user by remember { mutableStateOf(auth?.currentUser) }
                    var playingWithoutAccount by remember { mutableStateOf(false) }
                    when {
                        auth == null && !playingWithoutAccount ->
                            FirebaseNotConfiguredScreen(onPlayWithoutAccount = { playingWithoutAccount = true }, modifier = modifier)
                        auth != null && user == null ->
                            AuthScreen(auth, onSignedIn = { user = it }, modifier = modifier)
                        // a new game screen for every account, so one player never sees another's town
                        else -> key(user?.uid) {
                            val signedIn = user
                            val game: @Composable (SaveData?) -> Unit = { save ->
                                GameScreen(
                                    user = signedIn,
                                    auth = auth,
                                    saves = saves,
                                    initialSave = save,
                                    onUserChanged = { user = it },
                                    onLoggedOut = { user = null },
                                    modifier = modifier,
                                )
                            }
                            if (signedIn != null && saves != null) {
                                SaveGate(signedIn.uid, saves, onLogOut = { auth?.signOut(); user = null }, modifier = modifier, content = game)
                            } else {
                                game(null)
                            }
                        }
                    }
                }
            }
        }
    }
}
