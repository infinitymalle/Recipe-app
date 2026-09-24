package dev.malkolm.recipeapp.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.malkolm.recipeapp.MainActivity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps cook timers alive and visible while the app is backgrounded, by staying a foreground
 * service (with an ongoing notification, as Android requires) for as long as [CookTimerRepository]
 * has a running timer. [CookTimerRepository] is the actual source of truth and does the ticking;
 * this service only reflects that state into a notification and, by being a foreground service,
 * makes the app's process much less likely to be killed while cooking.
 *
 * What this does not do: survive the OS killing the app process outright (some phones' battery
 * managers still do this to backgrounded apps despite the foreground notification), or use an
 * `AlarmManager` fallback that could ring even after such a kill. If timers stop unexpectedly,
 * excluding this app from battery optimization (Settings) is the usual fix.
 */
@AndroidEntryPoint
class CookTimerService : Service() {
    @Inject
    lateinit var cookTimerRepository: CookTimerRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var previousTimers: List<RunningCookTimer> = emptyList()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification(emptyList()))
        scope.launch { cookTimerRepository.timers.collect(::handleUpdate) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun handleUpdate(timers: List<RunningCookTimer>) {
        val justFinished =
            timers.filter { timer ->
                timer.remainingSeconds == 0 && previousTimers.any { it.id == timer.id && it.isRunning }
            }
        justFinished.forEach(::postCompletionNotification)
        previousTimers = timers

        val running = timers.filter { it.isRunning }
        if (running.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(ONGOING_NOTIFICATION_ID, buildOngoingNotification(running))
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildOngoingNotification(running: List<RunningCookTimer>): Notification {
        val text =
            if (running.isEmpty()) {
                "No timers running"
            } else {
                running.joinToString(" · ") { timer ->
                    val label = timer.label.ifBlank { timer.recipeTitle }
                    "$label: ${formatSeconds(timer.remainingSeconds)}"
                }
            }
        return NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setContentTitle("Cooking timers")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()
    }

    private fun postCompletionNotification(timer: RunningCookTimer) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val label = timer.label.ifBlank { timer.recipeTitle }
        val notification =
            NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("Timer done")
                .setContentText("$label is done")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setAutoCancel(true)
                .setContentIntent(contentIntent())
                .build()
        NotificationManagerCompat.from(this).notify(timer.id.hashCode(), notification)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(ONGOING_CHANNEL_ID, "Cooking timers", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL_ID, "Timer alerts", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val ONGOING_CHANNEL_ID = "cook_timers_ongoing"
        private const val ALERT_CHANNEL_ID = "cook_timers_alert"
    }
}

fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
