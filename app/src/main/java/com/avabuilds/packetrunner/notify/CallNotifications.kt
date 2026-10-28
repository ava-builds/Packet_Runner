package com.avabuilds.packetrunner.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.avabuilds.packetrunner.MainActivity
import com.avabuilds.packetrunner.R
import com.avabuilds.packetrunner.data.WorkStore
import com.avabuilds.packetrunner.data.timeSource
import com.avabuilds.packetrunner.game.Neighbors
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.NotificationRules
import com.avabuilds.packetrunner.work.SystemTimeSource
import com.avabuilds.packetrunner.work.WallTime
import java.util.concurrent.TimeUnit

/** Is the game on screen? Then calls ring inside the game and no notification is needed. */
object AppVisibility {
    @Volatile
    var inForeground: Boolean = false
}

/** The notification channel, the permission and the notification itself. */
object CallNotifications {
    const val CHANNEL_ID = "customer_calls"
    const val EXTRA_CALL_ID = "call_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Customer calls", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Neighbors phone you during your work hours"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Android 13 and later need the player's permission to show notifications; earlier versions always allow them. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun needsPermissionRequest(context: Context) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission(context)

    /** Shows "Hana is calling" for [callId]; tapping it opens the game on that call. */
    fun show(context: Context, callId: Int, neighborName: String, message: String) {
        if (!hasPermission(context)) return
        createChannel(context)
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_CALL_ID, callId)
        val pending = PendingIntent.getActivity(context, callId, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("$neighborName is calling")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        @Suppress("MissingPermission") // checked above
        NotificationManagerCompat.from(context).notify(callId, n)
    }

    fun cancel(context: Context, callId: Int) = NotificationManagerCompat.from(context).cancel(callId)
}

/** Sets an alarm (WorkManager) for every call that is still to come, so notifications arrive with the app closed. */
object CallScheduler {
    private fun name(callId: Int) = "call-$callId"

    fun schedule(context: Context, books: List<CallBook>, now: WallTime, factor: Int) {
        val wm = WorkManager.getInstance(context)
        NotificationRules.toSchedule(books, now).forEach { (book, call) ->
            val delay = NotificationRules.delaySeconds(book.epochDay, call.minute, now, factor)
            val request = OneTimeWorkRequestBuilder<CallNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putInt(CallNotifications.EXTRA_CALL_ID, call.request.id).build())
                .build()
            // REPLACE: running this again after a change (demo mode, new settings) simply moves the alarm
            wm.enqueueUniqueWork(name(call.request.id), ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancelAll(context: Context, books: List<CallBook>) {
        val wm = WorkManager.getInstance(context)
        books.flatMap { it.calls }.forEach { wm.cancelUniqueWork(name(it.request.id)) }
    }
}

/**
 * Runs when a planned call is due. It looks at the saved data again (the game may have changed things since the alarm
 * was set) and only shows a notification when the rules say so.
 */
class CallNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getInt(CallNotifications.EXTRA_CALL_ID, -1)
        if (id < 0) return Result.success()
        val data = WorkStore(applicationContext).load()
        val now = data.timeSource(SystemTimeSource()).now()
        val call = (data.calls.calls + data.nextCalls.calls).firstOrNull { it.request.id == id }
        if (NotificationRules.shouldNotify(call, data.schedule, data.log, now, AppVisibility.inForeground)) {
            val neighbor = Neighbors.default().firstOrNull { it.id == call!!.request.neighborId }
            CallNotifications.show(applicationContext, id, neighbor?.name ?: "A neighbor", call!!.request.message)
        }
        return Result.success()
    }
}
