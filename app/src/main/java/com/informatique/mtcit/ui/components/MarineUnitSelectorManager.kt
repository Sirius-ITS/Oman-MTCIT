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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarineUnitSelectorManager(
    modifier: Modifier = Modifier,
    units: List<MarineUnit>,
    selectedUnitIds: List<String>,
    allowMultipleSelection: Boolean = true,
    showOwnedUnitsWarning: Boolean = true,
    onSelectionChange: (List<String>) -> Unit
) {
    val extraColors = LocalExtraColors.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedUnitForDetails by remember { mutableStateOf<MarineUnit?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // فصل السفن حسب الملكية
    val nonOwnedUnits = units.filter { !it.isOwned }
    val ownedUnits = units.filter { it.isOwned }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // عرض عدد السفن المختارة
        if (selectedUnitIds.isNotEmpty()) {
            Text(
                text = localizedApp(R.string.num_of_ships) + ": ${selectedUnitIds.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = extraColors.whiteInDarkMode
            )
        }

        // السفن غير المملوكة
        nonOwnedUnits.forEach { unit ->
            MarineUnitSelectionCard(
                unit = unit,
                isSelected = selectedUnitIds.contains(unit.maritimeId),
                onToggleSelection = {
                    val newSelection = if (allowMultipleSelection) {
                        if (selectedUnitIds.contains(unit.maritimeId)) {
                            selectedUnitIds - unit.maritimeId
                        } else {
                            selectedUnitIds + unit.maritimeId
                        }
                    } else {
                        if (selectedUnitIds.contains(unit.maritimeId)) {
                            emptyList()
                        } else {
                            listOf(unit.maritimeId)
                        }
                    }
                    onSelectionChange(newSelection)
                },
                onShowDetails = {
                    selectedUnitForDetails = unit
                    showBottomSheet = true
                }
            )
        }

        // Warning Card للسفن المملوكة
        if (showOwnedUnitsWarning && ownedUnits.isNotEmpty()) {
            WarningCard()

            ownedUnits.forEach { unit ->
                MarineUnitSelectionCard(
                    unit = unit,
                    isSelected = selectedUnitIds.contains(unit.maritimeId),
                    onToggleSelection = {
                        val newSelection = if (allowMultipleSelection) {
                            if (selectedUnitIds.contains(unit.maritimeId)) {
                                selectedUnitIds - unit.maritimeId
                            } else {
                                selectedUnitIds + unit.maritimeId
                            }
                        } else {
                            if (selectedUnitIds.contains(unit.maritimeId)) {
                                emptyList()
                            } else {
                                listOf(unit.maritimeId)
                            }
                        }
                        onSelectionChange(newSelection)
                    },
                    onShowDetails = {
                        selectedUnitForDetails = unit
                        showBottomSheet = true
                    }
                )
            }
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
    onToggleSelection: () -> Unit,
    onShowDetails: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

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
        colors = CardDefaults.cardColors(LocalExtraColors.current.cardBackground),
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
                            .background(LocalExtraColors.current.iconGreyBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🚢", fontSize = 20.sp)
                    }

                    Text(
                        text = unit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = LocalExtraColors.current.whiteInDarkMode,
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

                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFFD1D5DB),
                                shape = CircleShape
                            )
                            .background(
                                color = if (isSelected) Color(0xFF1E3A5F) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable(onClick = onToggleSelection),
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
                    MarineInfoRow(label = "رقم IMO", value = unit.imoNumber)
                    MarineInfoRow(label = "رمز النداء", value = unit.callSign)
                    MarineInfoRow(label = "رقم الهوية البحرية", value = unit.maritimeId)
                    MarineInfoRow(label = "ميناء التسجيل", value = unit.registrationPort)
                    MarineInfoRow(label = "النشاط البحري", value = unit.activity)

                    Button(
                        onClick = onShowDetails,
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
                    text = "هذه السفن مملوكة أو موظفة، ولن تتم السماح بتعاملها بشكل أنشطة يحتاج لها السماح.",
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


        ExpandableBottomSheetSection(
            title = "الأبعاد",
            content = {
                BottomSheetInfoCard(label = "الطول الكلي", value = "٢٤٠ متر")
                BottomSheetInfoCard(label = "الطول بين العموديين", value = "٢١٠ متر")
                BottomSheetInfoCard(label = "العرض الكلي", value = "٣٣ متر")
                BottomSheetInfoCard(label = "الغاطس", value = "١٠ أمتار")
                BottomSheetInfoCard(label = "الإرتفاع", value = "٤٥ متر")
                BottomSheetInfoCard(label = "عدد الطوابق", value = "9")
            }
        )

        // Capacity Section
        ExpandableBottomSheetSection(
            title = "السعة والحمولة",
            content = {
                BottomSheetInfoCard(label = "الحمولة الإجمالية", value = "٥٠٠٠٠ طن")
                BottomSheetInfoCard(label = "سعة الحاويات", value = "٤٥٠٠ حاوية")
            }
        )

        // Violations Section
        ExpandableBottomSheetSection(
            title = "المخالفات والاحتجازات",
            content = {
                BottomSheetInfoCard(label = "عدد المخالفات", value = "٢")
                BottomSheetInfoCard(label = "عدد الاحتجازات", value = "٠")
            }
        )

        // Debts Section
        ExpandableBottomSheetSection(
            title = "الديون والمستحقات",
            content = {
                BottomSheetInfoCard(label = "المبلغ المستحق", value = "١٢٠٠ ريال")
                BottomSheetInfoCard(label = "حالة السداد", value = "مستحق")
            }
        )
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