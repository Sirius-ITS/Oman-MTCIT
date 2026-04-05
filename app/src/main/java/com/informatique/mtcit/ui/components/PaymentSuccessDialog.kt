package com.informatique.mtcit.ui.components

import androidx.compose.runtime.Composable
import java.util.Locale
import com.informatique.mtcit.common.util.LocalAppLocale
import androidx.compose.ui.res.stringResource

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
    val isArabic = LocalAppLocale.current.language == "ar"

    // Build items list for payment details
    val items = listOf(
        SuccessDialogItem(
            label = stringResource(R.string.receipt_number),
            value = receiptNumber,
            icon = "📄"
        ),
        SuccessDialogItem(
            label = stringResource(R.string.paid_amount),
            value = paidAmount,
            icon = "💰"
        ),
        SuccessDialogItem(
            label = stringResource(R.string.date_time),
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
