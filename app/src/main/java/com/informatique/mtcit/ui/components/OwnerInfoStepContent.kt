package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable

@Serializable
data class OwnerData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fullName: String = "",
    val nationality: String = "",
    val idNumber: String = "",
    val passportNumber: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    val city: String = "",
    val country: String = "",
    val postalCode: String = "",
    val isCompany: Boolean = false,
    val companyRegistrationNumber: String = "",
    val companyName: String = "",
    val companyType: String = ""
)

/**
 * Reusable Owner Form Bottom Sheet
 * Can be called from any component that needs to add/edit owner information
 *
 * Usage:
 * ```
 * var showSheet by remember { mutableStateOf(false) }
 * if (showSheet) {
 *     OwnerFormBottomSheet(
 *         owner = existingOwner,
 *         nationalities = listOf(...),
 *         countries = listOf(...),
 *         onDismiss = { showSheet = false },
 *         onSave = { ownerData -> /* handle save */ }
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerFormBottomSheet(
    owner: OwnerData? = null,
    nationalities: List<String>,
    countries: List<String>,
    includeCompanyFields: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (OwnerData) -> Unit
) {
    val extraColors = LocalExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fullName by remember { mutableStateOf(owner?.fullName ?: "") }
    var nationality by remember { mutableStateOf(owner?.nationality ?: "") }
    var idNumber by remember { mutableStateOf(owner?.idNumber ?: "") }
    var passportNumber by remember { mutableStateOf(owner?.passportNumber ?: "") }
    var email by remember { mutableStateOf(owner?.email ?: "") }
    var mobile by remember { mutableStateOf(owner?.mobile ?: "") }
    var address by remember { mutableStateOf(owner?.address ?: "") }
    var city by remember { mutableStateOf(owner?.city ?: "") }
    var country by remember { mutableStateOf(owner?.country ?: "") }
    var postalCode by remember { mutableStateOf(owner?.postalCode ?: "") }
    var isCompany by remember { mutableStateOf(owner?.isCompany ?: false) }
    var companyRegistrationNumber by remember { mutableStateOf(owner?.companyRegistrationNumber ?: "") }
    var companyName by remember { mutableStateOf(owner?.companyName ?: "") }
    var companyType by remember { mutableStateOf(owner?.companyType ?: "") }

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
                text = if (owner != null) localizedApp(R.string.edit_owner) else localizedApp(R.string.add_owner),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = extraColors.white,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Is Company Checkbox (only if company fields are enabled)
            if (includeCompanyFields) {
                CustomCheckBox(
                    checked = isCompany,
                    onCheckedChange = { isCompany = it },
                    label = localizedApp(R.string.is_company)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Full Name
            CustomTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = localizedApp(R.string.owner_full_name),
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Nationality
            CustomDropdown(
                label = localizedApp(R.string.owner_nationality),
                options = nationalities,
                selectedOption = nationality,
                onOptionSelected = { nationality = it },
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ID Number
            CustomTextField(
                value = idNumber,
                onValueChange = { idNumber = it },
                label = localizedApp(R.string.owner_id_number),
                isNumeric = true,
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Passport Number
            CustomTextField(
                value = passportNumber,
                onValueChange = { passportNumber = it },
                label = localizedApp(R.string.owner_passport_number),
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            CustomTextField(
                value = email,
                onValueChange = { email = it },
                label = localizedApp(R.string.email),
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mobile
            CustomTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = localizedApp(R.string.owner_mobile),
                isNumeric = true,
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Address
            CustomTextField(
                value = address,
                onValueChange = { address = it },
                label = localizedApp(R.string.owner_address),
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // City
            CustomTextField(
                value = city,
                onValueChange = { city = it },
                label = localizedApp(R.string.owner_city),
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Country
            CustomDropdown(
                label = localizedApp(R.string.country),
                options = countries,
                selectedOption = country,
                onOptionSelected = { country = it },
                mandatory = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Postal Code
            CustomTextField(
                value = postalCode,
                onValueChange = { postalCode = it },
                label = localizedApp(R.string.owner_postal_code),
                isNumeric = true,
                mandatory = false
            )

            // Company Fields (conditionally shown)
            if (includeCompanyFields && isCompany) {
                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = companyRegistrationNumber,
                    onValueChange = { companyRegistrationNumber = it },
                    label = localizedApp(R.string.company_registration_number),
                    isNumeric = true,
                    mandatory = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = localizedApp(R.string.company_name),
                    mandatory = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = companyType,
                    onValueChange = { companyType = it },
                    label = localizedApp(R.string.owner_type),
                    mandatory = false
                )
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
                    Text(localizedApp(R.string.cancel_button) , color = extraColors.white)
                }

                Button(
                    onClick = {
                        val ownerData = OwnerData(
                            id = owner?.id ?: java.util.UUID.randomUUID().toString(),
                            fullName = fullName,
                            nationality = nationality,
                            idNumber = idNumber,
                            passportNumber = passportNumber,
                            email = email,
                            mobile = mobile,
                            address = address,
                            city = city,
                            country = country,
                            postalCode = postalCode,
                            isCompany = isCompany,
                            companyRegistrationNumber = companyRegistrationNumber,
                            companyName = companyName,
                            companyType = companyType
                        )
                        onSave(ownerData)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.blue6
                    )
                ) {
                    Text(localizedApp(R.string.save_button) , color = extraColors.white)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
