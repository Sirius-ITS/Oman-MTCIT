package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.company.CompanyRepository
import com.informatique.mtcit.data.model.companyModels.CompanyLookupResponse
import com.informatique.mtcit.data.network.ApiInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepositoryImpl @Inject constructor(
    private val apiInterface: ApiInterface
) : CompanyRepository {

    override suspend fun lookupCompany(registrationNumber: String): CompanyLookupResponse {
        return apiInterface.lookupCompany(registrationNumber)
    }
}
