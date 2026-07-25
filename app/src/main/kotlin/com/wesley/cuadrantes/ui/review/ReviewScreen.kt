package com.wesley.cuadrantes.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wesley.cuadrantes.model.DayStatus
import com.wesley.cuadrantes.model.Shift
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun LocalTime2str(h: Int, m: Int) = "%02d:%02d".format(h, m)

@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val days by viewModel.days.collectAsState()
    val selectedIndex by viewModel.selectedDayIndex.collectAsState()
    val plannedAlarms by viewModel.plannedAlarms.collectAsState()
    val context = LocalContext.current

    // dayIndex + which field (START/END) is being edited
    var editTarget by remember { mutableStateOf<Pair<Int, ShiftField>?>(null) }

    plannedAlarms?.let { alarms ->
        AlarmConfirmDialog(
            alarms = alarms,
            onConfirm = { viewModel.confirmAlarms(it) },
            onDismiss = { viewModel.dismissAlarmConfirmation() },
        )
    }

    editTarget?.let { (dayIdx, field) ->
        val day = days[dayIdx]
        val shift = day.shifts.firstOrNull()
            ?: Shift(java.time.LocalTime.of(8, 0), java.time.LocalTime.of(16, 0))
        val initial = if (field == ShiftField.START) shift.start else shift.end
        ShiftEditBottomSheet(
            initialTime = initial,
            field = field,
            title = if (field == ShiftField.START) "hora de entrada" else "hora de salida",
            onApply = { time ->
                if (field == ShiftField.START) viewModel.editShiftStart(dayIdx, 0, time)
                else viewModel.editShiftEnd(dayIdx, 0, time)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = viewModel.employeeName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEachIndexed { index, day ->
                DayCard(
                    day = day,
                    isSelected = index == selectedIndex,
                    onClick = { viewModel.selectDay(index) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (days.isNotEmpty()) {
            DetailPanel(
                day = days[selectedIndex],
                onStatusChange = { viewModel.changeStatus(selectedIndex, it) },
                onToggleAlarm = { viewModel.toggleAlarm(selectedIndex) },
                onEditStart = { editTarget = selectedIndex to ShiftField.START },
                onEditEnd = { editTarget = selectedIndex to ShiftField.END },
                modifier = Modifier.weight(1f),
            )
        }

        if (viewModel.needsExactAlarmPermission) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Se necesita permiso de alarmas exactas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }) { Text("Ajustes") }
                }
            }
        }

        if (viewModel.needsFullScreenIntentPermission) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Sin permiso de pantalla completa la alarma no se verá en pantalla de bloqueo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    }) { Text("Ajustes") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.requestAlarmConfirmation() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.needsExactAlarmPermission,
        ) {
            Text("Programar alarmas (${viewModel.enabledAlarmCount} días)")
        }
    }
}

@Composable
private fun DayCard(
    day: EditableDayState,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        day.status == DayStatus.FREE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        day.status == DayStatus.VACATION -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.width(80.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = day.date.format(DateTimeFormatter.ofPattern("EEE", Locale("es"))).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            when (day.status) {
                DayStatus.WORK -> {
                    day.shifts.firstOrNull()?.let { shift ->
                        Text(
                            text = "${LocalTime2str(shift.start.hour, shift.start.minute)}\n" +
                                    LocalTime2str(shift.end.hour, shift.end.minute),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (day.alarmEnabled) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.height(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                DayStatus.FREE -> Text(
                    "LIBRE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                DayStatus.VACATION -> Text(
                    "VAC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                DayStatus.UNKNOWN -> Text(
                    "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailPanel(
    day: EditableDayState,
    onStatusChange: (DayStatus) -> Unit,
    onToggleAlarm: () -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = day.date.format(
                    DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))
                ).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "Tipo de día",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = day.status == DayStatus.WORK,
                    onClick = { onStatusChange(DayStatus.WORK) },
                    label = { Text("Trabajo") },
                )
                FilterChip(
                    selected = day.status == DayStatus.FREE,
                    onClick = { onStatusChange(DayStatus.FREE) },
                    label = { Text("Libre") },
                )
                FilterChip(
                    selected = day.status == DayStatus.VACATION,
                    onClick = { onStatusChange(DayStatus.VACATION) },
                    label = { Text("Vacaciones") },
                )
            }

            if (day.status == DayStatus.WORK) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                day.shifts.forEachIndexed { i, shift ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = if (day.shifts.size > 1) "Turno ${i + 1}" else "Turno",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(52.dp),
                        )
                        Text(
                            text = LocalTime2str(shift.start.hour, shift.start.minute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onEditStart,
                                )
                                .padding(horizontal = 4.dp),
                        )
                        Text(" → ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = LocalTime2str(shift.end.hour, shift.end.minute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onEditEnd,
                                )
                                .padding(horizontal = 4.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        val mins = java.time.Duration.between(shift.start, shift.end).toMinutes()
                            .let { if (it < 0) it + 24 * 60 else it }
                        Text(
                            text = if (mins % 60 == 0L) "${mins / 60}h" else "${mins / 60}h${mins % 60}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (day.hasMidnightCrossing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Turno cruza medianoche",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val alarmTime = day.shifts.firstOrNull()?.start?.minusMinutes(60)
                    Text(
                        text = if (day.alarmEnabled && alarmTime != null)
                            "Alarma a las ${LocalTime2str(alarmTime.hour, alarmTime.minute)}"
                        else "Alarma desactivada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onToggleAlarm) {
                        Icon(
                            if (day.alarmEnabled) Icons.Default.Notifications
                            else Icons.Default.NotificationsOff,
                            contentDescription = "Toggle alarma",
                            tint = if (day.alarmEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
