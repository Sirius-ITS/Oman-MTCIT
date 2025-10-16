package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.R
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData

/**
 * Reusable Step Library for Ship Category Transactions
 * Provides common step templates that can be customized per transaction
 *
 * Usage:
 * - ShipRegistrationStrategy uses all shared steps
 * - NameChangeStrategy reuses owner info + documents (different requirements)
 * - DimensionChangeStrategy reuses owner info + adds technical fields
 */
object SharedSteps {

    // Special step ID to identify the owner info step with custom UI
    const val STEP_ID_OWNER_INFO_MULTIPLE = "owner_info_multiple"

    /**
     * Unit Selection Step (Ship Information)
     * Used by: Ship Registration, Name Change, Dimension Change, etc.
     *
     * @param shipTypes List of ship types from API
     * @param ports List of ports from API
     * @param countries List of countries from API
     * @param includeIMO Show IMO number field (default: true)
     * @param includeMMSI Show MMSI number field (default: true)
     * @param includeManufacturer Show manufacturer fields (default: true)
     * @param includeProofDocument Show proof document upload (default: true)
     * @param includeConstructionDates Show construction dates (default: true)
     * @param includeRegistrationCountry Show registration country (default: true)
     * @param additionalFields Add transaction-specific fields
     */
    fun unitSelectionStep(
        shipTypes: List<String>,
        ports: List<String>,
        countries: List<String>,
        includeIMO: Boolean = true,
        includeMMSI: Boolean = true,
        includeManufacturer: Boolean = true,
        includeProofDocument: Boolean = true,
        includeConstructionDates: Boolean = true,
        includeRegistrationCountry: Boolean = true,
        additionalFields: List<FormField> = emptyList()
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Always included: Unit Type
        fields.add(
            FormField.DropDown(
                id = "unitType",
                labelRes = R.string.select_unit_type_placeholder,
                options = shipTypes,
                mandatory = true
            )
        )

        // Always included: Call Sign
        fields.add(
            FormField.TextField(
                id = "callSign",
                labelRes = R.string.call_sign,
                mandatory = true
            )
        )

        // Optional: IMO Number
        if (includeIMO) {
            fields.add(
                FormField.TextField(
                    id = "imoNumber",
                    labelRes = R.string.imo_number,
                    isNumeric = true,
                    mandatory = true
                )
            )
        }

        // Optional: MMSI Number
        if (includeMMSI) {
            fields.add(
                FormField.TextField(
                    id = "mmsi",
                    labelRes = R.string.mmsi_number,
                    isNumeric = true,
                    mandatory = true
                )
            )
        }

        // Always included: Registration Port
        fields.add(
            FormField.DropDown(
                id = "registrationPort",
                labelRes = R.string.registration_port,
                options = ports,
                mandatory = true
            )
        )

        // Optional: Manufacturer Information
        if (includeManufacturer) {
            fields.addAll(
                listOf(
                    FormField.TextField(
                        id = "shipManufacturer",
                        labelRes = R.string.ship_manufacturer,
                        mandatory = true
                    ),
                    FormField.TextField(
                        id = "manufacturerYear",
                        labelRes = R.string.ship_manufacture_year,
                        isNumeric = true,
                        mandatory = true
                    )
                )
            )
        }

        // Optional: Proof Document
        if (includeProofDocument) {
            fields.addAll(
                listOf(
                    FormField.DropDown(
                        id = "proofType",
                        labelRes = R.string.proof_type,
                        optionRes = listOf(
                            R.string.ownership_certificate,
                            R.string.sale_contract,
                            R.string.registration_document,
                            R.string.other
                        ),
                        mandatory = true
                    ),
                    FormField.FileUpload(
                        id = "proofDocument",
                        labelRes = R.string.proof_document,
                        allowedTypes = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"),
                        mandatory = true
                    )
                )
            )
        }

        // Optional: Construction Dates
        if (includeConstructionDates) {
            fields.addAll(
                listOf(
                    FormField.DatePicker(
                        id = "constructionStartDate",
                        labelRes = R.string.construction_start_date,
                        allowPastDates = true,
                        mandatory = true
                    ),
                    FormField.DatePicker(
                        id = "constructionEndDate",
                        labelRes = R.string.construction_end_date,
                        allowPastDates = false,
                        mandatory = true
                    ),
                    FormField.DatePicker(
                        id = "firstRegistrationDate",
                        labelRes = R.string.first_registration_date_optional,
                        allowPastDates = false,
                        mandatory = false
                    )
                )
            )
        }

        // Optional: Registration Country
        if (includeRegistrationCountry) {
            fields.add(
                FormField.DropDown(
                    id = "registrationCountry",
                    labelRes = R.string.building_country_optional,
                    options = countries,
                    mandatory = false
                )
            )
        }

        // Add transaction-specific fields
        fields.addAll(additionalFields)

        return StepData(
            titleRes = R.string.unit_data,
            descriptionRes = R.string.unit_data_description,
            fields = fields
        )
    }

    /**
     * Owner Information Step - WITH MULTIPLE OWNERS SUPPORT
     * Used by: All ship transactions (registration, name change, dimension change)
     *
     * This returns a step with OwnerList field type that will be handled generically by DynamicForm
     * No special detection needed - it works like any other field type!
     *
     * @param nationalities List of countries for nationality dropdown
     * @param countries List of countries for address dropdown
     * @param includeCompanyFields Show company registration fields (default: true)
     */
    fun ownerInfoStep(
        nationalities: List<String>,
        countries: List<String>,
        includeCompanyFields: Boolean = true
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Owner list field - this will be rendered as OwnerListManager component
        // The OwnerListManager will display the total count input internally
        fields.add(
            FormField.OwnerList(
                id = "owners",
                labelRes = R.string.owner_info,
                value = "[]", // Empty array by default
                nationalities = nationalities,
                countries = countries,
                includeCompanyFields = includeCompanyFields,
                totalCountFieldId = "totalOwnersCount", // This tells OwnerListManager to show count input
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.owner_info,
            descriptionRes = R.string.owner_info_description,
            fields = fields
        )
    }

    /**
     * Documents Step (Customizable per transaction)
     * Used by: All ship transactions with different document requirements
     *
     * @param requiredDocuments List of required document configurations
     */
    fun documentsStep(
        requiredDocuments: List<DocumentConfig>
    ): StepData {
        val fields = requiredDocuments.map { doc ->
            FormField.FileUpload(
                id = doc.id,
                labelRes = doc.labelRes,
                allowedTypes = doc.allowedTypes,
                maxSizeMB = doc.maxSizeMB,
                mandatory = doc.mandatory
            )
        }

        return StepData(
            titleRes = R.string.documents,
            descriptionRes = R.string.documents_description,
            fields = fields
        )
    }

    /**
     * Review Step (Generic across all transactions)
     */
    fun reviewStep(): StepData {
        return StepData(
            titleRes = R.string.review,
            descriptionRes = R.string.step_placeholder_content,
            fields = emptyList()
        )
    }
}

/**
 * Document Configuration for customizable documents step
 */
data class DocumentConfig(
    val id: String,
    val labelRes: Int,
    val allowedTypes: List<String> = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"),
    val maxSizeMB: Int = 5,
    val mandatory: Boolean = true
)
