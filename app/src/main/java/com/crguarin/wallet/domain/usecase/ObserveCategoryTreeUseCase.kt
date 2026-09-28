package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveCategoryTreeUseCase
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
    ) {
        operator fun invoke(
            type: CategoryType,
            includeArchived: Boolean = false,
        ): Flow<List<CategoryNode>> =
            categoryRepository.observeCategories(type, includeArchived).map { categories ->
                val roots = categories.filter { it.parentId == null }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
                roots.map { root ->
                    val children =
                        categories
                            .filter { it.parentId == root.id }
                            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                            .map { CategoryNode(it) }
                    CategoryNode(category = root, children = children)
                }
            }
    }
