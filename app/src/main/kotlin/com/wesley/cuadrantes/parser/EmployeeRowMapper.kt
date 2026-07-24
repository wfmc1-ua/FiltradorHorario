package com.wesley.cuadrantes.parser

import com.wesley.cuadrantes.model.DaySchedule
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.EmployeeSchedule
import com.wesley.cuadrantes.model.Shift
import java.time.LocalTime

private val ID_PATTERN = Regex("""^[A-Z0-9]\d{7}[A-Z0-9]$""")
private val TIME_PATTERN = Regex("""^(\d{1,2}):(\d{2})$""")
private val SHIFT_PATTERN = Regex("""(\d{1,2}:\d{2})\s+(\d{1,2}:\d{2})""")
private val HOURS_PATTERN = Regex("""^\d+$""")

private val DISCARDED_NAMES = setOf(
    "INVENTARIO", "TOTAL", "TOTALES",
)

class EmployeeRowMapper {

    fun map(row: GridRow, columns: List<ColumnHeader>): EmployeeSchedule? {
        val meta = row.metaTokens

        // Must have a row number as first token (small positive integer)
        val rowNumToken = meta.firstOrNull() ?: return null
        if (!rowNumToken.text.matches(Regex("""^\d{1,3}$"""))) return null
        val rowNum = rowNumToken.text.toInt()
        if (rowNum < 1 || rowNum > 200) return null

        // Remaining meta tokens after the row number
        val afterNum = meta.drop(1)
        if (afterNum.isEmpty()) return null

        // Check for discardable rows
        val firstText = afterNum.firstOrNull()?.text?.uppercase() ?: return null
        if (firstText in DISCARDED_NAMES) return null

        // ID: optional, matches DNI/NIE pattern
        var idx = 0
        var rawId: String? = null
        if (ID_PATTERN.matches(afterNum[idx].text)) {
            rawId = afterNum[idx].text
            idx++
        }

        // Name: consecutive uppercase-looking tokens
        val nameTokens = mutableListOf<String>()
        while (idx < afterNum.size) {
            val t = afterNum[idx].text
            if (t.all { it.isLetter() || it == '\'' || it == '-' } && t.length > 1) {
                nameTokens.add(t.uppercase())
                idx++
            } else break
        }
        if (nameTokens.isEmpty()) return null
        val name = nameTokens.joinToString(" ")

        // Contract hours: next integer after name, if present
        val contractHours: Int? = if (idx < afterNum.size) {
            val t = afterNum[idx].text
            if (HOURS_PATTERN.matches(t)) t.toInt().also { idx++ } else null
        } else null

        // Map cells to DaySchedules
        val days = columns.mapIndexed { i, col ->
            val turnoAContent = row.cells[i * 2] ?: ""
            val turnoBContent = row.cells[i * 2 + 1] ?: ""

            val status = when {
                turnoAContent == "L" || turnoBContent == "L" -> DayStatus.FREE
                turnoAContent == "V" -> DayStatus.VACATION
                turnoAContent.isEmpty() && turnoBContent.isEmpty() -> DayStatus.UNKNOWN
                else -> DayStatus.WORK
            }

            val shifts = if (status == DayStatus.WORK) {
                listOfNotNull(
                    parseShift(turnoAContent),
                    parseShift(turnoBContent),
                )
            } else emptyList()

            DaySchedule(date = col.date, status = status, shifts = shifts)
        }

        return EmployeeSchedule(
            rawId = rawId,
            name = name,
            contractHours = contractHours,
            days = days,
        )
    }

    private fun parseShift(cell: String): Shift? {
        if (cell.isBlank() || cell == "L" || cell == "V") return null
        val match = SHIFT_PATTERN.find(cell) ?: return null
        val start = parseTime(match.groupValues[1]) ?: return null
        val end = parseTime(match.groupValues[2]) ?: return null
        return Shift(start, end)
    }

    private fun parseTime(text: String): LocalTime? {
        val m = TIME_PATTERN.matchEntire(text) ?: return null
        return LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
    }
}
