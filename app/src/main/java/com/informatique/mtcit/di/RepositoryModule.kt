package com.informatique.mtcit.di

import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.LookupRepositoryImpl
import com.informatique.mtcit.data.repository.LandingRepository
import com.informatique.mtcit.data.repository.LandingRepositoryImpl
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMainCategoriesRepository(
        impl: LandingRepositoryImpl
    ): LandingRepository

    @Binds
    @Singleton
    abstract fun bindShipRegistrationRepository(
        impl: ShipRegistrationRepositoryImpl
    ): ShipRegistrationRepository

    @Binds
    @Singleton
    abstract fun bindLookupRepository(
        impl: LookupRepositoryImpl
    ): LookupRepository
}

