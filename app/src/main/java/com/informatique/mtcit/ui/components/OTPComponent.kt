package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.informatique.mtcit.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPComponent(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    phoneNumber: String = "",
    otpLength: Int = 6,
    remainingTime: Int = 33,
    onResendOTP: () -> Unit = {},
    error: String? = null,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current
    var timer by remember { mutableStateOf(remainingTime) }
    val scope = rememberCoroutineScope()

    // Countdown Timer
    LaunchedEffect(Unit) {
        scope.launch {
            while (timer > 0) {
                delay(1000L)
                timer--
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        // Phone Number Display in Card
        if (phoneNumber.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .background(
                        color = extraColors.cardBackground,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedApp(R.string.phone_number),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
                    )
                    Text(
                        text = phoneNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.whiteInDarkMode,
                        maxLines = 1
                    )
                }
            }
        }

        // Label with mandatory indicator
        if (label.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode,
                    textAlign = TextAlign.End
                )
            }
        }

        // OTP Boxes (Individual boxes for each digit) - Clickable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(otpLength) { index ->
                    OTPBox(
                        digit = value.getOrNull(index)?.toString() ?: "",
                        isFocused = value.length == index,
                        hasError = error != null,
                        extraColors = extraColors
                    )

                    if (index < otpLength - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            // Invisible TextField overlay for input handling
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.length <= otpLength && newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                cursorBrush = SolidColor(Color.Transparent), // إخفاء الـ cursor
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        innerTextField()
                    }
                },
                textStyle = TextStyle(
                    color = Color.Transparent
                )
            )
        }

        // Timer Display
        Text(
            text = String.format("%02d:%02d", timer / 60, timer % 60),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.padding(top = 20.dp),
            textAlign = TextAlign.Center
        )

        // Resend OTP Link
        TextButton(
            onClick = {
                if (timer == 0) {
                    onResendOTP()
                    timer = remainingTime
                }
            },
            enabled = timer == 0,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = localizedApp(R.string.didnt_receive_OTP),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (timer == 0) Color(0xFF2196F3) else extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }

        // Error Message
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = Color(0xFFE74C3C),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OTPBox(
    digit: String,
    isFocused: Boolean,
    hasError: Boolean,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = extraColors.cardBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    hasError -> Color(0xFFE74C3C)
                    isFocused -> Color(0xFF2196F3)
                    else -> extraColors.whiteInDarkMode.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = extraColors.whiteInDarkMode,
            textAlign = TextAlign.Center
        )
    }
}