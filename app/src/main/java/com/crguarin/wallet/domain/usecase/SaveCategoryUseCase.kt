package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNameMatcher
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.normalizedName
import com.crguarin.wallet.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val categoryNameMatcher: CategoryNameMatcher,
    ) {
        suspend operator fun invoke(category: Category): DomainResult<Long> {
            val trimmedName = category.name?.trim()
            if (category.name != null) {
                if (trimmedName.isNullOrEmpty()) {
                    return DomainResult.Failure(DomainError.NameEmpty)
                }
                if (trimmedName.length > DomainLimits.NAME_MAX_LENGTH) {
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

            var existing: Category? = null
            if (category.id != 0L) {
                existing =
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

            val allInType = categoryRepository.getAllCategories(category.type)
            val siblings = allInType.filter { it.parentId == category.parentId && it.id != category.id }

            if (trimmedName != null) {
                // Archived siblings do not block a name, like archived accounts; unarchiving re-checks it.
                val activeSiblings = siblings.filter { !it.isArchived }
                if (categoryNameMatcher.collides(listOf(trimmedName.normalizedName()), activeSiblings)) {
                    return DomainResult.Failure(DomainError.NameDuplicated)
                }
            }

            // Order and archived state are owned by storage: an edit keeps them (moving to another
            // parent appends it there), and a new category goes after its siblings.
            val appendedSortOrder = (siblings.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val categoryToSave =
                if (existing != null) {
                    category.copy(
                        name = trimmedName,
                        isArchived = existing.isArchived,
                        sortOrder =
                            if (existing.parentId == category.parentId && existing.type == category.type) {
                                existing.sortOrder
                            } else {
                                appendedSortOrder
                            },
                    )
                } else {
                    category.copy(name = trimmedName, isArchived = false, sortOrder = appendedSortOrder)
                }
            val id = categoryRepository.saveCategory(categoryToSave)
            return DomainResult.Success(id)
        }
    }
