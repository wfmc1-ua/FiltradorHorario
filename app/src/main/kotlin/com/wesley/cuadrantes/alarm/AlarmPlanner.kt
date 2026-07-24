package com.wesley.cuadrantes.alarm

import com.wesley.cuadrantes.model.AlarmPlan
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.ui.review.EditableDayState
import com.wesley.cuadrantes.util.Clock
import com.wesley.cuadrantes.util.SystemClock
import java.time.LocalTime

data class PlannedAlarm(val plan: AlarmPlan, val isPast: Boolean)

class AlarmPlanner(
    private val clock: Clock = SystemClock(),
    private val leadMinutes: Long = 60,
) {

    fun plan(days: List<EditableDayState>): List<PlannedAlarm> {
        val now = clock.now()
        return days.mapNotNull { day ->
            if (day.status != DayStatus.WORK) return@mapNotNull null
            if (!day.alarmEnabled) return@mapNotNull null
            val firstShift = day.shifts.firstOrNull() ?: return@mapNotNull null

            val alarmTime = firstShift.start.minusMinutes(leadMinutes)
            val alarmDateTime = day.date.atTime(alarmTime)
            val label = buildLabel(day.shifts.map { "${formatTime(it.start)}–${formatTime(it.end)}" })

            PlannedAlarm(
                plan = AlarmPlan(
                    date = day.date,
                    time = alarmTime,
                    label = label,
                    enabled = true,
                ),
                isPast = alarmDateTime.isBefore(now),
            )
        }
    }

    private fun buildLabel(shiftLabels: List<String>): String =
        "Turno " + shiftLabels.joinToString(" / ")

    private fun formatTime(t: LocalTime): String =
        "%02d:%02d".format(t.hour, t.minute)
}
