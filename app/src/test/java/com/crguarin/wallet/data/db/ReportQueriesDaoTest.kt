package com.crguarin.wallet.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
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
class ReportQueriesDaoTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao

    private var accountCashId: Long = 0
    private var accountSavingsId: Long = 0
    private var rootCatFoodId: Long = 0
    private var subCatGroceriesId: Long = 0
    private var rootCatTransportId: Long = 0
    private var incomeCatSalaryId: Long = 0

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database =
                Room
                    .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            accountDao = database.accountDao()
            categoryDao = database.categoryDao()
            transactionDao = database.transactionDao()

            accountCashId =
                accountDao.insert(
                    AccountEntity(
                        name = "Efectivo",
                        type = AccountType.CASH,
                        currencyCode = "COP",
                        initialBalance = 1_000_000L,
                        color = 0xFF00FF00.toInt(),
                        icon = "wallet",
                        createdAt = Instant.ofEpochMilli(1000L),
                    ),
                )
            accountSavingsId =
                accountDao.insert(
                    AccountEntity(
                        name = "Ahorros",
                        type = AccountType.SAVINGS,
                        currencyCode = "COP",
                        initialBalance = 5_000_000L,
                        color = 0xFF0000FF.toInt(),
                        icon = "savings",
                        createdAt = Instant.ofEpochMilli(1000L),
                    ),
                )
            rootCatFoodId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Alimentación",
                        systemKey = "food",
                        type = CategoryType.EXPENSE,
                        parentId = null,
                        color = 0xFFFF0000.toInt(),
                        icon = "restaurant",
                    ),
                )
            subCatGroceriesId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Mercado",
                        systemKey = "groceries",
                        type = CategoryType.EXPENSE,
                        parentId = rootCatFoodId,
                        color = 0xFFFF0000.toInt(),
                        icon = "shopping_cart",
                    ),
                )
            rootCatTransportId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Transporte",
                        systemKey = "transport",
                        type = CategoryType.EXPENSE,
                        parentId = null,
                        color = 0xFFFFFF00.toInt(),
                        icon = "directions_bus",
                    ),
                )
            incomeCatSalaryId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Salario",
                        systemKey = "salary",
                        type = CategoryType.INCOME,
                        parentId = null,
                        color = 0xFF00FFFF.toInt(),
                        icon = "work",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeCategoryTotals_groupsByCategoryAndBringsParentData() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, categoryId = rootCatFoodId)
            insertTx(type = TransactionType.EXPENSE, amount = 25L, categoryId = subCatGroceriesId)
            insertTx(type = TransactionType.EXPENSE, amount = 5L, categoryId = subCatGroceriesId)
            insertTx(type = TransactionType.EXPENSE, amount = 40L, categoryId = rootCatTransportId)

            val rows = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.EXPENSE, null).first()

            assertThat(rows).hasSize(3)
            val groceries = rows.single { it.categoryId == subCatGroceriesId }
            assertThat(groceries.categoryName).isEqualTo("Mercado")
            assertThat(groceries.categorySystemKey).isEqualTo("groceries")
            assertThat(groceries.categoryColor).isEqualTo(0xFFFF0000.toInt())
            assertThat(groceries.categoryIcon).isEqualTo("shopping_cart")
            assertThat(groceries.parentId).isEqualTo(rootCatFoodId)
            assertThat(groceries.parentName).isEqualTo("Alimentación")
            assertThat(groceries.parentSystemKey).isEqualTo("food")
            assertThat(groceries.parentColor).isEqualTo(0xFFFF0000.toInt())
            assertThat(groceries.parentIcon).isEqualTo("restaurant")
            assertThat(groceries.total).isEqualTo(30L)
            assertThat(groceries.transactionCount).isEqualTo(2)

            val food = rows.single { it.categoryId == rootCatFoodId }
            assertThat(food.categoryName).isEqualTo("Alimentación")
            assertThat(food.parentId).isNull()
            assertThat(food.parentName).isNull()
            assertThat(food.total).isEqualTo(10L)
            assertThat(food.transactionCount).isEqualTo(1)

            val transport = rows.single { it.categoryId == rootCatTransportId }
            assertThat(transport.total).isEqualTo(40L)
            assertThat(transport.parentId).isNull()
        }

    @Test
    fun observeCategoryTotals_respectsInclusiveBoundsAndAccountFilter() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, accountId = accountCashId, categoryId = rootCatFoodId, date = SEP_START)
            insertTx(type = TransactionType.EXPENSE, amount = 20L, accountId = accountCashId, categoryId = rootCatFoodId, date = SEP_END)
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 100L,
                accountId = accountCashId,
                categoryId = rootCatFoodId,
                date = SEP_START.minusDays(1),
            )
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 200L,
                accountId = accountCashId,
                categoryId = rootCatFoodId,
                date = SEP_END.plusDays(1),
            )
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 400L,
                accountId = accountSavingsId,
                categoryId = rootCatFoodId,
                date = LocalDate.of(2026, 9, 15),
            )

            val cash = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.EXPENSE, accountCashId).first()
            assertThat(cash).hasSize(1)
            assertThat(cash.first().total).isEqualTo(30L)
            assertThat(cash.first().transactionCount).isEqualTo(2)

            val all = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.EXPENSE, null).first()
            assertThat(all).hasSize(1)
            assertThat(all.first().total).isEqualTo(430L)
            assertThat(all.first().transactionCount).isEqualTo(3)
        }

    @Test
    fun observeCategoryTotals_excludesTransfersAndOtherTypes() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, categoryId = rootCatFoodId)
            insertTx(type = TransactionType.INCOME, amount = 500L, categoryId = incomeCatSalaryId)
            insertTx(type = TransactionType.TRANSFER, amount = 700L, toAccountId = accountSavingsId)

            val expenses = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.EXPENSE, null).first()
            assertThat(expenses).hasSize(1)
            assertThat(expenses.first().categoryId).isEqualTo(rootCatFoodId)
            assertThat(expenses.first().total).isEqualTo(10L)

            val incomes = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.INCOME, null).first()
            assertThat(incomes).hasSize(1)
            assertThat(incomes.first().categoryId).isEqualTo(incomeCatSalaryId)
            assertThat(incomes.first().total).isEqualTo(500L)
        }

    @Test
    fun observeCategoryTotals_includesArchivedCategories() =
        runTest {
            val archivedId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Café",
                        systemKey = "coffee",
                        type = CategoryType.EXPENSE,
                        parentId = null,
                        color = 0xFF123456.toInt(),
                        icon = "local_cafe",
                        isArchived = true,
                    ),
                )
            insertTx(type = TransactionType.EXPENSE, amount = 42L, categoryId = archivedId)

            val rows = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.EXPENSE, null).first()

            assertThat(rows).hasSize(1)
            assertThat(rows.first().categoryId).isEqualTo(archivedId)
            assertThat(rows.first().categoryName).isEqualTo("Café")
            assertThat(rows.first().total).isEqualTo(42L)
        }

    @Test
    fun observeCategoryTotals_groupsUncategorizedMovements() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 15L, categoryId = null)
            insertTx(type = TransactionType.EXPENSE, amount = 35L, categoryId = null)

            val rows = transactionDao.observeCategoryTotals(SEP_START, SEP_END, TransactionType.EXPENSE, null).first()

            assertThat(rows).hasSize(1)
            assertThat(rows.first().categoryId).isNull()
            assertThat(rows.first().categoryName).isNull()
            assertThat(rows.first().parentId).isNull()
            assertThat(rows.first().total).isEqualTo(50L)
            assertThat(rows.first().transactionCount).isEqualTo(2)
        }

    @Test
    fun observeDailyIncomeExpense_separatesTypesOrdersDaysAndExcludesTransfers() =
        runTest {
            insertTx(type = TransactionType.INCOME, amount = 100L, categoryId = incomeCatSalaryId, date = LocalDate.of(2026, 9, 1))
            insertTx(type = TransactionType.EXPENSE, amount = 5L, categoryId = rootCatFoodId, date = LocalDate.of(2026, 9, 1))
            insertTx(type = TransactionType.EXPENSE, amount = 20L, categoryId = rootCatFoodId, date = LocalDate.of(2026, 9, 2))
            insertTx(type = TransactionType.INCOME, amount = 200L, categoryId = incomeCatSalaryId, date = LocalDate.of(2026, 9, 3))
            insertTx(type = TransactionType.TRANSFER, amount = 50L, toAccountId = accountSavingsId, date = LocalDate.of(2026, 9, 3))
            insertTx(type = TransactionType.TRANSFER, amount = 60L, toAccountId = accountSavingsId, date = LocalDate.of(2026, 9, 4))

            val rows = transactionDao.observeDailyIncomeExpense(SEP_START, SEP_END, null).first()

            assertThat(rows.map { it.date })
                .containsExactly(
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 9, 3),
                ).inOrder()
            assertThat(rows[0].income).isEqualTo(100L)
            assertThat(rows[0].expense).isEqualTo(5L)
            assertThat(rows[1].income).isEqualTo(0L)
            assertThat(rows[1].expense).isEqualTo(20L)
            assertThat(rows[2].income).isEqualTo(200L)
            assertThat(rows[2].expense).isEqualTo(0L)
        }

    @Test
    fun observeDailyIncomeExpense_filtersByAccountAndInclusiveBounds() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, accountId = accountCashId, date = SEP_START)
            insertTx(type = TransactionType.EXPENSE, amount = 20L, accountId = accountCashId, date = SEP_END)
            insertTx(type = TransactionType.EXPENSE, amount = 30L, accountId = accountSavingsId, date = LocalDate.of(2026, 9, 15))
            insertTx(type = TransactionType.EXPENSE, amount = 40L, accountId = accountCashId, date = SEP_START.minusDays(1))
            insertTx(type = TransactionType.EXPENSE, amount = 50L, accountId = accountCashId, date = SEP_END.plusDays(1))

            val cash = transactionDao.observeDailyIncomeExpense(SEP_START, SEP_END, accountCashId).first()
            val all = transactionDao.observeDailyIncomeExpense(SEP_START, SEP_END, null).first()

            assertThat(cash.map { it.date }).containsExactly(SEP_START, SEP_END).inOrder()
            assertThat(cash.map { it.expense }).containsExactly(10L, 20L).inOrder()
            assertThat(all.map { it.expense }).containsExactly(10L, 30L, 20L).inOrder()
        }

    @Test
    fun observeBalanceDeltas_combinesIncomeExpenseAndTransferSides() =
        runTest {
            insertTx(type = TransactionType.INCOME, amount = 100L, date = LocalDate.of(2026, 9, 5))
            insertTx(type = TransactionType.EXPENSE, amount = 30L, date = LocalDate.of(2026, 9, 5))
            insertTx(
                type = TransactionType.TRANSFER,
                amount = 50L,
                toAccountId = accountSavingsId,
                date = LocalDate.of(2026, 9, 6),
            )
            insertTx(
                type = TransactionType.TRANSFER,
                amount = 20L,
                accountId = accountSavingsId,
                toAccountId = accountCashId,
                date = LocalDate.of(2026, 9, 7),
            )

            val rows =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountCashId),
                        SEP_START,
                        SEP_END,
                        SEP_START.minusDays(1),
                    ).first()

            assertThat(rows)
                .containsExactly(
                    BalanceDeltaRow(LocalDate.of(2026, 9, 5), 70L),
                    BalanceDeltaRow(LocalDate.of(2026, 9, 6), -50L),
                    BalanceDeltaRow(LocalDate.of(2026, 9, 7), 20L),
                ).inOrder()
        }

    @Test
    fun observeBalanceDeltas_transferBetweenSetAccountsNetsToZero() =
        runTest {
            insertTx(type = TransactionType.INCOME, amount = 100L, date = LocalDate.of(2026, 9, 10))
            insertTx(
                type = TransactionType.TRANSFER,
                amount = 50L,
                toAccountId = accountSavingsId,
                date = LocalDate.of(2026, 9, 10),
            )

            val rows =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountCashId, accountSavingsId),
                        SEP_START,
                        SEP_END,
                        SEP_START.minusDays(1),
                    ).first()

            assertThat(rows).containsExactly(BalanceDeltaRow(LocalDate.of(2026, 9, 10), 100L))
        }

    @Test
    fun observeBalanceDeltas_transferFromOutsideOnlyCountsTheDestinationSide() =
        runTest {
            insertTx(
                type = TransactionType.TRANSFER,
                amount = 50L,
                toAccountId = accountSavingsId,
                date = LocalDate.of(2026, 9, 10),
            )
            insertTx(type = TransactionType.EXPENSE, amount = 10L, date = LocalDate.of(2026, 9, 10))

            val rows =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountSavingsId),
                        SEP_START,
                        SEP_END,
                        SEP_START.minusDays(1),
                    ).first()

            // The cash expense is outside the set, so only the incoming +50 remains.
            assertThat(rows).containsExactly(BalanceDeltaRow(LocalDate.of(2026, 9, 10), 50L))
        }

    @Test
    fun observeBalanceDeltas_groupsMovementsBeforeFromIntoTheOpeningDate() =
        runTest {
            insertTx(type = TransactionType.INCOME, amount = 100L, date = LocalDate.of(2026, 8, 20))
            insertTx(type = TransactionType.EXPENSE, amount = 30L, date = SEP_START)
            insertTx(type = TransactionType.EXPENSE, amount = 5L, date = LocalDate.of(2026, 9, 5))

            val from = LocalDate.of(2026, 9, 2)
            val rows =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountCashId),
                        from,
                        SEP_END,
                        from.minusDays(1),
                    ).first()

            assertThat(rows)
                .containsExactly(
                    BalanceDeltaRow(LocalDate.of(2026, 9, 1), 70L),
                    BalanceDeltaRow(LocalDate.of(2026, 9, 5), -5L),
                ).inOrder()
        }

    @Test
    fun observeBalanceDeltas_toIsInclusiveAndLaterMovementsAreExcluded() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, date = SEP_END)
            insertTx(type = TransactionType.INCOME, amount = 999L, date = SEP_END.plusDays(1))

            val rows =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountCashId),
                        SEP_START,
                        SEP_END,
                        SEP_START.minusDays(1),
                    ).first()

            assertThat(rows).containsExactly(BalanceDeltaRow(SEP_END, -10L))
        }

    @Test
    fun observeBalanceDeltas_singleAccountSetIgnoresOtherAccounts() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, date = LocalDate.of(2026, 9, 10))
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 999L,
                accountId = accountSavingsId,
                date = LocalDate.of(2026, 9, 10),
            )

            val cash =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountCashId),
                        SEP_START,
                        SEP_END,
                        SEP_START.minusDays(1),
                    ).first()
            val both =
                transactionDao
                    .observeBalanceDeltas(
                        listOf(accountCashId, accountSavingsId),
                        SEP_START,
                        SEP_END,
                        SEP_START.minusDays(1),
                    ).first()

            assertThat(cash).containsExactly(BalanceDeltaRow(LocalDate.of(2026, 9, 10), -10L))
            assertThat(both).containsExactly(BalanceDeltaRow(LocalDate.of(2026, 9, 10), -1009L))
        }

    private suspend fun insertTx(
        type: TransactionType,
        amount: Long,
        accountId: Long = accountCashId,
        toAccountId: Long? = null,
        categoryId: Long? = null,
        date: LocalDate = LocalDate.of(2026, 9, 15),
    ): Long =
        transactionDao.insert(
            TransactionEntity(
                type = type,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                date = date,
                createdAt = Instant.ofEpochMilli(2000L),
                updatedAt = Instant.ofEpochMilli(2000L),
            ),
        )

    private companion object {
        private val SEP_START: LocalDate = LocalDate.of(2026, 9, 1)
        private val SEP_END: LocalDate = LocalDate.of(2026, 9, 30)
    }
}
