package com.avabuilds.packetrunner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avabuilds.packetrunner.data.FirebaseAuthRepository
import com.avabuilds.packetrunner.data.FirestoreNotebookRepository
import com.avabuilds.packetrunner.data.CloudBackup
import com.avabuilds.packetrunner.data.FirestoreSaveRepository
import com.avabuilds.packetrunner.data.LocalNotebookRepository
import com.avabuilds.packetrunner.data.LocalSaveRepository
import com.avabuilds.packetrunner.notify.AppVisibility
import com.avabuilds.packetrunner.platform.androidServices
import com.avabuilds.packetrunner.notify.CallNotifications
import com.avabuilds.packetrunner.ui.PacketRunnerApp

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
        val services = androidServices(this)
        val localSaves = LocalSaveRepository(this)
        val notebook = LocalNotebookRepository(this)
        val cloud: CloudBackup? = if (FirebaseAuthRepository.isConfigured(this)) {
            CloudBackup(FirebaseAuthRepository(), FirestoreSaveRepository(), FirestoreNotebookRepository())
        } else {
            null
        }
        setContent {
            PacketRunnerApp(
                services = services,
                localSaves = localSaves,
                notebook = notebook,
                cloud = cloud,
                openCallId = openCallId,
                onCallOpened = { openCallId = null },
            )
        }
    }
}
