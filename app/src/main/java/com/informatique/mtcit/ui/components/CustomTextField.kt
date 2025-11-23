package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors


@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    isNumeric: Boolean = false,
    error: String? = null,
    mandatory: Boolean = false,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    enabled: Boolean = true
) {
    val extraColors = LocalExtraColors.current

    // Treat messages starting with a check mark as success messages (green)
    val isSuccess = error?.trimStart()?.startsWith("✔") == true
    val successColor = Color(0xFF2E7D32) // Green 700

    Column(modifier = Modifier.fillMaxWidth()) {
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
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        color = extraColors.textSubTitle,
                        fontSize = 16.sp
                    )
                }
            },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = extraColors.whiteInDarkMode,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else null,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    isNumeric -> KeyboardType.Number
                    isPassword -> KeyboardType.Password
                    else -> KeyboardType.Text
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null && !isSuccess,
            singleLine = true,
            enabled = enabled
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
}