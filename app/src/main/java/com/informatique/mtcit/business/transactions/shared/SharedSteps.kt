package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.R
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.components.PersonType
import com.informatique.mtcit.ui.components.SelectableItem
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

    fun personTypeStep(
        options: List<PersonType>
    ): StepData {
        val fields = mutableListOf<FormField>()

        fields.add(
            FormField.SelectableList(
                id = "selectionPersonType",
                options = options,
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.person_type_title,
            descriptionRes = R.string.person_type_desc,
            fields = fields
        )
    }

    fun commercialRegistrationStep(
        options: List<SelectableItem>
    ): StepData {
        val fields = mutableListOf<FormField>()

        fields.add(
            FormField.SelectableList(
                id = "selectionData",
                options = options,
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.commercial_registration_title,
            descriptionRes = R.string.commercial_registration_desc,
            fields = fields
        )
    }

    fun engineInfoStep(
        manufacturers: List<String>,
        countries: List<String>,
        fuelTypes: List<String>,
        engineConditions: List<String>
    ): StepData {
        val fields = mutableListOf<FormField>()

        fields.add(
            FormField.EngineList(
                id = "engines",
                labelRes = R.string.engine_info,
                value = "[]",
                manufacturers = manufacturers,
                countries = countries,
                fuelTypes = fuelTypes,
                engineConditions = engineConditions,
                mandatory = true,
            )
        )

        return StepData(
            titleRes = R.string.engine_title,
            descriptionRes = R.string.engine_description,
            fields = fields
        )
    }

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
        maritimeactivity: List<String>,
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
                mandatory = true,
                placeholder = R.string.select_unit_type_placeholder.toString()

            )
        )
        fields.add(
            FormField.DropDown(
                id = "unitClassification",
                labelRes = R.string.select_unit_classification_placeholder,
                options = shipTypes,
                mandatory = true,
                placeholder = R.string.select_unit_classification_placeholder.toString()

            )
        )

        // Always included: Call Sign
        fields.add(
            FormField.TextField(
                id = "callSign",
                labelRes = R.string.enter_call_sign,
                mandatory = true,
                placeholder = R.string.enter_call_sign.toString()  // استخدم placeholderRes
            )
        )

        // Optional: IMO Number
        if (includeIMO) {
            fields.add(
                FormField.TextField(
                    id = "imoNumber",
                    labelRes = R.string.enter_imo_number,
                    isNumeric = true,
                    mandatory = true
                )
            )
        }

        // Always included: Registration Port
        fields.add(
            FormField.DropDown(
                id = "registrationPort",
                labelRes = R.string.select_registration_port,
                options = ports,
                mandatory = true
            )
        )

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

        // Optional: Manufacturer Information
        if (includeManufacturer) {
            fields.addAll(
                listOf(
//                    FormField.TextField(
//                        id = "shipManufacturer",
//                        labelRes = R.string.ship_manufacturer,
//                        mandatory = true
//                    ),
                    FormField.TextField(
                        id = "manufacturerYear",
                        labelRes = R.string.ship_manufacture_year,
                        isNumeric = true,
                        mandatory = true
                    )
                )
            )
        }

        fields.add(
            FormField.DropDown(
                id = "unitClassification",
                labelRes = R.string.select_maritime_activity,
                options = maritimeactivity,
                mandatory = true,
                placeholder = R.string.select_maritime_activity.toString()

            )
        )

        // Always included: Call Sign
        fields.add(
            FormField.TextField(
                id = "constructionpool",
                labelRes = R.string.enter_construction_pool,
                mandatory = false,
                placeholder = R.string.enter_construction_pool.toString()  // استخدم placeholderRes
            )
        )

        fields.add(
            FormField.DropDown(
                id = "unitClassification",
                labelRes = R.string.select_proof_type,
                options = shipTypes,
                mandatory = true,
                placeholder = R.string.select_proof_type.toString()

            )
        )

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
                        allowedTypes = listOf(
                            "pdf",
                            "jpg",
                            "jpeg",
                            "png",
                            "doc",
                            "docx",
                            "xls",
                            "xlsx",
                            "txt"
                        ),
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
        fields.add(
            FormField.DropDown(
                id = "unitClassification",
                labelRes = R.string.select_building_material,
                options = maritimeactivity,
                mandatory = true,
                placeholder = R.string.select_building_material.toString()

            )
        )

        // Add transaction-specific fields
        fields.addAll(additionalFields)

        return StepData(
            titleRes = R.string.unit_data,
            descriptionRes = R.string.unit_data_description,
            fields = fields
        )
    }

    /**
     * Marine Unit Dimensions Step
     * Used for: Ship Registration, Dimension Change, Technical Modifications
     *
     * Collects physical dimensions of the marine unit including:
     * - Overall Length
     * - Overall Width
     * - Depth/Draft
     * - Number of Decks (optional)
     * - Height (optional)
     */
    fun marineUnitDimensionsStep(
        includeHeight: Boolean = true,
        includeDecksCount: Boolean = true
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Overall Length (mandatory)
        fields.add(
            FormField.TextField(
                id = "overallLength",
                labelRes = R.string.overall_length_placeholder,
                placeholder = R.string.overall_length_placeholder.toString(),
                isNumeric = true,
                mandatory = true
            )
        )

        // Overall Width (mandatory)
        fields.add(
            FormField.TextField(
                id = "overallWidth",
                labelRes = R.string.overall_width_placeholder,
                placeholder = R.string.overall_width_placeholder.toString(),
                isNumeric = true,
                mandatory = true
            )
        )

        // Depth/Draft (mandatory)
        fields.add(
            FormField.TextField(
                id = "depth",
                labelRes = R.string.depth_placeholder,
                placeholder = R.string.depth_placeholder.toString(),
                isNumeric = true,
                mandatory = true
            )
        )

        // Optional: Height from base to highest point
        if (includeHeight) {
            fields.add(
                FormField.TextField(
                    id = "height",
                    labelRes = R.string.height_placeholder,
                    placeholder = R.string.height_placeholder.toString(),
                    isNumeric = true,
                    mandatory = false
                )
            )
        }

        // Optional: Number of Decks
        if (includeDecksCount) {
            fields.add(
                FormField.TextField(
                    id = "decksCount",
                    labelRes = R.string.decks_count_placeholder,
                    placeholder = R.string.decks_count_placeholder.toString(),
                    isNumeric = true,
                    mandatory = false
                )
            )
        }

        return StepData(
            titleRes = R.string.marine_unit_dimensions_title,
            descriptionRes = R.string.marine_unit_dimensions_description,
            fields = fields
        )
    }


    /**
     * Marine Unit Name Selection Step
     * Used for: Ship Registration, Name Change
     *
     * Allows user to choose and reserve a name for the marine unit.
     * The name will be reserved for a limited time period and requires payment to confirm.
     *
     * @param showReservationInfo Show reservation information message (default: true)
     */
    fun marineUnitNameSelectionStep(
        showReservationInfo: Boolean = true,
        selectedMarineUnits: List<MarineUnit> = emptyList(), // ✅ السفن المختارة
        isAddingNewUnit: Boolean = false // ✅ هل بيضيف سفينة جديدة؟
    ): StepData {
        val fields = mutableListOf<FormField>()

        // ✅ لو اختار سفينة موجودة، جيب اسمها
        val prefilledName = if (!isAddingNewUnit && selectedMarineUnits.isNotEmpty()) {
            selectedMarineUnits.first().name // أول سفينة مختارة
        } else {
            ""
        }

        // Marine Unit Name (mandatory)
        fields.add(
            FormField.TextField(
                id = "marineUnitName",
                labelRes = R.string.marine_unit_name,
                placeholder = R.string.marine_unit_name_placeholder.toString(),
                mandatory = true,
                initialValue = prefilledName, // ✅ املأ الاسم لو موجود
                enabled = isAddingNewUnit || selectedMarineUnits.isEmpty()
            )
        )

        return StepData(
            titleRes = R.string.marine_unit_name_selection_title,
            descriptionRes = if (showReservationInfo) {
                R.string.marine_unit_name_selection_description
            } else {
                R.string.marine_unit_name_selection_description_simple
            },
            fields = fields
        )
    }


    /**
     * Insurance Document Data Step
     * Used for: Ship transactions that require insurance information
     *
     * Collects insurance document information including:
     * - Insurance Document Number
     * - Country
     * - Insurance Company
     * - Insurance Document Attachment (PDF, images, etc.)
     *
     * @param countries List of countries from API
     */
    fun insuranceDocumentStep(
        countries: List<String>

    ): StepData {
        val fields = mutableListOf<FormField>()

        // Insurance Document Number (mandatory)
        fields.add(
            FormField.TextField(
                id = "insuranceDocumentNumber",
                labelRes = R.string.insurance_document_number_placeholder,
                placeholder = R.string.insurance_document_number_placeholder.toString(),
                mandatory = true
            )
        )

        // Country (mandatory)
        fields.add(
            FormField.DropDown(
                id = "insuranceCountry",
                labelRes = R.string.insurance_country_placeholder,
                options = countries,
                mandatory = true,
                placeholder = R.string.insurance_country_placeholder.toString()
            )
        )

        // Insurance Company (mandatory)
        fields.add(
            FormField.DropDown(
                id = "insuranceCompany",
                labelRes = R.string.insurance_company_placeholder,
                options = countries, // This should be populated from API
                mandatory = true,
                placeholder = R.string.insurance_company_placeholder.toString()
            )
        )

        // Insurance Document Attachment (mandatory)
        fields.add(
            FormField.FileUpload(
                id = "insuranceDocumentFile",
                labelRes = R.string.insurance_document_attachment,
                allowedTypes = listOf("pdf", "jpg", "jpeg", "png"),
                maxSizeMB = 5,
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.insurance_document_title,
            descriptionRes = R.string.insurance_document_description,
            fields = fields
        )
    }


    /**
     * Marine Unit Registration Certificate Step
     * Used for: Various ship transactions that require checking registration status
     *
     * Asks if the marine unit has a temporary registration certificate.
     * This determines the flow and requirements for the transaction.
     *
     * @param showInfoMessage Show additional information message (default: true)
     */
    fun marineUnitRegistrationCertificateStep(
        showInfoMessage: Boolean = true
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Registration Certificate Question (mandatory)
        fields.add(
            FormField.RadioGroup(
                id = "hasTemporaryRegistrationCertificate",
                labelRes = R.string.has_temporary_registration_certificate_question,
                options = listOf(
                    FormField.RadioOption(
                        value = "yes",
                        labelRes = R.string.yes_has_temporary_certificate,
                        isEnabled = true // ✅ ضروري تضيفها!
                    ),
                    FormField.RadioOption(
                        value = "no",
                        labelRes = R.string.no_temporary_certificate,
                        isEnabled = true // ✅ ضروري تضيفها!
                    )
                ),
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.marine_unit_registration_certificate_title,
            descriptionRes = if (showInfoMessage) {
                R.string.marine_unit_registration_certificate_description
            } else {
                R.string.marine_unit_registration_certificate_description_simple
            },
            fields = fields
        )
    }


    /**
     * Marine Unit Weights and Loads Step
     * Used for: Ship Registration, Technical Modifications, Load Capacity Changes
     *
     * Collects weight and load capacity information including:
     * - Gross Tonnage
     * - Net Tonnage
     * - Static Load (DWT)
     * - Maximum Permitted Load (optional)
     *
     * @param includeMaxPermittedLoad Show maximum permitted load field (default: true)
     */
    fun marineUnitWeightsStep(
        includeMaxPermittedLoad: Boolean = true
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Gross Tonnage (mandatory)
        fields.add(
            FormField.TextField(
                id = "grossTonnage",
                labelRes = R.string.gross_tonnage_placeholder,
                placeholder = R.string.gross_tonnage_placeholder.toString(),
                isNumeric = true,
                mandatory = true
            )
        )

        // Net Tonnage (mandatory)
        fields.add(
            FormField.TextField(
                id = "netTonnage",
                labelRes = R.string.net_tonnage_placeholder,
                placeholder = R.string.net_tonnage_placeholder.toString(),
                isNumeric = true,
                mandatory = true
            )
        )

        // Static Load - DWT (mandatory)
        fields.add(
            FormField.TextField(
                id = "staticLoad",
                labelRes = R.string.static_load_placeholder,
                placeholder = R.string.static_load_placeholder.toString(),
                isNumeric = true,
                mandatory = true
            )
        )

        // Optional: Maximum Permitted Load
        if (includeMaxPermittedLoad) {
            fields.add(
                FormField.TextField(
                    id = "maxPermittedLoad",
                    labelRes = R.string.max_permitted_load_placeholder,
                    placeholder = R.string.max_permitted_load_placeholder.toString(),
                    isNumeric = true,
                    mandatory = false
                )
            )
        }

        return StepData(
            titleRes = R.string.marine_unit_weights_title,
            descriptionRes = R.string.marine_unit_weights_description,
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
                mandatory = true,
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

    fun marineUnitSelectionStep(
        units: List<MarineUnit>,
        allowMultipleSelection: Boolean = true,
        showOwnedUnitsWarning: Boolean = true,
        showAddNewButton: Boolean = true, // ✅ أضف الـ parameter ده
    ): StepData {
        return StepData(
            titleRes = R.string.owned_ships,
            descriptionRes = R.string.owned_ships,
            fields = listOf(
                FormField.MarineUnitSelector(
                    id = "selectedMarineUnits",
                    labelRes = R.string.owned_ships,
                    value = "[]", // Empty array by default
                    units = units,
                    allowMultipleSelection = allowMultipleSelection,
                    showOwnedUnitsWarning = showOwnedUnitsWarning,
                    showAddNewButton = showAddNewButton, // ✅ مرهه هنا
                    mandatory = true
                )
            )
        )
    }

}

/**
 * Document Configuration for customizable documents step
 */
data class DocumentConfig(
    val id: String,
    val labelRes: Int,
    val allowedTypes: List<String> = listOf(
        "pdf",
        "jpg",
        "jpeg",
        "png",
        "doc",
        "docx",
        "xls",
        "xlsx",
        "txt"
    ),
    val maxSizeMB: Int = 5,
    val mandatory: Boolean = true
)

data class MarineUnit(
    val id: String,                    // معرف فريد
    val name: String,                  // اسم الوحدة
    val type: String,                  // نوع الوحدة البحرية
    val imoNumber: String,             // رقم IMO
    val callSign: String,              // رمز النداء
    val maritimeId: String,            // رقم الهوية البحرية
    val registrationPort: String,      // ميناء التسجيل
    val activity: String,              // النشاط البحري
    val isOwned: Boolean = false,      // هل مملوكة/موظفة؟

    // الأبعاد
    val totalLength: String = "",                   // الطول الكلي
    val lengthBetweenPerpendiculars: String = "",   // الطول بين العموديين
    val totalWidth: String = "",                    // العرض الكلي
    val draft: String = "",                         // الغاطس
    val height: String = "",                        // الإرتفاع
    val numberOfDecks: String = "",                 // عدد الطوابق

    // السعة والحمولة
    val totalCapacity: String = "",                 // الحمولة الإجمالية
    val containerCapacity: String = "",             // سعة الحاويات

    // المخالفات والاحتجازات
    val violationsCount: String = "",               // عدد المخالفات
    val detentionsCount: String = "",               // عدد الاحتجازات

    // الديون والمستحقات
    val amountDue: String = "",                     // المبلغ المستحق
    val paymentStatus: String = "",                 // حالة السداد

    // NEW: Extended fields for business validation (fetched from backend)
    val registrationStatus: String = "ACTIVE",      // حالة التسجيل: ACTIVE, SUSPENDED, CANCELLED
    val registrationType: String = "PERMANENT",     // نوع التسجيل: PERMANENT, TEMPORARY
    val isMortgaged: Boolean = false,               // هل مرهونة؟
    val mortgageDetails: MortgageDetails? = null,   // تفاصيل الرهن إن وجدت

    // NEW: Inspection fields for Temporary Registration
    val isInspected: Boolean = false,               // هل تم الفحص؟
    val inspectionStatus: String = "NOT_VERIFIED",  // حالة الفحص: VALID, PENDING, NOT_VERIFIED, REJECTED, EXPIRED
    val inspectionRemarks: String = ""              // ملاحظات الفحص
)

/**
 * Mortgage details for a marine unit
 */
data class MortgageDetails(
    val mortgageId: String,
    val bankName: String,
    val startDate: String,
    val endDate: String,
    val amount: String
)
