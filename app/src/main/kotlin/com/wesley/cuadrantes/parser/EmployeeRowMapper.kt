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

        // Extract row number from start of first meta token.
        // On Android PDFBox merges nearby tokens: "4" + "Y6853762Y" → "4Y6853762Y".
        val firstTokenText = meta.firstOrNull()?.text?.trim() ?: return null
        val rowNumLen = firstTokenText.takeWhile { it.isDigit() }.length
        if (rowNumLen == 0 || rowNumLen > 3) return null
        val rowNum = firstTokenText.take(rowNumLen).toInt()
        if (rowNum < 1 || rowNum > 200) return null

        // Any content after the digits in the first token gets re-injected as the first element
        val remainder = firstTokenText.drop(rowNumLen).trim()
        val afterNum: List<PositionedText> = if (remainder.isEmpty()) {
            meta.drop(1)
        } else {
            val pos = meta.first()
            listOf(PositionedText(remainder, pos.x, pos.y)) + meta.drop(1)
        }
        if (afterNum.isEmpty()) return null

        // Discard non-employee rows
        val firstWord = afterNum.first().text.trim()
            .split(Regex("\\s+")).firstOrNull()?.uppercase() ?: return null
        if (firstWord in DISCARDED_NAMES) return null

        // ID: optional DNI/NIE
        var idx = 0
        var rawId: String? = null
        if (ID_PATTERN.matches(afterNum[idx].text.trim())) {
            rawId = afterNum[idx].text.trim()
            idx++
        }

        // Name: one or more tokens whose words are all purely alphabetic.
        // Handles both separate ("CORONEL", "SOTO") and merged ("CORONEL SOTO ALEXA") tokens.
        val nameTokens = mutableListOf<String>()
        while (idx < afterNum.size) {
            val t = afterNum[idx].text.trim()
            val words = t.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val isNamePart = words.isNotEmpty() && words.all { w ->
                w.all { c -> c.isLetter() || c == '\'' || c == '-' } && w.length > 1
            }
            if (isNamePart) {
                nameTokens.addAll(words.map { it.uppercase() })
                idx++
            } else break
        }
        if (nameTokens.isEmpty()) return null
        val name = nameTokens.joinToString(" ")

        // Contract hours: next integer after name, if present
        val contractHours: Int? = if (idx < afterNum.size) {
            val t = afterNum[idx].text.trim()
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
