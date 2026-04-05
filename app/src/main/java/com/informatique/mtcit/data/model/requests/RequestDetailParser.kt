package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.json.*
import java.util.Locale
import com.informatique.mtcit.common.util.AppLanguage

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

        // ✅ Extract engineer-specific inspection details card
        val inspectionDetails: InspectionDetails? = if (isScheduledInspection) {
            try {
                val ir = dataObject["inspectionRequest"]?.jsonObject
                val scheduledDate = dataObject["scheduledDate"]?.jsonPrimitive?.contentOrNull ?: ""
                if (ir != null) {
                    val isArabic = AppLanguage.isArabic
                    val reqNumber = ir["requestNumber"]?.jsonPrimitive?.contentOrNull ?: ""
                    val sName = ir["shipInfo"]?.jsonObject?.get("ship")?.jsonObject
                        ?.get("shipName")?.jsonPrimitive?.contentOrNull ?: ""
                    val authority = ir["authority"]?.jsonObject?.let { a ->
                        if (isArabic) a["nameAr"]?.jsonPrimitive?.contentOrNull
                        else a["nameEn"]?.jsonPrimitive?.contentOrNull
                    } ?: ""
                    val place = ir["place"]?.jsonObject?.let { p ->
                        if (isArabic) p["nameAr"]?.jsonPrimitive?.contentOrNull
                        else p["nameEn"]?.jsonPrimitive?.contentOrNull
                    } ?: ""
                    val inspDate = ir["inspectionDate"]?.jsonPrimitive?.contentOrNull ?: ""
                    val purpose = ir["purpose"]?.jsonObject?.let { p ->
                        if (isArabic) p["nameAr"]?.jsonPrimitive?.contentOrNull
                        else p["nameEn"]?.jsonPrimitive?.contentOrNull
                    } ?: ""
                    InspectionDetails(
                        requestNumber = reqNumber,
                        shipName = sName,
                        authorityName = authority,
                        portName = place,
                        inspectionDate = formatDateSimple(inspDate),
                        purposeName = purpose,
                        scheduledTime = formatTimeSimple(scheduledDate)
                    )
                } else null
            } catch (e: Exception) {
                println("⚠️ Error parsing inspectionDetails: ${e.message}")
                null
            }
        } else null

        // ✅ Extract work orders (assigned engineers list)
        val engineerWorkOrders: List<EngineerWorkOrder> = if (isScheduledInspection) {
            try {
                val isArabic = AppLanguage.isArabic
                dataObject["workOrders"]?.jsonArray?.map { element ->
                    val wo = element.jsonObject
                    val eng = wo["inspectionEngineer"]?.jsonObject
                    val engName = if (isArabic)
                        eng?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: ""
                    else
                        eng?.get("nameEn")?.jsonPrimitive?.contentOrNull ?: ""
                    val job = eng?.get("job")?.jsonObject?.let { j ->
                        if (isArabic) j["nameAr"]?.jsonPrimitive?.contentOrNull
                        else j["nameEn"]?.jsonPrimitive?.contentOrNull
                    }
                    val st = wo["status"]?.jsonObject
                    val stId = st?.get("id")?.jsonPrimitive?.contentOrNull ?: ""
                    val stName = if (isArabic)
                        st?.get("nameAr")?.jsonPrimitive?.contentOrNull ?: ""
                    else
                        st?.get("nameEn")?.jsonPrimitive?.contentOrNull ?: ""
                    EngineerWorkOrder(engineerName = engName, jobTitle = job, statusId = stId, statusName = stName)
                } ?: emptyList()
            } catch (e: Exception) {
                println("⚠️ Error parsing workOrders: ${e.message}")
                emptyList()
            }
        } else emptyList()

        // ✅ Checklist notes will be loaded from API separately and stored in ViewModel
        // Pass null here; ViewModel will update after loading checklist settings
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
            scheduledRequestId = scheduledRequestId,
            inspectionDetails = inspectionDetails,
            engineerWorkOrders = engineerWorkOrders
        )
    }

    private fun formatDateSimple(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (_: Exception) { dateString }
    }

    private fun formatTimeSimple(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (_: Exception) { dateString }
    }

    /**
     * Get localized value based on current locale
     */
    private fun getLocalizedValue(jsonObject: JsonObject, fallbackKey: String): String {
        val isArabic = AppLanguage.isArabic

        return when {
            isArabic -> jsonObject["nameAr"]?.jsonPrimitive?.contentOrNull
            else -> jsonObject["nameEn"]?.jsonPrimitive?.contentOrNull
        } ?: jsonObject[fallbackKey]?.jsonPrimitive?.contentOrNull ?: "N/A"
    }

    /**
     * Get request type name from ID.
     * Names match the official backend RegMdRequestTypeEnum (all 22 types).
     * Used as fallback when the API response does not include nameAr / nameEn.
     */
    private fun getRequestTypeName(typeId: Int): String {
        val isArabic = AppLanguage.isArabic
        return when (typeId) {
            // ── Ship Registration Services ────────────────────────────────
            1  -> if (AppLanguage.isArabic) "إصدار شهادة تسجيل مؤقتة للسفن والوحدات البحرية" else "Temporary Registration Certificate for Ships and Marine Units"
            2  -> if (AppLanguage.isArabic) "إصدار شهادة تسجيل دائمة للسفن والوحدات البحرية" else "Permanent Registration Certificate for Ships and Marine Units"
            15 -> if (AppLanguage.isArabic) "إصدار شهادة تسجيل دائمة للسفن والوحدات البحرية التي عمرها يتعدى (20 سنة)" else "Permanent Registration Certificate for Ships and Marine Units Over 20 Years"
            16 -> if (AppLanguage.isArabic) "إصدار شهادة تسجيل وحدة بحرية مسجَّلة بنظام التسجيل الموازي" else "Parallel Registration Certificate for Marine Units"
            19 -> if (AppLanguage.isArabic) "تعليق شهادة تسجيل دائمة للسفن والوحدات البحرية" else "Suspend Permanent Registration Certificate"
            17 -> if (AppLanguage.isArabic) "تعليق شهادة تسجيل وحدة بحرية بنظام التسجيل المواز�� للعمل تحت العلم العُماني" else "Suspend Parallel Registration Certificate (Under Omani Flag)"
            18 -> if (AppLanguage.isArabic) "فك تعليق شهادة تسجيل وحدة بحرية بنظام التسجيل الموازي للعمل تحت العلم العُماني" else "Unsuspend Parallel Registration Certificate (Under Omani Flag)"
            7  -> if (AppLanguage.isArabic) "إصدار شهادة شطب التسجيل الدائمة للسفينة أو الوحدة البحرية" else "Cancellation of Permanent Registration Certificate"
            4  -> if (AppLanguage.isArabic) "إصدار شهادة رهن للسفن والوحدات البحرية" else "Mortgage Certificate for Ships and Marine Units"
            5  -> if (AppLanguage.isArabic) "إصدار شهادة فك رهن للسفن والوحدات البحرية" else "Mortgage Release Certificate for Ships and Marine Units"
            // ── Ship Data Modification Services ──────────────────────────
            9  -> if (AppLanguage.isArabic) "اضافة / تعديل على ملف سفينة / الوحدة البحرية" else "Add / Edit Ship or Marine Unit File"
            21 -> if (AppLanguage.isArabic) "طلب تغيير أبعاد السفينة أو الوحدة البحرية" else "Change Ship or Marine Unit Dimensions"
            22 -> if (AppLanguage.isArabic) "طلب استبدال محرك السفينة أو الوحدة البحرية" else "Change Ship or Marine Unit Engine"
            12 -> if (AppLanguage.isArabic) "طلب تغيير ميناء تسجيل السفينة أو الوحدة البحرية" else "Change Ship or Marine Unit Registration Port"
            20 -> if (AppLanguage.isArabic) "طلب تغيير ملاك السفينة أو الوحدة البحرية" else "Change Ship or Marine Unit Owners"
            11 -> if (AppLanguage.isArabic) "طلب تغيير اسم السفينة أو الوحدة البحرية" else "Change Ship or Marine Unit Name"
            10 -> if (AppLanguage.isArabic) "طلب تغيير اسم ربان السفينة أو الوحدة البحرية" else "Change Captain Name of Ship or Marine Unit"
            13 -> if (AppLanguage.isArabic) "طلب تغيير نشاط السفينة أو الوحدة البحرية" else "Change Ship or Marine Unit Activity"
            // ── Navigation Permits / Licences ─────────────────────────────
            3  -> if (AppLanguage.isArabic) "إصدار تصريح ملاحة للسفن والوحدات البحرية" else "Navigation Permit for Ships and Marine Units"
            6  -> if (AppLanguage.isArabic) "تجديد تصريح ملاحة للسفن والوحدات البحرية" else "Navigation Permit Renewal for Ships and Marine Units"
            14 -> if (AppLanguage.isArabic) "ايقاف تصريح ملاحة للسفن والوحدات البحرية" else "Suspend Navigation Permit for Ships and Marine Units"
            // ── Inspection Services ───────────────────────────────────────
            8  -> if (AppLanguage.isArabic) "تقديم طلب معاينة للسفن والوحدات البحرية" else "Inspection Request for Ships and Marine Units"
            else -> if (AppLanguage.isArabic) "نوع غير معروف" else "Unknown Type"
        }
    }

    /**
     * Get status name from ID
     */
    private fun getStatusName(statusId: Int): String {
        val isArabic = AppLanguage.isArabic
        return when (statusId) {
            1 -> if (AppLanguage.isArabic) "مسودة" else "Draft"
            2 -> if (AppLanguage.isArabic) "مرفوض" else "Rejected"
            3 -> if (AppLanguage.isArabic) "معلق" else "Pending"
            4 -> if (AppLanguage.isArabic) "تم الإرسال" else "Submitted"
            5 -> if (AppLanguage.isArabic) "قيد الانتظار" else "Waiting"
            6 -> if (AppLanguage.isArabic) "مجدول" else "Scheduled"
            7 -> if (AppLanguage.isArabic) "مقبول" else "Accepted"
            8 -> if (AppLanguage.isArabic) "قيد التنفيذ" else "In Progress"
            9 -> if (AppLanguage.isArabic) "مكتمل" else "Completed"
            10 -> if (AppLanguage.isArabic) "ملغي" else "Cancelled"
            11 -> if (AppLanguage.isArabic) "منتهي" else "Expired"
            12 -> if (AppLanguage.isArabic) "موقوف" else "Suspended"
            13 -> if (AppLanguage.isArabic) "جاهز للإصدار" else "Ready for Issuance"
            14 -> if (AppLanguage.isArabic) "مصدر" else "Issued"
            15 -> if (AppLanguage.isArabic) "قيد المراجعة" else "Under Review"
            16 -> if (AppLanguage.isArabic) "يتطلب معلومات إضافية" else "Requires Additional Information"
            else -> if (AppLanguage.isArabic) "غير معروف" else "Unknown"
        }
    }
}
