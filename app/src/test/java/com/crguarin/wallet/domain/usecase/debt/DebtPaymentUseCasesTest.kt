package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.debt.testDebtPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Covers [AddDebtPaymentUseCase], [SettleDebtUseCase], [DeleteDebtPaymentUseCase] and [DeleteDebtUseCase].
 */
class DebtPaymentUseCasesTest {
    private lateinit var debtRepository: FakeDebtRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var addDebtPaymentUseCase: AddDebtPaymentUseCase
    private lateinit var settleDebtUseCase: SettleDebtUseCase
    private lateinit var deleteDebtPaymentUseCase: DeleteDebtPaymentUseCase
    private lateinit var deleteDebtUseCase: DeleteDebtUseCase

    private val today = LocalDate.of(2026, 10, 5)
    private val now = Instant.parse("2026-10-05T12:00:00Z")

    @Before
    fun setUp() {
        debtRepository = FakeDebtRepository()
        timeProvider = FakeTimeProvider(currentInstant = now)
        transactionRunner = FakeTransactionRunner()
        addDebtPaymentUseCase = AddDebtPaymentUseCase(debtRepository, timeProvider, transactionRunner)
        settleDebtUseCase = SettleDebtUseCase(debtRepository, timeProvider, transactionRunner)
        deleteDebtPaymentUseCase = DeleteDebtPaymentUseCase(debtRepository)
        deleteDebtUseCase = DeleteDebtUseCase(debtRepository)
    }

    private fun seedDebt(
        amount: Long = 100_000L,
        payments: List<Long> = emptyList(),
    ) {
        debtRepository.setInitialData(
            listOf(testDebt(id = 1L, amount = Money(amount), date = today.minusDays(10))),
            payments.mapIndexed { index, paid ->
                testDebtPayment(id = index + 1L, debtId = 1L, amount = Money(paid), date = today.minusDays(5))
            },
        )
    }

    private suspend fun paidOf(debtId: Long): Long = debtRepository.getPaid(debtId).minorUnits

    @Test
    fun `add payment to a missing debt fails with NotFound`() =
        runTest {
            val result = addDebtPaymentUseCase(99L, Money(1_000L), today, null)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `zero payment fails with AmountInvalid`() =
        runTest {
            seedDebt()

            assertThat(addDebtPaymentUseCase(1L, Money.ZERO, today, null).errorOrNull()).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `payment above the remaining amount fails with DebtPaymentExceedsRemaining`() =
        runTest {
            seedDebt(payments = listOf(60_000L))

            val result = addDebtPaymentUseCase(1L, Money(40_001L), today, null)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.DebtPaymentExceedsRemaining)
            assertThat(paidOf(1L)).isEqualTo(60_000L)
        }

    @Test
    fun `payment equal to the remaining amount settles the debt`() =
        runTest {
            seedDebt(payments = listOf(60_000L))

            val result = addDebtPaymentUseCase(1L, Money(40_000L), today, null)

            assertThat(result.isSuccess).isTrue()
            assertThat(paidOf(1L)).isEqualTo(100_000L)
            assertThat(debtRepository.observeSummary(1L).first()?.isSettled).isTrue()
        }

    @Test
    fun `payment on a settled debt fails with DebtAlreadySettled`() =
        runTest {
            seedDebt(payments = listOf(100_000L))

            val result = addDebtPaymentUseCase(1L, Money(1_000L), today, null)

            assertThat(result.errorOrNull()).isEqualTo(DomainError.DebtAlreadySettled)
            assertThat(debtRepository.observePayments(1L).first()).hasSize(1)
        }

    @Test
    fun `note longer than 200 characters fails with NoteTooLong`() =
        runTest {
            seedDebt()

            assertThat(addDebtPaymentUseCase(1L, Money(1_000L), today, "x".repeat(201)).errorOrNull())
                .isEqualTo(DomainError.NoteTooLong)
            assertThat(addDebtPaymentUseCase(1L, Money(1_000L), today, "x".repeat(200)).isSuccess).isTrue()
        }

    @Test
    fun `payment note is trimmed and a blank note is stored as null`() =
        runTest {
            seedDebt()

            val withNote = addDebtPaymentUseCase(1L, Money(1_000L), today, "  abono  ")
            val blankNote = addDebtPaymentUseCase(1L, Money(1_000L), today, "   ")

            assertThat(debtRepository.getPayment((withNote as DomainResult.Success).value)?.note).isEqualTo("abono")
            assertThat(debtRepository.getPayment((blankNote as DomainResult.Success).value)?.note).isNull()
        }

    @Test
    fun `payment date beyond one year ahead fails with DateOutOfRange`() =
        runTest {
            seedDebt()

            assertThat(addDebtPaymentUseCase(1L, Money(1_000L), today.plusYears(1).plusDays(1), null).errorOrNull())
                .isEqualTo(DomainError.DateOutOfRange)
        }

    @Test
    fun `past payment date is accepted and the payment is stamped with the clock`() =
        runTest {
            seedDebt()

            val result = addDebtPaymentUseCase(1L, Money(1_000L), today.minusMonths(2), null)

            val saved = debtRepository.getPayment((result as DomainResult.Success).value)
            assertThat(saved?.date).isEqualTo(today.minusMonths(2))
            assertThat(saved?.createdAt).isEqualTo(now)
            assertThat(saved?.debtId).isEqualTo(1L)
        }

    @Test
    fun `add payment runs inside the transaction runner`() =
        runTest {
            seedDebt()

            addDebtPaymentUseCase(1L, Money(1_000L), today, null)
            addDebtPaymentUseCase(99L, Money(1_000L), today, null)

            assertThat(transactionRunner.runs).isEqualTo(2)
        }

    @Test
    fun `settle records exactly the outstanding amount dated today`() =
        runTest {
            seedDebt(payments = listOf(30_000L))

            val result = settleDebtUseCase(1L)

            val saved = debtRepository.getPayment((result as DomainResult.Success).value)
            assertThat(saved?.amount).isEqualTo(Money(70_000L))
            assertThat(saved?.date).isEqualTo(today)
            assertThat(saved?.note).isNull()
            assertThat(saved?.createdAt).isEqualTo(now)
            assertThat(paidOf(1L)).isEqualTo(100_000L)
        }

    @Test
    fun `settle of an untouched debt records one payment for the whole amount`() =
        runTest {
            seedDebt(amount = 250_000L)

            settleDebtUseCase(1L)

            val payments = debtRepository.observePayments(1L).first()
            assertThat(payments.map { it.amount }).containsExactly(Money(250_000L))
        }

    @Test
    fun `settle of a missing debt fails with NotFound`() =
        runTest {
            assertThat(settleDebtUseCase(99L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `settle of a settled debt fails with DebtAlreadySettled and records nothing`() =
        runTest {
            seedDebt(payments = listOf(100_000L))

            assertThat(settleDebtUseCase(1L).errorOrNull()).isEqualTo(DomainError.DebtAlreadySettled)
            assertThat(debtRepository.observePayments(1L).first()).hasSize(1)
        }

    @Test
    fun `settle runs inside the transaction runner`() =
        runTest {
            seedDebt()

            settleDebtUseCase(1L)

            assertThat(transactionRunner.runs).isEqualTo(1)
        }

    @Test
    fun `delete payment raises the outstanding amount again`() =
        runTest {
            seedDebt(payments = listOf(30_000L, 20_000L))

            val result = deleteDebtPaymentUseCase(1L)

            assertThat(result).isEqualTo(DomainResult.Success(Unit))
            assertThat(debtRepository.getPayment(1L)).isNull()
            assertThat(paidOf(1L)).isEqualTo(20_000L)
        }

    @Test
    fun `delete of a missing payment fails with NotFound`() =
        runTest {
            seedDebt()

            assertThat(deleteDebtPaymentUseCase(99L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `delete debt removes the debt and its payments`() =
        runTest {
            seedDebt(payments = listOf(30_000L))

            val result = deleteDebtUseCase(1L)

            assertThat(result).isEqualTo(DomainResult.Success(Unit))
            assertThat(debtRepository.getDebt(1L)).isNull()
            assertThat(debtRepository.getPayment(1L)).isNull()
            assertThat(debtRepository.observePayments(1L).first()).isEmpty()
        }

    @Test
    fun `delete debt keeps the payments of other debts`() =
        runTest {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L), testDebt(id = 2L, personName = "Luis")),
                listOf(
                    testDebtPayment(id = 1L, debtId = 1L),
                    testDebtPayment(id = 2L, debtId = 2L, amount = Money(5_000L)),
                ),
            )

            deleteDebtUseCase(1L)

            assertThat(debtRepository.getPayment(2L)?.debtId).isEqualTo(2L)
            assertThat(debtRepository.getPaid(2L)).isEqualTo(Money(5_000L))
        }

    @Test
    fun `delete of a missing debt fails with NotFound`() =
        runTest {
            assertThat(deleteDebtUseCase(99L).errorOrNull()).isEqualTo(DomainError.NotFound)
        }
}
