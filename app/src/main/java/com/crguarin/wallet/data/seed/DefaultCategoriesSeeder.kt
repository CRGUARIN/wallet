package com.crguarin.wallet.data.seed

import androidx.room.withTransaction
import com.crguarin.wallet.data.db.CategoryDao
import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.di.IoDispatcher
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.seed.CategorySeeder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCategoriesSeeder
    @Inject
    constructor(
        private val walletDatabase: WalletDatabase,
        private val categoryDao: CategoryDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CategorySeeder {
        private data class SubcategorySeed(
            val systemKey: String,
            val icon: String,
        )

        private data class CategorySeed(
            val systemKey: String,
            val type: CategoryType,
            val color: Int,
            val icon: String,
            val subcategories: List<SubcategorySeed> = emptyList(),
        )

        private val defaultCategories =
            listOf(
                // Expense categories
                CategorySeed(
                    systemKey = "food",
                    type = CategoryType.EXPENSE,
                    color = 0xFFEF5350.toInt(),
                    icon = "restaurant",
                    subcategories =
                        listOf(
                            SubcategorySeed("groceries", "local_grocery_store"),
                            SubcategorySeed("restaurants", "restaurant"),
                        ),
                ),
                CategorySeed(
                    systemKey = "transport",
                    type = CategoryType.EXPENSE,
                    color = 0xFFFFA726.toInt(),
                    icon = "directions_bus",
                    subcategories =
                        listOf(
                            SubcategorySeed("public_transport", "directions_bus"),
                            SubcategorySeed("fuel", "local_gas_station"),
                            SubcategorySeed("taxi", "local_taxi"),
                        ),
                ),
                CategorySeed(
                    systemKey = "housing",
                    type = CategoryType.EXPENSE,
                    color = 0xFFAB47BC.toInt(),
                    icon = "home",
                    subcategories =
                        listOf(
                            SubcategorySeed("rent", "apartment"),
                            SubcategorySeed("utilities", "power"),
                        ),
                ),
                CategorySeed(
                    systemKey = "health",
                    type = CategoryType.EXPENSE,
                    color = 0xFF26A69A.toInt(),
                    icon = "medical_services",
                ),
                CategorySeed(
                    systemKey = "education",
                    type = CategoryType.EXPENSE,
                    color = 0xFF42A5F5.toInt(),
                    icon = "school",
                ),
                CategorySeed(
                    systemKey = "entertainment",
                    type = CategoryType.EXPENSE,
                    color = 0xFFEC407A.toInt(),
                    icon = "movie",
                ),
                CategorySeed(
                    systemKey = "shopping",
                    type = CategoryType.EXPENSE,
                    color = 0xFFFF7043.toInt(),
                    icon = "shopping_bag",
                ),
                CategorySeed(
                    systemKey = "other_expense",
                    type = CategoryType.EXPENSE,
                    color = 0xFF78909C.toInt(),
                    icon = "more_horiz",
                ),
                CategorySeed(
                    systemKey = "card_interest_fees",
                    type = CategoryType.EXPENSE,
                    color = 0xFFE53935.toInt(),
                    icon = "credit_card",
                ),
                // Income categories
                CategorySeed(
                    systemKey = "salary",
                    type = CategoryType.INCOME,
                    color = 0xFF66BB6A.toInt(),
                    icon = "payments",
                ),
                CategorySeed(
                    systemKey = "freelance",
                    type = CategoryType.INCOME,
                    color = 0xFF26C6DA.toInt(),
                    icon = "work",
                ),
                CategorySeed(
                    systemKey = "gifts",
                    type = CategoryType.INCOME,
                    color = 0xFFFFCA28.toInt(),
                    icon = "card_giftcard",
                ),
                CategorySeed(
                    systemKey = "interest",
                    type = CategoryType.INCOME,
                    color = 0xFF9CCC65.toInt(),
                    icon = "trending_up",
                ),
                CategorySeed(
                    systemKey = "other_income",
                    type = CategoryType.INCOME,
                    color = 0xFF8D6E63.toInt(),
                    icon = "account_balance",
                ),
                CategorySeed(
                    systemKey = "card_adjustment",
                    type = CategoryType.INCOME,
                    color = 0xFF00897B.toInt(),
                    icon = "receipt_long",
                ),
            )

        override suspend fun seedIfNeeded() {
            withContext(ioDispatcher) {
                walletDatabase.withTransaction {
                    val existingCategories = categoryDao.getAll()
                    val existingSystemKeys = existingCategories.mapNotNull { it.systemKey }.toSet()

                    defaultCategories.forEachIndexed { rootIndex, rootSeed ->
                        val rootId =
                            if (rootSeed.systemKey in existingSystemKeys) {
                                existingCategories.first { it.systemKey == rootSeed.systemKey }.id
                            } else {
                                categoryDao.insert(
                                    CategoryEntity(
                                        name = null,
                                        systemKey = rootSeed.systemKey,
                                        type = rootSeed.type,
                                        parentId = null,
                                        color = rootSeed.color,
                                        icon = rootSeed.icon,
                                        sortOrder = rootIndex,
                                    ),
                                )
                            }

                        rootSeed.subcategories.forEachIndexed { subIndex, subSeed ->
                            if (subSeed.systemKey !in existingSystemKeys) {
                                categoryDao.insert(
                                    CategoryEntity(
                                        name = null,
                                        systemKey = subSeed.systemKey,
                                        type = rootSeed.type,
                                        parentId = rootId,
                                        color = rootSeed.color,
                                        icon = subSeed.icon,
                                        sortOrder = subIndex,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
