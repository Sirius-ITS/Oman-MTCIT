package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    allowPastDates: Boolean = true,
    mandatory: Boolean = false
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(
                text = if (mandatory) "$label *" else label,
                color = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date",
                    tint = if (error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error
        )
    )

    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { selectedDate ->
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                onValueChange(formatter.format(Date(selectedDate)))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            allowPastDates = allowPastDates,
            initialDate = value
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    allowPastDates: Boolean = true,
    initialDate: String? = null
) {
    val today = System.currentTimeMillis()

    // Parse initial date if provided
    val initialDateMillis = remember(initialDate) {
        if (initialDate != null && initialDate.isNotEmpty()) {
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                formatter.parse(initialDate)?.time
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return if (allowPastDates) {
                utcTimeMillis <= today
            } else {
                utcTimeMillis >= today
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text("موافق")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
