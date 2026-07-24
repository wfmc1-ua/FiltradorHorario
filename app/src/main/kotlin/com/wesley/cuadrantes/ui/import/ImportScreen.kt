package com.wesley.cuadrantes.ui.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    pendingUri: Uri?,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Si llegó un URI por intent, lanzar la importación automáticamente
    LaunchedEffect(pendingUri) {
        if (pendingUri != null) viewModel.importFrom(pendingUri)
    }

    LaunchedEffect(state) {
        if (state is ImportUiState.Done) onDone()
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importFrom(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val s = state) {
            is ImportUiState.Idle -> {
                Text(
                    text = "Importa tu cuadrante",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Comparte el PDF desde WhatsApp o Google Drive, o ábrelo desde el explorador de archivos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { picker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Seleccionar PDF")
                }
            }

            is ImportUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Analizando cuadrante…", style = MaterialTheme.typography.bodyLarge)
            }

            is ImportUiState.Error -> {
                Text(
                    text = "No se pudo leer el PDF",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.reset()
                        picker.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Seleccionar otro PDF")
                }
            }

            is ImportUiState.Done -> {
                // LaunchedEffect ya navigó, no renderizamos nada
            }
        }
    }
}
