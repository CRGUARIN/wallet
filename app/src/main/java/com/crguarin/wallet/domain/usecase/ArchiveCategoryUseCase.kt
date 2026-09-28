package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.CategoryRepository
import javax.inject.Inject

class ArchiveCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
    ) {
        suspend operator fun invoke(categoryId: Long): DomainResult<Unit> {
            categoryRepository.getCategory(categoryId)
                ?: return DomainResult.Failure(DomainError.NotFound)

            categoryRepository.archiveCategoryAndSubcategories(categoryId)
            return DomainResult.Success(Unit)
        }
    }
