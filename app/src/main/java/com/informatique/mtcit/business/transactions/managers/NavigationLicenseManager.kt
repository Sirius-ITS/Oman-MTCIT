package com.informatique.mtcit.business.transactions.managers

import com.informatique.mtcit.data.api.NavigationRequestResDto
import com.informatique.mtcit.data.dto.CrewReqDto
import com.informatique.mtcit.data.dto.CrewResDto
import com.informatique.mtcit.data.dto.CountryReqDto
import com.informatique.mtcit.data.dto.NavigationAreaResDto
import com.informatique.mtcit.data.repository.NavigationLicenseRepository
import io.ktor.http.content.PartData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic manager for Navigation License operations
 * Used by both IssueNavigationPermitStrategy and RenewNavigationPermitStrategy
 *
 * Handles:
 * - Creating requests (Issue/Renew)
 * - Managing navigation areas (add/update/load)
 * - Managing crew (add/update/delete/load/Excel upload)
 * - Conditional logic between Issue vs Renew
 */
@Singleton
class NavigationLicenseManager @Inject constructor(
    private val repository: NavigationLicenseRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ========================================
    // REQUEST CREATION
    // ========================================

    /**
     * Create a new issue request
     * @param shipInfoId Ship information ID
     * @return Request ID if successful
     */
    suspend fun createIssueRequest(shipInfoId: Long): Result<Long> {
        println("🚢 NavigationLicenseManager: Creating issue request for shipInfoId=$shipInfoId")
        return repository.createIssueRequest(shipInfoId).map { it.id }
    }

    /**
     * Create a new renewal request
     * @param shipInfoId Ship information ID
     * @param lastNavLicId Last issued navigation license ID
     * @return Request ID and lastNavLicId if successful
     */
    suspend fun createRenewalRequest(
        shipInfoId: Long,
        lastNavLicId: Long
    ): Result<Pair<Long, Long>> {
        println("🔄 NavigationLicenseManager: Creating renewal request for shipInfoId=$shipInfoId, lastNavLicId=$lastNavLicId")
        return repository.createRenewalRequest(shipInfoId, lastNavLicId).map {
            Pair(it.id, it.lastNavLicId ?: lastNavLicId)
        }
    }

    /**
     * Create a new renewal request with simple body (only shipInfo)
     * Return full NavigationRequestResDto so callers can access lastNavLicId
     */
    suspend fun createRenewalRequestSimple(shipInfoId: Long): Result<NavigationRequestResDto> {
        println("🔄 NavigationLicenseManager: Creating renewal request (simple) for shipInfoId=$shipInfoId")
        return repository.createRenewalRequestSimple(shipInfoId)
    }

    // ========================================
    // NAVIGATION AREAS MANAGEMENT
    // ========================================

    /**
     * Add navigation areas (Issue transaction)
     * @param requestId Navigation license request ID
     * @param areaIds List of navigation area IDs
     * @return Success/Failure result
     */
    suspend fun addNavigationAreasIssue(
        requestId: Long,
        areaIds: List<Int>
    ): Result<Unit> {
        println("📍 NavigationLicenseManager: Adding navigation areas (Issue)")
        return repository.addNavigationAreasIssue(requestId, areaIds).map { Unit }
    }

    /**
     * Load existing navigation areas (Renew transaction)
     * @param requestId Navigation license request ID
     * @return List of existing navigation areas
     */
    suspend fun loadNavigationAreasRenew(requestId: Long): Result<List<NavigationAreaResDto>> {
        println("📥 NavigationLicenseManager: Loading navigation areas (Renew)")
        return repository.getNavigationAreasRenew(requestId)
    }

    /**
     * Add new navigation areas (Renew transaction)
     * @param requestId Navigation license request ID
     * @param areaIds List of navigation area IDs
     * @return Success/Failure result
     */
    suspend fun addNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<Unit> {
        println("📍 NavigationLicenseManager: Adding navigation areas (Renew)")
        return repository.addNavigationAreasRenew(requestId, areaIds).map { Unit }
    }

    /**
     * Update navigation areas (Renew transaction)
     * @param requestId Navigation license request ID
     * @param areaIds Updated list of navigation area IDs
     * @return Success/Failure result
     */
    suspend fun updateNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<Unit> {
        println("✏️ NavigationLicenseManager: Updating navigation areas (Renew)")
        return repository.updateNavigationAreasRenew(requestId, areaIds).map { Unit }
    }

    // ========================================
    // CREW MANAGEMENT
    // ========================================

    /**
     * Add crew members in bulk (Issue transaction)
     * @param requestId Navigation license request ID
     * @param crewData List of crew data from form
     * @return List of created crew members
     */
    suspend fun addCrewBulkIssue(
        requestId: Long,
        crewData: List<Map<String, Any>>
    ): Result<List<CrewResDto>> {
        println("👥 NavigationLicenseManager: Adding crew bulk (Issue) - count=${crewData.size}")
        println("📋 Raw crewData received:")
        crewData.forEachIndexed { index, crew ->
            println("   Crew[$index]: $crew")
        }

        val crewList = crewData.map { crew ->
            // Extract nationality ID from nested map or string
            val nationalityId = when (val nat = crew["nationality"]) {
                is Map<*, *> -> {
                    val id = nat["id"]?.toString() ?: ""
                    println("   🌍 Nationality from Map: $nat -> ID: '$id'")
                    id
                }
                is String -> {
                    println("   🌍 Nationality from String: '$nat'")
                    nat
                }
                else -> {
                    println("   ⚠️ Nationality type unknown: ${nat?.javaClass?.simpleName}")
                    ""
                }
            }

            val jobTitleValue = crew["jobTitle"]
            val jobTitleInt = when (jobTitleValue) {
                is Int -> {
                    println("   💼 JobTitle from Int: $jobTitleValue")
                    jobTitleValue
                }
                is String -> {
                    val converted = jobTitleValue.toIntOrNull() ?: 0
                    println("   💼 JobTitle from String: '$jobTitleValue' -> $converted")
                    converted
                }
                else -> {
                    println("   ⚠️ JobTitle type unknown: ${jobTitleValue?.javaClass?.simpleName}, value: $jobTitleValue")
                    0
                }
            }

            println("   ➡️ Creating CrewReqDto:")
            println("      - nameAr: ${crew["nameAr"]}")
            println("      - nameEn: ${crew["nameEn"]}")
            println("      - jobTitle: $jobTitleInt")
            println("      - nationality ID: '$nationalityId' (type: ${nationalityId.javaClass.simpleName})")

            val dto = CrewReqDto(
                id = requestId, // Include requestId in each crew member
                nameAr = crew["nameAr"]?.toString() ?: "",
                nameEn = crew["nameEn"]?.toString() ?: "",
                jobTitle = jobTitleInt,
                civilNo = crew["civilNo"]?.toString(),
                seamenBookNo = crew["seamenBookNo"]?.toString() ?: "",
                nationality = if (nationalityId.isNotBlank()) CountryReqDto(nationalityId) else null
            )

            println("   ✅ Created DTO: jobTitle=${dto.jobTitle}, nationality.id='${dto.nationality?.id}'")
            dto
        }

        println("📤 Sending ${crewList.size} crew members to API")
        println("=" .repeat(60))
        crewList.forEachIndexed { index, crew ->
            println("Crew[$index]:")
            println("  - nameAr: ${crew.nameAr}")
            println("  - nameEn: ${crew.nameEn}")
            println("  - jobTitle: ${crew.jobTitle}")
            println("  - nationality: ${crew.nationality?.id ?: "null"}")
        }
        println("=" .repeat(60))

        return repository.addCrewBulkIssue(requestId, crewList)
    }

    /**
     * Upload crew Excel file (Issue transaction)
     * @param requestId Navigation license request ID
     * @param fileParts Multipart file data
     * @return List of imported crew members
     */
    suspend fun uploadCrewExcelIssue(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        println("📤 NavigationLicenseManager: Uploading crew Excel (Issue)")
        return repository.uploadCrewExcelIssue(requestId, fileParts)
    }

    /**
     * Load existing crew members (Issue transaction - for editing)
     * @param requestId Navigation license request ID
     * @return List of existing crew members
     */
    suspend fun loadCrewIssue(requestId: Long): Result<List<CrewResDto>> {
        println("📥 NavigationLicenseManager: Loading crew (Issue)")
        return repository.getCrewIssue(requestId)
    }

    /**
     * Load existing crew members (Renew transaction)
     * @param lastNavLicId Last issued navigation license ID
     * @return List of existing crew members
     */
    suspend fun loadCrewRenew(lastNavLicId: Long): Result<List<CrewResDto>> {
        println("📥 NavigationLicenseManager: Loading crew (Renew)")
        return repository.getCrewRenew(lastNavLicId)
    }

    /**
     * Add crew members in bulk (Renew transaction)
     * @param requestId Navigation license request ID
     * @param crewData List of crew data from form
     * @return List of created crew members
     */
    suspend fun addCrewBulkRenew(
        requestId: Long,
        crewData: List<Map<String, String>>
    ): Result<List<CrewResDto>> {
        println("👥 NavigationLicenseManager: Adding crew bulk (Renew) - count=${crewData.size}")

        val crewList = crewData.map { crew ->
            CrewReqDto(
                id = requestId, // Include requestId in each crew member
                nameAr = crew["nameAr"] ?: "",
                nameEn = crew["nameEn"] ?: "",
                jobTitle = crew["jobTitle"]?.toIntOrNull() ?: 0,
                civilNo = crew["civilNo"],
                seamenBookNo = crew["seamenBookNo"] ?: "",
                nationality = crew["nationality"]?.let { CountryReqDto(it) } // Keep as String
            )
        }

        return repository.addCrewBulkRenew(requestId, crewList)
    }

    /**
     * Update crew member (Issue transaction)
     * @param requestId Navigation license request ID
     * @param crewId Crew member ID
     * @param crewData Updated crew data
     * @return Updated crew member
     */
    suspend fun updateCrewMemberIssue(
        requestId: Long,
        crewId: Long,
        crewData: Map<String, String>
    ): Result<CrewResDto> {
        println("✏️ NavigationLicenseManager: Updating crew member (Issue)")

        val crew = CrewReqDto(
            id = requestId, // Include requestId in each crew member
            nameAr = crewData["nameAr"] ?: "",
            nameEn = crewData["nameEn"] ?: "",
            jobTitle = crewData["jobTitle"]?.toIntOrNull() ?: 0,
            civilNo = crewData["civilNo"],
            seamenBookNo = crewData["seamenBookNo"] ?: "",
            nationality = crewData["nationality"]?.let { CountryReqDto(it) } // Keep as String
        )

        return repository.updateCrewMemberIssue(requestId, crewId, crew)
    }

    /**
     * Update crew member (Renew transaction)
     * @param requestId Navigation license request ID
     * @param crewId Crew member ID
     * @param crewData Updated crew data
     * @return Updated crew member
     */
    suspend fun updateCrewMemberRenew(
        requestId: Long,
        crewId: Long,
        crewData: Map<String, String>
    ): Result<CrewResDto> {
        println("✏️ NavigationLicenseManager: Updating crew member (Renew)")

        val crew = CrewReqDto(
            id = requestId, // Include requestId
            nameAr = crewData["nameAr"] ?: "",
            nameEn = crewData["nameEn"] ?: "",
            jobTitle = crewData["jobTitle"]?.toIntOrNull() ?: 0,
            civilNo = crewData["civilNo"],
            seamenBookNo = crewData["seamenBookNo"] ?: "",
            nationality = crewData["nationality"]?.let { CountryReqDto(it) } // Keep as String
        )

        return repository.updateCrewMemberRenew(requestId, crewId, crew)
    }

    /**
     * Delete crew member (Issue transaction)
     * @param requestId Navigation license request ID
     * @param crewId Crew member ID
     * @return Success/Failure result
     */
    suspend fun deleteCrewMemberIssue(
        requestId: Long,
        crewId: Long
    ): Result<Unit> {
        println("🗑️ NavigationLicenseManager: Deleting crew member (Issue)")
        return repository.deleteCrewMemberIssue(requestId, crewId)
    }

    /**
     * Delete crew member (Renew transaction)
     * @param requestId Navigation license request ID
     * @param crewId Crew member ID
     * @return Success/Failure result
     */
    suspend fun deleteCrewMemberRenew(
        requestId: Long,
        crewId: Long
    ): Result<Unit> {
        println("🗑️ NavigationLicenseManager: Deleting crew member (Renew)")
        return repository.deleteCrewMemberRenew(requestId, crewId)
    }

    /**
     * Upload crew Excel file (Renew transaction)
     * @param requestId Navigation license request ID
     * @param fileParts Multipart file data
     * @return List of imported crew members
     */
    suspend fun uploadCrewExcelRenew(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        println("📤 NavigationLicenseManager: Uploading crew Excel (Renew)")
        return repository.uploadCrewExcelRenew(requestId, fileParts)
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Parse crew data from form fields
     * Handles both individual crew entry and list of crew
     * Maps SailorData from UI to API format:
     * - job -> jobTitle
     * - nameAr -> nameAr
     * - nameEn -> nameEn
     * - identityNumber -> civilNo
     * - seamanPassportNumber -> seamenBookNo
     * - nationality -> nationality { id: "XX" }
     */
    fun parseCrewFromFormData(formData: Map<String, String>): List<Map<String, Any>> {
        println("🔍 parseCrewFromFormData - Available keys: ${formData.keys}")

        val sailorsJson = formData["sailors"]

        if (sailorsJson == null || sailorsJson == "[]") {
            println("⚠️ No 'sailors' field found or empty array")
            return emptyList()
        }

        println("📋 sailors JSON: $sailorsJson")

        // Parse the JSON array of sailors
        return try {
            val jsonArray = json.parseToJsonElement(sailorsJson).jsonArray

            println("✅ Parsed JSON array with ${jsonArray.size} elements")

            jsonArray.mapNotNull { element ->
                try {
                    val sailor = element.jsonObject

                    // Extract fields from SailorData format
                    val jobFull = sailor["job"]?.jsonPrimitive?.content ?: ""
                    val nameAr = sailor["nameAr"]?.jsonPrimitive?.content ?: ""
                    val nameEn = sailor["nameEn"]?.jsonPrimitive?.content ?: ""
                    val identityNumber = sailor["identityNumber"]?.jsonPrimitive?.content ?: ""
                    val seamanPassportNumber = sailor["seamanPassportNumber"]?.jsonPrimitive?.content ?: ""
                    val nationalityFull = sailor["nationality"]?.jsonPrimitive?.content ?: ""

                    // ✅ Extract job ID from "ID|Name" format
                    val jobId = if (jobFull.contains("|")) {
                        jobFull.split("|").firstOrNull()?.toIntOrNull() ?: 0
                    } else {
                        jobFull.toIntOrNull() ?: 0
                    }

                    // ✅ Extract nationality ID from "ID|Name" format
                    val nationalityId = if (nationalityFull.contains("|")) {
                        nationalityFull.split("|").firstOrNull() ?: ""
                    } else {
                        nationalityFull
                    }

                    println("🔍 Raw sailor data:")
                    println("   - job: '$jobFull' -> ID: $jobId")
                    println("   - nameAr: '$nameAr'")
                    println("   - nameEn: '$nameEn'")
                    println("   - identityNumber: '$identityNumber'")
                    println("   - seamanPassportNumber: '$seamanPassportNumber'")
                    println("   - nationality: '$nationalityFull' -> ID: '$nationalityId'")

                    // Validate required fields
                    if (nameEn.isBlank() && nameAr.isBlank()) {
                        println("⚠️ Skipping sailor with empty name")
                        return@mapNotNull null
                    }

                    if (jobId == 0) {
                        println("⚠️ Skipping sailor with empty or invalid job ID")
                        return@mapNotNull null
                    }

                    // ✅ Map to API format - NO "id" field, only required fields
                    val crewMember = mutableMapOf<String, Any>(
                        "nameAr" to (nameAr.ifBlank { nameEn }),
                        "nameEn" to (nameEn.ifBlank { nameAr }),
                        "jobTitle" to jobId, // ✅ Use extracted ID
                        "civilNo" to identityNumber.ifBlank { "12345678" },
                        "seamenBookNo" to seamanPassportNumber.ifBlank { "SM-${System.currentTimeMillis() % 100000}" }
                    )

                    // ✅ Add nationality as nested object with ID only
                    if (nationalityId.isNotBlank()) {
                        crewMember["nationality"] = mapOf("id" to nationalityId)
                    }

                    println("✅ Mapped crew member: nameAr='$nameAr', nameEn='$nameEn', jobId=$jobId, nationalityId='$nationalityId'")
                    println("   Final map keys: ${crewMember.keys}")
                    println("   jobTitle type: ${crewMember["jobTitle"]?.javaClass?.simpleName}, value: ${crewMember["jobTitle"]}")
                    println("   nationality: ${crewMember["nationality"]}")
                    crewMember
                } catch (e: Exception) {
                    println("⚠️ Failed to parse sailor: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to parse sailors JSON: ${e.message}")
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            println("❌ Failed to parse sailors JSON: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Check if user chose Excel upload vs manual entry
     * @param formData Form data with user selection
     * @return true if Excel upload is selected
     */
    fun isExcelUploadSelected(formData: Map<String, String>): Boolean {
        return formData["crewInputMethod"] == "excel"
    }
}
