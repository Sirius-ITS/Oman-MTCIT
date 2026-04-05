package com.informatique.mtcit.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.util.Locale
import com.informatique.mtcit.common.util.LocalAppLocale
import androidx.compose.ui.res.stringResource

/**
 * Data class representing a single issued certificate item for the bottom sheet.
 */
data class IssuedCertItem(
    val number: String,
    val typeEn: String,
    val typeAr: String,
    val onView: () -> Unit
)

/**
 * Bottom sheet shown after successful issuance of one or more certificates
 * in change transactions (types 10/11/12/13).
 *
 * Each certificate appears as a card (styled like the Affected Certificates step)
 * with a "View Certificate" button.  A single "Close" button sits at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuedCertificatesBottomSheet(
    title: String,
    items: List<IssuedCertItem>,
    onDismiss: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val isArabic = LocalAppLocale.current.language == "ar"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Success icon ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Title ──────────────────────────────────────────────────────
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Certificate cards (scrollable) ─────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)   // mirrors CertificatesList outer padding
            ) {
                items.forEach { cert ->
                    IssuedCertCard(cert = cert, isArabic = isArabic)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Disclaimer note ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.previous_certificates_have_been_cancelled_expired_and_are_only_available_in_the),
                fontSize = 12.sp,
                color = extraColors.textSubTitle.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Close button ───────────────────────────────────────────────
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, extraColors.blue1)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    color = extraColors.blue1,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * A single certificate card inside the issued-certificates bottom sheet.
 * Layout mirrors CertificateCard.kt — same spacings, colours and typography —
 * with a "View Certificate" button replacing the dates row.
 */
@Composable
private fun IssuedCertCard(
    cert: IssuedCertItem,
    isArabic: Boolean
) {
    val extraColors = LocalExtraColors.current
    val typeName = if (isArabic)
        cert.typeAr.ifBlank { cert.typeEn }
    else
        cert.typeEn.ifBlank { cert.typeAr }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),  // ← same as CertificateCard
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Certificate number (top) ────────────────────────────────
            Text(
                text = stringResource(R.string.certificate_no_cert_number),
                fontSize = 12.sp,
                color = extraColors.textSubTitle.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))   // ← same as CertificateCard

            // ── Certificate type name (centre, bold) ────────────────────
            if (typeName.isNotBlank()) {
                Text(
                    text = typeName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,             // ← same as CertificateCard
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))   // ← same as CertificateCard

            // ── "View Certificate" button (replaces dates row) ──────────
            Button(
                onClick = cert.onView,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = extraColors.blue1),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.view_certificate),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

