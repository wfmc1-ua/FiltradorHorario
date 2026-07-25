package com.wesley.cuadrantes.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wesley.cuadrantes.CuadrantesApp
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val container = (context.applicationContext as CuadrantesApp).container
        val store = AlarmStore(context)
        val now = LocalDateTime.now()

        store.load()
            .filter { plan -> plan.date.atTime(plan.time).isAfter(now) }
            .forEach { plan -> container.alarmScheduler.schedule(plan) }
    }
}
