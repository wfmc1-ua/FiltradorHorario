package com.wesley.cuadrantes.parser

data class GridRow(
    val metaTokens: List<PositionedText>,
    val cells: Map<Int, String>,
)
