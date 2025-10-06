package com.informatique.mtcit.data.network

import com.informatique.mtcit.data.model.loginModels.LoginRequest
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import com.informatique.mtcit.data.model.companyModels.CompanyLookupResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {
    @POST("students/Login")
    suspend fun getLoginData(@Body request: LoginRequest): LoginResponse

    @GET("commercialRegistrationNew.ashx")
    suspend fun lookupCompany(@Query("segelNo") registrationNumber: String): CompanyLookupResponse
}