package com.wesley.cuadrantes.ui.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wesley.cuadrantes.alarm.PlannedAlarm
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("EEE dd MMM", Locale("es"))

@Composable
fun AlarmConfirmDialog(
    alarms: List<PlannedAlarm>,
    onConfirm: (List<PlannedAlarm>) -> Unit,
    onDismiss: () -> Unit,
) {
    val checked = remember { mutableStateListOf(*Array(alarms.size) { !alarms[it].isPast }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar alarmas") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (alarms.isEmpty()) {
                    Text(
                        "No hay alarmas que programar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        "Revisa y desmarca las que no quieras:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    alarms.forEachIndexed { i, planned ->
                        AlarmRow(
                            planned = planned,
                            checked = checked[i],
                            onCheckedChange = { checked[i] = it },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selected = alarms.filterIndexed { i, _ -> checked[i] }
                onConfirm(selected)
            }) {
                Text("Programar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun AlarmRow(
    planned: PlannedAlarm,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked && !planned.isPast,
            onCheckedChange = if (planned.isPast) null else onCheckedChange,
            enabled = !planned.isPast,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = planned.plan.date.format(dayFmt),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (planned.isPast) TextDecoration.LineThrough else TextDecoration.None,
                color = if (planned.isPast)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (planned.isPast)
                    "Alarma ${planned.plan.time} · ya pasó"
                else
                    "Alarma a las ${planned.plan.time} · ${planned.plan.label}",
                style = MaterialTheme.typography.bodySmall,
                color = if (planned.isPast)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
