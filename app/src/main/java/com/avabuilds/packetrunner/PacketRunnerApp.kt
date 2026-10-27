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
    data class Briefing(val world: Int) : Screen
    data class Play(val world: Int) : Screen
    data class Debrief(val world: Int, val result: LevelResult) : Screen
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
                    onPlayWorld = { screen = Screen.Briefing(it) },
                )

                is Screen.Briefing -> BriefingScreen(
                    briefing = briefingFor(current.world),
                    onStart = { screen = Screen.Play(current.world) },
                    onBack = { screen = Screen.Map },
                )

                is Screen.Play -> {
                    val onFinish = { result: LevelResult ->
                        if (result.cleared) {
                            worldStars[current.world] = maxOf(worldStars[current.world] ?: 0, result.stars)
                        }
                        screen = Screen.Debrief(current.world, result)
                    }
                    val quiz = QuizLevels[current.world]
                    if (quiz != null) {
                        QuizScreen(level = quiz, onFinish = onFinish, onQuit = { screen = Screen.Map })
                    } else {
                        RouterStationScreen(onFinish = onFinish, onQuit = { screen = Screen.Map })
                    }
                }

                is Screen.Debrief -> DebriefScreen(
                    result = current.result,
                    whatReallyHappened = whatReallyHappenedFor(current.world),
                    onRetry = { screen = Screen.Play(current.world) },
                    onMap = { screen = Screen.Map },
                )
            }
        }
    }
}

private fun briefingFor(world: Int): Briefing =
    QuizLevels[world]?.briefing ?: RouterStationLevel.briefing

private fun whatReallyHappenedFor(world: Int): String =
    QuizLevels[world]?.whatReallyHappened ?: RouterStationLevel.WHAT_REALLY_HAPPENED
