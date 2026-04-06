package com.informatique.mtcit.business.company

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.base.BaseUseCase
import com.informatique.mtcit.data.model.companyModels.CompanyLookupResponse
import javax.inject.Inject
import com.informatique.mtcit.common.util.AppLanguage

data class CompanyLookupParams(
    val registrationNumber: String
)

interface CompanyRepository {
    suspend fun lookupCompany(registrationNumber: String): CompanyLookupResponse
}

class CompanyLookupUseCase @Inject constructor(
    private val repository: CompanyRepository
) : BaseUseCase<CompanyLookupParams, CompanyLookupResponse>() {

    override suspend fun invoke(parameters: CompanyLookupParams): BusinessState<CompanyLookupResponse> {
        return try {
            // Validate registration number
            if (parameters.registrationNumber.isBlank()) {
                return BusinessState.Error(if (AppLanguage.isArabic) "رقم السجل التجاري مطلوب" else "Commercial registration number is required")
            }

            if (parameters.registrationNumber.length < 3) {
                return BusinessState.Error(if (AppLanguage.isArabic) "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام" else "Commercial registration number must be more than 3 digits")
            }

            // Call API
            val result = repository.lookupCompany(parameters.registrationNumber)

            if (result.result != null) {
                BusinessState.Success(result)
                if (result.result.status) {
                    BusinessState.Success(result)
                } else {
                    BusinessState.Error(result.message ?: if (AppLanguage.isArabic) "خطأ في السجل التجاري" else "Commercial registration error")
                }
            } else
                BusinessState.Error(if (AppLanguage.isArabic) "خطأ في السجل التجاري" else "Commercial registration error")
        } catch (e: Exception) {
            BusinessState.Error(if (AppLanguage.isArabic) "خطأ في السجل التجاري" else "Commercial registration error")
        }
    }
}
