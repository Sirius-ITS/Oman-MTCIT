package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.util.*

/**
 * Dialog shown when payment is in progress (paymentStatus = 1)
 * Asks user if they want to retry payment or cancel
 */
@Composable
fun PaymentRetryDialog(
    onContinue: () -> Unit,
    onClose: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val isArabic = Locale.getDefault().language == "ar"

    val title = if (isArabic) "عملية دفع قيد التنفيذ" else "Payment In Progress"
    val message = if (isArabic) {
        "هذه المعاملة قيد التنفيذ حالياً. هل تريد محاولة الدفع مرة أخرى؟"
    } else {
        "This transaction is in progress. Do you want to attempt payment again?"
    }
    val continueText = if (isArabic) "متابعة" else "Continue"
    val closeText = if (isArabic) "إغلاق" else "Close"

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = extraColors.cardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Warning Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFFFFA726).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Payment in progress",
                        modifier = Modifier.size(44.dp),
                        tint = Color(0xFFFFA726)
                    )
                }

                // Title
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.whiteInDarkMode,
                    textAlign = TextAlign.Center
                )

                // Message
                Text(
                    text = message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Close Button
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = extraColors.whiteInDarkMode
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            extraColors.whiteInDarkMode.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = closeText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Continue Button
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA726)
                        )
                    ) {
                        Text(
                            text = continueText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}



