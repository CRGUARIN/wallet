package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNameMatcher
import com.crguarin.wallet.domain.repository.CategoryRepository
import javax.inject.Inject

class UnarchiveCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val categoryNameMatcher: CategoryNameMatcher,
    ) {
        suspend operator fun invoke(categoryId: Long): DomainResult<Unit> {
            val category =
                categoryRepository.getCategory(categoryId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            if (!category.isArchived) {
                return DomainResult.Success(Unit)
            }

            val allInType = categoryRepository.getAllCategories(category.type)

            fun isNameColliding(
                target: Category,
                parentId: Long?,
                excludeId: Long,
            ): Boolean {
                val candidateNames = categoryNameMatcher.namesOf(target)
                val activeSiblings = allInType.filter { !it.isArchived && it.parentId == parentId && it.id != excludeId }
                return categoryNameMatcher.collides(candidateNames, activeSiblings)
            }

            if (category.parentId == null) {
                if (isNameColliding(category, parentId = null, excludeId = category.id)) {
                    return DomainResult.Failure(DomainError.NameDuplicated)
                }
                categoryRepository.unarchiveCategory(categoryId, unarchiveParent = false)
                return DomainResult.Success(Unit)
            } else {
                val parent =
                    categoryRepository.getCategory(category.parentId)
                        ?: return DomainResult.Failure(DomainError.NotFound)

                if (parent.isArchived) {
                    if (isNameColliding(parent, parentId = null, excludeId = parent.id)) {
                        return DomainResult.Failure(DomainError.NameDuplicated)
                    }
                }

                if (isNameColliding(category, parentId = parent.id, excludeId = category.id)) {
                    return DomainResult.Failure(DomainError.NameDuplicated)
                }

                categoryRepository.unarchiveCategory(categoryId, unarchiveParent = parent.isArchived)
                return DomainResult.Success(Unit)
            }
        }
    }
