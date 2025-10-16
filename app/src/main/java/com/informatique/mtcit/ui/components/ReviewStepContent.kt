package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.serialization.json.Json

/**
 * Generic Review Step Content Component
 * Displays all collected data from previous steps in expandable/collapsible sections
 *
 * Note: This is rendered inside TransactionFormContent's card with header already shown,
 * so we just display the fields in expandable cards per step
 */
@Composable
fun ReviewStepContent(
    steps: List<StepData>,
    formData: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    // Check if there's any data
    if (formData.isEmpty()) {
        Text(
            text = localizedApp(R.string.no_data_to_review),
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.blue2,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        // Display each step as an expandable card
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            steps.forEachIndexed { index, step ->
                // Get fields for this step that have values
                val stepFieldsWithData = step.fields.filter { field ->
                    val value = formData[field.id]
                    !value.isNullOrBlank() && value != "[]"
                }

                // Only show step if it has data (skip empty steps and review step itself)
                if (stepFieldsWithData.isNotEmpty() && step.fields.isNotEmpty()) {
                    ExpandableStepCard(
                        step = step,
                        stepFieldsWithData = stepFieldsWithData,
                        formData = formData,
                        isExpandedByDefault = index == 0 // First step expanded by default
                    )
                }
            }
        }
    }
}

/**
 * Expandable card for each step's data
 */
@Composable
private fun ExpandableStepCard(
    step: StepData,
    stepFieldsWithData: List<com.informatique.mtcit.common.FormField>,
    formData: Map<String, String>,
    isExpandedByDefault: Boolean
) {
    val extraColors = LocalExtraColors.current
    var isExpanded by remember { mutableStateOf(isExpandedByDefault) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp , vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.white),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedApp(step.titleRes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = extraColors.blue1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = localizedApp(step.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.blue2
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Expand/Collapse Icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = extraColors.blue1,
                        modifier = Modifier.padding(6.dp)
                    )
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = extraColors.blue2.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Display fields
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        stepFieldsWithData.forEach { field ->
                            val value = formData[field.id] ?: ""

                            ReviewFieldItem(
                                label = if (field.labelRes != 0) localizedApp(field.labelRes) else field.label,
                                value = value,
                                field = field
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Display individual field data in review (step-like format)
 * Handles special cases like OwnerList (JSON data) and FileUpload (URI)
 */
@Composable
private fun ReviewFieldItem(
    label: String,
    value: String,
    field: com.informatique.mtcit.common.FormField
) {
    val extraColors = LocalExtraColors.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Handle special field types - check by field type
        when (field) {
            is com.informatique.mtcit.common.FormField.OwnerList -> {
                // This is an owner list field - parse JSON
                DisplayOwnerListData(value, extraColors)
            }

            is com.informatique.mtcit.common.FormField.FileUpload -> {
                // This is a file upload field - show filename
                // Field Label
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = extraColors.blue2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DisplayFileAttachment(value, field, extraColors)
            }

            is com.informatique.mtcit.common.FormField.CheckBox -> {
                // Display Yes/No for checkboxes
                // Field Label
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = extraColors.blue2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DisplayRegularValue(
                    if (value == "true") localizedApp(R.string.yes) else localizedApp(R.string.no)
                )
            }

            else -> {
                // Regular text display for TextField, DropDown, DatePicker
                // Field Label
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = extraColors.blue2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DisplayRegularValue(value)
            }
        }
    }
}

/**
 * Display owner list data - parses JSON and shows in cards
 */
@Composable
private fun DisplayOwnerListData(
    value: String,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    val owners = remember(value) {
        try {
            Json.decodeFromString<List<OwnerData>>(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    if (owners.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            owners.forEachIndexed { index, owner ->
                ExpandableOwnerReviewCard(
                    owner = owner,
                    index = index,
                    extraColors = extraColors
                )
            }
        }
    } else {
        // Fallback - if JSON parsing fails, show the raw value
        DisplayRegularValue(value)
    }
}

/**
 * Display file attachment - extracts filename from URI
 */
@Composable
private fun DisplayFileAttachment(
    value: String,
    field: com.informatique.mtcit.common.FormField.FileUpload,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    if (value.isNotBlank()) {
        val fileName = remember(value) {
            // Extract filename from URI
            try {
                when {
                    value.startsWith("content://") -> {
                        // Content URI - extract last segment
                        val lastSegment = value.substringAfterLast("/")
                        if (lastSegment.contains(".")) {
                            lastSegment
                        } else {
                            "document.${field.allowedTypes.firstOrNull() ?: "pdf"}"
                        }
                    }
                    value.startsWith("file://") -> {
                        value.substringAfterLast("/")
                    }
                    value.contains("/") -> {
                        value.substringAfterLast("/")
                    }
                    else -> value
                }
            } catch (_: Exception) {
                "Attached File"
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = extraColors.background
            ),
            elevation = CardDefaults.cardElevation(1.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = extraColors.blue1
                    )
                    Text(
                        text = localizedApp(R.string.file_attached),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.blue2
                    )
                }

                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = extraColors.blue1,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        DisplayRegularValue(localizedApp(R.string.no_file_attached))
    }
}

/**
 * Display owner detail row (label: value)
 */
@Composable
private fun OwnerDetailRow(
    label: String,
    value: String
) {
    val extraColors = LocalExtraColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extraColors.blue2,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = extraColors.blue1,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Display regular field value in a card
 */
@Composable
private fun DisplayRegularValue(value: String) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.grayCard
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = extraColors.blue1,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExpandableOwnerReviewCard(
    owner: OwnerData,
    index: Int,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.white
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clickable Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${localizedApp(R.string.owner_info)} ${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.blue1,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = owner.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.blue2,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.blue1,
                    modifier = Modifier.padding(6.dp)
                )
            }

            // Animated Expandable Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = extraColors.blue2.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Owner Details
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OwnerDetailRow(
                            label = localizedApp(R.string.owner_full_name),
                            value = owner.fullName
                        )
                        OwnerDetailRow(
                            label = localizedApp(R.string.owner_nationality),
                            value = owner.nationality
                        )
                        OwnerDetailRow(
                            label = localizedApp(R.string.owner_id_number),
                            value = owner.idNumber
                        )
                        if (owner.isCompany && owner.companyName.isNotBlank()) {
                            OwnerDetailRow(
                                label = localizedApp(R.string.company_name),
                                value = owner.companyName
                            )
                        }
                    }
                }
            }
        }
    }
}


//@Composable
//private fun DisplayOwnerListData(
//    value: String,
//    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
//) {
//    val owners = remember(value) {
//        try {
//            Json.decodeFromString<List<OwnerData>>(value)
//        } catch (_: Exception) {
//            emptyList()
//        }
//    }
//
//    if (owners.isNotEmpty()) {
//        Column(
//            modifier = Modifier.fillMaxWidth(),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            owners.forEachIndexed { index, owner ->
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = extraColors.background
//                    ),
//                    elevation = CardDefaults.cardElevation(2.dp),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text(
//                            text = "${localizedApp(R.string.owner_info)} ${index + 1}",
//                            style = MaterialTheme.typography.titleSmall.copy(
//                                fontWeight = FontWeight.Bold
//                            ),
//                            color = extraColors.blue1,
//                            modifier = Modifier.padding(bottom = 12.dp)
//                        )
//
//                        // Owner Details
//                        Column(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalArrangement = Arrangement.spacedBy(8.dp)
//                        ) {
//                            OwnerDetailRow(
//                                label = localizedApp(R.string.owner_full_name),
//                                value = owner.fullName
//                            )
//                            OwnerDetailRow(
//                                label = localizedApp(R.string.owner_nationality),
//                                value = owner.nationality
//                            )
//                            OwnerDetailRow(
//                                label = localizedApp(R.string.owner_id_number),
//                                value = owner.idNumber
//                            )
//                            if (owner.isCompany && owner.companyName.isNotBlank()) {
//                                OwnerDetailRow(
//                                    label = localizedApp(R.string.company_name),
//                                    value = owner.companyName
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    } else {
//        // Fallback - if JSON parsing fails, show the raw value
//        DisplayRegularValue(value)
//    }
//}