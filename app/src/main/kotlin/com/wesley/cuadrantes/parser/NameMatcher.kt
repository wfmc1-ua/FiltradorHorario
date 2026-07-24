package com.wesley.cuadrantes.parser

import com.wesley.cuadrantes.model.EmployeeSchedule
import java.text.Normalizer

class NameMatcher {

    fun findBestMatch(query: String, employees: List<EmployeeSchedule>): List<EmployeeSchedule> {
        val queryTokens = normalize(query).split(" ").filter { it.isNotEmpty() }
        return employees.filter { emp ->
            val nameTokens = normalize(emp.name).split(" ").filter { it.isNotEmpty() }
            queryTokens.all { it in nameTokens }
        }
    }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text.uppercase(), Normalizer.Form.NFD)
        return nfd.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
