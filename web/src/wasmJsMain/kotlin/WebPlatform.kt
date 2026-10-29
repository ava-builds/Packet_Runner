import androidx.compose.runtime.Composable
import com.avabuilds.packetrunner.data.KeyValueStore
import com.avabuilds.packetrunner.game.ChimeNote
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.platform.CallAlerts
import com.avabuilds.packetrunner.platform.SoundPlayer
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.NotificationRules
import com.avabuilds.packetrunner.work.WallTime

/** The browser's localStorage. If the browser blocks it (a private window), saving reports failure and the game still plays. */
object LocalStorageKeyValueStore : KeyValueStore {
    override fun get(key: String): String? = jsStorageGet(key)
    override fun set(key: String, value: String): Boolean = jsStorageSet(key, value)
    override fun remove(key: String) = jsStorageRemove(key)
}

/** Plays the tune with the Web Audio API. The browser only allows sound after the player has touched the page once. */
object WebSound : SoundPlayer {
    override fun play(notes: List<ChimeNote>) = jsPlayNotes(notes.joinToString(",") { "${it.hz}:${it.ms}" })
}

/**
 * Call alerts in a browser: while the page is open in the background and the player allowed notifications,
 * a timer shows "Hana is calling" when a call is due. In the foreground the game rings by itself.
 */
object WebCallAlerts : CallAlerts {
    override fun needsPermissionRequest() = false // a browser only asks after a tap: see requestOptIn

    @Composable
    override fun rememberPermissionRequester(): () -> Unit = {}

    override fun schedule(books: List<CallBook>, now: WallTime, factor: Int) {
        jsCancelAllNotifications()
        val neighbors = Neighbors.default()
        NotificationRules.toSchedule(books, now).forEach { (book, call) ->
            val delaySeconds = NotificationRules.delaySeconds(book.epochDay, call.minute, now, factor)
            val name = neighbors.firstOrNull { it.id == call.request.neighborId }?.name ?: "A neighbor"
            jsScheduleNotification(call.request.id, delaySeconds * 1000.0, "$name is calling", call.request.message)
        }
    }

    override fun cancel(callId: Int) = jsCancelNotification(callId)

    override fun optInState(): String = jsNotificationPermission()
    override fun requestOptIn() = jsRequestNotificationPermission()
}
