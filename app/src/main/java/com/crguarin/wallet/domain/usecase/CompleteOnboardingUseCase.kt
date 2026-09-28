package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.repository.PreferencesRepository
import javax.inject.Inject

class CompleteOnboardingUseCase
    @Inject
    constructor(
        private val saveAccountUseCase: SaveAccountUseCase,
        private val seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase,
        private val preferencesRepository: PreferencesRepository,
    ) {
        suspend operator fun invoke(account: Account): DomainResult<Long> {
            val saveResult = saveAccountUseCase(account)
            if (saveResult !is DomainResult.Success) {
                return saveResult
            }

            val seedResult = seedDefaultCategoriesUseCase()
            if (seedResult is DomainResult.Failure) {
                return DomainResult.Failure(seedResult.error)
            }

            preferencesRepository.setOnboardingCompleted(true)
            return saveResult
        }
    }
