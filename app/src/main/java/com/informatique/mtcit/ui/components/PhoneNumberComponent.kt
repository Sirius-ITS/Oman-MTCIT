package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors

@Composable
fun PhoneNumberComponent(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    countryCodes: List<String> = listOf("+968", "+966", "+971", "+974", "+965", "+973"),
    selectedCountryCode: String = "+968",
    onCountryCodeChange: (String) -> Unit = {},
    placeholder: String? = null,
    error: String? = null,
    mandatory: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = if (mandatory) "$label *" else label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Phone Number Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Country Code Dropdown
            Box(
                modifier = Modifier.width(110.dp)
            ) {
                OutlinedTextField(
                    value = selectedCountryCode,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = extraColors.cardBackground,
                        unfocusedContainerColor = extraColors.cardBackground,
                        disabledContainerColor = extraColors.cardBackground,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedTextColor = extraColors.whiteInDarkMode,
                        unfocusedTextColor = extraColors.whiteInDarkMode,
                        disabledTextColor = extraColors.whiteInDarkMode
                    ),
                    placeholder = {
                        Text(
                            text = "كود",
                            color = extraColors.textSubTitle,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "اختر كود الدولة",
                            tint = extraColors.whiteInDarkMode
                        )
                    },
                    singleLine = true,
                    enabled = false
                )

                // Dropdown Menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(110.dp)
                        .background(extraColors.cardBackground)
                ) {
                    countryCodes.forEach { code ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = code,
                                    fontSize = 16.sp,
                                    color = extraColors.whiteInDarkMode,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            onClick = {
                                onCountryCodeChange(code)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Phone Number Input
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = extraColors.cardBackground,
                    unfocusedContainerColor = extraColors.cardBackground,
                    disabledContainerColor = extraColors.cardBackground,
                    focusedBorderColor = if (error != null) Color(0xFFE74C3C) else Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedTextColor = extraColors.whiteInDarkMode,
                    unfocusedTextColor = extraColors.whiteInDarkMode,
                    cursorColor = extraColors.whiteInDarkMode
                ),
                placeholder = {
                    Text(
                        text = placeholder ?: "أدخل رقم الهاتف",
                        color = extraColors.textSubTitle,
                        fontSize = 16.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                isError = error != null
            )
        }

        // Error Message
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = Color(0xFFE74C3C),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}