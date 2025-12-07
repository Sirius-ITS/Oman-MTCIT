package com.informatique.mtcit.business.transactions.mapper

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.data.repository.LookupRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper to convert form data to API request models
 * ✅ REFACTORED: Now uses LookupRepository for dynamic ID lookups instead of static mappings
 */
@Singleton
class RegistrationRequestMapper @Inject constructor(
    private val lookupRepository: LookupRepository
) {

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
        requestId: Int? = null
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
            officialNumber = formData["imoNumber"] ?: formData["officialNumber"],
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
                id = requestId,
                shipInfo = ShipInfo(
                    ship = ship,
                    isCurrent = 0
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
            val date = LocalDate.parse(dateString, inputDateFormatter)
            date.format(outputDateFormatter)
        } catch (e: Exception) {
            println("⚠️ Failed to parse date: $dateString - ${e.message}")
            dateString
        }
    }

    /**
     * ✅ REFACTORED: Extract port ID from selected port name using LookupRepository
     */
    private fun extractPortId(portName: String?): String {
        if (portName.isNullOrBlank()) {
            println("⚠️ Port name is null or blank, using default")
            return "OMSOH" // Default fallback
        }

        val portId = lookupRepository.getPortId(portName)
        if (portId == null) {
            println("⚠️ Could not find port ID for: $portName, using default")
            return "OMSOH" // Default fallback
        }

        println("✅ Mapped port '$portName' to ID: $portId")
        return portId
    }

    /**
     * ✅ REFACTORED: Extract marine activity ID from selected activity name using LookupRepository
     */
    private fun extractMarineActivityId(activityName: String?): Int {
        if (activityName.isNullOrBlank()) {
            println("⚠️ Marine activity name is null or blank, using default")
            return 1 // Default fallback
        }

        val activityId = lookupRepository.getMarineActivityId(activityName)
        if (activityId == null) {
            println("⚠️ Could not find marine activity ID for: $activityName, using default")
            return 1 // Default fallback
        }

        println("✅ Mapped marine activity '$activityName' to ID: $activityId")
        return activityId
    }

    /**
     * ✅ REFACTORED: Extract ship category ID from selected category name using LookupRepository
     */
    private fun extractShipCategoryId(categoryName: String?): Int {
        if (categoryName.isNullOrBlank()) {
            println("⚠️ Ship category name is null or blank, using default")
            return 3 // Default fallback
        }

        val categoryId = lookupRepository.getShipCategoryId(categoryName)
        if (categoryId == null) {
            println("⚠️ Could not find ship category ID for: $categoryName, using default")
            return 3 // Default fallback
        }

        println("✅ Mapped ship category '$categoryName' to ID: $categoryId")
        return categoryId
    }

    /**
     * ✅ REFACTORED: Extract ship type ID from selected type name using LookupRepository
     */
    private fun extractShipTypeId(typeName: String?): Int {
        if (typeName.isNullOrBlank()) {
            println("⚠️ Ship type name is null or blank, using default")
            return 13 // Default fallback
        }

        val typeId = lookupRepository.getShipTypeId(typeName)
        if (typeId == null) {
            println("⚠️ Could not find ship type ID for: $typeName, using default")
            return 13 // Default fallback
        }

        println("✅ Mapped ship type '$typeName' to ID: $typeId")
        return typeId
    }

    /**
     * ✅ REFACTORED: Extract proof type ID from selected proof type name using LookupRepository
     */
    private fun extractProofTypeId(proofTypeName: String?): Int {
        if (proofTypeName.isNullOrBlank()) {
            println("⚠️ Proof type name is null or blank, using default")
            return 1 // Default fallback
        }

        val proofTypeId = lookupRepository.getProofTypeId(proofTypeName)
        if (proofTypeId == null) {
            println("⚠️ Could not find proof type ID for: $proofTypeName, using default")
            return 1 // Default fallback
        }

        println("✅ Mapped proof type '$proofTypeName' to ID: $proofTypeId")
        return proofTypeId
    }

    /**
     * ✅ REFACTORED: Extract country ID (ISO code) from country name using LookupRepository
     */
    private fun extractCountryId(countryName: String?): String {
        if (countryName.isNullOrBlank()) {
            println("⚠️ Country name is null or blank, using default")
            return "OM" // Default fallback
        }

        val countryId = lookupRepository.getCountryId(countryName)
        if (countryId == null) {
            println("⚠️ Could not find country ID for: $countryName, using default")
            return "OM" // Default fallback
        }

        println("✅ Mapped country '$countryName' to ID: $countryId")
        return countryId
    }

    /**
     * ✅ REFACTORED: Extract building material ID from material name using LookupRepository
     */
    private fun extractBuildMaterialId(materialName: String?): Int {
        if (materialName.isNullOrBlank()) {
            println("⚠️ Build material name is null or blank, using default")
            return 21 // Default fallback (Metal)
        }

        val materialId = lookupRepository.getBuildMaterialId(materialName)
        if (materialId == null) {
            println("⚠️ Could not find build material ID for: $materialName, using default")
            return 21 // Default fallback (Metal)
        }

        println("✅ Mapped build material '$materialName' to ID: $materialId")
        return materialId
    }
}
