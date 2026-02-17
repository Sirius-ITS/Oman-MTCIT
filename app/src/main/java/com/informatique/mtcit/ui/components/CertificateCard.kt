package com.informatique.mtcit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.business.transactions.shared.Certificate
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * Composable function to display a single certificate card
 * Matches the design from the screenshot
 */
@Composable
fun CertificateCard(
    certificate: Certificate,
    onClick: (Certificate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onClick(certificate) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Certificate Number (Top Right)
            Text(
                text = "رقم الشهادة: ${certificate.certificateNumber}",
                fontSize = 12.sp,
                color = extraColors.textSubTitle.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Certificate Title (Center)
            Text(
                text = certificate.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dates Row (Bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expiry Date (Left in RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الانتهاء:",
                        fontSize = 12.sp,
                        color = extraColors.textSubTitle.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = certificate.expiryDate,
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp),
                    color = Color(0xFFE0E0E0)
                )

                // Issue Date (Right in RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الإصدار:",
                        fontSize = 12.sp,
                        color = extraColors.textSubTitle.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = certificate.issueDate,
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
