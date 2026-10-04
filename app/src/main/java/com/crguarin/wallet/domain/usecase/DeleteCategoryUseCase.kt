package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

class DeleteCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val recurringRepository: RecurringRepository,
    ) {
        suspend operator fun invoke(categoryId: Long): DomainResult<Unit> {
            val category =
                categoryRepository.getCategory(categoryId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            val childrenCount = categoryRepository.countChildren(categoryId)
            if (childrenCount > 0) {
                return DomainResult.Failure(DomainError.CategoryHasChildren)
            }

            val txCount = categoryRepository.countTransactions(categoryId)
            if (txCount > 0) {
                return DomainResult.Failure(DomainError.CategoryHasTransactions(txCount))
            }

            if (recurringRepository.countByCategory(categoryId) > 0) {
                return DomainResult.Failure(DomainError.CategoryHasRecurring)
            }

            categoryRepository.deleteCategory(category)
            return DomainResult.Success(Unit)
        }
    }
