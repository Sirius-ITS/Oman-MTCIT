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
    // 1-change name of ship or unit (تغيير اسم السفينة او الوحدة)
    private val changeNameOfShipOrUnitStrategy: Provider<ChangeNameOfShipOrUnitStrategy>,

    // 2-change captain name of ship or unit (تغيير اسم ربان للسفينة او الوحدة)
    private val changeCaptainNameOfShipOrUnitStrategy: Provider<ChangeCaptainNameOfShipOrUnitStrategy>,

    // 3-change activity of ship or unit (تغيير نشاط السفينة او الوحدة)
    private val changeActivityOfShipOrUnitStrategy: Provider<ChangeActivityOfShipOrUnitStrategy>,

    // 4-change port of ship or unit (تغيير ميناء السفينة او الوحدة)
    private val changePortOfShipOrUnitStrategy: Provider<ChangePortOfShipOrUnitStrategy>,

    //Other ship data modifications
    private val shipRegistrationStrategy: Provider<ShipRegistrationStrategy>,
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

            // Navigation permits
            TransactionType.ISSUE_NAVIGATION_PERMIT -> issueNavigationPermitStrategy.get()
            TransactionType.RENEW_NAVIGATION_PERMIT -> renewNavigationPermitStrategy.get()

            // Ship Data Modifications transactions
            // 1-change name of ship or unit (تغيير اسم السفينة او الوحدة)
            TransactionType.SHIP_NAME_CHANGE -> changeNameOfShipOrUnitStrategy.get()

            // 2-change captain name of ship or unit (تغيير اسم ربان للسفينة او الوحدة)
            TransactionType.CAPTAIN_NAME_CHANGE -> changeCaptainNameOfShipOrUnitStrategy.get()

            // 3-change activity of ship or unit (تغيير نشاط السفينة او الوحدة)
            TransactionType.SHIP_ACTIVITY_CHANGE -> changeActivityOfShipOrUnitStrategy.get()

            // 4-change port of ship or unit (تغيير ميناء السفينة او الوحدة)
            TransactionType.SHIP_PORT_CHANGE -> changePortOfShipOrUnitStrategy.get()

            // 5- ship dimensions change
            TransactionType.SHIP_DIMENSIONS_CHANGE -> shipDimensionsChangeStrategy.get()

            //Other ship data modifications
            TransactionType.SHIP_ENGINE_CHANGE -> changeCaptainNameOfShipOrUnitStrategy.get()
            TransactionType.SHIP_OWNERSHIP_CHANGE -> changeCaptainNameOfShipOrUnitStrategy.get()

        }
    }
}
