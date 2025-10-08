package com.informatique.mtcit.di

import com.informatique.mtcit.business.company.CompanyRepository
import com.informatique.mtcit.data.repository.CompanyRepositoryImpl
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.ShipRegistrationRepositoryImpl
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
}

