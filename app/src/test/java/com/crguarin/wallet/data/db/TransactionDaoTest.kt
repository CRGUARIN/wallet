package com.crguarin.wallet.data.db

import android.content.Context
import androidx.paging.PagingSource
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
class TransactionDaoTest {
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

    private suspend fun insertTx(
        type: TransactionType,
        amount: Long,
        accountId: Long = accountCashId,
        toAccountId: Long? = null,
        categoryId: Long? = null,
        date: LocalDate = LocalDate.of(2026, 9, 15),
        note: String? = null,
        createdAt: Instant = Instant.ofEpochMilli(2000L),
    ): Long =
        transactionDao.insert(
            TransactionEntity(
                type = type,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                date = date,
                note = note,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )

    @Test
    fun observeTransactions_filterByDateRange() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, categoryId = rootCatTransportId, date = LocalDate.of(2026, 9, 1))
            insertTx(type = TransactionType.EXPENSE, amount = 20L, categoryId = rootCatTransportId, date = LocalDate.of(2026, 9, 15))
            insertTx(type = TransactionType.EXPENSE, amount = 30L, categoryId = rootCatTransportId, date = LocalDate.of(2026, 9, 30))

            transactionDao
                .observeTransactions(
                    startDate = LocalDate.of(2026, 9, 10),
                    endDate = LocalDate.of(2026, 9, 20),
                ).test {
                    val list = awaitItem()
                    assertThat(list).hasSize(1)
                    assertThat(list.first().transaction.amount).isEqualTo(20L)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun observeTransactions_filterByAccountId_matchesSourceAndDestination() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, accountId = accountCashId, categoryId = rootCatFoodId)
            insertTx(type = TransactionType.EXPENSE, amount = 20L, accountId = accountSavingsId, categoryId = rootCatFoodId)
            insertTx(type = TransactionType.TRANSFER, amount = 30L, accountId = accountCashId, toAccountId = accountSavingsId)

            // Filter by savings account: should match the second expense and the transfer where it is destination
            transactionDao.observeTransactions(accountId = accountSavingsId).test {
                val list = awaitItem()
                assertThat(list).hasSize(2)
                assertThat(list.map { it.transaction.amount }).containsExactly(30L, 20L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTransactions_filterByRootCategory_includesSubcategories() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 15L, categoryId = rootCatFoodId)
            insertTx(type = TransactionType.EXPENSE, amount = 25L, categoryId = subCatGroceriesId)
            insertTx(type = TransactionType.EXPENSE, amount = 35L, categoryId = rootCatTransportId)

            // Filter by root food category: should return both food and groceries transactions
            transactionDao.observeTransactions(categoryId = rootCatFoodId).test {
                val list = awaitItem()
                assertThat(list).hasSize(2)
                assertThat(list.map { it.transaction.amount }).containsExactly(25L, 15L)
                cancelAndIgnoreRemainingEvents()
            }

            // Filter by subcategory: should only return the subcategory transaction
            transactionDao.observeTransactions(categoryId = subCatGroceriesId).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.amount).isEqualTo(25L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTransactions_filterByType() =
        runTest {
            insertTx(type = TransactionType.INCOME, amount = 100L, categoryId = incomeCatSalaryId)
            insertTx(type = TransactionType.EXPENSE, amount = 40L, categoryId = rootCatFoodId)
            insertTx(type = TransactionType.TRANSFER, amount = 50L, toAccountId = accountSavingsId)

            transactionDao.observeTransactions(type = TransactionType.INCOME).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.type).isEqualTo(TransactionType.INCOME)
                cancelAndIgnoreRemainingEvents()
            }

            transactionDao.observeTransactions(type = TransactionType.TRANSFER).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.type).isEqualTo(TransactionType.TRANSFER)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTransactions_filterByNote_substringAndCaseInsensitive() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, categoryId = rootCatFoodId, note = "Almuerzo con amigos")
            insertTx(type = TransactionType.EXPENSE, amount = 20L, categoryId = rootCatFoodId, note = "Cena familiar")
            insertTx(type = TransactionType.EXPENSE, amount = 30L, categoryId = rootCatFoodId, note = null)

            transactionDao.observeTransactions(noteQuery = "almuerzo").test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.amount).isEqualTo(10L)
                cancelAndIgnoreRemainingEvents()
            }

            transactionDao.observeTransactions(noteQuery = "FAMILIAR").test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.amount).isEqualTo(20L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTransactions_combinedFilters() =
        runTest {
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 10L,
                accountId = accountCashId,
                categoryId = subCatGroceriesId,
                date = LocalDate.of(2026, 9, 15),
                note = "Mercado del mes",
            )
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 20L,
                accountId = accountCashId,
                categoryId = rootCatTransportId,
                date = LocalDate.of(2026, 9, 15),
                note = "Taxi",
            )
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 30L,
                accountId = accountSavingsId,
                categoryId = subCatGroceriesId,
                date = LocalDate.of(2026, 9, 15),
                note = "Mercado extra",
            )

            transactionDao
                .observeTransactions(
                    startDate = LocalDate.of(2026, 9, 1),
                    endDate = LocalDate.of(2026, 9, 30),
                    accountId = accountCashId,
                    categoryId = rootCatFoodId, // includes subcategory
                    type = TransactionType.EXPENSE,
                    noteQuery = "mercado",
                ).test {
                    val list = awaitItem()
                    assertThat(list).hasSize(1)
                    assertThat(list.first().transaction.amount).isEqualTo(10L)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun observeRecent_respectsLimitAndOrdering() =
        runTest {
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 1L,
                categoryId = rootCatFoodId,
                date = LocalDate.of(2026, 9, 1),
                createdAt = Instant.ofEpochMilli(100),
            )
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 2L,
                categoryId = rootCatFoodId,
                date = LocalDate.of(2026, 9, 2),
                createdAt = Instant.ofEpochMilli(200),
            )
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 3L,
                categoryId = rootCatFoodId,
                date = LocalDate.of(2026, 9, 3),
                createdAt = Instant.ofEpochMilli(300),
            )

            transactionDao.observeRecent(limit = 2).test {
                val list = awaitItem()
                assertThat(list).hasSize(2)
                assertThat(list[0].transaction.amount).isEqualTo(3L)
                assertThat(list[1].transaction.amount).isEqualTo(2L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getWithDetailsById_populatesAllJoins() =
        runTest {
            val txId =
                insertTx(
                    type = TransactionType.EXPENSE,
                    amount = 55_000L,
                    accountId = accountCashId,
                    categoryId = subCatGroceriesId,
                    date = LocalDate.of(2026, 9, 20),
                    note = "Supermercado Éxito",
                )

            val details = transactionDao.getWithDetailsById(txId)
            assertThat(details).isNotNull()
            assertThat(details!!.accountName).isEqualTo("Efectivo")
            assertThat(details.accountColor).isEqualTo(0xFF00FF00.toInt())
            assertThat(details.accountIcon).isEqualTo("wallet")

            assertThat(details.categoryName).isEqualTo("Mercado")
            assertThat(details.categorySystemKey).isEqualTo("groceries")
            assertThat(details.categoryColor).isEqualTo(0xFFFF0000.toInt())
            assertThat(details.categoryIcon).isEqualTo("shopping_cart")

            assertThat(details.parentCategoryName).isEqualTo("Alimentación")
            assertThat(details.parentCategorySystemKey).isEqualTo("food")

            assertThat(details.toAccountName).isNull()
        }

    @Test
    fun getWithDetailsById_transfer_populatesDestinationAccount() =
        runTest {
            val txId =
                insertTx(
                    type = TransactionType.TRANSFER,
                    amount = 200_000L,
                    accountId = accountCashId,
                    toAccountId = accountSavingsId,
                    categoryId = null,
                )

            val details = transactionDao.getWithDetailsById(txId)
            assertThat(details).isNotNull()
            assertThat(details!!.accountName).isEqualTo("Efectivo")
            assertThat(details.toAccountName).isEqualTo("Ahorros")
            assertThat(details.toAccountColor).isEqualTo(0xFF0000FF.toInt())
            assertThat(details.toAccountIcon).isEqualTo("savings")
            assertThat(details.categoryName).isNull()
        }

    @Test
    fun observeMonthSummary_reactiveAndExcludesTransfers() =
        runTest {
            transactionDao
                .observeMonthSummary(
                    from = LocalDate.of(2026, 9, 1),
                    to = LocalDate.of(2026, 9, 30),
                ).test {
                    val empty = awaitItem()
                    assertThat(empty.income).isEqualTo(0L)
                    assertThat(empty.expense).isEqualTo(0L)
                    assertThat(empty.net).isEqualTo(0L)

                    // Insert income
                    insertTx(
                        type = TransactionType.INCOME,
                        amount = 1_000L,
                        categoryId = incomeCatSalaryId,
                        date = LocalDate.of(2026, 9, 5),
                    )
                    val afterIncome = awaitItem()
                    assertThat(afterIncome.income).isEqualTo(1_000L)
                    assertThat(afterIncome.expense).isEqualTo(0L)
                    assertThat(afterIncome.net).isEqualTo(1_000L)

                    // Insert expense
                    insertTx(type = TransactionType.EXPENSE, amount = 300L, categoryId = rootCatFoodId, date = LocalDate.of(2026, 9, 10))
                    val afterExpense = awaitItem()
                    assertThat(afterExpense.income).isEqualTo(1_000L)
                    assertThat(afterExpense.expense).isEqualTo(300L)
                    assertThat(afterExpense.net).isEqualTo(700L)

                    // Insert transfer (should not change income or expense)
                    insertTx(
                        type = TransactionType.TRANSFER,
                        amount = 500L,
                        toAccountId = accountSavingsId,
                        date = LocalDate.of(2026, 9, 12),
                    )
                    // Room may re-emit on invalidation; the values must not change
                    assertThat(
                        transactionDao.getMonthSummary(
                            from = LocalDate.of(2026, 9, 1),
                            to = LocalDate.of(2026, 9, 30),
                        ),
                    ).isEqualTo(afterExpense)

                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun getMonthSummary_filtersByAccount() =
        runTest {
            val from = LocalDate.of(2026, 9, 1)
            val to = LocalDate.of(2026, 9, 30)
            insertTx(type = TransactionType.INCOME, amount = 1_000L, categoryId = incomeCatSalaryId)
            insertTx(type = TransactionType.EXPENSE, amount = 300L, categoryId = rootCatFoodId)
            insertTx(
                type = TransactionType.EXPENSE,
                amount = 200L,
                accountId = accountSavingsId,
                categoryId = rootCatFoodId,
            )

            val cash = transactionDao.getMonthSummary(from, to, accountId = accountCashId)
            assertThat(cash.income).isEqualTo(1_000L)
            assertThat(cash.expense).isEqualTo(300L)

            val savings = transactionDao.getMonthSummary(from, to, accountId = accountSavingsId)
            assertThat(savings.income).isEqualTo(0L)
            assertThat(savings.expense).isEqualTo(200L)

            val all = transactionDao.getMonthSummary(from, to)
            assertThat(all.expense).isEqualTo(500L)
        }

    @Test
    fun observeTransactionsPaged_loadsCorrectData() =
        runTest {
            insertTx(type = TransactionType.EXPENSE, amount = 10L, categoryId = rootCatFoodId, date = LocalDate.of(2026, 9, 1))
            insertTx(type = TransactionType.EXPENSE, amount = 20L, categoryId = rootCatFoodId, date = LocalDate.of(2026, 9, 2))

            val pagingSource = transactionDao.observeTransactionsPaged()
            val loadResult =
                pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = 10,
                        placeholdersEnabled = false,
                    ),
                )

            assertThat(loadResult).isInstanceOf(PagingSource.LoadResult.Page::class.java)
            val page = loadResult as PagingSource.LoadResult.Page<Int, TransactionWithDetails>
            assertThat(page.data).hasSize(2)
            assertThat(page.data[0].transaction.amount).isEqualTo(20L)
            assertThat(page.data[1].transaction.amount).isEqualTo(10L)
        }
}
