package com.informatique.mtcit.di

import com.informatique.mtcit.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection module for Marine Unit and Mortgage features
 * Provides repositories and business rules to the application
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MarineUnitModule {

    /**
     * Provides MarineUnitRepository implementation
     * Used for fetching user's marine units and validation data
     */
    @Binds
    @Singleton
    abstract fun bindMarineUnitRepository(
        impl: MarineUnitRepositoryImpl
    ): MarineUnitRepository

    /**
     * Provides MortgageRepository implementation
     * Used for checking mortgage status and bank approval
     */
    @Binds
    @Singleton
    abstract fun bindMortgageRepository(
        impl: MortgageRepositoryImpl
    ): MortgageRepository
}

