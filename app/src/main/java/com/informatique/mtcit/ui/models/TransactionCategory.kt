package com.informatique.mtcit.ui.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.R
import com.informatique.mtcit.common.util.AppLanguage.isArabic

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
    val fees: String = if (isArabic) "10 ريال" else "10 OMR",
    // New: list of requirement descriptions to show in TransactionRequirementsScreen
    val requirements: Requirement? = null,
    // New: estimated duration string (shown in list and forwarded to requirements screen)
    val duration: String = if (isArabic) "4-1 أيام" else "1-4 Days",
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
                            fees = if (isArabic) "50 ر.ع" else "50 OMR",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "50 ريال" else "50 OMR",
                                        label = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                        ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = if (isArabic) "10 دقائق" else "10 Minutes",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المستندات المطلوبة" else "Required Documents",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "شهادة بناء السفينة او عقد بيع في حالة الشراء(الزامي)" else "Ship Construction Certificate or Sales Contract in Case of Purchase (Mandatory)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "مستندات المعاينة ستتم داخل دورة المعاينة" else "Inspection Documents Will Be Processed Within the Inspection Cycle",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   25 ر.ع   " else "   25 OMR   ") ,
                                                subTitle = null,
                                                value = if (isArabic) "25 ر.ع" else "25 OMR"
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   30  ر.ع   " else "   30 OMR   "),
                                                subTitle = null,
                                                value = if (isArabic) "30 ر.ع" else "30 OMR"
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "خطوات طلب الخدمة" else "Service Request Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "إدخال بيانات الوحدة البحرية" else "Enter Marine Unit Data",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "تسجيل الأبعاد الخاصة بالوحدة" else "Register Unit Dimensions",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إدخال الأوزان والحمولات المعتمدة" else "Enter Approved Weights and Tonnages",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "تعبئة بيانات المحركات الخاصة بالوحدة" else "Fill Engine Data for the Unit",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "شروط الخدمة" else "Service Conditions",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "مالك السفينة / الموكل من مالك السفينة" else "Ship Owner / Authorized by Ship Owner",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ان يكون مواطن عماني مقيم" else "Must Be an Omani Resident Citizen",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = if (isArabic) "10 دقائق" else "10 Minutes",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = "permanent_registration_form",
                            routeInfo = "route-2",
                            fees = if (isArabic) "50 ر.ع" else "50 OMR",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "50 ريال" else "50 OMR",
                                        label = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = if (isArabic) "10 دقائق" else "10 Minutes",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المستندات المطلوبة" else "Required Documents",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "مستندات المعاينة ستتم داخل دورة المعاينةشهادة بناء السفينة أو عقد بيع في حالة الشراء وشهادة شطب في حالة تغيير الجنسية للعلم العماني (الزامي)" else "Inspection documents will be within the inspection cycle. Construction certificate or sale contract for purchase, and cancellation certificate for flag change to Omani flag (Mandatory)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "مستندات المعاينة ستتم داخل دورة المعاينة" else "Inspection Documents Will Be Processed Within the Inspection Cycle",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "وثيقة التأمين سارية" else "Valid Insurance Policy",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ترخيص هيئة تنظيم الاتصالات" else "Telecommunications Regulatory Authority License",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ترخيص النشاط" else "Activity License",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "الملاك ونصيب كل مالك" else "Owners and Each Owner's Share",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "التفويض بالإدارة في حالة وجود أكثر من مالك" else "Management Authorization if More Than One Owner",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   25 ر.ع   " else "   25 OMR   ") ,
                                                subTitle = null,
                                                value = if (isArabic) "25 ر.ع" else "25 OMR"
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   30  ر.ع   " else "   30 OMR   "),
                                                subTitle = null,
                                                value = if (isArabic) "30 ر.ع" else "30 OMR"
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "خطوات طلب الخدمة" else "Service Request Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "تحديد نوع المستخدم" else "Define User Type",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "حدد السجل التجاري لنشاطك اختياري، بناء على الخطوة (1)" else "Select Commercial Registration for Your Activity (Optional, based on Step 1)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "التحقق من وجود شهادة تسجيل مؤقتة للوحدة البحرية" else "Verify Temporary Registration Certificate Exists for the Marine Unit",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إدخال بيانات الوحدة البحرية" else "Enter Marine Unit Data",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إدخال الأبعاد الخاصة بالوحدة" else "Enter Unit Dimensions",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "شروط الخدمة" else "Service Conditions",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "مالك السفينة / الموكل من مالك السفينة" else "Ship Owner / Authorized by Ship Owner",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ان تكون السفينة ليست من السفن المحظور استقبالها" else "The Ship Must Not Be on the Prohibited Reception List",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "أن يكون مواطن عماني / مقيم" else "Must Be an Omani Citizen / Resident",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "وجود ترخيص من هيئة تنظيم الاتصالات" else "Have a Telecommunications Regulatory Authority License",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "وجود وثيقة تأمين بحري ساري" else "Have a Valid Marine Insurance Policy",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "موافقة وزارة الثروة الزراعية السمكية ( سيتم جلب الموافقة في حالة عدم وجودها )" else "Ministry of Agricultural Wealth and Fisheries Approval (Will be fetched if not present)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "وجود معاينة ( سيتم التحويل لدورة المعاينات في حالة عدم وجود معاينة )" else "Inspection Required (Will be transferred to inspection cycle if no inspection exists)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ترخيص مزاولة نشاط في حالة سفينة خدمة طبقا لنوع الخدمة" else "Activity License for Service Ships According to Service Type",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = if (isArabic) "5-1 أيام" else "1-5 Days",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
                        ),
                        Transaction(
                            id = "permanent_registration_certificate",
                            titleRes = R.string.transaction_permanent_registration_certificate,
                            descriptionRes = R.string.transaction_permanent_registration_certificate_desc,
                            route = NavRoutes.PermanentRegistrationRoute.route,
                            routeInfo = "route-2",
                            duration = if (isArabic) "10 دقائق" else "10 Minutes",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents" ,if (isArabic) "استلام" else "Receive")
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
                            routeInfo = "route-5",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "20 ريال" else "20 OMR",
                                        label = if (isArabic) "الرسوم" else "Fees"

                                    ),
                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "5-1 أيام" else "1-5 Days",
                                        label = if (isArabic) "المدة الزمنية" else "Duration"
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات" else "Requirements",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية" else "Copy of Identity Card",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)" else "Ship Photo (New and Clear)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم" else "Fee Payment Receipt",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "المتطلبات 2" else "Requirements 2",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية2" else "Copy of Identity Card 2",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)2" else "Ship Photo (New and Clear) 2",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم2" else "Fee Payment Receipt 2",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات 3" else "Requirements 3",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية 3" else "Copy of Identity Card 3",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة) 3" else "Ship Photo (New and Clear) 3",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم 3" else "Fee Payment Receipt 3",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                )
                            ),
                            duration = if (isArabic) "5-1 أيام" else "1-5 Days",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
                        ),
                        Transaction(
                            id = "release_mortgage",
                            titleRes = R.string.transaction_release_mortgage,
                            descriptionRes = R.string.transaction_release_mortgage_desc,
                            route = NavRoutes.ReleaseMortgageRoute.route,
                            routeInfo = "route-6",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "20 ريال" else "20 OMR",
                                        label = if (isArabic) "الرسوم" else "Fees"
                                    ),
                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "5-1 أيام" else "1-5 Days",
                                        label = if (isArabic) "المدة الزمنية" else "Duration"
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات" else "Requirements",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية" else "Copy of Identity Card",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)" else "Ship Photo (New and Clear)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم" else "Fee Payment Receipt",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "المتطلبات 2" else "Requirements 2",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية2" else "Copy of Identity Card 2",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)2" else "Ship Photo (New and Clear) 2",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم2" else "Fee Payment Receipt 2",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات 3" else "Requirements 3",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية 3" else "Copy of Identity Card 3",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة) 3" else "Ship Photo (New and Clear) 3",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم 3" else "Fee Payment Receipt 3",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                )
                            ),
                            duration = if (isArabic) "5-1 أيام" else "1-5 Days",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
                        )
                    )
                ),
                SubCategory(
                    id = "marine_unit_request_for_inspection",
                    titleRes = R.string.subcategory_marine_unit_request_for_inspection,
                    parentCategoryId = "registration_department",
                    transactions = listOf(
                        Transaction(
                            id = "request_for_inspection",
                            titleRes = R.string.request_for_inspection_title,
                            descriptionRes = R.string.request_for_inspection_des,
                            route = NavRoutes.RequestForInspection.route,
                            routeInfo = "route-3",
                            fees = if (isArabic) "50 ر.ع" else "50 OMR",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "50 ريال" else "50 OMR",
                                        label = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = if (isArabic) "10 دقائق" else "10 Minutes",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المستندات المطلوبة" else "Required Documents",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "شهادة بناء السفينة او عقد بيع في حالة الشراء(الزامي)" else "Ship Construction Certificate or Sales Contract in Case of Purchase (Mandatory)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "مستندات المعاينة ستتم داخل دورة المعاينة" else "Inspection Documents Will Be Processed Within the Inspection Cycle",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   25 ر.ع   " else "   25 OMR   ") ,
                                                subTitle = null,
                                                value = if (isArabic) "25 ر.ع" else "25 OMR"
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   30  ر.ع   " else "   30 OMR   "),
                                                subTitle = null,
                                                value = if (isArabic) "30 ر.ع" else "30 OMR"
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "خطوات طلب الخدمة" else "Service Request Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "إدخال بيانات الوحدة البحرية" else "Enter Marine Unit Data",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "تسجيل الأبعاد الخاصة بالوحدة" else "Register Unit Dimensions",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إدخال الأوزان والحمولات المعتمدة" else "Enter Approved Weights and Tonnages",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "تعبئة بيانات المحركات الخاصة بالوحدة" else "Fill Engine Data for the Unit",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "شروط الخدمة" else "Service Conditions",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "مالك السفينة / الموكل من مالك السفينة" else "Ship Owner / Authorized by Ship Owner",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ان يكون مواطن عماني مقيم" else "Must Be an Omani Resident Citizen",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = if (isArabic) "10 دقائق" else "10 Minutes",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
                        ),
                        Transaction(
                            id = "request_for_inspection",
                            titleRes = R.string.request_for_inspection_title,
                            descriptionRes = R.string.request_for_inspection_des,
                            route = NavRoutes.RequestForInspection.route,
                            routeInfo = "route-3",
                            fees = if (isArabic) "50 ر.ع" else "50 OMR",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        icon = Icons.Default.MonetizationOn,
                                        value = if (isArabic) "50 ريال" else "50 OMR",
                                        label = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = if (isArabic) "10 دقائق" else "10 Minutes",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المستندات المطلوبة" else "Required Documents",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "شهادة بناء السفينة او عقد بيع في حالة الشراء(الزامي)" else "Ship Construction Certificate or Sales Contract in Case of Purchase (Mandatory)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "مستندات المعاينة ستتم داخل دورة المعاينة" else "Inspection Documents Will Be Processed Within the Inspection Cycle",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   25 ر.ع   " else "   25 OMR   ") ,
                                                subTitle = null,
                                                value = if (isArabic) "25 ر.ع" else "25 OMR"
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = (if (isArabic) "الرسوم اللازم دفعها للحصول على شهادة تسجيل مؤقتة" else "Fees Required to Obtain a Temporary Registration Certificate") + (if (isArabic) "   30  ر.ع   " else "   30 OMR   "),
                                                subTitle = null,
                                                value = if (isArabic) "30 ر.ع" else "30 OMR"
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "خطوات طلب الخدمة" else "Service Request Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "إدخال بيانات الوحدة البحرية" else "Enter Marine Unit Data",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "تسجيل الأبعاد الخاصة بالوحدة" else "Register Unit Dimensions",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إدخال الأوزان والحمولات المعتمدة" else "Enter Approved Weights and Tonnages",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "تعبئة بيانات المحركات الخاصة بالوحدة" else "Fill Engine Data for the Unit",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "شروط الخدمة" else "Service Conditions",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "مالك السفينة / الموكل من مالك السفينة" else "Ship Owner / Authorized by Ship Owner",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "ان يكون مواطن عماني مقيم" else "Must Be an Omani Resident Citizen",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = if (isArabic) "10 دقائق" else "10 Minutes",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
                        ),


                        )),

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
                            requirements = Requirement(
                                id = "issue_navigation_permit_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        value = if (isArabic) "20 ريال" else "20 OMR",
                                        label = if (isArabic) "الرسوم" else "Fees",
                                        icon = Icons.Default.MonetizationOn
                                    ),
                                    ServiceSummary(
                                        type = "steps",
                                        value = "4",
                                        label = if (isArabic) "الخطوات" else "Steps",
                                        icon = Icons.Default.MonetizationOn
                                    ),
                                    ServiceSummary(
                                        type = "duration",
                                        value = if (isArabic) "5-1 أيام" else "1-5 Days",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                        icon = Icons.Default.MonetizationOn
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات" else "Requirements",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية" else "Copy of Identity Card",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)" else "Ship Photo (New and Clear)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم" else "Fee Payment Receipt",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "خطوات الخدمة" else "Service Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "ملء النموذج" else "Fill the Form",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "رفع المستندات" else "Upload Documents",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "مراجعة الطلب" else "Review Request",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = if (isArabic) "استلام التصريح" else "Receive Permit",
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
                            routeInfo = "route-8",
                            requirements = Requirement(
                                id = "issue_navigation_permit_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        value = if (isArabic) "20 ريال" else "20 OMR",
                                        label = if (isArabic) "الرسوم" else "Fees",
                                        icon = Icons.Default.MonetizationOn
                                    ),
                                    ServiceSummary(
                                        type = "steps",
                                        value = "4",
                                        label = if (isArabic) "الخطوات" else "Steps",
                                        icon = Icons.Default.MonetizationOn
                                    ),
                                    ServiceSummary(
                                        type = "duration",
                                        value = if (isArabic) "5-1 أيام" else "1-5 Days",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                        icon = Icons.Default.MonetizationOn
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات" else "Requirements",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية" else "Copy of Identity Card",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)" else "Ship Photo (New and Clear)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم" else "Fee Payment Receipt",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "خطوات الخدمة" else "Service Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "ملء النموذج" else "Fill the Form",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "رفع المستندات" else "Upload Documents",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "مراجعة الطلب" else "Review Request",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = if (isArabic) "استلام التصريح" else "Receive Permit",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            )
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
                                        value = if (isArabic) "50 ريال" else "50 OMR",
                                        label = if (isArabic) "رسوم الخدمة" else "Service Fees",
                                    ),

                                    ServiceSummary(
                                        type = "duration",
                                        icon = Icons.Default.Timer,
                                        value = if (isArabic) "10 دقائق" else "10 Minutes",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات" else "Requirements",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية" else "Copy of Identity Card",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)" else "Ship Photo (New and Clear)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم" else "Fee Payment Receipt",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "خطوات الخدمة" else "Service Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "ملء النموذج" else "Fill the Form",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "رفع المستندات" else "Upload Documents",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "مراجعة الطلب" else "Review Request",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = if (isArabic) "استلام التصريح" else "Receive Permit",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 3,
                                        title = if (isArabic) "خطوات الخدمة" else "Service Steps",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "ملء النموذج" else "Fill the Form",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "رفع المستندات" else "Upload Documents",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "مراجعة الطلب" else "Review Request",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 4,
                                                stepNo = 4,
                                                title = if (isArabic) "استلام التصريح" else "Receive Permit",
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
                            routeInfo = "route-4",
                            fees = if (isArabic) "20 ريال" else "20 OMR",
                            requirements = Requirement(
                                id = "temporary_registration_certificate_id",
                                serviceSummaryList = listOf(
                                    ServiceSummary(
                                        type = "fee",
                                        value = if (isArabic) "20 ريال" else "20 OMR",
                                        label = if (isArabic) "الرسوم" else "Fees",
                                        icon = Icons.Default.Timer,
                                    ),
                                    ServiceSummary(
                                        type = "duration",
                                        value = if (isArabic) "5-1 أيام" else "1-5 Days",
                                        label = if (isArabic) "المدة الزمنية" else "Duration",
                                        icon = Icons.Default.Timer,
                                    )
                                ),
                                serviceInfoList = listOf(
                                    ServiceInfo(
                                        id = 1,
                                        title = if (isArabic) "المتطلبات" else "Requirements",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية" else "Copy of Identity Card",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)" else "Ship Photo (New and Clear)",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم" else "Fee Payment Receipt",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    ),
                                    ServiceInfo(
                                        id = 2,
                                        title = if (isArabic) "المتطلبات 2" else "Requirements 2",
                                        data = listOf(
                                            ServiceInfoSteps(
                                                id = 1,
                                                stepNo = 1,
                                                title = if (isArabic) "نسخة من بطاقة الهوية2" else "Copy of Identity Card 2",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 2,
                                                stepNo = 2,
                                                title = if (isArabic) "صورة للسفينة (جديدة وواضحة)2" else "Ship Photo (New and Clear) 2",
                                                subTitle = null,
                                                value = null
                                            ),
                                            ServiceInfoSteps(
                                                id = 3,
                                                stepNo = 3,
                                                title = if (isArabic) "إيصال دفع الرسوم2" else "Fee Payment Receipt 2",
                                                subTitle = null,
                                                value = null
                                            )
                                        )
                                    )
                                )
                            ),
                            duration = if (isArabic) "3-1 أيام" else "1-3 Days",
                            steps = listOf(if (isArabic) "ملء النموذج" else "Fill the Form", if (isArabic) "رفع المستندات" else "Upload Documents", if (isArabic) "مراجعة الطلب" else "Review Request", if (isArabic) "استلام" else "Receive")
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

