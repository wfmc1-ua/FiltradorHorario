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

        val result = mutableListOf<GridRow>()
        for (row in rows) {
            if (row.first().y < firstDataRowY - 0.5f) continue
            if (isDisplacementSection(row)) break

            val meta = row.filter { it.x < firstColumnLeft }
            val cellTokens = row.filter { it.x in firstColumnLeft..lastColumnRight }

            val cellMap = mutableMapOf<Int, MutableList<String>>()
            for (token in cellTokens) {
                val colIdx = allColumnCenters.minByOrNull { (_, cx) ->
                    kotlin.math.abs(token.x - cx)
                }!!.first
                cellMap.getOrPut(colIdx) { mutableListOf() }.add(token.text)
            }

            result.add(GridRow(
                metaTokens = meta,
                cells = cellMap.mapValues { (_, texts) -> texts.joinToString(" ") },
            ))
        }
        return result
    }

    // Detecta cabeceras de sección de desplazamiento ("VAN A GRAN VIA", "VAN A PASEO…").
    // Cuando aparece una, el cuadrante principal ha terminado: las filas que siguen
    // son personal desplazado ya incluido en el cuadrante principal.
    private fun isDisplacementSection(row: List<PositionedText>): Boolean {
        val upper = row.joinToString(" ") { it.text }.uppercase()
        return "VAN A" in upper
    }
}
