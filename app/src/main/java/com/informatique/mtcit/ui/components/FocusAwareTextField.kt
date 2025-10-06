package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun FocusAwareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: (String) -> Unit = {},
    label: String,
    isPassword: Boolean = false,
    isNumeric: Boolean = false,
    error: String? = null,
    mandatory: Boolean = false,
    isLoading: Boolean = false,
    readOnly: Boolean = false
) {
    var wasFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = if (mandatory) "$label *" else label,
                color = when {
                    error != null -> MaterialTheme.colorScheme.error
                    readOnly -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                isNumeric -> KeyboardType.Number
                isPassword -> KeyboardType.Password
                else -> KeyboardType.Text
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (wasFocused && !focusState.isFocused && value.isNotBlank()) {
                    // User lost focus and field has content
                    onFocusLost(value)
                }
                wasFocused = focusState.isFocused
            },
        isError = error != null,
        singleLine = true,
        readOnly = readOnly || isLoading,
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = when {
                error != null -> MaterialTheme.colorScheme.error
                readOnly -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.primary
            },
            focusedLabelColor = when {
                error != null -> MaterialTheme.colorScheme.error
                readOnly -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.primary
            },
            unfocusedBorderColor = when {
                error != null -> MaterialTheme.colorScheme.error
                readOnly -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outline
            },
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
}
