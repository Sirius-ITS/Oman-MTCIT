package com.informatique.mtcit.util

import android.content.Context
import com.informatique.mtcit.data.datastorehelper.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper object to get user information from JWT token
 */
object UserHelper {

    /**
     * Get civil ID from token to use as owner ID
     * Returns null if token is not available (should trigger login flow)
     */
    suspend fun getOwnerCivilId(context: Context): String? {
        return withContext(Dispatchers.IO) {
            TokenManager.getCivilId(context)
        }
    }

    /**
     * Get user name from token
     */
    suspend fun getUserName(context: Context): String? {
        return withContext(Dispatchers.IO) {
            TokenManager.getUserName(context)
        }
    }

    /**
     * Get user email from token
     */
    suspend fun getUserEmail(context: Context): String? {
        return withContext(Dispatchers.IO) {
            TokenManager.getUserEmail(context)
        }
    }

    /**
     * Get all user data from token
     */
    suspend fun getUserData(context: Context): TokenManager.UserData? {
        return withContext(Dispatchers.IO) {
            TokenManager.getUserData(context)
        }
    }
}
