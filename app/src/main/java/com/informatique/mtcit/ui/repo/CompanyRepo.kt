package com.informatique.mtcit.ui.repo

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.data.model.companyModels.CompanyLookupResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

@ViewModelScoped
class CompanyRepo
@Inject constructor(
    val repo: AppRepository,
    val json: Json
) {

    fun fetchCompanyLookup(registrationNumber: String): Flow<BusinessState<CompanyLookupResponse>> = flow {
        val serviceState = repo.onGet("commercialRegistrationNew.ashx?segelNo=$registrationNumber")
        when (serviceState) {
            is RepoServiceState.Success -> {
                val response = serviceState.response
                if (!response.jsonObject.isEmpty()) {
                    val companyLookupResponse: CompanyLookupResponse =
                        json.decodeFromJsonElement(response.jsonObject)

                    emit(BusinessState.Success(companyLookupResponse))
                }
            }

            is RepoServiceState.Error -> {
                emit(
                    BusinessState.Error(serviceState.error.toString()))
            }
        }
    }

}