package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.json.*
import java.util.Locale

/**
 * Utility to parse dynamic JSON request detail responses
 * Automatically traverses nested objects and arrays
 */
object RequestDetailParser {

    /**
     * Parse RequestDetailResponse into UI-friendly model
     * @param response API response containing request data
     * @param knownRequestTypeId Optional request type ID (used when API doesn't return requestType field)
     */
    fun parseToUiModel(response: RequestDetailResponse, knownRequestTypeId: Int? = null): RequestDetailUiModel {
        val dataObject = response.data.jsonObject

        // ✅ Check if this is a scheduled inspection request (engineer view)
        val isScheduledInspection = dataObject.containsKey("scheduledDate") && dataObject.containsKey("inspectionRequest")

        // ✅ For scheduled inspection, extract data from nested inspectionRequest
        val actualDataObject = if (isScheduledInspection) {
            dataObject["inspectionRequest"]?.jsonObject ?: dataObject
        } else {
            dataObject
        }

        // Extract core fields
        val requestId = actualDataObject["id"]?.jsonPrimitive?.intOrNull ?: 0
        val message = actualDataObject["message"]?.jsonPrimitive?.contentOrNull
        val messageDetails = actualDataObject["messageDetails"]?.jsonPrimitive?.contentOrNull

        // ✅ Handle both formats:
        // 1. Registration: separate "requestSerial" and "requestYear" fields
        // 2. Inspection: combined "requestNumber" field (e.g., "180/2026")
        val requestSerial: String
        val requestYear: Int

        val requestNumber = actualDataObject["requestNumber"]?.jsonPrimitive?.contentOrNull
        if (requestNumber != null) {
            // Format: "180/2026" - split and parse
            val parts = requestNumber.split("/")
            requestSerial = parts.getOrNull(0) ?: "0"
            requestYear = parts.getOrNull(1)?.toIntOrNull() ?: 0
        } else {
            // Separate fields
            requestSerial = actualDataObject["requestSerial"]?.jsonPrimitive?.intOrNull?.toString() ?: "0"
            requestYear = actualDataObject["requestYear"]?.jsonPrimitive?.intOrNull ?: 0
        }

        // Extract request type info (or use known type ID if not in response)
        val requestType = actualDataObject["requestType"]?.jsonObject?.let { rt ->
            val typeId = rt["id"]?.jsonPrimitive?.intOrNull ?: 0
            val nameAr = rt["nameAr"]?.jsonPrimitive?.contentOrNull
            val nameEn = rt["nameEn"]?.jsonPrimitive?.contentOrNull

            // ✅ If API only provides ID without names, use fallback from getRequestTypeName
            val name = if (nameAr == null && nameEn == null) {
                getRequestTypeName(typeId)
            } else {
                getLocalizedValue(rt, "name")
            }

            RequestTypeInfo(
                id = typeId,
                name = name,
                nameAr = nameAr,
                nameEn = nameEn
            )
        } ?: if (knownRequestTypeId != null) {
            // ✅ Use known request type ID when API doesn't provide it (e.g., inspection requests)
            RequestTypeInfo(
                id = knownRequestTypeId,
                name = getRequestTypeName(knownRequestTypeId),
                nameAr = null,
                nameEn = null
            )
        } else {
            RequestTypeInfo(0, "Unknown", null, null)
        }

        // Extract status info (handle both "status" and "requestStatus" keys)
        val status = (actualDataObject["status"] ?: actualDataObject["requestStatus"])?.jsonObject?.let { st ->
            val statusId = st["id"]?.jsonPrimitive?.intOrNull ?: 0
            val nameAr = st["nameAr"]?.jsonPrimitive?.contentOrNull
            val nameEn = st["nameEn"]?.jsonPrimitive?.contentOrNull

            // ✅ If API only provides ID without names, use fallback from getStatusName
            val name = if (nameAr == null && nameEn == null) {
                getStatusName(statusId)
            } else {
                getLocalizedValue(st, "name")
            }

            RequestStatusInfo(
                id = statusId,
                name = name,
                nameAr = nameAr,
                nameEn = nameEn
            )
        } ?: RequestStatusInfo(0, "Unknown", null, null)

        // ✅ Use ShipDataExtractor to get structured ship data sections
        val sections = ShipDataExtractor.extractShipDataSections(response.data)

        // ✅ Extract isPaid field (comes as string "0" or "1" from API)
        val isPaid = actualDataObject["isPaid"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0

        // ✅ Extract hasAcceptance from requestType (if present) or default to 0
        val hasAcceptance = actualDataObject["requestType"]?.jsonObject
            ?.get("hasAcceptance")?.jsonPrimitive?.intOrNull ?: 0

        println("📋 RequestDetailParser: hasAcceptance = $hasAcceptance (requestTypeId = ${requestType.id})")

        // ✅ Extract ship name for header display
        val shipName = actualDataObject["shipInfo"]?.jsonObject
            ?.get("ship")?.jsonObject
            ?.get("shipName")?.jsonPrimitive?.contentOrNull

        // ✅ Extract purposeId for checklist loading (engineer only)
        val purposeId = actualDataObject["purpose"]?.jsonObject
            ?.get("id")?.jsonPrimitive?.intOrNull

        // ✅ Extract workOrderResult for completed inspections (engineer only)
        // ⚠️ IMPORTANT: workOrderResult is in the ROOT dataObject, not in inspectionRequest!
        val workOrderResult = try {
            println("🔍 Checking for workOrderResult in ROOT dataObject...")
            println("   - dataObject keys: ${dataObject.keys}")

            dataObject["workOrderResult"]?.jsonObject?.let { wor ->
                println("✅ Found workOrderResult!")
                println("   - workOrderResult keys: ${wor.keys}")

                val id = wor["id"]?.jsonPrimitive?.intOrNull
                println("   - workOrderResult id: $id")

                // ✅ Parse answers array (actual API structure has 'answers' not 'checklistAnswers')
                println("   - Checking for 'answers' array...")
                val answersArray = wor["answers"]?.jsonArray
                println("   - answers array size: ${answersArray?.size ?: 0}")

                val answers = answersArray?.map { answerElement ->
                    val answer = answerElement.jsonObject
                    val checklistItem = answer["checklistSettingsItem"]?.jsonObject

                    // Extract checklistItemId and question from checklistSettingsItem
                    val checklistItemId = checklistItem?.get("id")?.jsonPrimitive?.intOrNull ?: 0
                    val question = checklistItem?.get("question")?.jsonPrimitive?.contentOrNull ?: ""
                    val rawAnswerValue = answer["answer"]?.jsonPrimitive?.contentOrNull ?: ""

                    // ✅ FIX: For List type (id=4), convert answer ID to actual answer text
                    val checklistTypeId = checklistItem?.get("checklistType")?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
                    val actualAnswerValue = if (checklistTypeId == 4 && checklistItem != null) {
                        // List type - lookup the answer text from choices
                        val choices = checklistItem["choices"]?.jsonArray
                        val matchingChoice = choices?.find {
                            it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == rawAnswerValue ||
                            it.jsonObject["id"]?.jsonPrimitive?.intOrNull?.toString() == rawAnswerValue
                        }
                        matchingChoice?.jsonObject?.get("answer")?.jsonPrimitive?.contentOrNull ?: rawAnswerValue
                    } else {
                        // Text type or other - use as is
                        rawAnswerValue
                    }

                    com.informatique.mtcit.data.model.ChecklistAnswer(
                        checklistItemId = checklistItemId,
                        fieldNameAr = question,  // Use question as field name
                        fieldNameEn = question,  // Use question as field name
                        answer = actualAnswerValue  // ✅ Use converted answer text
                    )
                } ?: emptyList()

                println("✅ Parsed workOrderResult with ${answers.size} answers")
                answers.forEach { ans ->
                    println("   - Item ${ans.checklistItemId}: ${ans.fieldNameAr} = ${ans.answer}")
                }

                // ✅ Extract checklistItems from answers for form display
                val checklistItemsFromResult = wor["answers"]?.jsonArray?.mapNotNull { answerElement ->
                    val answer = answerElement.jsonObject
                    val checklistItem = answer["checklistSettingsItem"]?.jsonObject ?: return@mapNotNull null

                    val typeObj = checklistItem["checklistType"]?.jsonObject
                    val choicesArray = checklistItem["choices"]?.jsonArray

                    com.informatique.mtcit.data.model.ChecklistItem(
                        id = checklistItem["id"]?.jsonPrimitive?.intOrNull ?: 0,
                        question = checklistItem["question"]?.jsonPrimitive?.contentOrNull ?: "",
                        checklistType = com.informatique.mtcit.data.model.ChecklistType(
                            id = typeObj?.get("id")?.jsonPrimitive?.intOrNull ?: 0,
                            nameAr = typeObj?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: "",
                            nameEn = typeObj?.get("nameEn")?.jsonPrimitive?.contentOrNull ?: "",
                            name = typeObj?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                        ),
                        isMandatory = checklistItem["isMandatory"]?.jsonPrimitive?.booleanOrNull ?: false,
                        itemOrder = checklistItem["itemOrder"]?.jsonPrimitive?.intOrNull ?: 0,
                        isActive = checklistItem["isActive"]?.jsonPrimitive?.booleanOrNull ?: true,
                        note = checklistItem["note"]?.jsonPrimitive?.contentOrNull,
                        choices = choicesArray?.map { choiceElement ->
                            val choice = choiceElement.jsonObject
                            com.informatique.mtcit.data.model.ChecklistChoice(
                                id = choice["id"]?.jsonPrimitive?.intOrNull ?: 0,
                                answer = choice["answer"]?.jsonPrimitive?.contentOrNull ?: "",
                                isActive = choice["isActive"]?.jsonPrimitive?.booleanOrNull ?: true
                            )
                        } ?: emptyList()
                    )
                }?.sortedBy { it.itemOrder } ?: emptyList()

                println("✅ Extracted ${checklistItemsFromResult.size} checklist items from workOrderResult")

                com.informatique.mtcit.data.model.WorkOrderResult(
                    id = id,
                    checklistAnswers = answers,
                    checklistItems = checklistItemsFromResult
                )
            }
        } catch (e: Exception) {
            println("⚠️ Error parsing workOrderResult: ${e.message}")
            e.printStackTrace()
            null
        }

        // ✅ Extract scheduledRequestId (root data.id for scheduled inspections)
        val scheduledRequestId = if (isScheduledInspection) {
            val rootId = dataObject["id"]?.jsonPrimitive?.intOrNull
            println("✅ Extracted scheduledRequestId from root: $rootId")
            rootId
        } else {
            null
        }

        return RequestDetailUiModel(
            requestId = requestId,
            requestSerial = "$requestSerial/$requestYear",
            requestYear = requestYear,
            requestType = requestType,
            status = status,
            message = message,
            messageDetails = messageDetails,
            sections = sections,
            isPaid = isPaid,
            hasAcceptance = hasAcceptance,
            shipName = shipName,
            purposeId = purposeId,
            workOrderResult = workOrderResult,
            scheduledRequestId = scheduledRequestId
        )
    }

    /**
     * Get localized value based on current locale
     */
    private fun getLocalizedValue(jsonObject: JsonObject, fallbackKey: String): String {
        val isArabic = Locale.getDefault().language == "ar"

        return when {
            isArabic -> jsonObject["nameAr"]?.jsonPrimitive?.contentOrNull
            else -> jsonObject["nameEn"]?.jsonPrimitive?.contentOrNull
        } ?: jsonObject[fallbackKey]?.jsonPrimitive?.contentOrNull ?: "N/A"
    }

    /**
     * Get request type name from ID
     */
    private fun getRequestTypeName(typeId: Int): String {
        val isArabic = Locale.getDefault().language == "ar"
        return when (typeId) {
            1 -> if (isArabic) "شهادة تسجيل مؤقتة" else "Temporary Registration"
            2 -> if (isArabic) "شهادة تسجيل دائمة" else "Permanent Registration"
            3 -> if (isArabic) "إصدار رخصة ملاحية" else "Issue Navigation Permit"
            4 -> if (isArabic) "شهادة رهن" else "Mortgage Certificate"
            5 -> if (isArabic) "فك الرهن" else "Release Mortgage"
            6 -> if (isArabic) "تجديد رخصة ملاحية" else "Renew Navigation Permit"
            7 -> if (isArabic) "إلغاء تسجيل دائم" else "Cancel Permanent Registration"
            8 -> if (isArabic) "طلب معاينة" else "Request for Inspection"
            else -> if (isArabic) "نوع غير معروف" else "Unknown Type"
        }
    }

    /**
     * Get status name from ID
     */
    private fun getStatusName(statusId: Int): String {
        val isArabic = Locale.getDefault().language == "ar"
        return when (statusId) {
            1 -> if (isArabic) "مسودة" else "Draft"
            2 -> if (isArabic) "مرفوض" else "Rejected"
            3 -> if (isArabic) "معلق" else "Pending"
            4 -> if (isArabic) "تم الإرسال" else "Submitted"
            5 -> if (isArabic) "قيد الانتظار" else "Waiting"
            6 -> if (isArabic) "مجدول" else "Scheduled"
            7 -> if (isArabic) "مقبول" else "Accepted"
            8 -> if (isArabic) "قيد التنفيذ" else "In Progress"
            9 -> if (isArabic) "مكتمل" else "Completed"
            10 -> if (isArabic) "ملغي" else "Cancelled"
            11 -> if (isArabic) "منتهي" else "Expired"
            12 -> if (isArabic) "موقوف" else "Suspended"
            13 -> if (isArabic) "جاهز للإصدار" else "Ready for Issuance"
            14 -> if (isArabic) "تم الإصدار" else "Issued"
            15 -> if (isArabic) "قيد المراجعة" else "Under Review"
            16 -> if (isArabic) "يتطلب معلومات إضافية" else "Requires Additional Information"
            else -> if (isArabic) "غير معروف" else "Unknown"
        }
    }
}
