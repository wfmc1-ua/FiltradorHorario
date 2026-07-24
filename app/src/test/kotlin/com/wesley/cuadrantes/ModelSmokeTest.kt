package com.wesley.cuadrantes

import com.google.common.truth.Truth.assertThat
import com.wesley.cuadrantes.model.AlarmPlan
import com.wesley.cuadrantes.model.Cuadrante
import com.wesley.cuadrantes.model.DaySchedule
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.EmployeeSchedule
import com.wesley.cuadrantes.model.Shift
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ModelSmokeTest {

    @Test
    fun `model data classes compile and hold values correctly`() {
        val shift = Shift(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val day = DaySchedule(LocalDate.of(2026, 7, 28), DayStatus.WORK, listOf(shift))
        val employee = EmployeeSchedule(rawId = "12345678A", name = "WESLEY MURILLO", contractHours = 40, days = listOf(day))
        val cuadrante = Cuadrante(weekStart = LocalDate.of(2026, 7, 28), center = "PLAZA MAR", employees = listOf(employee))

        assertThat(cuadrante.employees).hasSize(1)
        assertThat(cuadrante.employees[0].days[0].shifts).hasSize(1)
        assertThat(cuadrante.employees[0].days[0].shifts[0].start).isEqualTo(LocalTime.of(9, 0))
    }

    @Test
    fun `fixture pdf exists in test resources`() {
        val stream = javaClass.classLoader!!.getResourceAsStream("fixtures/cuadrante_plaza_mar_sem31.pdf")
        assertThat(stream).isNotNull()
        stream!!.close()
    }

    @Test
    fun `alarm plan defaults to enabled`() {
        val plan = AlarmPlan(LocalDate.of(2026, 7, 28), LocalTime.of(8, 0), "Turno 09:00-17:00")
        assertThat(plan.enabled).isTrue()
    }
}
