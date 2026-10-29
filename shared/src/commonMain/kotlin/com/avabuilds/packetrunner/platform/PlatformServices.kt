package com.avabuilds.packetrunner.platform

import androidx.compose.runtime.Composable
import com.avabuilds.packetrunner.data.WorkDataStore
import com.avabuilds.packetrunner.game.ChimeNote
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.WallTime

/** Plays a short tune. Android uses an AudioTrack, the browser the Web Audio API. A device without sound just stays silent. */
interface SoundPlayer {
    fun play(notes: List<ChimeNote>)
}

/**
 * How customer calls reach the player when the game is not on screen: notifications. On Android they are scheduled with
 * WorkManager and work with the app closed; in the browser they only work while the page is open.
 */
interface CallAlerts {
    /** True when the system still has to ask the player for permission to show notifications. */
    fun needsPermissionRequest(): Boolean

    /** Gives a function that asks the player for the permission (a system dialog), made inside a composition. */
    @Composable
    fun rememberPermissionRequester(): () -> Unit

    /** Sets up an alert for every call in [books] that is still to come. Calling it again moves the alerts. */
    fun schedule(books: List<CallBook>, now: WallTime, factor: Int)

    /** Removes the alert of a call (the player has dealt with it in the game). */
    fun cancel(callId: Int)

    /**
     * For platforms where the player chooses to turn alerts on (a browser): "granted", "denied", "default" (can still be
     * asked) or "unsupported" (nothing to choose here: the system asks by itself, or there are no alerts).
     */
    fun optInState(): String = "unsupported"

    /** Asks for the permission. A browser only allows this right after a tap of the player. */
    fun requestOptIn() = Unit
}

/** What the game needs from the device that is not the same everywhere. Each platform builds one and hands it to the game. */
class PlatformServices(
    val workStore: WorkDataStore,
    val sound: SoundPlayer = SilentSound,
    val callAlerts: CallAlerts = NoCallAlerts,
    /** True in a debug build: the Profile gets a button for trying the levels. */
    val isDebug: Boolean = false,
)

object SilentSound : SoundPlayer {
    override fun play(notes: List<ChimeNote>) = Unit
}

object NoCallAlerts : CallAlerts {
    override fun needsPermissionRequest() = false

    @Composable
    override fun rememberPermissionRequester(): () -> Unit = {}

    override fun schedule(books: List<CallBook>, now: WallTime, factor: Int) = Unit
    override fun cancel(callId: Int) = Unit
}
