package com.informatique.mtcit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.abanoub.myapp.di.security.EnvironmentConfig
import com.abanoub.myapp.di.security.EnvironmentType
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Your existing initialization code

        EnvironmentConfig.Builder(context = this)
            .setCurrentEnvironmentType(EnvironmentType.DEVELOPMENT)
            .setEnableFeatureFlags(true)
            .setEnableSecurityConfig(false)
            .build()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}