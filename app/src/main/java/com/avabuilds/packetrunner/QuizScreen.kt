package com.avabuilds.packetrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Feedback(val text: String, val isError: Boolean = false)

/**
 * Gameplay for worlds 1, 2, 4 and 5. For each puzzle the player reads the scenario board
 * and picks the right answer. A wrong pick costs a life; the right pick shows why.
 */
@Composable
fun QuizScreen(level: QuizLevel, onFinish: (LevelResult) -> Unit, onQuit: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    val mistakes = remember { mutableStateListOf<Mistake>() }
    var feedback by remember { mutableStateOf(Feedback("Read the board, then pick the answer.")) }
    var solved by remember(index) { mutableStateOf(false) }
    val tried = remember(index) { mutableStateListOf<String>() }

    val question = level.questions[index]
    val options = remember(index) { (question.wrong + question.answer).shuffled() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onQuit) { Text("✕", fontSize = 18.sp, color = PR.Muted) }
            Column(Modifier.weight(1f)) {
                Text("World ${level.world} · ${level.hudName}", fontSize = 12.sp, color = PR.Muted)
                Text(
                    "Puzzle ${index + 1} of ${level.questions.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("❤".repeat(lives) + "♡".repeat(3 - lives), fontSize = 16.sp, color = PR.Red)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(PR.Board, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Scenario board", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PR.Amber)
            Text(
                question.scenario,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = PR.BoardText,
            )
        }

        Panel(color = PR.Purple50) {
            Text(question.prompt, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            options.forEach { option ->
                val isRight = solved && option == question.answer
                val isWrong = option in tried
                Text(
                    option,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isRight -> PR.Green800
                        isWrong -> PR.Red800
                        else -> PR.Ink
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isRight -> PR.Green50
                                isWrong -> PR.Red50
                                else -> PR.White
                            },
                            RoundedCornerShape(12.dp),
                        )
                        .border(1.dp, PR.Line, RoundedCornerShape(12.dp))
                        .clickable(enabled = !solved && !isWrong) {
                            if (option == question.answer) {
                                solved = true
                                feedback = Feedback("Correct! " + question.explanation)
                            } else {
                                tried.add(option)
                                mistakes.add(
                                    Mistake(
                                        question = question.prompt,
                                        yourAnswer = option,
                                        correctAnswer = question.answer,
                                        explanation = question.explanation,
                                    ),
                                )
                                lives -= 1
                                feedback = Feedback("Not quite. Look at the board again.", isError = true)
                                if (lives <= 0) {
                                    onFinish(LevelResult(cleared = false, stars = 0, mistakes = mistakes.toList()))
                                }
                            }
                        }
                        .padding(14.dp),
                )
            }
        }

        if (solved) {
            val last = index == level.questions.lastIndex
            PrimaryButton(if (last) "Finish mission" else "Next puzzle →") {
                if (last) {
                    onFinish(
                        LevelResult(
                            cleared = true,
                            stars = RouterStationLevel.starsFor(mistakes.size),
                            mistakes = mistakes.toList(),
                        ),
                    )
                } else {
                    feedback = Feedback("Here comes the next puzzle!")
                    index += 1
                }
            }
        }

        NanaSays(feedback.text, feedback.isError)
    }
}
