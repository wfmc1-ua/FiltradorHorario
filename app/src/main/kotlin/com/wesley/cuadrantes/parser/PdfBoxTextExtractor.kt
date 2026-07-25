package com.wesley.cuadrantes.parser

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.InputStream

class PdfBoxTextExtractor : PdfTextExtractor {

    override fun extract(input: InputStream): List<PositionedText> {
        val result = mutableListOf<PositionedText>()
        PDDocument.load(input).use { doc ->
            val stripper = PositionedStripper(result)
            stripper.sortByPosition = true
            stripper.getText(doc)
        }
        return result
    }

    private class PositionedStripper(
        private val result: MutableList<PositionedText>,
    ) : PDFTextStripper() {

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return
            val pos = textPositions.firstOrNull() ?: return
            result.add(PositionedText(trimmed, pos.xDirAdj, pos.yDirAdj))
        }

        // Suprimir la escritura al Writer interno pero NO tocar writePage():
        // writePage() es el que agrupa posiciones y llama a nuestro writeString.
        override fun writeString(text: String) {}
        override fun writeLineSeparator() {}
        override fun writeWordSeparator() {}
        override fun writeCharacters(text: TextPosition) {}
        override fun writePageStart() {}
        override fun writePageEnd() {}
        override fun startDocument(document: PDDocument) {}
        override fun endDocument(document: PDDocument) {}
    }
}
