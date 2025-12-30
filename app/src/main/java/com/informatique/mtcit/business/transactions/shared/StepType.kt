package com.informatique.mtcit.business.transactions.shared

/**
 * Enum representing different step types in transaction workflows
 * Used to identify steps in a type-safe way instead of relying on hard-coded strings
 *
 * This allows strategies to handle step-specific logic without checking field IDs
 */
enum class StepType {
    /**
     * Person type selection step (Individual vs Company)
     */
    PERSON_TYPE,

    /**
     * Commercial registration step (for companies)
     */
    COMMERCIAL_REGISTRATION,

    /**
     * Marine unit/ship selection step
     */
    MARINE_UNIT_SELECTION,

    /**
     * Marine unit/ship selection step
     */
    MARINE_UNIT_DATA,

    /**
     * Marine unit name selection/reservation step
     */
    MARINE_UNIT_NAME_SELECTION,

    /**
     * Navigation areas/sailing regions selection step
     */
    NAVIGATION_AREAS,

    /**
     * Crew management/sailor info step
     */
    CREW_MANAGEMENT,

    /**
     * Ship dimensions step
     */
    SHIP_DIMENSIONS,

    /**
     * Ship weights step
     */
    SHIP_WEIGHTS,

    /**
     * Owner information step
     */
    OWNER_INFO,

    /**
     * Engine information step
     */
    ENGINE_INFO,

    /**
     * Documents upload step
     */
    DOCUMENTS,

    /**
     * Inspection purposes and authorities step
     */
    INSPECTION_PURPOSES_AND_AUTHORITIES,

    /**
     * Documents upload step
     */
    MORTGAGE_DATA,

    /**
     * Payment details and submission step
     */
    PAYMENT,

    /**
     * Payment confirmation step (when user clicks Pay button)
     */
    PAYMENT_CONFIRMATION,

    /**
     * Payment success confirmation step
     */
    PAYMENT_SUCCESS,

    /**
     * Review/summary step (final step)
     */
    REVIEW,

    /**
     * OTP verification step (for login/authentication)
     */
    OTP_VERIFICATION,

    /**
     * Maritime identification fields step (IMO, MMSI, Call Sign)
     * Used when these fields are missing after ship selection
     */
    MARITIME_IDENTIFICATION,

    /**
     * Custom step (default fallback)
     */
    CUSTOM
}
