package com.informatique.mtcit.data.repository

import android.content.Context
import com.informatique.mtcit.data.api.RegistrationApiService
import com.informatique.mtcit.data.model.CreateRegistrationRequest
import com.informatique.mtcit.data.model.CreateRegistrationResponse
import com.informatique.mtcit.data.model.EngineFileUpload
import com.informatique.mtcit.data.model.EngineSubmissionRequest
import com.informatique.mtcit.data.model.EngineSubmissionResponse
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.data.model.OwnerSubmissionRequest
import com.informatique.mtcit.data.model.OwnerSubmissionResponse
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.UpdateWeightsRequest
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
     * Validate build status documents (NEW - multipart with files)
     * POST api/v1/registration-requests/{requestId}/validate-build-status
     */
    suspend fun validateBuildStatus(
        requestId: Int,
        shipbuildingCertificateFile: ByteArray?,
        shipbuildingCertificateName: String?,
        inspectionDocumentsFile: ByteArray?,
        inspectionDocumentsName: String?
    ): Result<com.informatique.mtcit.data.model.DocumentValidationResponse>

    /**
     * Send registration request and check if inspection is needed
     * POST api/v1/registration-requests/{request-id}/send-request
     */
    suspend fun sendRequest(requestId: Int): Result<com.informatique.mtcit.data.model.SendRequestResponse>

    /**
     * Reserve ship/marine name
     * POST api/v1/registration-requests/{id}/{name}/shipNameReservtion
     */
    suspend fun shipNameReservation(requestId: Int, marineName: String): Result<Unit>

    // Create navigation license request for a selected ship (returns API response wrapper)
    suspend fun createNavigationLicense(shipInfoId: Int): Result<com.informatique.mtcit.data.model.CreateNavigationResponse>
}

@Singleton
class ShipRegistrationRepositoryImpl @Inject constructor(
    private val registrationApiService: RegistrationApiService
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
}
