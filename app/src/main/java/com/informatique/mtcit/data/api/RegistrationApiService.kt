package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.CreateRegistrationRequest
import com.informatique.mtcit.data.model.CreateRegistrationResponse
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.UpdateWeightsRequest
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for Ship Registration Requests
 */
@Singleton
class RegistrationApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Create a new registration request
     * POST api/v1/registration-requests
     */
    suspend fun createRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        return try {
            println("🚀 RegistrationApiService: Creating registration request...")
            println("📤 Request Body: ${json.encodeToString(request)}")

            // Convert request to JSON string
            val requestBody = json.encodeToString(request)

            when (val response = repo.onPostAuth("api/v1/registration-requests", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            // Parse the full response
                            val registrationResponse: CreateRegistrationResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Registration request created successfully!")
                            println("   Request ID: ${registrationResponse.data.id}")
                            println("   Ship Info ID: ${registrationResponse.data.shipInfo?.id}")
                            println("   Ship ID: ${registrationResponse.data.shipInfo?.ship?.id}")
                            println("   Request Serial: ${registrationResponse.data.requestSerial}")
                            println("   Request Year: ${registrationResponse.data.requestYear}")
                            println("   Status: ${registrationResponse.data.status?.nameEn}")

                            Result.success(registrationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to create registration request"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createRegistrationRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to create registration request: ${e.message}"))
        }
    }

    /**
     * Update ship dimensions
     * PUT api/v1/registration-requests/{requestId}/dimensions
     */
    suspend fun updateDimensions(requestId: String, dimensionsData: UpdateDimensionsRequest): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating dimensions for requestId=$requestId...")
            println("📤 Request Body: ${json.encodeToString(dimensionsData)}")

            val requestBody = json.encodeToString(dimensionsData)

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/dimensions", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Dimensions API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Dimensions updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update dimensions"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateDimensions: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship weights
     * PUT api/v1/registration-requests/{requestId}/weights
     */
    suspend fun updateWeights(requestId: String, weightsData: UpdateWeightsRequest): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating weights for requestId=$requestId...")
            println("📤 Request Body: ${json.encodeToString(weightsData)}")

            val requestBody = json.encodeToString(weightsData)

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/weights", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Weights API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Weights updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update weights"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateWeights: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship engines
     * PUT api/v1/registration-requests/{requestId}/engines
     */
    suspend fun updateEngines(requestId: String, enginesJson: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating engines for requestId=$requestId...")
            println("📤 Request Body: $enginesJson")

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/engines", enginesJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Engines API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Engines updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update engines"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateEngines: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship owners
     * PUT api/v1/registration-requests/{requestId}/owners
     */
    suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating owners for requestId=$requestId...")
            println("📤 Request Body: $ownersJson")

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/owners", ownersJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Owners API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Owners updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update owners"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateOwners: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
