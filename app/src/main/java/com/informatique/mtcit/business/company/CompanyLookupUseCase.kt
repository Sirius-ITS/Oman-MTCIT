package com.informatique.mtcit.business.company

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.base.BaseUseCase
import com.informatique.mtcit.data.model.companyModels.CompanyLookupResponse
import javax.inject.Inject

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
                return BusinessState.Error("رقم السجل التجاري مطلوب")
            }

            if (parameters.registrationNumber.length < 3) {
                return BusinessState.Error("رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام")
            }

            // Call API
            val result = repository.lookupCompany(parameters.registrationNumber)

            if (result.result != null) {
                BusinessState.Success(result)
                if (result.result.status) {
                    BusinessState.Success(result)
                } else {
                    BusinessState.Error(result.message ?: "خطأ في السجل التجاري")
                }
            } else
                BusinessState.Error("خطأ في السجل التجاري")
        } catch (e: Exception) {
            BusinessState.Error("خطأ في السجل التجاري")
        }
    }
}
