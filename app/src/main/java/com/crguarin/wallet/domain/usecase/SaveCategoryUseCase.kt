package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.SystemCategoryNames
import com.crguarin.wallet.domain.repository.CategoryRepository
import java.util.Locale
import javax.inject.Inject

class SaveCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val systemCategoryNames: SystemCategoryNames,
    ) {
        suspend operator fun invoke(category: Category): DomainResult<Long> {
            val trimmedName = category.name?.trim()
            if (category.name != null) {
                if (trimmedName.isNullOrEmpty()) {
                    return DomainResult.Failure(DomainError.NameEmpty)
                }
                if (trimmedName.length > 40) {
                    return DomainResult.Failure(DomainError.NameTooLong)
                }
            } else if (category.systemKey == null) {
                return DomainResult.Failure(DomainError.NameEmpty)
            }

            if (category.parentId != null) {
                if (category.parentId == category.id) {
                    return DomainResult.Failure(DomainError.MaxDepthExceeded)
                }
                val parent =
                    categoryRepository.getCategory(category.parentId)
                        ?: return DomainResult.Failure(DomainError.NotFound)

                if (parent.parentId != null) {
                    return DomainResult.Failure(DomainError.MaxDepthExceeded)
                }
                if (category.type != parent.type) {
                    return DomainResult.Failure(DomainError.CategoryTypeMismatch)
                }
            }

            if (category.id != 0L) {
                val existing =
                    categoryRepository.getCategory(category.id)
                        ?: return DomainResult.Failure(DomainError.NotFound)

                val childrenCount = categoryRepository.countChildren(category.id)
                if (childrenCount > 0 && category.parentId != null) {
                    return DomainResult.Failure(DomainError.MaxDepthExceeded)
                }

                if (category.type != existing.type) {
                    val txCount = categoryRepository.countTransactions(category.id)
                    if (txCount > 0) {
                        return DomainResult.Failure(DomainError.CategoryHasTransactions(txCount))
                    }
                    if (childrenCount > 0) {
                        return DomainResult.Failure(DomainError.CategoryHasChildren)
                    }
                }
            }

            if (trimmedName != null) {
                val candidateNormalized = trimmedName.lowercase(Locale.ROOT)
                val allInType = categoryRepository.getAllCategories(category.type)
                val siblings = allInType.filter { it.parentId == category.parentId && it.id != category.id }

                for (sibling in siblings) {
                    if (sibling.name != null) {
                        if (sibling.name.trim().lowercase(Locale.ROOT) == candidateNormalized) {
                            return DomainResult.Failure(DomainError.NameDuplicated)
                        }
                    } else if (sibling.systemKey != null) {
                        val systemNames = systemCategoryNames.namesFor(sibling.systemKey)
                        if (systemNames.any { it.trim().lowercase(Locale.ROOT) == candidateNormalized }) {
                            return DomainResult.Failure(DomainError.NameDuplicated)
                        }
                    }
                }
            }

            val categoryToSave = category.copy(name = trimmedName)
            val id = categoryRepository.saveCategory(categoryToSave)
            return DomainResult.Success(id)
        }
    }
