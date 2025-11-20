package com.informatique.mtcit.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable

@Serializable
sealed interface RequestDetail {
    @Serializable
    data class CheckShipCondition(
        val transactionTitle: String,
        val title: String,
        val description: String,
        val referenceNumber: String,
        val refuseReason: String,
        val shipData: Map<String, String>
    ) : RequestDetail
    @Serializable
    data class Attachments(val requestData: String) : RequestDetail

    @Serializable
    data class AcceptedAndPayment(
        val transactionTitle: String,
        val title: String,
        val referenceNumber: String,
        val dataSubmitted: Map<String, String>
    ) : RequestDetail
}

/**
 * Enhanced Request Detail Screen
 * - Always shows status at top (rejection/approval/pending)
 * - Displays all data in expandable sections like review step
 * - Parses structured data automatically
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(navController: NavController, requestDetail: RequestDetail) {
    val extraColors = LocalExtraColors.current

    // Parse the data based on request type
    val parsedData = remember(requestDetail) {
        parseRequestDetailData(requestDetail)
    }

    BackHandler {
        navController.navigate(NavRoutes.MainCategoriesRoute.route) {
            popUpTo(NavRoutes.MainCategoriesRoute.route) {
                inclusive = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.background)
    ) {
        // Top App Bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = parsedData.title,
                    fontSize = 18.sp,
                    color = extraColors.whiteInDarkMode,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // REQUEST STATUS CARD - ALWAYS AT TOP
        RequestStatusCard(
            status = parsedData.status,
            title = parsedData.statusTitle,
            message = parsedData.statusMessage,
            extraColors = extraColors
        )

        // SCROLLABLE DATA SECTIONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Display all data sections as expandable cards
            parsedData.sections.forEachIndexed { index, section ->
                ExpandableDataSection(
                    title = section.title,
                    items = section.items,
                    isExpandedByDefault = index == 0, // First section expanded
                    extraColors = extraColors
                )
            }
        }

        // BACK/ACTION BUTTON
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
        ) {
            Button(
                onClick = {
                    navController.navigate(NavRoutes.MainCategoriesRoute.route) {
                        popUpTo(NavRoutes.MainCategoriesRoute.route) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.startServiceButton,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = localizedApp(R.string.request_detail_back_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Request Status Card - Always shown at top
 */
@Composable
private fun RequestStatusCard(
    status: RequestStatus,
    title: String,
    message: String,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    val statusColor = when (status) {
        RequestStatus.REJECTED -> Color(0xFFEF4444)
        RequestStatus.APPROVED -> Color(0xFF10B981)
        RequestStatus.PENDING -> Color(0xFFF59E0B) // Orange color for pending/in progress
    }

    val statusIcon = when (status) {
        RequestStatus.REJECTED -> Icons.Default.Cancel
        RequestStatus.APPROVED -> Icons.Default.CheckCircle
        RequestStatus.PENDING -> Icons.Default.HourglassEmpty // In Progress icon
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Status Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extraColors.textSubTitle,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Expandable Data Section (like review step cards)
 */
@Composable
private fun ExpandableDataSection(
    title: String,
    items: List<DataItem>,
    isExpandedByDefault: Boolean,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    var isExpanded by remember { mutableStateOf(isExpandedByDefault) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground2.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = extraColors.whiteInDarkMode
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = extraColors.textBlueSubTitle
                )
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        DataItemCard(
                            label = item.label,
                            value = item.value,
                            extraColors = extraColors
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Data Item Card
 */
@Composable
private fun DataItemCard(
    label: String,
    value: String,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(extraColors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = extraColors.textSubTitle,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

// ==================== DATA MODELS ====================

enum class RequestStatus {
    REJECTED,
    APPROVED,
    PENDING
}

data class ParsedRequestData(
    val title: String,
    val status: RequestStatus,
    val statusTitle: String,
    val statusMessage: String,
    val sections: List<DataSection>
)

data class DataSection(
    val title: String,
    val items: List<DataItem>
)

data class DataItem(
    val label: String,
    val value: String
)

// ==================== DATA PARSER ====================

/**
 * Parses the request detail data into structured sections
 * Handles formatted strings from compliance checks or any other source
 */
private fun parseRequestDetailData(requestDetail: RequestDetail): ParsedRequestData {
    return when (requestDetail) {
        is RequestDetail.CheckShipCondition -> {
            parseShipComplianceData(requestDetail.shipData)
        }
        is RequestDetail.Attachments -> {
            parseAttachmentsData(requestDetail.requestData)
        }
    }
}

/**
 * Parse ship compliance data (from compliance check)
 */
private fun parseShipComplianceData(data: String): ParsedRequestData {
    val sections = mutableListOf<DataSection>()
    val lines = data.lines()

    var currentSection: MutableList<DataItem>? = null
    var currentSectionTitle = ""

    for (line in lines) {
        val trimmedLine = line.trim()

        when {
            // Section headers (with emoji or Arabic headers)
            trimmedLine.startsWith("📋") || trimmedLine.startsWith("⚠️") -> {
                // Save previous section if exists
                if (currentSection != null && currentSection.isNotEmpty()) {
                    sections.add(DataSection(currentSectionTitle, currentSection.toList()))
                }
                // Start new section
                currentSectionTitle = trimmedLine.removePrefix("📋 ").removePrefix("⚠️ ")
                currentSection = mutableListOf()
            }

            // Data lines (contain ":")
            trimmedLine.contains(":") && !trimmedLine.startsWith("━") && !trimmedLine.startsWith("📌") -> {
                val parts = trimmedLine.split(":", limit = 2)
                if (parts.size == 2) {
                    val label = parts[0].trim().removePrefix("🚢 ").removePrefix("🔢 ")
                        .removePrefix("📍 ").removePrefix("⚓ ").removePrefix("🎯 ")
                        .removePrefix("📏 ").removePrefix("•").trim()
                    val value = parts[1].trim()
                    currentSection?.add(DataItem(label, value))
                }
            }

            // Sub-items (start with "•" or spaces)
            (trimmedLine.startsWith("•") || trimmedLine.startsWith("   •")) && trimmedLine.contains(":") -> {
                val cleanLine = trimmedLine.removePrefix("•").removePrefix("   •").trim()
                val parts = cleanLine.split(":", limit = 2)
                if (parts.size == 2) {
                    currentSection?.add(DataItem(parts[0].trim(), parts[1].trim()))
                }
            }
        }
    }

    // Add last section
    if (currentSection != null && currentSection.isNotEmpty()) {
        sections.add(DataSection(currentSectionTitle, currentSection.toList()))
    }

    // Detect if it's pending or rejected based on content
    val isPending = data.contains("قيد المعالجة") ||
            data.contains("قيد المراجعة") ||
            data.contains("PENDING") ||
            data.contains("الطلب قيد المعالجة")

    // Extract rejection/pending reason
    val statusMessage = when {
        isPending -> {
            // Extract pending message
            lines.find {
                it.contains("قيد المعالجة") ||
                        it.contains("قيد المراجعة")
            }?.trim() ?: "الطلب قيد المعالجة. يرجى الانتظار حتى اكتمال عملية التحقق من الفحص"
        }
        else -> {
            // Extract rejection reason
            lines.find { it.contains("📌 سبب الرفض:") }?.let { line ->
                val index = lines.indexOf(line)
                lines.drop(index + 1).joinToString("\n").trim()
            } ?: "تم رفض الطلب بسبب مشاكل في البيانات المقدمة"
        }
    }

    return ParsedRequestData(
        title = "تفاصيل الطلب",
        status = if (isPending) RequestStatus.PENDING else RequestStatus.REJECTED,
        statusTitle = if (isPending) "طلب قيد المعالجة" else "تم رفض الطلب",
        statusMessage = statusMessage,
        sections = sections.filter { it.items.isNotEmpty() }
    )
}

/**
 * Parse attachments/general request data
 */
private fun parseAttachmentsData(data: String): ParsedRequestData {
    // Simple key-value parsing
    val items = data.lines()
        .filter { it.contains(":") }
        .mapNotNull { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                DataItem(parts[0].trim(), parts[1].trim())
            } else null
        }

    return ParsedRequestData(
        title = "تفاصيل الطلب",
        status = RequestStatus.PENDING,
        statusTitle = "قيد المراجعة",
        statusMessage = "طلبك قيد المراجعة من قبل الجهات المختصة",
        sections = listOf(
            DataSection("بيانات الطلب", items)
        )
    )
}
