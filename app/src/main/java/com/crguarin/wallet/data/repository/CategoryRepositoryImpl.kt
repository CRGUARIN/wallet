package com.crguarin.wallet.data.repository

import androidx.room.withTransaction
import com.crguarin.wallet.data.db.CategoryDao
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val walletDatabase: WalletDatabase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CategoryRepository {
        override fun observeCategories(
            type: CategoryType?,
            includeArchived: Boolean,
        ): Flow<List<Category>> =
            categoryDao
                .observeAll(type)
                .map { list ->
                    list
                        .map { it.toDomain() }
                        .filter { includeArchived || !it.isArchived }
                }.flowOn(ioDispatcher)

        override fun observeCategory(id: Long): Flow<Category?> = categoryDao.observeById(id).map { it?.toDomain() }.flowOn(ioDispatcher)

        override suspend fun getCategory(id: Long): Category? =
            withContext(ioDispatcher) {
                categoryDao.getById(id)?.toDomain()
            }

        override suspend fun getAllCategories(type: CategoryType?): List<Category> =
            withContext(ioDispatcher) {
                categoryDao.getAll(type).map { it.toDomain() }
            }

        override suspend fun saveCategory(category: Category): Long =
            withContext(ioDispatcher) {
                if (category.id == 0L) {
                    categoryDao.insert(category.toEntity())
                } else {
                    categoryDao.update(category.toEntity())
                    category.id
                }
            }

        override suspend fun saveCategories(categories: List<Category>): List<Long> =
            withContext(ioDispatcher) {
                categoryDao.insertAll(categories.map { it.toEntity() })
            }

        override suspend fun deleteCategory(category: Category) {
            withContext(ioDispatcher) {
                categoryDao.delete(category.toEntity())
            }
        }

        override suspend fun countChildren(categoryId: Long): Int =
            withContext(ioDispatcher) {
                categoryDao.countChildren(categoryId)
            }

        override suspend fun countTransactions(categoryId: Long): Int =
            withContext(ioDispatcher) {
                categoryDao.countTransactions(categoryId)
            }

        override suspend fun reassignAndDelete(
            fromCategoryId: Long,
            toCategoryId: Long,
        ) {
            withContext(ioDispatcher) {
                walletDatabase.withTransaction {
                    categoryDao.reassignTransactions(fromId = fromCategoryId, toId = toCategoryId)
                    val source = categoryDao.getById(fromCategoryId)
                    if (source != null) {
                        categoryDao.delete(source)
                    }
                }
            }
        }

        override suspend fun archiveCategoryAndSubcategories(categoryId: Long) {
            withContext(ioDispatcher) {
                categoryDao.archiveCategoryAndSubcategories(categoryId)
            }
        }
    }
