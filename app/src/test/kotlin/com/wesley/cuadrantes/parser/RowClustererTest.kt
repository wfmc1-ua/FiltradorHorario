package com.wesley.cuadrantes.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RowClustererTest {

    private val clusterer = RowClusterer(yTolerance = 3f)

    @Test
    fun `empty input returns empty list`() {
        assertThat(clusterer.cluster(emptyList())).isEmpty()
    }

    @Test
    fun `tokens within tolerance are grouped together`() {
        val tokens = listOf(
            PositionedText("A", 10f, 100f),
            PositionedText("B", 50f, 101.5f),
            PositionedText("C", 90f, 102f),
        )
        val result = clusterer.cluster(tokens)
        assertThat(result).hasSize(1)
        assertThat(result[0].map { it.text }).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun `tokens beyond tolerance form separate rows`() {
        val tokens = listOf(
            PositionedText("Row1", 10f, 100f),
            PositionedText("Row2", 10f, 110f),
        )
        val result = clusterer.cluster(tokens)
        assertThat(result).hasSize(2)
        assertThat(result[0][0].text).isEqualTo("Row1")
        assertThat(result[1][0].text).isEqualTo("Row2")
    }

    @Test
    fun `tokens within a row are sorted by x`() {
        val tokens = listOf(
            PositionedText("C", 90f, 100f),
            PositionedText("A", 10f, 100f),
            PositionedText("B", 50f, 100f),
        )
        val result = clusterer.cluster(tokens)
        assertThat(result).hasSize(1)
        assertThat(result[0].map { it.text }).containsExactly("A", "B", "C").inOrder()
    }

    @Test
    fun `three distinct rows are separated correctly`() {
        val tokens = (1..3).flatMap { row ->
            listOf(
                PositionedText("L${row}A", 10f, row * 20f),
                PositionedText("L${row}B", 50f, row * 20f + 1f),
            )
        }
        val result = clusterer.cluster(tokens)
        assertThat(result).hasSize(3)
        result.forEach { assertThat(it).hasSize(2) }
    }
}
