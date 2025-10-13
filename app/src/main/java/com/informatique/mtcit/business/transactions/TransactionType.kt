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

    // Ship Data Modifications Category
    SHIP_NAME_CHANGE,
    CAPTAIN_NAME_CHANGE,
    SHIP_ACTIVITY_CHANGE,
    SHIP_DIMENSIONS_CHANGE,
    SHIP_ENGINE_CHANGE,
    SHIP_PORT_CHANGE,
    SHIP_OWNERSHIP_CHANGE
}
