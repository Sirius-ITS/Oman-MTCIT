package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.RegistrationApiService
import com.informatique.mtcit.data.model.CreateRegistrationRequest
import com.informatique.mtcit.data.model.CreateRegistrationResponse
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
     * Update ship dimensions
     */
    suspend fun updateDimensions(requestId: String, dimensionsData: UpdateDimensionsRequest): Result<Unit>

    /**
     * Update ship weights
     */
    suspend fun updateWeights(requestId: String, weightsData: UpdateWeightsRequest): Result<Unit>

    /**
     * Update ship engines
     */
    suspend fun updateEngines(requestId: String, enginesJson: String): Result<Unit>

    /**
     * Update ship owners
     */
    suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit>
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

    override suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit> {
        println("📞 ShipRegistrationRepository: Calling updateOwners API...")
        return registrationApiService.updateOwners(requestId, ownersJson)
    }
}
