package com.wesley.cuadrantes.alarm

import com.google.common.truth.Truth.assertThat
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.Shift
import com.wesley.cuadrantes.ui.review.EditableDayState
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AlarmPlannerTest {

    private val monday = LocalDate.of(2026, 7, 27)
    private val shift10to15 = Shift(LocalTime.of(10, 0), LocalTime.of(15, 0))
    private val shift16to21 = Shift(LocalTime.of(16, 0), LocalTime.of(21, 0))

    // Clock fijo en domingo 26 jul 08:00 → ninguna alarma de la semana está en el pasado
    private val fakeClock = FakeClock(LocalDateTime.of(2026, 7, 26, 8, 0))

    private fun planner(leadMinutes: Long = 60) = AlarmPlanner(fakeClock, leadMinutes)

    private fun workDay(
        date: LocalDate = monday,
        shifts: List<Shift> = listOf(shift10to15),
        alarmEnabled: Boolean = true,
    ) = EditableDayState(
        date = date,
        originalShifts = shifts,
        status = DayStatus.WORK,
        shifts = shifts,
        alarmEnabled = alarmEnabled,
    )

    private fun freeDay(date: LocalDate = monday) = EditableDayState(
        date = date,
        originalShifts = emptyList(),
        status = DayStatus.FREE,
        shifts = emptyList(),
        alarmEnabled = false,
    )

    @Test
    fun `work day generates alarm 60 min before first shift`() {
        val result = planner().plan(listOf(workDay()))
        assertThat(result).hasSize(1)
        assertThat(result[0].plan.time).isEqualTo(LocalTime.of(9, 0))
        assertThat(result[0].plan.date).isEqualTo(monday)
        assertThat(result[0].isPast).isFalse()
    }

    @Test
    fun `free day generates no alarm`() {
        val result = planner().plan(listOf(freeDay()))
        assertThat(result).isEmpty()
    }

    @Test
    fun `vacation day generates no alarm`() {
        val day = EditableDayState(monday, emptyList(), DayStatus.VACATION, emptyList(), false)
        assertThat(planner().plan(listOf(day))).isEmpty()
    }

    @Test
    fun `alarm disabled generates no alarm`() {
        val result = planner().plan(listOf(workDay(alarmEnabled = false)))
        assertThat(result).isEmpty()
    }

    @Test
    fun `split shift generates only one alarm for first shift`() {
        val day = workDay(shifts = listOf(shift10to15, shift16to21))
        val result = planner().plan(listOf(day))
        assertThat(result).hasSize(1)
        assertThat(result[0].plan.time).isEqualTo(LocalTime.of(9, 0)) // 10:00 - 60 min
    }

    @Test
    fun `lead minutes is configurable`() {
        val result = planner(leadMinutes = 30).plan(listOf(workDay()))
        assertThat(result[0].plan.time).isEqualTo(LocalTime.of(9, 30)) // 10:00 - 30 min
    }

    @Test
    fun `past alarm is marked isPast`() {
        // Clock en lunes 27 jul 11:00 → alarma de las 09:00 del mismo día ya pasó
        val pastClock = FakeClock(LocalDateTime.of(2026, 7, 27, 11, 0))
        val result = AlarmPlanner(pastClock).plan(listOf(workDay()))
        assertThat(result[0].isPast).isTrue()
    }

    @Test
    fun `alarm label includes shift times`() {
        val result = planner().plan(listOf(workDay()))
        assertThat(result[0].plan.label).contains("10:00")
        assertThat(result[0].plan.label).contains("15:00")
    }

    @Test
    fun `multiple days produce multiple alarms`() {
        val days = listOf(
            workDay(monday),
            freeDay(monday.plusDays(1)),
            workDay(monday.plusDays(2), listOf(Shift(LocalTime.of(9, 0), LocalTime.of(14, 0)))),
        )
        val result = planner().plan(days)
        assertThat(result).hasSize(2)
    }
}

class FakeClock(private val fixed: LocalDateTime) : com.wesley.cuadrantes.util.Clock {
    override fun now(): LocalDateTime = fixed
}
