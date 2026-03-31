package com.tx.carrecord.feature.addcar.ui

import androidx.compose.runtime.Composable
import com.tx.carrecord.core.common.ui.AppDatePickerDialog
import com.tx.carrecord.core.common.ui.AppMileagePickerDialog
import java.time.LocalDate

@Composable
fun CarPurchaseDatePickerDialog(
    purchaseDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    AppDatePickerDialog(
        currentDate = purchaseDate,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun CarMileagePickerDialog(
    wan: Int,
    qian: Int,
    bai: Int,
    onDismiss: () -> Unit,
    onConfirm: (wan: Int, qian: Int, bai: Int) -> Unit,
) {
    AppMileagePickerDialog(
        title = "选择里程",
        wan = wan,
        qian = qian,
        bai = bai,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
