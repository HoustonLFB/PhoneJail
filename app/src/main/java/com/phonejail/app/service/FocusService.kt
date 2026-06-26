package com.phonejail.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.phonejail.app.MainActivity
import com.phonejail.app.R
import com.phonejail.app.data.FocusSession
import com.phonejail.app.session.SessionManager
import com.phonejail.app.util.formatElapsed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the focus timer running while the phone is in
 * jail and watches for the user picking it up.
 *
 * "Don't touch it" detection: once the screen turns off after the session
 * starts, the session is *armed*. The next time the device is unlocked
 * (ACTION_USER_PRESENT) the session is recorded as broken.
 */
class FocusService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var tickJob: Job? = null
    private var armed = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // Phone has been set down — from now on, picking it up breaks focus.
                Intent.ACTION_SCREEN_OFF -> armed = true
                Intent.ACTION_USER_PRESENT -> if (armed && SessionManager.isRunning) {
                    SessionManager.stop(this@FocusService, FocusSession.EndReason.BROKEN)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RELEASE) {
            SessionManager.stop(this, FocusSession.EndReason.RELEASED)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(0))
        startTicking()
        return START_STICKY
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val session = SessionManager.active.value
                if (session == null) {
                    stopSelf()
                    break
                }
                notificationManager().notify(NOTIFICATION_ID, buildNotification(session.elapsed()))
                delay(1_000)
            }
        }
    }

    private fun buildNotification(elapsedMillis: Long): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val release = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusService::class.java).setAction(ACTION_RELEASE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Phone in jail 🔒")
            .setContentText("Focused for ${formatElapsed(elapsedMillis)} — don't touch it")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(false)
            .setContentIntent(open)
            .addAction(0, "Release", release)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.focus_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.focus_channel_desc) }
            notificationManager().createNotificationChannel(channel)
        }
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        tickJob?.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "focus_session"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_RELEASE = "com.phonejail.app.action.RELEASE"
    }
}
