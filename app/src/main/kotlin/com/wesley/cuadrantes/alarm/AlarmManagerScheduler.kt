package com.wesley.cuadrantes.alarm

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wesley.cuadrantes.model.AlarmPlan
import java.time.ZoneId

class AlarmManagerScheduler(
    private val context: Context,
    private val store: AlarmStore,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(plan: AlarmPlan) {
        val triggerMs = plan.date.atTime(plan.time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val pendingIntent = buildPendingIntent(plan)

        val showIntent = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAlarmClock(AlarmClockInfo(triggerMs, showIntent), pendingIntent)

        val current = store.load().toMutableList()
        if (current.none { it.date == plan.date }) current.add(plan)
        store.save(current)
    }

    override fun cancelAll() {
        store.load().forEach { plan ->
            alarmManager.cancel(buildPendingIntent(plan))
        }
        store.clear()
    }

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    fun canUseFullScreenIntent(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .canUseFullScreenIntent()
        } else {
            true
        }

    private fun buildPendingIntent(plan: AlarmPlan): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_LABEL, plan.label)
        }
        val requestCode = plan.date.toEpochDay().toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
