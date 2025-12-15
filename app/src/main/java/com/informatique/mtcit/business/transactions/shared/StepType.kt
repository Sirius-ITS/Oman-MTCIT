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
     * Payment details and submission step
     */
    PAYMENT,

    /**
     * Payment success confirmation step
     */
    PAYMENT_SUCCESS,

    /**
     * Review/summary step (final step)
     */
    REVIEW,

    /**
     * Custom step (default fallback)
     */
    CUSTOM
}
