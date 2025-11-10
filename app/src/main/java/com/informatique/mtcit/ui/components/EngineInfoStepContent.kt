package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R
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
    val condition: String = ""
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
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type
            CustomTextField(
                value = type,
                onValueChange = { type = it },
                label = stringResource(R.string.engine_type_title),
                mandatory = true,
                placeholder = stringResource(R.string.engine_type_title),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Power
            CustomTextField(
                value = power,
                onValueChange = { power = it },
                label = stringResource(R.string.engine_power_title),
                isNumeric = true,
                mandatory = true,
                placeholder =stringResource(R.string.engine_power_title)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cylinder
            CustomTextField(
                value = cylinder,
                onValueChange = { cylinder = it },
                label = stringResource(R.string.engine_cylinders_title),
                isNumeric = true,
                mandatory = true,
                placeholder = stringResource(R.string.engine_cylinders_title)
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
                            condition = condition
                        )
                        onSave(engineData)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.startServiceButton
                    )
                ) {
                    Text(stringResource(R.string.save_button) , color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
