package com.informatique.mtcit.data.model.requests

/**
 * Maps Request Type IDs to their corresponding API endpoints
 * Easily extensible for new request types
 */
enum class RequestTypeEndpoint(
    val requestTypeId: Int,
    val endpointPath: String
) {
    // Temporary Registration
    TEMP_REGISTRATION(
        requestTypeId = 1,
        endpointPath = "registration-requests"
    ),

    // Permanent Registration
    PERM_REGISTRATION(
        requestTypeId = 2,
        endpointPath = "registration-requests"
    ),

    // Mortgage Request
    MORTGAGE(
        requestTypeId = 3,
        endpointPath = "mortgage-requests"
    ),

    // Add more request types here as needed
    // Example:
    // RENEWAL(
    //     requestTypeId = 4,
    //     endpointPath = "renewal-requests"
    // ),

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
         * Check if request type is supported for detail fetching
         */
        fun isSupported(typeId: Int): Boolean {
            return values().any { it.requestTypeId == typeId }
        }
    }
}

