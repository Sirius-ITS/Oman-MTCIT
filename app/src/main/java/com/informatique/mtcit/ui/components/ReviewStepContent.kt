package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.shared.MarineUnit
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
    modifier: Modifier = Modifier,
    onCheckChanged: (Boolean) -> Unit,
    checked: Boolean
) {
    val extraColors = LocalExtraColors.current

    // Check if there's any data
    if (formData.isEmpty()) {
        Text(
            text = localizedApp(R.string.no_data_to_review),
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        )
    } else {
        // Display each step as an expandable card
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {

            Card(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = extraColors.blue2.copy(alpha = 0.05f)),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "المبلغ الواجب دفعه",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium ,
                        color = extraColors.whiteInDarkMode
                    )

                    Text(
                        modifier = Modifier.weight(1f),
                        text = "50 ريال عماني",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.whiteInDarkMode,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = extraColors.whiteInDarkMode)
                }
            }

            steps.forEachIndexed { index, step ->
                // Get fields for this step that have values
                if (index == 0) return@forEachIndexed

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
                        isExpandedByDefault = index == 1 // First step expanded by default
                    )
                }
            }

            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = "الان أرسل طلبك", //localizedApp(currentStepData.descriptionRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = extraColors.whiteInDarkMode
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = "من خلال تقديم هذا الطلب, فإنك تؤكد علي حد علمك أن التفاصيل التي تقدمها صحيحة.", //localizedApp(currentStepData.descriptionRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = extraColors.textSubTitle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomCheckBox(
                        checked = checked,
                        onCheckedChange = onCheckChanged,
                        label = "إقرار ذاتي"
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = "أقرّ بأن جميع المعلومات المذكورة صحيحة ودقيقة، وأتعهد بإبلاغ الإدارة بأي تغيير يطرأ عليها. وأدرك أن تقديم بيانات أو مستندات غير صحيحة قد يؤدي إلى بطلان الطلب وتحميلي المسؤولية القانونية.", //localizedApp(currentStepData.descriptionRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textSubTitle
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
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(14.dp)
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
                            fontWeight = FontWeight.Medium
                        ),
                        color = extraColors.whiteInDarkMode
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = localizedApp(step.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Expand/Collapse Icon
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = extraColors.whiteInDarkMode,
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
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
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

            is com.informatique.mtcit.common.FormField.MarineUnitSelector -> {
                DisplaySelectedMarineUnits(value, field.units, extraColors)
            }

            is com.informatique.mtcit.common.FormField.FileUpload -> {
                // This is a file upload field - show filename
                // Field Label
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
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
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
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
                    color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DisplayRegularValue(value)
            }
        }
    }
}

@Composable
private fun DisplaySelectedMarineUnits(
    value: String,
    allUnits: List<MarineUnit>,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    // Parse selected unit IDs from JSON
    val selectedUnitIds = remember(value) {
        try {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Get the actual unit objects
    val selectedUnits = remember(selectedUnitIds, allUnits) {
        allUnits.filter { it.maritimeId in selectedUnitIds }
    }

    if (selectedUnits.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            selectedUnits.forEachIndexed { index, unit ->
                ExpandableMarineUnitReviewCard(
                    unit = unit,
                    index = index,
                    extraColors = extraColors
                )
            }
        }
    }
}


@Composable
private fun ExpandableMarineUnitReviewCard(
    unit: MarineUnit,
    index: Int,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors
) {
    var isExpanded by remember { mutableStateOf(index == 0) } // أول سفينة مفتوحة

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground2.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ship Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(extraColors.startServiceButton.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🚢", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = unit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.whiteInDarkMode,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = unit.type,
                                style = MaterialTheme.typography.bodyMedium,
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            // Badge for ownership status
                            if (unit.isOwned) {
                                Surface(
                                    color = Color(0xFFFFA726).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "مملوكة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFA726),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.whiteInDarkMode,
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

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Basic Information
                        Text(
                            text = "البيانات الأساسية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.startServiceButton,
                            fontSize = 14.sp
                        )

                        MarineUnitDetailRow(label = "نوع الوحدة البحرية", value = unit.type)
                        MarineUnitDetailRow(label = "رقم IMO", value = unit.imoNumber)
                        MarineUnitDetailRow(label = "رمز النداء", value = unit.callSign)
                        MarineUnitDetailRow(label = "رقم الهوية البحرية", value = unit.maritimeId)
                        MarineUnitDetailRow(label = "ميناء التسجيل", value = unit.registrationPort)
                        MarineUnitDetailRow(label = "النشاط البحري", value = unit.activity)

                        // Dimensions Section (only if has data)
                        if (unit.totalLength.isNotEmpty() || unit.lengthBetweenPerpendiculars.isNotEmpty() ||
                            unit.totalWidth.isNotEmpty() || unit.draft.isNotEmpty() ||
                            unit.height.isNotEmpty() || unit.numberOfDecks.isNotEmpty()) {

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الأبعاد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.startServiceButton,
                                fontSize = 14.sp
                            )

                            if (unit.totalLength.isNotEmpty()) {
                                MarineUnitDetailRow(label = "الطول الكلي", value = unit.totalLength)
                            }
                            if (unit.lengthBetweenPerpendiculars.isNotEmpty()) {
                                MarineUnitDetailRow(label = "الطول بين العموديين", value = unit.lengthBetweenPerpendiculars)
                            }
                            if (unit.totalWidth.isNotEmpty()) {
                                MarineUnitDetailRow(label = "العرض الكلي", value = unit.totalWidth)
                            }
                            if (unit.draft.isNotEmpty()) {
                                MarineUnitDetailRow(label = "الغاطس", value = unit.draft)
                            }
                            if (unit.height.isNotEmpty()) {
                                MarineUnitDetailRow(label = "الإرتفاع", value = unit.height)
                            }
                            if (unit.numberOfDecks.isNotEmpty()) {
                                MarineUnitDetailRow(label = "عدد الطوابق", value = unit.numberOfDecks)
                            }
                        }

                        // Capacity Section (only if has data)
                        if (unit.totalCapacity.isNotEmpty() || unit.containerCapacity.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "السعة والحمولة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.startServiceButton,
                                fontSize = 14.sp
                            )

                            if (unit.totalCapacity.isNotEmpty()) {
                                MarineUnitDetailRow(label = "الحمولة الإجمالية", value = unit.totalCapacity)
                            }
                            if (unit.containerCapacity.isNotEmpty() && unit.containerCapacity != "-") {
                                MarineUnitDetailRow(label = "سعة الحاويات", value = unit.containerCapacity)
                            }
                        }

                        // Violations Section (only if has violations)
                        if ((unit.violationsCount.isNotEmpty() && unit.violationsCount != "0") ||
                            (unit.detentionsCount.isNotEmpty() && unit.detentionsCount != "0")) {

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "المخالفات والاحتجازات",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA726),
                                fontSize = 14.sp
                            )

                            if (unit.violationsCount.isNotEmpty() && unit.violationsCount != "0") {
                                MarineUnitDetailRow(label = "عدد المخالفات", value = unit.violationsCount)
                            }
                            if (unit.detentionsCount.isNotEmpty() && unit.detentionsCount != "0") {
                                MarineUnitDetailRow(label = "عدد الاحتجازات", value = unit.detentionsCount)
                            }
                        }

                        // Debts Section (only if has debts)
                        if ((unit.amountDue.isNotEmpty() && unit.amountDue != "0 ريال") ||
                            (unit.paymentStatus.isNotEmpty() && unit.paymentStatus != "مسدد")) {

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الديون والمستحقات",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA726),
                                fontSize = 14.sp
                            )

                            if (unit.amountDue.isNotEmpty()) {
                                MarineUnitDetailRow(label = "المبلغ المستحق", value = unit.amountDue)
                            }
                            if (unit.paymentStatus.isNotEmpty()) {
                                MarineUnitDetailRow(label = "حالة السداد", value = unit.paymentStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================
// 4. Helper Function لعرض صف البيانات
// ========================================

@Composable
private fun MarineUnitDetailRow(
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
            color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.End
        )
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
                containerColor = extraColors.cardBackground2.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(0.dp),
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
                        color = extraColors.whiteInDarkMode
                    )
                    Text(
                        text = localizedApp(R.string.file_attached),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.textSubTitle
                    )
                }

                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = extraColors.whiteInDarkMode,
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
            color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = extraColors.whiteInDarkMode,
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
            containerColor = extraColors.cardBackground2.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = extraColors.whiteInDarkMode,
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground2.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon based on owner type
                    Icon(
                        imageVector = if (owner.isCompany) Icons.Default.Business else Icons.Default.Person,
                        contentDescription = null,
                        tint = extraColors.startServiceButton,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${localizedApp(R.string.owner_info)} ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.whiteInDarkMode,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (owner.isCompany) owner.companyName else owner.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            // Badge for owner type
                            Surface(
                                color = extraColors.startServiceButton.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (owner.isCompany) "شركة" else "فرد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extraColors.whiteInDarkMode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.whiteInDarkMode,
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
                        if (owner.isCompany) {
                            // Company Information Section
                            Text(
                                text = "بيانات الشركة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.startServiceButton,
                                fontSize = 14.sp
                            )

                            OwnerDetailRow(
                                label = localizedApp(R.string.company_name),
                                value = owner.companyName
                            )

                            OwnerDetailRow(
                                label = localizedApp(R.string.company_registration_number),
                                value = owner.companyRegistrationNumber
                            )

                            if (owner.companyType.isNotEmpty()) {
                                OwnerDetailRow(
                                    label = "نوع الشركة",
                                    value = owner.companyType
                                )
                            }
                        } else {
                            // Individual Information Section
                            Text(
                                text = "بيانات المالك",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.startServiceButton,
                                fontSize = 14.sp
                            )

                            OwnerDetailRow(
                                label = localizedApp(R.string.owner_full_name_ar),
                                value = owner.fullName
                            )

                            OwnerDetailRow(
                                label = localizedApp(R.string.owner_id_number),
                                value = owner.idNumber
                            )

                            if (owner.nationality.isNotEmpty()) {
                                OwnerDetailRow(
                                    label = localizedApp(R.string.owner_nationality),
                                    value = owner.nationality
                                )
                            }
                        }

                        // Common Information Section
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "معلومات الملكية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.startServiceButton,
                            fontSize = 14.sp
                        )

                        OwnerDetailRow(
                            label = localizedApp(R.string.enter_ownershippercentage),
                            value = "${owner.ownerShipPercentage}%"
                        )

                        // Contact Information Section
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "معلومات الاتصال",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.startServiceButton,
                            fontSize = 14.sp
                        )

                        OwnerDetailRow(
                            label = localizedApp(R.string.email),
                            value = owner.email
                        )

                        OwnerDetailRow(
                            label = localizedApp(R.string.owner_mobile),
                            value = owner.mobile
                        )

                        // Address Information Section
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "معلومات العنوان",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.startServiceButton,
                            fontSize = 14.sp
                        )

                        OwnerDetailRow(
                            label = localizedApp(R.string.owner_address),
                            value = owner.address
                        )

                        if (owner.city.isNotEmpty()) {
                            OwnerDetailRow(
                                label = "المدينة",
                                value = owner.city
                            )
                        }

                        if (owner.country.isNotEmpty()) {
                            OwnerDetailRow(
                                label = "الدولة",
                                value = owner.country
                            )
                        }

                        if (owner.postalCode.isNotEmpty()) {
                            OwnerDetailRow(
                                label = "الرمز البريدي",
                                value = owner.postalCode
                            )
                        }

                        // Document Information
                        if (owner.documentName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "المستندات",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extraColors.startServiceButton,
                                fontSize = 14.sp
                            )

                            OwnerDetailRow(
                                label = localizedApp(R.string.ownership_proof_document),
                                value = owner.documentName
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerDetailRow(
    label: String,
    value: String,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors = LocalExtraColors.current
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value.ifEmpty { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.whiteInDarkMode,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
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