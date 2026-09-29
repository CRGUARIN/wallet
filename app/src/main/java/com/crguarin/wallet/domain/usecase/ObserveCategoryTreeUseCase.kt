package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.buildCategoryTree
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
            categoryRepository.observeCategories(type, includeArchived).map { categories -> buildCategoryTree(categories) }
    }
