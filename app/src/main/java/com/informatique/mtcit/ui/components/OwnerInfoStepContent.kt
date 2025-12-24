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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.util.UriCache
import kotlinx.serialization.Serializable

@Serializable
data class OwnerData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fullName: String = "", // Deprecated - keeping for backward compatibility
    val ownerName: String = "", // Arabic name (for individuals) or Arabic company name
    val ownerNameEn: String = "", // English name (for individuals) or English company name
    val nationality: String = "",
    val idNumber: String = "",
    val ownerShipPercentage: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    val city: String = "",
    val country: String = "",
    val postalCode: String = "",
    val isCompany: Boolean = false,
    val companyRegistrationNumber: String = "",
    val companyName: String = "",
    val companyType: String = "",
    val isRepresentative: Boolean = false, // New field for representative checkbox
    val ownershipProofDocument: String = "",
    val documentName: String = ""
)

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
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(owner?.fullName ?: "") }
    var ownerName by remember { mutableStateOf(owner?.ownerName ?: "") }
    var ownerNameEn by remember { mutableStateOf(owner?.ownerNameEn ?: "") }
    var nationality by remember { mutableStateOf(owner?.nationality ?: "") }
    var idNumber by remember { mutableStateOf(owner?.idNumber ?: "") }
    var ownerShipPercentage by remember { mutableStateOf(owner?.ownerShipPercentage ?: "") }
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
    var documentUri by remember { mutableStateOf(owner?.ownershipProofDocument ?: "") }
    var isRepresentative by remember { mutableStateOf(owner?.isRepresentative ?: false) }

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
        }
    }

    LaunchedEffect(filePickerFieldId) {
        filePickerFieldId?.let {
            filePickerLauncher.launch("*/*")
            filePickerFieldId = null
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
                text = if (owner != null) localizedApp(R.string.edit_owner) else localizedApp(R.string.add_owner),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Owner Type Selector (Company or Individual)
            if (includeCompanyFields) {
                Text(
                    text = localizedApp(R.string.owner_type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Individual Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isCompany = false }
                            .border(
                                width = 2.dp,
                                color = if (!isCompany) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isCompany)
                                extraColors.startServiceButton.copy(alpha = 0.1f)
                            else extraColors.background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "فرد",
                                tint = if (!isCompany) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = localizedApp(R.string.person),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (!isCompany) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isCompany) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode
                            )
                        }
                    }

                    // Company Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isCompany = true }
                            .border(
                                width = 2.dp,
                                color = if (isCompany) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompany)
                                extraColors.startServiceButton.copy(alpha = 0.1f)
                            else extraColors.background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "شركة",
                                tint = if (isCompany) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = localizedApp(R.string.company),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCompany) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCompany) extraColors.startServiceButton
                                else extraColors.whiteInDarkMode
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (includeCompanyFields && isCompany) {
                // Company Name in Arabic
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = localizedApp(R.string.company_name_arabic),
                    mandatory = true,
                    placeholder = localizedApp(R.string.company_name_arabic),
                    enabled = true,
                    maxLength = 100
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Company Name in English
                CustomTextField(
                    value = ownerNameEn,
                    onValueChange = { ownerNameEn = it },
                    label = localizedApp(R.string.company_name_english),
                    mandatory = true,
                    placeholder = localizedApp(R.string.company_name_english),
                    enabled = true,
                    maxLength = 100
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Commercial Registration Number
                CustomTextField(
                    value = companyRegistrationNumber,
                    onValueChange = { companyRegistrationNumber = it },
                    label = localizedApp(R.string.company_registration_number),
                    isNumeric = true,
                    mandatory = true,
                    placeholder = localizedApp(R.string.company_registration_number),
                    enabled = true,
                    maxLength = 20
                )
            } else {
                // Owner Name in Arabic
                CustomTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = localizedApp(R.string.owner_name_arabic),
                    mandatory = true,
                    placeholder = localizedApp(R.string.owner_name_arabic),
                    enabled = true,
                    maxLength = 100
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Owner Name in English
                CustomTextField(
                    value = ownerNameEn,
                    onValueChange = { ownerNameEn = it },
                    label = localizedApp(R.string.owner_name_english),
                    mandatory = true,
                    placeholder = localizedApp(R.string.owner_name_english),
                    enabled = true,
                    maxLength = 100
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ID Number
                CustomTextField(
                    value = idNumber,
                    onValueChange = { idNumber = it },
                    label = localizedApp(R.string.owner_id_number),
                    isNumeric = true,
                    mandatory = true,
                    placeholder = localizedApp(R.string.owner_id_number),
                    enabled = true,
                    maxLength = 20
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ownerShipPercentage
            CustomTextField(
                value = ownerShipPercentage,
                onValueChange = { ownerShipPercentage = it },
                label = localizedApp(R.string.enter_ownershippercentage),
                mandatory = true,
                placeholder = localizedApp(R.string.enter_ownershippercentage),
                enabled = true,
                maxLength = 5 ,
                minLength = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Is Representative Checkbox
            CustomCheckBox(
                checked = isRepresentative,
                onCheckedChange = { isRepresentative = it },
                label = localizedApp(R.string.is_representative),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            CustomTextField(
                value = email,
                onValueChange = { email = it },
                label = localizedApp(R.string.email),
                mandatory = true,
                placeholder = localizedApp(R.string.email),
                enabled = true,
                maxLength = 100
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mobile
            CustomTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = localizedApp(R.string.owner_mobile),
                isNumeric = true,
                mandatory = true,
                placeholder = localizedApp(R.string.owner_mobile),
                enabled = true,
                maxLength = 30
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Address
            CustomTextField(
                value = address,
                onValueChange = { address = it },
                label = localizedApp(R.string.owner_address),
                mandatory = true,
                placeholder = localizedApp(R.string.owner_address),
                enabled = true,
                maxLength = 300
            )

            Spacer(modifier = Modifier.height(16.dp))

            // File Upload Section using CustomFileUpload
            CustomFileUpload(
                value = documentUri,
                onValueChange = { documentUri = it },
                label = localizedApp(R.string.ownership_proof_document),
                mandatory = true,
                fieldId = "ownershipProofDocument",
                onOpenFilePicker = { fieldId, allowedTypes ->
                    filePickerFieldId = fieldId
                    filePickerAllowedTypes = allowedTypes
                },
                onRemoveFile = { documentUri = "" }
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
                        val ownerData = OwnerData(
                            id = owner?.id ?: java.util.UUID.randomUUID().toString(),
                            fullName = fullName,
                            ownerName = ownerName,
                            ownerNameEn = ownerNameEn,
                            nationality = nationality,
                            idNumber = idNumber,
                            ownerShipPercentage = ownerShipPercentage,
                            email = email,
                            mobile = mobile,
                            address = address,
                            city = city,
                            country = country,
                            postalCode = postalCode,
                        isCompany = isCompany,
                        companyRegistrationNumber = companyRegistrationNumber,
                        companyName = companyName,
                        companyType = companyType,
                        isRepresentative = isRepresentative,
                        ownershipProofDocument = documentUri,
                        documentName = ""
                        )
                        onSave(ownerData)
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
