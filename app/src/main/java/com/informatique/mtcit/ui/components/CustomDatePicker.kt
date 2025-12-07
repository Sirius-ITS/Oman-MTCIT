//package com.informatique.mtcit.ui.components
//
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.DateRange
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import java.text.SimpleDateFormat
//import java.util.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CustomDatePicker(
//    mortgageValue: String,
//    onValueChange: (String) -> Unit,
//    label: String,
//    error: String? = null,
//    allowPastDates: Boolean = true,
//    mandatory: Boolean = false
//) {
//    var showDatePicker by remember { mutableStateOf(false) }
//
//    OutlinedTextField(
//        mortgageValue = mortgageValue,
//        onValueChange = {},
//        readOnly = true,
//        label = {
//            Text(
//                text = if (mandatory) "$label *" else label,
//                color = if (error != null) MaterialTheme.colorScheme.error
//                else MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        },
//        trailingIcon = {
//            IconButton(onClick = { showDatePicker = true }) {
//                Icon(
//                    imageVector = Icons.Default.DateRange,
//                    contentDescription = "Select date",
//                    tint = if (error != null) MaterialTheme.colorScheme.error
//                    else MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        },
//        modifier = Modifier.fillMaxWidth(),
//        isError = error != null,
//        colors = OutlinedTextFieldDefaults.colors(
//            focusedBorderColor = MaterialTheme.colorScheme.primary,
//            focusedLabelColor = MaterialTheme.colorScheme.primary,
//            errorBorderColor = MaterialTheme.colorScheme.error,
//            errorLabelColor = MaterialTheme.colorScheme.error
//        )
//    )
//
//    if (error != null) {
//        Text(
//            text = error,
//            color = MaterialTheme.colorScheme.error,
//            style = MaterialTheme.typography.bodySmall,
//            modifier = Modifier.fillMaxWidth()
//        )
//    }
//
//    if (showDatePicker) {
//        DatePickerModal(
//            onDateSelected = { selectedDate ->
//                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                onValueChange(formatter.format(Date(selectedDate)))
//                showDatePicker = false
//            },
//            onDismiss = { showDatePicker = false },
//            allowPastDates = allowPastDates,
//            initialDate = mortgageValue
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DatePickerModal(
//    onDateSelected: (Long) -> Unit,
//    onDismiss: () -> Unit,
//    allowPastDates: Boolean = true,
//    initialDate: String? = null
//) {
//    val today = System.currentTimeMillis()
//
//    // Parse initial date if provided
//    val initialDateMillis = remember(initialDate) {
//        if (initialDate != null && initialDate.isNotEmpty()) {
//            try {
//                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                formatter.parse(initialDate)?.time
//            } catch (e: Exception) {
//                null
//            }
//        } else {
//            null
//        }
//    }
//
//    val selectableDates = object : SelectableDates {
//        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
//            return if (allowPastDates) {
//                utcTimeMillis <= today
//            } else {
//                utcTimeMillis >= today
//            }
//        }
//    }
//
//    val datePickerState = rememberDatePickerState(
//        initialSelectedDateMillis = initialDateMillis,
//        selectableDates = selectableDates
//    )
//
//    DatePickerDialog(
//        onDismissRequest = onDismiss,
//        confirmButton = {
//            TextButton(
//                onClick = {
//                    datePickerState.selectedDateMillis?.let { selectedDate ->
//                        onDateSelected(selectedDate)
//                    }
//                }
//            ) {
//                Text("موافق")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("إلغاء")
//            }
//        }
//    ) {
//        DatePicker(state = datePickerState)
//    }
//}


package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors
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
    val extraColors = LocalExtraColors.current
    var showDatePicker by remember { mutableStateOf(false) }

    // Treat messages starting with a check mark as success messages (green)
    val isSuccess = error?.trimStart()?.startsWith("✔") == true
    val successColor = Color(0xFF2E7D32) // Green 700

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = extraColors.whiteInDarkMode)) {
                    append(label)
                }
                if (mandatory) {
                    append(" ")
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append("*")
                    }
                }
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extraColors.cardBackground,
                unfocusedContainerColor = extraColors.cardBackground,
                disabledContainerColor = extraColors.cardBackground,
                focusedBorderColor = when {
                    isSuccess -> successColor
                    error != null -> Color(0xFFE74C3C)
                    else -> Color.Transparent
                },
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = extraColors.whiteInDarkMode,
                unfocusedTextColor = extraColors.whiteInDarkMode,
                disabledTextColor = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                cursorColor = extraColors.whiteInDarkMode
            ),
            placeholder = {
                Text(
                    text = label,
                    color = extraColors.textSubTitle,
                    fontSize = 16.sp
                )
            },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date",
                        tint = extraColors.iconBlueGrey,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null && !isSuccess,
            singleLine = true
        )

        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = if (isSuccess) successColor else Color(0xFFE74C3C),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { selectedDate ->
                // Emit ISO format (yyyy-MM-dd) to the form mortgageValue
                // Force English locale so formatted date uses English digits/format even on Arabic locale
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
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
                // Accept ISO (yyyy-MM-dd) or dd/MM/yyyy as initial formats
                // Use English locale for parsing to keep behavior consistent and to ensure digits/format are parsed as expected
                val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val localFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                try {
                    isoFormatter.parse(initialDate)?.time
                } catch (e: Exception) {
                    try {
                        localFormatter.parse(initialDate)?.time
                    } catch (e2: Exception) {
                        null
                    }
                }
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
