package com.crguarin.wallet.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryDaoTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountDao = database.accountDao()
        categoryDao = database.categoryDao()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createCategory(
        name: String? = "Alimentación",
        systemKey: String? = null,
        type: CategoryType = CategoryType.EXPENSE,
        parentId: Long? = null,
        color: Int = -1,
        icon: String = "restaurant",
        isArchived: Boolean = false,
        sortOrder: Int = 0,
    ): CategoryEntity =
        CategoryEntity(
            name = name,
            systemKey = systemKey,
            type = type,
            parentId = parentId,
            color = color,
            icon = icon,
            isArchived = isArchived,
            sortOrder = sortOrder,
        )

    private fun createAccount(name: String = "Efectivo"): AccountEntity =
        AccountEntity(
            name = name,
            type = AccountType.CASH,
            currencyCode = "COP",
            initialBalance = 100_000L,
            color = -1,
            icon = "wallet",
            createdAt = Instant.ofEpochMilli(1000L),
        )

    @Test
    fun observeAll_filtersByType() =
        runTest {
            categoryDao.insert(createCategory(name = "Comida", type = CategoryType.EXPENSE))
            categoryDao.insert(createCategory(name = "Transporte", type = CategoryType.EXPENSE))
            categoryDao.insert(createCategory(name = "Salario", type = CategoryType.INCOME))

            categoryDao.observeAll(null).test {
                assertThat(awaitItem()).hasSize(3)
                cancelAndIgnoreRemainingEvents()
            }

            categoryDao.observeAll(CategoryType.EXPENSE).test {
                val list = awaitItem()
                assertThat(list).hasSize(2)
                assertThat(list.map { it.name }).containsExactly("Comida", "Transporte")
                cancelAndIgnoreRemainingEvents()
            }

            categoryDao.observeAll(CategoryType.INCOME).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().name).isEqualTo("Salario")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun countChildren_returnsCorrectCount() =
        runTest {
            val rootId = categoryDao.insert(createCategory(name = "Alimentación", parentId = null))
            assertThat(categoryDao.countChildren(rootId)).isEqualTo(0)

            categoryDao.insert(createCategory(name = "Mercado", parentId = rootId))
            categoryDao.insert(createCategory(name = "Restaurantes", parentId = rootId))

            assertThat(categoryDao.countChildren(rootId)).isEqualTo(2)
        }

    @Test
    fun countTransactions_returnsCorrectCount() =
        runTest {
            val accId = accountDao.insert(createAccount())
            val catId = categoryDao.insert(createCategory(name = "Salud"))

            assertThat(categoryDao.countTransactions(catId)).isEqualTo(0)

            transactionDao.insert(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 50L,
                    accountId = accId,
                    categoryId = catId,
                    date = LocalDate.now(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            )
            transactionDao.insert(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 25L,
                    accountId = accId,
                    categoryId = catId,
                    date = LocalDate.now(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            )

            assertThat(categoryDao.countTransactions(catId)).isEqualTo(2)
        }

    @Test
    fun reassignTransactions_movesTransactionsAndAllowsDelete() =
        runTest {
            val accId = accountDao.insert(createAccount())
            val catA = categoryDao.insert(createCategory(name = "Vieja Categoria"))
            val catB = categoryDao.insert(createCategory(name = "Nueva Categoria"))

            val tx1 =
                transactionDao.insert(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 10L,
                        accountId = accId,
                        categoryId = catA,
                        date = LocalDate.now(),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                )
            val tx2 =
                transactionDao.insert(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = 20L,
                        accountId = accId,
                        categoryId = catA,
                        date = LocalDate.now(),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                )

            assertThat(categoryDao.countTransactions(catA)).isEqualTo(2)
            assertThat(categoryDao.countTransactions(catB)).isEqualTo(0)

            val updatedCount = categoryDao.reassignTransactions(fromId = catA, toId = catB)
            assertThat(updatedCount).isEqualTo(2)

            assertThat(categoryDao.countTransactions(catA)).isEqualTo(0)
            assertThat(categoryDao.countTransactions(catB)).isEqualTo(2)

            assertThat(transactionDao.getById(tx1)!!.categoryId).isEqualTo(catB)
            assertThat(transactionDao.getById(tx2)!!.categoryId).isEqualTo(catB)

            // Ahora catA se puede borrar sin violación de foreign key
            val oldCat = categoryDao.getById(catA)!!
            categoryDao.delete(oldCat)
            assertThat(categoryDao.getById(catA)).isNull()
        }

    @Test
    fun existsSiblingWithName_checksCaseInsensitiveAndParent() =
        runTest {
            val rootFood = categoryDao.insert(createCategory(name = "Alimentación", parentId = null, type = CategoryType.EXPENSE))
            val subMercado = categoryDao.insert(createCategory(name = "Mercado", parentId = rootFood, type = CategoryType.EXPENSE))

            // Same root level
            assertThat(categoryDao.existsSiblingWithName("alimentación", CategoryType.EXPENSE, null)).isTrue()
            assertThat(categoryDao.existsSiblingWithName("  ALIMENTACIÓN  ", CategoryType.EXPENSE, null)).isTrue()
            assertThat(categoryDao.existsSiblingWithName("Transporte", CategoryType.EXPENSE, null)).isFalse()

            // Sibling at root with different type doesn't conflict
            assertThat(categoryDao.existsSiblingWithName("Alimentación", CategoryType.INCOME, null)).isFalse()

            // Sibling under same parent
            assertThat(categoryDao.existsSiblingWithName("mercado", CategoryType.EXPENSE, rootFood)).isTrue()
            assertThat(categoryDao.existsSiblingWithName("Restaurantes", CategoryType.EXPENSE, rootFood)).isFalse()

            // Subcategory name at root level doesn't conflict
            assertThat(categoryDao.existsSiblingWithName("Mercado", CategoryType.EXPENSE, null)).isFalse()

            // Exclude ID excludes self
            assertThat(categoryDao.existsSiblingWithName("Mercado", CategoryType.EXPENSE, rootFood, excludeId = subMercado)).isFalse()
        }

    @Test
    fun foreignKeyRestrict_deletingCategoryWithSubcategories_fails() =
        runTest {
            val rootId = categoryDao.insert(createCategory(name = "Padre"))
            categoryDao.insert(createCategory(name = "Hijo", parentId = rootId))

            val rootCategory = categoryDao.getById(rootId)!!
            val exception =
                try {
                    categoryDao.delete(rootCategory)
                    null
                } catch (e: SQLiteConstraintException) {
                    e
                }
            assertThat(exception).isNotNull()
        }

    @Test
    fun foreignKeyRestrict_deletingCategoryWithTransactions_fails() =
        runTest {
            val accId = accountDao.insert(createAccount())
            val catId = categoryDao.insert(createCategory(name = "Compras"))
            val category = categoryDao.getById(catId)!!

            transactionDao.insert(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 15L,
                    accountId = accId,
                    categoryId = catId,
                    date = LocalDate.now(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            )

            val exception =
                try {
                    categoryDao.delete(category)
                    null
                } catch (e: SQLiteConstraintException) {
                    e
                }
            assertThat(exception).isNotNull()
        }
}
