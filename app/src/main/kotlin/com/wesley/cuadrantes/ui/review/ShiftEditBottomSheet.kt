package com.wesley.cuadrantes.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
enum class ShiftField { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftEditBottomSheet(
    initialTime: LocalTime,
    field: ShiftField,
    title: String,
    onApply: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialTime.minute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Editar $title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Desliza para cambiar la hora",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TimeWheel(
                    items = (0..23).toList(),
                    selected = selectedHour,
                    label = "Horas",
                    format = { "%02d".format(it) },
                    onSelected = { selectedHour = it },
                )
                Spacer(Modifier.width(8.dp))
                Text(":", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                TimeWheel(
                    items = (0..59).toList(),
                    selected = selectedMinute,
                    label = "Min",
                    format = { "%02d".format(it) },
                    onSelected = { selectedMinute = it },
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onApply(LocalTime.of(selectedHour, selectedMinute)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Aplicar — %02d:%02d".format(selectedHour, selectedMinute))
            }
        }
    }
}

@Composable
private fun TimeWheel(
    items: List<Int>,
    selected: Int,
    label: String,
    format: (Int) -> String,
    onSelected: (Int) -> Unit,
) {
    val itemHeight = 48.dp
    val visibleItems = 5
    // El LazyColumn tiene 2 ítems de padding arriba + items.size reales + 2 de padding abajo.
    // Para que `items[k]` quede en el centro de 5 filas visibles, firstVisibleItemIndex debe ser k.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstIndex ->
                if (firstIndex in items.indices) onSelected(items[firstIndex])
            }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(itemHeight * visibleItems)
                .drawWithContent {
                    drawContent()
                    // highlight strip
                    val stripTop = size.height / 2 - itemHeight.toPx() / 2
                    drawRect(
                        color = Color(0x221976D2),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, stripTop),
                        size = androidx.compose.ui.geometry.Size(size.width, itemHeight.toPx()),
                    )
                },
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // padding items so first and last can center
                items(2) { Box(Modifier.height(itemHeight)) }
                items(items.size) { idx ->
                    val isSelected = items[idx] == selected
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(itemHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = format(items[idx]),
                            fontSize = if (isSelected) 22.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(2) { Box(Modifier.height(itemHeight)) }
            }
        }
    }
}
