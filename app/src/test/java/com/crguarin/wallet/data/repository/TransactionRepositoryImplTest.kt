package com.crguarin.wallet.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionFilter
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
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
class TransactionRepositoryImplTest {
    private lateinit var database: WalletDatabase
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl

    private val now = Instant.parse("2026-09-28T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        transactionRepository = TransactionRepositoryImpl(database.transactionDao(), Dispatchers.Unconfined)
        accountRepository = AccountRepositoryImpl(database.accountDao(), database, Dispatchers.Unconfined)
        categoryRepository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `save, get and delete transaction`() =
        runTest {
            val accId =
                accountRepository.saveAccount(
                    Account(
                        name = "Efectivo",
                        type = AccountType.CASH,
                        initialBalance = Money(100_000L),
                        color = 1,
                        icon = "w",
                        createdAt = now,
                    ),
                )
            val catId =
                categoryRepository.saveCategory(
                    Category(name = "Comida", type = CategoryType.EXPENSE, color = 1, icon = "r"),
                )

            val tx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(20_000L),
                    accountId = accId,
                    categoryId = catId,
                    date = LocalDate.of(2026, 9, 28),
                    note = "Almuerzo",
                    createdAt = now,
                    updatedAt = now,
                )

            val id = transactionRepository.saveTransaction(tx)
            assertThat(id).isGreaterThan(0L)

            val loaded = transactionRepository.getTransaction(id)
            assertThat(loaded?.amount).isEqualTo(Money(20_000L))
            assertThat(loaded?.note).isEqualTo("Almuerzo")

            val details = transactionRepository.getTransactionDetails(id)
            assertThat(details?.accountName).isEqualTo("Efectivo")
            assertThat(details?.categoryName).isEqualTo("Comida")

            transactionRepository.deleteTransaction(id)
            assertThat(transactionRepository.getTransaction(id)).isNull()
        }

    @Test
    fun `observeTransactions with filters works correctly`() =
        runTest {
            val a1Id =
                accountRepository.saveAccount(
                    Account(name = "A1", type = AccountType.CASH, initialBalance = Money(100_000L), color = 1, icon = "w", createdAt = now),
                )
            val a2Id =
                accountRepository.saveAccount(
                    Account(name = "A2", type = AccountType.CASH, initialBalance = Money(100_000L), color = 2, icon = "w", createdAt = now),
                )
            val catId =
                categoryRepository.saveCategory(
                    Category(name = "C1", type = CategoryType.EXPENSE, color = 1, icon = "r"),
                )

            val tx1 =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(10_000L),
                    accountId = a1Id,
                    categoryId = catId,
                    date = LocalDate.of(2026, 9, 10),
                    note = "Note A",
                    createdAt = now,
                    updatedAt = now,
                )
            val tx2 =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(20_000L),
                    accountId = a2Id,
                    categoryId = catId,
                    date = LocalDate.of(2026, 9, 20),
                    note = "Note B",
                    createdAt = now,
                    updatedAt = now,
                )
            transactionRepository.saveTransaction(tx1)
            transactionRepository.saveTransaction(tx2)

            // Filter by account a1
            transactionRepository.observeTransactions(TransactionFilter(accountId = a1Id)).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.accountId).isEqualTo(a1Id)
                cancelAndIgnoreRemainingEvents()
            }

            // Filter by note query
            transactionRepository.observeTransactions(TransactionFilter(noteQuery = "Note B")).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().transaction.note).isEqualTo("Note B")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `restoreTransaction keeps the same id and fields`() =
        runTest {
            val accId =
                accountRepository.saveAccount(
                    Account(
                        name = "Efectivo",
                        type = AccountType.CASH,
                        initialBalance = Money(100_000L),
                        color = 1,
                        icon = "w",
                        createdAt = now,
                    ),
                )
            val catId =
                categoryRepository.saveCategory(
                    Category(name = "Comida", type = CategoryType.EXPENSE, color = 1, icon = "r"),
                )
            val id =
                transactionRepository.saveTransaction(
                    Transaction(
                        type = TransactionType.EXPENSE,
                        amount = Money(20_000L),
                        accountId = accId,
                        categoryId = catId,
                        date = LocalDate.of(2026, 9, 28),
                        note = "Almuerzo",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            val saved = transactionRepository.getTransaction(id)!!

            transactionRepository.deleteTransaction(id)
            assertThat(transactionRepository.getTransaction(id)).isNull()

            transactionRepository.restoreTransaction(saved)

            val restored = transactionRepository.getTransaction(id)
            assertThat(restored).isEqualTo(saved)
            assertThat(restored?.createdAt).isEqualTo(now)
            assertThat(restored?.updatedAt).isEqualTo(now)
        }

    @Test
    fun `observeDailyTotals signs transfers only when filtering by account`() =
        runTest {
            val cashId =
                accountRepository.saveAccount(
                    Account(name = "Efectivo", type = AccountType.CASH, initialBalance = Money(0), color = 1, icon = "w", createdAt = now),
                )
            val savingsId =
                accountRepository.saveAccount(
                    Account(
                        name = "Ahorros",
                        type = AccountType.SAVINGS,
                        initialBalance = Money(0),
                        color = 2,
                        icon = "s",
                        createdAt = now,
                    ),
                )
            val expenseCat =
                categoryRepository.saveCategory(
                    Category(name = "Comida", type = CategoryType.EXPENSE, color = 1, icon = "r"),
                )
            val day = LocalDate.of(2026, 9, 15)

            transactionRepository.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(30_000L),
                    accountId = cashId,
                    categoryId = expenseCat,
                    date = day,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            transactionRepository.saveTransaction(
                Transaction(
                    type = TransactionType.TRANSFER,
                    amount = Money(50_000L),
                    accountId = cashId,
                    toAccountId = savingsId,
                    date = day,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

            val all = transactionRepository.observeDailyTotals(TransactionFilter(startDate = day, endDate = day)).first()
            val cash =
                transactionRepository
                    .observeDailyTotals(TransactionFilter(startDate = day, endDate = day, accountId = cashId))
                    .first()
            val savings =
                transactionRepository
                    .observeDailyTotals(TransactionFilter(startDate = day, endDate = day, accountId = savingsId))
                    .first()

            assertThat(all[day]).isEqualTo(Money(-30_000L))
            assertThat(cash[day]).isEqualTo(Money(-80_000L))
            assertThat(savings[day]).isEqualTo(Money(50_000L))
        }
}
