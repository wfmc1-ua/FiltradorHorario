package com.wesley.cuadrantes.parser

import kotlin.math.abs

class RowClusterer(val yTolerance: Float = 3f) {

    fun cluster(tokens: List<PositionedText>): List<List<PositionedText>> {
        if (tokens.isEmpty()) return emptyList()

        val sorted = tokens.sortedBy { it.y }
        val rows = mutableListOf<MutableList<PositionedText>>()
        var current = mutableListOf(sorted.first())

        for (token in sorted.drop(1)) {
            val rowY = current.sumOf { it.y.toDouble() }.toFloat() / current.size
            if (abs(token.y - rowY) <= yTolerance) {
                current.add(token)
            } else {
                rows.add(current)
                current = mutableListOf(token)
            }
        }
        rows.add(current)

        return rows.map { row -> row.sortedBy { it.x } }
    }
}
