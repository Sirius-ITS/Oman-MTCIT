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

        // Extract core fields
        val requestId = dataObject["id"]?.jsonPrimitive?.intOrNull ?: 0
        val message = dataObject["message"]?.jsonPrimitive?.contentOrNull
        val messageDetails = dataObject["messageDetails"]?.jsonPrimitive?.contentOrNull

        // ✅ Handle both formats:
        // 1. Registration: separate "requestSerial" and "requestYear" fields
        // 2. Inspection: combined "requestNumber" field (e.g., "180/2026")
        val requestSerial: String
        val requestYear: Int

        val requestNumber = dataObject["requestNumber"]?.jsonPrimitive?.contentOrNull
        if (requestNumber != null) {
            // Format: "180/2026" - split and parse
            val parts = requestNumber.split("/")
            requestSerial = parts.getOrNull(0) ?: "0"
            requestYear = parts.getOrNull(1)?.toIntOrNull() ?: 0
        } else {
            // Separate fields
            requestSerial = dataObject["requestSerial"]?.jsonPrimitive?.intOrNull?.toString() ?: "0"
            requestYear = dataObject["requestYear"]?.jsonPrimitive?.intOrNull ?: 0
        }

        // Extract request type info (or use known type ID if not in response)
        val requestType = dataObject["requestType"]?.jsonObject?.let { rt ->
            RequestTypeInfo(
                id = rt["id"]?.jsonPrimitive?.intOrNull ?: 0,
                name = getLocalizedValue(rt, "name"),
                nameAr = rt["nameAr"]?.jsonPrimitive?.contentOrNull,
                nameEn = rt["nameEn"]?.jsonPrimitive?.contentOrNull
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
        val status = (dataObject["status"] ?: dataObject["requestStatus"])?.jsonObject?.let { st ->
            RequestStatusInfo(
                id = st["id"]?.jsonPrimitive?.intOrNull ?: 0,
                name = getLocalizedValue(st, "name"),
                nameAr = st["nameAr"]?.jsonPrimitive?.contentOrNull,
                nameEn = st["nameEn"]?.jsonPrimitive?.contentOrNull
            )
        } ?: RequestStatusInfo(0, "Unknown", null, null)

        // ✅ Use ShipDataExtractor to get structured ship data sections
        val sections = ShipDataExtractor.extractShipDataSections(response.data)

        // ✅ Extract isPaid field (comes as string "0" or "1" from API)
        val isPaid = dataObject["isPaid"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0

        return RequestDetailUiModel(
            requestId = requestId,
            requestSerial = "$requestSerial/$requestYear",
            requestYear = requestYear,
            requestType = requestType,
            status = status,
            message = message,
            messageDetails = messageDetails,
            sections = sections,
            isPaid = isPaid
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
}
