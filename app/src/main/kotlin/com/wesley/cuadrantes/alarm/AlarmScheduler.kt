package com.wesley.cuadrantes.alarm

import com.wesley.cuadrantes.model.AlarmPlan

interface AlarmScheduler {
    fun schedule(plan: AlarmPlan)
    fun cancelAll()
}
