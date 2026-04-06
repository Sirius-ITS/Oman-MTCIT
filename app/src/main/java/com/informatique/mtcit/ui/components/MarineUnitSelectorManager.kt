package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.informatique.mtcit.business.transactions.shared.CoreShipInfo
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.ValidationState
import kotlinx.coroutines.flow.distinctUntilChanged
import com.informatique.mtcit.common.util.LocalAppLocale

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
    onMarineUnitSelected: ((String) -> Unit)? = null,
    // ✅ INFINITE SCROLL params
    onLoadMore: (() -> Unit)? = null,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    // ✅ Ship details API fetch for "عرض جميع البيانات"
    fetchShipDetails: (suspend (String) -> Result<CoreShipInfo>)? = null
) {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"

    // ✅ SEARCH: local search query state
    var searchQuery by remember { mutableStateOf("") }

    // ✅ SEARCH: filter units by name or maritime ID
    val filteredUnits = remember(units, searchQuery) {
        if (searchQuery.isBlank()) units
        else units.filter { unit ->
            unit.name.contains(searchQuery, ignoreCase = true) ||
            unit.shipName.contains(searchQuery, ignoreCase = true) ||
            unit.maritimeId.contains(searchQuery, ignoreCase = true)
        }
    }

    // فصل السفن حسب حالة التفعيل (on filtered list)
    val activeUnits = filteredUnits.filter { it.isActive }
    val nonActiveUnits = filteredUnits.filter { !it.isActive }

    // ✅ INFINITE SCROLL: LazyListState to detect when user reaches the end
    val lazyListState = rememberLazyListState()

    // ✅ INFINITE SCROLL: Trigger load-more when user reaches the last visible item
    LaunchedEffect(lazyListState, hasMore) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Pair(totalItems, lastVisible)
        }
            .distinctUntilChanged()
            .collect { (totalItems, lastVisible) ->
                if (totalItems > 0 && lastVisible >= totalItems - 2 && hasMore && !isLoadingMore) {
                    onLoadMore?.invoke()
                }
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showAddNewButton) {
            Button(
                onClick = addNewUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalExtraColors.current.startServiceButton
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Text(
                    text = localizedApp(R.string.add_ship),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // Validation loading indicator
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isAr) "جاري التحقق من الوحدة البحرية..." else "Checking marine unit...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }

        // ✅ SEARCH: Search bar (only shown when there are ships to search through)
        if (units.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = extraColors.whiteInDarkMode
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (isAr) "ابحث باسم السفينة أو الرقم البحري" else "Search by ship name or maritime number",
                                        fontSize = 14.sp,
                                        color = Color(0xFFBDBDBD)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    // ✅ Clear button when there's a query
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ✅ INFINITE SCROLL: LazyColumn for ships list (fixed height avoids nested scroll conflict)
        val listHeight = if (filteredUnits.isEmpty()) 80.dp else minOf((filteredUnits.size * 88).dp, 480.dp)
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = listHeight),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ✅ SEARCH: Empty state when no results
            if (filteredUnits.isEmpty() && searchQuery.isNotBlank()) {
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAr) "لا توجد نتائج لـ \"$searchQuery\"" else "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }

            // ✅ Active ships (selectable)
            items(activeUnits, key = { it.id }) { unit ->
                MarineUnitSelectionCard(
                    unit = unit,
                    isSelected = selectedUnitIds.contains(unit.id),
                    isValidating = validationState is ValidationState.Validating && selectedUnitIds.contains(unit.id),
                    onToggleSelection = {
                        val newSelection = if (allowMultipleSelection) {
                            if (selectedUnitIds.contains(unit.id)) selectedUnitIds - unit.id
                            else selectedUnitIds + unit.id
                        } else {
                            if (selectedUnitIds.contains(unit.id)) emptyList()
                            else listOf(unit.id)
                        }
                        onSelectionChange(newSelection)
                        onMarineUnitSelected?.invoke(unit.id)
                    },
                    fetchShipDetails = fetchShipDetails,
                    isSelectable = true
                )
            }

            // ✅ Warning card before inactive ships
            if (nonActiveUnits.isNotEmpty()) {
                item(key = "non_active_warning") {
                    NonActiveWarningCard()
                }
            }

            // ✅ Inactive ships (display only)
            items(nonActiveUnits, key = { "inactive_${it.id}" }) { unit ->
                MarineUnitSelectionCard(
                    unit = unit,
                    isSelected = false,
                    isValidating = false,
                    onToggleSelection = {},
                    fetchShipDetails = fetchShipDetails,
                    isSelectable = false
                )
            }

            // ✅ INFINITE SCROLL: Loading footer
            if (isLoadingMore) {
                item(key = "loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isAr) "جاري تحميل المزيد..." else "Loading more...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }

}

// ============================================================
// Selection Card
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarineUnitSelectionCard(
    unit: MarineUnit,
    isSelected: Boolean,
    isValidating: Boolean = false,
    onToggleSelection: () -> Unit,
    fetchShipDetails: (suspend (String) -> Result<CoreShipInfo>)? = null,
    isSelectable: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"

    // ✅ Per-card details state
    var coreShipInfo by remember { mutableStateOf<CoreShipInfo?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var showFullBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ✅ Auto-fetch details when card first expands (for Arabic names, IMO, callSign)
    // try/finally ensures isLoadingDetails is ALWAYS reset even if the coroutine is cancelled
    // (e.g. user expands → collapses → expands rapidly)
    LaunchedEffect(expanded) {
        if (expanded && coreShipInfo == null && fetchShipDetails != null) {
            try {
                isLoadingDetails = true
                fetchShipDetails(unit.id).onSuccess { coreShipInfo = it }
            } finally {
                isLoadingDetails = false
            }
        }
    }

    // ✅ Derived display values — prefer Arabic from coreShipInfo, fall back to list-API data
    val displayType     = coreShipInfo?.shipType?.ifEmpty { unit.type } ?: unit.type
    val displayImo      = coreShipInfo?.imoNumber?.ifEmpty { unit.imoNumber ?: "" } ?: (unit.imoNumber ?: "")
    val displayCallSign = coreShipInfo?.callSign?.ifEmpty { unit.callSign } ?: unit.callSign
    val displayMaritimeId = coreShipInfo?.officialNumber?.ifEmpty { unit.maritimeId } ?: unit.maritimeId
    val displayPort     = coreShipInfo?.portOfRegistry?.ifEmpty { unit.registrationPort } ?: unit.registrationPort
    val displayActivity = coreShipInfo?.marineActivity?.ifEmpty { unit.activity } ?: unit.activity
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
            .then(if (isSelected) Modifier.border(2.dp, Color(0xFF1E3A5F), RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(0.7f),
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
                        maxLines = 2,
                        fontWeight = FontWeight.Medium,
                        color = titleColor,
                        fontSize = 16.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.rotate(rotationAngle)
                    )
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
                                .border(width = 2.dp, color = checkboxBorder, shape = CircleShape)
                                .background(color = checkboxFill, shape = CircleShape)
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

            // Expanded content – quick data rows + "عرض جميع البيانات"
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    // ✅ Show small loading indicator while fetching Arabic names
                    if (isLoadingDetails) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "جاري تحميل البيانات..." else "Loading data...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }

                    // ✅ Quick data rows using Arabic values from coreShipInfo where available
                    if (displayType.isNotEmpty()) {
                        MarineInfoRow(label = if (isAr) "نوع الوحدة البحرية" else "Marine Unit Type", value = displayType)
                    }
                    if (displayImo.isNotEmpty()) {
                        MarineInfoRow(label = if (isAr) "رقم IMO" else "IMO Number", value = displayImo)
                    }
                    if (displayCallSign.isNotEmpty()) {
                        MarineInfoRow(label = if (isAr) "رمز النداء" else "Call Sign", value = displayCallSign)
                    }
                    if (displayMaritimeId.isNotEmpty()) {
                        MarineInfoRow(label = if (isAr) "رقم الهوية البحرية" else "Maritime ID Number", value = displayMaritimeId)
                    }
                    if (displayPort.isNotEmpty()) {
                        MarineInfoRow(label = if (isAr) "ميناء التسجيل" else "Registration Port", value = displayPort)
                    }
                    if (displayActivity.isNotEmpty()) {
                        MarineInfoRow(label = if (isAr) "النشاط البحري" else "Maritime Activity", value = displayActivity)
                    }

                    Button(
                        onClick = { showFullBottomSheet = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalExtraColors.current.startServiceButton
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Text(
                            text = if (isAr) "عرض جميع البيانات" else "View All Data",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // ✅ Full details bottom sheet (per-card)
    if (showFullBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { showFullBottomSheet = false },
            sheetState = sheetState,
            containerColor = LocalExtraColors.current.background,
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
        ) {
            when {
                isLoadingDetails -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Text(
                                text = if (isAr) "جاري تحميل بيانات السفينة..." else "Loading ship data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalExtraColors.current.textSubTitle
                            )
                        }
                    }
                }
                coreShipInfo != null -> CoreShipInfoBottomSheet(info = coreShipInfo!!)
                else -> MarineUnitBottomSheet(unit = unit)
            }
        }
    }
}

// ============================================================
// Shared info rows
// ============================================================

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
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = extraColors.whiteInDarkMode, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = extraColors.textSubTitle, fontSize = 14.sp)
    }
}

@Composable
private fun WarningCard() {
    val isAr = LocalAppLocale.current.language == "ar"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (isAr) "تنبيه" else "Warning", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF6B5D00), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAr) "هذه السفن معلقة أو موقوفة ولن يتم السماح باستغلالها، نظرًا لأنها مسجلة مع رهونات نشطة، مخالفات، واحتجازات. يُرجى مراجعة تفاصيل كل سفينة قبل اتخاذ أي إجراء." else "These ships are suspended and cannot be operated, as they are registered with active mortgages, violations, and detentions. Please review each ship before taking action.",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B5D00), fontSize = 13.sp, lineHeight = 20.sp
                )
            }
            Box(modifier = Modifier.size(24.dp).background(Color(0xFFFFA726), CircleShape), contentAlignment = Alignment.Center) {
                Text(text = "!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun NonActiveWarningCard() {
    val isAr = LocalAppLocale.current.language == "ar"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(32.dp).background(Color(0xFFFFA726), CircleShape), contentAlignment = Alignment.Center) {
                Text("!", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (isAr) "هناك سفن غير نشطة لا يمكن اختيارها" else "There are inactive ships that cannot be selected", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B5D00))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAr) "هذه السفن معلقة أو موقوفة ولن يتم السماح باستغلالها، نظرًا لأنها مسجلة مع رهونات نشطة، مخالفات، واحتجازات. يُرجى مراجعة تفاصيل كل سفينة قبل اتخاذ أي إجراء." else "These ships are suspended and cannot be operated, as they are registered with active mortgages, violations, and detentions. Please review each ship before taking action.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B5D00)
                )
            }
        }
    }
}

// ============================================================
// ✅ NEW: CoreShipInfo bottom sheet (from API /coreshipinfo/ship/{id})
// ============================================================

@Composable
private fun CoreShipInfoBottomSheet(info: CoreShipInfo) {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isAr) "بيانات الوحدة البحرية" else "Marine Unit Data",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = extraColors.whiteInDarkMode
        )

        // ── Basic Info ──────────────────────────────────────────
        ExpandableBottomSheetSection(title = if (isAr) "البيانات الأساسية" else "Basic Data", initiallyExpanded = true) {
            if (info.shipName.isNotEmpty())           BottomSheetInfoCard(if (isAr) "اسم السفينة" else "Ship Name", info.shipName)
            if (info.imoNumber.isNotEmpty())           BottomSheetInfoCard(if (isAr) "رقم IMO" else "IMO Number", info.imoNumber)
            if (info.callSign.isNotEmpty())            BottomSheetInfoCard(if (isAr) "رمز النداء" else "Call Sign", info.callSign)
            if (info.officialNumber.isNotEmpty())      BottomSheetInfoCard(if (isAr) "الرقم الرسمي" else "Official Number", info.officialNumber)
            if (info.registrationNumber.isNotEmpty())  BottomSheetInfoCard(if (isAr) "رقم التسجيل" else "Registration Number", info.registrationNumber)
            if (info.portOfRegistry.isNotEmpty())      BottomSheetInfoCard(if (isAr) "ميناء التسجيل" else "Registration Port", info.portOfRegistry)
            if (info.marineActivity.isNotEmpty())      BottomSheetInfoCard(if (isAr) "النشاط البحري" else "Maritime Activity", info.marineActivity)
            if (info.shipCategory.isNotEmpty())        BottomSheetInfoCard(if (isAr) "فئة السفينة" else "Ship Category", info.shipCategory)
            if (info.shipType.isNotEmpty())            BottomSheetInfoCard(if (isAr) "نوع السفينة" else "Ship Type", info.shipType)
            if (info.buildMaterial.isNotEmpty())       BottomSheetInfoCard(if (isAr) "مادة البناء" else "Building Material", info.buildMaterial)
            if (info.shipBuildYear.isNotEmpty())       BottomSheetInfoCard(if (isAr) "سنة البناء" else "Year of Construction", info.shipBuildYear)
            if (info.buildEndDate.isNotEmpty())        BottomSheetInfoCard(if (isAr) "تاريخ انتهاء البناء" else "Construction End Date", info.buildEndDate)
            BottomSheetInfoCard("نوع التسجيل", if (info.isTemp) "مؤقت" else if (isAr) "دائم" else "Permanent")
        }

        // ── Dimensions & Tonnage ───────────────────────────────
        if (info.vesselLengthOverall.isNotEmpty() || info.vesselBeam.isNotEmpty() ||
            info.vesselDraft.isNotEmpty() || info.grossTonnage.isNotEmpty() || info.netTonnage.isNotEmpty()) {
            ExpandableBottomSheetSection(title = if (isAr) "الأبعاد والحمولة" else "Dimensions and Tonnage") {
                if (info.vesselLengthOverall.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الطول الكلي" else "Overall Length", "${info.vesselLengthOverall} م")
                if (info.vesselBeam.isNotEmpty())          BottomSheetInfoCard(if (isAr) "العرض" else "Width", "${info.vesselBeam} م")
                if (info.vesselDraft.isNotEmpty())         BottomSheetInfoCard(if (isAr) "الغاطس" else "Draft", "${info.vesselDraft} م")
                if (info.grossTonnage.isNotEmpty())        BottomSheetInfoCard(if (isAr) "الحمولة الإجمالية" else "Gross Tonnage", "${info.grossTonnage} طن")
                if (info.netTonnage.isNotEmpty())          BottomSheetInfoCard(if (isAr) "الحمولة الصافية" else "Net Tonnage", "${info.netTonnage} طن")
            }
        }

        // ── Engines ───────────────────────────────────────────
        if (info.engines.isNotEmpty()) {
            ExpandableBottomSheetSection(title = if (isAr) "المحركات (${info.engines.size})" else "Engines (${info.engines.size})") {
                info.engines.forEachIndexed { index, engine ->
                    Text(
                        text = if (isAr) "محرك ${index + 1}" else "Engine ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.textBlueSubTitle,
                        modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp)
                    )
                    if (engine.serialNumber.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الرقم التسلسلي" else "Serial Number", engine.serialNumber)
                    if (engine.engineType.isNotEmpty())   BottomSheetInfoCard(if (isAr) "نوع الوقود" else "Fuel Type", engine.engineType)
                    if (engine.enginePower.isNotEmpty())  BottomSheetInfoCard(if (isAr) "القدرة (حصان)" else "Power (HP)", engine.enginePower)
                    if (engine.engineStatus.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الحالة" else "Status", engine.engineStatus)
                }
            }
        }

        // ── Owners ────────────────────────────────────────────
        if (info.owners.isNotEmpty()) {
            ExpandableBottomSheetSection(title = if (isAr) "الملاك (${info.owners.size})" else "Owners (${info.owners.size})") {
                info.owners.forEachIndexed { index, owner ->
                    Text(
                        text = if (isAr) "مالك ${index + 1}" else "Owner ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.textBlueSubTitle,
                        modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp)
                    )
                    if (owner.ownerName.isNotEmpty())   BottomSheetInfoCard(if (isAr) "الاسم" else "Name", owner.ownerName)
                    if (owner.ownerCivilId.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الرقم المدني" else "Civil Number", owner.ownerCivilId)
                    BottomSheetInfoCard(if (isAr) "نسبة الملكية" else "Ownership Percentage", "${owner.ownershipPercentage.toInt()}%")
                    if (owner.isRepresentative) BottomSheetInfoCard(if (isAr) "الصفة" else "Title", if (isAr) "ممثل قانوني" else "Legal Representative")
                }
            }
        }

        // ── Certifications ───────────────────────────────────
        if (info.certifications.isNotEmpty()) {
            ExpandableBottomSheetSection(title = if (isAr) "الشهادات (${info.certifications.size})" else "Certificates (${info.certifications.size})") {
                info.certifications.forEachIndexed { index, cert ->
                    Text(
                        text = if (isAr) "شهادة ${index + 1}" else "Certificate ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.textBlueSubTitle,
                        modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp)
                    )
                    if (cert.certificationType.isNotEmpty())  BottomSheetInfoCard(if (isAr) "النوع" else "Type", cert.certificationType)
                    if (cert.certificationNumber.isNotEmpty()) BottomSheetInfoCard(if (isAr) "رقم الشهادة" else "Certificate Number", cert.certificationNumber)
                    if (cert.issuedDate.isNotEmpty())          BottomSheetInfoCard(if (isAr) "تاريخ الإصدار" else "Issue Date", cert.issuedDate)
                    if (cert.expiryDate.isNotEmpty())          BottomSheetInfoCard(if (isAr) "تاريخ الانتهاء" else "Expiry Date", cert.expiryDate)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Legacy bottom sheet kept for fallback ─────────────────────
@Composable
private fun MarineUnitBottomSheet(unit: MarineUnit) {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = if (isAr) "بيانات الوحدة البحرية" else "Marine Unit Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = extraColors.whiteInDarkMode)

        if (unit.totalLength.isNotEmpty() || unit.lengthBetweenPerpendiculars.isNotEmpty() ||
            unit.totalWidth.isNotEmpty() || unit.draft.isNotEmpty() || unit.height.isNotEmpty() || unit.numberOfDecks.isNotEmpty()) {
            ExpandableBottomSheetSection(title = if (isAr) "الأبعاد" else "Dimensions") {
                if (unit.totalLength.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الطول الكلي" else "Overall Length", unit.totalLength)
                if (unit.lengthBetweenPerpendiculars.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الطول بين العموديين" else "Length Between Perpendiculars", unit.lengthBetweenPerpendiculars)
                if (unit.totalWidth.isNotEmpty()) BottomSheetInfoCard(if (isAr) "العرض الكلي" else "Overall Width", unit.totalWidth)
                if (unit.draft.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الغاطس" else "Draft", unit.draft)
                if (unit.height.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الإرتفاع" else "Height", unit.height)
                if (unit.numberOfDecks.isNotEmpty()) BottomSheetInfoCard(if (isAr) "عدد الطوابق" else "Number of Decks", unit.numberOfDecks)
            }
        }
        if (unit.totalCapacity.isNotEmpty() || unit.containerCapacity.isNotEmpty()) {
            ExpandableBottomSheetSection(title = if (isAr) "السعة والحمولة" else "Capacity and Tonnage") {
                if (unit.totalCapacity.isNotEmpty()) BottomSheetInfoCard(if (isAr) "الحمولة الإجمالية" else "Gross Tonnage", unit.totalCapacity)
                if (unit.containerCapacity.isNotEmpty() && unit.containerCapacity != "-") BottomSheetInfoCard(if (isAr) "سعة الحاويات" else "Container Capacity", unit.containerCapacity)
            }
        }
    }
}

@Composable
private fun BottomSheetInfoCard(label: String, value: String) {
    val extraColors = LocalExtraColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(extraColors.cardBackground)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = extraColors.whiteInDarkMode)
            Text(text = value, fontSize = 14.sp, color = extraColors.textSubTitle)
        }
    }
}

@Composable
private fun ExpandableBottomSheetSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extraColors.cardBackground2.copy(alpha = 0.1f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = extraColors.whiteInDarkMode)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = extraColors.textBlueSubTitle
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content()
                }
            }
        }
    }
}