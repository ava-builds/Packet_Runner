package com.avabuilds.packetrunner

import android.content.Intent
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.avabuilds.packetrunner.data.FirebaseAuthRepository
import com.avabuilds.packetrunner.data.FirestoreNotebookRepository
import com.avabuilds.packetrunner.data.CloudBackup
import com.avabuilds.packetrunner.data.FirestoreSaveRepository
import com.avabuilds.packetrunner.data.LOCAL_UID
import com.avabuilds.packetrunner.data.LocalNotebookRepository
import com.avabuilds.packetrunner.data.LocalSaveRepository
import com.avabuilds.packetrunner.notify.AppVisibility
import com.avabuilds.packetrunner.notify.CallNotifications
import com.avabuilds.packetrunner.ui.GameScreen
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.SaveGate
import com.avabuilds.packetrunner.ui.theme.PacketRunnerTheme

class MainActivity : ComponentActivity() {
    /** The call the player opened from a notification, until the game has dealt with it. */
    private var openCallId by mutableStateOf<Int?>(null)

    override fun onStart() {
        super.onStart()
        AppVisibility.inForeground = true
    }

    override fun onStop() {
        AppVisibility.inForeground = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openCallId = intent.getIntExtra(CallNotifications.EXTRA_CALL_ID, -1).takeIf { it >= 0 }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallNotifications.createChannel(this)
        openCallId = intent?.getIntExtra(CallNotifications.EXTRA_CALL_ID, -1)?.takeIf { it >= 0 }
        enableEdgeToEdge()
        // The game and the notebook live on the phone. The cloud backup exists only when google-services.json is present.
        val localSaves = LocalSaveRepository(this)
        val notebook = LocalNotebookRepository(this)
        val cloud: CloudBackup? = if (FirebaseAuthRepository.isConfigured(this)) {
            CloudBackup(FirebaseAuthRepository(), FirestoreSaveRepository(), FirestoreNotebookRepository())
        } else {
            null
        }
        setContent {
            PacketRunnerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PR.Bg,
                ) { innerPadding ->
                    var user by remember { mutableStateOf(cloud?.auth?.currentUser) }
                    // Restoring a backup changes the game on the phone and then loads it again, so the key changes
                    var gameKey by remember { mutableIntStateOf(0) }
                    key(gameKey) {
                        SaveGate(LOCAL_UID, localSaves, modifier = Modifier.padding(innerPadding)) { save ->
                            GameScreen(
                                localSaves = localSaves,
                                notebook = notebook,
                                cloud = cloud,
                                user = user,
                                initialSave = save,
                                onUserChanged = { user = it },
                                onLoggedOut = { user = null },
                                onRestore = { copy ->
                                    val restored = copy.save ?: return@GameScreen
                                    localSaves.save(LOCAL_UID, restored) {
                                        notebook.replaceAll(LOCAL_UID, copy.notes) { gameKey++ }
                                    }
                                },
                                openCallId = openCallId,
                                onCallOpened = { openCallId = null },
                                modifier = Modifier,
                                contentPadding = innerPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}
