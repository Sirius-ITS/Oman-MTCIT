package com.informatique.mtcit.business.home

import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.api.CategoriesApiService
import com.informatique.mtcit.data.model.category.SubCategory
import com.informatique.mtcit.data.model.category.TransactionDetail
import javax.inject.Inject
import javax.inject.Provider

/**
 * Factory for creating home strategies
 * Uses Hilt's Provider to lazily create strategies only when needed
 */
class MainCategoriesStrategyFactory @Inject constructor(
    private val categoriesStrategy: Provider<MainCategoriesStrategy>,
    val apiService: CategoriesApiService
) {

    /**
     * Create a strategy instance for the given home feature
     */
    fun createCategoriesStrategy(): MainCategoriesStrategyInterface {
        return categoriesStrategy.get()
    }

    suspend fun fetchSubCategories(): Result<List<SubCategory>> {
        return try {
            apiService.getSubCategories()
        } catch (e: ApiException) {
            // ✅ Re-throw ApiException to preserve status code
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTransactionDetail(serviceId: Int): Result<TransactionDetail> {
        return try {
            apiService.getTransactionInfo(serviceId = serviceId)
        } catch (e: ApiException) {
            // ✅ Re-throw ApiException to preserve status code
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
