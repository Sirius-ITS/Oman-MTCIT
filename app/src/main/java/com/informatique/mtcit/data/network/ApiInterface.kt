package com.informatique.mtcit.data.network

import com.informatique.mtcit.data.model.loginModels.LoginRequest
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {
    @POST("students/login")
    suspend fun getLoginData(@Body request: LoginRequest): LoginResponse



}