package com.informatique.mtcit.ui.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.informatique.mtcit.R

/**
 * Main Category Model (e.g., Marine Affairs, Building Permits)
 */
data class MainCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val subCategories: List<SubCategory>
)

/**
 * Sub Category Model (e.g., Registration, Navigation)
 */
data class SubCategory(
    val id: String,
    @StringRes val titleRes: Int,
    val parentCategoryId: String,
    val transactions: List<Transaction>
)

/**
 * Transaction Model
 */
data class Transaction(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val route: String,
    val routeInfo: String = "route-n",
    // Legacy numeric count - kept for compatibility but prefer `steps.size` when available
    val stepCount: Int = 3,
    val isAvailable: Boolean = true,
    val fees: String = "10 ريال",
    // New: list of requirement descriptions to show in TransactionRequirementsScreen
    val requirements: List<String> = emptyList(),
    // New: estimated duration string (shown in list and forwarded to requirements screen)
    val duration: String = "4-1 أيام",
    // New: optional explicit step titles. If empty, UI will fallback to generic "خطوة N" using stepCount
    val steps: List<String> = emptyList()
)

/**
 * Get all main categories with sub-categories
 * THIS IS THE SINGLE SOURCE OF TRUTH - All data comes from here
 */
fun getMainCategories(): List<MainCategory> {
    return listOf(
        // 1. REGISTRATION DEPARTMENT (قسم التسجيل)
        MainCategory(
            id = "registration_department",
            titleRes = R.string.main_category_registration_department,
            descriptionRes = R.string.main_category_registration_department_desc,
            iconRes = R.drawable.ic_ship_registration,
            subCategories = listOf(
                SubCategory(
                    id = "marine_unit_registration",
                    titleRes = R.string.subcategory_marine_unit_registration,
                    parentCategoryId = "registration_department",
                    transactions = listOf(
                        Transaction(
                            id = "temporary_registration_certificate",
                            titleRes = R.string.transaction_temporary_registration_certificate,
                            descriptionRes = R.string.transaction_temporary_registration_certificate_desc,
                            route = "ship_registration_form",
                            routeInfo = "route-1",
                            fees = "20 ريال",
                            requirements = listOf(
                                "نسخة من بطاقة الهوية",
                                "صورة للسفينة (جديدة وواضحة)",
                                "إيصال دفع الرسوم"
                            ),
                            duration = "5-1 أيام",
                            steps = listOf("ملء النموذج", "رفع المستندات", "مراجعة الطلب", "استلام")
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = "permanent_registration_form",
                            routeInfo = "route-2"
                        ),
                        Transaction(
                            id = "suspend_permanent_registration",
                            titleRes = R.string.transaction_suspend_permanent_registration,
                            descriptionRes = R.string.transaction_suspend_permanent_registration_desc,
                            route = "suspend_registration_form",
                            routeInfo = "route-3"
                        ),
                        Transaction(
                            id = "cancel_permanent_registration",
                            titleRes = R.string.transaction_cancel_permanent_registration,
                            descriptionRes = R.string.transaction_cancel_permanent_registration_desc,
                            route = "cancel_registration_form",
                            routeInfo = "route-4"
                        ),
                        Transaction(
                            id = "mortgage_certificate",
                            titleRes = R.string.transaction_mortgage_certificate,
                            descriptionRes = R.string.transaction_mortgage_certificate_desc,
                            route = "mortgage_certificate_form",
                            routeInfo = "route-5"
                        ),
                        Transaction(
                            id = "release_mortgage",
                            titleRes = R.string.transaction_release_mortgage,
                            descriptionRes = R.string.transaction_release_mortgage_desc,
                            route = "release_mortgage_form",
                            routeInfo = "route-6"
                        )
                    )
                ),
                SubCategory(
                    id = "navigation_permits",
                    titleRes = R.string.subcategory_navigation_permits,
                    parentCategoryId = "seamen_affairs_department",
                    transactions = listOf(
                        Transaction(
                            id = "issue_navigation_permit",
                            titleRes = R.string.transaction_issue_navigation_permit,
                            descriptionRes = R.string.transaction_issue_navigation_permit_desc,
                            route = "issue_navigation_permit_form",
                            routeInfo = "route-7",
                            requirements = listOf(
                                "طلب إصدار تصريح ملاحية مكتمل",
                                "نسخة من رخصة القبطان (إن وجدت)",
                                "خريطة مسار الرحلة (إن لزم)"
                            )
                        ),
                        Transaction(
                            id = "renew_navigation_permit",
                            titleRes = R.string.transaction_renew_navigation_permit,
                            descriptionRes = R.string.transaction_renew_navigation_permit_desc,
                            route = "renew_navigation_permit_form",
                            routeInfo = "route-8"
                        ),
                        Transaction(
                            id = "suspend_navigation_permit",
                            titleRes = R.string.transaction_suspend_navigation_permit,
                            descriptionRes = R.string.transaction_suspend_navigation_permit_desc,
                            route = "suspend_navigation_permit_form",
                            routeInfo = "route-9"
                        )
                    )
                ),
                SubCategory(
                    id = "ship_data_modifications",
                    titleRes = R.string.subcategory_ship_data_modifications,
                    parentCategoryId = "facilities_docks_department",
                    transactions = listOf(
                        Transaction(
                            id = "ship_name_change",
                            titleRes = R.string.transaction_ship_name_change,
                            descriptionRes = R.string.transaction_ship_name_change_desc,
                            route = "ship_name_change_form",
                            routeInfo = "route-10",
                            requirements = listOf(
                                "طلب تغيير اسم السفينة موقع",
                                "شهادة تسجيل قديمة",
                                "موافقة المالك الجديد (إن وجدت)"
                            )
                        ),
                        Transaction(
                            id = "ship_dimensions_change",
                            titleRes = R.string.transaction_ship_dimensions_change,
                            descriptionRes = R.string.transaction_ship_dimensions_change_desc,
                            route = "ship_dimensions_change_form",
                            routeInfo = "route-11"
                        ),
                        Transaction(
                            id = "ship_engine_change",
                            titleRes = R.string.transaction_ship_engine_change,
                            descriptionRes = R.string.transaction_ship_engine_change_desc,
                            route = "ship_engine_change_form",
                            routeInfo = "route-12"
                        ),
                        Transaction(
                            id = "captain_name_change",
                            titleRes = R.string.transaction_captain_name_change,
                            descriptionRes = R.string.transaction_captain_name_change_desc,
                            route = "captain_name_change_form",
                            routeInfo = "route-13"
                        ),
                        Transaction(
                            id = "ship_activity_change",
                            titleRes = R.string.transaction_ship_activity_change,
                            descriptionRes = R.string.transaction_ship_activity_change_desc,
                            route = "ship_activity_change_form",
                            routeInfo = "route-14"
                        ),
                        Transaction(
                            id = "ship_port_change",
                            titleRes = R.string.transaction_ship_port_change,
                            descriptionRes = R.string.transaction_ship_port_change_desc,
                            route = "ship_port_change_form",
                            routeInfo = "route-15"
                        ),
                        Transaction(
                            id = "ship_ownership_change",
                            titleRes = R.string.transaction_ship_ownership_change,
                            descriptionRes = R.string.transaction_ship_ownership_change_desc,
                            route = "ship_ownership_change_form",
                            routeInfo = "route-16"
                        )
                    )
                )
            )
        ),

        // 2. SEAMEN'S AFFAIRS DEPARTMENT (قسم شؤون البحارة)
        MainCategory(
            id = "seamen_affairs_department",
            titleRes = R.string.main_category_seamen_affairs_department,
            descriptionRes = R.string.main_category_seamen_affairs_department_desc,
            iconRes = R.drawable.ic_navigation,
            subCategories = listOf(
                SubCategory(
                    id = "marine_unit_registration",
                    titleRes = R.string.subcategory_marine_unit_registration,
                    parentCategoryId = "registration_department",
                    transactions = listOf(
                        Transaction(
                            id = "temporary_registration_certificate",
                            titleRes = R.string.transaction_temporary_registration_certificate,
                            descriptionRes = R.string.transaction_temporary_registration_certificate_desc,
                            route = "ship_registration_form",
                            routeInfo = "route-1"
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = "permanent_registration_form",
                            routeInfo = "route-2"
                        ),
                        Transaction(
                            id = "suspend_permanent_registration",
                            titleRes = R.string.transaction_suspend_permanent_registration,
                            descriptionRes = R.string.transaction_suspend_permanent_registration_desc,
                            route = "suspend_registration_form",
                            routeInfo = "route-3"
                        ),
                        Transaction(
                            id = "cancel_permanent_registration",
                            titleRes = R.string.transaction_cancel_permanent_registration,
                            descriptionRes = R.string.transaction_cancel_permanent_registration_desc,
                            route = "cancel_registration_form",
                            routeInfo = "route-4"
                        ),
                        Transaction(
                            id = "mortgage_certificate",
                            titleRes = R.string.transaction_mortgage_certificate,
                            descriptionRes = R.string.transaction_mortgage_certificate_desc,
                            route = "mortgage_certificate_form",
                            routeInfo = "route-5"
                        ),
                        Transaction(
                            id = "release_mortgage",
                            titleRes = R.string.transaction_release_mortgage,
                            descriptionRes = R.string.transaction_release_mortgage_desc,
                            route = "release_mortgage_form",
                            routeInfo = "route-6"
                        )
                    )
                )
            )
        ),

        // 3. FACILITIES AND DOCKS LICENSING DEPARTMENT (قسم تراخيص المرافق والأرصفة)
        MainCategory(
            id = "facilities_docks_department",
            titleRes = R.string.main_category_facilities_docks_department,
            descriptionRes = R.string.main_category_facilities_docks_department_desc,
            iconRes = R.drawable.ic_ship_modification,
            subCategories = listOf(
            )
        )
    )
}
