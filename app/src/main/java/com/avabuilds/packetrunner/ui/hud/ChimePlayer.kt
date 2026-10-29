package com.avabuilds.packetrunner.ui.hud

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.avabuilds.packetrunner.game.Chime
import com.avabuilds.packetrunner.game.ChimeNote

/** Plays the generated [Chime] on the phone's media volume. A phone that cannot play it stays silent: sound is never needed to play. */
object ChimePlayer {
    fun play(notes: List<ChimeNote>) {
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
