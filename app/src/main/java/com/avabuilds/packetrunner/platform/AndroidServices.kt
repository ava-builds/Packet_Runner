package com.avabuilds.packetrunner.platform

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.avabuilds.packetrunner.data.WorkStore
import com.avabuilds.packetrunner.game.Chime
import com.avabuilds.packetrunner.game.ChimeNote
import com.avabuilds.packetrunner.notify.CallNotifications
import com.avabuilds.packetrunner.notify.CallScheduler
import com.avabuilds.packetrunner.work.CallBook
import com.avabuilds.packetrunner.work.WallTime

/** Plays the generated [Chime] on the phone's media volume. A phone that cannot play it stays silent: sound is never needed to play. */
class AndroidSoundPlayer : SoundPlayer {
    override fun play(notes: List<ChimeNote>) {
        try {
            val samples = Chime.samples(notes)
            if (samples.isEmpty()) return
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Chime.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) = t.release() // free the player when the tune is over
                override fun onPeriodicNotification(t: AudioTrack) = Unit
            })
            track.play()
        } catch (_: Exception) {
            // no sound is fine
        }
    }
}

/** Call alerts as Android notifications: scheduled with WorkManager, so they arrive even with the app closed. */
class AndroidCallAlerts(private val context: Context) : CallAlerts {
    override fun needsPermissionRequest() = CallNotifications.needsPermissionRequest(context)

    @Composable
    override fun rememberPermissionRequester(): () -> Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        return { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    override fun schedule(books: List<CallBook>, now: WallTime, factor: Int) = CallScheduler.schedule(context, books, now, factor)

    override fun cancel(callId: Int) = CallNotifications.cancel(context, callId)
}

/** What the game needs from an Android phone. */
fun androidServices(context: Context): PlatformServices {
    val app = context.applicationContext
    val debuggable = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    return PlatformServices(
        workStore = WorkStore(app),
        sound = AndroidSoundPlayer(),
        callAlerts = AndroidCallAlerts(app),
        isDebug = debuggable,
    )
}
