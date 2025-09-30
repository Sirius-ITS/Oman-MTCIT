package com.informatique.mtcit.di.module

import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.informatique.mtcit.common.Const
import com.informatique.mtcit.common.dispatcher.DefaultDispatcherProvider
import com.informatique.mtcit.common.dispatcher.DispatcherProvider
import com.informatique.mtcit.common.logger.AppLogger
import com.informatique.mtcit.common.logger.Logger
import com.informatique.mtcit.common.networkhelper.NetworkHelper
import com.informatique.mtcit.common.networkhelper.NetworkHelperImpl
import com.informatique.mtcit.data.network.ApiInterface
import com.informatique.mtcit.data.network.ApiKeyInterceptor
import com.informatique.mtcit.di.ApiKey
import com.informatique.mtcit.di.BaseUrl
import com.informatique.mtcit.di.DbName
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {

    @Singleton
    @Provides
    fun provideGsonConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()

    @ApiKey
    @Provides
    fun provideApiKey(): String = Const.API_KEY

    @BaseUrl
    @Provides
    fun provideBaseUrl(): String = Const.BASE_URL

    @DbName
    @Provides
    fun provideDbName(): String = Const.DB_NAME

    @Singleton
    @Provides
    fun provideNetworkService(
        @BaseUrl baseUrl: String,
        gsonFactory: GsonConverterFactory,
        apiKeyInterceptor: ApiKeyInterceptor
    ): ApiInterface {
        val client = OkHttpClient
            .Builder()
            .addInterceptor(apiKeyInterceptor)
            .build()

        return Retrofit
            .Builder()
            .client(client) //adding client to intercept all responses
            .baseUrl(baseUrl)
            .addConverterFactory(gsonFactory)
            .build()
            .create(ApiInterface::class.java)
    }

    @Provides
    @Singleton
    fun provideLogger(): Logger = AppLogger()

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
    }

    @Provides
    @Singleton
    fun provideNetworkHelper(
        @ApplicationContext context: Context
    ): NetworkHelper {
        return NetworkHelperImpl(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    }


}