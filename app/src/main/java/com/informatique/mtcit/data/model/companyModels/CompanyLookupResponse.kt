package com.informatique.mtcit.data.model.companyModels

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class CompanyLookupResponse(
    @SerialName("result")
    val result: CompanyResult? = null,
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class CompanyResult(
    @SerialName("arabicCommercialName")
    val arabicCommercialName: String,
    @SerialName("commercialRegistrationEntityType")
    val commercialRegistrationEntityType: String,
    @SerialName("commercialRegistrationCode")
    val commercialRegistrationCode: String,
    @SerialName("location")
    val location: String,
    @SerialName("creationDate")
    val creationDate: String,
    @SerialName("expiryDate")
    val expiryDate: String,
    @SerialName("companyStartDate")
    val companyStartDate: String,
    @SerialName("branchesCount")
    val branchesCount: String,
    @SerialName("addressPOBox")
    val addressPOBox: String,
    @SerialName("telephone")
    val telephone: String,
    @SerialName("companyCapital")
    val companyCapital: String,
    @SerialName("humanPartners")
    val humanPartners: List<HumanPartner>? = null,
    @SerialName("establishmentPartners")
    val establishmentPartners: List<EstablishmentPartner>? = null,
    @SerialName("signatories")
    val signatories: List<Signatory>? = null,
    @SerialName("activities")
    val activities: List<Activity>? = null,
    @SerialName("statuses")
    val statuses: Status? = null,
    @SerialName("branches")
    val branches: List<Branch>? = null,
    @SerialName("status")
    val status: Boolean,
)

@Serializable
data class HumanPartner(
    @SerialName("nameAr")
    val nameAr: String,
    @SerialName("nationality")
    val nationality: String,
    @SerialName("nIN")
    val nIN: String,
    @SerialName("percentage")
    val percentage: String,
    @SerialName("nINType")
    val nINType: NINType,
    @SerialName("partnerType")
    val partnerType: PartnerType
)

@Serializable
data class EstablishmentPartner(
    @SerialName("commercialNameAr")
    val commercialNameAr: String,
    @SerialName("nationality")
    val nationality: String,
    @SerialName("commercialRegistrationCode")
    val commercialRegistrationCode: String,
    @SerialName("percentage")
    val percentage: String,
    @SerialName("partnerType")
    val partnerType: PartnerType
)

@Serializable
data class Signatory(
    @SerialName("nameAr")
    val nameAr: String,
    @SerialName("nationality")
    val nationality: String,
    @SerialName("nIN")
    val nIN: String,
    @SerialName("nINType")
    val nINType: NINType,
    @SerialName("partnerType")
    val partnerType: PartnerType
)

@Serializable
data class Activity(
    @SerialName("title")
    val title: String,
    @SerialName("activitySerial")
    val activitySerial: String,
    @SerialName("cost")
    val cost: String
)

@Serializable
data class NINType(
    @SerialName("Code")
    val code: String,
    @SerialName("description")
    val description: String
)

@Serializable
data class PartnerType(
    @SerialName("code")
    val code: String,
    @SerialName("description")
    val description: String
)

@Serializable
data class Status(
    @SerialName("code")
    val code: String,
    @SerialName("description")
    val description: String
)

@Serializable
data class Branch(
    @SerialName("nameAr")
    val nameAr: String,
    @SerialName("serialNumber")
    val serialNumber: String,
    @SerialName("statuses")
    val statuses: Status
)
