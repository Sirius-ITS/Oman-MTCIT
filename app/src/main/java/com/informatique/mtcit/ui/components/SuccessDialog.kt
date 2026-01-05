package com.informatique.mtcit.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.util.Base64
import java.util.Locale

/**
 * Unified Success Dialog - Used for payment, certificate issuance, and other success scenarios
 * Replaces PaymentSuccessDialog and CertificateIssuanceSuccessDialog
 */
@Composable
fun SuccessDialog(
    title: String,
    items: List<SuccessDialogItem>,
    qrCode: String? = null, // Base64 encoded PNG
    onDismiss: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val isArabic = Locale.getDefault().language == "ar"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp), // Limit max height
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = extraColors.cardBackground
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            // ✅ Use Column with weight to separate scrollable content from fixed button
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ✅ Scrollable content (takes available space)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) // Takes space but doesn't force full height
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Green checkmark icon with circular background
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    // Success message title
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.whiteInDarkMode,
                        textAlign = TextAlign.Center
                    )

                    // Details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = extraColors.cardBackground.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items.forEachIndexed { index, item ->
                                SuccessDialogItemRow(
                                    label = item.label,
                                    value = item.value,
                                    icon = item.icon
                                )

                                // Add divider between items (but not after last item)
                                if (index < items.size - 1) {
                                    HorizontalDivider(
                                        color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }

                    // QR Code (if provided)
                    if (!qrCode.isNullOrEmpty()) {
                        Text(
                            text = if (isArabic) "رمز QR" else "QR Code",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
                        )

                        // Decode Base64 QR code outside composable using remember
                        val qrBitmap = remember(qrCode) {
                            try {
                                val decodedBytes = Base64.getDecoder().decode(qrCode)
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                println("❌ Error decoding QR code: ${e.message}")
                                null
                            }
                        }

                        if (qrBitmap != null) {
                            Card(
                                modifier = Modifier.size(200.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            // If QR code decoding fails, show error message
                            Text(
                                text = if (isArabic) "خطأ في تحميل رمز QR" else "Error loading QR code",
                                fontSize = 12.sp,
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // ✅ Fixed OK button at bottom (always visible)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 24.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.blue1
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "حسناً" else "OK",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom spacing
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Row displaying a detail item with icon, label, and value
 */
@Composable
private fun SuccessDialogItemRow(
    label: String,
    value: String,
    icon: String
) {
    val extraColors = LocalExtraColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon in circular background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = extraColors.blue2.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 16.sp
                )
            }

            // Label
            Text(
                text = label,
                fontSize = 14.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
            )
        }

        // Value
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.whiteInDarkMode,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/**
 * Data class for dialog items
 */
data class SuccessDialogItem(
    val label: String,
    val value: String,
    val icon: String
)

