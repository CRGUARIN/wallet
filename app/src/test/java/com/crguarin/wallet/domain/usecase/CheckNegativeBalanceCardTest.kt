package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CheckNegativeBalanceCardTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var checkNegativeBalanceUseCase: CheckNegativeBalanceUseCase

    private val baseDate = LocalDate.of(2026, 9, 28)
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")

    private val cardAccount =
        Account(
            id = 1L,
            name = "RappiCard",
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-200_000L),
            color = 1,
            icon = "credit_card",
            createdAt = baseInstant,
            creditLimit = Money(1_000_000L),
            statementDay = 15,
            paymentDueDay = 25,
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
        accountRepository.setInitialData(
            listOf(cardAccount, savingsAccount),
            balanceMap = mapOf(1L to Money(-200_000L), 2L to Money(500_000L)),
        )
        checkNegativeBalanceUseCase =
            CheckNegativeBalanceUseCase(
                accountRepository = accountRepository,
                transactionRepository = transactionRepository,
            )
    }

    private fun cardExpense(
        amount: Long,
        id: Long = 0L,
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(amount),
        accountId = 1L,
        categoryId = 1L,
        date = baseDate,
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    @Test
    fun `card expense within limit returns none`() =
        runTest {
            // Balance -200_000; new expense 500_000 -> -700_000, above -1_000_000.
            assertThat(checkNegativeBalanceUseCase(cardExpense(500_000L)))
                .isEqualTo(BalanceWarning.NONE)
        }

    @Test
    fun `card expense exceeding limit returns over credit limit`() =
        runTest {
            // Balance -200_000; new expense 900_000 -> -1_100_000, below -1_000_000.
            assertThat(checkNegativeBalanceUseCase(cardExpense(900_000L)))
                .isEqualTo(BalanceWarning.OVER_CREDIT_LIMIT)
        }

    @Test
    fun `card expense never returns negative balance`() =
        runTest {
            assertThat(checkNegativeBalanceUseCase(cardExpense(50_000_000L)))
                .isEqualTo(BalanceWarning.OVER_CREDIT_LIMIT)
        }

    @Test
    fun `card income never warns`() =
        runTest {
            val income = cardExpense(50_000_000L).copy(type = TransactionType.INCOME)
            assertThat(checkNegativeBalanceUseCase(income)).isEqualTo(BalanceWarning.NONE)
        }

    @Test
    fun `card transfer out exceeding limit warns`() =
        runTest {
            val cashAdvance =
                cardExpense(900_000L).copy(type = TransactionType.TRANSFER, toAccountId = 2L)
            assertThat(checkNegativeBalanceUseCase(cashAdvance))
                .isEqualTo(BalanceWarning.OVER_CREDIT_LIMIT)
        }

    @Test
    fun `transfer into card evaluates the source account`() =
        runTest {
            val payment =
                Transaction(
                    type = TransactionType.TRANSFER,
                    amount = Money(100_000L),
                    accountId = 2L,
                    toAccountId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            // Savings keeps 400_000: no warning under the classic rule.
            assertThat(checkNegativeBalanceUseCase(payment)).isEqualTo(BalanceWarning.NONE)

            val bigPayment = payment.copy(amount = Money(600_000L))
            assertThat(checkNegativeBalanceUseCase(bigPayment))
                .isEqualTo(BalanceWarning.NEGATIVE_BALANCE)
        }

    @Test
    fun `editing card expense with same amount does not warn`() =
        runTest {
            // Current balance already reflects the old 900_000 expense: -1_100_000.
            accountRepository.setBalance(1L, Money(-1_100_000L))
            val existing = cardExpense(900_000L, id = 10L)
            transactionRepository.setInitialData(listOf(existing))

            // Baseline without old effect: -200_000; same expense -> -1_100_000 is over limit?
            // -1_100_000 < -1_000_000, so re-saving the same over-limit expense still warns.
            assertThat(checkNegativeBalanceUseCase(existing.copy()))
                .isEqualTo(BalanceWarning.OVER_CREDIT_LIMIT)
        }

    @Test
    fun `editing card expense lowering amount clears warning`() =
        runTest {
            accountRepository.setBalance(1L, Money(-1_100_000L))
            val existing = cardExpense(900_000L, id = 10L)
            transactionRepository.setInitialData(listOf(existing))

            // Baseline -200_000; new expense 500_000 -> -700_000, within limit.
            assertThat(checkNegativeBalanceUseCase(existing.copy(amount = Money(500_000L))))
                .isEqualTo(BalanceWarning.NONE)
        }
}
