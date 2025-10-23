package com.informatique.mtcit.business.landing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating Landing Strategy instances
 */
@Singleton
class LandingStrategyFactory @Inject constructor(
    private val landingStrategy: LandingStrategy
) {
    fun createLandingStrategy(): LandingStrategyInterface {
        return landingStrategy
    }
}

