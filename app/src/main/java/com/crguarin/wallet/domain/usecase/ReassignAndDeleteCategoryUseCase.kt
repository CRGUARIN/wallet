package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.CategoryRepository
import javax.inject.Inject

class ReassignAndDeleteCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
    ) {
        suspend operator fun invoke(
            sourceCategoryId: Long,
            targetCategoryId: Long,
        ): DomainResult<Unit> {
            if (sourceCategoryId == targetCategoryId) {
                return DomainResult.Failure(DomainError.SameCategoryReassignment)
            }

            val source =
                categoryRepository.getCategory(sourceCategoryId)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            val target =
                categoryRepository.getCategory(targetCategoryId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            if (source.type != target.type) {
                return DomainResult.Failure(DomainError.CategoryTypeMismatch)
            }

            val childrenCount = categoryRepository.countChildren(sourceCategoryId)
            if (childrenCount > 0) {
                return DomainResult.Failure(DomainError.CategoryHasChildren)
            }

            categoryRepository.reassignAndDelete(
                fromCategoryId = sourceCategoryId,
                toCategoryId = targetCategoryId,
            )
            return DomainResult.Success(Unit)
        }
    }
