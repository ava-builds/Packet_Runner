package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.LoadResult
import com.avabuilds.packetrunner.data.SaveRepository
import com.avabuilds.packetrunner.game.SaveData

/**
 * Loads the player's save before the game starts, so a new game never overwrites an old one by mistake.
 * While loading it shows a short message; when loading fails it offers Retry and Log out, never "start fresh".
 */
@Composable
fun SaveGate(
    uid: String,
    saves: SaveRepository,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (SaveData?) -> Unit,
) {
    var attempt by remember(uid) { mutableIntStateOf(0) }
    var result by remember(uid) { mutableStateOf<LoadResult?>(null) }
    LaunchedEffect(uid, attempt) {
        result = null
        saves.load(uid) { result = it }
    }

    when (val r = result) {
        null -> Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Loading your town…", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
        }
        is LoadResult.Failed -> Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Panel(color = PR.White, borderColor = PR.Red) {
                Text("Your town could not be loaded", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(r.message, fontSize = 14.sp, lineHeight = 20.sp)
                PrimaryButton("Try again") { attempt++ }
                SecondaryButton("Log out", onClick = onLogOut)
            }
        }
        is LoadResult.Found -> content(r.save)
    }
}
