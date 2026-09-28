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

class SaveTransactionUseCaseTest {
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var saveTransactionUseCase: SaveTransactionUseCase

    private val baseDate = LocalDate.of(2026, 9, 28)
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")

    private val cashAccount =
        Account(
            id = 1L,
            name = "Efectivo",
            type = AccountType.CASH,
            initialBalance = Money(100_000L),
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
    private val foodCategory =
        Category(
            id = 1L,
            name = "Alimentación",
            type = CategoryType.EXPENSE,
            color = 1,
            icon = "restaurant",
        )
    private val salaryCategory =
        Category(
            id = 2L,
            name = "Salario",
            type = CategoryType.INCOME,
            color = 2,
            icon = "payments",
        )

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository()
        categoryRepository = FakeCategoryRepository()
        timeProvider = FakeTimeProvider(baseInstant)

        accountRepository.setInitialData(listOf(cashAccount, savingsAccount))
        categoryRepository.setInitialData(listOf(foodCategory, salaryCategory))

        saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = timeProvider,
            )
    }

    @Test
    fun `fails when amount is zero or negative`() =
        runTest {
            val zeroTx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money.ZERO,
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(zeroTx).errorOrNull()).isEqualTo(DomainError.AmountInvalid)

            val negTx = zeroTx.copy(amount = Money(-10L))
            assertThat(saveTransactionUseCase(negTx).errorOrNull()).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `fails when amount exceeds MAX`() =
        runTest {
            val bigTx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(Money.MAX_MINOR_UNITS + 1),
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(bigTx).errorOrNull()).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `fails when note exceeds 200 chars`() =
        runTest {
            val longNoteTx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate,
                    note = "N".repeat(201),
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(longNoteTx).errorOrNull()).isEqualTo(DomainError.NoteTooLong)
        }

    @Test
    fun `fails when date is beyond today plus 1 year`() =
        runTest {
            val futureTx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate.plusYears(1).plusDays(1),
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(futureTx).errorOrNull()).isEqualTo(DomainError.DateOutOfRange)
        }

    @Test
    fun `fails when expense or income is missing category`() =
        runTest {
            val noCatTx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 1L,
                    categoryId = null,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(noCatTx).errorOrNull()).isEqualTo(DomainError.CategoryRequired)
        }

    @Test
    fun `fails when category type does not match transaction type`() =
        runTest {
            // Expense with income category
            val mismatchTx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 1L,
                    categoryId = 2L, // salary (INCOME)
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(mismatchTx).errorOrNull()).isEqualTo(DomainError.CategoryTypeMismatch)
        }

    @Test
    fun `fails when transfer has category or missing destination or same account`() =
        runTest {
            val transferWithCat =
                Transaction(
                    type = TransactionType.TRANSFER,
                    amount = Money(1000L),
                    accountId = 1L,
                    toAccountId = 2L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(transferWithCat).errorOrNull()).isEqualTo(DomainError.InvalidTransaction)

            val transferNoDest = transferWithCat.copy(categoryId = null, toAccountId = null)
            assertThat(saveTransactionUseCase(transferNoDest).errorOrNull()).isEqualTo(DomainError.InvalidTransaction)

            val transferSameAcc = transferWithCat.copy(categoryId = null, toAccountId = 1L)
            assertThat(saveTransactionUseCase(transferSameAcc).errorOrNull()).isEqualTo(DomainError.SameAccountTransfer)
        }

    @Test
    fun `fails when account or category is archived on new transaction`() =
        runTest {
            val archivedAccount = cashAccount.copy(id = 3L, isArchived = true)
            val archivedCategory = foodCategory.copy(id = 3L, isArchived = true)
            accountRepository.setInitialData(listOf(cashAccount, savingsAccount, archivedAccount))
            categoryRepository.setInitialData(listOf(foodCategory, salaryCategory, archivedCategory))

            val txArchivedAccount =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 3L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(txArchivedAccount).errorOrNull()).isEqualTo(DomainError.AccountArchived)

            val txArchivedCategory =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 1L,
                    categoryId = 3L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            assertThat(saveTransactionUseCase(txArchivedCategory).errorOrNull()).isEqualTo(DomainError.CategoryArchived)
        }

    @Test
    fun `allows editing transaction preserving existing archived category or account`() =
        runTest {
            val archivedCategory = foodCategory.copy(id = 3L, isArchived = true)
            categoryRepository.setInitialData(listOf(foodCategory, salaryCategory, archivedCategory))

            val existingTx =
                Transaction(
                    id = 10L,
                    type = TransactionType.EXPENSE,
                    amount = Money(1000L),
                    accountId = 1L,
                    categoryId = 3L,
                    date = baseDate,
                    note = "Old note",
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            transactionRepository.setInitialData(listOf(existingTx))

            // Editing note only, keeping archived category
            val updatedTx = existingTx.copy(note = "New note")
            val result = saveTransactionUseCase(updatedTx)
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `calculates willBeNegative true when expense exceeds balance`() =
        runTest {
            accountRepository.setBalance(1L, Money(50_000L))

            val tx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(60_000L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            val result = saveTransactionUseCase(tx)
            assertThat(result.isSuccess).isTrue()
            val saveResult = result.getOrNull()!!
            assertThat(saveResult.willBeNegative).isTrue()
        }

    @Test
    fun `calculates willBeNegative false when expense does not exceed balance`() =
        runTest {
            accountRepository.setBalance(1L, Money(50_000L))

            val tx =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(30_000L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            val result = saveTransactionUseCase(tx)
            assertThat(result.isSuccess).isTrue()
            val saveResult = result.getOrNull()!!
            assertThat(saveResult.willBeNegative).isFalse()
        }

    @Test
    fun `discounts old transaction effect when calculating willBeNegative on edit`() =
        runTest {
            // Suppose current balance in repo is 40_000 (which already reflected an old 60_000 expense)
            // If we edit the expense from 60_000 to 50_000:
            // Baseline without old = 40_000 - (-60_000) = 100_000.
            // New resulting balance = 100_000 - 50_000 = 50_000 (positive!)
            accountRepository.setBalance(1L, Money(40_000L))

            val existingTx =
                Transaction(
                    id = 10L,
                    type = TransactionType.EXPENSE,
                    amount = Money(60_000L),
                    accountId = 1L,
                    categoryId = 1L,
                    date = baseDate,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                )
            transactionRepository.setInitialData(listOf(existingTx))

            val updatedTx = existingTx.copy(amount = Money(50_000L))
            val result = saveTransactionUseCase(updatedTx)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()!!.willBeNegative).isFalse()
        }
}
