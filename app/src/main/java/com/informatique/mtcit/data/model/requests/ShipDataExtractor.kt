package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.json.*
import java.util.Locale

/**
 * Extracts and structures ship data from API response
 * Organizes data into logical sections with localized labels
 */
object ShipDataExtractor {

    /**
     * Extract structured ship data sections from API response
     */
    fun extractShipDataSections(dataJson: JsonElement): List<RequestDetailSection> {
        val sections = mutableListOf<RequestDetailSection>()

        try {
            val dataObject = dataJson.jsonObject

            // ✅ Check if this is a scheduled inspection request (engineer view)
            if (dataObject.containsKey("scheduledDate") && dataObject.containsKey("inspectionRequest")) {
                return extractScheduledInspectionSections(dataObject)
            }

            // Extract shipInfo if present (normal flow)
            val shipInfo = dataObject["shipInfo"]?.jsonObject
            if (shipInfo != null) {
                // 1. Ship Basic Information
                extractShipBasicInfo(shipInfo)?.let { sections.add(it) }

                // 2. Dimensions
                extractDimensions(shipInfo)?.let { sections.add(it) }

                // 3. Weights & Tonnage
                extractWeights(shipInfo)?.let { sections.add(it) }

                // 4. Engines
                extractEngines(shipInfo)?.let { sections.add(it) }

                // 5. Owners
                extractOwners(shipInfo)?.let { sections.add(it) }
            }

            // 6. Documents
            extractDocuments(dataObject)?.let { sections.add(it) }

            // 7. Insurance Info
            extractInsuranceInfo(dataObject)?.let { sections.add(it) }

        } catch (e: Exception) {
            println("❌ Error extracting ship data: ${e.message}")
        }

        return sections
    }

    /**
     * ✅ Extract sections for scheduled inspection request (engineer view)
     */
    private fun extractScheduledInspectionSections(dataObject: JsonObject): List<RequestDetailSection> {
        val sections = mutableListOf<RequestDetailSection>()

        try {
            // 1. Scheduled Inspection Info
            extractScheduledInspectionInfo(dataObject)?.let { sections.add(it) }

            // 2. Inspection Request Details
            val inspectionRequest = dataObject["inspectionRequest"]?.jsonObject
            if (inspectionRequest != null) {
                extractInspectionRequestInfo(inspectionRequest)?.let { sections.add(it) }

                // 3. Ship Info from inspection request
                val shipInfo = inspectionRequest["shipInfo"]?.jsonObject
                if (shipInfo != null) {
                    extractShipBasicInfo(shipInfo)?.let { sections.add(it) }
                    extractDimensions(shipInfo)?.let { sections.add(it) }
                    extractWeights(shipInfo)?.let { sections.add(it) }
                    extractEngines(shipInfo)?.let { sections.add(it) }
                    extractOwners(shipInfo)?.let { sections.add(it) }
                }
            }

            // 4. Engineers
            extractEngineersInfo(dataObject)?.let { sections.add(it) }

            // 5. Work Orders
            extractWorkOrdersInfo(dataObject)?.let { sections.add(it) }

            // 6. Work Order Result
            extractWorkOrderResultInfo(dataObject)?.let { sections.add(it) }

        } catch (e: Exception) {
            println("❌ Error extracting scheduled inspection data: ${e.message}")
            e.printStackTrace()
        }

        return sections
    }

    /**
     * Extract basic ship information
     */
    private fun extractShipBasicInfo(shipInfo: JsonObject): RequestDetailSection? {
        val ship = shipInfo["ship"]?.jsonObject ?: return null
        val fields = mutableListOf<RequestDetailField>()

        // IMO Number
        ship["imoNumber"]?.jsonPrimitive?.intOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("imo_number"),
                value = it.toString()
            ))
        }

        // Call Sign
        ship["callSign"]?.jsonPrimitive?.contentOrNull?.let {
            if (it.isNotBlank()) {
                fields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("call_sign"),
                    value = it
                ))
            }
        }

        // MMSI Number
        ship["mmsiNumber"]?.jsonPrimitive?.intOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("mmsi_number"),
                value = it.toString()
            ))
        }

        // Official Number
        ship["officialNumber"]?.jsonPrimitive?.contentOrNull?.let {
            if (it.isNotBlank()) {
                fields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("official_number"),
                    value = it
                ))
            }
        }

        // Port of Registry
        ship["portOfRegistry"]?.jsonObject?.let { port ->
            val portName = getLocalizedName(port)
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("port_of_registry"),
                value = portName
            ))
        }

        // Marine Activity
        ship["marineActivity"]?.jsonObject?.let { activity ->
            val activityName = getLocalizedName(activity)
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("marine_activity"),
                value = activityName
            ))
        }

        // Ship Category
        ship["shipCategory"]?.jsonObject?.let { category ->
            val categoryName = getLocalizedName(category)
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("ship_category"),
                value = categoryName
            ))
        }

        // Ship Type
        ship["shipType"]?.jsonObject?.let { type ->
            val typeName = getLocalizedName(type)
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("ship_type"),
                value = typeName
            ))
        }

        // Build Country
        ship["buildCountry"]?.jsonObject?.let { country ->
            val countryName = getLocalizedName(country)
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("build_country"),
                value = countryName
            ))
        }

        // Build Material
        ship["buildMaterial"]?.jsonObject?.let { material ->
            val materialName = getLocalizedName(material)
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("build_material"),
                value = materialName
            ))
        }

        // Build Year
        ship["shipBuildYear"]?.jsonPrimitive?.intOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("build_year"),
                value = it.toString()
            ))
        }

        // Build End Date
        ship["buildEndDate"]?.jsonPrimitive?.contentOrNull?.let {
            if (it.isNotBlank()) {
                fields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("build_end_date"),
                    value = formatDate(it)
                ))
            }
        }

        // First Registration Date
        ship["firstRegistrationDate"]?.jsonPrimitive?.contentOrNull?.let {
            if (it.isNotBlank()) {
                fields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("first_registration_date"),
                    value = formatDate(it)
                ))
            }
        }

        // Decks Number
        ship["decksNumber"]?.jsonPrimitive?.intOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("decks_number"),
                value = it.toString()
            ))
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("ship_information"),
                fields = fields
            )
        } else null
    }

    /**
     * Extract dimensions
     */
    private fun extractDimensions(shipInfo: JsonObject): RequestDetailSection? {
        val ship = shipInfo["ship"]?.jsonObject ?: return null
        val fields = mutableListOf<RequestDetailField>()

        // Length Overall
        ship["vesselLengthOverall"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("length_overall"),
                value = "$it ${getLocalizedLabel("meters")}"
            ))
        }

        // Beam (Width)
        ship["vesselBeam"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("beam_width"),
                value = "$it ${getLocalizedLabel("meters")}"
            ))
        }

        // Draft
        ship["vesselDraft"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("draft"),
                value = "$it ${getLocalizedLabel("meters")}"
            ))
        }

        // Height
        ship["vesselHeight"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("height"),
                value = "$it ${getLocalizedLabel("meters")}"
            ))
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("dimensions"),
                fields = fields
            )
        } else null
    }

    /**
     * Extract weights and tonnage
     */
    private fun extractWeights(shipInfo: JsonObject): RequestDetailSection? {
        val ship = shipInfo["ship"]?.jsonObject ?: return null
        val fields = mutableListOf<RequestDetailField>()

        // Gross Tonnage
        ship["grossTonnage"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("gross_tonnage"),
                value = "$it ${getLocalizedLabel("tons")}"
            ))
        }

        // Net Tonnage
        ship["netTonnage"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("net_tonnage"),
                value = "$it ${getLocalizedLabel("tons")}"
            ))
        }

        // Deadweight Tonnage
        ship["deadweightTonnage"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("deadweight_tonnage"),
                value = "$it ${getLocalizedLabel("tons")}"
            ))
        }

        // Max Load Capacity
        ship["maxLoadCapacity"]?.jsonPrimitive?.doubleOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("max_load_capacity"),
                value = "$it ${getLocalizedLabel("tons")}"
            ))
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("weights_tonnage"),
                fields = fields
            )
        } else null
    }

    /**
     * Extract engines information
     */
    private fun extractEngines(shipInfo: JsonObject): RequestDetailSection? {
        val enginesArray = shipInfo["shipInfoEngines"]?.jsonArray ?: return null
        if (enginesArray.isEmpty()) return null

        val engineItems = mutableListOf<List<RequestDetailField>>()

        enginesArray.forEach { engineElement ->
            val engineInfo = engineElement.jsonObject
            val engine = engineInfo["engine"]?.jsonObject

            if (engine != null) {
                val fields = mutableListOf<RequestDetailField>()

                // Engine Serial Number
                engine["engineSerialNumber"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("engine_serial"),
                        value = it
                    ))
                }

                // Engine Type
                engine["engineType"]?.jsonObject?.let { type ->
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("engine_type"),
                        value = getLocalizedName(type)
                    ))
                }

                // Engine Power
                engine["enginePower"]?.jsonPrimitive?.doubleOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("engine_power"),
                        value = "$it ${getLocalizedLabel("hp")}"
                    ))
                }

                // Cylinders Count
                engine["cylindersCount"]?.jsonPrimitive?.intOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("cylinders"),
                        value = it.toString()
                    ))
                }

                // Engine Model
                engine["engineModel"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("engine_model"),
                        value = it
                    ))
                }

                // Manufacturer
                engine["engineManufacturer"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("manufacturer"),
                        value = it
                    ))
                }

                // Manufacturing Country
                engine["engineCountry"]?.jsonObject?.let { country ->
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("manufacturing_country"),
                        value = getLocalizedName(country)
                    ))
                }

                // Build Year
                engine["engineBuildYear"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("build_year"),
                        value = it
                    ))
                }

                if (fields.isNotEmpty()) {
                    engineItems.add(fields)
                }
            }
        }

        return if (engineItems.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("engines"),
                fields = listOf(RequestDetailField.ArrayField(
                    label = getLocalizedLabel("engines"),
                    items = engineItems
                ))
            )
        } else null
    }

    /**
     * Extract owners information
     */
    private fun extractOwners(shipInfo: JsonObject): RequestDetailSection? {
        val ownersArray = shipInfo["shipInfoOwners"]?.jsonArray ?: return null
        if (ownersArray.isEmpty()) return null

        val ownerItems = mutableListOf<List<RequestDetailField>>()

        ownersArray.forEach { ownerElement ->
            val ownerInfo = ownerElement.jsonObject
            val owner = ownerInfo["owner"]?.jsonObject

            if (owner != null) {
                val fields = mutableListOf<RequestDetailField>()

                // Owner Name (Arabic)
                owner["ownerName"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("owner_name_ar"),
                        value = it
                    ))
                }

                // Owner Name (English)
                owner["ownerNameEn"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("owner_name_en"),
                        value = it
                    ))
                }

                // Civil ID
                owner["ownerCivilId"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("civil_id"),
                        value = it
                    ))
                }

                // Ownership Percentage
                owner["ownershipPercentage"]?.jsonPrimitive?.doubleOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("ownership_percentage"),
                        value = "$it%"
                    ))
                }

                // Address
                owner["ownerAddress"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("address"),
                        value = it
                    ))
                }

                // Phone
                owner["ownerPhone"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("phone"),
                        value = it
                    ))
                }

                // Email
                owner["ownerEmail"]?.jsonPrimitive?.contentOrNull?.let {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("email"),
                        value = it
                    ))
                }

                // Is Representative
                owner["isRepresentative"]?.jsonPrimitive?.intOrNull?.let {
                    val isRep = it == 1
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("is_representative"),
                        value = if (isRep) getLocalizedLabel("yes") else getLocalizedLabel("no")
                    ))
                }

                if (fields.isNotEmpty()) {
                    ownerItems.add(fields)
                }
            }
        }

        return if (ownerItems.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("owners"),
                fields = listOf(RequestDetailField.ArrayField(
                    label = getLocalizedLabel("owners"),
                    items = ownerItems
                ))
            )
        } else null
    }

    /**
     * Extract documents
     */
    private fun extractDocuments(dataObject: JsonObject): RequestDetailSection? {
        val documentsArray = dataObject["documents"]?.jsonArray ?: return null
        if (documentsArray.isEmpty()) return null

        val fields = mutableListOf<RequestDetailField>()

        documentsArray.forEachIndexed { index, docElement ->
            val doc = docElement.jsonObject
            val fileName = doc["fileName"]?.jsonPrimitive?.contentOrNull ?: "Document ${index + 1}"
            val docRefNum = doc["docRefNum"]?.jsonPrimitive?.contentOrNull ?: ""

            fields.add(RequestDetailField.SimpleField(
                label = "${getLocalizedLabel("document")} ${index + 1}",
                value = fileName
            ))
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("documents"),
                fields = fields
            )
        } else null
    }

    /**
     * Extract insurance information
     */
    private fun extractInsuranceInfo(dataObject: JsonObject): RequestDetailSection? {
        val insuranceArray = dataObject["insuranceInfo"]?.jsonArray ?: return null
        if (insuranceArray.isEmpty()) return null

        // Parse insurance info if present
        // For now, return null as structure not fully defined
        return null
    }

    /**
     * Get localized name from object (nameAr/nameEn)
     */
    private fun getLocalizedName(jsonObject: JsonObject): String {
        val isArabic = Locale.getDefault().language == "ar"
        return if (isArabic) {
            jsonObject["nameAr"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["name"]?.jsonPrimitive?.contentOrNull
                ?: ""
        } else {
            jsonObject["nameEn"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["name"]?.jsonPrimitive?.contentOrNull
                ?: ""
        }
    }

    /**
     * ✅ Extract scheduled inspection info
     */
    private fun extractScheduledInspectionInfo(dataObject: JsonObject): RequestDetailSection? {
        val fields = mutableListOf<RequestDetailField>()

        // Scheduled Date
        dataObject["scheduledDate"]?.jsonPrimitive?.contentOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("scheduled_date"),
                value = formatDateTime(it)
            ))
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("scheduled_inspection_info"),
                fields = fields
            )
        } else null
    }

    /**
     * ✅ Extract inspection request info
     */
    private fun extractInspectionRequestInfo(inspectionRequest: JsonObject): RequestDetailSection? {
        val fields = mutableListOf<RequestDetailField>()

        // Request Number
        inspectionRequest["requestNumber"]?.jsonPrimitive?.contentOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("request_number"),
                value = it
            ))
        }

        // Purpose
        inspectionRequest["purpose"]?.jsonObject?.let { purpose ->
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("inspection_purpose"),
                value = getLocalizedName(purpose)
            ))
        }

        // Authority
        inspectionRequest["authority"]?.jsonObject?.let { authority ->
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("authority"),
                value = getLocalizedName(authority)
            ))
        }

        // Request Status
        inspectionRequest["requestStatus"]?.jsonObject?.let { status ->
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("request_status"),
                value = getLocalizedName(status)
            ))
        }

        // Inspection Date
        inspectionRequest["inspectionDate"]?.jsonPrimitive?.contentOrNull?.let {
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("inspection_date"),
                value = formatDate(it)
            ))
        }

        // Place
        inspectionRequest["place"]?.jsonObject?.let { place ->
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("inspection_place"),
                value = getLocalizedName(place)
            ))

            // Port of Registry for the place
            place["portOfRegistry"]?.jsonObject?.let { port ->
                fields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("place_port"),
                    value = getLocalizedName(port)
                ))
            }
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("inspection_request_details"),
                fields = fields
            )
        } else null
    }

    /**
     * ✅ Extract engineers info
     */
    private fun extractEngineersInfo(dataObject: JsonObject): RequestDetailSection? {
        val engineers = dataObject["engineers"]?.jsonArray ?: return null
        if (engineers.isEmpty()) return null

        val fields = mutableListOf<RequestDetailField>()

        engineers.forEachIndexed { index, element ->
            val engineer = element.jsonObject
            val engineerFields = mutableListOf<RequestDetailField>()

            // Name
            engineer["nameAr"]?.jsonPrimitive?.contentOrNull?.let { nameAr ->
                engineer["nameEn"]?.jsonPrimitive?.contentOrNull?.let { nameEn ->
                    engineerFields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("engineer_name"),
                        value = if (Locale.getDefault().language == "ar") nameAr else nameEn
                    ))
                }
            }

            // National ID
            engineer["nationalId"]?.jsonPrimitive?.contentOrNull?.let {
                engineerFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("national_id"),
                    value = it
                ))
            }

            // Job
            engineer["job"]?.jsonObject?.let { job ->
                engineerFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("job_title"),
                    value = getLocalizedName(job)
                ))
            }

            // Port
            engineer["port"]?.jsonObject?.let { port ->
                engineerFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("engineer_port"),
                    value = getLocalizedName(port)
                ))
            }

            // Phone
            engineer["phoneNum"]?.jsonPrimitive?.contentOrNull?.let {
                engineerFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("phone"),
                    value = it
                ))
            }

            // Email
            engineer["email"]?.jsonPrimitive?.contentOrNull?.let {
                engineerFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("email"),
                    value = it
                ))
            }

            if (engineerFields.isNotEmpty()) {
                fields.add(RequestDetailField.NestedObject(
                    label = "${getLocalizedLabel("engineer")} ${index + 1}",
                    fields = engineerFields
                ))
            }
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("assigned_engineers"),
                fields = fields
            )
        } else null
    }

    /**
     * ✅ Extract work orders info
     */
    private fun extractWorkOrdersInfo(dataObject: JsonObject): RequestDetailSection? {
        val workOrders = dataObject["workOrders"]?.jsonArray ?: return null
        if (workOrders.isEmpty()) return null

        val fields = mutableListOf<RequestDetailField>()

        workOrders.forEachIndexed { index, element ->
            val workOrder = element.jsonObject
            val orderFields = mutableListOf<RequestDetailField>()

            // Inspection Engineer
            workOrder["inspectionEngineer"]?.jsonObject?.let { engineer ->
                val engineerName = getLocalizedName(engineer)
                orderFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("inspection_engineer"),
                    value = engineerName
                ))
            }

            // Inspection Date
            workOrder["inspectionDate"]?.jsonPrimitive?.contentOrNull?.let {
                orderFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("inspection_date"),
                    value = formatDateTime(it)
                ))
            }

            // Status
            workOrder["status"]?.jsonObject?.let { status ->
                orderFields.add(RequestDetailField.SimpleField(
                    label = getLocalizedLabel("work_order_status"),
                    value = getLocalizedName(status)
                ))
            }

            if (orderFields.isNotEmpty()) {
                fields.add(RequestDetailField.NestedObject(
                    label = "${getLocalizedLabel("work_order")} ${index + 1}",
                    fields = orderFields
                ))
            }
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("work_orders"),
                fields = fields
            )
        } else null
    }

    /**
     * ✅ Extract work order result info
     */
    private fun extractWorkOrderResultInfo(dataObject: JsonObject): RequestDetailSection? {
        val workOrderResult = dataObject["workOrderResult"]?.jsonObject ?: return null
        val fields = mutableListOf<RequestDetailField>()

        // Status
        workOrderResult["status"]?.jsonObject?.let { status ->
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("result_status"),
                value = getLocalizedName(status)
            ))
        }

        // Decision
        workOrderResult["decision"]?.jsonObject?.let { decision ->
            fields.add(RequestDetailField.SimpleField(
                label = getLocalizedLabel("inspection_decision"),
                value = getLocalizedName(decision)
            ))
        }

        // Answers
        val answers = workOrderResult["answers"]?.jsonArray
        if (answers != null && answers.isNotEmpty()) {
            val answerFields = mutableListOf<RequestDetailField>()

            answers.forEach { element ->
                val answer = element.jsonObject
                val checklistItem = answer["checklistSettingsItem"]?.jsonObject

                checklistItem?.let { item ->
                    val question = item["question"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    val answerValue = answer["answer"]?.jsonPrimitive?.contentOrNull ?: ""

                    // Get answer display value (from choices if applicable)
                    val displayValue = when (item["checklistType"]?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull) {
                        4 -> { // List type - lookup choice by ID
                            val choices = item["choices"]?.jsonArray
                            choices?.find {
                                it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == answerValue
                            }?.jsonObject?.get("answer")?.jsonPrimitive?.contentOrNull ?: answerValue
                        }
                        else -> answerValue
                    }

                    answerFields.add(RequestDetailField.SimpleField(
                        label = question,
                        value = displayValue
                    ))
                }
            }

            if (answerFields.isNotEmpty()) {
                fields.add(RequestDetailField.NestedObject(
                    label = getLocalizedLabel("inspection_answers"),
                    fields = answerFields
                ))
            }
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("work_order_result"),
                fields = fields
            )
        } else null
    }

    /**
     * Format datetime string
     */
    private fun formatDateTime(dateTimeStr: String): String {
        return try {
            // Convert ISO format to readable format
            // 2026-01-22T15:35:00 -> 2026-01-22 15:35
            dateTimeStr.replace("T", " ").take(16)
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    /**
     * Get localized label for field keys
     */
    private fun getLocalizedLabel(key: String): String {
        val isArabic = Locale.getDefault().language == "ar"

        return when (key) {
            // Sections
            "ship_information" -> if (isArabic) "معلومات السفينة" else "Ship Information"
            "dimensions" -> if (isArabic) "الأبعاد" else "Dimensions"
            "weights_tonnage" -> if (isArabic) "الأوزان والحمولة" else "Weights & Tonnage"
            "engines" -> if (isArabic) "المحركات" else "Engines"
            "owners" -> if (isArabic) "المالكون" else "Owners"
            "documents" -> if (isArabic) "المستندات" else "Documents"

            // Ship Basic Info
            "imo_number" -> if (isArabic) "رقم IMO" else "IMO Number"
            "call_sign" -> if (isArabic) "رمز النداء" else "Call Sign"
            "mmsi_number" -> if (isArabic) "رقم MMSI" else "MMSI Number"
            "official_number" -> if (isArabic) "الرقم الرسمي" else "Official Number"
            "port_of_registry" -> if (isArabic) "ميناء التسجيل" else "Port of Registry"
            "marine_activity" -> if (isArabic) "النشاط البحري" else "Marine Activity"
            "ship_category" -> if (isArabic) "فئة السفينة" else "Ship Category"
            "ship_type" -> if (isArabic) "نوع السفينة" else "Ship Type"
            "build_country" -> if (isArabic) "بلد الصنع" else "Build Country"
            "build_material" -> if (isArabic) "مادة البناء" else "Build Material"
            "build_year" -> if (isArabic) "سنة الصنع" else "Build Year"
            "build_end_date" -> if (isArabic) "تاريخ انتهاء البناء" else "Build End Date"
            "first_registration_date" -> if (isArabic) "تاريخ أول تسجيل" else "First Registration Date"
            "decks_number" -> if (isArabic) "عدد الطوابق" else "Number of Decks"

            // Dimensions
            "length_overall" -> if (isArabic) "الطول الإجمالي" else "Length Overall"
            "beam_width" -> if (isArabic) "العرض" else "Beam (Width)"
            "draft" -> if (isArabic) "الغاطس" else "Draft"
            "height" -> if (isArabic) "الارتفاع" else "Height"
            "meters" -> if (isArabic) "متر" else "m"

            // Weights
            "gross_tonnage" -> if (isArabic) "الحمولة الإجمالية" else "Gross Tonnage"
            "net_tonnage" -> if (isArabic) "الحمولة الصافية" else "Net Tonnage"
            "deadweight_tonnage" -> if (isArabic) "حمولة الوزن الساكن" else "Deadweight Tonnage"
            "max_load_capacity" -> if (isArabic) "أقصى سعة تحميل" else "Max Load Capacity"
            "tons" -> if (isArabic) "طن" else "tons"

            // Engines
            "engine_serial" -> if (isArabic) "الرقم التسلسلي" else "Serial Number"
            "engine_type" -> if (isArabic) "نوع المحرك" else "Engine Type"
            "engine_power" -> if (isArabic) "قوة المحرك" else "Engine Power"
            "cylinders" -> if (isArabic) "عدد الأسطوانات" else "Cylinders"
            "engine_model" -> if (isArabic) "الموديل" else "Model"
            "manufacturer" -> if (isArabic) "الشركة المصنعة" else "Manufacturer"
            "manufacturing_country" -> if (isArabic) "بلد التصنيع" else "Manufacturing Country"
            "hp" -> if (isArabic) "حصان" else "HP"

            // Owners
            "owner_name_ar" -> if (isArabic) "اسم المالك (عربي)" else "Owner Name (Arabic)"
            "owner_name_en" -> if (isArabic) "اسم المالك (إنجليزي)" else "Owner Name (English)"
            "civil_id" -> if (isArabic) "الرقم المدني" else "Civil ID"
            "ownership_percentage" -> if (isArabic) "نسبة الملكية" else "Ownership %"
            "address" -> if (isArabic) "العنوان" else "Address"
            "phone" -> if (isArabic) "الهاتف" else "Phone"
            "email" -> if (isArabic) "البريد الإلكتروني" else "Email"
            "is_representative" -> if (isArabic) "الممثل القانوني" else "Legal Representative"

            // Documents
            "document" -> if (isArabic) "مستند" else "Document"

            // Common
            "yes" -> if (isArabic) "نعم" else "Yes"
            "no" -> if (isArabic) "لا" else "No"

            // ✅ Scheduled Inspection Fields
            "scheduled_inspection_info" -> if (isArabic) "معلومات الجدولة" else "Scheduling Information"
            "scheduled_date" -> if (isArabic) "موعد المعاينة المجدول" else "Scheduled Date"
            "inspection_request_details" -> if (isArabic) "تفاصيل طلب المعاينة" else "Inspection Request Details"
            "request_number" -> if (isArabic) "رقم الطلب" else "Request Number"
            "inspection_purpose" -> if (isArabic) "الغرض من المعاينة" else "Inspection Purpose"
            "authority" -> if (isArabic) "الجهة" else "Authority"
            "request_status" -> if (isArabic) "حالة الطلب" else "Request Status"
            "inspection_date" -> if (isArabic) "تاريخ المعاينة" else "Inspection Date"
            "inspection_place" -> if (isArabic) "مكان المعاينة" else "Inspection Place"
            "place_port" -> if (isArabic) "ميناء المكان" else "Place Port"
            "assigned_engineers" -> if (isArabic) "المهندسون المعينون" else "Assigned Engineers"
            "engineer" -> if (isArabic) "مهندس" else "Engineer"
            "engineer_name" -> if (isArabic) "اسم المهندس" else "Engineer Name"
            "national_id" -> if (isArabic) "الرقم الوطني" else "National ID"
            "job_title" -> if (isArabic) "المسمى الوظيفي" else "Job Title"
            "engineer_port" -> if (isArabic) "الميناء" else "Port"
            "work_orders" -> if (isArabic) "أوامر العمل" else "Work Orders"
            "work_order" -> if (isArabic) "أمر عمل" else "Work Order"
            "inspection_engineer" -> if (isArabic) "مهندس المعاينة" else "Inspection Engineer"
            "work_order_status" -> if (isArabic) "حالة أمر العمل" else "Work Order Status"
            "work_order_result" -> if (isArabic) "نتيجة أمر العمل" else "Work Order Result"
            "result_status" -> if (isArabic) "حالة النتيجة" else "Result Status"
            "inspection_decision" -> if (isArabic) "قرار المعاينة" else "Inspection Decision"
            "inspection_answers" -> if (isArabic) "إجابات قائمة الفحص" else "Inspection Checklist Answers"

            else -> key
        }
    }

    /**
     * Format date string
     */
    private fun formatDate(dateStr: String): String {
        return try {
            // Simple date formatting - can be enhanced
            dateStr.take(10)
        } catch (e: Exception) {
            dateStr
        }
    }
}

