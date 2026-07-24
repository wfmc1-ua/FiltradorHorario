package com.wesley.cuadrantes.model

import java.time.LocalDate
import java.time.LocalTime

data class Cuadrante(
    val weekStart: LocalDate,
    val center: String?,
    val employees: List<EmployeeSchedule>,
)

data class EmployeeSchedule(
    val rawId: String?,
    val name: String,
    val contractHours: Int?,
    val days: List<DaySchedule>,
)

data class DaySchedule(
    val date: LocalDate,
    val status: DayStatus,
    val shifts: List<Shift>,
)

enum class DayStatus { WORK, FREE, VACATION, UNKNOWN }

data class Shift(val start: LocalTime, val end: LocalTime)

data class AlarmPlan(
    val date: LocalDate,
    val time: LocalTime,
    val label: String,
    val enabled: Boolean = true,
)
