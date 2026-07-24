package com.wesley.cuadrantes.parser

private val TOKEN_PATTERN =
    Regex(""""text"\s*:\s*"([^"]*)"\s*,\s*"x"\s*:\s*([\d.]+)\s*,\s*"y"\s*:\s*([\d.]+)""")

fun loadFixtureTokens(filename: String): List<PositionedText> {
    val json = ClassLoader.getSystemResourceAsStream("fixtures/$filename")
        ?: error("Fixture no encontrado: fixtures/$filename")
    val text = json.bufferedReader(Charsets.UTF_8).readText()
    return TOKEN_PATTERN.findAll(text).map { m ->
        PositionedText(
            text = m.groupValues[1],
            x = m.groupValues[2].toFloat(),
            y = m.groupValues[3].toFloat(),
        )
    }.toList()
}
