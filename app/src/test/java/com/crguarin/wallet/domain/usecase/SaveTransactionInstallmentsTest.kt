package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SaveTransactionInstallmentsTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var saveTransactionUseCase: SaveTransactionUseCase

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
    private val cashAccount =
        Account(
            id = 2L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(500_000L),
            color = 2,
            icon = "wallet",
            createdAt = baseInstant,
        )
    private val expenseCategory =
        Category(id = 1L, name = "Compras", type = CategoryType.EXPENSE, color = 1, icon = "cart")
    private val incomeCategory =
        Category(id = 2L, name = "Devolución", type = CategoryType.INCOME, color = 2, icon = "refund")

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        accountRepository.setInitialData(listOf(cardAccount, cashAccount))
        categoryRepository.setInitialData(listOf(expenseCategory, incomeCategory))
        saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = FakeTimeProvider(baseInstant),
                checkNegativeBalanceUseCase =
                    CheckNegativeBalanceUseCase(
                        accountRepository = accountRepository,
                        transactionRepository = transactionRepository,
                    ),
            )
    }

    private fun expense(
        accountId: Long = 1L,
        installments: Int? = null,
        categoryId: Long = 1L,
    ) = Transaction(
        type = TransactionType.EXPENSE,
        amount = Money(60_000L),
        accountId = accountId,
        categoryId = categoryId,
        date = baseDate,
        installments = installments,
        createdAt = baseInstant,
        updatedAt = baseInstant,
    )

    @Test
    fun `card expense with installments in range saves them`() =
        runTest {
            val result = saveTransactionUseCase(expense(installments = 12))
            assertThat(result.isSuccess).isTrue()
            val saved = transactionRepository.getTransaction(result.getOrNull()!!.transactionId)!!
            assertThat(saved.installments).isEqualTo(12)
        }

    @Test
    fun `card expense without installments succeeds`() =
        runTest {
            assertThat(saveTransactionUseCase(expense()).isSuccess).isTrue()
        }

    @Test
    fun `card expense with installments outside 1-48 fails`() =
        runTest {
            assertThat(saveTransactionUseCase(expense(installments = 0)).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
            assertThat(saveTransactionUseCase(expense(installments = 49)).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `non-card expense with installments fails`() =
        runTest {
            assertThat(saveTransactionUseCase(expense(accountId = 2L, installments = 3)).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `card income with installments fails`() =
        runTest {
            val income =
                expense(installments = 3).copy(
                    type = TransactionType.INCOME,
                    categoryId = 2L,
                )
            assertThat(saveTransactionUseCase(income).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `transfer with installments fails`() =
        runTest {
            val transfer =
                Transaction(
                    type = TransactionType.TRANSFER,
                    amount = Money(10_000L),
                    accountId = 2L,
                    toAccountId = 1L,
                    date = baseDate,
                    installments = 3,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(transfer).errorOrNull())
                .isEqualTo(DomainError.InvalidTransaction)
        }

    @Test
    fun `validate rejects without saving`() =
        runTest {
            val result = saveTransactionUseCase.validate(expense(accountId = 2L, installments = 3))
            assertThat(result.errorOrNull()).isEqualTo(DomainError.InvalidTransaction)
            assertThat(transactionRepository.getTransaction(1L)).isNull()
        }
}
