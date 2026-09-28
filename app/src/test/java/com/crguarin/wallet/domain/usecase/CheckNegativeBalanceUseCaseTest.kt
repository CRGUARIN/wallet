package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CheckNegativeBalanceUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var checkNegativeBalanceUseCase: CheckNegativeBalanceUseCase

    private val baseDate = LocalDate.of(2026, 9, 28)
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")

    private val cashAccount =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(50_000L),
            color = 1,
            icon = "wallet",
            createdAt = baseInstant,
        )
    private val savingsAccount =
        Account(
            id = 2L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            initialBalance = Money(500_000L),
            color = 2,
            icon = "savings",
            createdAt = baseInstant,
        )

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        accountRepository.setInitialData(listOf(cashAccount, savingsAccount))
        checkNegativeBalanceUseCase =
            CheckNegativeBalanceUseCase(
                accountRepository = accountRepository,
                transactionRepository = transactionRepository,
            )
    }

    private fun expense(
        amount: Long,
        accountId: Long = 1L,
        id: Long = 0L,
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(amount),
        accountId = accountId,
        categoryId = 1L,
        date = baseDate,
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    @Test
    fun `new expense exceeding balance returns true`() =
        runTest {
            assertThat(checkNegativeBalanceUseCase(expense(60_000L))).isTrue()
        }

    @Test
    fun `new expense within balance returns false`() =
        runTest {
            assertThat(checkNegativeBalanceUseCase(expense(30_000L))).isFalse()
        }

    @Test
    fun `income never returns true`() =
        runTest {
            val income =
                expense(1_000_000L).copy(type = TransactionType.INCOME)
            assertThat(checkNegativeBalanceUseCase(income)).isFalse()
        }

    @Test
    fun `editing with same amount does not warn`() =
        runTest {
            // Current balance already reflects the old 60_000 expense.
            accountRepository.setBalance(1L, Money(40_000L))
            val existing = expense(60_000L, id = 10L)
            transactionRepository.setInitialData(listOf(existing))

            assertThat(checkNegativeBalanceUseCase(existing.copy())).isFalse()
        }

    @Test
    fun `editing increasing amount warns when new total exceeds balance`() =
        runTest {
            accountRepository.setBalance(1L, Money(40_000L))
            val existing = expense(60_000L, id = 10L)
            transactionRepository.setInitialData(listOf(existing))

            // Baseline without old effect: 100_000; new expense 120_000 -> negative.
            assertThat(checkNegativeBalanceUseCase(existing.copy(amount = Money(120_000L)))).isTrue()
        }

    @Test
    fun `editing moving expense to another account evaluates the new account`() =
        runTest {
            accountRepository.setBalance(1L, Money(40_000L))
            val existing = expense(60_000L, id = 10L)
            transactionRepository.setInitialData(listOf(existing))

            // Savings has 500_000; moving a 60_000 expense there is safe.
            assertThat(checkNegativeBalanceUseCase(existing.copy(accountId = 2L))).isFalse()
            // Moving a 600_000 expense there is not.
            assertThat(checkNegativeBalanceUseCase(existing.copy(accountId = 2L, amount = Money(600_000L)))).isTrue()
        }
}
