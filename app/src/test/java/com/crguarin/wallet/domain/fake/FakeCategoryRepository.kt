package com.crguarin.wallet.domain.fake

import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCategoryRepository : CategoryRepository {
    private val categories = mutableMapOf<Long, Category>()
    private val transactionCounts = mutableMapOf<Long, Int>()
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private var nextId: Long = 1L

    fun setInitialData(
        categoryList: List<Category>,
        txCounts: Map<Long, Int> = emptyMap(),
    ) {
        categories.clear()
        transactionCounts.clear()
        categoryList.forEach { category ->
            categories[category.id] = category
            if (category.id >= nextId) nextId = category.id + 1
        }
        transactionCounts.putAll(txCounts)
        notifyChanged()
    }

    fun setTransactionCount(
        categoryId: Long,
        count: Int,
    ) {
        transactionCounts[categoryId] = count
    }

    private fun notifyChanged() {
        categoriesFlow.value = categories.values.sortedWith(compareBy({ it.isArchived }, { it.sortOrder }, { it.id }))
    }

    override fun observeCategories(
        type: CategoryType?,
        includeArchived: Boolean,
    ): Flow<List<Category>> =
        categoriesFlow.map { list ->
            list
                .filter { type == null || it.type == type }
                .filter { includeArchived || !it.isArchived }
        }

    override fun observeCategory(id: Long): Flow<Category?> = categoriesFlow.map { categories[id] }

    override suspend fun getCategory(id: Long): Category? = categories[id]

    override suspend fun getAllCategories(type: CategoryType?): List<Category> =
        categories.values.filter { type == null || it.type == type }

    override suspend fun saveCategory(category: Category): Long {
        val id = if (category.id == 0L) nextId++ else category.id
        val saved = category.copy(id = id)
        categories[id] = saved
        notifyChanged()
        return id
    }

    override suspend fun saveCategories(categories: List<Category>): List<Long> = categories.map { saveCategory(it) }

    override suspend fun deleteCategory(category: Category) {
        categories.remove(category.id)
        transactionCounts.remove(category.id)
        notifyChanged()
    }

    override suspend fun countChildren(categoryId: Long): Int = categories.values.count { it.parentId == categoryId }

    override suspend fun countTransactions(categoryId: Long): Int = transactionCounts[categoryId] ?: 0

    override suspend fun reassignAndDelete(
        fromCategoryId: Long,
        toCategoryId: Long,
    ) {
        val currentFromCount = transactionCounts[fromCategoryId] ?: 0
        val currentToCount = transactionCounts[toCategoryId] ?: 0
        transactionCounts[toCategoryId] = currentToCount + currentFromCount
        transactionCounts.remove(fromCategoryId)
        categories.remove(fromCategoryId)
        notifyChanged()
    }

    override suspend fun archiveCategoryAndSubcategories(categoryId: Long) {
        val target = categories[categoryId]
        if (target != null) {
            categories[categoryId] = target.copy(isArchived = true)
        }
        for ((id, cat) in categories) {
            if (cat.parentId == categoryId) {
                categories[id] = cat.copy(isArchived = true)
            }
        }
        notifyChanged()
    }
}
