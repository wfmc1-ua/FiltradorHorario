package com.wesley.cuadrantes.parser

class GridBuilder {

    fun build(
        rows: List<List<PositionedText>>,
        columns: List<ColumnHeader>,
        firstDataRowY: Float,
    ): List<GridRow> {
        val firstColumnLeft = columns.first().turnoACenterX - 20f
        val lastColumnRight = columns.last().turnoBCenterX + 40f

        val allColumnCenters: List<Pair<Int, Float>> = columns.flatMapIndexed { i, col ->
            listOf(i * 2 to col.turnoACenterX, i * 2 + 1 to col.turnoBCenterX)
        }

        return rows
            .filter { row -> row.first().y >= firstDataRowY - 0.5f }
            .map { row ->
                val meta = row.filter { it.x < firstColumnLeft }
                val cellTokens = row.filter { it.x in firstColumnLeft..lastColumnRight }

                val cellMap = mutableMapOf<Int, MutableList<String>>()
                for (token in cellTokens) {
                    val colIdx = allColumnCenters.minByOrNull { (_, cx) ->
                        kotlin.math.abs(token.x - cx)
                    }!!.first
                    cellMap.getOrPut(colIdx) { mutableListOf() }.add(token.text)
                }

                GridRow(
                    metaTokens = meta,
                    cells = cellMap.mapValues { (_, texts) -> texts.joinToString(" ") },
                )
            }
    }
}
