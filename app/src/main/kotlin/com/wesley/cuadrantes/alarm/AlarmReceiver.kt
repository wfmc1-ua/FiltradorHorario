package com.wesley.cuadrantes.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.wesley.cuadrantes.R
import com.wesley.cuadrantes.ui.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_LABEL = "alarm_label"
        private const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Turno"
        ensureChannel(context)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(EXTRA_LABEL, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, NOTIFICATION_ID, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label)
            .setContentText("Toca para ver los detalles")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarmas de turno",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }
}
