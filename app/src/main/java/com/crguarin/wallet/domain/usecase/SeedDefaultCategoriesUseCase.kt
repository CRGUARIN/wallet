package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.seed.CategorySeeder
import javax.inject.Inject

class SeedDefaultCategoriesUseCase
    @Inject
    constructor(
        private val categorySeeder: CategorySeeder,
    ) {
        suspend operator fun invoke(): DomainResult<Unit> {
            categorySeeder.seedIfNeeded()
            return DomainResult.Success(Unit)
        }
    }
