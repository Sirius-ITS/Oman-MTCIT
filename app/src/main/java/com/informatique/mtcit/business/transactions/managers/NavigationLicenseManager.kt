package com.informatique.mtcit.business.transactions.managers

import com.informatique.mtcit.data.api.NavigationRequestResDto
import com.informatique.mtcit.data.dto.CrewReqDto
import com.informatique.mtcit.data.dto.CrewResDto
import com.informatique.mtcit.data.dto.CountryReqDto
import com.informatique.mtcit.data.dto.NavigationAreaResDto
import com.informatique.mtcit.data.repository.NavigationLicenseRepository
import io.ktor.http.content.PartData
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
        crewData: List<Map<String, String>>
    ): Result<List<CrewResDto>> {
        println("👥 NavigationLicenseManager: Adding crew bulk (Issue) - count=${crewData.size}")

        val crewList = crewData.map { crew ->
            CrewReqDto(
                nameAr = crew["nameAr"] ?: "",
                nameEn = crew["nameEn"] ?: "",
                jobTitle = crew["jobTitle"]?.toIntOrNull() ?: 0,
                civilNo = crew["civilNo"],
                seamenBookNo = crew["seamenBookNo"] ?: "",
                nationality = crew["nationality"]?.toIntOrNull()?.let { CountryReqDto(it) }
            )
        }

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
                nameAr = crew["nameAr"] ?: "",
                nameEn = crew["nameEn"] ?: "",
                jobTitle = crew["jobTitle"]?.toIntOrNull() ?: 0,
                civilNo = crew["civilNo"],
                seamenBookNo = crew["seamenBookNo"] ?: "",
                nationality = crew["nationality"]?.toIntOrNull()?.let { CountryReqDto(it) }
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
            nameAr = crewData["nameAr"] ?: "",
            nameEn = crewData["nameEn"] ?: "",
            jobTitle = crewData["jobTitle"]?.toIntOrNull() ?: 0,
            civilNo = crewData["civilNo"],
            seamenBookNo = crewData["seamenBookNo"] ?: "",
            nationality = crewData["nationality"]?.toIntOrNull()?.let { CountryReqDto(it) }
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
            nameAr = crewData["nameAr"] ?: "",
            nameEn = crewData["nameEn"] ?: "",
            jobTitle = crewData["jobTitle"]?.toIntOrNull() ?: 0,
            civilNo = crewData["civilNo"],
            seamenBookNo = crewData["seamenBookNo"] ?: "",
            nationality = crewData["nationality"]?.toIntOrNull()?.let { CountryReqDto(it) }
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
     */
    fun parseCrewFromFormData(formData: Map<String, String>): List<Map<String, String>> {
        // TODO: Implement parsing logic based on your form structure
        // This will extract crew data from form fields
        return emptyList()
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

