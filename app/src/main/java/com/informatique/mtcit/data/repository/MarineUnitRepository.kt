package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.shared.MarineUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/**
 * Repository for marine unit data operations
 * All methods will eventually call backend APIs
 */
interface MarineUnitRepository {
    /**
     * Get all marine units belonging to a user
     */
    suspend fun getUserMarineUnits(userId: String): List<MarineUnit>

    /**
     * ✅ NEW: Load ships for a specific owner (individual or company)
     * Called explicitly when user selects type and presses Next
     * @param requestTypeId Transaction ID to filter ships (e.g., 7 for temp cert, 8 for permanent)
     */
    suspend fun loadShipsForOwner(
        ownerCivilId: String?,
        commercialRegNumber: String?,
        requestTypeId: String? = null
    ): List<MarineUnit>

    /**
     * 🔒 NEW: Load ONLY mortgaged ships for owner (for Release Mortgage transaction)
     * Uses dedicated API endpoint: GET /ship/{ownerId}/owner-mortgaged-ships
     * @param ownerId The owner ID (civil ID or commercial registration number)
     */
    suspend fun loadMortgagedShipsForOwner(ownerId: String): List<MarineUnit>

    /**
     * Get the current status of a marine unit
     * Returns: ACTIVE, SUSPENDED, CANCELLED
     */
    suspend fun getUnitStatus(unitId: String): String

    /**
     * Get the registration type of a marine unit
     * Returns: PERMANENT, TEMPORARY
     */
    suspend fun getRegistrationType(unitId: String): String

    /**
     * Verify if user owns a specific marine unit
     */
    suspend fun verifyOwnership(unitId: String, userId: String): Boolean

    /**
     * Check if marine unit has been inspected
     * Backend API: /api/marine-units/{unitId}/inspection-status
     * Returns inspection status with details
     */
    suspend fun getInspectionStatus(unitId: String): InspectionStatus

    /**
     * ✅ NEW: Get fishing boat data from Ministry of Agriculture
     * Backend API: /api/agriculture/fishing-boat?requestNumber={requestNumber}
     * Returns complete fishing boat data for auto-filling form
     */
    suspend fun getFishingBoatData(requestNumber: String): Result<FishingBoatData>

    /**
     * ✅ NEW: Send transaction request (for review step)
     * Calls marineUnitsApiService.sendTransactionRequest internally
     *
     * @param endpoint The API endpoint (e.g., "temporary-registration")
     * @param requestId The registration request ID
     * @param transactionType The transaction type name for logging
     * @return Result with ReviewResponse containing message and needInspection flag
     */
    suspend fun sendTransactionRequest(
        endpoint: String,
        requestId: Int,
        transactionType: String,
        sendRequestPostOrPut: String
    ): Result<com.informatique.mtcit.business.transactions.shared.ReviewResponse>
}


/**
 * Data class for inspection status
 */
data class InspectionStatus(
    val isInspected: Boolean,
    val inspectionDate: String? = null,
    val inspectionType: String? = null, // "SAFETY", "TECHNICAL", "ENVIRONMENTAL", etc.
    val inspectorName: String? = null,
    val certificateNumber: String? = null,
    val expiryDate: String? = null,
    val status: String? = null, // "VALID", "EXPIRED", "PENDING"
    val remarks: String? = null
)

/**
 * ✅ NEW: Fishing boat data from Ministry of Agriculture
 */
data class FishingBoatData(
    // Unit Selection Data
    val unitType: String,
    val unitClassification: String,
    val callSign: String,
    val imoNumber: String?,
    val registrationPort: String,
    val mmsi: String?,
    val manufacturerYear: String,
    val maritimeActivity: String,
    val buildingDock: String?,
    val constructionPool: String?,
    val buildingMaterial: String?,
    val constructionStartDate: String?,
    val constructionEndDate: String?,
    val buildingCountry: String?,
    val firstRegistrationDate: String?,
    val registrationCountry: String?,

    // Dimensions
    val overallLength: String,
    val overallWidth: String,
    val depth: String,
    val height: String?,
    val decksCount: String?,

    // Weights
    val grossTonnage: String,
    val netTonnage: String,
    val staticLoad: String?,
    val maxPermittedLoad: String?,

    // Owner Info (Single Owner - Primary Owner)
    val ownerFullNameAr: String,
    val ownerFullNameEn: String?,
    val ownerNationality: String,
    val ownerIdNumber: String,
    val ownerPassportNumber: String?,
    val ownerMobile: String,
    val ownerEmail: String?,
    val ownerAddress: String?,
    val ownerCity: String?,
    val ownerCountry: String,
    val ownerPostalCode: String?,

    // ✅ NEW: Multiple Owners Support (JSON array of owners)
    val owners: List<OwnerData>? = null,
    val totalOwnersCount: String? = null,

    // ✅ NEW: Engine Information (JSON array of engines)
    val engines: List<EngineData>? = null
)

/**
 * ✅ Owner data structure (matches OwnerListManager)
 */
@Serializable
data class OwnerData(
    val ownerFullNameAr: String = "",
    val ownerFullNameEn: String = "",
    val ownerNationality: String = "",
    val ownerIdNumber: String = "",
    val ownerPassportNumber: String = "",
    val ownerMobile: String = "",
    val ownerEmail: String = "",
    val ownerAddress: String = "",
    val ownerCity: String = "",
    val ownerCountry: String = "",
    val ownerPostalCode: String = "",
    val ownershipPercentage: String = "",
    val companyName: String = "",
    val companyRegistrationNumber: String = ""
)

/**
 * ✅ Engine data structure (matches EngineListManager)
 */
@Serializable
data class EngineData(
    val engineNumber: String = "",
    val engineType: String = "",
    val enginePower: String = "",
    val cylindersCount: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val manufactureYear: String = "",
    val producingCountry: String = "",
    val fuelType: String = "",
    val engineCondition: String = ""
)

@Singleton
class MarineUnitRepositoryImpl @Inject constructor(
    private val apiService: com.informatique.mtcit.data.api.MarineUnitsApiService
) : MarineUnitRepository {

    override suspend fun getUserMarineUnits(userId: String): List<MarineUnit> {
        // Call the API service to get ships for the user
        // If userId is empty, pass null to let the API use the authenticated user's civilId
        val civilId = if (userId.isEmpty()) null else userId
        // Note: stepActive must be true for the call to happen. useTestCivilId allows fixed test ID.
        return apiService.getMyShips(ownerCivilId = civilId, stepActive = false, useTestCivilId = false).getOrElse {
            println("⚠️ Failed to fetch ships from API: ${it.message}")
            emptyList()
        }
    }

    /**
     * ✅ NEW: Load ships explicitly when user selects type and presses Next
     * For testing: uses fixed civil id "12345678"
     * @param requestTypeId Transaction ID to filter ships based on transaction type
     */
    override suspend fun loadShipsForOwner(
        ownerCivilId: String?,
        commercialRegNumber: String?,
        requestTypeId: String?
    ): List<MarineUnit> {
        println("🚢 loadShipsForOwner called with ownerCivilId=$ownerCivilId, commercialRegNumber=$commercialRegNumber, requestTypeId=$requestTypeId")

        // ✅ For testing: use fixed test ID "12345678" for both person types
        // The API always requires ownerId, even for companies
        val testCivilId = "12345678"

        return when {
            // Company: send BOTH ownerCivilId AND commercialRegNumber
            !commercialRegNumber.isNullOrBlank() -> {
                println("✅ Loading ships for COMPANY with commercialRegNumber=$commercialRegNumber, ownerCivilId=$ownerCivilId")
                apiService.getMyShips(
                    ownerCivilId = ownerCivilId ?: testCivilId, // ✅ Send owner civil ID (required by API)
                    commercialRegNumber = commercialRegNumber,
                    requestTypeId = requestTypeId, // ✅ Pass transaction ID
                    stepActive = true,
                    useTestCivilId = false
                )
            }
            // Individual: use owner civil ID only
            !ownerCivilId.isNullOrBlank() -> {
                println("✅ Loading ships for INDIVIDUAL with ownerCivilId=$ownerCivilId")
                apiService.getMyShips(
                    ownerCivilId = ownerCivilId,
                    commercialRegNumber = null, // ✅ Don't send commercial reg for individuals
                    requestTypeId = requestTypeId, // ✅ Pass transaction ID
                    stepActive = true,
                    useTestCivilId = false
                )
            }
            // Fallback for testing: use test civil ID
            else -> {
                println("✅ Loading ships with TEST civil ID (fallback)")
                apiService.getMyShips(
                    ownerCivilId = testCivilId,
                    commercialRegNumber = null,
                    requestTypeId = requestTypeId, // ✅ Pass transaction ID
                    stepActive = true,
                    useTestCivilId = true
                )
            }
        }.getOrElse {
            println("⚠️ Failed to fetch ships from API: ${it.message}")
            emptyList()
        }
    }

    /**
     * 🔒 NEW: Load ONLY mortgaged ships for Release Mortgage transaction
     * Uses dedicated API: GET /ship/{ownerId}/owner-mortgaged-ships
     */
    override suspend fun loadMortgagedShipsForOwner(ownerId: String): List<MarineUnit> {
        println("🔒 loadMortgagedShipsForOwner called with ownerId=$ownerId")
        println("📡 Using dedicated mortgaged ships API endpoint")

        return apiService.getMortgagedShips(ownerId).getOrElse {
            println("⚠️ Failed to fetch mortgaged ships from API: ${it.message}")
            emptyList()
        }
    }

    override suspend fun getUnitStatus(unitId: String): String {
        // TODO: Replace with actual API call
        // Example: return apiService.getUnitStatus(unitId).status

        // Mock: All units active
        return "ACTIVE" // ACTIVE, SUSPENDED, CANCELLED
    }

    override suspend fun getRegistrationType(unitId: String): String {
        // TODO: Replace with actual API call
        // Example: return apiService.getRegistrationDetails(unitId).type

        // Mock: All units have permanent registration
        return "PERMANENT"
    }

    override suspend fun verifyOwnership(unitId: String, userId: String): Boolean {
        // TODO: Replace with actual API call
        // Example: return apiService.verifyOwnership(unitId, userId).isOwned

        // Mock: User owns units 1, 3, 4, 5, 6
        // Unit 2 (السلام البحري) is NOT owned - will show "not owned" error
        return unitId in listOf("1", "3", "4", "5", "6")
    }

    override suspend fun getInspectionStatus(unitId: String): InspectionStatus {
        // TODO: Replace with actual API call
        // Example: return apiService.getInspectionStatus(unitId)

        // Mock: Different inspection statuses for testing
        return when (unitId) {
            "6" -> {
                // Unit 6 - Under Verification (PENDING)
                InspectionStatus(
                    isInspected = false,
                    inspectionDate = null,
                    inspectionType = null,
                    inspectorName = null,
                    certificateNumber = null,
                    expiryDate = null,
                    status = "PENDING",
                    remarks = "الطلب قيد المراجعة من قبل الفريق الفني"
                )
            }
            "5" -> {
                // Unit 5 - Not Verified (Rejected or Not Submitted)
                InspectionStatus(
                    isInspected = false,
                    inspectionDate = null,
                    inspectionType = null,
                    inspectorName = null,
                    certificateNumber = null,
                    expiryDate = null,
                    status = "NOT_VERIFIED",
                    remarks = "لم يتم تقديم طلب الفحص بعد"
                )
            }
            "2" -> {
                // Unit 2 - Expired Inspection
                InspectionStatus(
                    isInspected = false,
                    inspectionDate = "2023-01-10",
                    inspectionType = "SAFETY",
                    inspectorName = "مدقق خارجي",
                    certificateNumber = "CERT-OLD-123",
                    expiryDate = "2024-01-10",
                    status = "EXPIRED",
                    remarks = "انتهت صلاحية شهادة الفحص"
                )
            }
            else -> {
                // All other units - Verified and Valid (units 1, 3, 4)
                InspectionStatus(
                    isInspected = true,
                    inspectionDate = "2024-01-10",
                    inspectionType = "SAFETY",
                    inspectorName = "مدقق خارجي",
                    certificateNumber = "CERT-123456",
                    expiryDate = "2025-01-10",
                    status = "VALID",
                    remarks = "لا توجد ملاحظات"
                )
            }
        }
    }

    /**
     * ✅ NEW: Fetch fishing boat data from Ministry of Agriculture
     * Simulated API call - returns mock data like other methods
     */
    override suspend fun getFishingBoatData(requestNumber: String): Result<FishingBoatData> {
        return try {
            // Simulate API delay
            kotlinx.coroutines.delay(1000)

            println("🔍 Simulating Ministry of Agriculture API call for request: $requestNumber")

            // Simulate different responses based on request number
            when {
                requestNumber == "12345" -> {
                    // Success case - return mock fishing boat data with engines and owners
                    val mockData = FishingBoatData(
                        // Unit Selection Data
                        unitType = "قارب صيد",
                        unitClassification = "قارب صغير",
                        callSign = "FB12345",
                        imoNumber = null,
                        registrationPort = "ميناء صحار",
                        mmsi = "461234567",
                        manufacturerYear = "2020",
                        maritimeActivity = "صيد تجاري",
                        buildingDock = "ترسانة صحار",
                        constructionPool = "حوض البناء رقم 3",
                        buildingMaterial = "فايبر جلاس",
                        constructionStartDate = "2020-01-15",
                        constructionEndDate = "2020-06-20",
                        buildingCountry = "عمان",
                        firstRegistrationDate = "2020-07-01",
                        registrationCountry = "عمان",

                        // Dimensions
                        overallLength = "18.5",
                        overallWidth = "5.2",
                        depth = "2.1",
                        height = "6.5",
                        decksCount = "1",

                        // Weights
                        grossTonnage = "45.5",
                        netTonnage = "38.2",
                        staticLoad = "20.0",
                        maxPermittedLoad = "50.0",

                        // Primary Owner Info
                        ownerFullNameAr = "أحمد بن محمد الحارثي",
                        ownerFullNameEn = "Ahmed Mohammed Al Harthi",
                        ownerNationality = "عمان",
                        ownerIdNumber = "12345678",
                        ownerPassportNumber = null,
                        ownerMobile = "+96891234567",
                        ownerEmail = "ahmed.alharthi@example.om",
                        ownerAddress = "ولاية صحار، شارع الوادي الكبير",
                        ownerCity = "صحار",
                        ownerCountry = "عمان",
                        ownerPostalCode = "321",

                        // ✅ Multiple Owners (2 owners)
                        totalOwnersCount = "2",
                        owners = listOf(
                            OwnerData(
                                ownerFullNameAr = "أحمد بن محمد الحارثي",
                                ownerFullNameEn = "Ahmed Mohammed Al Harthi",
                                ownerNationality = "عمان",
                                ownerIdNumber = "12345678",
                                ownerPassportNumber = "",
                                ownerMobile = "+96891234567",
                                ownerEmail = "ahmed.alharthi@example.om",
                                ownerAddress = "ولاية صحار، شارع الوادي الكبير",
                                ownerCity = "صحار",
                                ownerCountry = "عمان",
                                ownerPostalCode = "321",
                                ownershipPercentage = "60",
                                companyName = "",
                                companyRegistrationNumber = ""
                            ),
                            OwnerData(
                                ownerFullNameAr = "سالم بن خميس البلوشي",
                                ownerFullNameEn = "Salem Khamis Al Balushi",
                                ownerNationality = "عمان",
                                ownerIdNumber = "87654321",
                                ownerPassportNumber = "",
                                ownerMobile = "+96892345678",
                                ownerEmail = "salem.balushi@example.om",
                                ownerAddress = "ولاية صحار، حي النهضة",
                                ownerCity = "صحار",
                                ownerCountry = "عمان",
                                ownerPostalCode = "321",
                                ownershipPercentage = "40",
                                companyName = "",
                                companyRegistrationNumber = ""
                            )
                        ),

                        // ✅ Engines (2 engines)
                        engines = listOf(
                            EngineData(
                                engineNumber = "ENG001",
                                engineType = "Diesel Marine",
                                enginePower = "250",
                                cylindersCount = "6",
                                manufacturer = "Caterpillar",
                                model = "C7.1",
                                manufactureYear = "2019",
                                producingCountry = "الولايات المتحدة",
                                fuelType = "Diesel",
                                engineCondition = "Used - Good"
                            ),
                            EngineData(
                                engineNumber = "ENG002",
                                engineType = "Diesel Marine",
                                enginePower = "250",
                                cylindersCount = "6",
                                manufacturer = "Caterpillar",
                                model = "C7.1",
                                manufactureYear = "2019",
                                producingCountry = "الولايات المتحدة",
                                fuelType = "Diesel",
                                engineCondition = "Used - Good"
                            )
                        )
                    )

                    println("✅ Mock data returned successfully with ${mockData.engines?.size ?: 0} engines and ${mockData.owners?.size ?: 0} owners")
                    Result.success(mockData)
                }
                requestNumber == "99999" -> {
                    // Error case - not found
                    println("❌ Request number not found")
                    Result.failure(Exception("رقم الطلب غير موجود في نظام وزارة الزراعة"))
                }
                requestNumber.length < 5 -> {
                    // Validation error
                    println("❌ Invalid request number")
                    Result.failure(Exception("رقم الطلب غير صحيح"))
                }
                else -> {
                    // Generic mock data for any other valid request number
                    val genericData = FishingBoatData(
                        unitType = "قارب صيد",
                        unitClassification = "قارب متوسط",
                        callSign = "FB${requestNumber.take(5)}",
                        imoNumber = null,
                        registrationPort = "ميناء مسقط",
                        mmsi = "461${requestNumber.take(6)}",
                        manufacturerYear = "2021",
                        maritimeActivity = "صيد ساحلي",
                        buildingDock = "ترسانة محلية",
                        constructionPool = "حوض البناء رقم 1",
                        buildingMaterial = "خشب",
                        constructionStartDate = "2021-03-01",
                        constructionEndDate = "2021-08-15",
                        buildingCountry = "عمان",
                        firstRegistrationDate = "2021-09-01",
                        registrationCountry = "عمان",

                        // Dimensions
                        overallLength = "15.0",
                        overallWidth = "4.5",
                        depth = "1.8",
                        height = "5.0",
                        decksCount = "1",

                        // Weights
                        grossTonnage = "30.0",
                        netTonnage = "25.0",
                        staticLoad = "15.0",
                        maxPermittedLoad = "35.0",

                        // Owner Info
                        ownerFullNameAr = "محمد بن سعيد البلوشي",
                        ownerFullNameEn = "Mohammed Said Al Balushi",
                        ownerNationality = "عمان",
                        ownerIdNumber = "87654321",
                        ownerPassportNumber = null,
                        ownerMobile = "+96899887766",
                        ownerEmail = "mohammed.balushi@example.om",
                        ownerAddress = "ولاية مسقط، روي",
                        ownerCity = "مسقط",
                        ownerCountry = "عمان",
                        ownerPostalCode = "100"
                    )

                    println("✅ Generic mock data returned")
                    Result.success(genericData)
                }
            }
        } catch (e: Exception) {
            println("❌ Exception occurred: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * ✅ NEW: Send transaction request (for review step)
     * Calls marineUnitsApiService.sendTransactionRequest internally
     *
     * @param endpoint The API endpoint (e.g., "temporary-registration")
     * @param requestId The registration request ID
     * @param transactionType The transaction type name for logging
     * @return Result with ReviewResponse containing message and needInspection flag
     */
    override suspend fun sendTransactionRequest(
        endpoint: String,
        requestId: Int,
        transactionType: String,
        sendRequestPostOrPut: String
    ): Result<com.informatique.mtcit.business.transactions.shared.ReviewResponse> {
        return try {
            // Simulate API call
            val response = apiService.sendTransactionRequest(endpoint, requestId, transactionType, sendRequestPostOrPut)

            // Log the transaction request
            println("📤 Transaction request sent: $transactionType (ID: $requestId)")

            // Return the response
            Result.success(response)
        } catch (e: Exception) {
            println("❌ Error sending transaction request: ${e.message}")
            Result.failure(e)
        }
    }

//    /**
//     * Mock data - will be replaced by API calls
//     */
//    private fun getMockMarineUnits(): List<MarineUnit> {
//        return listOf(
//            MarineUnit(
//                id = "1",
//                name = "الريادة البحرية",
//                type = "سفينة صيد",
//                imoNumber = "9990001",
//                callSign = "A9BC2",
//                maritimeId = "470123456",
//                registrationPort = "صحار",
//                activity = "صيد",
//                isOwned = true,
//                registrationStatus = "ACTIVE",
//                registrationType = "PERMANENT",
//                isMortgaged = false,
//                // Full details for compliance screen
//                totalLength = "25.5 متر",
//                totalWidth = "6.2 متر",
//                draft = "2.8 متر",
//                height = "8.5 متر",
//                numberOfDecks = "2",
//                totalCapacity = "150 طن",
//                containerCapacity = "-",
//                lengthBetweenPerpendiculars = "23.8 متر",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            ),
//            MarineUnit(
//                id = "2",
//                name = "السلام البحري",
//                type = "قارب نزهة",
//                imoNumber = "IMO9990002",
//                callSign = "C7DE4",
//                maritimeId = "470123458",
//                registrationPort = "صلالة",
//                activity = "نزهة",
//                isOwned = false, // Not owned by current user
//                registrationStatus = "ACTIVE",
//                registrationType = "PERMANENT",
//                isMortgaged = false,
//                // Full details
//                totalLength = "15.2 متر",
//                totalWidth = "4.5 متر",
//                draft = "1.8 متر",
//                height = "5.2 متر",
//                numberOfDecks = "1",
//                totalCapacity = "50 طن",
//                containerCapacity = "-",
//                lengthBetweenPerpendiculars = "14.0 متر",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            ),
//            MarineUnit(
//                id = "3",
//                name = "النجم الساطع",
//                type = "سفينة شحن",
//                imoNumber = "9990002",
//                callSign = "B8CD3",
//                maritimeId = "470123457",
//                registrationPort = "مسقط",
//                activity = "شحن دولي",
//                isOwned = true,
//                registrationStatus = "ACTIVE",
//                registrationType = "PERMANENT",
//                isMortgaged = true, // Already mortgaged
//                mortgageDetails = com.informatique.mtcit.business.transactions.shared.MortgageDetails(
//                    mortgageId = "MTG-2024-001",
//                    bankName = "بنك مسقط",
//                    startDate = "2024-01-15",
//                    endDate = "2029-01-15",
//                    amount = "50000 ر.ع"
//                ),
//                // Full details
//                totalLength = "85.3 متر",
//                totalWidth = "16.8 متر",
//                draft = "7.2 متر",
//                height = "22.5 متر",
//                numberOfDecks = "4",
//                totalCapacity = "2500 طن",
//                containerCapacity = "120 حاوية",
//                lengthBetweenPerpendiculars = "82.0 متر",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            ),
//            MarineUnit(
//                id = "4",
//                name = "البحار الهادئ",
//                type = "سفينة صيد",
//                imoNumber = "9990003",
//                callSign = "D6EF5",
//                maritimeId = "470123459",
//                registrationPort = "صحار",
//                activity = "صيد",
//                isOwned = true,
//                registrationStatus = "ACTIVE",
//                registrationType = "PERMANENT",
//                isMortgaged = false, // ✅ Available for mortgage - CAN PROCEED
//                // Full details
//                totalLength = "28.7 متر",
//                totalWidth = "7.1 متر",
//                draft = "3.2 متر",
//                height = "9.8 متر",
//                numberOfDecks = "2",
//                totalCapacity = "180 طن",
//                containerCapacity = "-",
//                lengthBetweenPerpendiculars = "26.5 متر",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            ),
//            // NEW: Additional test ships for different scenarios
//            MarineUnit(
//                id = "5",
//                name = "نسيم البحر",
//                type = "يخت سياحي",
//                imoNumber = "9990004",
//                callSign = "E8FG6",
//                maritimeId = "OMN000123", // ← Will trigger DEBTS scenario
//                registrationPort = "مسقط",
//                activity = "سياحة",
//                isOwned = true,
//                registrationStatus = "ACTIVE",
//                registrationType = "PERMANENT",
//                isMortgaged = false,
//                // Full details
//                totalLength = "42.5 متر",
//                totalWidth = "9.8 متر",
//                draft = "3.5 متر",
//                height = "15.2 متر",
//                numberOfDecks = "3",
//                totalCapacity = "80 طن",
//                containerCapacity = "-",
//                lengthBetweenPerpendiculars = "40.0 متر",
//                violationsCount = "0",
//                detentionsCount = "1",
//                amountDue = "2500 ريال",
//                paymentStatus = "غير مسدد"
//            ),
//            MarineUnit(
//                id = "6",
//                name = "الأمل الجديد",
//                type = "سفينة بضائع",
//                imoNumber = "9990005",
//                callSign = "F9GH7",
//                maritimeId = "OMN000999", // ← Will trigger DETENTION scenario
//                registrationPort = "صلالة",
//                activity = "شحن",
//                isOwned = true,
//                registrationStatus = "ACTIVE",
//                registrationType = "PERMANENT",
//                isMortgaged = false,
//                // Full details
//                totalLength = "95.0 متر",
//                totalWidth = "18.5 متر",
//                draft = "8.2 متر",
//                height = "25.0 متر",
//                numberOfDecks = "5",
//                totalCapacity = "3500 طن",
//                containerCapacity = "150 حاوية",
//                lengthBetweenPerpendiculars = "92.0 متر",
//                violationsCount = "0",
//                detentionsCount = "0",
//                amountDue = "0 ريال",
//                paymentStatus = "مسدد"
//            )
//        )
//    }
}
