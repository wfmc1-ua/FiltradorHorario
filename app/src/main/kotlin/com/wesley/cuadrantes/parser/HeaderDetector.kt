package com.wesley.cuadrantes.parser

import java.time.LocalDate
import java.time.Month

private val MONTH_MAP = mapOf(
    "ene" to Month.JANUARY, "feb" to Month.FEBRUARY, "mar" to Month.MARCH,
    "abr" to Month.APRIL, "may" to Month.MAY, "jun" to Month.JUNE,
    "jul" to Month.JULY, "ago" to Month.AUGUST, "sep" to Month.SEPTEMBER,
    "oct" to Month.OCTOBER, "nov" to Month.NOVEMBER, "dic" to Month.DECEMBER,
)

private val DATE_PATTERN = Regex("""^(\d{1,2})-([a-zA-Z]{3})$""")

class HeaderDetector {

    data class Result(
        val center: String?,
        val columns: List<ColumnHeader>,
        val firstDataRowY: Float,
    )

    fun detect(rows: List<List<PositionedText>>, year: Int): Result {
        val dateRow = rows.firstOrNull { row ->
            row.count { DATE_PATTERN.matches(it.text) } >= 5
        } ?: error("No se encontró la fila de fechas en el PDF")

        val turnoRow = rows.firstOrNull { row ->
            row.count { it.text.equals("Turno", ignoreCase = true) } >= 10
        } ?: error("No se encontró la fila de Turnos en el PDF")

        val dates = dateRow
            .filter { DATE_PATTERN.matches(it.text) }
            .sortedBy { it.x }
            .map { token ->
                val (day, monthStr) = DATE_PATTERN.find(token.text)!!.destructured
                val month = MONTH_MAP[monthStr.lowercase()]
                    ?: error("Mes desconocido: $monthStr")
                Pair(token.x, LocalDate.of(year, month, day.toInt()))
            }

        val turnoCenters = turnoRow
            .filter { it.text.equals("Turno", ignoreCase = true) }
            .sortedBy { it.x }
            .map { it.x }

        if (turnoCenters.size < 14) error(
            "Se esperaban 14 columnas Turno pero se encontraron ${turnoCenters.size}"
        )

        val columns = dates.mapIndexed { i, (_, date) ->
            ColumnHeader(
                date = date,
                turnoACenterX = turnoCenters[i * 2],
                turnoBCenterX = turnoCenters[i * 2 + 1],
            )
        }

        val center = findCenter(rows, dateRow.minOf { it.y })

        val turnoRowY = turnoRow.first().y
        val firstDataRowY = rows
            .map { it.first().y }
            .filter { it > turnoRowY }
            .minOrNull() ?: error("No hay filas de datos tras las cabeceras")

        return Result(center, columns, firstDataRowY)
    }

    private fun findCenter(rows: List<List<PositionedText>>, beforeY: Float): String? {
        val candidates = rows
            .filter { row -> row.all { it.y < beforeY } }
            .flatMap { row ->
                row.filter { it.x < 300 && it.text.all { c -> c.isLetter() || c.isWhitespace() } }
            }
            .map { it.text.uppercase() }
            .filter { it.length > 2 && it !in setOf("EL", "LA", "DE", "DEL", "LOS", "LAS") }
        return if (candidates.isEmpty()) null else candidates.joinToString(" ")
    }
}
