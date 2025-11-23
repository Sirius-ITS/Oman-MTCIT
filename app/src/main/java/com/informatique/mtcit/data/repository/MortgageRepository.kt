package com.informatique.mtcit.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the mortgage status of a marine unit
 */
data class MortgageStatus(
    val isMortgaged: Boolean,
    val mortgageId: String? = null,
    val bankName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isApprovedBank: Boolean = true,
    val mortgageAmount: String? = null
)

/**
 * Repository for mortgage-related data operations
 * All methods will eventually call backend APIs
 */
interface MortgageRepository {
    /**
     * Get mortgage status for a marine unit from backend
     */
    suspend fun getMortgageStatus(unitId: String): MortgageStatus

    /**
     * Check if a bank is in the approved list
     */
    suspend fun isApprovedBank(bankId: String): Boolean
}

@Singleton
class MortgageRepositoryImpl @Inject constructor(
    // TODO: Inject API service when available
    // private val apiService: MortgageApiService
) : MortgageRepository {

    override suspend fun getMortgageStatus(unitId: String): MortgageStatus {
        // TODO: Replace with actual API call
        // Example: return apiService.getMortgageStatus(unitId)

        // Mock data for demonstration - Unit "3" is mortgaged
        return when (unitId) {
            "3" -> MortgageStatus(
                isMortgaged = true,
                mortgageId = "MTG-2024-001",
                bankName = "بنك مسقط",
                startDate = "2024-01-15",
                endDate = "2029-01-15",
                isApprovedBank = true,
                mortgageAmount = "50000 ر.ع"
            )
            else -> MortgageStatus(isMortgaged = false)
        }
    }

    override suspend fun isApprovedBank(bankId: String): Boolean {
        // TODO: Replace with actual API call
        return true // Mock: all banks approved
    }
}

