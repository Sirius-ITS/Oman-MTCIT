package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    useModal: Boolean = true,
    allowPastDates: Boolean = true
) {
    if (useModal) {
        DatePickerFieldToModal(
            label = label,
            value = value,
            onValueChange = onValueChange,
            allowPastDates = allowPastDates
        )
    } else {
        DatePickerDocked(
            label = label,
            value = value,
            onValueChange = onValueChange,
            allowPastDates = allowPastDates
        )
    }

    if (error != null) {
        Text(
            text = error,
            color = androidx.compose.ui.graphics.Color.Red,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDocked(
    label: String,
    value: String?,
    onValueChange: (String) -> Unit,
    allowPastDates: Boolean
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val today = System.currentTimeMillis()
    val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return if (allowPastDates) {
                utcTimeMillis <= today
            } else {
                utcTimeMillis >= today
            }
        }
    }

    val datePickerState = rememberDatePickerState(selectableDates = selectableDates)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = { }, // نسيبها فاضية لأنه readOnly
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        if (showDatePicker) {
            Popup(
                onDismissRequest = { showDatePicker = false },
                alignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    DatePicker(state = datePickerState, showModeToggle = false)

                    // مجرد تحديث للقيمة عند التغيير
                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePickerState.selectedDateMillis?.let {
                            onValueChange(convertMillisToDate(it))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFieldToModal(
    label: String,
    value: String?,
    onValueChange: (String) -> Unit,
    allowPastDates: Boolean
) {
    var showModal by remember { mutableStateOf(false) }

    val today = System.currentTimeMillis()
    val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return if (allowPastDates) {
                utcTimeMillis <= today
            } else {
                utcTimeMillis >= today
            }
        }
    }

    val datePickerState = rememberDatePickerState(selectableDates = selectableDates)

    OutlinedTextField(
        value = value ?: "",
        onValueChange = { },
        label = { Text(label) },
        placeholder = { Text("MM/DD/YYYY") },
        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select date") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) showModal = true
                }
            }
    )

    if (showModal) {
        DatePickerDialog(
            onDismissRequest = { showModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onValueChange(convertMillisToDate(it))
                        }
                        showModal = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showModal = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
