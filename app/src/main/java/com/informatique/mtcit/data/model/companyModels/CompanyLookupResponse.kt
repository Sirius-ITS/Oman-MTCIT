package com.informatique.mtcit.data.model.companyModels

import com.google.gson.annotations.SerializedName

data class CompanyLookupResponse(
    @SerializedName("result")
    val result: CompanyResult? = null,
    @SerializedName("message")
    val message: String? = null
)

data class CompanyResult(
    @SerializedName("arabicCommercialName")
    val arabicCommercialName: String,
    @SerializedName("commercialRegistrationEntityType")
    val commercialRegistrationEntityType: String,
    @SerializedName("commercialRegistrationCode")
    val commercialRegistrationCode: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("creationDate")
    val creationDate: String,
    @SerializedName("expiryDate")
    val expiryDate: String,
    @SerializedName("companyStartDate")
    val companyStartDate: String,
    @SerializedName("branchesCount")
    val branchesCount: String,
    @SerializedName("addressPOBox")
    val addressPOBox: String,
    @SerializedName("telephone")
    val telephone: String,
    @SerializedName("companyCapital")
    val companyCapital: String,
    @SerializedName("humanPartners")
    val humanPartners: List<HumanPartner>? = null,
    @SerializedName("establishmentPartners")
    val establishmentPartners: List<EstablishmentPartner>? = null,
    @SerializedName("signatories")
    val signatories: List<Signatory>? = null,
    @SerializedName("activities")
    val activities: List<Activity>? = null,
    @SerializedName("statuses")
    val statuses: Status? = null,
    @SerializedName("branches")
    val branches: List<Branch>? = null,
    @SerializedName("status")
    val status: Boolean,
)

data class HumanPartner(
    @SerializedName("nameAr")
    val nameAr: String,
    @SerializedName("nationality")
    val nationality: String,
    @SerializedName("nIN")
    val nIN: String,
    @SerializedName("percentage")
    val percentage: String,
    @SerializedName("nINType")
    val nINType: NINType,
    @SerializedName("partnerType")
    val partnerType: PartnerType
)

data class EstablishmentPartner(
    @SerializedName("commercialNameAr")
    val commercialNameAr: String,
    @SerializedName("nationality")
    val nationality: String,
    @SerializedName("commercialRegistrationCode")
    val commercialRegistrationCode: String,
    @SerializedName("percentage")
    val percentage: String,
    @SerializedName("partnerType")
    val partnerType: PartnerType
)

data class Signatory(
    @SerializedName("nameAr")
    val nameAr: String,
    @SerializedName("nationality")
    val nationality: String,
    @SerializedName("nIN")
    val nIN: String,
    @SerializedName("nINType")
    val nINType: NINType,
    @SerializedName("partnerType")
    val partnerType: PartnerType
)

data class Activity(
    @SerializedName("title")
    val title: String,
    @SerializedName("activitySerial")
    val activitySerial: String,
    @SerializedName("cost")
    val cost: String
)

data class NINType(
    @SerializedName("Code")
    val code: String,
    @SerializedName("description")
    val description: String
)

data class PartnerType(
    @SerializedName("code")
    val code: String,
    @SerializedName("description")
    val description: String
)

data class Status(
    @SerializedName("code")
    val code: String,
    @SerializedName("description")
    val description: String
)

data class Branch(
    @SerializedName("nameAr")
    val nameAr: String,
    @SerializedName("serialNumber")
    val serialNumber: String,
    @SerializedName("statuses")
    val statuses: Status
)
