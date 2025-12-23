package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.ValidationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarineUnitSelectorManager(
    modifier: Modifier = Modifier,
    units: List<MarineUnit>,
    selectedUnitIds: List<String>,
    addNewUnit: () -> Unit,
    allowMultipleSelection: Boolean = false,
    showOwnedUnitsWarning: Boolean = false,
    showAddNewButton: Boolean = false,
    onSelectionChange: (List<String>) -> Unit,
    validationState: ValidationState = ValidationState.Idle,
    onMarineUnitSelected: ((String) -> Unit)? = null
) {
    val extraColors = LocalExtraColors.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedUnitForDetails by remember { mutableStateOf<MarineUnit?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // فصل السفن حسب حالة التفعيل
    val activeUnits = units.filter { it.isActive }
    val nonActiveUnits = units.filter { !it.isActive }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showAddNewButton == true) {
            Button(
                onClick = addNewUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalExtraColors.current.startServiceButton
                ),
                border = ButtonDefaults.outlinedButtonBorder(
                    enabled = true
                )
            ) {
                Text(
                    text = localizedApp(R.string.add_ship),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // NEW: Validation loading indicator
        if (validationState is ValidationState.Validating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(Color(0xFFF3F4F6))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "جاري التحقق من الوحدة البحرية...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }

        // عرض عدد السفن المختارة
//        if (selectedUnitIds.isNotEmpty()) {
//            Text(
//                text = localizedApp(R.string.num_of_ships) + ": ${selectedUnitIds.size}",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Medium,
//                color = extraColors.whiteInDarkMode
//            )
//        }

        // السفن النشطة (قابلة للاختيار)
        activeUnits.forEach { unit ->
            MarineUnitSelectionCard(
                unit = unit,
                isSelected = selectedUnitIds.contains(unit.id),
                isValidating = validationState is ValidationState.Validating && selectedUnitIds.contains(unit.id),
                onToggleSelection = {
                    if (onMarineUnitSelected != null) {
                        val newSelection = if (allowMultipleSelection) {
                            if (selectedUnitIds.contains(unit.id)) {
                                selectedUnitIds - unit.id
                            } else {
                                selectedUnitIds + unit.id
                            }
                        } else {
                            if (selectedUnitIds.contains(unit.id)) {
                                emptyList()
                            } else {
                                listOf(unit.id)
                            }
                        }
                        onSelectionChange(newSelection)
                        onMarineUnitSelected(unit.id)
                    } else {
                        val newSelection = if (allowMultipleSelection) {
                            if (selectedUnitIds.contains(unit.id)) {
                                selectedUnitIds - unit.id
                            } else {
                                selectedUnitIds + unit.id
                            }
                        } else {
                            if (selectedUnitIds.contains(unit.id)) {
                                emptyList()
                            } else {
                                listOf(unit.id)
                            }
                        }
                        onSelectionChange(newSelection)
                    }
                },
                onShowDetails = {
                    selectedUnitForDetails = unit
                    showBottomSheet = true
                },
                isSelectable = true
            )
        }

        // رسالة تحذير عند وجود سفن غير نشطة
        if (nonActiveUnits.isNotEmpty()) {
            NonActiveWarningCard()
        }

        // السفن غير النشطة (عرض فقط بدون اختيار)
        nonActiveUnits.forEach { unit ->
            MarineUnitSelectionCard(
                unit = unit,
                isSelected = false,
                isValidating = false,
                onToggleSelection = {},
                onShowDetails = {
                    selectedUnitForDetails = unit
                    showBottomSheet = true
                },
                isSelectable = false
            )
        }
    }

    // Bottom Sheet للتفاصيل الكاملة
    if (showBottomSheet && selectedUnitForDetails != null) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = extraColors.background,
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
        ) {
            MarineUnitBottomSheet(unit = selectedUnitForDetails!!)
        }
    }
}

@Composable
private fun MarineUnitSelectionCard(
    unit: MarineUnit,
    isSelected: Boolean,
    isValidating: Boolean = false,
    onToggleSelection: () -> Unit,
    onShowDetails: () -> Unit,
    isSelectable: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )
    val extraColors = LocalExtraColors.current
    val cardBg = if (isSelectable) extraColors.cardBackground else extraColors.cardBackground.copy(alpha = 0.6f)
    val titleColor = if (isSelectable) extraColors.whiteInDarkMode else extraColors.whiteInDarkMode.copy(alpha = 0.6f)
    val checkboxBorder = if (isSelectable) {
        if (isSelected) Color(0xFF1E3A5F) else Color(0xFFD1D5DB)
    } else {
        extraColors.textSubTitle.copy(alpha = 0.4f)
    }
    val checkboxFill = if (isSelectable && isSelected) Color(0xFF1E3A5F) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Color(0xFF1E3A5F), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSelectable) extraColors.iconGreyBackground
                                else extraColors.iconGreyBackground.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🚢", fontSize = 20.sp)
                    }

                    Text(
                        text = unit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = titleColor,
                        fontSize = 16.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.rotate(rotationAngle)
                    )

                    // NEW: Show loading indicator if validating
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF1E3A5F)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(
                                    width = 2.dp,
                                    color = checkboxBorder,
                                    shape = CircleShape
                                )
                                .background(
                                    color = checkboxFill,
                                     shape = CircleShape
                                 )
                                .clickable(enabled = isSelectable, onClick = onToggleSelection),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    MarineInfoRow(label = "نوع الوحدة البحرية", value = unit.type)
                    MarineInfoRow(label = "رقم IMO", value = unit.imoNumber?.toString() ?: "-")
                    MarineInfoRow(label = "رمز النداء", value = unit.callSign)
                    MarineInfoRow(label = "رقم الهوية البحرية", value = unit.maritimeId)
                    MarineInfoRow(label = "ميناء التسجيل", value = unit.registrationPort)
                    MarineInfoRow(label = "النشاط البحري", value = unit.activity)

                    Button(
                        onClick = onShowDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalExtraColors.current.startServiceButton
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(
                            enabled = true
                        )
                    ) {
                        Text(
                            text = "عرض جميع البيانات",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarineInfoRow(label: String, value: String) {
    val extraColors = LocalExtraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(extraColors.cardBackground2.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.whiteInDarkMode,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.textSubTitle,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun WarningCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تنبيه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B5D00),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "هذه السفن معلقة أو موقوفة ولن يتم السماح باستغلالها، نظرًا لأنها مسجلة مع رهونات نشطة، مخالفات، واحتجازات. يُرجى مراجعة تفاصيل كل سفينة قبل اتخاذ أي إجراء.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B5D00),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFFFFA726), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun NonActiveWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFA726), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("!", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "هناك سفن غير نشطة لا يمكن اختيارها",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B5D00)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "هذه السفن معلقة أو موقوفة ولن يتم السماح باستغلالها، نظرًا لأنها مسجلة مع رهونات نشطة، مخالفات، واحتجازات. يُرجى مراجعة تفاصيل كل سفينة قبل اتخاذ أي إجراء.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B5D00)
                )
            }
        }
    }
}

// نقل الـ Bottom Sheet Components هنا كمان
@Composable
private fun MarineUnitBottomSheet(unit: MarineUnit) {
    val extraColors = LocalExtraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "بيانات الوحدة البحرية",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = extraColors.whiteInDarkMode
        )


        // Dimensions Section - Show only if has data
        if (!unit.totalLength.isNullOrEmpty() || !unit.lengthBetweenPerpendiculars.isNullOrEmpty() ||
            !unit.totalWidth.isNullOrEmpty() || !unit.draft.isNullOrEmpty() ||
            !unit.height.isNullOrEmpty() || !unit.numberOfDecks.isNullOrEmpty()) {
            ExpandableBottomSheetSection(
                title = "الأبعاد",
                content = {
                    if (!unit.totalLength.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "الطول الكلي", value = unit.totalLength!!)
                    }
                    if (!unit.lengthBetweenPerpendiculars.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "الطول بين العموديين", value = unit.lengthBetweenPerpendiculars!!)
                    }
                    if (!unit.totalWidth.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "العرض الكلي", value = unit.totalWidth!!)
                    }
                    if (!unit.draft.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "الغاطس", value = unit.draft!!)
                    }
                    if (!unit.height.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "الإرتفاع", value = unit.height!!)
                    }
                    if (!unit.numberOfDecks.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "عدد الطوابق", value = unit.numberOfDecks!!)
                    }
                }
            )
        }

        // Capacity Section - Show only if has data
        if (unit.totalCapacity.isNotEmpty() || !unit.containerCapacity.isNullOrEmpty()) {
            ExpandableBottomSheetSection(
                title = "السعة والحمولة",
                content = {
                    if (unit.totalCapacity.isNotEmpty()) {
                        BottomSheetInfoCard(label = "الحمولة الإجمالية", value = unit.totalCapacity)
                    }
                    if (!unit.containerCapacity.isNullOrEmpty() && unit.containerCapacity != "-") {
                        BottomSheetInfoCard(label = "سعة الحاويات", value = unit.containerCapacity!!)
                    }
                }
            )
        }

        // Violations Section - Show only if has violations or detentions
        if ((!unit.violationsCount.isNullOrEmpty() && unit.violationsCount != "0") ||
            (!unit.detentionsCount.isNullOrEmpty() && unit.detentionsCount != "0")) {
            ExpandableBottomSheetSection(
                title = "المخالفات والاحتجازات",
                content = {
                    if (!unit.violationsCount.isNullOrEmpty() && unit.violationsCount != "0") {
                        BottomSheetInfoCard(label = "عدد المخالفات", value = unit.violationsCount!!)
                    }
                    if (!unit.detentionsCount.isNullOrEmpty() && unit.detentionsCount != "0") {
                        BottomSheetInfoCard(label = "عدد الاحتجازات", value = unit.detentionsCount!!)
                    }
                }
            )
        }

        // Debts Section - Show only if has debts
        if ((!unit.amountDue.isNullOrEmpty() && unit.amountDue != "0 ريال") ||
            (!unit.paymentStatus.isNullOrEmpty() && unit.paymentStatus != "مسدد")) {
            ExpandableBottomSheetSection(
                title = "الديون والمستحقات",
                content = {
                    if (!unit.amountDue.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "المبلغ المستحق", value = unit.amountDue!!)
                    }
                    if (!unit.paymentStatus.isNullOrEmpty()) {
                        BottomSheetInfoCard(label = "حالة السداد", value = unit.paymentStatus!!)
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomSheetInfoCard(label: String, value: String) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(extraColors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium , color = extraColors.whiteInDarkMode)
            Text(text = value, fontSize = 14.sp, color = extraColors.textSubTitle)
        }
    }
}

@Composable
private fun ExpandableBottomSheetSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground2.copy(alpha = 0.1f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp , color = extraColors.whiteInDarkMode)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = extraColors.textBlueSubTitle
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}