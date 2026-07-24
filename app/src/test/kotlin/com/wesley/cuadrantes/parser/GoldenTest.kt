package com.wesley.cuadrantes.parser

import com.google.common.truth.Truth.assertThat
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.Shift
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class GoldenTest {

    private val tokens by lazy { loadFixtureTokens("cuadrante_plaza_mar_sem31.json") }
    private val cuadrante by lazy { CuadranteParser().parse(tokens, year = 2026) }

    // --- Wesley Fabian Murillo Castro: días libres intercalados, sábado en Turno B ---

    @Test
    fun `wesley - semana empieza el 27 de julio`() {
        val w = findWesley()
        assertThat(w.days).hasSize(7)
        assertThat(w.days.first().date).isEqualTo(LocalDate.of(2026, 7, 27))
    }

    @Test
    fun `wesley - lunes trabaja 10h00 a 15h00`() {
        val day = findWesley().days[0]
        assertThat(day.date).isEqualTo(LocalDate.of(2026, 7, 27))
        assertThat(day.status).isEqualTo(DayStatus.WORK)
        assertThat(day.shifts).containsExactly(Shift(LocalTime.of(10, 0), LocalTime.of(15, 0)))
    }

    @Test
    fun `wesley - martes y miercoles son dias libres`() {
        val days = findWesley().days
        assertThat(days[1].status).isEqualTo(DayStatus.FREE)
        assertThat(days[1].shifts).isEmpty()
        assertThat(days[2].status).isEqualTo(DayStatus.FREE)
        assertThat(days[2].shifts).isEmpty()
    }

    @Test
    fun `wesley - jueves trabaja 9h00 a 14h30`() {
        val day = findWesley().days[3]
        assertThat(day.date).isEqualTo(LocalDate.of(2026, 7, 30))
        assertThat(day.status).isEqualTo(DayStatus.WORK)
        assertThat(day.shifts).containsExactly(Shift(LocalTime.of(9, 0), LocalTime.of(14, 30)))
    }

    @Test
    fun `wesley - viernes trabaja 10h00 a 15h00`() {
        val day = findWesley().days[4]
        assertThat(day.status).isEqualTo(DayStatus.WORK)
        assertThat(day.shifts).containsExactly(Shift(LocalTime.of(10, 0), LocalTime.of(15, 0)))
    }

    @Test
    fun `wesley - sabado turno de tarde 15h00 a 22h00`() {
        val day = findWesley().days[5]
        assertThat(day.date).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(day.status).isEqualTo(DayStatus.WORK)
        assertThat(day.shifts).containsExactly(Shift(LocalTime.of(15, 0), LocalTime.of(22, 0)))
    }

    @Test
    fun `wesley - domingo trabaja 10h00 a 15h00`() {
        val day = findWesley().days[6]
        assertThat(day.date).isEqualTo(LocalDate.of(2026, 8, 2))
        assertThat(day.status).isEqualTo(DayStatus.WORK)
        assertThat(day.shifts).containsExactly(Shift(LocalTime.of(10, 0), LocalTime.of(15, 0)))
    }

    // --- SANCHEZ GARCIA CRISTINA: semana completa de vacaciones ---

    @Test
    fun `sanchez - todos los dias son vacaciones`() {
        val employees = NameMatcher().findBestMatch("Sanchez Garcia Cristina", cuadrante.employees)
        assertThat(employees).hasSize(1)
        val schedule = employees.single()
        schedule.days.forEach { day ->
            assertThat(day.status).isEqualTo(DayStatus.VACATION)
            assertThat(day.shifts).isEmpty()
        }
    }

    // --- CORONEL SOTO ALEXA: miércoles y jueves libres, resto trabaja 7h00-15h00 ---

    @Test
    fun `coronel - miercoles y jueves libres`() {
        val schedule = findCoronel()
        assertThat(schedule.days[2].status).isEqualTo(DayStatus.FREE)
        assertThat(schedule.days[3].status).isEqualTo(DayStatus.FREE)
    }

    @Test
    fun `coronel - dias laborables con turno 7h00 a 15h00`() {
        val schedule = findCoronel()
        val workDays = listOf(0, 1, 4, 5, 6)
        workDays.forEach { i ->
            val day = schedule.days[i]
            assertThat(day.status).isEqualTo(DayStatus.WORK)
            assertThat(day.shifts).containsExactly(Shift(LocalTime.of(7, 0), LocalTime.of(15, 0)))
        }
    }

    // --- Turno partido (datos sintéticos — este PDF no tiene ninguno) ---

    @Test
    fun `turno partido sintetico - dos shifts en el mismo dia`() {
        val mapper = EmployeeRowMapper()
        val columns = listOf(
            ColumnHeader(
                date = java.time.LocalDate.of(2026, 7, 27),
                turnoACenterX = 100f,
                turnoBCenterX = 140f,
            )
        )
        val row = GridRow(
            metaTokens = listOf(
                PositionedText("1", 24f, 0f),
                PositionedText("PEREZ", 80f, 0f),
            ),
            cells = mapOf(
                0 to "9:00 14:00",
                1 to "16:00 21:00",
            ),
        )
        val schedule = mapper.map(row, columns)!!
        assertThat(schedule.days).hasSize(1)
        assertThat(schedule.days[0].status).isEqualTo(DayStatus.WORK)
        assertThat(schedule.days[0].shifts).containsExactly(
            Shift(LocalTime.of(9, 0), LocalTime.of(14, 0)),
            Shift(LocalTime.of(16, 0), LocalTime.of(21, 0)),
        ).inOrder()
    }

    // --- helpers ---

    private fun findWesley() =
        NameMatcher().findBestMatch("Wesley Murillo", cuadrante.employees)
            .also { assertThat(it).hasSize(1) }
            .single()

    private fun findCoronel() =
        NameMatcher().findBestMatch("Coronel Soto Alexa", cuadrante.employees)
            .also { assertThat(it).hasSize(1) }
            .single()
}
