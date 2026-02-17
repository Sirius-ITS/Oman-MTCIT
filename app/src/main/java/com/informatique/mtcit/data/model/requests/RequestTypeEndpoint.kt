package com.informatique.mtcit.data.model.requests

/**
 * Maps Request Type IDs to their corresponding API endpoints
 * Includes both detail fetch endpoints and certificate issuance endpoints
 * Easily extensible for new request types
 */
enum class RequestTypeEndpoint(
    val requestTypeId: Int,
    val endpointPath: String,
    val issuanceEndpoint: String? = null // ✅ NEW: Endpoint for certificate issuance
) {
    // 1. Temporary Registration (مؤقت)
    TEMP_REGISTRATION(
        requestTypeId = 1,
        endpointPath = "registration-requests",
        issuanceEndpoint = "registration-requests/{requestId}/issuance-provisional-registration-certificate"
    ),

    // 2. Permanent Registration (دائم)
    PERM_REGISTRATION(
        requestTypeId = 2,
        endpointPath = "perm-registration-requests",
        issuanceEndpoint = "perm-registration-requests/{requestId}/issuance-provisional-registration-certificate"
    ),

    // 3. Issue Navigation Permit (ترخيص)
    ISSUE_NAVIGATION_PERMIT(
        requestTypeId = 3,
        endpointPath = "ship-navigation-license-request",
        issuanceEndpoint = "ship-navigation-license-request/{requestId}/issuance"
    ),

    // 4. Mortgage Certificate (رهن)
    MORTGAGE(
        requestTypeId = 4,
        endpointPath = "mortgage-request",
        issuanceEndpoint = "certificate/{requestId}/mortgage-certificate"
    ),

    // 5. Release Mortgage (فك رهن)
    RELEASE_MORTGAGE(
        requestTypeId = 5,
        endpointPath = "mortgage-redemption-request",
        issuanceEndpoint = "certificate/{requestId}/mortgage-redemption-certificate"
    ),

    // 6. Renew Navigation Permit (تجديد ترخيص)
    RENEW_NAVIGATION_PERMIT(
        requestTypeId = 6,
        endpointPath = "navigation-license-renewal-request",
        issuanceEndpoint = "navigation-license-renewal-request/{requestId}/issuance"
    ),

    // 7. Cancel Permanent Registration (شطب)
    CANCEL_PERMANENT_REGISTRATION(
        requestTypeId = 7,
        endpointPath = "deletion-requests",
        issuanceEndpoint = "deletion-requests/{requestId}/issuance"
    ),

    // 8. Request for Inspection (No certificate issuance)
    REQUEST_FOR_INSPECTION(
        requestTypeId = 8,
        endpointPath = "inspection-requests",
        issuanceEndpoint = null // ✅ Inspection requests do not have certificate issuance
    ),

    ;

    companion object {
        /**
         * Get endpoint path by request type ID
         * @param typeId Request type ID
         * @return Endpoint path or null if not found
         */
        fun getEndpointByTypeId(typeId: Int): String? {
            return values().find { it.requestTypeId == typeId }?.endpointPath
        }

        /**
         * ✅ NEW: Get issuance endpoint by request type ID
         * @param typeId Request type ID
         * @param requestId The actual request ID to replace {requestId} placeholder
         * @return Full endpoint path with requestId replaced, or null if not found
         */
        fun getIssuanceEndpoint(typeId: Int, requestId: Int): String? {
            val template = values().find { it.requestTypeId == typeId }?.issuanceEndpoint
            return template?.replace("{requestId}", requestId.toString())
        }

        /**
         * Check if request type is supported for detail fetching
         */
        fun isSupported(typeId: Int): Boolean {
            return values().any { it.requestTypeId == typeId }
        }

        /**
         * ✅ NEW: Check if issuance is supported for this request type
         */
        fun isIssuanceSupported(typeId: Int): Boolean {
            return values().find { it.requestTypeId == typeId }?.issuanceEndpoint != null
        }
    }
}

