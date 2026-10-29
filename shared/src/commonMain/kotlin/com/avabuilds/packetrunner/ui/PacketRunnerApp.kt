package com.avabuilds.packetrunner.ui

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
import com.avabuilds.packetrunner.data.CloudBackup
import com.avabuilds.packetrunner.data.LOCAL_UID
import com.avabuilds.packetrunner.data.NotebookRepository
import com.avabuilds.packetrunner.data.SaveRepository
import com.avabuilds.packetrunner.platform.PlatformServices
import com.avabuilds.packetrunner.ui.theme.PacketRunnerTheme

/**
 * The whole game as one composable, the same on every platform. Each platform builds its [PlatformServices] and its save
 * and notebook storage and hands them in. [cloud] is the optional online backup (only where Firebase exists).
 */
@Composable
fun PacketRunnerApp(
    services: PlatformServices,
    localSaves: SaveRepository,
    notebook: NotebookRepository,
    cloud: CloudBackup? = null,
    /** The call the player tapped in a notification, until the game has dealt with it. */
    openCallId: Int? = null,
    onCallOpened: () -> Unit = {},
) {
    PacketRunnerTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PR.Bg,
        ) { innerPadding ->
            var user by remember { mutableStateOf(cloud?.auth?.currentUser) }
            // Restoring a backup changes the game on the device and then loads it again, so the key changes
            var gameKey by remember { mutableIntStateOf(0) }
            key(gameKey) {
                SaveGate(LOCAL_UID, localSaves, modifier = Modifier.padding(innerPadding)) { save ->
                    GameScreen(
                        services = services,
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
                        onCallOpened = onCallOpened,
                        modifier = Modifier,
                        contentPadding = innerPadding,
                    )
                }
            }
        }
    }
}
