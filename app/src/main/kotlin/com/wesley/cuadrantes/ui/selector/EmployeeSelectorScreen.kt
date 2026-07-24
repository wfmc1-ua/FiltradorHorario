package com.wesley.cuadrantes.ui.selector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wesley.cuadrantes.model.EmployeeSchedule

@Composable
fun EmployeeSelectorScreen(
    viewModel: SelectorViewModel,
    onEmployeeSelected: (Int) -> Unit,
) {
    val query by viewModel.query.collectAsState()
    val results = viewModel.filteredEmployees()
    val allEmployees = viewModel.employees
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (viewModel.cuadranteInfo.isNotEmpty()) {
            Text(
                text = viewModel.cuadranteInfo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = {
                viewModel.onQueryChange(it)
                selectedIndex = -1
            },
            label = { Text("Buscar por nombre…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        val hint = when {
            query.isBlank() -> "${allEmployees.size} empleados"
            results.isEmpty() -> "Sin coincidencias"
            else -> "${results.size} encontrados"
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(results) { listIdx, employee ->
                val globalIndex = allEmployees.indexOf(employee)
                val isSelected = selectedIndex == globalIndex
                EmployeeItem(
                    employee = employee,
                    isSelected = isSelected,
                    onClick = { selectedIndex = globalIndex },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (selectedIndex >= 0) onEmployeeSelected(selectedIndex) },
            enabled = selectedIndex >= 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Confirmar selección")
        }
    }
}

@Composable
private fun EmployeeItem(
    employee: EmployeeSchedule,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Text(
                    text = employee.name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val meta = buildString {
                    employee.rawId?.let { append("Nº $it") }
                    employee.contractHours?.let {
                        if (isNotEmpty()) append(" · ")
                        append("${it}h")
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
