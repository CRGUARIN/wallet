package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.SystemCategoryNames
import com.crguarin.wallet.domain.repository.CategoryRepository
import java.util.Locale
import javax.inject.Inject

class UnarchiveCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val systemCategoryNames: SystemCategoryNames,
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
                val candidateNames: List<String> =
                    if (target.name != null) {
                        listOf(target.name.trim().lowercase(Locale.ROOT))
                    } else if (target.systemKey != null) {
                        systemCategoryNames.namesFor(target.systemKey).map { it.trim().lowercase(Locale.ROOT) }
                    } else {
                        emptyList()
                    }

                if (candidateNames.isEmpty()) return false

                val activeSiblings = allInType.filter { !it.isArchived && it.parentId == parentId && it.id != excludeId }
                for (sibling in activeSiblings) {
                    if (sibling.name != null) {
                        if (sibling.name.trim().lowercase(Locale.ROOT) in candidateNames) {
                            return true
                        }
                    } else if (sibling.systemKey != null) {
                        val siblingNames = systemCategoryNames.namesFor(sibling.systemKey).map { it.trim().lowercase(Locale.ROOT) }
                        if (siblingNames.any { it in candidateNames }) {
                            return true
                        }
                    }
                }
                return false
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
