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
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EngineData(
    val id: String = UUID.randomUUID().toString(),
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
    val documentUri: String = "", // مسار المستند
    val documentName: String = "" // اسم الملف
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineFormBottomSheet(
    engine: EngineData? = null,
    manufacturers: List<String>,
    countries: List<String>,
    fuelTypes: List<String>,
    engineConditions: List<String>,
    onDismiss: () -> Unit,
    onSave: (EngineData) -> Unit
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
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var documentName by remember { mutableStateOf(engine?.documentName ?: "") }


    // File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            documentUri = it
            // استخراج اسم الملف من Uri
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        documentName = c.getString(nameIndex)
                    }
                }
            }
        }
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
                text = if (engine != null) stringResource(R.string.engine_edit_title)
                else stringResource(R.string.engine_add_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Number
            CustomTextField(
                value = number,
                onValueChange = { number = it },
                label = stringResource(R.string.engine_no_title),
                mandatory = true,
                placeholder = stringResource(R.string.engine_no_title),
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type
            CustomTextField(
                value = type,
                onValueChange = { type = it },
                label = stringResource(R.string.engine_type_title),
                mandatory = true,
                placeholder = stringResource(R.string.engine_type_title),
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Power
            CustomTextField(
                value = power,
                onValueChange = { power = it },
                label = stringResource(R.string.engine_power_title),
                isNumeric = true,
                mandatory = true,
                placeholder =stringResource(R.string.engine_power_title),
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cylinder
            CustomTextField(
                value = cylinder,
                onValueChange = { cylinder = it },
                label = stringResource(R.string.engine_cylinders_title),
                isNumeric = true,
                mandatory = true,
                placeholder = stringResource(R.string.engine_cylinders_title),
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Manufacturer
            CustomDropdown(
                label = stringResource(R.string.engine_manufacturer_title),
                options = manufacturers,
                selectedOption = manufacturer,
                onOptionSelected = { manufacturer = it },
                mandatory = true,
                placeholder = manufacturer.ifEmpty { stringResource(R.string.engine_manufacturer_title) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Model
            CustomTextField(
                value = model,
                onValueChange = { model = it },
                label = stringResource(R.string.engine_model_title),
                mandatory = true,
                placeholder =stringResource(R.string.engine_model_title),
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Manufacture Year
            CustomTextField(
                value = manufactureYear,
                onValueChange = { manufactureYear = it },
                label = stringResource(R.string.engine_manufacture_year_title),
                isNumeric = true,
                mandatory = true,
                placeholder =stringResource(R.string.engine_manufacture_year_title),
                enabled = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Production Country
            CustomDropdown(
                label = stringResource(R.string.engine_producing_country_title),
                options = countries,
                selectedOption = productionCountry,
                onOptionSelected = { productionCountry = it },
                mandatory = true,
                placeholder = productionCountry.ifEmpty { stringResource(R.string.engine_producing_country_title) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fuel Type
            CustomDropdown(
                label = stringResource(R.string.engine_fuel_type_title),
                options = fuelTypes,
                selectedOption = fuelType,
                onOptionSelected = { fuelType = it },
                mandatory = true,
                placeholder = fuelType.ifEmpty { stringResource(R.string.engine_fuel_type_title) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Engine Condition
            CustomDropdown(
                label = stringResource(R.string.engine_condition_title),
                options = engineConditions,
                selectedOption = condition,
                onOptionSelected = { condition = it },
                mandatory = true,
                placeholder = condition.ifEmpty { stringResource(R.string.engine_condition_title) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // File Upload Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.attaching_the_engine_documents),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.whiteInDarkMode
                    )
                    Text(
                        text = " *",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filePickerLauncher.launch("*/*") }
                        .border(
                            width = 1.dp,
                            color = if (documentUri != null) extraColors.startServiceButton
                            else extraColors.whiteInDarkMode.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = extraColors.background
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (documentUri != null) Icons.Default.CheckCircle
                                else Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = if (documentUri != null) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = if (documentName.isNotEmpty()) documentName
                                else stringResource(R.string.attaching_the_engine_documents),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (documentUri != null) extraColors.whiteInDarkMode
                                else extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "اختر ملف",
                            tint = extraColors.startServiceButton,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (documentUri != null) {
                    Text(
                        text = "تم اختيار الملف بنجاح ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.startServiceButton,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

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
                    Text(stringResource(R.string.cancel_button) , color = extraColors.whiteInDarkMode)
                }

                Button(
                    onClick = {
                        val engineData = EngineData(
                            id = engine?.id ?: UUID.randomUUID().toString(),
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
                            documentUri = documentUri?.toString() ?: "",
                            documentName = documentName

                        )
                        onSave(engineData)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.startServiceButton
                    ),
                    enabled = documentUri != null // تفعيل الزر فقط لما يتم اختيار ملف
                ) {
                    Text(stringResource(R.string.save_button) , color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
