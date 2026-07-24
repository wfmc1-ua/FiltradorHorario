package com.wesley.cuadrantes.ui.review

import com.google.common.truth.Truth.assertThat
import com.wesley.cuadrantes.model.DaySchedule
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.EmployeeSchedule
import com.wesley.cuadrantes.model.Shift
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EditableDayStateTest {

    private val monday = LocalDate.of(2026, 7, 27)
    private val shift9to14 = Shift(LocalTime.of(9, 0), LocalTime.of(14, 0))
    private val shift16to21 = Shift(LocalTime.of(16, 0), LocalTime.of(21, 0))

    private fun workDay(shifts: List<Shift> = listOf(shift9to14)) = EditableDayState(
        date = monday,
        originalShifts = shifts,
        status = DayStatus.WORK,
        shifts = shifts,
        alarmEnabled = true,
    )

    private fun freeDay() = EditableDayState(
        date = monday,
        originalShifts = emptyList(),
        status = DayStatus.FREE,
        shifts = emptyList(),
        alarmEnabled = false,
    )

    @Test
    fun `changeStatus to FREE clears shifts and disables alarm`() {
        val result = workDay().changeStatus(DayStatus.FREE)
        assertThat(result.status).isEqualTo(DayStatus.FREE)
        assertThat(result.shifts).isEmpty()
        assertThat(result.alarmEnabled).isFalse()
    }

    @Test
    fun `changeStatus to VACATION clears shifts and disables alarm`() {
        val result = workDay().changeStatus(DayStatus.VACATION)
        assertThat(result.status).isEqualTo(DayStatus.VACATION)
        assertThat(result.shifts).isEmpty()
        assertThat(result.alarmEnabled).isFalse()
    }

    @Test
    fun `changeStatus back to WORK restores original shifts`() {
        val original = workDay(listOf(shift9to14))
        val freed = original.changeStatus(DayStatus.FREE)
        val restored = freed.changeStatus(DayStatus.WORK)
        assertThat(restored.shifts).containsExactly(shift9to14)
        assertThat(restored.alarmEnabled).isTrue()
    }

    @Test
    fun `editShiftStart updates only that shift`() {
        val day = workDay(listOf(shift9to14, shift16to21))
        val newStart = LocalTime.of(10, 30)
        val result = day.editShiftStart(0, newStart)
        assertThat(result.shifts[0].start).isEqualTo(newStart)
        assertThat(result.shifts[0].end).isEqualTo(shift9to14.end)
        assertThat(result.shifts[1]).isEqualTo(shift16to21)
    }

    @Test
    fun `editShiftEnd updates only that shift`() {
        val day = workDay(listOf(shift9to14))
        val newEnd = LocalTime.of(15, 30)
        val result = day.editShiftEnd(0, newEnd)
        assertThat(result.shifts[0].end).isEqualTo(newEnd)
        assertThat(result.shifts[0].start).isEqualTo(shift9to14.start)
    }

    @Test
    fun `toggleAlarm flips the flag`() {
        assertThat(workDay().toggleAlarm().alarmEnabled).isFalse()
        assertThat(freeDay().toggleAlarm().alarmEnabled).isTrue()
    }

    @Test
    fun `hasMidnightCrossing detects end before start`() {
        val midnight = workDay(listOf(Shift(LocalTime.of(22, 0), LocalTime.of(6, 0))))
        assertThat(midnight.hasMidnightCrossing).isTrue()
        assertThat(workDay().hasMidnightCrossing).isFalse()
    }

    @Test
    fun `toEditableDays maps WORK day correctly`() {
        val schedule = EmployeeSchedule(
            rawId = null,
            name = "PEREZ ANA",
            contractHours = 40,
            days = listOf(
                DaySchedule(monday, DayStatus.WORK, listOf(shift9to14)),
                DaySchedule(monday.plusDays(1), DayStatus.FREE, emptyList()),
            ),
        )
        val result = schedule.toEditableDays()
        assertThat(result).hasSize(2)
        assertThat(result[0].status).isEqualTo(DayStatus.WORK)
        assertThat(result[0].shifts).containsExactly(shift9to14)
        assertThat(result[0].alarmEnabled).isTrue()
        assertThat(result[1].status).isEqualTo(DayStatus.FREE)
        assertThat(result[1].alarmEnabled).isFalse()
    }
}
