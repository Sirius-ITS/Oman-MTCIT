package com.informatique.mtcit.di

import com.informatique.mtcit.business.company.CompanyRepository
import com.informatique.mtcit.data.repository.CompanyRepositoryImpl
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepositoryImpl
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.data.repository.LookupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds ShipRegistrationRepositoryImpl to ShipRegistrationRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindShipRegistrationRepository(
        impl: ShipRegistrationRepositoryImpl
    ): ShipRegistrationRepository

    /**
     * Binds CompanyRepositoryImpl to CompanyRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindCompanyRepository(
        impl: CompanyRepositoryImpl
    ): CompanyRepository

    /**
     * Binds LookupRepositoryImpl to LookupRepository interface
     * Provides dropdown options from API (mock data until real API is ready)
     */
    @Binds
    @Singleton
    abstract fun bindLookupRepository(
        impl: LookupRepositoryImpl
    ): LookupRepository
}
