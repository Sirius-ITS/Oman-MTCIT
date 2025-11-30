package com.informatique.mtcit.business.transactions.mapper

import com.informatique.mtcit.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Mapper to convert form data to API request models
 */
object RegistrationRequestMapper {

    // Date formatters
    private val inputDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val outputDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    /**
     * Map form data from Unit Selection Step to CreateRegistrationRequest
     *
     * @param formData All accumulated form data from previous steps
     * @param requestTypeId The type of registration request (1 = Temporary, 2 = Permanent, etc.)
     * @param requestId Optional request ID for PUT updates (null for initial POST)
     * @return CreateRegistrationRequest ready to send to API
     */
    fun mapToCreateRegistrationRequest(
        formData: Map<String, String>,
        requestTypeId: Int,
        requestId: Int? = null // ✅ NEW: Optional for PUT updates
    ): CreateRegistrationRequest {

        // Extract isCompany from person type selection
        val isCompany = if (formData["selectionPersonType"] == "شركة") 1 else 0

        // Get current date in ISO format
        val currentDate = LocalDate.now().format(outputDateFormatter)

        // Extract and map ship data
        val ship = Ship(
            shipName = formData["marineUnitName"] ?: "",
            imoNumber = formData["imoNumber"]?.toIntOrNull(),
            callSign = formData["callSign"] ?: "",
            mmsiNumber = formData["mmsi"]?.toIntOrNull(),
            officialNumber = formData["imoNumber"] ?: formData["officialNumber"], // Use IMO number for now
            portOfRegistry = PortOfRegistryRef(
                id = extractPortId(formData["registrationPort"])
            ),
            marineActivity = MarineActivityRef(
                id = extractMarineActivityId(formData["maritimeActivity"])
            ),
            shipCategory = ShipCategoryRef(
                id = extractShipCategoryId(formData["unitClassification"])
            ),
            shipType = ShipTypeRef(
                id = extractShipTypeId(formData["unitType"]),
                shipCategory = ShipCategoryRef(
                    id = extractShipCategoryId(formData["unitClassification"])
                )
            ),
            proofType = ProofTypeRef(
                id = extractProofTypeId(formData["proofType"])
            ),
            buildCountry = formData["registrationCountry"]?.let {
                BuildCountryRef(id = extractCountryId(it))
            },
            buildMaterial = formData["buildingMaterial"]?.let {
                BuildMaterialRef(id = extractBuildMaterialId(it))
            } ?: BuildMaterialRef(id = 21), // Default to Metal (21) if not provided
            shipBuildYear = formData["manufacturerYear"]?.toIntOrNull() ?: LocalDate.now().year,
            buildEndDate = convertDateFormat(formData["constructionEndDate"]) ?: currentDate,
            shipYardName = formData["constructionpool"],
            firstRegistrationDate = convertDateFormat(formData["firstRegistrationDate"]),
            requestSubmissionDate = currentDate
        )

        return CreateRegistrationRequest(
            regShipRegRequestReqDto = RegShipRegRequestReqDto(
                id = requestId, // ✅ NEW: Include request ID for PUT updates
                shipInfo = ShipInfo(
                    ship = ship,
                    isCurrent = 0 // Always 0 for now
                ),
                requestType = RequestType(id = requestTypeId)
            ),
            isCompany = isCompany
        )
    }

    /**
     * Convert date from dd/MM/yyyy format to yyyy-MM-dd format
     * Returns null if input is null or invalid
     */
    private fun convertDateFormat(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null

        return try {
            // Parse the date from dd/MM/yyyy format
            val date = LocalDate.parse(dateString, inputDateFormatter)
            // Format it to yyyy-MM-dd
            date.format(outputDateFormatter)
        } catch (e: Exception) {
            println("⚠️ Failed to parse date: $dateString - ${e.message}")
            // If parsing fails, return the original string (might already be in correct format)
            dateString
        }
    }

    /**
     * Extract port ID from selected port name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractPortId(portName: String?): String {
        // For now, return a mock ID based on port name
        // In production, you should have a Map<String, String> from lookups
        return when {
            portName?.contains("صحار") == true || portName?.contains("Sohar") == true -> "OMSOH"
            portName?.contains("صلالة") == true || portName?.contains("Salalah") == true -> "OMSAL"
            portName?.contains("مسقط") == true || portName?.contains("Muscat") == true -> "OMMUS"
            portName?.contains("الدقم") == true || portName?.contains("Duqm") == true -> "OMDQM"
            else -> "OMSOH" // Default
        }
    }

    /**
     * Extract marine activity ID from selected activity name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractMarineActivityId(activityName: String?): Int {
        // For now, return a mock ID
        // In production, you should have a Map<String, Int> from lookups
        return when {
            activityName?.contains("صيد") == true || activityName?.contains("Fishing") == true -> 1
            activityName?.contains("شحن") == true || activityName?.contains("Cargo") == true -> 2
            activityName?.contains("ركاب") == true || activityName?.contains("Passenger") == true -> 3
            activityName?.contains("نقل") == true || activityName?.contains("Transport") == true -> 4
            activityName?.contains("سياحة") == true || activityName?.contains("Tourism") == true -> 5
            activityName?.contains("نفط") == true || activityName?.contains("Oil") == true -> 6
            else -> 1 // Default
        }
    }

    /**
     * Extract ship category ID from selected category name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractShipCategoryId(categoryName: String?): Int {
        // For now, return a mock ID
        return when {
            categoryName?.contains("A") == true -> 1
            categoryName?.contains("B") == true -> 2
            categoryName?.contains("C") == true -> 3
            categoryName?.contains("D") == true -> 4
            else -> 3 // Default
        }
    }

    /**
     * Extract ship type ID from selected type name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractShipTypeId(typeName: String?): Int {
        // For now, return a mock ID
        return when {
            typeName?.contains("صيد") == true || typeName?.contains("Fishing") == true -> 10
            typeName?.contains("شحن") == true || typeName?.contains("Cargo") == true -> 11
            typeName?.contains("ركاب") == true || typeName?.contains("Passenger") == true -> 12
            typeName?.contains("يخت") == true || typeName?.contains("Yacht") == true -> 13
            typeName?.contains("صهريج") == true || typeName?.contains("Tanker") == true -> 14
            else -> 13 // Default
        }
    }

    /**
     * Extract proof type ID from selected proof type name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractProofTypeId(proofTypeName: String?): Int {
        // For now, return a mock ID
        return when {
            proofTypeName?.contains("ملكية") == true || proofTypeName?.contains("Ownership") == true -> 1
            proofTypeName?.contains("بيع") == true || proofTypeName?.contains("Sale") == true -> 2
            proofTypeName?.contains("تسجيل") == true || proofTypeName?.contains("Registration") == true -> 3
            else -> 1 // Default
        }
    }

    /**
     * Extract country ID (ISO code) from country name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractCountryId(countryName: String?): String {
        // For now, return a mock ISO code
        return when {
            countryName?.contains("عُمان") == true || countryName?.contains("Oman") == true -> "OM"
            countryName?.contains("الإمارات") == true || countryName?.contains("UAE") == true -> "AE"
            countryName?.contains("السعودية") == true || countryName?.contains("Saudi") == true -> "SA"
            countryName?.contains("الكويت") == true || countryName?.contains("Kuwait") == true -> "KW"
            countryName?.contains("البحرين") == true || countryName?.contains("Bahrain") == true -> "BH"
            countryName?.contains("قطر") == true || countryName?.contains("Qatar") == true -> "QA"
            else -> "OM" // Default
        }
    }

    /**
     * Extract building material ID from material name
     * TODO: Replace with actual lookup mapping when available
     */
    private fun extractBuildMaterialId(materialName: String?): Int {
        // For now, return a mock ID
        return when {
            materialName?.contains("خشب") == true || materialName?.contains("Wood") == true -> 20
            materialName?.contains("معدن") == true || materialName?.contains("Metal") == true -> 21
            materialName?.contains("ألياف") == true || materialName?.contains("Fiber") == true -> 22
            materialName?.contains("بلاستيك") == true || materialName?.contains("Plastic") == true -> 23
            else -> 21 // Default (Metal)
        }
    }
}
