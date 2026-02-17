package com.informatique.mtcit.business.transactions

/**
 * Enum representing different types of transactions in the system
 * Each transaction now has its own TransactionContext with API endpoints
 */
enum class TransactionType(
    /**
     * Transaction type ID from the API
     */
    val typeId: Int,
    /**
     * Transaction context containing all API endpoints and configuration
     */
    val context: TransactionContext
) {
    // Marine Unit Registration Category (6 transactions)
    TEMPORARY_REGISTRATION_CERTIFICATE(
        typeId = 1,
        context = TransactionContext(
            inspectionPreviewBaseContext = "registration-requests",
            displayName = "شهادة تسجيل مؤقتة",
            createEndpoint = "registration-requests",
            updateStatusEndpoint = "registration-requests/{requestId}/update-status",
            sendRequestEndpoint = "registration-requests/{requestId}/send-request",
            sendRequestPostOrPut = "POST",
            getRequestEndpoint = "registration-requests/{requestId}",
            deleteRequestEndpoint = "registration-requests/{requestId}/delete",
            paymentReceiptEndpoint = "registration-requests/payment",
            paymentSubmitEndpoint = "registration-requests/add-payment",
            proceedRequestEndpoint = "registration-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    PERMANENT_REGISTRATION_CERTIFICATE(
        typeId = 2,
        context = TransactionContext(
            inspectionPreviewBaseContext = "perm-registration-requests",
            displayName = "شهادة تسجيل دائمة",
            createEndpoint = "perm-registration-requests/create-permanent",
            updateStatusEndpoint = "perm-registration-requests/{requestId}/update-status",
            sendRequestEndpoint = "perm-registration-requests/{requestId}/send-request",
            sendRequestPostOrPut = "POST",
            getRequestEndpoint = "perm-registration-requests/{requestId}",
            deleteRequestEndpoint = "perm-registration-requests/{requestId}/delete",
            paymentReceiptEndpoint = "perm-registration-requests/payment",
            paymentSubmitEndpoint = "perm-registration-requests/add-payment",
            proceedRequestEndpoint = "perm-registration-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    SUSPEND_PERMANENT_REGISTRATION(
        typeId = 0,
        context = TransactionContext(
            inspectionPreviewBaseContext = "registration",
            displayName = "تعليق تسجيل دائم",
            createEndpoint = "registration/suspend",
            updateStatusEndpoint = "registration/{requestId}/update-status",
            sendRequestPostOrPut = "POST",
            sendRequestEndpoint = "registration/{requestId}/send-request",
            paymentReceiptEndpoint = "registration/payment",
            paymentSubmitEndpoint = "registration/add-payment"
        )
    ),
    CANCEL_PERMANENT_REGISTRATION(
        typeId = 7,
        context = TransactionContext(
            inspectionPreviewBaseContext = "deletion-requests",
            displayName = "إلغاء تسجيل دائم",
            createEndpoint = "deletion-requests/cancel",
            updateStatusEndpoint = "deletion-requests/{requestId}/update-status",
            sendRequestEndpoint = "deletion-requests/{requestId}/send-request",
            sendRequestPostOrPut = "POST",
            getRequestEndpoint = "deletion-requests/{requestId}",
            deleteRequestEndpoint = "deletion-requests/{requestId}/delete",
            paymentReceiptEndpoint = "deletion-requests/payment",
            paymentSubmitEndpoint = "deletion-requests/add-payment",
            proceedRequestEndpoint = "deletion-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    MORTGAGE_CERTIFICATE(
        typeId = 4,
        context = TransactionContext(
            inspectionPreviewBaseContext = "mortgage-request",
            displayName = "طلب شهادة رهن",
            createEndpoint = "mortgage-request/create-mortgage-request",
            updateStatusEndpoint = "mortgage-request/{requestId}/update-status",
            sendRequestEndpoint = "mortgage-request/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            getRequestEndpoint = "mortgage-request/{requestId}",
            deleteRequestEndpoint = "mortgage-request/{requestId}/delete",
            paymentReceiptEndpoint = "mortgage-request/payment",
            paymentSubmitEndpoint = "mortgage-request/add-payment",
            proceedRequestEndpoint = "mortgage-request/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    RELEASE_MORTGAGE(
        typeId = 5,
        context = TransactionContext(
            inspectionPreviewBaseContext = "mortgage-redemption-requests",
            displayName = "طلب فك رهن",
            createEndpoint = "mortgage-redemption-request/create-mortgage-redemption-request",
            updateStatusEndpoint = "mortgage-redemption-request/{requestId}/update-status",
            sendRequestEndpoint = "mortgage-redemption-request/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            getRequestEndpoint = "mortgage-redemption-request/{requestId}",
            deleteRequestEndpoint = "mortgage-redemption-request/{requestId}/delete",
            paymentReceiptEndpoint = "mortgage-redemption-request/payment",
            paymentSubmitEndpoint = "mortgage-redemption-request/add-payment",
            proceedRequestEndpoint = "mortgage-redemption-request/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    REQUEST_FOR_INSPECTION(
        typeId = 8,
        context = TransactionContext(
            inspectionPreviewBaseContext = "inspection-requests",
            displayName = "طلب معاينة",
            createEndpoint = "inspection-requests/create",
            updateStatusEndpoint = "inspection-requests/{requestId}/update-status",
            sendRequestEndpoint = "inspection-requests/{requestId}/send",
            sendRequestPostOrPut = "POST",
             paymentReceiptEndpoint = "inspection-requests/payment",
            paymentSubmitEndpoint = "inspection-requests/add-payment",
            proceedRequestEndpoint = "inspection-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),

    // Ship Data Modifications Category
    SHIP_NAME_CHANGE(
        typeId = 14,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-modifications",
            displayName = "تغيير اسم السفينة",
            createEndpoint = "ship-modifications/name-change",
            updateStatusEndpoint = "ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-modifications/payment",
            paymentSubmitEndpoint = "ship-modifications/add-payment"
        )
    ),
    CAPTAIN_NAME_CHANGE(
        typeId = 15,
        context = TransactionContext(
            inspectionPreviewBaseContext = "change-captain",
            displayName = "تغيير اسم الربان",
            createEndpoint = "change-captain/captain-change",
            updateStatusEndpoint = "change-captain/{requestId}/update-status",
            sendRequestEndpoint = "change-captain/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "change-captain/payment",
            paymentSubmitEndpoint = "change-captain/add-payment"
        )
    ),
    SHIP_ACTIVITY_CHANGE(
        typeId = 16,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-modifications",
            displayName = "تغيير نشاط السفينة",
            createEndpoint = "ship-modifications/activity-change",
            updateStatusEndpoint = "ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-modifications/payment",
            paymentSubmitEndpoint = "ship-modifications/add-payment"
        )
    ),
    SHIP_DIMENSIONS_CHANGE(
        typeId = 0,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-modifications",
            displayName = "تغيير أبعاد السفينة",
            createEndpoint = "ship-modifications/dimensions-change",
            updateStatusEndpoint = "ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-modifications/payment",
            paymentSubmitEndpoint = "ship-modifications/add-payment"
        )
    ),
    SHIP_ENGINE_CHANGE(
        typeId = 0,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-modifications",
            displayName = "تغيير محرك السفينة",
            createEndpoint = "ship-modifications/engine-change",
            updateStatusEndpoint = "ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-modifications/payment",
            paymentSubmitEndpoint = "ship-modifications/add-payment"
        )
    ),
    SHIP_PORT_CHANGE(
        typeId = 19,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-modifications",
            displayName = "تغيير ميناء السفينة",
            createEndpoint = "ship-modifications/port-change",
            updateStatusEndpoint = "ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-modifications/payment",
            paymentSubmitEndpoint = "ship-modifications/add-payment"
        )
    ),
    SHIP_OWNERSHIP_CHANGE(
        typeId = 0,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-modifications",
            displayName = "تغيير ملكية السفينة",
            createEndpoint = "ship-modifications/ownership-change",
            updateStatusEndpoint = "ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-modifications/payment",
            paymentSubmitEndpoint = "ship-modifications/add-payment"
        )
    ),

    // Navigation permits
    ISSUE_NAVIGATION_PERMIT(
        typeId = 3,
        context = TransactionContext(
            inspectionPreviewBaseContext = "ship-navigation-license-request",
            displayName = "إصدار تصريح إبحار",
            createEndpoint = "ship-navigation-license-request/issue",
            updateStatusEndpoint = "ship-navigation-license-request/{requestId}/update-status",
            sendRequestEndpoint = "ship-navigation-license-request/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "ship-navigation-license-request/payment",
            paymentSubmitEndpoint = "ship-navigation-license-request/add-payment",
            proceedRequestEndpoint = "ship-navigation-license-request/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    RENEW_NAVIGATION_PERMIT(
        typeId = 6,
        context = TransactionContext(
            inspectionPreviewBaseContext = "navigation-license-renewal-request",
            displayName = "تجديد تصريح إبحار",
            createEndpoint = "navigation-license-renewal-request/renew",
            updateStatusEndpoint = "navigation-license-renewal-request/{requestId}/update-status",
            sendRequestEndpoint = "navigation-license-renewal-request/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "navigation-license-renewal-request/payment",
            paymentSubmitEndpoint = "navigation-license-renewal-request/add-payment",
            proceedRequestEndpoint = "navigation-license-renewal-request/ship-info/{shipInfoId}/proceed-request"
        )
    );

    companion object {
        /**
         * Map API requestTypeId to TransactionType
         * @param typeId The request type ID from the API
         * @return TransactionType or null if not found
         */
        fun fromTypeId(typeId: Int): TransactionType? {
            return entries.firstOrNull { it.typeId == typeId }
        }
        /**
         * Get display name for a request type ID
         */
        fun getDisplayName(typeId: Int): String {
            return fromTypeId(typeId)?.context?.displayName ?: "نوع غير معروف"
        }
    }
}

/**
 * Extension function to get the requestTypeId for API calls
 * Maps TransactionType enum to the backend transaction ID
 */
fun TransactionType.toRequestTypeId(): String {
    return when (this) {
        TransactionType.ISSUE_NAVIGATION_PERMIT -> "3"
        TransactionType.RENEW_NAVIGATION_PERMIT -> "6"
        TransactionType.SUSPEND_PERMANENT_REGISTRATION -> "0"
        TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> "1"
        TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> "2"
        TransactionType.REQUEST_FOR_INSPECTION -> "8"
        TransactionType.CANCEL_PERMANENT_REGISTRATION -> "7"
        TransactionType.MORTGAGE_CERTIFICATE -> "4"
        TransactionType.RELEASE_MORTGAGE -> "5"
        TransactionType.SHIP_NAME_CHANGE -> "14"
        TransactionType.SHIP_ACTIVITY_CHANGE -> "16"
        TransactionType.CAPTAIN_NAME_CHANGE -> "15"
        TransactionType.SHIP_PORT_CHANGE -> "19"
        // Default values for transactions without explicit IDs
        TransactionType.SHIP_DIMENSIONS_CHANGE -> "0"
        TransactionType.SHIP_ENGINE_CHANGE -> "0"
        TransactionType.SHIP_OWNERSHIP_CHANGE -> "0"
    }
}
