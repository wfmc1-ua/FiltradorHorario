package com.wesley.cuadrantes.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wesley.cuadrantes.AppContainer
import com.wesley.cuadrantes.alarm.AlarmManagerScheduler
import com.wesley.cuadrantes.alarm.PlannedAlarm
import com.wesley.cuadrantes.model.DayStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime

class ReviewViewModel(
    private val container: AppContainer,
    employeeIndex: Int,
) : ViewModel() {

    val employeeName: String

    private val _days = MutableStateFlow<List<EditableDayState>>(emptyList())
    val days: StateFlow<List<EditableDayState>> = _days

    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex

    private val _plannedAlarms = MutableStateFlow<List<PlannedAlarm>?>(null)
    val plannedAlarms: StateFlow<List<PlannedAlarm>?> = _plannedAlarms

    // true si AlarmManager no tiene permiso de alarmas exactas (API 31+)
    val needsExactAlarmPermission: Boolean
        get() = (container.alarmScheduler as? AlarmManagerScheduler)
            ?.canScheduleExact() == false

    // true si el permiso USE_FULL_SCREEN_INTENT no está concedido (Android 14+)
    val needsFullScreenIntentPermission: Boolean
        get() = (container.alarmScheduler as? AlarmManagerScheduler)
            ?.canUseFullScreenIntent() == false

    init {
        val schedule = container.currentCuadrante!!.employees[employeeIndex]
        employeeName = schedule.name
        _days.value = schedule.toEditableDays()
    }

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index
    }

    fun changeStatus(dayIndex: Int, status: DayStatus) {
        _days.update { list ->
            list.toMutableList().also { it[dayIndex] = it[dayIndex].changeStatus(status) }
        }
    }

    fun editShiftStart(dayIndex: Int, shiftIndex: Int, newStart: LocalTime) {
        _days.update { list ->
            list.toMutableList().also {
                it[dayIndex] = it[dayIndex].editShiftStart(shiftIndex, newStart)
            }
        }
    }

    fun editShiftEnd(dayIndex: Int, shiftIndex: Int, newEnd: LocalTime) {
        _days.update { list ->
            list.toMutableList().also {
                it[dayIndex] = it[dayIndex].editShiftEnd(shiftIndex, newEnd)
            }
        }
    }

    fun toggleAlarm(dayIndex: Int) {
        _days.update { list ->
            list.toMutableList().also { it[dayIndex] = it[dayIndex].toggleAlarm() }
        }
    }

    fun requestAlarmConfirmation() {
        _plannedAlarms.value = container.alarmPlanner.plan(_days.value)
    }

    fun dismissAlarmConfirmation() {
        _plannedAlarms.value = null
    }

    fun confirmAlarms(selected: List<PlannedAlarm>) {
        selected.filter { !it.isPast }.forEach { container.alarmScheduler.schedule(it.plan) }
        _plannedAlarms.value = null
    }

    val enabledAlarmCount: Int
        get() = _days.value.count { it.status == DayStatus.WORK && it.alarmEnabled }

    class Factory(
        private val container: AppContainer,
        private val employeeIndex: Int,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReviewViewModel(container, employeeIndex) as T
    }
}
