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

            // Extract shipInfo if present
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
     * Get localized label
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

