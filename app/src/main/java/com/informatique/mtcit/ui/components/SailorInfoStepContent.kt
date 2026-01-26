package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SailorData(
    val id: String = UUID.randomUUID().toString(),
    val apiId: Long? = null,  // ✅ Real crew ID from API (null for new sailors)
    val job: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val identityNumber: String = "",
    val seamanPassportNumber: String = "",
    val nationality: String = "",
    // Keep for backward compatibility
    val fullName: String = nameEn
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SailorFormBottomSheet(
    sailor: SailorData? = null,
    jobs: List<String>,
    nationalities: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (SailorData) -> Unit
) {
    val extraColors = LocalExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var job by remember { mutableStateOf(sailor?.job ?: "") }
    var nameAr by remember { mutableStateOf(sailor?.nameAr ?: "") }
    var nameEn by remember { mutableStateOf(sailor?.nameEn ?: "") }
    var identityNumber by remember { mutableStateOf(sailor?.identityNumber ?: "") }
    var seamanPassportNumber by remember { mutableStateOf(sailor?.seamanPassportNumber ?: "") }
    var nationality by remember { mutableStateOf(sailor?.nationality ?: "") }

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
                text = if (sailor != null) localizedApp(R.string.sailor_edit_title)
                else localizedApp(R.string.sailor_add_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.padding(bottom = 16.dp)
            )


            // Jobs
            CustomDropdown(
                label = localizedApp(R.string.sailor_job_title),
                options = jobs.map { it.split("|").lastOrNull() ?: it },
                selectedOption = job.split("|").lastOrNull() ?: job,
                onOptionSelected = { selectedLabel ->
                    // Find the full "ID|Label" string from the selected label
                    job = jobs.find { it.endsWith("|$selectedLabel") } ?: selectedLabel
                },
                mandatory = true,
                placeholder = job.ifEmpty { localizedApp(R.string.sailor_job_title) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name in Arabic
            CustomTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = localizedApp(R.string.name_ar_label),
                mandatory = true,
                placeholder = localizedApp(R.string.name_ar_label),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name in English
            CustomTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = localizedApp(R.string.name_en_label),
                mandatory = true,
                placeholder = localizedApp(R.string.name_en_label),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Nationality
            if (nationalities.isNotEmpty()) {
                CustomDropdown(
                    label = localizedApp(R.string.nationality_label),
                    options = nationalities.map { it.split("|").lastOrNull() ?: it },
                    selectedOption = nationality.split("|").lastOrNull() ?: nationality,
                    onOptionSelected = { selectedLabel ->
                        // Find the full "ID|Label" string from the selected label
                        nationality = nationalities.find { it.endsWith("|$selectedLabel") } ?: selectedLabel
                    },
                    mandatory = true,
                    placeholder = localizedApp(R.string.nationality_label)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Identity number
            CustomTextField(
                value = identityNumber,
                onValueChange = { identityNumber = it },
                label = localizedApp(R.string.sailor_identity_number_title),
                mandatory = true,
                placeholder = localizedApp(R.string.sailor_identity_number_title),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Seaman's passport number
            CustomTextField(
                value = seamanPassportNumber,
                onValueChange = { seamanPassportNumber = it },
                label = localizedApp(R.string.sailor_seaman_passport_number_title),
                isNumeric = true,
                mandatory = true,
                placeholder =localizedApp(R.string.sailor_seaman_passport_number_title)
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
                    Text(localizedApp(R.string.cancel_button) , color = extraColors.whiteInDarkMode)
                }

                Button(
                    onClick = {
                        val sailorData = SailorData(
                            id = sailor?.id ?: UUID.randomUUID().toString(),
                            apiId = sailor?.apiId,  // ✅ Preserve API ID for existing sailors
                            job = job,
                            nameAr = nameAr,
                            nameEn = nameEn,
                            identityNumber = identityNumber,
                            seamanPassportNumber = seamanPassportNumber,
                            nationality = nationality,
                            fullName = nameEn // Backward compatibility
                        )
                        onSave(sailorData)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.startServiceButton
                    )
                ) {
                    Text(localizedApp(R.string.save_button) , color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
