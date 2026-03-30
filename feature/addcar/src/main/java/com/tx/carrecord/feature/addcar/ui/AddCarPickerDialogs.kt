package com.tx.carrecord.feature.addcar.ui

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarPurchaseDatePickerDialog(
    purchaseDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToEpochMillis(purchaseDate),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        onConfirm(epochMillisToLocalDate(selectedMillis))
                    }
                    onDismiss()
                },
            ) {
                Text(text = "应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
        )
    }
}

@Composable
fun CarMileagePickerDialog(
    wan: Int,
    qian: Int,
    bai: Int,
    onDismiss: () -> Unit,
    onConfirm: (wan: Int, qian: Int, bai: Int) -> Unit,
) {
    var editingWan by remember(wan) { mutableStateOf(wan.coerceAtLeast(0)) }
    var editingQian by remember(qian) { mutableStateOf(qian.coerceIn(0, 9)) }
    var editingBai by remember(bai) { mutableStateOf(bai.coerceIn(0, 9)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "选择里程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    NumberWheelPicker(
                        label = "万",
                        value = editingWan,
                        range = 0..99,
                        onValueChange = { editingWan = it },
                    )
                    NumberWheelPicker(
                        label = "千",
                        value = editingQian,
                        range = 0..9,
                        onValueChange = { editingQian = it },
                    )
                    NumberWheelPicker(
                        label = "百",
                        value = editingBai,
                        range = 0..9,
                        onValueChange = { editingBai = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(editingWan, editingQian, editingBai)
                },
            ) {
                Text(text = "完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun NumberWheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    val itemHeightPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = false
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setOnValueChangedListener { _, _, newVal ->
                        onValueChange(newVal)
                    }
                }
            },
            update = { picker ->
                picker.minValue = range.first
                picker.maxValue = range.last
                picker.value = value.coerceIn(range.first, range.last)
                picker.setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
                picker.layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    itemHeightPx * 3,
                )
            },
        )
    }
}

private fun localDateToEpochMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
