package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors

@Composable
fun FocusAwareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: (String) -> Unit = {},
    label: String,
    isPassword: Boolean = false,
    isNumeric: Boolean = false,
    isDecimal: Boolean = false, // ✅ NEW: Support for decimal numbers
    error: String? = null,
    mandatory: Boolean = false,
    isLoading: Boolean = false,
    readOnly: Boolean = false,
    placeholder: String? = null
) {
    val extraColors = LocalExtraColors.current
    var wasFocused by remember { mutableStateOf(false) }

    // Treat messages starting with a check mark as success messages (green)
    val isSuccess = error?.trimStart()?.startsWith("✔") == true
    val successColor = Color(0xFF2E7D32) // Green 700

    Column(modifier = Modifier.fillMaxWidth()) {
        // Label above the field (like CustomTextField)
        Text(
            text = if (mandatory) "$label *" else label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extraColors.cardBackground,
                unfocusedContainerColor = extraColors.cardBackground,
                disabledContainerColor = extraColors.cardBackground,
                focusedBorderColor = when {
                    isSuccess -> successColor
                    error != null -> Color(0xFFE74C3C)
                    else -> Color.Transparent // ✅ Transparent when no error
                },
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = extraColors.whiteInDarkMode,
                unfocusedTextColor = extraColors.whiteInDarkMode,
                disabledTextColor = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                cursorColor = extraColors.whiteInDarkMode
            ),
            placeholder = {
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        color = extraColors.textSubTitle,
                        fontSize = 16.sp
                    )
                }
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    isDecimal -> KeyboardType.Decimal // ✅ Decimal keyboard (allows . or ,)
                    isNumeric -> KeyboardType.Number   // Integer only
                    isPassword -> KeyboardType.Password
                    else -> KeyboardType.Text
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (wasFocused && !focusState.isFocused && value.isNotBlank()) {
                        // User lost focus and field has content - trigger API call
                        onFocusLost(value)
                    }
                    wasFocused = focusState.isFocused
                },
            isError = error != null && !isSuccess,
            singleLine = true,
            readOnly = readOnly || isLoading,
            enabled = !readOnly && !isLoading,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = extraColors.whiteInDarkMode
                    )
                }
            }
        )

        // Error/Success message below the field
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = if (isSuccess) successColor else Color(0xFFE74C3C),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
