package com.avabuilds.packetrunner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.avabuilds.packetrunner.game.Bubbles
import com.avabuilds.packetrunner.game.FlagOutcome
import com.avabuilds.packetrunner.game.GameEngine
import com.avabuilds.packetrunner.game.NetworkMonitor
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.game.Progress
import com.avabuilds.packetrunner.game.RequestGenerator
import com.avabuilds.packetrunner.game.RequestType
import com.avabuilds.packetrunner.game.SaveData
import com.avabuilds.packetrunner.game.Upgrades
import com.avabuilds.packetrunner.game.Town
import com.avabuilds.packetrunner.network.Concept
import com.avabuilds.packetrunner.network.LabGame
import com.avabuilds.packetrunner.network.NatPuzzle
import com.avabuilds.packetrunner.network.NatPuzzles
import com.avabuilds.packetrunner.network.RoutingPuzzle
import com.avabuilds.packetrunner.network.RoutingPuzzles
import com.avabuilds.packetrunner.network.Scenario
import com.avabuilds.packetrunner.network.Scenarios
import com.avabuilds.packetrunner.ui.hud.BottomNav
import com.avabuilds.packetrunner.ui.hud.BottomNavHeight
import com.avabuilds.packetrunner.ui.hud.NavTab
import com.avabuilds.packetrunner.ui.hud.PhoneFab
import com.avabuilds.packetrunner.ui.hud.ProfileScreen
import com.avabuilds.packetrunner.ui.hud.TopBar
import com.avabuilds.packetrunner.ui.hud.VoicemailScreen
import com.avabuilds.packetrunner.ui.hud.TopBarHeight
import com.avabuilds.packetrunner.ui.lab.CrcGame
import com.avabuilds.packetrunner.ui.lab.EncodingGame
import com.avabuilds.packetrunner.ui.lab.LabGameShell
import com.avabuilds.packetrunner.ui.lab.LanGame
import com.avabuilds.packetrunner.ui.lab.MobileIpGame
import com.avabuilds.packetrunner.ui.lab.TransportGame
import com.avabuilds.packetrunner.ui.lab.LabMenu
import com.avabuilds.packetrunner.ui.lab.WindowGame
import com.avabuilds.packetrunner.ui.notebook.NotebookPanel
import com.avabuilds.packetrunner.notify.CallNotifications
import com.avabuilds.packetrunner.notify.CallScheduler
import com.avabuilds.packetrunner.work.CallPlanner
import com.avabuilds.packetrunner.work.nextWorkdayAfter
import com.avabuilds.packetrunner.work.CallStatus
import com.avabuilds.packetrunner.work.WorkState
import com.avabuilds.packetrunner.game.LunchChat
import com.avabuilds.packetrunner.ui.work.ClockButton
import com.avabuilds.packetrunner.ui.work.FreeTimeCard
import com.avabuilds.packetrunner.ui.work.MarketScreen
import com.avabuilds.packetrunner.ui.work.OfficeRoomScreen
import com.avabuilds.packetrunner.ui.work.PaydayScreen
import com.avabuilds.packetrunner.work.FreeTime
import com.avabuilds.packetrunner.work.Payday
import com.avabuilds.packetrunner.work.WeekRules
import com.avabuilds.packetrunner.work.WeekSnapshot
import com.avabuilds.packetrunner.ui.work.LunchCard
import com.avabuilds.packetrunner.ui.work.LunchScene
import com.avabuilds.packetrunner.ui.work.WeeklyTopicScreen
import com.avabuilds.packetrunner.work.Practice
import com.avabuilds.packetrunner.work.WeeklyTopics
import com.avabuilds.packetrunner.ui.work.WorkChip
import com.avabuilds.packetrunner.ui.work.WorkSettingsScreen
import com.avabuilds.packetrunner.ui.work.rememberWorkLife
import com.avabuilds.packetrunner.ui.office.UpgradeShop
import com.avabuilds.packetrunner.ui.office.Workbench
import com.avabuilds.packetrunner.ui.scene.NatScreen
import com.avabuilds.packetrunner.ui.scene.RoutingScreen
import com.avabuilds.packetrunner.ui.scene.SceneScreen
import kotlinx.coroutines.delay
import kotlin.random.Random

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
    /** The call the player tapped in a notification; the game brings it to the phone. */
    openCallId: Int? = null,
    onCallOpened: () -> Unit = {},
    /** The space taken by the system bars: the town ignores it (it fills the screen), the rest keeps out of it. */
    contentPadding: PaddingValues = PaddingValues(),
) {
    val haptics = LocalHapticFeedback.current
    val work = rememberWorkLife()
    var workSettingsOpen by remember { mutableStateOf(false) }
    var inboxOpen by remember { mutableStateOf(false) }
    var topicOpen by remember { mutableStateOf(false) }
    var roomOpen by remember { mutableStateOf(false) }
    var marketOpen by remember { mutableStateOf(false) }
    var payday by remember { mutableStateOf<Payday?>(null) }
    var lunchOpen by remember { mutableStateOf(false) }
    var lunchSkipped by remember { mutableStateOf(false) }
    var inboxReason by remember { mutableStateOf<String?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val town = remember { Town.default() }
    val neighbors = remember { Neighbors.default() }
    // A new game gets a random seed; a loaded game keeps its own, so the same days come back
    val generator = remember { RequestGenerator(seed = initialSave?.seed ?: Random.nextLong()) }
    val controller = remember { TownController(town) }
    var state by remember {
        mutableStateOf(if (initialSave != null) GameEngine.restore(initialSave, neighbors, generator) else GameEngine.newGame(neighbors, generator))
    }
    var tab by remember { mutableStateOf(NavTab.TOWN) } // the bottom navigation: Town, Notebook, Shop or Profile
    var toast by remember { mutableStateOf<String?>(null) }
    var scenario by remember { mutableStateOf<Scenario?>(null) } // the room the player is working in, if any
    var saveMark by remember { mutableStateOf("") } // cloud mark: ... while saving, a tick when stored, ! when it failed
    var nat by remember { mutableStateOf<NatPuzzle?>(null) } // the Apartments NAT job, if open
    var routing by remember { mutableStateOf<RoutingPuzzle?>(null) } // the City Hall router job, if open
    var workbenchOpen by remember { mutableStateOf(false) }
    var labMenuOpen by remember { mutableStateOf(false) }
    var labGame by remember { mutableStateOf<LabGame?>(null) }
    var monitorOpen by remember { mutableStateOf(false) }
    var monitorMessage by remember { mutableStateOf<String?>(null) }

    // The work clock: customer calls come from the real (or demo) clock, only while the engineer is working.
    // First the day's calls are planned (or read back after a restart), then the game follows the clock every second.
    var callsReady by remember { mutableStateOf(false) }
    LaunchedEffect(work.loaded, work.needsSetup, work.now.epochDay, work.schedule) {
        if (!work.loaded || work.needsSetup) return@LaunchedEffect
        val today = work.now
        val book = work.callBook
        if (book.epochDay == today.epochDay) {
            // the same day after a restart: calls that were not rung yet come back
            val pending = book.expire(today.minuteOfDay).also { work.updateCallBook(it) }.pending()
            // changing the work settings during a call must not hang up on it: the ringing phone and the open job stay
            val busy = state
            state = GameEngine.startWorkDay(state, pending, newDay = false).copy(voicemails = work.callBook.voicemails(), ringing = busy.ringing, active = busy.active)
        } else {
            val newDay = book.epochDay != -1L
            val nextDay = if (newDay) state.day + 1 else state.day
            val ahead = work.nextBook.takeIf { it.epochDay == today.epochDay }
            val planned = ahead ?: CallPlanner.plan(work.schedule, today, nextDay, neighbors, state.connected, generator, fromMinute = today.minuteOfDay)
            work.updateCallBook(planned)
            state = GameEngine.startWorkDay(state, planned.pending(), newDay = newDay)
        }
        // Plan the next workday ahead too, so its notifications can be set up while the app is closed
        work.schedule.nextWorkdayAfter(today)?.let { next ->
            if (work.nextBook.epochDay != next.epochDay) {
                work.updateNextBook(CallPlanner.plan(work.schedule, next, state.day + 1, neighbors, state.connected, generator))
            }
        }
        // The week: start tracking, or find a payday that was missed because the app was closed
        val snap = WeekSnapshot(state.solved.size, state.hearts.values.sum(), state.xp)
        val thisWeek = WeeklyTopics.weekOf(today)
        val tracker = work.week
        if (tracker.week < 0) work.updateWeek(WeekRules.startWeek(tracker, thisWeek, snap))
        else if (thisWeek > tracker.week) {
            payday = WeekRules.missedPayday(tracker, thisWeek, state.solved, snap)
            work.updateWeek(WeekRules.startWeek(tracker, thisWeek, snap))
        }
        callsReady = true
    }

    // Payday: when the shift ends on the last workday of the week, show what the week brought
    var previousWork by remember { mutableStateOf<WorkState?>(null) }
    LaunchedEffect(work.state, callsReady) {
        if (!callsReady) return@LaunchedEffect
        val before = previousWork
        previousWork = work.state
        if ((before == WorkState.WORKING || before == WorkState.LUNCH) && work.state == WorkState.OFF_DUTY) {
            val snap = WeekSnapshot(state.solved.size, state.hearts.values.sum(), state.xp)
            WeekRules.paydayAtClockOut(work.week, work.schedule, work.now, state.solved, snap)?.let { payday = it }
        }
    }

    // Alarms for the calls still to come (today and the next workday), so notifications arrive with the app closed
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(callsReady, work.callBook, work.nextBook, work.demoMode, work.schedule) {
        if (!callsReady) return@LaunchedEffect
        CallScheduler.schedule(appContext, listOf(work.callBook, work.nextBook), work.now, work.demoFactor)
    }

    // Ask once for permission to show notifications (Android 13 and later)
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(work.loaded, work.needsSetup) {
        if (work.loaded && !work.needsSetup && !work.notificationsAsked && CallNotifications.needsPermissionRequest(appContext)) {
            work.markNotificationsAsked()
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // A call opened from a notification rings in the game right away; its notification goes away
    LaunchedEffect(openCallId, callsReady) {
        val id = openCallId ?: return@LaunchedEffect
        if (!callsReady) return@LaunchedEffect
        CallNotifications.cancel(appContext, id)
        state = GameEngine.bringCallForward(state, id)
        tab = NavTab.TOWN
        onCallOpened()
    }
    // A call that rings inside the game no longer needs its notification
    LaunchedEffect(state.ringing?.id) { state.ringing?.let { CallNotifications.cancel(appContext, it.id) } }
    LaunchedEffect(work.now, callsReady) {
        if (!callsReady) return@LaunchedEffect
        val minute = work.now.minuteOfDay + work.now.secondOfDay % 60 / 60f
        val synced = GameEngine.syncTo(state, minute, canRing = work.state == WorkState.WORKING)
        var book = work.callBook
        synced.ringing?.let { book = book.markRang(it.id) }
        // a call that could not ring in time (you were busy or on a break) goes to the voicemail inbox
        val expired = book.expire(work.now.minuteOfDay)
        val newlyMissed = expired.calls.filter { c -> c.status == CallStatus.MISSED && book.calls.first { it.request.id == c.request.id }.status == CallStatus.PENDING }.map { it.request }
        val missedIds = newlyMissed.map { it.id }.toSet()
        val withInbox = synced.copy(
            upcoming = synced.upcoming.filter { it.id !in missedIds },
            voicemails = synced.voicemails + newlyMissed.filter { m -> synced.voicemails.none { it.id == m.id } },
        )
        state = withInbox
        work.updateCallBook(expired.syncVoicemails(withInbox.voicemails.map { it.id }.toSet()))
    }

    // Clock-out (or the end of work): voicemails still in the inbox fade away and those neighbors are a little sad
    LaunchedEffect(work.state, callsReady, state.voicemails.size) {
        if (!callsReady || work.state == WorkState.WORKING || work.state == WorkState.LUNCH || state.voicemails.isEmpty()) return@LaunchedEffect
        val sad = state.voicemails.map { it.neighborId }.distinct().mapNotNull { id -> neighbors.firstOrNull { it.id == id }?.name }
        state = GameEngine.endShift(state)
        work.updateCallBook(work.callBook.expireVoicemails())
        toast = "Shift over. ${sad.joinToString(" and ")} ${if (sad.size == 1) "is" else "are"} a little sad about the missed call${if (sad.size == 1) "" else "s"}."
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
    // A short buzz when the phone starts to ring
    LaunchedEffect(state.ringing?.id) { if (state.ringing != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

    // A message when the engineer reaches a new level
    val level = Progress.progress(state.xp).level
    var lastLevel by remember { mutableIntStateOf(level) }
    LaunchedEffect(level) {
        if (level > lastLevel) {
            toast = "Level up! You are now level $level"
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastLevel = level
    }
    // Picks up the ringing phone; a quick answer pays a bonus and the neighbor likes you more
    fun answerPhone() {
        val before = state
        state = GameEngine.answerCall(state)
        if (state.todayQuickAnswers > before.todayQuickAnswers) toast = "Quick answer! +₩${GameEngine.QUICK_BONUS} and a heart"
    }
    // A new lunch break lets the lunch card appear again
    LaunchedEffect(work.state) { if (work.state != WorkState.LUNCH) lunchSkipped = false }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    // The scooter makes the engineer faster
    SideEffect { controller.walkSpeed = Upgrades.walkSpeed(state.upgrades[Upgrades.SCOOTER] ?: 0) }

    // How each building's link to the office is doing, for the cables and Wi-Fi rings on the map.
    // derivedStateOf: the map is only rebuilt (and the town redrawn) when a status really changes.
    val linkStatus by remember {
        derivedStateOf { neighbors.associate { it.buildingId to NetworkMonitor.status(state, it) } }
    }

    // The bubbles above the buildings. derivedStateOf: rebuilt only when something in them really changes.
    val bubbles by remember { derivedStateOf { Bubbles.of(state, neighbors) } }

    val active = state.active
    val ringing = state.ringing
    val atTarget = active != null && controller.arrivedAt?.id == active.buildingId

    Box(modifier.fillMaxSize()) {
        // The town draws edge to edge, under the system bars, so the time-of-day color covers the whole screen.
        // Everything else keeps clear of the bars.
        IsoTownScreen(controller, targetBuildingId = active?.buildingId, linkStatus = linkStatus, dayMinute = { state.minute }, bubbles = bubbles, controlsPadding = PaddingValues(top = contentPadding.calculateTopPadding() + TopBarHeight + 4.dp),
            onBuildingTap = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) })
        Box(Modifier.fillMaxSize().padding(contentPadding)) {

        // Top: the bar with the engineer, level, money and time; messages appear just below it
        Column(Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp).padding(top = 12.dp)) {
            TopBar(
                state = state,
                saveMark = if (user != null) saveMark else "",
                onAvatar = { tab = NavTab.PROFILE },
                onTimeLongPress = null,
            )
            if (work.loaded) WorkChip(work, Modifier.padding(top = 6.dp))
            toast?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PR.Green800, modifier = Modifier.padding(top = 6.dp, start = 4.dp)) }
        }

        // Bottom: the ringing phone, or the job in progress
        if (tab == NavTab.TOWN) Column(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = BottomNavHeight + 8.dp)) {
            if (ringing != null) {
                val neighbor = neighbors.first { it.id == ringing.neighborId }
                CallBubble(
                    request = ringing,
                    neighbor = neighbor,
                    hearts = state.hearts[neighbor.id] ?: 0,
                    onAnswer = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        answerPhone()
                        controller.walkToDoorOf(town.building(ringing.buildingId))
                    },
                    onIgnore = { state = GameEngine.ignore(state) },
                )
            } else if (monitorOpen) {
                // the monitor sits above the town; nothing else is needed down here
            } else if (active == null && FreeTime.isFreeTime(work.state, work.canClockIn)) {
                FreeTimeCard(
                    state = work.state,
                    marketOpen = FreeTime.marketOpen(work.now),
                    onRoom = { roomOpen = true },
                    onMarket = { marketOpen = true },
                    onShop = { tab = NavTab.SHOP },
                    onTopic = { topicOpen = true },
                    onLab = { labMenuOpen = true },
                )
            } else if (active == null && work.state == WorkState.LUNCH && !lunchSkipped) {
                val partner = LunchChat.partner(neighbors)
                if (partner != null) {
                    LunchCard(
                        atCafe = controller.arrivedAt?.type == BuildingType.CAFE,
                        alreadyHadLunch = GameEngine.LUNCH_ID in state.labDone,
                        partnerName = partner.name,
                        onWalk = { controller.walkToDoorOf(town.building(partner.buildingId)) },
                        onEat = { lunchOpen = true },
                        onSkip = { lunchSkipped = true },
                    )
                }
            } else if (active == null && controller.arrivedAt?.type == BuildingType.OFFICE) {
                OfficeCard(onOpenMonitor = { monitorOpen = true; monitorMessage = null }, onOpenLab = { labMenuOpen = true }, onOpenWorkbench = { workbenchOpen = true }, onOpenShop = { tab = NavTab.SHOP }, clockButton = { ClockButton(work) }, voicemails = state.voicemails.size, onOpenVoicemail = { inboxOpen = true; inboxReason = null }, onOpenTopic = { topicOpen = true })
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
                            } else if (active.type == RequestType.NAT) {
                                nat = NatPuzzles.forJob(state.solved.count { it.type == RequestType.NAT })
                            } else {
                                scenario = Scenarios.forRequest(active)?.let {
                                    it.withCablePrice(Upgrades.cablePrice(it.cablePricePerCell, state.upgrades[Upgrades.CABLE_SPOOL] ?: 0))
                                }
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
        if (tab == NavTab.NOTEBOOK && !state.dayOver) {
            NotebookPanel(
                uid = user?.uid ?: LOCAL_UID,
                repo = notebook,
                onClose = { tab = NavTab.TOWN },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = TopBarHeight + 16.dp, start = 12.dp, end = 12.dp),
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
                modifier = Modifier.align(Alignment.TopCenter).padding(top = TopBarHeight + 16.dp, start = 12.dp, end = 12.dp),
            )
        }

        // The bottom bar and the phone button. Rooms, the Lab and the other full-screen tools are drawn after them, so they cover them.
        BottomNav(tab, onSelect = { tab = it; if (it != NavTab.TOWN) monitorOpen = false }, modifier = Modifier.align(Alignment.BottomCenter))
        if (!state.dayOver && tab == NavTab.TOWN) {
            PhoneFab(
                ringing = ringing != null,
                onClick = {
                    if (ringing != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        answerPhone()
                        controller.walkToDoorOf(town.building(ringing.buildingId))
                        tab = NavTab.TOWN
                    } else if (state.voicemails.isNotEmpty()) {
                        inboxOpen = true
                        inboxReason = null
                    } else {
                        toast = "No one is calling right now. Neighbors phone during your work hours."
                    }
                },
                voicemails = state.voicemails.size,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
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

        // The office tools: the Upgrade shop and the Workbench; the clock waits while they are open
        if (tab == NavTab.SHOP) {
            UpgradeShop(
                money = state.money, levels = state.upgrades,
                onBuy = { state = GameEngine.buy(state, it) },
                onClose = { tab = NavTab.TOWN },
                modifier = Modifier.padding(bottom = BottomNavHeight),
            )
        }
        if (tab == NavTab.PROFILE) {
            ProfileScreen(
                state = state, neighbors = neighbors, user = user, auth = auth, saves = saves, notebook = notebook,
                onUserChanged = onUserChanged, onLoggedOut = onLoggedOut, onClose = { tab = NavTab.TOWN }, onWorkSettings = { workSettingsOpen = true },
                modifier = Modifier.padding(bottom = BottomNavHeight),
            )
        }
        if (workbenchOpen) Workbench(onClose = { workbenchOpen = false })

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
                    LabGame.LAN -> LanGame(onSuccess)
                    LabGame.MOBILE -> MobileIpGame(onSuccess)
                    LabGame.TRANSPORT -> TransportGame(onSuccess)
                }
            }
        } else if (labMenuOpen) {
            LabMenu(doneToday = state.labDone, onPick = { labGame = it }, onClose = { labMenuOpen = false })
        }

        val natJob = nat
        if (natJob != null && active != null) {
            val neighbor = neighbors.first { it.id == active.neighborId }
            NatScreen(
                puzzle = natJob,
                tenantName = neighbor.name,
                reward = active.reward,
                onSaveToNotebook = { title, concepts -> saveConceptsAsNote(title, concepts) },
                onFinish = { success, payout, stars ->
                    nat = null
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

        // Coins pop up under the money in the top bar whenever money is earned. Drawn last, so they show over rooms and menus too.
        CoinPops(
            money = state.money,
            anchor = { size -> Offset(size.width * 0.58f, (contentPadding.calculateTopPadding().value + 56f) * density) },
            onEarned = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
        )

        // Free time: the office room and the Saturday market
        if (roomOpen) {
            OfficeRoomScreen(
                owned = Upgrades.decor.filter { (state.upgrades[it] ?: 0) > 0 }.toSet(),
                onShop = { roomOpen = false; tab = NavTab.SHOP },
                onClose = { roomOpen = false },
            )
        }
        if (marketOpen) {
            MarketScreen(
                state = state, neighbors = neighbors,
                onGift = { n -> state = GameEngine.giveGift(state, n.id); toast = "${n.name} loves the gift! +1 heart" },
                onClose = { marketOpen = false },
            )
        }
        payday?.let { due ->
            PaydayScreen(due, onCollect = {
                state = GameEngine.collectPayday(state, due.bonus)
                work.updateWeek(work.week.copy(paidWeek = maxOf(work.week.paidWeek, due.week)))
                if (due.bonus > 0) toast = "Payday! +₩${due.bonus}"
                payday = null
            })
        }

        // Lunch at the cafe, and the weekly topic
        val lunchPartner = LunchChat.partner(neighbors)
        if (lunchOpen && lunchPartner != null) {
            LunchScene(
                partner = lunchPartner,
                alreadyHadLunch = GameEngine.LUNCH_ID in state.labDone,
                onFinish = {
                    if (GameEngine.LUNCH_ID !in state.labDone) {
                        state = GameEngine.haveLunch(state, lunchPartner.id)
                        toast = "Lunch with ${lunchPartner.name}: +1 heart"
                    }
                    lunchOpen = false
                },
            )
        }
        if (topicOpen) {
            WeeklyTopicScreen(
                topic = WeeklyTopics.forWeek(work.now),
                weekNumber = WeeklyTopics.weekOf(work.now),
                claimedToday = GameEngine.WEEKLY_ID in state.labDone,
                onPractice = { practice ->
                    when (practice) {
                        is Practice.InLab -> { topicOpen = false; labGame = practice.game }
                        Practice.OnWorkbench -> { topicOpen = false; workbenchOpen = true }
                        is Practice.OnTheJob -> Unit
                    }
                },
                onClose = {
                    if (GameEngine.WEEKLY_ID !in state.labDone) {
                        state = GameEngine.readWeeklyTopic(state)
                        toast = "Weekly topic read: +${WeeklyTopics.CLAIM_XP} XP"
                    }
                    topicOpen = false
                },
            )
        }

        // The voicemail inbox: missed calls, to be returned before clock-out
        if (inboxOpen) {
            VoicemailScreen(
                voicemails = state.voicemails,
                neighbors = neighbors,
                canCallBack = state.ringing == null && state.active == null && work.state == WorkState.WORKING,
                reason = inboxReason ?: when {
                    work.state != WorkState.WORKING -> "You can call back while you are working."
                    state.active != null || state.ringing != null -> "Finish your current call first."
                    else -> null
                },
                onCallBack = { call ->
                    state = GameEngine.returnVoicemail(state, call.id)
                    controller.walkToDoorOf(town.building(call.buildingId))
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    inboxOpen = false
                    tab = NavTab.TOWN
                },
                onClose = { inboxOpen = false },
                modifier = Modifier.padding(bottom = BottomNavHeight),
            )
        }

        // Work settings: first launch asks for the work week; later it opens from the Profile
        if (work.needsSetup) WorkSettingsScreen(work, firstTime = true, onClose = {})
        else if (workSettingsOpen) {
            WorkSettingsScreen(
                work, firstTime = false, onClose = { workSettingsOpen = false },
                onRingNow = {
                    if (work.state != WorkState.WORKING) {
                        toast = "You are ${work.state.label.lowercase()}. Clock in first to get a call."
                    } else {
                        val pool = neighbors.filter { it.movesInDay <= state.day }
                        val caller = pool[Random.nextInt(pool.size)]
                        val call = generator.requestFor(
                            id = 900_000 + Random.nextInt(99_999), neighbor = caller, connected = state.connected,
                            day = state.day, callMinute = state.minute, rnd = Random(System.nanoTime()),
                        )
                        state = GameEngine.addCallNow(state, call)
                        tab = NavTab.TOWN
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
}
