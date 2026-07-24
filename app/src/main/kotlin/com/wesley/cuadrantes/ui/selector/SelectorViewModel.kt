package com.wesley.cuadrantes.ui.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wesley.cuadrantes.AppContainer
import com.wesley.cuadrantes.model.EmployeeSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SelectorViewModel(private val container: AppContainer) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val employees: List<EmployeeSchedule>
        get() = container.currentCuadrante?.employees ?: emptyList()

    val cuadranteInfo: String
        get() {
            val c = container.currentCuadrante ?: return ""
            val center = c.center ?: ""
            return buildString {
                if (center.isNotEmpty()) append("$center · ")
                append("Sem. del ${c.weekStart}")
            }
        }

    fun filteredEmployees(): List<EmployeeSchedule> {
        val q = _query.value
        return if (q.isBlank()) employees
        else container.nameMatcher.findBestMatch(q, employees)
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SelectorViewModel(container) as T
    }
}
