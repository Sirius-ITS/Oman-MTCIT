package com.informatique.mtcit.business.transactions

/**
 * Enum representing different types of transactions in the system
 * Each transaction now has its own TransactionContext with API endpoints
 */
enum class TransactionType(
    /**
     * Transaction context containing all API endpoints and configuration
     */
    val context: TransactionContext
) {
    // Marine Unit Registration Category (6 transactions)
    TEMPORARY_REGISTRATION_CERTIFICATE(
        context = TransactionContext(
            displayName = "شهادة تسجيل مؤقتة",
            createEndpoint = "api/v1/registration-requests",
            updateStatusEndpoint = "api/v1/registration-requests/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/registration-requests/{requestId}/send-request",
            sendRequestPostOrPut = "POST",
            getRequestEndpoint = "api/v1/registration-requests/{requestId}",
            deleteRequestEndpoint = "api/v1/registration-requests/{requestId}/delete",
            paymentReceiptEndpoint = "api/v1/registration-requests/payment",
            paymentSubmitEndpoint = "api/v1/registration-requests/add-payment",
            proceedRequestEndpoint = "api/v1/registration-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    PERMANENT_REGISTRATION_CERTIFICATE(
        context = TransactionContext(
            displayName = "شهادة تسجيل دائمة",
            createEndpoint = "api/v1/perm-registration-requests/create-permanent",
            updateStatusEndpoint = "api/v1/perm-registration-requests/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/perm-registration-requests/{requestId}/send-request",
            sendRequestPostOrPut = "POST",
            getRequestEndpoint = "api/v1/perm-registration-requests/{requestId}",
            deleteRequestEndpoint = "api/v1/perm-registration-requests/{requestId}/delete",
            paymentReceiptEndpoint = "api/v1/perm-registration-requests/payment",
            paymentSubmitEndpoint = "api/v1/perm-registration-requests/add-payment",
            proceedRequestEndpoint = "api/v1/perm-registration-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    SUSPEND_PERMANENT_REGISTRATION(
        context = TransactionContext(
            displayName = "تعليق تسجيل دائم",
            createEndpoint = "api/v1/registration/suspend",
            updateStatusEndpoint = "api/v1/registration/{requestId}/update-status",
            sendRequestPostOrPut = "POST",
            sendRequestEndpoint = "api/v1/registration/{requestId}/send-request",
            paymentReceiptEndpoint = "api/v1/registration/payment",
            paymentSubmitEndpoint = "api/v1/registration/add-payment"
        )
    ),
    CANCEL_PERMANENT_REGISTRATION(
        context = TransactionContext(
            displayName = "إلغاء تسجيل دائم",
            createEndpoint = "api/v1/deletion-requests/cancel",
            updateStatusEndpoint = "api/v1/deletion-requests/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/deletion-requests/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            getRequestEndpoint = "api/v1/deletion-requests/{requestId}",
            deleteRequestEndpoint = "api/v1/deletion-requests/{requestId}/delete",
            paymentReceiptEndpoint = "api/v1/deletion-requests/payment",
            paymentSubmitEndpoint = "api/v1/deletion-requests/add-payment",
            proceedRequestEndpoint = "api/v1/deletion-requests/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    MORTGAGE_CERTIFICATE(
        context = TransactionContext(
            displayName = "طلب شهادة رهن",
            createEndpoint = "api/v1/mortgage-request/create-mortgage-request",
            updateStatusEndpoint = "api/v1/mortgage-request/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/mortgage-request/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            getRequestEndpoint = "api/v1/mortgage-request/{requestId}",
            deleteRequestEndpoint = "api/v1/mortgage-request/{requestId}/delete",
            paymentReceiptEndpoint = "api/v1/mortgage-request/payment",
            paymentSubmitEndpoint = "api/v1/mortgage-request/add-payment",
            proceedRequestEndpoint = "api/v1/mortgage-request/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    RELEASE_MORTGAGE(
        context = TransactionContext(
            displayName = "طلب فك رهن",
            createEndpoint = "api/v1/mortgage-redemption-request/create-mortgage-redemption-request",
            updateStatusEndpoint = "api/v1/mortgage-redemption-request/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/mortgage-redemption-request/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            getRequestEndpoint = "api/v1/mortgage-redemption-request/{requestId}",
            deleteRequestEndpoint = "api/v1/mortgage-redemption-request/{requestId}/delete",
            paymentReceiptEndpoint = "api/v1/mortgage-redemption-request/payment",
            paymentSubmitEndpoint = "api/v1/mortgage-redemption-request/add-payment",
            proceedRequestEndpoint = "api/v1/mortgage-redemption-request/ship-info/{shipInfoId}/proceed-request"
        )
    ),
    REQUEST_FOR_INSPECTION(
        context = TransactionContext(
            displayName = "طلب معاينة",
            createEndpoint = "api/v1/inspection/create",
            updateStatusEndpoint = "api/v1/inspection/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/inspection/{requestId}/send-request",
            sendRequestPostOrPut = "POST",
            paymentReceiptEndpoint = "api/v1/inspection/payment",
            paymentSubmitEndpoint = "api/v1/inspection/add-payment"
        )
    ),

    // Ship Data Modifications Category
    SHIP_NAME_CHANGE(
        context = TransactionContext(
            displayName = "تغيير اسم السفينة",
            createEndpoint = "api/v1/ship-modifications/name-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),
    CAPTAIN_NAME_CHANGE(
        context = TransactionContext(
            displayName = "تغيير اسم الربان",
            createEndpoint = "api/v1/ship-modifications/captain-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),
    SHIP_ACTIVITY_CHANGE(
        context = TransactionContext(
            displayName = "تغيير نشاط السفينة",
            createEndpoint = "api/v1/ship-modifications/activity-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),
    SHIP_DIMENSIONS_CHANGE(
        context = TransactionContext(
            displayName = "تغيير أبعاد السفينة",
            createEndpoint = "api/v1/ship-modifications/dimensions-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),
    SHIP_ENGINE_CHANGE(
        context = TransactionContext(
            displayName = "تغيير محرك السفينة",
            createEndpoint = "api/v1/ship-modifications/engine-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),
    SHIP_PORT_CHANGE(
        context = TransactionContext(
            displayName = "تغيير ميناء السفينة",
            createEndpoint = "api/v1/ship-modifications/port-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),
    SHIP_OWNERSHIP_CHANGE(
        context = TransactionContext(
            displayName = "تغيير ملكية السفينة",
            createEndpoint = "api/v1/ship-modifications/ownership-change",
            updateStatusEndpoint = "api/v1/ship-modifications/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/ship-modifications/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/ship-modifications/payment",
            paymentSubmitEndpoint = "api/v1/ship-modifications/add-payment"
        )
    ),

    // Navigation permits
    ISSUE_NAVIGATION_PERMIT(
        context = TransactionContext(
            displayName = "إصدار تصريح إبحار",
            createEndpoint = "api/v1/navigation-permit/issue",
            updateStatusEndpoint = "api/v1/navigation-permit/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/navigation-permit/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/navigation-permit/payment",
            paymentSubmitEndpoint = "api/v1/navigation-permit/add-payment"
        )
    ),
    RENEW_NAVIGATION_PERMIT(
        context = TransactionContext(
            displayName = "تجديد تصريح إبحار",
            createEndpoint = "api/v1/navigation-permit/renew",
            updateStatusEndpoint = "api/v1/navigation-permit/{requestId}/update-status",
            sendRequestEndpoint = "api/v1/navigation-permit/{requestId}/send-request",
            sendRequestPostOrPut = "PUT",
            paymentReceiptEndpoint = "api/v1/navigation-permit/payment",
            paymentSubmitEndpoint = "api/v1/navigation-permit/add-payment"
        )
    );
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
        TransactionType.SHIP_NAME_CHANGE -> "0"
        TransactionType.SHIP_ACTIVITY_CHANGE -> "0"
        TransactionType.CAPTAIN_NAME_CHANGE -> "0"
        TransactionType.SHIP_PORT_CHANGE -> "0"
        // Default values for transactions without explicit IDs
        TransactionType.SHIP_DIMENSIONS_CHANGE -> "0"
        TransactionType.SHIP_ENGINE_CHANGE -> "0"
        TransactionType.SHIP_OWNERSHIP_CHANGE -> "0"
    }
}
