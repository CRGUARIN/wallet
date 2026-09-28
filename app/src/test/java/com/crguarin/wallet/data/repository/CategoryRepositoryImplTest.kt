package com.crguarin.wallet.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
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
class CategoryRepositoryImplTest {
    private lateinit var database: WalletDatabase
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl

    private val now = Instant.parse("2026-09-28T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        categoryRepository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
        accountRepository = AccountRepositoryImpl(database.accountDao(), Dispatchers.Unconfined)
        transactionRepository = TransactionRepositoryImpl(database.transactionDao(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saveCategory inserts and updates successfully`() =
        runTest {
            val cat =
                Category(
                    name = "Alimentación",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "restaurant",
                )
            val id = categoryRepository.saveCategory(cat)
            assertThat(id).isGreaterThan(0L)

            val loaded = categoryRepository.getCategory(id)
            assertThat(loaded?.name).isEqualTo("Alimentación")

            val updated = loaded!!.copy(name = "Comida")
            categoryRepository.saveCategory(updated)
            assertThat(categoryRepository.getCategory(id)?.name).isEqualTo("Comida")
        }

    @Test
    fun `archiveCategoryAndSubcategories archives parent and all its children`() =
        runTest {
            val parentId =
                categoryRepository.saveCategory(
                    Category(name = "Vivienda", type = CategoryType.EXPENSE, color = 1, icon = "home"),
                )
            val sub1Id =
                categoryRepository.saveCategory(
                    Category(name = "Arriendo", type = CategoryType.EXPENSE, parentId = parentId, color = 1, icon = "home"),
                )
            val sub2Id =
                categoryRepository.saveCategory(
                    Category(name = "Servicios", type = CategoryType.EXPENSE, parentId = parentId, color = 1, icon = "power"),
                )
            val otherId =
                categoryRepository.saveCategory(
                    Category(name = "Salud", type = CategoryType.EXPENSE, color = 2, icon = "medical_services"),
                )

            categoryRepository.archiveCategoryAndSubcategories(parentId)

            assertThat(categoryRepository.getCategory(parentId)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(sub1Id)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(sub2Id)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(otherId)?.isArchived).isFalse()
        }

    @Test
    fun `reassignAndDelete moves transactions and deletes source category atomically`() =
        runTest {
            val accId =
                accountRepository.saveAccount(
                    Account(
                        name = "Cash",
                        type = AccountType.CASH,
                        initialBalance = Money(100_000L),
                        color = 1,
                        icon = "w",
                        createdAt = now,
                    ),
                )
            val sourceId =
                categoryRepository.saveCategory(
                    Category(name = "Restaurantes", type = CategoryType.EXPENSE, color = 1, icon = "restaurant"),
                )
            val targetId =
                categoryRepository.saveCategory(
                    Category(name = "Alimentación General", type = CategoryType.EXPENSE, color = 2, icon = "restaurant"),
                )

            val txId =
                transactionRepository.saveTransaction(
                    Transaction(
                        type = TransactionType.EXPENSE,
                        amount = Money(25_000L),
                        accountId = accId,
                        categoryId = sourceId,
                        date = LocalDate.of(2026, 9, 28),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )

            assertThat(categoryRepository.countTransactions(sourceId)).isEqualTo(1)
            assertThat(categoryRepository.countTransactions(targetId)).isEqualTo(0)

            categoryRepository.reassignAndDelete(fromCategoryId = sourceId, toCategoryId = targetId)

            assertThat(categoryRepository.getCategory(sourceId)).isNull()
            assertThat(categoryRepository.getCategory(targetId)).isNotNull()
            assertThat(categoryRepository.countTransactions(targetId)).isEqualTo(1)

            val reloadedTx = transactionRepository.getTransaction(txId)
            assertThat(reloadedTx?.categoryId).isEqualTo(targetId)
        }

    @Test
    fun `unarchiveCategory unarchives category and optionally parent`() =
        runTest {
            val parentId =
                categoryRepository.saveCategory(
                    Category(name = "Vivienda", type = CategoryType.EXPENSE, color = 1, icon = "home", isArchived = true),
                )
            val subId =
                categoryRepository.saveCategory(
                    Category(
                        name = "Arriendo",
                        type = CategoryType.EXPENSE,
                        parentId = parentId,
                        color = 1,
                        icon = "home",
                        isArchived = true,
                    ),
                )

            // Test unarchiving only root
            categoryRepository.unarchiveCategory(parentId, unarchiveParent = false)
            assertThat(categoryRepository.getCategory(parentId)?.isArchived).isFalse()
            assertThat(categoryRepository.getCategory(subId)?.isArchived).isTrue()

            // Re-archive parent
            categoryRepository.archiveCategoryAndSubcategories(parentId)
            assertThat(categoryRepository.getCategory(parentId)?.isArchived).isTrue()
            assertThat(categoryRepository.getCategory(subId)?.isArchived).isTrue()

            // Test unarchiving subcategory with parent
            categoryRepository.unarchiveCategory(subId, unarchiveParent = true)
            assertThat(categoryRepository.getCategory(subId)?.isArchived).isFalse()
            assertThat(categoryRepository.getCategory(parentId)?.isArchived).isFalse()
        }
}
