package com.informatique.mtcit.business.transactions

/**
 * Enum representing different types of transactions in the system
 */
enum class TransactionType {
    // Marine Unit Registration Category (6 transactions)
    TEMPORARY_REGISTRATION_CERTIFICATE,
    PERMANENT_REGISTRATION_CERTIFICATE,
    SUSPEND_PERMANENT_REGISTRATION,
    CANCEL_PERMANENT_REGISTRATION,
    MORTGAGE_CERTIFICATE,
    RELEASE_MORTGAGE,
    REQUEST_FOR_INSPECTION,


    // Ship Data Modifications Category
    SHIP_NAME_CHANGE,
    CAPTAIN_NAME_CHANGE,
    SHIP_ACTIVITY_CHANGE,
    SHIP_DIMENSIONS_CHANGE,
    SHIP_ENGINE_CHANGE,
    SHIP_PORT_CHANGE,
    SHIP_OWNERSHIP_CHANGE,

    // Navigation permits
    ISSUE_NAVIGATION_PERMIT,
    RENEW_NAVIGATION_PERMIT,
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

