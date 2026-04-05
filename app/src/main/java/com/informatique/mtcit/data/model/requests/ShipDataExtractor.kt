package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.json.*
import java.util.Locale
import com.informatique.mtcit.common.util.AppLanguage

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

            // 8. Navigation Areas (change transactions: types 10/11/12/13)
            extractNavRequestAreas(dataObject)?.let { sections.add(it) }

            // 9. Crew List (change captain / navigation permit transactions)
            extractCrewLists(dataObject)?.let { sections.add(it) }

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
     * Extract navigation request areas (navRequestAreas) — present in change transactions (10/11/12/13)
     */
    private fun extractNavRequestAreas(dataObject: JsonObject): RequestDetailSection? {
        val areasArray = dataObject["navRequestAreas"]?.jsonArray ?: return null
        if (areasArray.isEmpty()) return null

        val fields = mutableListOf<RequestDetailField>()
        areasArray.forEachIndexed { index, element ->
            val area = element.jsonObject
            val areaName = getLocalizedName(area)
            if (areaName.isNotBlank()) {
                fields.add(RequestDetailField.SimpleField(
                    label = "${getLocalizedLabel("area")} ${index + 1}",
                    value = areaName
                ))
            }
        }

        return if (fields.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("navigation_areas"),
                fields = fields
            )
        } else null
    }

    /**
     * Extract crew list (crewLists) — present in change-captain and navigation-permit transactions
     */
    private fun extractCrewLists(dataObject: JsonObject): RequestDetailSection? {
        val crewArray = dataObject["crewLists"]?.jsonArray ?: return null
        if (crewArray.isEmpty()) return null

        val crewItems = mutableListOf<List<RequestDetailField>>()

        crewArray.forEach { element ->
            val crew = element.jsonObject
            val fields = mutableListOf<RequestDetailField>()

            // Name (Arabic)
            crew["nameAr"]?.jsonPrimitive?.contentOrNull?.let {
                if (it.isNotBlank()) {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("crew_name_ar"),
                        value = it
                    ))
                }
            }

            // Name (English)
            crew["nameEn"]?.jsonPrimitive?.contentOrNull?.let {
                if (it.isNotBlank()) {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("crew_name_en"),
                        value = it
                    ))
                }
            }

            // Job Title
            crew["jobTitle"]?.jsonObject?.let { jobTitle ->
                val jobName = getLocalizedName(jobTitle)
                if (jobName.isNotBlank()) {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("job_title"),
                        value = jobName
                    ))
                }
            }

            // Civil Number
            crew["civilNo"]?.jsonPrimitive?.contentOrNull?.let {
                if (it.isNotBlank()) {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("civil_id"),
                        value = it
                    ))
                }
            }

            // Seaman Book Number
            crew["seamenBookNo"]?.jsonPrimitive?.contentOrNull?.let {
                if (it.isNotBlank()) {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("seamen_book_no"),
                        value = it
                    ))
                }
            }

            // Nationality
            crew["nationality"]?.jsonObject?.let { nationality ->
                val nationalityName = getLocalizedName(nationality)
                if (nationalityName.isNotBlank()) {
                    fields.add(RequestDetailField.SimpleField(
                        label = getLocalizedLabel("nationality"),
                        value = nationalityName
                    ))
                }
            }

            if (fields.isNotEmpty()) {
                crewItems.add(fields)
            }
        }

        return if (crewItems.isNotEmpty()) {
            RequestDetailSection(
                title = getLocalizedLabel("crew_list"),
                fields = listOf(RequestDetailField.ArrayField(
                    label = getLocalizedLabel("crew_list"),
                    items = crewItems
                ))
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
        val isArabic = AppLanguage.isArabic
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
                        value = if (AppLanguage.isArabic) nameAr else nameEn
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
        val isArabic = AppLanguage.isArabic

        return when (key) {
            // Sections
            "ship_information" -> if (AppLanguage.isArabic) "معلومات السفينة" else "Ship Information"
            "dimensions" -> if (AppLanguage.isArabic) "الأبعاد" else "Dimensions"
            "weights_tonnage" -> if (AppLanguage.isArabic) "الأوزان والحمولة" else "Weights & Tonnage"
            "engines" -> if (AppLanguage.isArabic) "المحركات" else "Engines"
            "owners" -> if (AppLanguage.isArabic) "المالكون" else "Owners"
            "documents" -> if (AppLanguage.isArabic) "رفع المستندات" else "Documents"

            // Ship Basic Info
            "imo_number" -> if (AppLanguage.isArabic) "رقم IMO" else "IMO Number"
            "call_sign" -> if (AppLanguage.isArabic) "رمز النداء" else "Call Sign"
            "mmsi_number" -> if (AppLanguage.isArabic) "رقم الهوية البحرية (MMSI)" else "Maritime ID (MMSI)"
            "official_number" -> if (AppLanguage.isArabic) "الرقم الرسمي" else "Official Number"
            "port_of_registry" -> if (AppLanguage.isArabic) "ميناء التسجيل" else "Port of Registry"
            "marine_activity" -> if (AppLanguage.isArabic) "النشاط البحري" else "Marine Activity"
            "ship_category" -> if (AppLanguage.isArabic) "فئة السفينة" else "Ship Category"
            "ship_type" -> if (AppLanguage.isArabic) "نوع السفينة" else "Ship Type"
            "build_country" -> if (AppLanguage.isArabic) "بلد الصنع" else "Build Country"
            "build_material" -> if (AppLanguage.isArabic) "مادة البناء" else "Build Material"
            "build_year" -> if (AppLanguage.isArabic) "سنة الصنع" else "Build Year"
            "build_end_date" -> if (AppLanguage.isArabic) "تاريخ انتهاء البناء" else "Build End Date"
            "first_registration_date" -> if (AppLanguage.isArabic) "تاريخ أول تسجيل" else "First Registration Date"
            "decks_number" -> if (AppLanguage.isArabic) "عدد الطوابق" else "Number of Decks"

            // Dimensions
            "length_overall" -> if (AppLanguage.isArabic) "الطول الإجمالي" else "Length Overall"
            "beam_width" -> if (AppLanguage.isArabic) "العرض" else "Beam (Width)"
            "draft" -> if (AppLanguage.isArabic) "الغاطس" else "Draft"
            "height" -> if (AppLanguage.isArabic) "الارتفاع" else "Height"
            "meters" -> if (AppLanguage.isArabic) "متر" else "m"

            // Weights
            "gross_tonnage" -> if (AppLanguage.isArabic) "الحمولة الإجمالية (طن)" else "Gross Tonnage (tons)"
            "net_tonnage" -> if (AppLanguage.isArabic) "الحمولة الصافية (طن)" else "Net Tonnage (tons)"
            "deadweight_tonnage" -> if (AppLanguage.isArabic) "حمولة الوزن الساكن" else "Deadweight Tonnage"
            "max_load_capacity" -> if (AppLanguage.isArabic) "أقصى سعة تحميل" else "Max Load Capacity"
            "tons" -> if (AppLanguage.isArabic) "طن" else "tons"

            // Engines
            "engine_serial" -> if (AppLanguage.isArabic) "الرقم التسلسلي" else "Serial Number"
            "engine_type" -> if (AppLanguage.isArabic) "نوع المحرك" else "Engine Type"
            "engine_power" -> if (AppLanguage.isArabic) "قوة المحرك (حصان)" else "Engine Power (HP)"
            "cylinders" -> if (AppLanguage.isArabic) "عدد الأسطوانات" else "Cylinders"
            "engine_model" -> if (AppLanguage.isArabic) "الموديل" else "Model"
            "manufacturer" -> if (AppLanguage.isArabic) "الشركة المصنعة" else "Manufacturer"
            "manufacturing_country" -> if (AppLanguage.isArabic) "بلد التصنيع" else "Manufacturing Country"
            "hp" -> if (AppLanguage.isArabic) "حصان" else "HP"

            // Owners
            "owner_name_ar" -> if (AppLanguage.isArabic) "اسم المالك (بالعربية)" else "Owner Name (Arabic)"
            "owner_name_en" -> if (AppLanguage.isArabic) "اسم المالك (بالانجليزية)" else "Owner Name (English)"
            "civil_id" -> if (AppLanguage.isArabic) "الرقم المدني" else "Civil ID"
            "ownership_percentage" -> if (AppLanguage.isArabic) "نسبة الملكية" else "Ownership %"
            "address" -> if (AppLanguage.isArabic) "العنوان" else "Address"
            "phone" -> if (AppLanguage.isArabic) "الهاتف" else "Phone"
            "email" -> if (AppLanguage.isArabic) "أدخل البريد الإلكتروني" else "Email:"
            "is_representative" -> if (AppLanguage.isArabic) "الممثل القانوني" else "Legal Representative"

            // Documents
            "document" -> if (AppLanguage.isArabic) "مستند" else "Document"

            // Navigation Areas (change transactions)
            "navigation_areas" -> if (AppLanguage.isArabic) "مناطق الإبحار" else "Navigation Areas"
            "area" -> if (AppLanguage.isArabic) "منطقة" else "Area"

            // Crew List
            "crew_list" -> if (AppLanguage.isArabic) "قائمة طاقم الملاحة" else "Crew List"
            "crew_name_ar" -> if (AppLanguage.isArabic) "الاسم (عربي)" else "Name (Arabic)"
            "crew_name_en" -> if (AppLanguage.isArabic) "الاسم (إنجليزي)" else "Name (English)"
            "seamen_book_no" -> if (AppLanguage.isArabic) "رقم كتيب البحار" else "Seaman Book No."
            "nationality" -> if (AppLanguage.isArabic) "الجنسية" else "Nationality:"

            // Common
            "yes" -> if (AppLanguage.isArabic) "نعم" else "Yes"
            "no" -> if (AppLanguage.isArabic) "لا" else "No"

            // ✅ Scheduled Inspection Fields
            "scheduled_inspection_info" -> if (AppLanguage.isArabic) "معلومات الجدولة" else "Scheduling Information"
            "scheduled_date" -> if (AppLanguage.isArabic) "موعد المعاينة المجدول" else "Scheduled Date"
            "inspection_request_details" -> if (AppLanguage.isArabic) "تفاصيل طلب المعاينة" else "Inspection Request Details"
            "request_number" -> if (AppLanguage.isArabic) "رقم الطلب" else "Request Number"
            "inspection_purpose" -> if (AppLanguage.isArabic) "الغرض من المعاينة" else "Inspection Purpose"
            "authority" -> if (AppLanguage.isArabic) "الجهة" else "Authority"
            "request_status" -> if (AppLanguage.isArabic) "حالة الطلب" else "Request Status"
            "inspection_date" -> if (AppLanguage.isArabic) "تاريخ المعاينة" else "Inspection Date"
            "inspection_place" -> if (AppLanguage.isArabic) "مكان المعاينة" else "Inspection Place"
            "place_port" -> if (AppLanguage.isArabic) "ميناء المكان" else "Place Port"
            "assigned_engineers" -> if (AppLanguage.isArabic) "المهندسون المعينون" else "Assigned Engineers"
            "engineer" -> if (AppLanguage.isArabic) "مهندس" else "Engineer"
            "engineer_name" -> if (AppLanguage.isArabic) "اسم المهندس" else "Engineer Name"
            "national_id" -> if (AppLanguage.isArabic) "الرقم القومي" else "National ID:"
            "job_title" -> if (AppLanguage.isArabic) "المسمى الوظيفي" else "Job Title"
            "engineer_port" -> if (AppLanguage.isArabic) "الميناء" else "Port"
            "work_orders" -> if (AppLanguage.isArabic) "أوامر العمل" else "Work Orders"
            "work_order" -> if (AppLanguage.isArabic) "أمر عمل" else "Work Order"
            "inspection_engineer" -> if (AppLanguage.isArabic) "مهندس المعاينة" else "Inspection Engineer"
            "work_order_status" -> if (AppLanguage.isArabic) "حالة أمر العمل" else "Work Order Status"
            "work_order_result" -> if (AppLanguage.isArabic) "نتيجة أمر العمل" else "Work Order Result"
            "result_status" -> if (AppLanguage.isArabic) "حالة النتيجة" else "Result Status"
            "inspection_decision" -> if (AppLanguage.isArabic) "قرار المعاينة" else "Inspection Decision"
            "inspection_answers" -> if (AppLanguage.isArabic) "إجابات قائمة الفحص" else "Inspection Checklist Answers"

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

