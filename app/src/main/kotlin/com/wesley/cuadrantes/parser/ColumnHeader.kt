package com.wesley.cuadrantes.parser

import java.time.LocalDate

data class ColumnHeader(
    val date: LocalDate,
    val turnoACenterX: Float,
    val turnoBCenterX: Float,
) {
    val splitX: Float get() = (turnoACenterX + turnoBCenterX) / 2f
}
