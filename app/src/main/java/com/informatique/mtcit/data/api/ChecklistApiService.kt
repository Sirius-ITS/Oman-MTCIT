package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.ChecklistSettingsResponse
import com.informatique.mtcit.data.model.InspectionDecisionResponse
import com.informatique.mtcit.data.model.WorkOrderResultRequest
import com.informatique.mtcit.data.model.WorkOrderResultResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for Checklist Settings
 * Handles checklist configuration for engineer inspections
 */
@Singleton
class ChecklistApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Get checklist settings by purpose ID
     * GET /api/v1/checklist-settings/by-purpose/{purposeId}
     *
     * @param purposeId The inspection purpose ID
     * @return Result with ChecklistSettingsResponse
     */
    suspend fun getChecklistByPurpose(
        purposeId: Int
    ): Result<ChecklistSettingsResponse> {
        return try {
            println("=".repeat(80))
            println("🔍 ChecklistApiService: Getting checklist for purposeId=$purposeId")
            println("=".repeat(80))

            val endpoint = "checklist-settings/by-purpose/$purposeId"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Checklist API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val checklistResponse: ChecklistSettingsResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Parsed ${checklistResponse.data.items.size} checklist items")
                            println("   Checklist: ${checklistResponse.data.nameEn}")
                            println("   Purpose: ${checklistResponse.data.purpose.nameEn}")
                            println("=".repeat(80))

                            Result.success(checklistResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "فشل في جلب قائمة الفحص"
                            println("❌ API returned error: $message")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("   HTTP Code: ${response.code}")
                    println("=".repeat(80))
                    Result.failure(Exception("Failed to get checklist: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getChecklistByPurpose: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to get checklist: ${e.message}"))
        }
    }

    /**
     * ✅ NEW: Get inspection decisions dropdown
     * GET /api/v1/work-order-results/decisions-ddl
     *
     * @return Result with InspectionDecisionResponse
     */
    suspend fun getInspectionDecisions(): Result<InspectionDecisionResponse> {
        return try {
            println("=".repeat(80))
            println("🔍 ChecklistApiService: Getting inspection decisions")
            println("=".repeat(80))

            val endpoint = "work-order-results/decisions-ddl"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Inspection Decisions API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val decisionsResponse: InspectionDecisionResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Parsed ${decisionsResponse.data.size} decision options")
                            decisionsResponse.data.forEach { decision ->
                                println("   - ${decision.id}: ${decision.nameEn}")
                            }
                            println("=".repeat(80))

                            Result.success(decisionsResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "فشل في جلب قرارات الفحص"
                            println("❌ API returned error: $message")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("   HTTP Code: ${response.code}")
                    println("=".repeat(80))
                    Result.failure(Exception("Failed to get decisions: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getInspectionDecisions: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to get decisions: ${e.message}"))
        }
    }

    /**
     * ✅ NEW: Submit work order result
     * POST /api/v1/work-order-results
     *
     * @param request The work order result request
     * @return Result with WorkOrderResultResponse
     */
    suspend fun submitWorkOrderResult(request: WorkOrderResultRequest): Result<WorkOrderResultResponse> {
        return try {
            println("=".repeat(80))
            println("🔍 ChecklistApiService: Submitting work order result")
            println("=".repeat(80))

            val endpoint = "work-order-results"
            println("📡 API Call: POST $endpoint")
            println("📤 Request:")
            println("   Decision ID: ${request.decisionId}")
            println("   Scheduled Request ID: ${request.scheduledRequestId}")
            println("   Expired Date: ${request.expiredDate ?: "N/A"}")
            println("   Answers: ${request.answers.size} items")

            val requestJson = json.encodeToString(request)
            println("📤 JSON Request: $requestJson")

            when (val response = repo.onPostAuth(endpoint, requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Work Order Result API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val resultResponse: WorkOrderResultResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Work order result submitted successfully")
                            println("   Message: ${resultResponse.message}")
                            println("=".repeat(80))

                            Result.success(resultResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "فشل في حفظ نتيجة الفحص"
                            println("❌ API returned error: $message")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("   HTTP Code: ${response.code}")
                    println("=".repeat(80))
                    Result.failure(Exception("Failed to submit work order result: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in submitWorkOrderResult: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to submit work order result: ${e.message}"))
        }
    }
}
