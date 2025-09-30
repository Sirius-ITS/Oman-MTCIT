package com.informatique.educationComposeVersion.data.network

import com.informatique.educationComposeVersion.data.model.loginModels.LoginRequest
import com.informatique.educationComposeVersion.data.model.loginModels.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {
    @POST("students/Loin")
    suspend fun getLoginData(@Body request: LoginRequest): LoginResponse



}