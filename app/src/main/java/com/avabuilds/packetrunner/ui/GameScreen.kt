package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avabuilds.packetrunner.data.AuthRepository
import com.avabuilds.packetrunner.data.NoteDraft
import com.avabuilds.packetrunner.data.NoteValidation
import com.avabuilds.packetrunner.data.NotebookRepository
import com.avabuilds.packetrunner.data.SaveRepository
import com.avabuilds.packetrunner.data.UserProfile
import com.avabuilds.packetrunner.game.BuildingType
import com.avabuilds.packetrunner.game.FlagOutcome
import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.NetworkMonitor
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.game.RequestGenerator
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.game.Town
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.network.LabGame
import com.avabuilds.packetrunner.network.RoutingPuzzle
import com.avabuilds.packetrunner.network.RoutingPuzzles
import com.avabuilds.packetrunner.network.Scenario
import com.avabuilds.packetrunner.network.Scenarios
import com.avabuilds.packetrunner.ui.auth.AccountPanel
import com.avabuilds.packetrunner.ui.lab.CrcGame
import com.avabuilds.packetrunner.ui.lab.EncodingGame
import com.avabuilds.packetrunner.ui.lab.LabGameShell
import com.avabuilds.packetrunner.ui.lab.LabMenu
import com.avabuilds.packetrunner.ui.lab.WindowGame
import com.avabuilds.packetrunner.ui.notebook.NotebookPanel
import com.avabuilds.packetrunner.ui.scene.RoutingScreen
import com.avabuilds.packetrunner.ui.scene.SceneScreen
import kotlinx.coroutines.delay

/** The notebook owner when nobody is logged in (playing without an account). */
private const val LOCAL_UID = "local"

/**
 * The whole game screen: the town, plus the day clock, the ringing phone and the job cards on top.
 * All the rules live in [GameEngine]; this screen shows the state and sends the player's choices to it.
 */
@Composable
fun GameScreen(
    user: UserProfile?,
    auth: AuthRepository?,
    saves: SaveRepository?,
    notebook: NotebookRepository,
    initialSave: SaveData?,
    onUserChanged: (UserProfile) -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val town = remember { Town.default() }
    val neighbors = remember { Neighbors.default() }
    val generator = remember { RequestGenerator(seed = 2026L) } // fixed seed so every run is the same while we build
    val controller = remember { TownController(town) }
    var state by remember {
        mutableStateOf(if (initialSave != null) GameEngine.restore(initialSave, neighbors, generator) else GameEngine.newGame(neighbors, generator))
    }
    var showFriends by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var scenario by remember { mutableStateOf<Scenario?>(null) } // the room the player is working in, if any
    var saveMark by remember { mutableStateOf("") } // cloud mark: ... while saving, a tick when stored, ! when it failed
    var showNotebook by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var routing by remember { mutableStateOf<RoutingPuzzle?>(null) } // the City Hall router job, if open
    var labMenuOpen by remember { mutableStateOf(false) }
    var labGame by remember { mutableStateOf<LabGame?>(null) }
    var monitorOpen by remember { mutableStateOf(false) }
    var monitorMessage by remember { mutableStateOf<String?>(null) }

    // The clock: time moves forward in small steps, measured against the real clock
    LaunchedEffect(Unit) {
        var last = System.nanoTime()
        while (true) {
            delay(200)
            val now = System.nanoTime()
            // The day clock waits while the player is inside a room, working
            if (scenario == null && routing == null && !labMenuOpen && labGame == null) state = GameEngine.advance(state, (now - last) / 1_000_000_000f)
            last = now
        }
    }
    // Autosave: whenever something that lasts changes (not the clock), store it a moment later
    val save by remember { derivedStateOf { SaveData.from(state) } }
    if (user != null && saves != null) {
        LaunchedEffect(save) {
            delay(600)
            saveMark = "☁…"
            saves.save(user.uid, save) { ok -> saveMark = if (ok) "☁✓" else "☁!" }
        }
    }
    fun saveConceptsAsNote(title: String, concepts: List<Concept>) {
        val body = concepts.joinToString("\n\n") { "${it.en} (${it.ko}): ${it.explanation}" }.take(NoteValidation.BODY_MAX)
        notebook.add(user?.uid ?: LOCAL_UID, NoteDraft(title, body, concepts.firstOrNull()?.en.orEmpty()), System.currentTimeMillis()) { saved ->
            toast = if (saved != null) "Saved to your notebook" else "Could not save the note"
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    val active = state.active
    val ringing = state.ringing
    val atTarget = active != null && controller.arrivedAt?.id == active.buildingId

    Box(modifier.fillMaxSize()) {
        IsoTownScreen(controller, targetBuildingId = active?.buildingId)

        // Top strip: title, day, money and friends
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text("Network Town", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PR.Ink)
            StatusPills(
                state,
                onFriends = { showFriends = !showFriends },
                onSkipDay = { state = GameEngine.skipToEvening(state) },
                playerName = user?.let { "${it.nickname} $saveMark".trim() },
                onAccount = { showAccount = !showAccount },
                onNotebook = { showNotebook = !showNotebook; if (showNotebook) monitorOpen = false },
            )
            toast?.let { Text(it, fontSize = 13.sp, color = PR.Green800, modifier = Modifier.padding(top = 6.dp)) }
            if (showAccount && user != null && auth != null) {
                AccountPanel(user, auth, saves, notebook, onUserChanged, onLoggedOut, onClose = { showAccount = false }, modifier = Modifier.padding(top = 8.dp))
            }
            if (showFriends) FriendsPanel(neighbors, state, Modifier.padding(top = 8.dp))
        }

        // Bottom: the ringing phone, or the job in progress
        Column(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            if (ringing != null) {
                val neighbor = neighbors.first { it.id == ringing.neighborId }
                CallBubble(
                    request = ringing,
                    neighbor = neighbor,
                    hearts = state.hearts[neighbor.id] ?: 0,
                    onAnswer = {
                        state = GameEngine.answer(state)
                        controller.walkToDoorOf(town.building(ringing.buildingId))
                    },
                    onIgnore = { state = GameEngine.ignore(state) },
                )
            } else if (monitorOpen) {
                // the monitor sits above the town; nothing else is needed down here
            } else if (active == null && controller.arrivedAt?.type == BuildingType.OFFICE) {
                OfficeCard(onOpenMonitor = { monitorOpen = true; monitorMessage = null }, onOpenLab = { labMenuOpen = true })
            } else if (active != null) {
                val neighbor = neighbors.first { it.id == active.neighborId }
                val building = town.building(active.buildingId)
                if (atTarget) {
                    ArrivalCard(
                        request = active,
                        neighbor = neighbor,
                        building = building,
                        onStart = {
                            if (active.type == RequestType.ROUTE) {
                                routing = RoutingPuzzles.forJob(state.solved.count { it.type == RequestType.ROUTE })
                            } else {
                                scenario = Scenarios.forRequest(active)
                            }
                        },
                        onGiveUp = {
                            toast = "${neighbor.name} is unhappy. −₩50"
                            state = GameEngine.complete(state, solved = false)
                        },
                    )
                } else {
                    JobBanner(active, neighbor, building)
                }
            }
        }

        // The notebook sits above the town like the monitor
        if (showNotebook && !state.dayOver) {
            NotebookPanel(
                uid = user?.uid ?: LOCAL_UID,
                repo = notebook,
                onClose = { showNotebook = false },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp, start = 12.dp, end = 12.dp),
            )
        }

        // The Network Monitor stays open while the clock runs, so problems can show up in front of the player
        if (monitorOpen && !state.dayOver) {
            NetworkMonitorPanel(
                state = state,
                neighbors = neighbors,
                message = monitorMessage,
                onReport = { neighbor ->
                    val result = NetworkMonitor.report(state, neighbor)
                    state = result.state
                    monitorMessage = when (result.outcome) {
                        FlagOutcome.EARLY_CATCH -> "Caught it early! ${neighbor.name} hasn't even noticed yet. +₩${result.bonus}"
                        FlagOutcome.ALREADY_REPORTED -> "You already reported ${neighbor.name}'s problem. Now be ready to fix it."
                        FlagOutcome.TOO_LATE -> "Too late, ${neighbor.name} already knows about the problem."
                        FlagOutcome.FALSE_ALARM -> "False alarm: ${neighbor.name}'s link is fine. −₩${NetworkMonitor.FALSE_ALARM_COST}"
                        FlagOutcome.NO_LINK -> "${neighbor.name} has no network yet, so there is nothing to watch."
                    }
                },
                onClose = { monitorOpen = false },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp, start = 12.dp, end = 12.dp),
            )
        }

        // The room: the player works on the network here, and the clock waits
        val room = scenario
        if (room != null && active != null) {
            val neighbor = neighbors.first { it.id == active.neighborId }
            SceneScreen(
                scenario = room,
                neighborName = neighbor.name,
                reward = active.reward,
                onSaveToNotebook = { title, concepts -> saveConceptsAsNote(title, concepts) },
                onFinish = { success, payout, stars ->
                    scenario = null
                    if (success) {
                        toast = "+₩$payout · ${"★".repeat(stars)} · ${neighbor.name} is happier"
                        state = GameEngine.complete(state, solved = true, payout = payout, bonusHearts = if (stars == 3) 1 else 0)
                    } else {
                        toast = "${neighbor.name} is unhappy. −₩50"
                        state = GameEngine.complete(state, solved = false)
                    }
                },
            )
        }

        // The Lab: a menu of three small games; the clock waits while the player is in it
        val playing = labGame
        if (playing != null) {
            val paysToday = playing.id !in state.labDone
            LabGameShell(
                game = playing,
                paysToday = paysToday,
                onQuit = { labGame = null },
                onFinish = {
                    if (paysToday) {
                        state = GameEngine.completeLab(state, playing.id, playing.reward)
                        toast = "+₩${playing.reward} from the Lab"
                    }
                    labGame = null
                },
                onSaveToNotebook = { title, concepts -> saveConceptsAsNote(title, concepts) },
            ) { onSuccess ->
                when (playing) {
                    LabGame.CRC -> CrcGame(onSuccess)
                    LabGame.ENCODING -> EncodingGame(onSuccess)
                    LabGame.WINDOW -> WindowGame(onSuccess)
                }
            }
        } else if (labMenuOpen) {
            LabMenu(doneToday = state.labDone, onPick = { labGame = it }, onClose = { labMenuOpen = false })
        }

        val puzzle = routing
        if (puzzle != null && active != null) {
            val neighbor = neighbors.first { it.id == active.neighborId }
            RoutingScreen(
                puzzle = puzzle,
                clerkName = neighbor.name,
                reward = active.reward,
                onSaveToNotebook = { title, concepts -> saveConceptsAsNote(title, concepts) },
                onFinish = { success, payout, stars ->
                    routing = null
                    if (success) {
                        toast = "+₩$payout · ${"★".repeat(stars)} · ${neighbor.name} is happier"
                        state = GameEngine.complete(state, solved = true, payout = payout, bonusHearts = if (stars == 3) 1 else 0)
                    } else {
                        toast = "${neighbor.name} is unhappy. −₩50"
                        state = GameEngine.complete(state, solved = false)
                    }
                },
            )
        }

        if (state.dayOver) {
            DayEndCard(state, onNextDay = {
                controller.teleportTo(town.start) // every morning starts at the office
                state = GameEngine.nextDay(state, neighbors, generator)
            })
        }
    }
}
