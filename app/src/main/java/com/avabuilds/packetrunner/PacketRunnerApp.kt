package com.avabuilds.packetrunner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Which screen is showing. */
sealed interface Screen {
    data object Map : Screen
    data object Briefing : Screen
    data object Play : Screen
    data class Debrief(val result: LevelResult) : Screen
}

@Composable
fun PacketRunnerApp(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<Screen>(Screen.Map) }
    // world number -> best stars earned
    val worldStars = remember { mutableStateMapOf<Int, Int>() }

    BackHandler(enabled = screen != Screen.Map) { screen = Screen.Map }

    CompositionLocalProvider(LocalContentColor provides PR.Ink) {
        Box(modifier.fillMaxSize().background(PR.Bg)) {
            when (val current = screen) {
                Screen.Map -> WorldMapScreen(
                    worldStars = worldStars,
                    onPlayWorld = { screen = Screen.Briefing },
                )

                Screen.Briefing -> BriefingScreen(
                    briefing = RouterStationLevel.briefing,
                    onStart = { screen = Screen.Play },
                    onBack = { screen = Screen.Map },
                )

                Screen.Play -> RouterStationScreen(
                    onFinish = { result ->
                        if (result.cleared) {
                            worldStars[3] = maxOf(worldStars[3] ?: 0, result.stars)
                        }
                        screen = Screen.Debrief(result)
                    },
                    onQuit = { screen = Screen.Map },
                )

                is Screen.Debrief -> DebriefScreen(
                    result = current.result,
                    onRetry = { screen = Screen.Play },
                    onMap = { screen = Screen.Map },
                )
            }
        }
    }
}
