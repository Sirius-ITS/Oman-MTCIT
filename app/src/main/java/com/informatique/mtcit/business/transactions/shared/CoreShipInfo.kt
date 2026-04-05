package com.informatique.mtcit.business.transactions.shared

/**
 * Full ship details returned by GET /coreshipinfo/ship/{id}
 * Used when user clicks "عرض جميع البيانات" in the marine unit selector.
 */
data class CoreShipInfo(
    val shipInfoId: Int = 0,
    val shipName: String = "",
    val imoNumber: String = "",             // رقم IMO
    val callSign: String = "",
    val officialNumber: String = "",
    val registrationNumber: String = "",
    val portOfRegistry: String = "",        // nameAr
    val marineActivity: String = "",        // nameAr
    val shipCategory: String = "",          // nameAr
    val shipType: String = "",              // nameAr
    val buildMaterial: String = "",         // nameAr
    val shipBuildYear: String = "",
    val buildEndDate: String = "",
    val grossTonnage: String = "",
    val netTonnage: String = "",
    val vesselLengthOverall: String = "",
    val vesselBeam: String = "",
    val vesselDraft: String = "",
    val isTemp: Boolean = false,
    val engines: List<CoreEngineInfo> = emptyList(),
    val owners: List<CoreOwnerInfo> = emptyList(),
    val certifications: List<CoreCertificationInfo> = emptyList()
)

data class CoreEngineInfo(
    val serialNumber: String = "",
    val engineType: String = "",
    val enginePower: String = "",
    val engineStatus: String = ""
)

data class CoreOwnerInfo(
    val ownerName: String = "",
    val ownerCivilId: String = "",
    val ownershipPercentage: Double = 0.0,
    val isRepresentative: Boolean = false
)

data class CoreCertificationInfo(
    val certificationNumber: String = "",
    val issuedDate: String = "",
    val expiryDate: String = "",
    val certificationType: String = ""
)


