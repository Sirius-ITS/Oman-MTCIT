package com.informatique.mtcit.ui.components

import androidx.compose.runtime.Composable
import java.util.Locale

/**
 * Payment Success Dialog - Wrapper around unified SuccessDialog
 * Displays payment receipt details with success animation
 *
 * @deprecated Use SuccessDialog directly for new implementations
 */
@Composable
fun PaymentSuccessDialog(
    message: String,
    receiptNumber: String,
    paidAmount: String,
    timestamp: String,
    onDismiss: () -> Unit
) {
    val isArabic = Locale.getDefault().language == "ar"

    // Build items list for payment details
    val items = listOf(
        SuccessDialogItem(
            label = if (isArabic) "رقم الإيصال" else "Receipt Number",
            value = receiptNumber,
            icon = "📄"
        ),
        SuccessDialogItem(
            label = if (isArabic) "المبلغ المدفوع" else "Paid Amount",
            value = paidAmount,
            icon = "💰"
        ),
        SuccessDialogItem(
            label = if (isArabic) "التاريخ والوقت" else "Date & Time",
            value = timestamp,
            icon = "⏰"
        )
    )

    // Use unified SuccessDialog
    SuccessDialog(
        title = message,
        items = items,
        qrCode = null, // Payment doesn't have QR code
        onDismiss = onDismiss
    )
}
