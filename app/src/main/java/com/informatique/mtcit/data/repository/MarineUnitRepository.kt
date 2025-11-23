package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.shared.MarineUnit
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class MarineUnitRepositoryImpl @Inject constructor(
    // TODO: Inject API service when available
    // private val apiService: MarineUnitApiService
) : MarineUnitRepository {

    override suspend fun getUserMarineUnits(userId: String): List<MarineUnit> {
        // TODO: Replace with actual API call
        // Example: return apiService.getUserMarineUnits(userId).map { it.toDomain() }

        // Mock data for demonstration
        return getMockMarineUnits()
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
     * Mock data - will be replaced by API calls
     */
    private fun getMockMarineUnits(): List<MarineUnit> {
        return listOf(
            MarineUnit(
                id = "1",
                name = "الريادة البحرية",
                type = "سفينة صيد",
                imoNumber = "9990001",
                callSign = "A9BC2",
                maritimeId = "470123456",
                registrationPort = "صحار",
                activity = "صيد",
                isOwned = true,
                registrationStatus = "ACTIVE",
                registrationType = "PERMANENT",
                isMortgaged = false,
                // Full details for compliance screen
                totalLength = "25.5 متر",
                totalWidth = "6.2 متر",
                draft = "2.8 متر",
                height = "8.5 متر",
                numberOfDecks = "2",
                totalCapacity = "150 طن",
                containerCapacity = "-",
                lengthBetweenPerpendiculars = "23.8 متر",
                violationsCount = "0",
                detentionsCount = "0",
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
            ),
            MarineUnit(
                id = "2",
                name = "السلام البحري",
                type = "قارب نزهة",
                imoNumber = "IMO9990002",
                callSign = "C7DE4",
                maritimeId = "470123458",
                registrationPort = "صلالة",
                activity = "نزهة",
                isOwned = false, // Not owned by current user
                registrationStatus = "ACTIVE",
                registrationType = "PERMANENT",
                isMortgaged = false,
                // Full details
                totalLength = "15.2 متر",
                totalWidth = "4.5 متر",
                draft = "1.8 متر",
                height = "5.2 متر",
                numberOfDecks = "1",
                totalCapacity = "50 طن",
                containerCapacity = "-",
                lengthBetweenPerpendiculars = "14.0 متر",
                violationsCount = "0",
                detentionsCount = "0",
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
            ),
            MarineUnit(
                id = "3",
                name = "النجم الساطع",
                type = "سفينة شحن",
                imoNumber = "9990002",
                callSign = "B8CD3",
                maritimeId = "470123457",
                registrationPort = "مسقط",
                activity = "شحن دولي",
                isOwned = true,
                registrationStatus = "ACTIVE",
                registrationType = "PERMANENT",
                isMortgaged = true, // Already mortgaged
                mortgageDetails = com.informatique.mtcit.business.transactions.shared.MortgageDetails(
                    mortgageId = "MTG-2024-001",
                    bankName = "بنك مسقط",
                    startDate = "2024-01-15",
                    endDate = "2029-01-15",
                    amount = "50000 ر.ع"
                ),
                // Full details
                totalLength = "85.3 متر",
                totalWidth = "16.8 متر",
                draft = "7.2 متر",
                height = "22.5 متر",
                numberOfDecks = "4",
                totalCapacity = "2500 طن",
                containerCapacity = "120 حاوية",
                lengthBetweenPerpendiculars = "82.0 متر",
                violationsCount = "0",
                detentionsCount = "0",
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
            ),
            MarineUnit(
                id = "4",
                name = "البحار الهادئ",
                type = "سفينة صيد",
                imoNumber = "9990003",
                callSign = "D6EF5",
                maritimeId = "470123459",
                registrationPort = "صحار",
                activity = "صيد",
                isOwned = true,
                registrationStatus = "ACTIVE",
                registrationType = "PERMANENT",
                isMortgaged = false, // ✅ Available for mortgage - CAN PROCEED
                // Full details
                totalLength = "28.7 متر",
                totalWidth = "7.1 متر",
                draft = "3.2 متر",
                height = "9.8 متر",
                numberOfDecks = "2",
                totalCapacity = "180 طن",
                containerCapacity = "-",
                lengthBetweenPerpendiculars = "26.5 متر",
                violationsCount = "0",
                detentionsCount = "0",
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
            ),
            // NEW: Additional test ships for different scenarios
            MarineUnit(
                id = "5",
                name = "نسيم البحر",
                type = "يخت سياحي",
                imoNumber = "9990004",
                callSign = "E8FG6",
                maritimeId = "OMN000123", // ← Will trigger DEBTS scenario
                registrationPort = "مسقط",
                activity = "سياحة",
                isOwned = true,
                registrationStatus = "ACTIVE",
                registrationType = "PERMANENT",
                isMortgaged = false,
                // Full details
                totalLength = "42.5 متر",
                totalWidth = "9.8 متر",
                draft = "3.5 متر",
                height = "15.2 متر",
                numberOfDecks = "3",
                totalCapacity = "80 طن",
                containerCapacity = "-",
                lengthBetweenPerpendiculars = "40.0 متر",
                violationsCount = "0",
                detentionsCount = "1",
                amountDue = "2500 ريال",
                paymentStatus = "غير مسدد"
            ),
            MarineUnit(
                id = "6",
                name = "الأمل الجديد",
                type = "سفينة بضائع",
                imoNumber = "9990005",
                callSign = "F9GH7",
                maritimeId = "OMN000999", // ← Will trigger DETENTION scenario
                registrationPort = "صلالة",
                activity = "شحن",
                isOwned = true,
                registrationStatus = "ACTIVE",
                registrationType = "PERMANENT",
                isMortgaged = false,
                // Full details
                totalLength = "95.0 متر",
                totalWidth = "18.5 متر",
                draft = "8.2 متر",
                height = "25.0 متر",
                numberOfDecks = "5",
                totalCapacity = "3500 طن",
                containerCapacity = "150 حاوية",
                lengthBetweenPerpendiculars = "92.0 متر",
                violationsCount = "0",
                detentionsCount = "0",
                amountDue = "0 ريال",
                paymentStatus = "مسدد"
            )
        )
    }
}
