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
            reconstructDates(row).size >= 5
        } ?: error("No se encontró la fila de fechas en el PDF")

        val turnoRow = rows.firstOrNull { row ->
            reconstructTurnoCount(row) >= 10
        } ?: error("No se encontró la fila de Turnos en el PDF")

        val dates = reconstructDates(dateRow)
            .sortedBy { it.first }
            .map { (x, text) ->
                val (day, monthStr) = DATE_PATTERN.find(text)!!.destructured
                val month = MONTH_MAP[monthStr.lowercase()]
                    ?: error("Mes desconocido: $monthStr")
                Pair(x, LocalDate.of(year, month, day.toInt()))
            }

        val turnoCenters = reconstructTurnoPositions(turnoRow)

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

    // Combina 1, 2 o 3 tokens consecutivos para reconstruir fechas "dd-mmm".
    // PDFBox puede partir "27-jul" en ["27", "-", "jul"] o ["27", "-jul"].
    private fun reconstructDates(row: List<PositionedText>): List<Pair<Float, String>> {
        val result = mutableListOf<Pair<Float, String>>()
        var i = 0
        while (i < row.size) {
            val t1 = row[i].text
            if (DATE_PATTERN.matches(t1)) {
                result += Pair(row[i].x, t1)
                i++
                continue
            }
            if (i + 1 < row.size) {
                val t2 = t1 + row[i + 1].text
                if (DATE_PATTERN.matches(t2)) {
                    result += Pair(row[i].x, t2)
                    i += 2
                    continue
                }
            }
            if (i + 2 < row.size) {
                val t3 = t1 + row[i + 1].text + row[i + 2].text
                if (DATE_PATTERN.matches(t3)) {
                    result += Pair(row[i].x, t3)
                    i += 3
                    continue
                }
            }
            i++
        }
        return result
    }

    // Devuelve las posiciones X de cada columna Turno.
    // Cubre tokens separados ("Turno" + "A") y fusionados ("Turno A", "Turno B").
    private fun reconstructTurnoPositions(row: List<PositionedText>): List<Float> {
        val result = mutableListOf<Float>()
        var i = 0
        while (i < row.size) {
            val t1 = row[i].text.trim()
            when {
                // "Turno" solo, o fusionado con el turno ("Turno A", "Turno B", "Turno 1"…)
                t1.startsWith("Turno", ignoreCase = true) -> { result += row[i].x; i++ }
                // dos fragmentos que juntos forman "Turno"
                i + 1 < row.size &&
                    (t1 + row[i + 1].text.trim()).equals("Turno", ignoreCase = true) -> {
                    result += row[i].x; i += 2
                }
                else -> i++
            }
        }
        return result.sorted()
    }

    private fun reconstructTurnoCount(row: List<PositionedText>): Int =
        reconstructTurnoPositions(row).size

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
