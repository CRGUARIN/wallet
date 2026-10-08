package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.debt.testDebtPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SaveDebtUseCaseTest {
    private lateinit var debtRepository: FakeDebtRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var saveDebtUseCase: SaveDebtUseCase

    private val today = LocalDate.of(2026, 10, 5)
    private val now = Instant.parse("2026-10-05T12:00:00Z")

    @Before
    fun setUp() {
        debtRepository = FakeDebtRepository()
        timeProvider = FakeTimeProvider(currentInstant = now)
        transactionRunner = FakeTransactionRunner()
        saveDebtUseCase = SaveDebtUseCase(debtRepository, timeProvider, transactionRunner)
    }

    @Test
    fun `create keeps the trimmed name and note`() =
        runTest {
            val result =
                saveDebtUseCase(testDebt(id = 0L, personName = "  Ana  ", note = "  Almuerzo  ", date = today))

            val saved = debtRepository.getDebt((result as DomainResult.Success).value)
            assertThat(saved?.personName).isEqualTo("Ana")
            assertThat(saved?.note).isEqualTo("Almuerzo")
        }

    @Test
    fun `create stamps createdAt and updatedAt from the clock`() =
        runTest {
            val result = saveDebtUseCase(testDebt(id = 0L, date = today))

            val saved = debtRepository.getDebt((result as DomainResult.Success).value)
            assertThat(saved?.createdAt).isEqualTo(now)
            assertThat(saved?.updatedAt).isEqualTo(now)
        }

    @Test
    fun `blank note is stored as null`() =
        runTest {
            val result = saveDebtUseCase(testDebt(id = 0L, date = today, note = "   "))

            assertThat(debtRepository.getDebt((result as DomainResult.Success).value)?.note).isNull()
        }

    @Test
    fun `blank name fails with NameEmpty`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, personName = "   ", date = today)).errorOrNull())
                .isEqualTo(DomainError.NameEmpty)
        }

    @Test
    fun `name of 40 characters is accepted and 41 fails with NameTooLong`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, personName = "x".repeat(40), date = today)).isSuccess).isTrue()
            assertThat(saveDebtUseCase(testDebt(id = 0L, personName = "x".repeat(41), date = today)).errorOrNull())
                .isEqualTo(DomainError.NameTooLong)
        }

    @Test
    fun `zero and negative amounts fail with AmountInvalid`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, amount = Money.ZERO, date = today)).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
            assertThat(saveDebtUseCase(testDebt(id = 0L, amount = Money(-1L), date = today)).errorOrNull())
                .isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `amount above the maximum fails and the maximum itself is accepted`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, amount = Money.MAX, date = today)).isSuccess).isTrue()
            assertThat(
                saveDebtUseCase(testDebt(id = 0L, amount = Money(Money.MAX_MINOR_UNITS + 1), date = today)).errorOrNull(),
            ).isEqualTo(DomainError.AmountInvalid)
        }

    @Test
    fun `note of 200 characters is accepted and 201 fails with NoteTooLong`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, date = today, note = "x".repeat(200))).isSuccess).isTrue()
            assertThat(saveDebtUseCase(testDebt(id = 0L, date = today, note = "x".repeat(201))).errorOrNull())
                .isEqualTo(DomainError.NoteTooLong)
        }

    @Test
    fun `date beyond one year ahead fails with DateOutOfRange`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, date = today.plusYears(1))).isSuccess).isTrue()
            assertThat(saveDebtUseCase(testDebt(id = 0L, date = today.plusYears(1).plusDays(1))).errorOrNull())
                .isEqualTo(DomainError.DateOutOfRange)
        }

    @Test
    fun `past date is accepted`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 0L, date = today.minusYears(2))).isSuccess).isTrue()
        }

    @Test
    fun `due date before the debt date fails with DebtDueBeforeDate`() =
        runTest {
            val result =
                saveDebtUseCase(testDebt(id = 0L, date = today, dueDate = today.minusDays(1)))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.DebtDueBeforeDate)
        }

    @Test
    fun `due date equal to the debt date is accepted`() =
        runTest {
            val result = saveDebtUseCase(testDebt(id = 0L, date = today, dueDate = today))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `edit keeps createdAt and stamps updatedAt`() =
        runTest {
            val createdAt = Instant.parse("2026-09-01T12:00:00Z")
            debtRepository.setInitialData(listOf(testDebt(id = 1L, createdAt = createdAt)))

            val result = saveDebtUseCase(testDebt(id = 1L, amount = Money(120_000L), createdAt = now, updatedAt = now))

            assertThat(result).isEqualTo(DomainResult.Success(1L))
            val saved = debtRepository.getDebt(1L)
            assertThat(saved?.amount).isEqualTo(Money(120_000L))
            assertThat(saved?.createdAt).isEqualTo(createdAt)
            assertThat(saved?.updatedAt).isEqualTo(now)
        }

    @Test
    fun `edit can change the direction and the due date`() =
        runTest {
            debtRepository.setInitialData(listOf(testDebt(id = 1L)))

            saveDebtUseCase(testDebt(id = 1L, direction = DebtDirection.I_OWE, dueDate = today.plusMonths(1)))

            val saved = debtRepository.getDebt(1L)
            assertThat(saved?.direction).isEqualTo(DebtDirection.I_OWE)
            assertThat(saved?.dueDate).isEqualTo(today.plusMonths(1))
        }

    @Test
    fun `edit cannot change the direction of a debt with payments`() =
        runTest {
            debtRepository.setInitialData(listOf(testDebt(id = 1L)), listOf(testDebtPayment(id = 1L, debtId = 1L)))

            val result = saveDebtUseCase(testDebt(id = 1L, direction = DebtDirection.I_OWE))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.DebtDirectionLocked)
            assertThat(debtRepository.getDebt(1L)?.direction).isEqualTo(DebtDirection.OWED_TO_ME)
        }

    @Test
    fun `edit cannot move the date after the first payment`() =
        runTest {
            val payment = testDebtPayment(id = 1L, debtId = 1L, date = LocalDate.of(2026, 9, 10))
            debtRepository.setInitialData(listOf(testDebt(id = 1L, date = LocalDate.of(2026, 9, 1))), listOf(payment))

            val after = saveDebtUseCase(testDebt(id = 1L, date = LocalDate.of(2026, 9, 11)))
            val sameDay = saveDebtUseCase(testDebt(id = 1L, date = LocalDate.of(2026, 9, 10)))

            assertThat(after.errorOrNull()).isEqualTo(DomainError.DebtDateAfterPayments)
            assertThat(sameDay.isSuccess).isTrue()
        }

    @Test
    fun `edit to an amount below what was paid fails with DebtAmountBelowPaid`() =
        runTest {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L, amount = Money(100_000L))),
                listOf(testDebtPayment(id = 1L, debtId = 1L, amount = Money(60_000L))),
            )

            val result = saveDebtUseCase(testDebt(id = 1L, amount = Money(50_000L)))

            assertThat(result.errorOrNull()).isEqualTo(DomainError.DebtAmountBelowPaid)
            assertThat(debtRepository.getDebt(1L)?.amount).isEqualTo(Money(100_000L))
        }

    @Test
    fun `edit to exactly the paid amount is accepted`() =
        runTest {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L, amount = Money(100_000L))),
                listOf(testDebtPayment(id = 1L, debtId = 1L, amount = Money(60_000L))),
            )

            val result = saveDebtUseCase(testDebt(id = 1L, amount = Money(60_000L)))

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `edit of a missing debt fails with NotFound`() =
        runTest {
            assertThat(saveDebtUseCase(testDebt(id = 99L, date = today)).errorOrNull()).isEqualTo(DomainError.NotFound)
        }

    @Test
    fun `save runs inside the transaction runner`() =
        runTest {
            saveDebtUseCase(testDebt(id = 0L, date = today))
            assertThat(transactionRunner.runs).isEqualTo(1)

            saveDebtUseCase(testDebt(id = 0L, personName = " ", date = today))
            assertThat(transactionRunner.runs).isEqualTo(2)
        }

    @Test
    fun `payments are not changed by an edit that is accepted`() =
        runTest {
            val payment = testDebtPayment(id = 1L, debtId = 1L, amount = Money(10_000L))
            debtRepository.setInitialData(listOf(testDebt(id = 1L)), listOf(payment))

            saveDebtUseCase(testDebt(id = 1L, amount = Money(90_000L)))

            assertThat(debtRepository.getPayment(1L)).isEqualTo(payment)
        }
}
