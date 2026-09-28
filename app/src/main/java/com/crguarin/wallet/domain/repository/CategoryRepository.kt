package com.crguarin.wallet.domain.repository

import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(
        type: CategoryType? = null,
        includeArchived: Boolean = false,
    ): Flow<List<Category>>

    fun observeCategory(id: Long): Flow<Category?>

    suspend fun getCategory(id: Long): Category?

    suspend fun getAllCategories(type: CategoryType? = null): List<Category>

    suspend fun saveCategory(category: Category): Long

    suspend fun saveCategories(categories: List<Category>): List<Long>

    suspend fun deleteCategory(category: Category)

    suspend fun countChildren(categoryId: Long): Int

    suspend fun countTransactions(categoryId: Long): Int

    suspend fun reassignAndDelete(
        fromCategoryId: Long,
        toCategoryId: Long,
    )

    suspend fun archiveCategoryAndSubcategories(categoryId: Long)
}
