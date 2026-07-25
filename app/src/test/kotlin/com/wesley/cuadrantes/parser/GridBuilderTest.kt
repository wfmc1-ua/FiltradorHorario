package com.wesley.cuadrantes.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class GridBuilderTest {

    private val columns = listOf(
        ColumnHeader(LocalDate.of(2026, 7, 27), turnoACenterX = 220f, turnoBCenterX = 260f),
    )
    private val firstDataRowY = 130f
    private val builder = GridBuilder()

    private fun pt(text: String, x: Float, y: Float) = PositionedText(text, x, y)

    @Test
    fun `rows above firstDataRowY are ignored`() {
        val rows = listOf(
            listOf(pt("HEADER", 10f, 100f)),
            listOf(pt("1", 10f, 140f), pt("PEREZ", 50f, 140f)),
        )
        val result = builder.build(rows, columns, firstDataRowY)
        assertThat(result).hasSize(1)
        assertThat(result[0].metaTokens.map { it.text }).contains("PEREZ")
    }

    @Test
    fun `rows after VAN A section separator are discarded`() {
        val rows = listOf(
            listOf(pt("1", 10f, 140f), pt("GARCIA", 50f, 140f)),
            listOf(pt("2", 10f, 150f), pt("LOPEZ", 50f, 150f)),
            listOf(pt("VAN", 10f, 160f), pt("A", 30f, 160f), pt("GRAN", 50f, 160f), pt("VIA", 70f, 160f)),
            listOf(pt("1", 10f, 170f), pt("GARCIA", 50f, 170f)),  // displaced — must be excluded
        )
        val result = builder.build(rows, columns, firstDataRowY)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.metaTokens.first().text }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `VAN A separator as merged token is also detected`() {
        val rows = listOf(
            listOf(pt("1", 10f, 140f), pt("PEREZ", 50f, 140f)),
            listOf(pt("VAN A GRAN VIA", 10f, 150f)),
            listOf(pt("1", 10f, 160f), pt("PEREZ", 50f, 160f)),  // displaced — must be excluded
        )
        val result = builder.build(rows, columns, firstDataRowY)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `other destinations also stop processing (VAN A PASEO)`() {
        val rows = listOf(
            listOf(pt("1", 10f, 140f), pt("GARCIA", 50f, 140f)),
            listOf(pt("VAN", 10f, 150f), pt("A", 30f, 150f), pt("PASEO", 50f, 150f)),
            listOf(pt("1", 10f, 160f), pt("GARCIA", 50f, 160f)),
        )
        val result = builder.build(rows, columns, firstDataRowY)
        assertThat(result).hasSize(1)
    }
}
