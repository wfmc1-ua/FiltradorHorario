package com.wesley.cuadrantes.parser

import com.google.common.truth.Truth.assertThat
import com.wesley.cuadrantes.model.DaySchedule
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.EmployeeSchedule
import org.junit.Test
import java.time.LocalDate

class NameMatcherTest {

    private val matcher = NameMatcher()
    private val employees = listOf(
        employee("WESLEY FABIAN MURILLO CASTRO"),
        employee("GONZALEZ PINOS JUAN MANUEL"),
        employee("SANCHEZ GARCIA CRISTINA"),
    )

    @Test
    fun `full exact match finds employee`() {
        val result = matcher.findBestMatch("WESLEY FABIAN MURILLO CASTRO", employees)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("WESLEY FABIAN MURILLO CASTRO")
    }

    @Test
    fun `partial match with subset of tokens finds employee`() {
        val result = matcher.findBestMatch("Wesley Murillo", employees)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("WESLEY FABIAN MURILLO CASTRO")
    }

    @Test
    fun `match ignores diacritics`() {
        val employees2 = listOf(employee("MÁRQUEZ PÉREZ ANA"))
        val result = matcher.findBestMatch("Marquez Perez", employees2)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `match is case insensitive`() {
        val result = matcher.findBestMatch("gonzalez pinos", employees)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("GONZALEZ PINOS JUAN MANUEL")
    }

    @Test
    fun `query token not in any name returns empty`() {
        val result = matcher.findBestMatch("Rodriguez", employees)
        assertThat(result).isEmpty()
    }

    @Test
    fun `ambiguous query returns multiple results`() {
        val ambiguous = listOf(
            employee("GARCIA MARTINEZ ANA"),
            employee("GARCIA LOPEZ PEDRO"),
        )
        val result = matcher.findBestMatch("Garcia", ambiguous)
        assertThat(result).hasSize(2)
    }

    private fun employee(name: String) = EmployeeSchedule(
        rawId = null,
        name = name,
        contractHours = null,
        days = List(7) { DaySchedule(LocalDate.now().plusDays(it.toLong()), DayStatus.UNKNOWN, emptyList()) },
    )
}
