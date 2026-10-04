package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardSystemCategories
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ApplyReconciliationUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var cardRepository: FakeCardStatementRepository
    private lateinit var applyReconciliationUseCase: ApplyReconciliationUseCase
    private lateinit var previewReconciliationUseCase: PreviewReconciliationUseCase

    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val closing = LocalDate.of(2026, 9, 15)
    private val due = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        cardRepository = FakeCardStatementRepository()
        accountRepository.setInitialData(
            listOf(
                Account(
                    id = 1L,
                    name = "RappiCard",
                    type = AccountType.CREDIT_CARD,
                    initialBalance = Money(-200_000L),
                    color = 1,
                    icon = "credit_card",
                    createdAt = now,
                    creditLimit = Money(1_000_000L),
                    statementDay = 15,
                    paymentDueDay = 25,
                ),
            ),
        )
        val saveTransactionUseCase =
            SaveTransactionUseCase(
                transactionRepository = transactionRepository,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                timeProvider = FakeTimeProvider(now),
                checkNegativeBalanceUseCase =
                    CheckNegativeBalanceUseCase(
                        accountRepository = accountRepository,
                        transactionRepository = transactionRepository,
                    ),
            )
        applyReconciliationUseCase =
            ApplyReconciliationUseCase(
                cardStatementRepository = cardRepository,
                categoryRepository = categoryRepository,
                saveTransactionUseCase = saveTransactionUseCase,
                timeProvider = FakeTimeProvider(now),
            )
        previewReconciliationUseCase =
            PreviewReconciliationUseCase(
                accountRepository = accountRepository,
                cardStatementRepository = cardRepository,
                timeProvider = FakeTimeProvider(now),
            )
    }

    private fun preview(difference: Long) =
        ReconciliationPreview(
            accountId = 1L,
            closingDate = closing,
            appDebt = Money(500_000L),
            statementBalance = Money(500_000L + difference),
            difference = Money(difference),
            interestMismatch = false,
        )

    @Test
    fun `positive difference creates interest expense`() =
        runTest {
            val id =
                applyReconciliationUseCase(
                    preview(100_000L),
                    due,
                    null,
                    null,
                    "Extracto 15/09",
                    false,
                ).getOrNull()!!

            val statements = cardRepository.observeStatements(1L).first()
            assertThat(statements).hasSize(1)
            assertThat(statements.single().id).isEqualTo(id)
            assertThat(statements.single().statementBalance).isEqualTo(Money(600_000L))

            val adjustmentId = statements.single().adjustmentTransactionId!!
            val adjustment =
                cardRepository
                    .observeCardTransactions(1L, LocalDate.of(2020, 1, 1))
                    .first()
                    .single { it.id == adjustmentId }
            assertThat(adjustment.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(adjustment.amount).isEqualTo(Money(100_000L))
            assertThat(adjustment.date).isEqualTo(closing)
            assertThat(adjustment.note).isEqualTo("Extracto 15/09")

            val category = categoryRepository.getCategory(adjustment.categoryId!!)!!
            assertThat(category.systemKey).isEqualTo(CardSystemCategories.INTEREST_FEES_KEY)
            assertThat(category.type).isEqualTo(CategoryType.EXPENSE)
            assertThat(category.color).isEqualTo(CardSystemCategories.INTEREST_FEES_COLOR)
            assertThat(category.icon).isEqualTo(CardSystemCategories.INTEREST_FEES_ICON)

            // The adjustment is only inserted by saveReconciliation, not by validation.
            assertThat(transactionRepository.getTransaction(adjustmentId)).isNull()
        }

    @Test
    fun `negative difference without force saves statement only`() =
        runTest {
            applyReconciliationUseCase(preview(-50_000L), due, null, null, "Extracto", false)
                .getOrNull()!!

            val statements = cardRepository.observeStatements(1L).first()
            assertThat(statements).hasSize(1)
            assertThat(statements.single().adjustmentTransactionId).isNull()
            assertThat(
                cardRepository.observeCardTransactions(1L, LocalDate.of(2020, 1, 1)).first(),
            ).isEmpty()
        }

    @Test
    fun `negative difference with force creates income adjustment`() =
        runTest {
            applyReconciliationUseCase(preview(-50_000L), due, null, null, "Extracto", true)
                .getOrNull()!!

            val statements = cardRepository.observeStatements(1L).first()
            val adjustmentId = statements.single().adjustmentTransactionId!!
            val adjustment =
                cardRepository
                    .observeCardTransactions(1L, LocalDate.of(2020, 1, 1))
                    .first()
                    .single { it.id == adjustmentId }
            assertThat(adjustment.type).isEqualTo(TransactionType.INCOME)
            assertThat(adjustment.amount).isEqualTo(Money(50_000L))

            val category = categoryRepository.getCategory(adjustment.categoryId!!)!!
            assertThat(category.systemKey).isEqualTo(CardSystemCategories.ADJUSTMENT_KEY)
            assertThat(category.type).isEqualTo(CategoryType.INCOME)
            assertThat(category.color).isEqualTo(CardSystemCategories.ADJUSTMENT_COLOR)
            assertThat(category.icon).isEqualTo(CardSystemCategories.ADJUSTMENT_ICON)
        }

    @Test
    fun `zero difference creates no adjustment`() =
        runTest {
            applyReconciliationUseCase(preview(0L), due, null, null, "Extracto", false)
                .getOrNull()!!

            val statements = cardRepository.observeStatements(1L).first()
            assertThat(statements).hasSize(1)
            assertThat(statements.single().adjustmentTransactionId).isNull()
        }

    @Test
    fun `reconciling the same closing keeps id and prior link`() =
        runTest {
            cardRepository.setInitialData(
                emptyList(),
                listOf(
                    Transaction(
                        id = 1L,
                        type = TransactionType.EXPENSE,
                        amount = Money(300_000L),
                        accountId = 1L,
                        categoryId = 1L,
                        date = LocalDate.of(2026, 9, 10),
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )
            val firstPreview =
                previewReconciliationUseCase(1L, closing, Money(600_000L), null).getOrNull()!!
            assertThat(firstPreview.difference).isEqualTo(Money(100_000L))
            val firstId =
                applyReconciliationUseCase(firstPreview, due, null, null, "Extracto", false)
                    .getOrNull()!!
            val firstLink =
                cardRepository
                    .observeStatements(1L)
                    .first()
                    .single()
                    .adjustmentTransactionId!!

            // The app debt now includes the adjustment, so the difference is gone.
            val secondPreview =
                previewReconciliationUseCase(1L, closing, Money(600_000L), null).getOrNull()!!
            assertThat(secondPreview.difference).isEqualTo(Money.ZERO)
            val secondId =
                applyReconciliationUseCase(secondPreview, due, Money(20_000L), null, "Extracto", false)
                    .getOrNull()!!

            assertThat(secondId).isEqualTo(firstId)
            val statements = cardRepository.observeStatements(1L).first()
            assertThat(statements).hasSize(1)
            assertThat(statements.single().adjustmentTransactionId).isEqualTo(firstLink)
            assertThat(statements.single().minimumPayment).isEqualTo(Money(20_000L))
        }

    @Test
    fun `archived system category is reused and unarchived`() =
        runTest {
            categoryRepository.setInitialData(
                listOf(
                    Category(
                        id = 5L,
                        name = null,
                        systemKey = CardSystemCategories.INTEREST_FEES_KEY,
                        type = CategoryType.EXPENSE,
                        color = 1,
                        icon = "other",
                        isArchived = true,
                    ),
                ),
            )

            applyReconciliationUseCase(preview(100_000L), due, null, null, "Extracto", false)
                .getOrNull()!!

            val reused = categoryRepository.getCategory(5L)!!
            assertThat(reused.isArchived).isFalse()
            val statements = cardRepository.observeStatements(1L).first()
            val adjustmentId = statements.single().adjustmentTransactionId!!
            val adjustment =
                cardRepository
                    .observeCardTransactions(1L, LocalDate.of(2020, 1, 1))
                    .first()
                    .single { it.id == adjustmentId }
            assertThat(adjustment.categoryId).isEqualTo(5L)
        }

    @Test
    fun `invalid adjustment fails without saving the statement`() =
        runTest {
            val result =
                applyReconciliationUseCase(
                    preview(100_000L),
                    due,
                    null,
                    null,
                    "N".repeat(201),
                    false,
                )

            assertThat(result.errorOrNull()).isEqualTo(DomainError.NoteTooLong)
            assertThat(cardRepository.observeStatements(1L).first()).isEmpty()
        }
}
