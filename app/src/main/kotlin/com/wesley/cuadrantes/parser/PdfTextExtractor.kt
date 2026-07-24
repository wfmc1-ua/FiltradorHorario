package com.wesley.cuadrantes.parser

import java.io.InputStream

interface PdfTextExtractor {
    fun extract(input: InputStream): List<PositionedText>
}
