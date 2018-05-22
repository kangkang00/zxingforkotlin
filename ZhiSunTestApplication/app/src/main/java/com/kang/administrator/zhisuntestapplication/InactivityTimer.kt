package com.kang.administrator.zhisuntestapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.BatteryManager
import android.util.Log
import java.util.concurrent.RejectedExecutionException

internal class InactivityTimer(private val activity: Activity) {
    private val powerStatusReceiver: BroadcastReceiver
    private var registered: Boolean = false
    private var inactivityTask: AsyncTask<Any, Any, Any>? = null

    init {
        powerStatusReceiver = PowerStatusReceiver()
        registered = false
        onActivity()
    }

    @Synchronized
    fun onActivity() {
        cancel()
        inactivityTask = InactivityAsyncTask()
        try {
            inactivityTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } catch (ree: RejectedExecutionException) {
            Log.w(TAG, "Couldn't schedule inactivity task; ignoring")
        }

    }

    @Synchronized
    fun onPause() {
        cancel()
        if (registered) {
            activity.unregisterReceiver(powerStatusReceiver)
            registered = false
        } else {
            Log.w(TAG, "PowerStatusReceiver was never registered?")
        }
    }

    @Synchronized
    fun onResume() {
        if (registered) {
            Log.w(TAG, "PowerStatusReceiver was already registered?")
        } else {
            activity.registerReceiver(powerStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            registered = true
        }
        onActivity()
    }

    @Synchronized
    private fun cancel() {
        val task = inactivityTask
        if (task != null) {
            task.cancel(true)
            inactivityTask = null
        }
    }

    fun shutdown() {
        cancel()
    }

    private inner class PowerStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                // 0 indicates that we're on battery
                val onBatteryNow = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0
                if (onBatteryNow) {
                    this@InactivityTimer.onActivity()
                } else {
                    this@InactivityTimer.cancel()
                }
            }
        }
    }

    private inner class InactivityAsyncTask : AsyncTask<Any, Any, Any>() {
        override fun doInBackground(vararg objects: Any): Any? {
            try {
                Thread.sleep(INACTIVITY_DELAY_MS)
                Log.i(TAG, "Finishing activity due to inactivity")
                activity.finish()
            } catch (e: InterruptedException) {
                // continue without killing
            }

            return null
        }
    }

    companion object {

        private val TAG = InactivityTimer::class.java.simpleName

        private val INACTIVITY_DELAY_MS = 5 * 60 * 1000L
    }

}