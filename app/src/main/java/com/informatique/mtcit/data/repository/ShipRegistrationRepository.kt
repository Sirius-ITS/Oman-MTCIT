package com.informatique.mtcit.data.repository

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
}

@Singleton
class ShipRegistrationRepositoryImpl @Inject constructor(
    // Inject API service and local storage when ready
    // private val apiService: ShipRegistrationApiService,
    // private val draftDao: DraftDao
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
}

