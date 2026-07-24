package com.wesley.cuadrantes.parser

import com.wesley.cuadrantes.model.Cuadrante

class CuadranteParser(
    private val rowClusterer: RowClusterer = RowClusterer(),
    private val headerDetector: HeaderDetector = HeaderDetector(),
    private val gridBuilder: GridBuilder = GridBuilder(),
    private val employeeRowMapper: EmployeeRowMapper = EmployeeRowMapper(),
) {

    fun parse(tokens: List<PositionedText>, year: Int): Cuadrante {
        val rows = rowClusterer.cluster(tokens)
        val header = headerDetector.detect(rows, year)
        val gridRows = gridBuilder.build(rows, header.columns, header.firstDataRowY)
        val employees = gridRows.mapNotNull { gridRow ->
            employeeRowMapper.map(gridRow, header.columns)
        }
        return Cuadrante(
            weekStart = header.columns.first().date,
            center = header.center,
            employees = employees,
        )
    }
}
