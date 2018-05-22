package com.kang.administrator.zhisuntestapplication

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import java.io.Closeable
import java.io.IOException

internal class BeepManager(private val activity: Activity) : MediaPlayer.OnErrorListener, Closeable {
    private var mediaPlayer: MediaPlayer? = null
    private var playBeep: Boolean = false
    private var vibrate: Boolean = false

    init {
        this.mediaPlayer = null
        updatePrefs()
    }

    @Synchronized
    fun updatePrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        playBeep = shouldBeep(prefs, activity)
        vibrate = prefs.getBoolean(PreferencesActivity().KEY_VIBRATE, false)
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
            // so we now play on the music stream.
            activity.volumeControlStream = AudioManager.STREAM_MUSIC
            mediaPlayer = buildMediaPlayer(activity)
        }
    }

    @Synchronized
    fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VIBRATE_DURATION)
        }
    }

    private fun buildMediaPlayer(activity: Context): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        try {
            activity.resources.openRawResourceFd(R.raw.beep).use { file ->
                mediaPlayer.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                mediaPlayer.setOnErrorListener(this)
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer.isLooping = false
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME)
                mediaPlayer.prepare()
                return mediaPlayer
            }
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
            mediaPlayer.release()
            return null
        }

    }

    @Synchronized
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required and finish
            activity.finish()
        } else {
            // possibly media player error, so release and recreate
            close()
            updatePrefs()
        }
        return true
    }

    @Synchronized
    override fun close() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    companion object {

        private val TAG = BeepManager::class.java.simpleName

        private val BEEP_VOLUME = 0.10f
        private val VIBRATE_DURATION = 200L

        private fun shouldBeep(prefs: SharedPreferences, activity: Context): Boolean {
            var shouldPlayBeep = prefs.getBoolean(PreferencesActivity().KEY_PLAY_BEEP, true)
            if (shouldPlayBeep) {
                // See if sound settings overrides this
                val audioService = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    shouldPlayBeep = false
                }
            }
            return shouldPlayBeep
        }
    }

}
