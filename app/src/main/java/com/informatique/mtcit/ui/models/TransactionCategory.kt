package com.informatique.mtcit.ui.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.informatique.mtcit.navigation.NavRoutes
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
    val requirements: Requirement? = null,
    // New: estimated duration string (shown in list and forwarded to requirements screen)
    val duration: String = "4-1 أيام",
    // New: optional explicit step titles. If empty, UI will fallback to generic "خطوة N" using stepCount
    val steps: List<String> = emptyList()
)


data class Requirement(
    val id: String = "",
    val serviceSummaryList: List<ServiceSummary> = emptyList(),
    val serviceInfoList: List<ServiceInfo> = emptyList()
)

data class ServiceSummary (
    var type: String,
    var icon : ImageVector,
    var value: String,
    var label: String,
)


data class ServiceInfo (
    var id: Int,
    var title: String,
    var data: List<ServiceInfoSteps>,
)

data class ServiceInfoSteps (
    var id: Int = 0,
    var stepNo: Int = 1,
    var title: String = "",
    var subTitle: String? = null,
    var value: String? = null
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
                            route = NavRoutes.ShipRegistrationRoute.route,
                            routeInfo = "route-1",
                            fees = "50 ر.ع",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = "50 ريال",
                                        label = "رسوم الخدمة",
                                        ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = "10 دقائق",
                                        label = "المدة الزمنية",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = "المستندات المطلوبة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "شهادة بناء السفينة او عقد بيع في حالة الشراء(الزامي)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "مستندات المعاينة ستتم داخل دورة المعاينة",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = "رسوم الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" +  "   25 ر.ع   " ,
                                                subTitle = null,
                                                value = "25 ر.ع"
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" +  "   30  ر.ع   ",
                                                subTitle = null,
                                                value = "30 ر.ع"
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = "خطوات طلب الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "إدخال بيانات الوحدة البحرية",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "تسجيل الأبعاد الخاصة بالوحدة",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "إدخال الأوزان والحمولات المعتمدة",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "تعبئة بيانات المحركات الخاصة بالوحدة",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = "شروط الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "مالك السفينة / الموكل من مالك السفينة",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "ان يكون مواطن عماني مقيم",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = "10 دقائق",
                            steps = listOf("ملء النموذج", "رفع المستندات", "مراجعة الطلب", "استلام","استلام","استلام","استلام","استلام","استلام","استلام","استلام","استلام","استلام")
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = "permanent_registration_form",
                            routeInfo = "route-2",
                            fees = "50 ر.ع",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = "50 ريال",
                                        label = "رسوم الخدمة",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = "10 دقائق",
                                        label = "المدة الزمنية",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = "المستندات المطلوبة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "مستندات المعاينة ستتم داخل دورة المعاينةشهادة بناء السفينة أو عقد بيع في حالة الشراء وشهادة شطب في حالة تغيير الجنسية للعلم العماني (الزامي)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "مستندات المعاينة ستتم داخل دورة المعاينة",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "وثيقة التأمين سارية",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "ترخيص هيئة تنظيم الاتصالات",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "ترخيص النشاط",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "الملاك ونصيب كل مالك",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "التفويض بالإدارة في حالة وجود أكثر من مالك",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = "رسوم الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" +  "   25 ر.ع   " ,
                                                subTitle = null,
                                                value = "25 ر.ع"
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" +  "   30  ر.ع   ",
                                                subTitle = null,
                                                value = "30 ر.ع"
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = "خطوات طلب الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "تحديد نوع المستخدم",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "حدد السجل التجاري لنشاطك اختياري، بناء على الخطوة (1)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "التحقق من وجود شهادة تسجيل مؤقتة للوحدة البحرية",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "إدخال بيانات الوحدة البحرية",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "إدخال الأبعاد الخاصة بالوحدة",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = "شروط الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "مالك السفينة / الموكل من مالك السفينة",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "ان تكون السفينة ليست من السفن المحظور استقبالها",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "أن يكون مواطن عماني / مقيم",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "وجود ترخيص من هيئة تنظيم الاتصالات",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "وجود وثيقة تأمين بحري ساري",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "موافقة وزارة الثروة الزراعية السمكية ( سيتم جلب الموافقة في حالة عدم وجودها )",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "وجود معاينة ( سيتم التحويل لدورة المعاينات في حالة عدم وجود معاينة )",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "ترخيص مزاولة نشاط في حالة سفينة خدمة طبقا لنوع الخدمة",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = "5-1 أيام",
                            steps = listOf("ملء النموذج", "رفع المستندات", "مراجعة الطلب", "استلام")
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = NavRoutes.PermanentRegistrationRoute.route,
                            routeInfo = "route-2",
                            duration = "10 دقائق",
                            steps = listOf("ملء النموذج", "رفع المستندات" ,"استلام","استلام","استلام","استلام","استلام","استلام","استلام","استلام","استلام")
                        ),
                        Transaction(
                            id = "suspend_permanent_registration",
                            titleRes = R.string.transaction_suspend_permanent_registration,
                            descriptionRes = R.string.transaction_suspend_permanent_registration_desc,
                            route = NavRoutes.SuspendRegistrationRoute.route,
                            routeInfo = "route-3"
                        ),
                        Transaction(
                            id = "cancel_permanent_registration",
                            titleRes = R.string.transaction_cancel_permanent_registration,
                            descriptionRes = R.string.transaction_cancel_permanent_registration_desc,
                            route = NavRoutes.CancelRegistrationRoute.route,
                            routeInfo = "route-4"
                        ),
                        Transaction(
                            id = "mortgage_certificate",
                            titleRes = R.string.transaction_mortgage_certificate,
                            descriptionRes = R.string.transaction_mortgage_certificate_desc,
                            route = NavRoutes.MortgageCertificateRoute.route,
                            routeInfo = "route-5"
                        ),
                        Transaction(
                            id = "release_mortgage",
                            titleRes = R.string.transaction_release_mortgage,
                            descriptionRes = R.string.transaction_release_mortgage_desc,
                            route = NavRoutes.ReleaseMortgageRoute.route,
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
                            route = NavRoutes.IssueNavigationPermitRoute.route,
                            routeInfo = "route-7",
//                            requirements = listOf(
//                                "طلب إصدار تصريح ملاحية مكتمل",
//                                "نسخة من رخصة القبطان (إن وجدت)",
//                                "خريطة مسار الرحلة (إن لزم)"
//                            )
                            requirements = Requirement(
                                id = "issue_navigation_permit_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = "50 ريال",
                                        label = "رسوم الخدمة",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = "10 دقائق",
                                        label = "المدة الزمنية",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = "المتطلبات",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "نسخة من بطاقة الهوية",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "صورة للسفينة (جديدة وواضحة)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "إيصال دفع الرسوم",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = "خطوات الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "ملء النموذج",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "رفع المستندات",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "مراجعة الطلب",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = "استلام التصريح",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                        ),
                        Transaction(
                            id = "renew_navigation_permit",
                            titleRes = R.string.transaction_renew_navigation_permit,
                            descriptionRes = R.string.transaction_renew_navigation_permit_desc,
                            route = NavRoutes.RenewNavigationPermitRoute.route,
                            routeInfo = "route-8"
                        ),
                        Transaction(
                            id = "suspend_navigation_permit",
                            titleRes = R.string.transaction_suspend_navigation_permit,
                            descriptionRes = R.string.transaction_suspend_navigation_permit_desc,
                            route = NavRoutes.SuspendNavigationPermitRoute.route,
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
                            route = NavRoutes.ShipNameChangeRoute.route,
                            routeInfo = "route-10",
//                            requirements = listOf(
//                                "طلب تغيير اسم السفينة موقع",
//                                "شهادة تسجيل قديمة",
//                                "موافقة المالك الجديد (إن وجدت)"
//                            )
                            requirements = Requirement(
                                id = "issue_navigation_permit_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = "50 ريال",
                                        label = "رسوم الخدمة",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = "10 دقائق",
                                        label = "المدة الزمنية",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = "المتطلبات",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "نسخة من بطاقة الهوية",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "صورة للسفينة (جديدة وواضحة)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "إيصال دفع الرسوم",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = "خطوات الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "ملء النموذج",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "رفع المستندات",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "مراجعة الطلب",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = "استلام التصريح",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 3,
                                        title = "خطوات الخدمة",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = "ملء النموذج",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = "رفع المستندات",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = "مراجعة الطلب",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = "استلام التصريح",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
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
                            route = NavRoutes.CaptainNameChangeRoute.route,
                            routeInfo = "route-13"
                        ),
                        Transaction(
                            id = "ship_activity_change",
                            titleRes = R.string.transaction_ship_activity_change,
                            descriptionRes = R.string.transaction_ship_activity_change_desc,
                            route = NavRoutes.ShipActivityChangeRoute.route,
                            routeInfo = "route-14"
                        ),
                        Transaction(
                            id = "ship_port_change",
                            titleRes = R.string.transaction_ship_port_change,
                            descriptionRes = R.string.transaction_ship_port_change_desc,
                            route = NavRoutes.ShipPortChangeRoute.route,
                            routeInfo = "route-15"
                        ),
                        Transaction(
                            id = "ship_ownership_change",
                            titleRes = R.string.transaction_ship_ownership_change,
                            descriptionRes = R.string.transaction_ship_ownership_change_desc,
                            route = NavRoutes.ShipOwnershipChangeRoute.route,
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
                            route = NavRoutes.ShipRegistrationRoute.route,
                            routeInfo = "route-1"
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = NavRoutes.PermanentRegistrationRoute.route,
                            routeInfo = "route-2"
                        ),
                        Transaction(
                            id = "suspend_permanent_registration",
                            titleRes = R.string.transaction_suspend_permanent_registration,
                            descriptionRes = R.string.transaction_suspend_permanent_registration_desc,
                            route = NavRoutes.SuspendRegistrationRoute.route,
                            routeInfo = "route-3"
                        ),
                        Transaction(
                            id = "cancel_permanent_registration",
                            titleRes = R.string.transaction_cancel_permanent_registration,
                            descriptionRes = R.string.transaction_cancel_permanent_registration_desc,
                            route = NavRoutes.CancelRegistrationRoute.route,
                            routeInfo = "route-4"
                        ),
                        Transaction(
                            id = "mortgage_certificate",
                            titleRes = R.string.transaction_mortgage_certificate,
                            descriptionRes = R.string.transaction_mortgage_certificate_desc,
                            route = NavRoutes.MortgageCertificateRoute.route,
                            routeInfo = "route-5"
                        ),
                        Transaction(
                            id = "release_mortgage",
                            titleRes = R.string.transaction_release_mortgage,
                            descriptionRes = R.string.transaction_release_mortgage_desc,
                            route = NavRoutes.ReleaseMortgageRoute.route,
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

