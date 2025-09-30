package com.informatique.educationComposeVersion.business.base

import com.informatique.educationComposeVersion.business.BusinessState

abstract class BaseUseCase<in P, R> {
    abstract suspend operator fun invoke(parameters: P): BusinessState<R>
}

