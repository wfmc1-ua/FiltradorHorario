package com.wesley.cuadrantes.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.wesley.cuadrantes.model.AlarmPlan

class IntentAlarmScheduler(private val context: Context) : AlarmScheduler {

    override fun cancelAll() { /* v1 no soporta cancelación */ }

    override fun schedule(plan: AlarmPlan) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, plan.time.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, plan.time.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, plan.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: open clock app UI if SKIP_UI is not supported
            intent.removeExtra(AlarmClock.EXTRA_SKIP_UI)
            context.startActivity(intent)
        }
    }
}
