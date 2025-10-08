package com.informatique.mtcit.data.repository

/*import com.informatique.mtcit.data.model.loginModels.LoginRequest
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import com.informatique.mtcit.data.network.ApiInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private val network: ApiInterface
) {
    fun loginUser(
        username: String,
        password: String
    ): Flow<LoginResponse> = flow {
        val request = LoginRequest(
            UserName = username,
            Password = password
        )
        emit(
            network.getLoginData(
               request
            )
        )
    }

}*/