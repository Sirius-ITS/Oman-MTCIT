package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.data.model.category.SubCategory
import com.informatique.mtcit.data.model.category.TransactionDetail
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriesApiService @Inject constructor(
    val repo: AppRepository,
    private val json: Json
)  {

    suspend fun getSubCategories(): Result<List<SubCategory>> {
        return try {
            when (val response = repo.onGet("metadata-service-categories")) {
                is RepoServiceState.Success -> {
                    val response = response.response
                    if (!response.jsonObject.isEmpty()) {
                        if (response.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && response.jsonObject.getValue("success").jsonPrimitive.boolean){

                            val data = response.jsonObject.getValue("data").jsonArray

                            val subCategories: List<SubCategory> =
                                json.decodeFromJsonElement(data)

                            Result.success(subCategories)

                        } else {
                            Result.failure(Exception("Service is failed"))
                        }
                    }else{
                        Result.failure(Exception("Empty sub categories"))
                    }
                }

                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(Exception(errorMessage))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get sub categories: ${e.message}"))
        }
    }

    suspend fun getTransactionInfo(serviceId: Int): Result<TransactionDetail> {
        return try {
            when (val response = repo.onGet("metadata-service-categories/services/${serviceId}")) {
                is RepoServiceState.Success -> {
                    val response = response.response
                    if (!response.jsonObject.isEmpty()) {
                        if (response.jsonObject.getValue("statusCode").jsonPrimitive.int == 200
                            && response.jsonObject.getValue("success").jsonPrimitive.boolean){

                            val data = response.jsonObject.getValue("data").jsonObject

                            val transactionDetail: TransactionDetail =
                                json.decodeFromJsonElement(data)

                            Result.success(transactionDetail)

                        } else {
                            Result.failure(Exception("Service is failed"))
                        }
                    }else{
                        Result.failure(Exception("Empty transaction detail"))
                    }
                }

                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(Exception(errorMessage))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get transaction detail: ${e.message}"))
        }
    }

}