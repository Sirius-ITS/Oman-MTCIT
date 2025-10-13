package com.informatique.mtcit.ui.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.informatique.mtcit.R

/**
 * Transaction Category Model
 */
data class TransactionCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val transactions: List<Transaction>
)

/**
 * Transaction Model
 */
data class Transaction(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val route: String
)

/**
 * Get all transaction categories
 */
fun getTransactionCategories(): List<TransactionCategory> {
    return listOf(
        // Category 1: Marine Unit Registration Certificates (التسجيل)
        TransactionCategory(
            id = "marine_unit_registrations",
            titleRes = R.string.category_marine_unit_registrations,
            descriptionRes = R.string.category_marine_unit_registrations_desc,
            iconRes = R.drawable.ic_ship_registration,
            transactions = listOf(
                Transaction(
                    id = "temporary_registration_certificate",
                    titleRes = R.string.transaction_temporary_registration_certificate,
                    descriptionRes = R.string.transaction_temporary_registration_certificate_desc,
                    route = "ship_registration_form"
                ),
                Transaction(
                    id = "permanent_registration_certificate",
                    titleRes = R.string.transaction_permanent_registration_certificate,
                    descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                    route = "permanent_registration_form"
                ),
                Transaction(
                    id = "suspend_permanent_registration",
                    titleRes = R.string.transaction_suspend_permanent_registration,
                    descriptionRes = R.string.transaction_suspend_permanent_registration_desc,
                    route = "suspend_registration_form"
                ),
                Transaction(
                    id = "cancel_permanent_registration",
                    titleRes = R.string.transaction_cancel_permanent_registration,
                    descriptionRes = R.string.transaction_cancel_permanent_registration_desc,
                    route = "cancel_registration_form"
                ),
                Transaction(
                    id = "mortgage_certificate",
                    titleRes = R.string.transaction_mortgage_certificate,
                    descriptionRes = R.string.transaction_mortgage_certificate_desc,
                    route = "mortgage_certificate_form"
                ),
                Transaction(
                    id = "release_mortgage",
                    titleRes = R.string.transaction_release_mortgage,
                    descriptionRes = R.string.transaction_release_mortgage_desc,
                    route = "release_mortgage_form"
                )
            )
        ),

        // Category 2: Navigation (الملاحة)
        TransactionCategory(
            id = "navigation",
            titleRes = R.string.category_navigation,
            descriptionRes = R.string.category_navigation_desc,
            iconRes = R.drawable.ic_navigation,
            transactions = listOf(
                Transaction(
                    id = "issue_navigation_permit",
                    titleRes = R.string.transaction_issue_navigation_permit,
                    descriptionRes = R.string.transaction_issue_navigation_permit_desc,
                    route = "issue_navigation_permit_form"
                ),
                Transaction(
                    id = "renew_navigation_permit",
                    titleRes = R.string.transaction_renew_navigation_permit,
                    descriptionRes = R.string.transaction_renew_navigation_permit_desc,
                    route = "renew_navigation_permit_form"
                ),
                Transaction(
                    id = "suspend_navigation_permit",
                    titleRes = R.string.transaction_suspend_navigation_permit,
                    descriptionRes = R.string.transaction_suspend_navigation_permit_desc,
                    route = "suspend_navigation_permit_form"
                )
            )
        ),

        // Category 3: Ship Data Modifications (تعديل بيانات السفينة)
        TransactionCategory(
            id = "ship_data_modifications",
            titleRes = R.string.category_ship_data_modifications,
            descriptionRes = R.string.category_ship_data_modifications_desc,
            iconRes = R.drawable.ic_ship_modification,
            transactions = listOf(
                Transaction(
                    id = "ship_name_change",
                    titleRes = R.string.transaction_ship_name_change,
                    descriptionRes = R.string.transaction_ship_name_change_desc,
                    route = "ship_name_change_form"
                ),
                Transaction(
                    id = "captain_name_change",
                    titleRes = R.string.transaction_captain_name_change,
                    descriptionRes = R.string.transaction_captain_name_change_desc,
                    route = "captain_name_change_form"
                ),
                Transaction(
                    id = "ship_activity_change",
                    titleRes = R.string.transaction_ship_activity_change,
                    descriptionRes = R.string.transaction_ship_activity_change_desc,
                    route = "ship_activity_change_form"
                ),
                Transaction(
                    id = "ship_dimensions_change",
                    titleRes = R.string.transaction_ship_dimensions_change,
                    descriptionRes = R.string.transaction_ship_dimensions_change_desc,
                    route = "ship_dimensions_change_form"
                ),
                Transaction(
                    id = "ship_engine_change",
                    titleRes = R.string.transaction_ship_engine_change,
                    descriptionRes = R.string.transaction_ship_engine_change_desc,
                    route = "ship_engine_change_form"
                ),
                Transaction(
                    id = "ship_port_change",
                    titleRes = R.string.transaction_ship_port_change,
                    descriptionRes = R.string.transaction_ship_port_change_desc,
                    route = "ship_port_change_form"
                ),
                Transaction(
                    id = "ship_ownership_change",
                    titleRes = R.string.transaction_ship_ownership_change,
                    descriptionRes = R.string.transaction_ship_ownership_change_desc,
                    route = "ship_ownership_change_form"
                )
            )
        )
    )
}
