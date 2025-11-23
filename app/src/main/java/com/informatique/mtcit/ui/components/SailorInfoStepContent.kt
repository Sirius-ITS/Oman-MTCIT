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
    val job: String = "",
    val fullName: String = "",
    val identityNumber: String = "",
    val seamanPassportNumber: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SailorFormBottomSheet(
    sailor: SailorData? = null,
    jobs: List<String>,
    onDismiss: () -> Unit,
    onSave: (SailorData) -> Unit
) {
    val extraColors = LocalExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var job by remember { mutableStateOf(sailor?.job ?: "") }
    var name by remember { mutableStateOf(sailor?.fullName ?: "") }
    var identityNumber by remember { mutableStateOf(sailor?.identityNumber ?: "") }
    var seamanPassportNumber by remember { mutableStateOf(sailor?.seamanPassportNumber ?: "") }

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
                options = jobs,
                selectedOption = job,
                onOptionSelected = { job = it },
                mandatory = true,
                placeholder = job.ifEmpty { localizedApp(R.string.sailor_job_title) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            CustomTextField(
                value = name,
                onValueChange = { name = it },
                label = localizedApp(R.string.sailor_name_title),
                mandatory = true,
                placeholder = localizedApp(R.string.sailor_name_title),
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                            job = job,
                            fullName = name,
                            identityNumber = identityNumber,
                            seamanPassportNumber = seamanPassportNumber
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
