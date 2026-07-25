package com.wesley.cuadrantes.alarm

import com.google.common.truth.Truth.assertThat
import com.wesley.cuadrantes.model.AlarmPlan
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AlarmStoreTest {

    private val codec = AlarmPlanCodec

    @Test
    fun `encode and decode roundtrip preserves all fields`() {
        val plan = AlarmPlan(
            date = LocalDate.of(2026, 7, 27),
            time = LocalTime.of(9, 0),
            label = "Turno 10:00–15:00",
            enabled = true,
        )
        val encoded = codec.encode(plan)
        val decoded = codec.decode(encoded)
        assertThat(decoded).isEqualTo(plan)
    }

    @Test
    fun `encode and decode roundtrip with midnight-edge time`() {
        val plan = AlarmPlan(
            date = LocalDate.of(2026, 8, 2),
            time = LocalTime.of(0, 15),
            label = "Turno 01:15–09:00",
        )
        val encoded = codec.encode(plan)
        val decoded = codec.decode(encoded)
        assertThat(decoded).isEqualTo(plan)
    }

    @Test
    fun `decode returns null for malformed string`() {
        assertThat(codec.decode("not-valid")).isNull()
        assertThat(codec.decode("")).isNull()
        assertThat(codec.decode("1|2")).isNull()
    }

    @Test
    fun `encode produces a string with no newlines`() {
        val plan = AlarmPlan(LocalDate.of(2026, 7, 27), LocalTime.of(9, 0), "Turno 10:00")
        assertThat(codec.encode(plan)).doesNotContain("\n")
    }
}
