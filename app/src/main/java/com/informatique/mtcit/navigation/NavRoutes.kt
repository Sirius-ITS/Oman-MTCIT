package com.informatique.mtcit.navigation

import android.net.Uri

sealed class NavRoutes(val route: String) {
    data object HomeRoute : NavRoutes("homepage")
    data object LoginRoute : NavRoutes("login")
    data object SettingsRoute : NavRoutes("settings_screen")

    data object MainCategoriesRoute : NavRoutes("mainCategoriesScreen/{categoryId}"){
        fun createRoute(categoryId: String) = "mainCategoriesScreen/${Uri.encode(categoryId)}"
    }

    data object TransactionListRoute : NavRoutes("transaction_list/{categoryId}/{subCategoryId}"){
        fun createRoute(categoryId: String, subCategoryId: String)
                = "transaction_list/${Uri.encode(categoryId)}/${Uri.encode(subCategoryId)}"
    }

    data object TransactionRequirementRoute : NavRoutes("transaction_requirements/{categoryId}/{subCategoryId}/{transactionId}/{parentTitleRes}"){
        fun createRoute(categoryId: String, subCategoryId: String, transactionId: String, parentTitleRes: String)
                = "transaction_requirements/${Uri.encode(categoryId)}/${Uri.encode(subCategoryId)}/${Uri.encode(transactionId)}/${Uri.encode(parentTitleRes)}"
    }

    data object ShipRegistrationRoute : NavRoutes("ship_registration_form")

    data object PermanentRegistrationRoute : NavRoutes("permanent_registration_form")

    data object SuspendRegistrationRoute : NavRoutes("suspend_registration_form")

    data object CancelRegistrationRoute : NavRoutes("cancel_registration_form")

    data object MortgageCertificateRoute : NavRoutes("mortgage_certificate_form")

    data object ReleaseMortgageRoute : NavRoutes("release_mortgage_form")

    data object IssueNavigationPermitRoute : NavRoutes("issue_navigation_permit_form")

    data object RenewNavigationPermitRoute : NavRoutes("renew_navigation_permit_form")

    data object SuspendNavigationPermitRoute : NavRoutes("suspend_navigation_permit_form")

    data object ShipNameChangeRoute : NavRoutes("ship_name_change_form")

    data object CaptainNameChangeRoute : NavRoutes("captain_name_change_form")

    data object ShipActivityChangeRoute : NavRoutes("ship_activity_change_form")

    data object ShipPortChangeRoute : NavRoutes("ship_port_change_form")

    data object ShipOwnershipChangeRoute : NavRoutes("ship_ownership_change_form")

    data object FileViewerRoute : NavRoutes("file_viewer/{fileUri}/{fileName}"){
        fun createRoute(fileUri: String, fileName: String)
                = "file_viewer/${Uri.encode(fileUri)}/${Uri.encode(fileName)}"
    }

    data object LanguageScreenRoute : NavRoutes("languagescreen")

    data object PaymentDetailsRoute : NavRoutes("pay")

    data object PaymentSuccessRoute : NavRoutes("paysuc")
}