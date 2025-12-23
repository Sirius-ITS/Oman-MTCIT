package com.informatique.mtcit.di.module

import com.abanoub.myapp.di.security.Environment
import com.abanoub.myapp.di.security.EnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(value = [SingletonComponent::class])
class NetworkModule {

    @Singleton
    @Provides
    fun provideEnvironmentConfig(): Environment {
        return EnvironmentConfig.currentEnvironment
    }

    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true  // ⚠️ مهم جداً
            prettyPrint = true
            coerceInputValues = true
        }
    }

    @Singleton
    @Provides
    fun provideHttpClient(environment: Environment, json: Json): HttpClient {
        return HttpClient(Android) {
            // Engine configuration
            engine {
                connectTimeout = 15_000
                socketTimeout = 30_000

                // ✅ DEVELOPMENT ONLY: Disable SSL certificate validation
                // This creates a trust manager that accepts ALL certificates
                val trustAllCerts = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())

                // Set the SSL socket factory and hostname verifier
                sslManager = { httpsURLConnection ->
                    httpsURLConnection.sslSocketFactory = sslContext.socketFactory
                    httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }

                // ⚠️ PRODUCTION: Use SSL Pinning (commented out for development)
                // if (environment.security.enableSSLPinning)
                //     this.sslManager = { SSLPinning.provideCertificatePinner() }
            }

            // Default request configuration
            defaultRequest {
                url(environment.baseUrl)
                header("X-API-Key", environment.apiKey)
                header("X-Client-Version", "BuildConfig.VERSION_NAME")
                header("X-Platform", "Android")
                header(
                    key = "X-Auth-Token",
                    // mortgageValue = "ZWE3ODUxMTEtMDM5Zi00ODQ2LTgyYWItMDJlZTIzYTEwNzBh" // Qrcode data
                    value = "ZWMxNjZjMTEtZTQwZS00OGE5LWJmMzYtZDkwNDA1ZWU5ZDdh"
                    // mortgageValue = preferences.getAuthToken()
                )
                contentType(ContentType.Application.Json)
            }

            install(ContentNegotiation) {
                json(json)
            }

            // Logging
            if (environment.features.enableDebugEndpoints) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                }
            }

            // Response caching
            if (environment.features.enableCaching) {
                install(HttpCache)
            }

            // Request retry with exponential backoff
            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnExceptionIf { request, cause ->
                    cause is ConnectTimeoutException
                }
                exponentialDelay()
            }

            // Custom timeout plugin
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000L
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 30_000L
            }

        }
    }

    @Singleton
    @Provides
    fun provideAppRepository(httpClient: HttpClient): AppRepository {
        return AppRepository(httpClient)
    }

}