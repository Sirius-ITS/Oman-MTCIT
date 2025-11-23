package com.informatique.mtcit.business.transactions

import javax.inject.Inject
import javax.inject.Provider

/**
 * Factory for creating transaction strategies based on transaction type
 * Uses Hilt's Provider to lazily create strategies only when needed
 * Note: No @Singleton scope - allows injection of ViewModelScoped dependencies
 */
class TransactionStrategyFactory @Inject constructor(
    // Marine Unit Registration Category (6 strategies)
    private val temporaryRegistrationStrategy: Provider<TemporaryRegistrationStrategy>,
    private val permanentRegistrationStrategy: Provider<PermanentRegistrationStrategy>,
    private val suspendRegistrationStrategy: Provider<SuspendRegistrationStrategy>,
    private val cancelRegistrationStrategy: Provider<CancelRegistrationStrategy>,
    private val mortgageCertificateStrategy: Provider<MortgageCertificateStrategy>,
    private val releaseMortgageStrategy: Provider<ReleaseMortgageStrategy>,
    // Marine Unit request for inspection
    private val requestInspectionStrategy: Provider<RequestInspectionStrategy>,

    // Ship Data Modifications Category
    private val shipRegistrationStrategy: Provider<ShipRegistrationStrategy>,
    private val shipNameChangeStrategy: Provider<ShipNameChangeStrategy>,
    private val captainNameChangeStrategy: Provider<CaptainNameChangeStrategy>,
    private val shipDimensionsChangeStrategy: Provider<ShipDimensionsChangeStrategy>,

    // Navigation permits
    private val issueNavigationPermitStrategy: Provider<IssueNavigationPermitStrategy>,
    private val renewNavigationPermitStrategy: Provider<RenewNavigationPermitStrategy>,

    ) {

    /**
     * Create a strategy instance for the given transaction type
     */
    fun create(type: TransactionType): TransactionStrategy {
        return when (type) {
            // Marine Unit Registration transactions
            TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> temporaryRegistrationStrategy.get()
            TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> permanentRegistrationStrategy.get()
            TransactionType.SUSPEND_PERMANENT_REGISTRATION -> suspendRegistrationStrategy.get()
            TransactionType.CANCEL_PERMANENT_REGISTRATION -> cancelRegistrationStrategy.get()
            TransactionType.MORTGAGE_CERTIFICATE -> mortgageCertificateStrategy.get()
            TransactionType.RELEASE_MORTGAGE -> releaseMortgageStrategy.get()

            //
            TransactionType.REQUEST_FOR_INSPECTION -> requestInspectionStrategy.get()

            // Ship Data Modifications transactions
            TransactionType.SHIP_NAME_CHANGE -> shipNameChangeStrategy.get()
            TransactionType.CAPTAIN_NAME_CHANGE -> captainNameChangeStrategy.get()
            TransactionType.SHIP_ACTIVITY_CHANGE -> captainNameChangeStrategy.get()
            TransactionType.SHIP_DIMENSIONS_CHANGE -> shipDimensionsChangeStrategy.get()
            TransactionType.SHIP_ENGINE_CHANGE -> captainNameChangeStrategy.get()
            TransactionType.SHIP_PORT_CHANGE -> captainNameChangeStrategy.get()
            TransactionType.SHIP_OWNERSHIP_CHANGE -> captainNameChangeStrategy.get()

            // Navigation permits
            TransactionType.ISSUE_NAVIGATION_PERMIT -> issueNavigationPermitStrategy.get()
            TransactionType.RENEW_NAVIGATION_PERMIT -> renewNavigationPermitStrategy.get()

        }
    }
}
