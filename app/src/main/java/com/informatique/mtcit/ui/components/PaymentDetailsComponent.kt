package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.text.DecimalFormat

/**
 * Payment Details Component - Displays payment breakdown from API response
 * ✅ Uses localized strings and app colors
 */
@Composable
fun PaymentDetailsComponent(
    arabicValue: String,
    lineItems: List<FormField.PaymentLineItem>,
    totalCost: Double,
    totalTax: Double,
    finalTotal: Double
) {
    val extraColors = LocalExtraColors.current
    val formatter = DecimalFormat("#,##0.000")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ✅ Amount to Pay Box - Using app colors and RTL layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(extraColors.startServiceButton.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Icon
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = extraColors.startServiceButton,
                    modifier = Modifier.size(24.dp)
                )

                // Right side: Title + Amount
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = localizedApp(R.string.amount_due),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.startServiceButton,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = arabicValue,
                        fontSize = 14.sp,
                        color = extraColors.startServiceButton.copy(alpha = 0.8f),
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        // ✅ Payment Breakdown Card - Using localized string
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = extraColors.cardBackground
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        extraColors.iconLightBlueBackground,
                                        extraColors.iconLightBlueBackground.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = extraColors.iconLightBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = localizedApp(R.string.payment_breakdown_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.whiteInDarkMode
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    extraColors.whiteInDarkMode.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Line Items
                lineItems.forEach { item ->
                    PaymentLineItemRow(
                        label = item.name,
                        value = "${formatter.format(item.amount)} ر.ع"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // ✅ Total Amount Box - Using app colors and localized string
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(extraColors.cardBackground.copy(alpha = 0.5f))
                .border(
                    1.dp,
                    extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedApp(R.string.total_amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
                )

                Text(
                    text = "${formatter.format(finalTotal)} ر.ع",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.whiteInDarkMode
                )
            }
        }
    }
}

@Composable
private fun PaymentLineItemRow(
    label: String,
    value: String
) {
    val extraColors = LocalExtraColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        extraColors.iconLightBlueBackground.copy(alpha = 0.2f),
                        extraColors.iconLightBlueBackground.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                1.dp,
                extraColors.iconLightBlue.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode,
        )
    }
}
