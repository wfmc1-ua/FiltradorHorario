package com.wesley.cuadrantes.ui.import

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wesley.cuadrantes.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Loading : ImportUiState()
    data class Error(val message: String) : ImportUiState()
    object Done : ImportUiState()
}

class ImportViewModel(
    private val container: AppContainer,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state

    fun importFrom(uri: Uri) {
        _state.value = ImportUiState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)!!.use { stream ->
                        val tokens = container.extractor.extract(stream)
                        container.parser.parse(tokens, year = container.clock.now().year)
                    }
                }
            }
            result.fold(
                onSuccess = { cuadrante ->
                    if (container.currentCuadrante != null) {
                        container.alarmScheduler.cancelAll()
                    }
                    container.currentCuadrante = cuadrante
                    _state.value = ImportUiState.Done
                },
                onFailure = { e ->
                    Log.e("ImportViewModel", "Error importando PDF", e)
                    _state.value = ImportUiState.Error(
                        "[${e.javaClass.simpleName}] ${e.message ?: "Error desconocido"}"
                    )
                },
            )
        }
    }

    fun reset() {
        _state.value = ImportUiState.Idle
    }

    class Factory(
        private val container: AppContainer,
        private val contentResolver: ContentResolver,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ImportViewModel(container, contentResolver) as T
    }
}
