package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
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

class PreviewReconciliationUseCaseTest {
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var cardRepository: FakeCardStatementRepository
    private lateinit var previewReconciliationUseCase: PreviewReconciliationUseCase

    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val closing = LocalDate.of(2026, 9, 15)

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
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
                Account(
                    id = 2L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(500_000L),
                    color = 2,
                    icon = "wallet",
                    createdAt = now,
                ),
            ),
        )
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
        previewReconciliationUseCase =
            PreviewReconciliationUseCase(
                accountRepository = accountRepository,
                cardStatementRepository = cardRepository,
                timeProvider = FakeTimeProvider(now),
            )
    }

    @Test
    fun `positive difference without stated interest`() =
        runTest {
            val preview = previewReconciliationUseCase(1L, closing, Money(520_000L), null).getOrNull()!!

            assertThat(preview.appDebt).isEqualTo(Money(500_000L))
            assertThat(preview.difference).isEqualTo(Money(20_000L))
            assertThat(preview.interestMismatch).isFalse()
        }

    @Test
    fun `interest mismatch when difference differs from stated`() =
        runTest {
            val mismatch =
                previewReconciliationUseCase(1L, closing, Money(520_000L), Money(15_000L))
                    .getOrNull()!!
            assertThat(mismatch.interestMismatch).isTrue()

            val match =
                previewReconciliationUseCase(1L, closing, Money(520_000L), Money(20_000L))
                    .getOrNull()!!
            assertThat(match.interestMismatch).isFalse()
        }

    @Test
    fun `negative difference never mismatches`() =
        runTest {
            val preview =
                previewReconciliationUseCase(1L, closing, Money(400_000L), Money(15_000L))
                    .getOrNull()!!

            assertThat(preview.difference).isEqualTo(Money(-100_000L))
            assertThat(preview.interestMismatch).isFalse()
        }

    @Test
    fun `non-card account fails with NotFound`() =
        runTest {
            assertThat(previewReconciliationUseCase(2L, closing, Money(100L), null).errorOrNull())
                .isEqualTo(DomainError.NotFound)
            assertThat(previewReconciliationUseCase(99L, closing, Money(100L), null).errorOrNull())
                .isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `negative statement balance fails with AmountInvalid`() =
        runTest {
            assertThat(previewReconciliationUseCase(1L, closing, Money(-1L), null).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `future closing fails with DateOutOfRange`() =
        runTest {
            assertThat(
                previewReconciliationUseCase(1L, LocalDate.of(2026, 9, 29), Money(100L), null)
                    .errorOrNull(),
            ).isEqualTo(DomainError.DateOutOfRange)
        }
}
