package com.informatique.mtcit.business.base

import com.informatique.mtcit.business.BusinessState

abstract class BaseUseCase<in P, R> {
    abstract suspend operator fun invoke(parameters: P): BusinessState<R>
}

