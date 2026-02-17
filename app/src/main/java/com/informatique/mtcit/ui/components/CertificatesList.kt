package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.business.transactions.shared.Certificate

@Composable
fun CertificatesList(
    certificates: List<Certificate>,
    onCertificateClick: (Certificate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = " قائمة الشهادات المتأثرة (${certificates.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        if (certificates.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("لا يوجد شهادات متاحة", fontSize = 16.sp)
            }
        } else {
            certificates.forEach { certificate ->
                CertificateCard(
                    certificate = certificate,
                    onClick = onCertificateClick
                )
            }
        }
    }
}