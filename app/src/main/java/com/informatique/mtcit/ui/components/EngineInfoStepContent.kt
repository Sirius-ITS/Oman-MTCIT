package com.informatique.mtcit.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.util.UriCache
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EngineData(
    val id: String = UUID.randomUUID().toString(), // Local UUID for UI tracking
    val dbId: Int? = null, // Database ID (null for newly added engines)
    val number: String = "",
    val type: String = "",
    val power: String = "",
    val cylinder: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val manufactureYear: String = "",
    val productionCountry: String = "",
    val fuelType: String = "",
    val condition: String = "",
    val documentUri: String = "", // مسار المستند (can be local URI or "draft:refNum")
    val documentName: String = "", // اسم الملف
    val documentRefNum: String? = null, // For draft documents - used in PUT requests
    val documentFileName: String? = null // Display name for draft documents
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineFormBottomSheet(
    engine: EngineData? = null,
    manufacturers: List<String>,
    countries: List<String>,
    fuelTypes: List<String>,
    engineConditions: List<String>,
    engineTypes: List<String>, // Added engineTypes parameter
    onDismiss: () -> Unit,
    onSave: (EngineData) -> Unit,
    onViewFile: ((String, String) -> Unit)? = null // ✅ For viewing draft documents
) {
    val extraColors = LocalExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var number by remember { mutableStateOf(engine?.number ?: "") }
    var type by remember { mutableStateOf(engine?.type ?: "") }
    var power by remember { mutableStateOf(engine?.power ?: "") }
    var cylinder by remember { mutableStateOf(engine?.cylinder ?: "") }
    var manufacturer by remember { mutableStateOf(engine?.manufacturer ?: "") }
    var model by remember { mutableStateOf(engine?.model ?: "") }
    var manufactureYear by remember { mutableStateOf(engine?.manufactureYear ?: "") }
    var productionCountry by remember { mutableStateOf(engine?.productionCountry ?: "") }
    var fuelType by remember { mutableStateOf(engine?.fuelType ?: "") }
    var condition by remember { mutableStateOf(engine?.condition ?: "") }

    // ✅ Initialize documentUri properly for draft documents
    var documentUri by remember {
        mutableStateOf(
            when {
                // If it's a draft document, use "draft:" prefix
                engine?.documentRefNum != null -> "draft:${engine.documentRefNum}"
                // Otherwise use the normal URI
                else -> engine?.documentUri ?: ""
            }
        )
    }

    // ✅ Track if this is a draft document (from API) or new upload
    val isDraftDocument = remember(engine) {
        engine?.documentRefNum != null
    }

    // ✅ Extract draft document info from engine data
    val draftDocumentRefNum = remember(engine) {
        engine?.documentRefNum
    }
    val draftDocumentFileName = remember(engine) {
        engine?.documentFileName ?: "Document"
    }

    // Track if user has uploaded a new document (replaces draft)
    var hasNewDocument by remember { mutableStateOf(false) }

    println("🔧 EngineFormBottomSheet: isDraft=$isDraftDocument, refNum=$draftDocumentRefNum, fileName=$draftDocumentFileName, uri=$documentUri")

    // File upload callbacks
    var filePickerFieldId by remember { mutableStateOf<String?>(null) }
    var filePickerAllowedTypes by remember { mutableStateOf<List<String>>(emptyList()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // ✅ CRITICAL: Cache the URI with its permissions before storing as string
            UriCache.cacheUri(context, it)
            documentUri = it.toString()
            hasNewDocument = true // Mark that user uploaded new document
            println("🔧 New document uploaded: $documentUri")
        }
    }

    LaunchedEffect(filePickerFieldId) {
        filePickerFieldId?.let {
            println("🔧 Opening file picker for field: $it")
            filePickerLauncher.launch("*/*")
            filePickerFieldId = null
        }
    }

    // ✅ Debug log when draft document changes
    LaunchedEffect(isDraftDocument, draftDocumentRefNum, draftDocumentFileName) {
        println("🔧 Draft document state: isDraft=$isDraftDocument, refNum=$draftDocumentRefNum, fileName=$draftDocumentFileName, documentUri=$documentUri")
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = extraColors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (engine != null) localizedApp(R.string.engine_edit_title)
                else localizedApp(R.string.engine_add_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Number
            CustomTextField(
                value = number,
                onValueChange = { number = it },
                label = localizedApp(R.string.engine_no_title),
                mandatory = true,
                placeholder = localizedApp(R.string.engine_no_title),
                enabled = true,
                maxLength = 10
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type - Changed to Dropdown
            CustomDropdown(
                label = localizedApp(R.string.engine_type_title),
                options = engineTypes,
                selectedOption = type,
                onOptionSelected = { type = it },
                mandatory = true,
                placeholder = type.ifEmpty { localizedApp(R.string.engine_type_title) },
                maxLength = 50
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Power
            CustomTextField(
                value = power,
                onValueChange = { power = it },
                label = localizedApp(R.string.engine_power_title),
                isNumeric = true,
                mandatory = true,
                placeholder = localizedApp(R.string.engine_power_title),
                enabled = true,
                maxLength = 6,
                minLength = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cylinder
            CustomTextField(
                value = cylinder,
                onValueChange = { cylinder = it },
                label = localizedApp(R.string.engine_cylinders_title),
                isNumeric = true,
                mandatory = true,
                placeholder = localizedApp(R.string.engine_cylinders_title),
                enabled = true,
                maxLength = 3,
                minLength = 0
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Manufacturer
            CustomTextField(
                value = manufacturer,
                onValueChange = { manufacturer = it },
                label = localizedApp(R.string.engine_manufacturer_title),
                placeholder = localizedApp(R.string.engine_manufacturer_title),
                mandatory = true,
                maxLength = 50
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Model
            CustomTextField(
                value = model,
                onValueChange = { model = it },
                label = localizedApp(R.string.engine_model_title),
                mandatory = true,
                placeholder = localizedApp(R.string.engine_model_title),
                enabled = true,
                maxLength = 10
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Manufacture Year
            CustomTextField(
                value = manufactureYear,
                onValueChange = { manufactureYear = it },
                label = localizedApp(R.string.engine_manufacture_year_title),
                isNumeric = true,
                mandatory = true,
                placeholder = localizedApp(R.string.engine_manufacture_year_title),
                enabled = true,
                maxLength = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Production Country
            CustomDropdown(
                label = localizedApp(R.string.engine_producing_country_title),
                options = countries,
                selectedOption = productionCountry,
                onOptionSelected = { productionCountry = it },
                mandatory = true,
                placeholder = productionCountry.ifEmpty { localizedApp(R.string.engine_producing_country_title) },
                maxLength = 50
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fuel Type
            CustomDropdown(
                label = localizedApp(R.string.engine_fuel_type_title),
                options = fuelTypes,
                selectedOption = fuelType,
                onOptionSelected = { fuelType = it },
                mandatory = true,
                placeholder = fuelType.ifEmpty { localizedApp(R.string.engine_fuel_type_title) },
                maxLength = 50
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Engine Condition
            CustomDropdown(
                label = localizedApp(R.string.engine_condition_title),
                options = engineConditions,
                selectedOption = condition,
                onOptionSelected = { condition = it },
                mandatory = true,
                placeholder = condition.ifEmpty { localizedApp(R.string.engine_condition_title) },
                maxLength = 50
            )

            Spacer(modifier = Modifier.height(24.dp))

            // File Upload Section using CustomFileUpload
            CustomFileUpload(
                value = documentUri,
                onValueChange = {
                    println("🔧 EngineForm: documentUri changed to: $it")
                    documentUri = it
                },
                label = localizedApp(R.string.attaching_the_engine_documents),
                mandatory = true,
                fieldId = "engineDocument",
                onOpenFilePicker = { fieldId, allowedTypes ->
                    println("🔧 EngineForm: Opening file picker")
                    filePickerFieldId = fieldId
                    filePickerAllowedTypes = allowedTypes
                },
                onRemoveFile = {
                    println("🔧 EngineForm: Removing file")
                    documentUri = ""
                    hasNewDocument = false
                },
                onViewFile = onViewFile?.let { viewFileCallback ->
                    { uri, mimeType ->
                        println("🔧 EngineForm: onViewFile called - uri=$uri, mimeType=$mimeType")
                        println("🔧 EngineForm: isDraftDocument=$isDraftDocument, draftDocumentRefNum=$draftDocumentRefNum")
                        // ✅ Handle draft documents - strip the "draft:" prefix
                        if (uri.startsWith("draft:")) {
                            val refNum = uri.removePrefix("draft:")
                            println("🔧 EngineForm: Viewing draft document with refNum=$refNum")
                            viewFileCallback(refNum, mimeType)
                        } else {
                            println("🔧 EngineForm: Viewing normal document with uri=$uri")
                            viewFileCallback(uri, mimeType)
                        }
                    }
                },
                draftDocumentRefNum = draftDocumentRefNum,
                draftDocumentFileName = draftDocumentFileName
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(localizedApp(R.string.cancel_button), color = extraColors.whiteInDarkMode)
                }

                Button(
                    onClick = {
                        println("🔧 EngineForm: Save button clicked")
                        println("   - number: $number")
                        println("   - type: $type")
                        println("   - condition: $condition")
                        println("   - documentUri: $documentUri")
                        println("   - isDraftDocument: $isDraftDocument")
                        println("   - hasNewDocument: $hasNewDocument")
                        println("   - draftDocumentRefNum: $draftDocumentRefNum")

                        val engineData = EngineData(
                            id = engine?.id ?: UUID.randomUUID().toString(),
                            dbId = engine?.dbId, // Preserve database ID for existing engines
                            number = number,
                            type = type,
                            power = power,
                            cylinder = cylinder,
                            manufacturer = manufacturer,
                            model = model,
                            manufactureYear = manufactureYear,
                            productionCountry = productionCountry,
                            fuelType = fuelType,
                            condition = condition,
                            documentUri = documentUri,
                            documentName = "",
                            // ✅ IMPORTANT: Always preserve documentRefNum if it exists
                            // - If hasNewDocument=true: API will see new file + old refNum → REPLACE old doc with new file
                            // - If hasNewDocument=false: API will see old refNum + no new file → KEEP existing doc
                            documentRefNum = draftDocumentRefNum,
                            documentFileName = draftDocumentFileName
                        )

                        println("🔧 EngineForm: Created engine data: $engineData")
                        println("🔧 EngineForm: Calling onSave...")
                        onSave(engineData)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.startServiceButton
                    ),
                    enabled = documentUri.isNotEmpty() // Enable when document exists (draft or new)
                ) {
                    Text(localizedApp(R.string.save_button), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
