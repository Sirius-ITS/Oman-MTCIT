package com.informatique.mtcit.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.ui.viewmodels.MarineRegistrationViewModel
import com.informatique.mtcit.ui.viewmodels.FileNavigationEvent
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.localizedApp
import androidx.core.net.toUri
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.informatique.mtcit.ui.viewmodels.StepData


/**
 * Marine Registration Screen
 *
 * Handles Marine Unit Registration Category (التسجيل):
 * - Temporary Registration Certificate
 * - Permanent Registration Certificate
 * - Suspend Permanent Registration
 * - Cancel Permanent Registration
 * - Mortgage Certificate
 * - Release Mortgage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarineRegistrationScreen(
    navController: NavController,
    transactionType: TransactionType
) {
    val viewModel: MarineRegistrationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()
    val fileNavigationEvent by viewModel.fileNavigationEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current


    // Initialize transaction type on first composition
    LaunchedEffect(transactionType) {
        viewModel.initializeTransaction(transactionType)
    }

    // State for file operations
    var currentFilePickerField by remember { mutableStateOf("") }
    var currentFilePickerTypes by remember { mutableStateOf(listOf<String>()) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.onFieldValueChange(currentFilePickerField, it.toString())
            } catch (e: Exception) {
                Toast.makeText(context, "Error selecting file: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Handle file navigation events
    LaunchedEffect(fileNavigationEvent) {
        fileNavigationEvent?.let { event ->
            when (event) {
                is FileNavigationEvent.OpenFilePicker -> {
                    currentFilePickerField = event.fieldId
                    currentFilePickerTypes = event.allowedTypes

                    val mimeTypes = event.allowedTypes.map { type ->
                        when (type.lowercase()) {
                            "pdf" -> "application/pdf"
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            "doc" -> "application/msword"
                            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            "xls" -> "application/vnd.ms-excel"
                            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            else -> "application/*"
                        }
                    }.toTypedArray()

                    filePickerLauncher.launch(mimeTypes)
                }

                is FileNavigationEvent.ViewFile -> {
                    val uri = event.fileUri.toUri()
                    viewModel.openFileOutsideApp(context, uri, event.fileType)
                }

                is FileNavigationEvent.RemoveFile -> {
                    viewModel.onFieldValueChange(event.fieldId, "")
                }
            }
        }
    }

    // Handle submission result
    LaunchedEffect(submissionState) {
        when (submissionState) {
            is UIState.Success -> {
                navController.navigateUp()
                viewModel.resetSubmissionState()
            }

            is UIState.Failure -> {
                viewModel.resetSubmissionState()
            }

            else -> { /* No action needed */
            }
        }
    }

    // Show loading during ViewModel initialization
    if (uiState.isLoading || !uiState.isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Main UI
    // Main UI
    if (uiState.currentStep == uiState.steps.size - 1) {
        // 🧩 Review Step UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.DirectionsBoat,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "مراجعة طلبك",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "يرجى مراجعة بياناتك والتأكد منها قبل الإرسال، حيث سيتم إعتمادها وإرسالها إلى الجهة المختصة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                val allData = uiState.formData
                val allSteps = uiState.steps

                if (allData.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            text = "لا توجد بيانات متاحة للمراجعة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF856404),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    // Each Step as Expandable Card
                    allSteps.forEachIndexed { index, step ->
                        var isExpanded by remember { mutableStateOf(index == 0) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                // Header (Clickable)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isExpanded = !isExpanded }
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(id = step.titleRes),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF1A1A1A)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "يرجى توفير معلومات كاملة عن ${stringResource(id = step.titleRes)} لتسهيل دراسة الطلب واتخاذ الإجراءات اللازمة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF999999)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Expand/Collapse Icon
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF0F0F0),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = Color(0xFF666666),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
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
                                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                                    ) {
                                        Divider(
                                            modifier = Modifier.padding(bottom = 16.dp),
                                            color = Color(0xFFE0E0E0),
                                            thickness = 1.dp
                                        )

                                        // Fields
                                        step.fields.forEach { field ->
                                            val value = allData[field.id]
                                            if (!value.isNullOrBlank()) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 16.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(id = field.labelRes),
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = Color(0xFF666666)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFF8F9FA),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = value,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = Color(0xFF1A1A1A),
                                                            modifier = Modifier.padding(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = viewModel::previousStep,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF2196F3)),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "السابق",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2196F3)
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = viewModel::submitForm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "إرسال",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    } else {
        // باقي الخطوات العادية
        TransactionFormContent(
            navController = navController,
            uiState = uiState,
            submissionState = submissionState,
            transactionTitle = getMarineRegistrationTitle(transactionType),
            onFieldValueChange = viewModel::onFieldValueChange,
            onFieldFocusLost = viewModel::onFieldFocusLost,
            isFieldLoading = viewModel::isFieldLoading,
            onOpenFilePicker = viewModel::openFilePicker,
            onViewFile = viewModel::viewFile,
            onRemoveFile = viewModel::removeFile,
            goToStep = viewModel::goToStep,
            previousStep = viewModel::previousStep,
            nextStep = viewModel::nextStep,
            submitForm = viewModel::submitForm
        )
    }
}


    @Composable
private fun getMarineRegistrationTitle(transactionType: TransactionType): String {
    return when (transactionType) {
        TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> localizedApp(R.string.transaction_temporary_registration_certificate)
        TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> localizedApp(R.string.transaction_permanent_registration_certificate)
        TransactionType.SUSPEND_PERMANENT_REGISTRATION -> localizedApp(R.string.transaction_suspend_permanent_registration)
        TransactionType.CANCEL_PERMANENT_REGISTRATION -> localizedApp(R.string.transaction_cancel_permanent_registration)
        TransactionType.MORTGAGE_CERTIFICATE -> localizedApp(R.string.transaction_mortgage_certificate)
        TransactionType.RELEASE_MORTGAGE -> localizedApp(R.string.transaction_release_mortgage)
        else -> "Unknown Transaction"
    }
}
//
//// Main UI
//if (uiState.currentStep == uiState.steps.size - 1) {
//    // 🧩 Review Step UI
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//            .verticalScroll(rememberScrollState())
//    ) {
//        // Header
//        Text(
//            text = "مراجعة طلبك",
//            style = MaterialTheme.typography.headlineMedium,
//            modifier = Modifier
//                .padding(bottom = 8.dp)
//                .align(Alignment.CenterHorizontally),
//            textAlign = TextAlign.Center
//        )
//
//        Text(
//            text = "يرجي مراجعة بياناتك والتأكد منها قبل الأرسال، حيث سيتم اعتمادها وإرسالها إلى الجهة المختصة.",
//            style = MaterialTheme.typography.bodySmall,
//            modifier = Modifier.padding(bottom = 24.dp),
//            textAlign = TextAlign.Center,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        val allData = viewModel.uiState.collectAsState().value.formData
//
//        if (allData.isEmpty()) {
//            Text(
//                text = "لا توجد بيانات لمراجعتها",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.error
//            )
//        } else {
//            // بيانات الوحدة
//            CollapsibleSection(
//                title = "بيانات الوحدة",
//                subtitle = "معلومات كاملة عن السفينة أو الوحدة البحرية",
//                items = allData.filterKeys {
//                    it in listOf("shipName", "shipType", "registrationNumber", "nationality")
//                }
//            )
//
//            // الأبعاد الخاصة بالوحدة البحرية
//            CollapsibleSection(
//                title = "الأبعاد الخاصة بالوحدة البحرية",
//                subtitle = "معلومات كاملة عن السفينة أو الوحدة البحرية",
//                items = allData.filterKeys {
//                    it in listOf("length", "width", "height", "tonnage")
//                }
//            )
//
//            // الأوزان والحمولات الخاصة بالوحدة البحرية
//            CollapsibleSection(
//                title = "الأوزان والحمولات الخاصة بالوحدة البحرية",
//                subtitle = "معلومات كاملة عن السفينة أو الوحدة البحرية",
//                items = allData.filterKeys {
//                    it in listOf("grossWeight", "netWeight", "cargoCapacity", "passengerCapacity")
//                }
//            )
//
//            // الحمولة الكلية
//            if (allData.containsKey("totalLoad")) {
//                SimpleField(
//                    label = "الحمولة الكلية",
//                    value = allData["totalLoad"].toString()
//                )
//            }
//
//            // الحمولة الصافية
//            if (allData.containsKey("netLoad")) {
//                SimpleField(
//                    label = "الحمولة الصافية",
//                    value = allData["netLoad"].toString()
//                )
//            }
//
//            // الحمولة الوزنية (اختياري)
//            if (allData.containsKey("weightLoad")) {
//                SimpleField(
//                    label = "الحمولة الوزنية (اختياري)",
//                    value = allData["weightLoad"].toString()
//                )
//            }
//
//            // الوزن الخفيف (اختياري)
//            if (allData.containsKey("lightWeight")) {
//                SimpleField(
//                    label = "الوزن الخفيف (اختياري)",
//                    value = allData["lightWeight"].toString()
//                )
//            }
//
//            // أقص حمولة مسموح بها (اختياري)
//            if (allData.containsKey("maxLoad")) {
//                SimpleField(
//                    label = "أقص حمولة مسموح بها (اختياري)",
//                    value = allData["maxLoad"].toString()
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // أزرار التنقل
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 8.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            Button(
//                onClick = viewModel::previousStep,
//                modifier = Modifier
//                    .weight(1f)
//                    .height(48.dp),
//                colors = ButtonDefaults.outlinedButtonColors()
//            ) {
//                Text("السابق")
//            }
//            Button(
//                onClick = viewModel::submitForm,
//                modifier = Modifier
//                    .weight(1f)
//                    .height(48.dp)
//            ) {
//                Text("إرسال")
//            }
//        }
//    }
//} else {
//    // باقي الخطوات العادية
//    TransactionFormContent(
//        navController = navController,
//        uiState = uiState,
//        submissionState = submissionState,
//        transactionTitle = getMarineRegistrationTitle(transactionType),
//        onFieldValueChange = viewModel::onFieldValueChange,
//        onFieldFocusLost = viewModel::onFieldFocusLost,
//        isFieldLoading = viewModel::isFieldLoading,
//        onOpenFilePicker = viewModel::openFilePicker,
//        onViewFile = viewModel::viewFile,
//        onRemoveFile = viewModel::removeFile,
//        goToStep = viewModel::goToStep,
//        previousStep = viewModel::previousStep,
//        nextStep = viewModel::nextStep,
//        submitForm = viewModel::submitForm
//    )
//}
//
//// Composable للأقسام القابلة للتوسع
//@Composable
//fun CollapsibleSection(
//    title: String,
//    subtitle: String,
//    items: Map<String, Any>
//) {
//    var expanded by remember { mutableStateOf(false) }
//
//    if (items.isNotEmpty()) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 12.dp),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.surface
//            ),
//            border = BorderStroke(
//                1.dp,
//                MaterialTheme.colorScheme.outlineVariant
//            )
//        ) {
//            Column {
//                // Header
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable { expanded = !expanded }
//                        .padding(12.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column(modifier = Modifier.weight(1f)) {
//                        Text(
//                            text = title,
//                            style = MaterialTheme.typography.bodyLarge,
//                            color = MaterialTheme.colorScheme.primary,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            text = subtitle,
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                    Icon(
//                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//
//                // Content
//                if (expanded) {
//                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
//                    Column(modifier = Modifier.padding(12.dp)) {
//                        items.forEach { (key, value) ->
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(bottom = 8.dp),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                Text(
//                                    text = formatFieldName(key),
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                                Text(
//                                    text = value.toString(),
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    color = MaterialTheme.colorScheme.onBackground,
//                                    fontWeight = FontWeight.SemiBold
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//// Composable لحقل بسيط
//@Composable
//fun SimpleField(
//    label: String,
//    value: String
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(bottom = 12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        ),
//        border = BorderStroke(
//            1.dp,
//            MaterialTheme.colorScheme.outlineVariant
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                text = value,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onBackground,
//                fontWeight = FontWeight.SemiBold
//            )
//        }
//    }
//}
//
//// دالة لتنسيق أسماء الحقول
//fun formatFieldName(fieldName: String): String {
//    return when (fieldName) {
//        "shipName" -> "اسم السفينة"
//        "shipType" -> "نوع السفينة"
//        "registrationNumber" -> "رقم التسجيل"
//        "nationality" -> "الجنسية"
//        "length" -> "الطول"
//        "width" -> "العرض"
//        "height" -> "الارتفاع"
//        "tonnage" -> "الحمولة"
//        "grossWeight" -> "الوزن الإجمالي"
//        "netWeight" -> "الوزن الصافي"
//        "cargoCapacity" -> "سعة الشحنة"
//        "passengerCapacity" -> "سعة الركاب"
//        else -> fieldName.replaceFirstChar { it.uppercase() }
//    }
//}
