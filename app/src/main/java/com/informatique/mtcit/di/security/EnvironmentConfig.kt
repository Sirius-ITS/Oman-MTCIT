package com.abanoub.myapp.di.security

import android.content.Context
import kotlinx.serialization.Serializable


enum class EnvironmentType {
    PRODUCTION,
    STAGE,
    DEVELOPMENT
}

class EnvironmentConfig {

    companion object {
        var currentEnvironment: Environment = Environment()
    }

    class Builder(val context: Context){
        private var currentEnvironmentType: EnvironmentType = EnvironmentType.DEVELOPMENT
        private var enableFeatureFlags: Boolean = true
        private var enableSecurityConfig: Boolean = false

        fun setCurrentEnvironmentType(type: EnvironmentType) =
            apply { this.currentEnvironmentType = type }
        fun setEnableFeatureFlags(isEnabled: Boolean) =
            apply { this.enableFeatureFlags = isEnabled }
        fun setEnableSecurityConfig(isEnabled: Boolean) =
            apply { this.enableSecurityConfig = isEnabled }

        fun build() {
            currentEnvironment = when(currentEnvironmentType){
                EnvironmentType.PRODUCTION -> {
                    Environment.createProduction()
                }

                EnvironmentType.STAGE -> {
                    Environment.createStage()
                }

                EnvironmentType.DEVELOPMENT -> {
                    Environment.createDefault()
                }
            }
        }
    }

}

@Serializable
data class Environment(
    val id: EnvironmentType = EnvironmentType.DEVELOPMENT,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val features: FeatureFlags = FeatureFlags(),
    val security: SecurityConfig = SecurityConfig()
) {

    companion object {
        fun createDefault(): Environment = Environment(
            id = EnvironmentType.DEVELOPMENT,
            name = "Development",
            baseUrl = "http://192.168.1.2:3002/",
            apiKey = "dev_default_key_secure_123",
            features = FeatureFlags(
                enableExperimentalFeatures = true,
                enableDebugEndpoints = true,
                logLevel = "debug",
                cacheTimeoutMs = 300000L
            ),
            security = SecurityConfig(
                enableSSLPinning = false,
                enableCertificateTransparency = false,
                requireBiometricAuth = false
            )
        )

        fun createStage(): Environment = Environment(
            id = EnvironmentType.STAGE,
            name = "Staging",
            baseUrl = "https://api-stage.example.com",
            apiKey = "dev_default_key_secure_123",
            features = FeatureFlags(
                enableExperimentalFeatures = true,
                enableDebugEndpoints = true,
                logLevel = "debug",
                cacheTimeoutMs = 300000L
            ),
            security = SecurityConfig(
                enableSSLPinning = false,
                enableCertificateTransparency = false,
                requireBiometricAuth = false
            )
        )

        fun createProduction(): Environment = Environment(
            id = EnvironmentType.PRODUCTION,
            name = "Production",
            baseUrl = "https://api-production.example.com",
            apiKey = "dev_default_key_secure_123",
            features = FeatureFlags(
                enableExperimentalFeatures = false,
                enableDebugEndpoints = false,
                logLevel = "warn",
                cacheTimeoutMs = 300000L
            ),
            security = SecurityConfig(
                enableSSLPinning = true,
                enableCertificateTransparency = true,
                requireBiometricAuth = true
            )
        )
    }

}

@Serializable
data class FeatureFlags(
    val enableExperimentalFeatures: Boolean = false,
    val enableDebugEndpoints: Boolean = false,
    val enableCaching: Boolean = false,
    val logLevel: String = "info", // debug, info, warn, error
    val cacheTimeoutMs: Long = 600000L,
    val enableAnalytics: Boolean = true,
    val enableCrashReporting: Boolean = true
)

@Serializable
data class SecurityConfig(
    val enableSSLPinning: Boolean = true,
    val enableCertificateTransparency: Boolean = true,
    val requireBiometricAuth: Boolean = false,
    val autoLogoutMinutes: Int = 30
)

//@Serializable
//private data class AppConfig(
//    val currentEnvironmentId: String = "development",
//    val environments: Map<String, EnvironmentConfig> = emptyMap(),
//    val lastUpdated: Long = System.currentTimeMillis(),
//    val version: Int = 1
//)