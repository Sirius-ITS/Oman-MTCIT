package com.informatique.mtcit.data.repository

import android.content.Context
import com.informatique.mtcit.data.api.RegistrationApiService
import com.informatique.mtcit.data.model.CreateRegistrationRequest
import com.informatique.mtcit.data.model.CreateRegistrationResponse
import com.informatique.mtcit.data.model.EngineFileUpload
import com.informatique.mtcit.data.model.EngineResponseData
import com.informatique.mtcit.data.model.EngineSubmissionRequest
import com.informatique.mtcit.data.model.EngineSubmissionResponse
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.data.model.OwnerResponseData
import com.informatique.mtcit.data.model.OwnerSubmissionRequest
import com.informatique.mtcit.data.model.OwnerSubmissionResponse
import com.informatique.mtcit.data.model.OwnersListResponse
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.cancelRegistration.DeletionFileUpload
import com.informatique.mtcit.data.model.UpdateWeightsRequest
import com.informatique.mtcit.data.model.cancelRegistration.DeletionReasonResponse
import com.informatique.mtcit.data.model.cancelRegistration.DeletionSubmitResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for ship registration operations
 */
interface ShipRegistrationRepository {
    suspend fun submitRegistration(formData: Map<String, String>): Result<Boolean>
    suspend fun saveDraft(formData: Map<String, String>): Result<Unit>
    suspend fun loadDraft(): Result<Map<String, String>?>
    suspend fun deleteDraft(): Result<Unit>
    suspend fun getDeletionReasons(): Result<DeletionReasonResponse>
    suspend fun submitDeletionRequest(deletionReasonId: Int, shipInfoId: Int, files: List<DeletionFileUpload>): Result<DeletionSubmitResponse>

    /**
     * Create a new registration request and get requestId
     * This is the first API call after collecting ship info
     */
    suspend fun createRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse>

    /**
     * Update an existing registration request
     * Used when user goes back and changes unit selection data
     */
    suspend fun updateRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse>

    /**
     * Update ship dimensions
     */
    suspend fun updateDimensions(requestId: String, dimensionsData: UpdateDimensionsRequest): Result<Unit>

    /**
     * Update ship weights
     */
    suspend fun updateWeights(requestId: String, weightsData: UpdateWeightsRequest): Result<Unit>

    /**
     * Update ship engines (OLD - JSON only)
     */
    suspend fun updateEngines(requestId: String, enginesJson: String): Result<Unit>

    /**
     * Submit engines with documents (NEW - multipart with files)
     */
    suspend fun submitEngines(
        context: Context,
        requestId: Int,
        engines: List<EngineSubmissionRequest>,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse>

    /**
     * Update ship owners
     */
    suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit>

    /**
     * Submit owners with documents (NEW - multipart with files)
     */
    suspend fun submitOwners(
        context: Context,
        requestId: Int,
        owners: List<OwnerSubmissionRequest>,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse>

    /**
     * Update a single engine
     * PUT /api/v1/registration-requests/{requestId}/engines/{engineId}
     */
    suspend fun updateEngine(
        context: Context,
        requestId: Int,
        engineId: Int,
        engine: EngineSubmissionRequest,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse>

    /**
     * Delete a single engine
     * DELETE /api/v1/registration-requests/{requestId}/engines/{engineId}
     */
    suspend fun deleteEngine(requestId: Int, engineId: Int): Result<Unit>

    /**
     * Update a single owner
     * PUT /api/v1/registration-requests/{requestId}/owners/{ownerId}
     */
    suspend fun updateOwner(
        context: Context,
        requestId: Int,
        ownerId: Int,
        owner: OwnerSubmissionRequest,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse>

    /**
     * Delete a single owner
     * DELETE /api/v1/registration-requests/{requestId}/owners/{ownerId}
     */
    suspend fun deleteOwner(requestId: Int, ownerId: Int): Result<Unit>

    /**
     * Validate build status documents (old version - kept for backward compatibility)
     * POST registration-requests/{requestId}/validate-build-status
     */
    suspend fun validateBuildStatus(
        requestId: Int,
        shipbuildingCertificateFile: ByteArray?,
        shipbuildingCertificateName: String?,
        inspectionDocumentsFile: ByteArray?,
        inspectionDocumentsName: String?
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse>

    /**
     * Validate build status with dynamic documents
     * POST registration-requests/{requestId}/validate-build-status
     */
    suspend fun validateBuildStatusWithDocuments(
        requestId: Int,
        documents: List<com.informatique.mtcit.data.model.DocumentFileUpload>
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse>

    /**
     * Validate build status with dynamic documents for PERMANENT registration
     * POST perm-registration-requests/{requestId}/validate-build-status
     */
    suspend fun validatePermanentBuildStatusWithDocuments(
        requestId: Int,
        documents: List<com.informatique.mtcit.data.model.DocumentFileUpload>
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse>

    /**
     * Send registration request and check if inspection is needed
     * POST registration-requests/{request-id}/send-request
     */
    suspend fun sendRequest(requestId: Int): Result<com.informatique.mtcit.data.model.SendRequestResponse>

    /**
     * Reserve ship/marine name
     * POST registration-requests/{id}/{name}/shipNameReservtion
     */
    suspend fun shipNameReservation(requestId: Int, marineName: String): Result<Unit>

    // Create navigation license request for a selected ship (returns API response wrapper)
    suspend fun createNavigationLicense(shipInfoId: Int): Result<com.informatique.mtcit.data.model.CreateNavigationResponse>

    /**
     * Add maritime identification (IMO, MMSI, Call Sign) to a ship
     * PATCH /api/v1/perm-registration-requests/{shipId}/add-ship-identity
     */
    suspend fun addMaritimeIdentity(
        shipId: Int,
        imoNumber: String?,
        mmsiNumber: String?,
        callSign: String?
    ): Result<com.informatique.mtcit.data.model.MaritimeIdentityResponse>

    /**
     * Get file preview URL by reference number
     * GET /api/v1/registration-request-view/file-preview?refNo={refNo}
     */
    suspend fun getFilePreview(refNo: String): Result<String>
}

@Singleton
class ShipRegistrationRepositoryImpl @Inject constructor(
    private val registrationApiService: RegistrationApiService,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService
) : ShipRegistrationRepository {

    override suspend fun submitRegistration(formData: Map<String, String>): Result<Boolean> {
        return runCatching {
            // TODO: Call actual API
            // val response = apiService.submitRegistration(formData)
            // response.isSuccessful

            // Simulate network delay
            kotlinx.coroutines.delay(2000)
            true
        }
    }

    override suspend fun saveDraft(formData: Map<String, String>): Result<Unit> {
        return runCatching {
            // TODO: Save to local database
            // draftDao.saveDraft(DraftEntity(formData))
        }
    }

    override suspend fun loadDraft(): Result<Map<String, String>?> {
        return runCatching {
            // TODO: Load from local database
            // draftDao.getDraft()?.formData
            null
        }
    }

    override suspend fun deleteDraft(): Result<Unit> {
        return runCatching {
            // TODO: Delete from local database
            // draftDao.deleteDraft()
        }
    }

    override suspend fun getDeletionReasons(): Result<DeletionReasonResponse> {
        return registrationApiService.getDeletionReasons()
    }

    override suspend fun submitDeletionRequest(deletionReasonId: Int, shipInfoId: Int, files: List<DeletionFileUpload>): Result<DeletionSubmitResponse> {
        return registrationApiService.submitDeletionRequest(deletionReasonId, shipInfoId, files)
    }

    override suspend fun createRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        println("📞 ShipRegistrationRepository: Calling API service...")
        return registrationApiService.createRegistrationRequest(request)
    }

    override suspend fun updateRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        println("📞 ShipRegistrationRepository: Calling updateRegistrationRequest API...")
        return registrationApiService.updateRegistrationRequest(request)
    }

    override suspend fun updateDimensions(requestId: String, dimensionsData: UpdateDimensionsRequest): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling updateDimensions API...")
        return registrationApiService.updateDimensions(requestId, dimensionsData)
    }

    override suspend fun updateWeights(requestId: String, weightsData: UpdateWeightsRequest): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling updateWeights API...")
        return registrationApiService.updateWeights(requestId, weightsData)
    }

    override suspend fun updateEngines(requestId: String, enginesJson: String): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling updateEngines API...")
        return registrationApiService.updateEngines(requestId, enginesJson)
    }

    override suspend fun submitEngines(
        context: Context,
        requestId: Int,
        engines: List<EngineSubmissionRequest>,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse> {
        println("📞 ShipRegistrationRepository: Calling submitEngines API...")
        return registrationApiService.submitEngines(requestId, engines, files)
    }

    override suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling updateOwners API...")
        return registrationApiService.updateOwners(requestId, ownersJson)
    }

    override suspend fun submitOwners(
        context: Context,
        requestId: Int,
        owners: List<OwnerSubmissionRequest>,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse> {
        println("📞 ShipRegistrationRepository: Calling submitOwners API...")
        return registrationApiService.submitOwners(requestId, owners, files)
    }

    override suspend fun updateEngine(
        context: Context,
        requestId: Int,
        engineId: Int,
        engine: EngineSubmissionRequest,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse> {
        println("📞 ShipRegistrationRepository: Calling updateEngine API...")
        return registrationApiService.updateEngine(requestId, engineId, engine, files)
    }

    override suspend fun deleteEngine(requestId: Int, engineId: Int): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling deleteEngine API...")
        return registrationApiService.deleteEngine(requestId, engineId)
    }

    override suspend fun updateOwner(
        context: Context,
        requestId: Int,
        ownerId: Int,
        owner: OwnerSubmissionRequest,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse> {
        println("📞 ShipRegistrationRepository: Calling updateOwner API...")
        return registrationApiService.updateOwner(requestId, ownerId, owner, files)
    }

    override suspend fun deleteOwner(requestId: Int, ownerId: Int): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling deleteOwner API...")
        return registrationApiService.deleteOwner(requestId, ownerId)
    }


    override suspend fun validateBuildStatus(
        requestId: Int,
        shipbuildingCertificateFile: ByteArray?,
        shipbuildingCertificateName: String?,
        inspectionDocumentsFile: ByteArray?,
        inspectionDocumentsName: String?
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse> {
        println("📞 ShipRegistrationRepository: Calling validateBuildStatus API...")
        return registrationApiService.validateBuildStatus(
            requestId,
            shipbuildingCertificateFile,
            shipbuildingCertificateName,
            inspectionDocumentsFile,
            inspectionDocumentsName
        )
    }

    override suspend fun validateBuildStatusWithDocuments(
        requestId: Int,
        documents: List<com.informatique.mtcit.data.model.DocumentFileUpload>
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse> {
        println("📞 ShipRegistrationRepository: Calling validateBuildStatusWithDocuments API...")
        return registrationApiService.validateBuildStatusWithDocuments(requestId, documents)
    }

    override suspend fun validatePermanentBuildStatusWithDocuments(
        requestId: Int,
        documents: List<com.informatique.mtcit.data.model.DocumentFileUpload>
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse> {
        println("📞 ShipRegistrationRepository: Calling validatePermanentBuildStatusWithDocuments API...")
        return registrationApiService.validatePermanentBuildStatusWithDocuments(requestId, documents)
    }

    override suspend fun sendRequest(requestId: Int): Result<com.informatique.mtcit.data.model.SendRequestResponse> {
        println("📞 ShipRegistrationRepository: Calling sendRequest API...")
        return registrationApiService.sendRequest(requestId)
    }

    override suspend fun shipNameReservation(requestId: Int, marineName: String): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling shipNameReservation API...")
        return registrationApiService.shipNameReservation(requestId, marineName)
    }

    override suspend fun createNavigationLicense(shipInfoId: Int): Result<com.informatique.mtcit.data.model.CreateNavigationResponse> {
        println("📞 ShipRegistrationRepository: Creating navigation license for shipInfoId=$shipInfoId")
        return registrationApiService.createNavigationLicense(shipInfoId)
    }

    override suspend fun addMaritimeIdentity(
        shipId: Int,
        imoNumber: String?,
        mmsiNumber: String?,
        callSign: String?
    ): Result<com.informatique.mtcit.data.model.MaritimeIdentityResponse> {
        println("📞 ShipRegistrationRepository: Adding maritime identity for shipId=$shipId")
        return marineUnitsApiService.addMaritimeIdentity(shipId, imoNumber, mmsiNumber, callSign)
    }

    override suspend fun getFilePreview(refNo: String): Result<String> {
        println("📞 ShipRegistrationRepository: Getting file preview for refNo=$refNo")
        return registrationApiService.getFilePreview(refNo)
    }
}
