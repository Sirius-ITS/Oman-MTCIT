package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Payment Receipt Request for GET /payment endpoint
 * Filter is base64 encoded JSON: {"requestType": 4, "coreShipsInfoId": "230"}
 */
@Serializable
data class PaymentReceiptRequest(
    val requestType: Int,
    val coreShipsInfoId: String
)

/**
 * Payment Receipt Response from GET /payment endpoint
 */
@Serializable
data class PaymentReceipt(
    val receiptSerial: Int,
    val receiptYear: Int,
    val invoiceType: InvoiceType,
    val totalCost: Double,
    val totalTax: Double,
    val finalTotal: Double,
    val arabicValue: String,
    val approximateFinalTotal: Double,
    val createdByDisplayName: String? = null,
    val changerDisplayName: String? = null,
    val paymentReceiptDetailsList: List<PaymentReceiptDetail>
)

@Serializable
data class InvoiceType(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val code: String? = null,
    val requestType: Int? = null,
    val isActive: String? = null,
    val notes: String? = null,
    val tariffList: List<String> = emptyList()
)

@Serializable
data class PaymentReceiptDetail(
    val name: String,
    val taxValue: Double,
    val value: Double,
    val finalTotal: Double,
    val taxRate: String? = null,  // ✅ Make optional - API doesn't always return this
    val tariffItem: TariffItem,
    val tariffRate: TariffRate,
    val tariffIncremental: TariffIncremental? = null,
    val approximateFinalTotal: Double,
    val createdByDisplayName: String? = null,
    val changerDisplayName: String? = null
)

@Serializable
data class TariffItem(
    val id: Int,
    val nameAr: String? = null,
    val invoiceType: InvoiceType? = null,
    val isTax: String? = null,
    val taxRate: Int? = null,
    val calculationType: String? = null,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val timeStamp: String? = null,
    val changerId: String? = null,
    val tariffRateList: List<TariffRate> = emptyList(),
    val billingVariablesList: List<BillingVariable> = emptyList(),
    val isActive: String? = null,
    val createdByDisplayName: String? = null,
    val changerDisplayName: String? = null
)

@Serializable
data class TariffRate(
    val id: Int? = null,
    val tariffItemId: Int? = null,
    val expressionCode: String? = null,
    val expressionText: String? = null,
    // Some API responses use a numeric 'tariffRate' field instead of expression fields
    val tariffRate: Double? = null,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val timeStamp: String? = null,
    val changerId: String? = null,
    val createdByDisplayName: String? = null,
    val changerDisplayName: String? = null
)

@Serializable
data class BillingVariable(
    val id: Int,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val timeStamp: String? = null,
    val changerId: String? = null,
    val createdByDisplayName: String? = null,
    val changerDisplayName: String? = null
)

@Serializable
data class TariffIncremental(
    val id: Int? = null
)

/**
 * Core Ship DTO for payment submission
 */
@Serializable
data class CoreShipsDto(
    val id: Long,
    val shipName: String? = null,
    val imoNumber: Long? = null,
    val callSign: String? = null,
    val mmsiNumber: Long? = null,
    val officialNumber: String? = null,
    val portOfRegistry: PortOfRegistryDto? = null,
    val marineActivity: MarineActivityDto? = null,
    val shipCategory: ShipCategoryDto? = null,
    val shipType: ShipTypeDto? = null,
    val proofType: ProofTypeDto? = null,
    val buildCountry: BuildCountryDto? = null,
    val buildMaterial: BuildMaterialDto? = null,
    val shipBuildYear: Int? = null,
    val buildEndDate: String? = null,
    val shipYardName: String? = null,
    val grossTonnage: Double? = null,
    val netTonnage: Double? = null,
    val deadweightTonnage: Int? = null,
    val maxLoadCapacity: Int? = null,
    val firstRegistrationDate: String? = null,
    val requestSubmissionDate: String? = null,
    val isTemp: Int? = null,
    val nameExpiryDate: String? = null,
    val vesselLengthOverall: Int? = null,
    val vesselBeam: Int? = null,
    val vesselDraft: Int? = null,
    val vesselHeight: Int? = null,
    val decksNumber: Int? = null,
    val mafwReqNum: String? = null,
    val registrationNumber: String? = null
)

@Serializable
data class PortOfRegistryDto(val id: String)

@Serializable
data class MarineActivityDto(val id: Int)

@Serializable
data class ShipCategoryDto(val id: Int)

@Serializable
data class ShipTypeDto(val id: Int)

@Serializable
data class ProofTypeDto(val id: Int)

@Serializable
data class BuildCountryDto(val id: String)

@Serializable
data class BuildMaterialDto(val id: Int)

/**
 * Payment Submission Request for POST /add-payment endpoint
 */
@Serializable
data class PaymentSubmissionRequest(
    val id: Int? = null,
    val receiptSerial: Int,
    val receiptYear: Int,
    val requestId: Long? = null,
    val requestTypeId: String,
    val penalties: List<String> = emptyList(),
    val invoiceType: InvoiceTypeIdOnly,
    val receiptNo: Int = 0,
    val comments: String = "",
    val description: String = "",
    val isPaid: Int = 0,
    val arabicValue: String,
    val totalCost: Double,
    val totalTax: Double,
    val finalTotal: Double,
    val approximateFinalTotal: Double,
    val paymentReceiptDetailsList: List<PaymentReceiptDetailSubmission>
)

@Serializable
data class InvoiceTypeIdOnly(val id: Int)

@Serializable
data class PaymentReceiptDetailSubmission(
    val name: String,
    val value: Double,
    val taxValue: Double,
    val finalTotal: Double,
    val approximateFinalTotal: Double,
    val tariffItem: TariffItemIdOnly,
    val tariffRate: TariffRateSubmission
)

@Serializable
data class TariffItemIdOnly(
    val id: Int,
    val name: String
)

@Serializable
data class TariffRateSubmission(
    val id: Int,
    val tariffItemId: Int,
    val expressionCode: String,
    val expressionText: String
)

/**
 * Payment API Response wrapper
 */
@Serializable
data class PaymentResponse<T>(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: T
)

/**
 * ✅ NEW: Simple payment submission request for addPayment API
 * Used when user clicks Pay button on payment details step
 */
@Serializable
data class SimplePaymentRequest(
    val requestType: Int,
    val requestId: Int,
    val coreShipsInfoId: String
)
