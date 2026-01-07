package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.NavigationLicenseApiService
import com.informatique.mtcit.data.api.NavigationRequestResDto
import com.informatique.mtcit.data.dto.CrewReqDto
import com.informatique.mtcit.data.dto.CrewResDto
import com.informatique.mtcit.data.dto.NavigationAreaResDto
import io.ktor.http.content.PartData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Navigation License operations (Issue & Renew)
 * Follows the same pattern as MarineUnitRepository
 */
interface NavigationLicenseRepository {

    // ========================================
    // ISSUE NAVIGATION LICENSE
    // ========================================

    /**
     * Create a new ship navigation license request
     */
    suspend fun createIssueRequest(shipInfoId: Long): Result<NavigationRequestResDto>

    /**
     * Add navigation areas in bulk (Issue)
     */
    suspend fun addNavigationAreasIssue(requestId: Long, areaIds: List<Int>): Result<NavigationRequestResDto>

    /**
     * Add crew members in bulk (Issue)
     */
    suspend fun addCrewBulkIssue(requestId: Long, crewList: List<CrewReqDto>): Result<List<CrewResDto>>

    /**
     * Upload crew Excel file (Issue)
     */
    suspend fun uploadCrewExcelIssue(requestId: Long, fileParts: List<PartData>): Result<List<CrewResDto>>

    /**
     * Get crew members (Issue)
     */
    suspend fun getCrewIssue(requestId: Long): Result<List<CrewResDto>>

    /**
     * Update crew member (Issue)
     */
    suspend fun updateCrewMemberIssue(requestId: Long, crewId: Long, crew: CrewReqDto): Result<CrewResDto>

    /**
     * Delete crew member (Issue)
     */
    suspend fun deleteCrewMemberIssue(requestId: Long, crewId: Long): Result<Unit>

    // ========================================
    // RENEW NAVIGATION LICENSE
    // ========================================

    /**
     * Create a new navigation license renewal request
     */
    suspend fun createRenewalRequest(shipInfoId: Long, lastNavLicId: Long): Result<NavigationRequestResDto>

    /**
     * Create a new navigation license renewal request (simple body)
     */
    suspend fun createRenewalRequestSimple(shipInfoId: Long): Result<NavigationRequestResDto>

    /**
     * Get existing navigation areas (Renew)
     */
    suspend fun getNavigationAreasRenew(requestId: Long): Result<List<NavigationAreaResDto>>

    /**
     * Add navigation areas in bulk (Renew)
     */
    suspend fun addNavigationAreasRenew(requestId: Long, areaIds: List<Int>): Result<NavigationRequestResDto>

    /**
     * Update navigation areas (Renew)
     */
    suspend fun updateNavigationAreasRenew(requestId: Long, areaIds: List<Int>): Result<NavigationRequestResDto>

    /**
     * Get existing crew members (Renew)
     */
    suspend fun getCrewRenew(lastNavLicId: Long): Result<List<CrewResDto>>

    /**
     * Add crew members in bulk (Renew)
     */
    suspend fun addCrewBulkRenew(requestId: Long, crewList: List<CrewReqDto>): Result<List<CrewResDto>>

    /**
     * Update crew member (Renew)
     */
    suspend fun updateCrewMemberRenew(requestId: Long, crewId: Long, crew: CrewReqDto): Result<CrewResDto>

    /**
     * Delete crew member (Renew)
     */
    suspend fun deleteCrewMemberRenew(requestId: Long, crewId: Long): Result<Unit>

    /**
     * Upload crew Excel file (Renew)
     */
    suspend fun uploadCrewExcelRenew(requestId: Long, fileParts: List<PartData>): Result<List<CrewResDto>>
}

@Singleton
class NavigationLicenseRepositoryImpl @Inject constructor(
    private val apiService: NavigationLicenseApiService
) : NavigationLicenseRepository {

    // ========================================
    // ISSUE NAVIGATION LICENSE
    // ========================================

    override suspend fun createIssueRequest(shipInfoId: Long): Result<NavigationRequestResDto> {
        println("🚢 Creating issue navigation license request for shipInfoId=$shipInfoId")
        return apiService.createIssueRequest(shipInfoId)
    }

    override suspend fun addNavigationAreasIssue(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        println("📍 Adding navigation areas (Issue): requestId=$requestId, areaIds=$areaIds")
        return apiService.addNavigationAreasIssue(requestId, areaIds)
    }

    override suspend fun addCrewBulkIssue(
        requestId: Long,
        crewList: List<CrewReqDto>
    ): Result<List<CrewResDto>> {
        println("👥 Adding crew bulk (Issue): requestId=$requestId, count=${crewList.size}")
        return apiService.addCrewBulkIssue(requestId, crewList)
    }

    override suspend fun uploadCrewExcelIssue(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        println("📤 Uploading crew Excel (Issue): requestId=$requestId")
        return apiService.uploadCrewExcelIssue(requestId, fileParts)
    }

    override suspend fun getCrewIssue(requestId: Long): Result<List<CrewResDto>> {
        println("📥 Getting crew list (Issue): requestId=$requestId")
        return apiService.getCrewIssue(requestId)
    }

    override suspend fun updateCrewMemberIssue(
        requestId: Long,
        crewId: Long,
        crew: CrewReqDto
    ): Result<CrewResDto> {
        println("✏️ Updating crew member (Issue): requestId=$requestId, crewId=$crewId")
        return apiService.updateCrewMemberIssue(requestId, crewId, crew)
    }

    override suspend fun deleteCrewMemberIssue(
        requestId: Long,
        crewId: Long
    ): Result<Unit> {
        println("🗑️ Deleting crew member (Issue): requestId=$requestId, crewId=$crewId")
        return apiService.deleteCrewMemberIssue(requestId, crewId)
    }

    // ========================================
    // RENEW NAVIGATION LICENSE
    // ========================================

    override suspend fun createRenewalRequest(
        shipInfoId: Long,
        lastNavLicId: Long
    ): Result<NavigationRequestResDto> {
        println("🔄 Creating renewal navigation license request: shipInfoId=$shipInfoId, lastNavLicId=$lastNavLicId")
        return apiService.createRenewalRequest(shipInfoId, lastNavLicId)
    }

    override suspend fun getNavigationAreasRenew(requestId: Long): Result<List<NavigationAreaResDto>> {
        println("📥 Getting existing navigation areas (Renew): requestId=$requestId")
        return apiService.getNavigationAreasRenew(requestId)
    }

    override suspend fun addNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        println("📍 Adding navigation areas (Renew): requestId=$requestId, areaIds=$areaIds")
        return apiService.addNavigationAreasRenew(requestId, areaIds)
    }

    override suspend fun updateNavigationAreasRenew(
        requestId: Long,
        areaIds: List<Int>
    ): Result<NavigationRequestResDto> {
        println("✏️ Updating navigation areas (Renew): requestId=$requestId, areaIds=$areaIds")
        return apiService.updateNavigationAreasRenew(requestId, areaIds)
    }

    override suspend fun getCrewRenew(lastNavLicId: Long): Result<List<CrewResDto>> {
        println("📥 Getting existing crew list (Renew): lastNavLicId=$lastNavLicId")
        return apiService.getCrewRenew(lastNavLicId)
    }

    override suspend fun addCrewBulkRenew(
        requestId: Long,
        crewList: List<CrewReqDto>
    ): Result<List<CrewResDto>> {
        println("👥 Adding crew bulk (Renew): requestId=$requestId, count=${crewList.size}")
        return apiService.addCrewBulkRenew(requestId, crewList)
    }

    override suspend fun updateCrewMemberRenew(
        requestId: Long,
        crewId: Long,
        crew: CrewReqDto
    ): Result<CrewResDto> {
        println("✏️ Updating crew member (Renew): requestId=$requestId, crewId=$crewId")
        return apiService.updateCrewMemberRenew(requestId, crewId, crew)
    }

    override suspend fun deleteCrewMemberRenew(
        requestId: Long,
        crewId: Long
    ): Result<Unit> {
        println("🗑️ Deleting crew member (Renew): requestId=$requestId, crewId=$crewId")
        return apiService.deleteCrewMemberRenew(requestId, crewId)
    }

    override suspend fun uploadCrewExcelRenew(
        requestId: Long,
        fileParts: List<PartData>
    ): Result<List<CrewResDto>> {
        println("📤 Uploading crew Excel (Renew): requestId=$requestId")
        return apiService.uploadCrewExcelRenew(requestId, fileParts)
    }

    override suspend fun createRenewalRequestSimple(
        shipInfoId: Long
    ): Result<NavigationRequestResDto> {
        println("🔄 Creating renewal navigation license request (simple): shipInfoId=$shipInfoId")
        return apiService.createRenewalRequestSimple(shipInfoId)
    }
}
