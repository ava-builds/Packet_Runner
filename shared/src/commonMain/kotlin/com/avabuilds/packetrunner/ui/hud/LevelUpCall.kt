package com.avabuilds.packetrunner.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.game.Chime
import com.avabuilds.packetrunner.game.LevelUpEvent
import com.avabuilds.packetrunner.platform.SoundPlayer
import com.avabuilds.packetrunner.ui.Nana
import com.avabuilds.packetrunner.ui.NanaSays
import com.avabuilds.packetrunner.ui.PR
import com.avabuilds.packetrunner.ui.Palette5
import com.avabuilds.packetrunner.ui.Panel
import com.avabuilds.packetrunner.ui.Pill
import com.avabuilds.packetrunner.ui.PrimaryButton

/**
 * The level-up moment: Nana phones to say well done. The player answers, Nana tells what is new and how much the bonus is,
 * and the player thanks her. A short chime plays when the phone rings. The coins of the bonus were already added to the money.
 */
@Composable
fun LevelUpCall(event: LevelUpEvent, sound: SoundPlayer, onThanks: () -> Unit, modifier: Modifier = Modifier) {
    var answered by remember(event.level) { mutableStateOf(false) }
    LaunchedEffect(event.level) { sound.play(Chime.levelUp) }

    Box(modifier.fillMaxSize().background(Color(0x99091540)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Panel(Modifier.padding(20.dp), color = PR.White, borderColor = Palette5.PersianBlue) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉 Level ${event.level}!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
                if (!answered) {
                    Nana(size = 96.dp)
                    Text("📞 Nana is calling…", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Palette5.PersianBlue)
                    PrimaryButton("Answer") { answered = true }
                } else {
                    NanaSays(event.message)
                    Panel(color = PR.Blue50, borderColor = PR.Line) {
                        Text("What is new", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        event.whatsNew.forEach { Text("• $it", fontSize = 13.sp, color = PR.Ink) }
                    }
                    Pill("+₩${event.bonus} level-up bonus", PR.Amber50, PR.Amber800)
                    PrimaryButton("Thanks, Nana!", onClick = onThanks)
                }
            }
        }
    }
}
