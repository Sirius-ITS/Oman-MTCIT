package com.informatique.mtcit.business.home

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
            val result = apiService.getSubCategories()
            result.fold(
                onSuccess = { subCategories ->
                    Result.success(subCategories)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTransactionDetail(serviceId: Int): Result<TransactionDetail> {
        return try {
            val result = apiService.getTransactionInfo(serviceId = serviceId)
            result.fold(
                onSuccess = { transactionDetail ->
                    Result.success(transactionDetail)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

