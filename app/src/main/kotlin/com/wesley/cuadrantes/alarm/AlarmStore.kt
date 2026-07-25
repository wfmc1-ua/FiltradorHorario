package com.wesley.cuadrantes.alarm

import android.content.Context
import com.wesley.cuadrantes.model.AlarmPlan

class AlarmStore(context: Context) {

    private val prefs = context.getSharedPreferences("alarm_store", Context.MODE_PRIVATE)
    private val key = "plans"

    fun save(plans: List<AlarmPlan>) {
        val encoded = plans.map { AlarmPlanCodec.encode(it) }.toSet()
        prefs.edit().putStringSet(key, encoded).apply()
    }

    fun load(): List<AlarmPlan> =
        prefs.getStringSet(key, emptySet())
            .orEmpty()
            .mapNotNull { AlarmPlanCodec.decode(it) }

    fun clear() {
        prefs.edit().remove(key).apply()
    }
}
