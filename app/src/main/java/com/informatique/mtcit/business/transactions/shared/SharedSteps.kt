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
            fields = fields,
            requiredLookups = listOf("countries", "engineStatuses",  "engineFuelTypes")
        )
    }

    fun sailorInfoStep(
        jobs: List<String>,
    ): StepData {
        val fields = mutableListOf<FormField>()

        fields.add(
            FormField.SailorList(
                id = "sailors",
                labelRes = R.string.sailor_info,
                value = "[]",
                jobs = jobs,
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.sailor_info,
            descriptionRes = R.string.sailor_info_description,
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
        shipCategories: List<String>,
        ports: List<String>,
        countries: List<String>,
        marineActivities: List<String>,
        proofTypes: List<String>,
        buildingMaterials: List<String> = emptyList(),
        engineStatuses: List<String> = emptyList(),
        includeIMO: Boolean = true,
        includeMMSI: Boolean = true,
        includeManufacturer: Boolean = true,
        includeProofDocument: Boolean = true,
        includeConstructionDates: Boolean = true,
        includeRegistrationCountry: Boolean = true,
        additionalFields: List<FormField> = emptyList(),
        isFishingBoat: Boolean = false,
        fishingBoatDataLoaded: Boolean = false
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Ship Category Dropdown (تصنيف الوحدة البحرية) - Select this first
        fields.add(
            FormField.DropDown(
                id = "unitClassification",
                labelRes = R.string.select_unit_classification_placeholder,
                options = shipCategories,
                mandatory = true,
                placeholder = R.string.select_unit_classification_placeholder.toString()
            )
        )

        // Ship Type Dropdown (نوع الوحدة البحرية) - Dependent on category selection
        // Starts empty and disabled until category is selected
        // When shipTypes are provided (after category selection), it becomes enabled
        fields.add(
            FormField.DropDown(
                id = "unitType",
                labelRes = R.string.select_unit_type_placeholder,
                options = shipTypes, // Use provided ship types (empty initially, populated after category selection)
                mandatory = true,
                placeholder = R.string.select_unit_type_placeholder.toString()
            )
        )

        // ✅ NEW: Agriculture Request Number field (only for fishing boats)
        if (isFishingBoat) {
            fields.add(
                FormField.TextField(
                    id = "agricultureRequestNumber",
                    labelRes = R.string.agriculture_request_number,
                    mandatory = true,
                    enabled = true,
                    placeholder = R.string.enter_agriculture_request_number.toString()
                )
            )
        }

        // Call Sign - DISABLED if fishing boat data loaded
        fields.add(
            FormField.TextField(
                id = "callSign",
                labelRes = R.string.enter_call_sign,
                mandatory = true,
                enabled = !fishingBoatDataLoaded,
                placeholder = R.string.enter_call_sign.toString()
            )
        )

        // Optional: IMO Number - DISABLED if fishing boat data loaded
        if (includeIMO) {
            fields.add(
                FormField.TextField(
                    id = "imoNumber",
                    labelRes = R.string.enter_imo_number,
                    isNumeric = true,
                    mandatory = false,
                    enabled = !fishingBoatDataLoaded
                )
            )
        }

        // Registration Port Dropdown (ميناء التسجيل)
        fields.add(
            FormField.DropDown(
                id = "registrationPort",
                labelRes = R.string.select_registration_port,
                options = ports,
                mandatory = true
            )
        )

        // Optional: MMSI Number - DISABLED if fishing boat data loaded
        if (includeMMSI) {
            fields.add(
                FormField.TextField(
                    id = "mmsi",
                    labelRes = R.string.mmsi_number,
                    isNumeric = true,
                    mandatory = false,
                    enabled = !fishingBoatDataLoaded
                )
            )
        }

        // Optional: Manufacturer Information - DISABLED if fishing boat data loaded
        if (includeManufacturer) {
            fields.addAll(
                listOf(
                    FormField.TextField(
                        id = "manufacturerYear",
                        labelRes = R.string.ship_manufacture_year,
                        isNumeric = true,
                        mandatory = true,
                        enabled = !fishingBoatDataLoaded
                    )
                )
            )
        }

        // Marine Activity Dropdown (النشاط البحري)
        fields.add(
            FormField.DropDown(
                id = "maritimeActivity",
                labelRes = R.string.select_maritime_activity,
                options = marineActivities,
                mandatory = true,
                placeholder = R.string.select_maritime_activity.toString()
            )
        )

        // Construction Pool TextField
        fields.add(
            FormField.TextField(
                id = "constructionpool",
                labelRes = R.string.enter_construction_pool,
                mandatory = false,
                placeholder = R.string.enter_construction_pool.toString()
            )
        )

        // Proof Type Dropdown (نوع الإثبات)
        fields.add(
            FormField.DropDown(
                id = "proofType",
                labelRes = R.string.select_proof_type,
                options = proofTypes,
                mandatory = true,
                placeholder = R.string.select_proof_type.toString()
            )
        )

        // Optional: Proof Document
        if (includeProofDocument) {
            fields.add(
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

        // Optional: Registration Country Dropdown (دولة البناء)
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

        // Building Material Dropdown (مادة البناء)
        if (buildingMaterials.isNotEmpty()) {
            fields.add(
                FormField.DropDown(
                    id = "buildingMaterial",
                    labelRes = R.string.select_building_material,
                    options = buildingMaterials,
                    mandatory = true,
                    placeholder = R.string.select_building_material.toString()
                )
            )
        }

        // Add transaction-specific fields
        fields.addAll(additionalFields)

        return StepData(
            titleRes = R.string.unit_data,
            descriptionRes = R.string.unit_data_description,
            fields = fields,
            requiredLookups = listOf("shipTypes", "shipCategories", "ports", "countries", "marineActivities", "proofTypes", "buildMaterials")
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
                isDecimal = true,
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
                isDecimal = true,
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
                isDecimal = true,
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
                    isDecimal = true,
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
            fields = fields)
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
                isDecimal = true,
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
                isDecimal = true,
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
                isDecimal = true,
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
                    isDecimal = true,
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
            fields = fields,
            requiredLookups = listOf("countries", "nationalities")
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
    /**
     * Inspection Purpose Step (الغرض من طلب المعاينة)
     * Allows selection of inspection purpose from available list
     * Used in inspection request transactions
     */
    fun inspectionPurposeStep(
        inspectionPurposes: List<String>
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Inspection Purpose Selection (mandatory)
        fields.add(
            FormField.DropDown(
                id = "inspectionPurpose",
                labelRes = R.string.inspection_purpose_selection,
                options = inspectionPurposes,
                mandatory = false,
                placeholder = R.string.select_inspection_purpose_placeholder.toString()
            )
        )
        fields.add(
            FormField.DropDown(
                id = "inspectionPurpose",
                labelRes = R.string.inspection_purpose_selection,
                options = inspectionPurposes,
                mandatory = false,
                placeholder = R.string.select_inspection_purpose_placeholder.toString()
            )
        )

        return StepData(
            titleRes = R.string.inspection_purpose_title,
            descriptionRes = R.string.inspection_purpose_description,
            fields = fields
        )
    }

    /**
     * Inspection Authority Step (جهة المعاينة)
     * Allows selection of authorized inspection authority
     * Used in inspection request transactions
     */
    fun inspectionAuthorityStep(
        inspectionAuthorities: List<String>
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Inspection Authority Selection (mandatory)
        fields.add(
            FormField.DropDown(
                id = "inspectionAuthority",
                labelRes = R.string.inspection_authority_selection,
                options = inspectionAuthorities,
                mandatory = false,
                placeholder = R.string.select_inspection_authority_placeholder.toString()
            )
        )

        return StepData(
            titleRes = R.string.inspection_authority_title,
            descriptionRes = R.string.inspection_authority_description,
            fields = fields
        )
    }
    /**
     * Transfer Inspection to Classification Society Step (تحويل المعاينة إلى هيئة تصنيف)
     * Allows transferring inspection to one of the approved classification societies
     * Used when redirecting inspection request to external classification body
     */
    fun transferInspectionToClassificationStep(
        classificationSocieties: List<String>
    ): StepData {
        val fields = mutableListOf<FormField>()

        // Classification Society Selection (mandatory)
        fields.add(
            FormField.DropDown(
                id = "classificationSociety",
                labelRes = R.string.classification_society_selection,
                options = classificationSocieties,
                mandatory = true,
                placeholder = R.string.select_classification_society_placeholder.toString()
            )
        )

        return StepData(
            titleRes = R.string.transfer_inspection_title,
            descriptionRes = R.string.transfer_inspection_description,
            fields = fields
        )
    }

    /**
     * Login Registration Step (تسجيل الدخول للبوابة)
     * Used for: Initial authentication/registration before accessing portal services
     *
     * Allows user to choose between:
     * - Mobile phone registration (requires locked phone SIM)
     * - Civil ID number (requires card reader)
     */
    fun loginRegistrationStep(): StepData {
        val fields = mutableListOf<FormField>()

        // ✅ 1. Benefits Section (InfoCard)
        fields.add(
            FormField.InfoCard(
                id = "loginBenefitsInfo",
                labelRes = R.string.login_benefits_title, // "ماذا تستفيد من تسجيل الدخول إلى البوابة؟"
                items = listOf(
                    R.string.benefit_unified_access,
                    R.string.benefit_personal_page,
                    R.string.benefit_easy_transfer,
                    R.string.benefit_edit_profile,
                    R.string.benefit_custom_notifications
                ),
                showCheckmarks = true,
                mandatory = false
            )
        )

        // ✅ 2. Registration Method Selection (RadioGroup)
        fields.add(
            FormField.RadioGroup(
                id = "registrationMethod",
                labelRes = R.string.registration_method_selection,
                options = listOf(
                    FormField.RadioOption(
                        value = "mobile_phone",
                        labelRes = R.string.mobile_phone_option,
                        descriptionRes = R.string.mobile_phone_description,
                        isEnabled = true
                    ),
                    FormField.RadioOption(
                        value = "civil_id",
                        labelRes = R.string.civil_id_option,
                        descriptionRes = R.string.civil_id_description,
                        isEnabled = true
                    )
                ),
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.login_registration_title, // "تسجيل الدخول"
            descriptionRes = R.string.login_registration_description,
            fields = fields
        )
    }

    /**
     * Mobile Phone Verification Step (إضافة رقم هاتفك المفعل)
     * Used for: Phone number verification via electronic authentication
     *
     * Collects mobile phone number with country code selection
     */
    fun mobilePhoneVerificationStep(): StepData {
        val fields = mutableListOf<FormField>()

        fields.add(
            FormField.PhoneNumberField(
                id = "mobilePhoneNumber",
                labelRes = R.string.mobile_phone_verification_label,
                countryCodes = listOf(
                    "+968", // Oman
                    "+966", // Saudi Arabia
                    "+971", // UAE
                    "+974", // Qatar
                    "+965", // Kuwait
                    "+973"  // Bahrain
                ),
                selectedCountryCode = "+968",
                placeholder = R.string.enter_phone_number,
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.mobile_phone_verification_title,
            descriptionRes = R.string.mobile_phone_verification_description,
            fields = fields
        )
    }
    /**
     * OTP Verification Step (تحقق من رقم هاتفك المحمول)
     * Used for: Verifying mobile phone number via OTP code
     *
     * Shows:
     * - Phone number entered by user
     * - OTP input field (6 digits)
     * - Countdown timer
     * - Resend OTP option
     */
    fun otpVerificationStep(
        phoneNumber: String = ""
    ): StepData {
        val fields = mutableListOf<FormField>()

        fields.add(
            FormField.OTPField(
                id = "otpCode",
                labelRes = R.string.otp_verification_label,
                phoneNumber = phoneNumber,
                otpLength = 6,
                remainingTime = 33,
                mandatory = true
            )
        )

        return StepData(
            titleRes = R.string.otp_verification_title, // "تحقق من رقم هاتفك المحمول"
            descriptionRes = R.string.otp_verification_description, // "لقد أرسلنا رمزاً مكوّن من 6 أرقام إلى رقم هاتفك المحمول."
            fields = fields
        )
    }

}

data class MarineUnit(
    // Core Ship Information
    val id: String = "",                                    // معرف فريد (string to match API ids)
    val shipName: String = "",                           // اسم السفينة
    val imoNumber: String? = null,                     // رقم IMO
    val callSign: String = "",                           // رمز النداء
    val mmsiNumber: String = "",                            // رقم MMSI
    val officialNumber: String = "",                     // الرقم الرسمي

    // Registration Information
    val portOfRegistry: PortOfRegistry = PortOfRegistry("") ,             // ميناء التسجيل
    val firstRegistrationDate: String = "",              // تاريخ التسجيل الأول
    val requestSubmissionDate: String = "",              // تاريخ تقديم الطلب
    val isTemp: String = "0",                                // هل مؤقت؟ ("1" = مؤقت، "0" = دائم)

    // Classification
    val marineActivity: MarineActivity = MarineActivity(0),             // النشاط البحري
    val shipCategory: ShipCategory = ShipCategory(0),                 // فئة السفينة
    val shipType: ShipType = ShipType(0),                         // نوع السفينة
    val proofType: ProofType = ProofType(0),                       // نوع الإثبات

    // Build Information
    val buildCountry: BuildCountry = BuildCountry(""),                 // بلد البناء
    val buildMaterial: BuildMaterial = BuildMaterial(0),               // مادة البناء
    val shipBuildYear: String = "",                         // سنة البناء (string for API compatibility)
    val buildEndDate: String = "",                       // تاريخ انتهاء البناء
    val shipYardName: String = "",                       // اسم حوض البناء

    // Tonnage & Capacity
    val grossTonnage: String = "",                       // الحمولة الإجمالية
    val netTonnage: String = "",                         // الحمولة الصافية
    val deadweightTonnage: String = "",                  // حمولة الوزن الثقيل
    val maxLoadCapacity: String = "",                    // الحد الأقصى للحمولة

    // Additional fields (optional - يمكن إضافتها لاحقاً)
    val totalLength: String = "",                // الطول الكلي
    val lengthBetweenPerpendiculars: String = "",// الطول بين العموديين
    val totalWidth: String = "",                 // العرض الكلي
    val draft: String = "",                      // الغاطس
    val height: String = "",                     // الإرتفاع
    val numberOfDecks: String = "",              // عدد الطوابق
    val containerCapacity: String = "",          // سعة الحاويات
    val violationsCount: String = "",            // عدد المخالفات
    val detentionsCount: String = "",            // عدد الاحتجازات
    val amountDue: String = "",                  // المبلغ المستحق
    val paymentStatus: String = "",              // حالة السداد
    val isMortgaged: Boolean = false,               // هل مرهونة؟
    val isInspected: Boolean = false,               // هل تم الفحص؟
    val inspectionStatus: String? = null,           // حالة الفحص
    val inspectionRemarks: String? = null           // ملاحظات الفحص
) {
     // Computed properties for UI compatibility
     val maritimeId: String get() = mmsiNumber  // Use MMSI as maritimeId
     val name: String get() = shipName                     // Alias for shipName
     val type: String get() = "نوع ${shipType.id}"         // TODO: Map shipType.id to Arabic name
     val registrationPort: String get() = portOfRegistry.id // TODO: Map port ID to name
     val activity: String get() = "نشاط ${marineActivity.id}" // TODO: Map activity ID to Arabic name
     val isOwned: Boolean get() = true // TODO: Determine ownership from API or user context
     val registrationStatus: String get() = if (isTemp == "1" || isTemp == "true") "TEMPORARY" else "PERMANENT"
     val registrationType: String get() = if (isTemp == "1" || isTemp == "true") "TEMPORARY" else "PERMANENT"
     val totalCapacity: String get() = if (grossTonnage.isNotEmpty()) "${grossTonnage} طن" else ""
}

// Nested data classes
data class PortOfRegistry(
    val id: String                                  // معرف الميناء
)

data class MarineActivity(
    val id: Int                                     // معرف النشاط البحري
)

data class ShipCategory(
    val id: Int                                     // معرف فئة السفينة
)

data class ShipType(
    val id: Int                                     // معرف نوع السفينة
)

data class ProofType(
    val id: Int                                     // معرف نوع الإثبات
)

data class BuildCountry(
    val id: String                                  // كود الدولة (ISO)
)

data class BuildMaterial(
    val id: Int                                     // معرف مادة البناء
)

// Response wrapper
data class ShipsResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: ShipsData
)

data class ShipsData(
    val content: List<ShipItem>,
    val pageable: Pageable,
    val last: Boolean,
    val totalPages: Int,
    val totalElements: Int,
    val size: Int,
    val number: Int,
    val sort: List<Sort>,
    val first: Boolean,
    val numberOfElements: Int,
    val empty: Boolean
)

data class ShipItem(
    val coreShipsResDto: MarineUnit
)

data class Pageable(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: List<Sort>,
    val offset: Int,
    val paged: Boolean,
    val unpaged: Boolean
)

data class Sort(
    val direction: String,
    val property: String,
    val ignoreCase: Boolean,
    val nullHandling: String,
    val ascending: Boolean,
    val descending: Boolean
)


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
