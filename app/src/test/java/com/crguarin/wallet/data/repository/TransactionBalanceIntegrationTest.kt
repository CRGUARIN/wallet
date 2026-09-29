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
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
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

/**
 * Verifies that saving, editing and deleting income/expense transactions is reflected in the
 * account balances observed through [ObserveAccountsUseCase] (the query used by the UI).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TransactionBalanceIntegrationTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var observeAccountsUseCase: ObserveAccountsUseCase
    private lateinit var observeTotalBalanceUseCase: ObserveTotalBalanceUseCase

    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val date = LocalDate.of(2026, 9, 28)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountRepository = AccountRepositoryImpl(database.accountDao(), database, Dispatchers.Unconfined)
        transactionRepository = TransactionRepositoryImpl(database.transactionDao(), Dispatchers.Unconfined)
        categoryRepository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
        observeAccountsUseCase = ObserveAccountsUseCase(accountRepository)
        observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun balanceOf(accountId: Long): Money =
        observeAccountsUseCase()
            .first()
            .first { it.account.id == accountId }
            .balance

    private suspend fun createAccount(
        name: String = "Efectivo",
        initialBalance: Money = Money(100_000L),
    ): Long =
        accountRepository.saveAccount(
            Account(
                name = name,
                type = AccountType.CASH,
                initialBalance = initialBalance,
                color = 1,
                icon = "wallet",
                createdAt = now,
            ),
        )

    private suspend fun totalBalance(): Money = observeTotalBalanceUseCase().first()

    private suspend fun createCategory(
        type: CategoryType,
        name: String,
    ): Long =
        categoryRepository.saveCategory(
            Category(name = name, type = type, color = 1, icon = "restaurant"),
        )

    private fun transaction(
        type: TransactionType,
        amount: Money,
        accountId: Long,
        categoryId: Long,
    ) = Transaction(
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = categoryId,
        date = date,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `saving an expense then an income updates the observed balance`() =
        runTest {
            val accountId = createAccount()
            val expenseCategoryId = createCategory(CategoryType.EXPENSE, "Comida")
            val incomeCategoryId = createCategory(CategoryType.INCOME, "Salario")

            assertThat(balanceOf(accountId)).isEqualTo(Money(100_000L))

            transactionRepository.saveTransaction(
                transaction(TransactionType.EXPENSE, Money(30_000L), accountId, expenseCategoryId),
            )
            assertThat(balanceOf(accountId)).isEqualTo(Money(70_000L))

            transactionRepository.saveTransaction(
                transaction(TransactionType.INCOME, Money(50_000L), accountId, incomeCategoryId),
            )
            assertThat(balanceOf(accountId)).isEqualTo(Money(120_000L))
        }

    @Test
    fun `editing a transaction recalculates the observed balance`() =
        runTest {
            val accountId = createAccount()
            val expenseCategoryId = createCategory(CategoryType.EXPENSE, "Comida")

            val expenseId =
                transactionRepository.saveTransaction(
                    transaction(TransactionType.EXPENSE, Money(30_000L), accountId, expenseCategoryId),
                )
            assertThat(balanceOf(accountId)).isEqualTo(Money(70_000L))

            val saved = transactionRepository.getTransaction(expenseId)!!
            transactionRepository.saveTransaction(saved.copy(amount = Money(10_000L)))
            assertThat(balanceOf(accountId)).isEqualTo(Money(90_000L))
        }

    @Test
    fun `deleting a transaction recalculates the observed balance`() =
        runTest {
            val accountId = createAccount()
            val expenseCategoryId = createCategory(CategoryType.EXPENSE, "Comida")
            val incomeCategoryId = createCategory(CategoryType.INCOME, "Salario")

            val expenseId =
                transactionRepository.saveTransaction(
                    transaction(TransactionType.EXPENSE, Money(30_000L), accountId, expenseCategoryId),
                )
            val incomeId =
                transactionRepository.saveTransaction(
                    transaction(TransactionType.INCOME, Money(50_000L), accountId, incomeCategoryId),
                )
            assertThat(balanceOf(accountId)).isEqualTo(Money(120_000L))

            transactionRepository.deleteTransaction(expenseId)
            assertThat(balanceOf(accountId)).isEqualTo(Money(150_000L))

            transactionRepository.deleteTransaction(incomeId)
            assertThat(balanceOf(accountId)).isEqualTo(Money(100_000L))
        }

    private fun transfer(
        amount: Money,
        accountId: Long,
        toAccountId: Long,
    ) = Transaction(
        type = TransactionType.TRANSFER,
        amount = amount,
        accountId = accountId,
        toAccountId = toAccountId,
        date = date,
        createdAt = now,
        updatedAt = now,
    )

    private suspend fun monthSummary(): MonthSummary =
        transactionRepository.getMonthSummary(
            from = date.withDayOfMonth(1),
            to = date.withDayOfMonth(date.lengthOfMonth()),
            accountId = null,
        )

    @Test
    fun `a transfer moves the balance without changing the total or the month summary`() =
        runTest {
            val originId = createAccount(name = "Efectivo", initialBalance = Money(100_000L))
            val destinationId = createAccount(name = "Ahorros", initialBalance = Money(50_000L))
            assertThat(totalBalance()).isEqualTo(Money(150_000L))

            val transferId =
                transactionRepository.saveTransaction(
                    transfer(Money(30_000L), originId, destinationId),
                )

            assertThat(balanceOf(originId)).isEqualTo(Money(70_000L))
            assertThat(balanceOf(destinationId)).isEqualTo(Money(80_000L))
            assertThat(totalBalance()).isEqualTo(Money(150_000L))

            val summary = monthSummary()
            assertThat(summary.income).isEqualTo(Money.ZERO)
            assertThat(summary.expense).isEqualTo(Money.ZERO)

            assertThat(transactionRepository.getTransaction(transferId)?.categoryId).isNull()
        }

    @Test
    fun `editing the transfer destination returns the amount and credits the new account`() =
        runTest {
            val originId = createAccount(name = "Efectivo", initialBalance = Money(100_000L))
            val firstDestinationId = createAccount(name = "Ahorros", initialBalance = Money(50_000L))
            val secondDestinationId = createAccount(name = "Inversión", initialBalance = Money.ZERO)

            val transferId =
                transactionRepository.saveTransaction(
                    transfer(Money(30_000L), originId, firstDestinationId),
                )
            assertThat(balanceOf(firstDestinationId)).isEqualTo(Money(80_000L))

            val saved = transactionRepository.getTransaction(transferId)!!
            transactionRepository.saveTransaction(saved.copy(toAccountId = secondDestinationId))

            assertThat(balanceOf(originId)).isEqualTo(Money(70_000L))
            assertThat(balanceOf(firstDestinationId)).isEqualTo(Money(50_000L))
            assertThat(balanceOf(secondDestinationId)).isEqualTo(Money(30_000L))
            assertThat(totalBalance()).isEqualTo(Money(150_000L))
        }

    @Test
    fun `deleting a transfer restores both balances`() =
        runTest {
            val originId = createAccount(name = "Efectivo", initialBalance = Money(100_000L))
            val destinationId = createAccount(name = "Ahorros", initialBalance = Money(50_000L))

            val transferId =
                transactionRepository.saveTransaction(
                    transfer(Money(30_000L), originId, destinationId),
                )
            assertThat(balanceOf(originId)).isEqualTo(Money(70_000L))
            assertThat(balanceOf(destinationId)).isEqualTo(Money(80_000L))

            transactionRepository.deleteTransaction(transferId)

            assertThat(balanceOf(originId)).isEqualTo(Money(100_000L))
            assertThat(balanceOf(destinationId)).isEqualTo(Money(50_000L))
            assertThat(totalBalance()).isEqualTo(Money(150_000L))
        }
}
