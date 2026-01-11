package com.informatique.mtcit.navigation

import android.net.Uri
import com.informatique.mtcit.ui.screens.RequestDetail
import kotlinx.serialization.json.Json
import com.informatique.mtcit.data.model.category.Transaction
import com.informatique.mtcit.data.model.category.TransactionDetail

sealed class NavRoutes(val route: String) {
    data object HomeRoute : NavRoutes("homepage")
    data object LoginRoute : NavRoutes("login/{targetTransactionType}/{categoryId}/{subCategoryId}/{transactionId}") {
        fun createRoute(
            targetTransactionType: String,
            categoryId: String,
            subCategoryId: String,
            transactionId: String
        ): String = "login/${Uri.encode(targetTransactionType)}/${Uri.encode(categoryId)}/${Uri.encode(subCategoryId)}/${Uri.encode(transactionId)}"
    }
    data object SettingsRoute : NavRoutes("settings_screen")

    data object MainCategoriesRoute : NavRoutes("mainCategoriesScreen/{categoryId}"){
        fun createRoute(categoryId: String) = "mainCategoriesScreen/${Uri.encode(categoryId)}"
    }
    data object MainCategoriesRouteWithoutID : NavRoutes("mainCategoriesScreen")
    data object ProfileScreenRoute : NavRoutes("profileScreen")
    data object NotificationScreen : NavRoutes("notificationScreen")

    data object TransactionListRoute : NavRoutes("transaction_list/{categoryId}/{subCategoryId}"){
        fun createRoute(categoryId: String, subCategoryId: String)
                = "transaction_list/${Uri.encode(categoryId)}/${Uri.encode(subCategoryId)}"
    }

//    data object TransactionRequirementRoute : NavRoutes("transaction_requirements/{categoryId}/{subCategoryId}/{transactionId}/{parentTitleRes}"){
//        fun createRoute(categoryId: String, subCategoryId: String, transactionId: String, parentTitleRes: String)
//                = "transaction_requirements/${Uri.encode(categoryId)}/${Uri.encode(subCategoryId)}/${Uri.encode(transactionId)}/${Uri.encode(parentTitleRes)}"
//    }

    data object TransactionRequirementRoute : NavRoutes("transaction_requirements/{transactionId}"){
        fun createRoute(transaction: Transaction)
                = "transaction_requirements/${Uri.encode(Json.encodeToString(transaction))}"
    }

    data object ShipRegistrationRoute : NavRoutes("7") {
        const val REQUEST_ID_ARG = "requestId"
        const val LAST_COMPLETED_STEP_ARG = "lastCompletedStep"

        fun createRouteWithResume(requestId: String, lastCompletedStep: Int? = null): String {
            return if (lastCompletedStep != null) {
                "7?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
            } else {
                "7?requestId=$requestId"
            }
        }
    }

    data object PermanentRegistrationRoute : NavRoutes("8"/*"permanent_registration_form"*/)

    data object RequestForInspection : NavRoutes("21") {
        // ✅ Support requestId and lastCompletedStep for payment resumption
        const val routeWithParams = "21?requestId={requestId}&lastCompletedStep={lastCompletedStep}"

        fun createRouteWithResume(requestId: String, lastCompletedStep: Int): String {
            return "21?requestId=$requestId&lastCompletedStep=$lastCompletedStep"
        }
    }

    data object SuspendRegistrationRoute : NavRoutes("suspend_registration_form")

    data object ChangeNameOfShipOrUnitRoute : NavRoutes("14")

    data object CancelRegistrationRoute : NavRoutes("cancel_registration_form")

    data object MortgageCertificateRoute : NavRoutes("12"/*"mortgage_certificate_form"*/)

    data object ReleaseMortgageRoute : NavRoutes("13"/*"release_mortgage_form"*/)

    data object IssueNavigationPermitRoute : NavRoutes("4"/*"issue_navigation_permit_form"*/)

    data object RenewNavigationPermitRoute : NavRoutes("5"/*"renew_navigation_permit_form"*/)

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

    data object RequestDetailRoute : NavRoutes("request-detail/{detail}"){
        fun createRoute(detail: RequestDetail)
                = "request-detail/${Uri.encode(Json.encodeToString(detail))}"
    }

    // ✅ NEW: API Request Detail Route - for dynamic API-fetched request details
    data object ApiRequestDetailRoute : NavRoutes("api-request-detail/{requestId}/{requestTypeId}") {
        fun createRoute(requestId: Int, requestTypeId: Int): String {
            return "api-request-detail/$requestId/$requestTypeId"
        }
    }

    // OAuth WebView Route
    data object OAuthWebViewRoute : NavRoutes("oauth_webview")
}