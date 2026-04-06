package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.ChecklistSettingsResponse
import com.informatique.mtcit.data.model.InspectionDecisionResponse
import com.informatique.mtcit.data.model.WorkOrderResultRequest
import com.informatique.mtcit.data.model.WorkOrderResultResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import com.informatique.mtcit.common.util.AppLanguage

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
                                ?: if (AppLanguage.isArabic) "فشل في جلب قائمة الفحص" else "Failed to fetch checklist"
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
                                ?: if (AppLanguage.isArabic) "فشل في جلب قرارات الفحص" else "Failed to fetch inspection decisions"
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
                                ?: if (AppLanguage.isArabic) "فشل في حفظ نتيجة الفحص" else "Failed to save inspection result"
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

    /**
     * ✅ NEW: Save inspection as draft (first time)
     * POST /api/v1/work-order-results
     *
     * @param request The draft save request
     * @return Result with WorkOrderResultResponse containing the created work order result ID
     */
    suspend fun saveDraft(request: com.informatique.mtcit.data.model.DraftSaveRequest): Result<WorkOrderResultResponse> {
        return try {
            println("=".repeat(80))
            println("💾 ChecklistApiService: Saving inspection as draft (POST)")
            println("=".repeat(80))

            val endpoint = "work-order-results"
            println("📡 API Call: POST $endpoint")
            println("📤 Draft Save Request:")
            println("   Scheduled Request ID: ${request.scheduledRequestId}")
            println("   Answers: ${request.answers.size} items")

            val requestJson = json.encodeToString(request)
            println("📤 JSON Request: $requestJson")

            when (val response = repo.onPostAuth(endpoint, requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Draft Save API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val resultResponse: WorkOrderResultResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Draft saved successfully")
                            println("   Message: ${resultResponse.message}")
                            println("   Work Order Result ID: ${resultResponse.data}")
                            println("=".repeat(80))

                            Result.success(resultResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: if (AppLanguage.isArabic) "فشل في حفظ المسودة" else "Failed to save draft"
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
                    Result.failure(Exception("Failed to save draft: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in saveDraft: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to save draft: ${e.message}"))
        }
    }

    /**
     * ✅ NEW: Update inspection draft (subsequent saves)
     * PUT /api/v1/work-order-results
     *
     * @param request The draft update request
     * @return Result with WorkOrderResultResponse
     */
    suspend fun updateDraft(request: com.informatique.mtcit.data.model.DraftUpdateRequest): Result<WorkOrderResultResponse> {
        return try {
            println("=".repeat(80))
            println("💾 ChecklistApiService: Updating inspection draft (PUT)")
            println("=".repeat(80))

            val endpoint = "work-order-results"
            println("📡 API Call: PUT $endpoint")
            println("📤 Draft Update Request:")
            println("   Work Order Result ID: ${request.id}")
            println("   Scheduled Request ID: ${request.scheduledRequestId}")
            println("   Answers: ${request.answers.size} items")

            val requestJson = json.encodeToString(request)
            println("📤 JSON Request: $requestJson")

            when (val response = repo.onPutAuth(endpoint, requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Draft Update API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val resultResponse: WorkOrderResultResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Draft updated successfully")
                            println("   Message: ${resultResponse.message}")
                            println("   Work Order Result ID: ${resultResponse.data}")
                            println("=".repeat(80))

                            Result.success(resultResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: if (AppLanguage.isArabic) "فشل في تحديث المسودة" else "Failed to update draft"
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
                    Result.failure(Exception("Failed to update draft: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateDraft: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to update draft: ${e.message}"))
        }
    }

    /**
     * ✅ NEW: Execute inspection (final submission)
     * POST /api/v1/work-order-results/execute/{id}
     *
     * @param workOrderResultId The work order result ID from previous save/update
     * @param request The execute inspection request
     * @return Result with WorkOrderResultResponse
     */
    suspend fun executeInspection(
        workOrderResultId: Int,
        request: com.informatique.mtcit.data.model.ExecuteInspectionRequest
    ): Result<WorkOrderResultResponse> {
        return try {
            println("=".repeat(80))
            println("✅ ChecklistApiService: Executing inspection (final submission)")
            println("=".repeat(80))

            val endpoint = "work-order-results/execute/$workOrderResultId"
            println("📡 API Call: POST $endpoint")
            println("📤 Execute Request:")
            println("   Work Order Result ID: $workOrderResultId")
            println("   Decision ID: ${request.decisionId}")
            println("   Refuse Notes: ${request.refuseNotes ?: "N/A"}")
            println("   Expired Date: ${request.expiredDate ?: "N/A"}")

            // ✅ Build conditional JSON - only include relevant fields
            val requestMap = com.informatique.mtcit.data.model.ExecuteInspectionRequest.create(
                decisionId = request.decisionId,
                refuseNotes = request.refuseNotes,
                expiredDate = request.expiredDate
            )

            // ✅ Manually build JSON string from map to avoid serialization issues with Any type
            val requestJson = buildString {
                append("{")
                requestMap.entries.forEachIndexed { index, entry ->
                    if (index > 0) append(",")
                    append("\"${entry.key}\":")
                    when (val value = entry.value) {
                        is String -> append("\"$value\"")
                        is Number -> append(value)
                        else -> append("\"$value\"")
                    }
                }
                append("}")
            }
            println("📤 JSON Request: $requestJson")

            when (val response = repo.onPostAuthJson(endpoint, requestJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Execute Inspection API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val resultResponse: WorkOrderResultResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Inspection executed successfully")
                            println("   Message: ${resultResponse.message}")
                            println("=".repeat(80))

                            Result.success(resultResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: if (AppLanguage.isArabic) "فشل في تنفيذ الفحص" else "Failed to execute inspection"
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
                    Result.failure(Exception("Failed to execute inspection: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in executeInspection: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to execute inspection: ${e.message}"))
        }
    }
}
