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
import com.informatique.mtcit.common.util.LocalAppLocale
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
    /** When true (Change Captain edit mode): only nameAr/nameEn are editable */
    editNameOnly: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (SailorData) -> Unit
) {
    val extraColors = LocalExtraColors.current
    val isAr = LocalAppLocale.current.language == "ar"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var job by remember { mutableStateOf(sailor?.job ?: "") }
    var nameAr by remember { mutableStateOf(sailor?.nameAr ?: "") }
    var nameEn by remember { mutableStateOf(sailor?.nameEn ?: "") }
    var identityNumber by remember { mutableStateOf(sailor?.identityNumber ?: "") }
    var seamanPassportNumber by remember { mutableStateOf(sailor?.seamanPassportNumber ?: "") }
    var nationality by remember { mutableStateOf(sailor?.nationality ?: "") }

    // ── Inline format-validation errors ────────────────────────────
    var nameArError by remember { mutableStateOf<String?>(null) }
    var nameEnError by remember { mutableStateOf<String?>(null) }
    var identityNumberError by remember { mutableStateOf<String?>(null) }
    var seamanPassportError by remember { mutableStateOf<String?>(null) }

    fun validateNameAr(v: String) {
        nameArError = if (v.isNotBlank() && !v.matches(Regex("^[\u0600-\u06FF\\s]+$")))
            if (isAr) "يجب أن يحتوي على أحرف عربية فقط" else "Must contain Arabic letters only"
        else null
    }
    fun validateNameEn(v: String) {
        nameEnError = if (v.isNotBlank() && !v.matches(Regex("^[a-zA-Z0-9\\s]+$")))
            if (isAr) "يجب أن يحتوي على أحرف إنجليزية وأرقام فقط" else "Must contain English letters and digits only"
        else null
    }
    fun validateIdentityNumber(v: String) {
        identityNumberError = if (v.isNotBlank() && !v.matches(Regex("^\\d+$")))
            if (isAr) "يجب أن يحتوي على أرقام فقط" else "Must contain digits only"
        else null
    }
    fun validateSeamanPassport(v: String) {
        seamanPassportError = if (v.isNotBlank() && !v.matches(Regex("^\\d+$")))
            if (isAr) "يجب أن يحتوي على أرقام فقط" else "Must contain digits only"
        else null
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
                    if (!editNameOnly) {
                        job = jobs.find { it.endsWith("|$selectedLabel") } ?: selectedLabel
                    }
                },
                mandatory = true,
                placeholder = job.ifEmpty { localizedApp(R.string.sailor_job_title) },
                enabled = !editNameOnly
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name in Arabic
            CustomTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                onFocusLost = { validateNameAr(it) },
                label = localizedApp(R.string.name_ar_label),
                mandatory = true,
                placeholder = localizedApp(R.string.name_ar_label),
                error = nameArError
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name in English
            CustomTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                onFocusLost = { validateNameEn(it) },
                label = localizedApp(R.string.name_en_label),
                mandatory = true,
                placeholder = localizedApp(R.string.name_en_label),
                error = nameEnError
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Nationality
            if (nationalities.isNotEmpty()) {
                CustomDropdown(
                    label = localizedApp(R.string.nationality_label),
                    options = nationalities.map { it.split("|").lastOrNull() ?: it },
                    selectedOption = nationality.split("|").lastOrNull() ?: nationality,
                    onOptionSelected = { selectedLabel ->
                        if (!editNameOnly) {
                            nationality = nationalities.find { it.endsWith("|$selectedLabel") } ?: selectedLabel
                        }
                    },
                    mandatory = true,
                    placeholder = localizedApp(R.string.nationality_label),
                    enabled = !editNameOnly
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Identity number
            CustomTextField(
                value = identityNumber,
                onValueChange = { if (!editNameOnly) identityNumber = it },
                onFocusLost = { validateIdentityNumber(it) },
                label = localizedApp(R.string.sailor_identity_number_title),
                mandatory = true,
                placeholder = localizedApp(R.string.sailor_identity_number_title),
                enabled = !editNameOnly,
                error = identityNumberError
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Seaman's passport number
            CustomTextField(
                value = seamanPassportNumber,
                onValueChange = { if (!editNameOnly) seamanPassportNumber = it },
                onFocusLost = { validateSeamanPassport(it) },
                label = localizedApp(R.string.sailor_seaman_passport_number_title),
                isNumeric = true,
                mandatory = true,
                placeholder = localizedApp(R.string.sailor_seaman_passport_number_title),
                enabled = !editNameOnly,
                error = seamanPassportError
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
                        // Re-validate all fields on save
                        validateNameAr(nameAr)
                        validateNameEn(nameEn)
                        validateIdentityNumber(identityNumber)
                        validateSeamanPassport(seamanPassportNumber)

                        val hasErrors = nameArError != null || nameEnError != null ||
                                identityNumberError != null || seamanPassportError != null
                        if (hasErrors) return@Button

                        val sailorData = SailorData(
                            id = sailor?.id ?: UUID.randomUUID().toString(),
                            apiId = sailor?.apiId,
                            job = job,
                            nameAr = nameAr,
                            nameEn = nameEn,
                            identityNumber = identityNumber,
                            seamanPassportNumber = seamanPassportNumber,
                            nationality = nationality,
                            fullName = nameEn
                        )
                        onSave(sailorData)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.startServiceButton
                    )
                ) {
                    Text(localizedApp(R.string.save_button), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
