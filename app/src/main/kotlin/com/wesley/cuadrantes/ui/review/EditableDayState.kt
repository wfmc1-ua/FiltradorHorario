package com.wesley.cuadrantes.ui.review

import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.EmployeeSchedule
import com.wesley.cuadrantes.model.Shift
import java.time.LocalDate
import java.time.LocalTime

data class EditableDayState(
    val date: LocalDate,
    val originalShifts: List<Shift>,
    val status: DayStatus,
    val shifts: List<Shift>,
    val alarmEnabled: Boolean,
) {
    fun changeStatus(newStatus: DayStatus): EditableDayState = when (newStatus) {
        DayStatus.FREE, DayStatus.VACATION -> copy(
            status = newStatus,
            shifts = emptyList(),
            alarmEnabled = false,
        )
        DayStatus.WORK -> copy(
            status = DayStatus.WORK,
            shifts = originalShifts,
            alarmEnabled = originalShifts.isNotEmpty(),
        )
        DayStatus.UNKNOWN -> copy(status = DayStatus.UNKNOWN)
    }

    fun editShiftStart(shiftIndex: Int, newStart: LocalTime): EditableDayState {
        val updated = shifts.toMutableList()
        updated[shiftIndex] = updated[shiftIndex].copy(start = newStart)
        return copy(shifts = updated)
    }

    fun editShiftEnd(shiftIndex: Int, newEnd: LocalTime): EditableDayState {
        val updated = shifts.toMutableList()
        updated[shiftIndex] = updated[shiftIndex].copy(end = newEnd)
        return copy(shifts = updated)
    }

    fun toggleAlarm(): EditableDayState = copy(alarmEnabled = !alarmEnabled)

    val hasMidnightCrossing: Boolean
        get() = shifts.any { it.end < it.start }
}

fun EmployeeSchedule.toEditableDays(): List<EditableDayState> = days.map { day ->
    EditableDayState(
        date = day.date,
        originalShifts = day.shifts,
        status = day.status,
        shifts = day.shifts,
        alarmEnabled = day.status == DayStatus.WORK && day.shifts.isNotEmpty(),
    )
}
