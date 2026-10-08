package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.debt.testDebtPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ObserveDebtsOverviewUseCaseTest {
    private lateinit var debtRepository: FakeDebtRepository
    private lateinit var observeDebtsOverviewUseCase: ObserveDebtsOverviewUseCase

    private val today = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        debtRepository = FakeDebtRepository()
        observeDebtsOverviewUseCase = ObserveDebtsOverviewUseCase(debtRepository)
    }

    @Test
    fun `empty ledger is empty and has no active debts`() =
        runTest {
            val overview = observeDebtsOverviewUseCase().first()

            assertThat(overview.isEmpty).isTrue()
            assertThat(overview.hasActive).isFalse()
            assertThat(overview.net).isEqualTo(Money.ZERO)
        }

    @Test
    fun `only settled debts is not active but is not empty`() =
        runTest {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L, amount = Money(10_000L))),
                listOf(testDebtPayment(id = 1L, debtId = 1L, amount = Money(10_000L))),
            )

            val overview = observeDebtsOverviewUseCase().first()

            assertThat(overview.hasActive).isFalse()
            assertThat(overview.isEmpty).isFalse()
            assertThat(overview.settled.map { it.debt.id }).containsExactly(1L)
        }

    @Test
    fun `splits active debts by direction and settled debts apart`() =
        runTest {
            debtRepository.setInitialData(
                listOf(
                    testDebt(id = 1L, direction = DebtDirection.OWED_TO_ME),
                    testDebt(id = 2L, direction = DebtDirection.I_OWE, personName = "Banco"),
                    testDebt(id = 3L, direction = DebtDirection.OWED_TO_ME, amount = Money(5_000L)),
                    testDebt(id = 4L, direction = DebtDirection.I_OWE, amount = Money(5_000L)),
                ),
                listOf(
                    testDebtPayment(id = 1L, debtId = 3L, amount = Money(5_000L)),
                    testDebtPayment(id = 2L, debtId = 4L, amount = Money(5_000L)),
                ),
            )

            val overview = observeDebtsOverviewUseCase().first()

            assertThat(overview.owedToMe.map { it.debt.id }).containsExactly(1L)
            assertThat(overview.iOwe.map { it.debt.id }).containsExactly(2L)
            assertThat(overview.settled.map { it.debt.id }).containsExactly(4L, 3L).inOrder()
            assertThat(overview.hasActive).isTrue()
            assertThat(overview.isEmpty).isFalse()
        }

    @Test
    fun `active debts sort by due date, then start date, then id, and no due date comes last`() =
        runTest {
            debtRepository.setInitialData(
                listOf(
                    testDebt(id = 5L, date = LocalDate.of(2026, 9, 10), dueDate = LocalDate.of(2026, 10, 20)),
                    testDebt(id = 3L, date = LocalDate.of(2026, 9, 1), dueDate = null),
                    testDebt(id = 4L, date = LocalDate.of(2026, 9, 5), dueDate = LocalDate.of(2026, 10, 20)),
                    testDebt(id = 2L, date = LocalDate.of(2026, 9, 5), dueDate = LocalDate.of(2026, 10, 20)),
                    testDebt(id = 6L, date = LocalDate.of(2026, 9, 1), dueDate = LocalDate.of(2026, 11, 1)),
                ),
            )

            val overview = observeDebtsOverviewUseCase().first()

            assertThat(overview.owedToMe.map { it.debt.id }).containsExactly(2L, 4L, 5L, 6L, 3L).inOrder()
        }

    @Test
    fun `settled debts are newest first by start date, then id descending`() =
        runTest {
            debtRepository.setInitialData(
                listOf(
                    testDebt(id = 1L, date = LocalDate.of(2026, 9, 1), amount = Money(1_000L)),
                    testDebt(id = 2L, date = LocalDate.of(2026, 9, 20), amount = Money(1_000L)),
                    testDebt(id = 3L, date = LocalDate.of(2026, 9, 20), amount = Money(1_000L)),
                ),
                listOf(
                    testDebtPayment(id = 1L, debtId = 1L, amount = Money(1_000L)),
                    testDebtPayment(id = 2L, debtId = 2L, amount = Money(1_000L)),
                    testDebtPayment(id = 3L, debtId = 3L, amount = Money(1_000L)),
                ),
            )

            val overview = observeDebtsOverviewUseCase().first()

            assertThat(overview.settled.map { it.debt.id }).containsExactly(3L, 2L, 1L).inOrder()
        }

    @Test
    fun `totals sum the outstanding amounts of active debts only and net subtracts I owe`() =
        runTest {
            debtRepository.setInitialData(
                listOf(
                    testDebt(id = 1L, direction = DebtDirection.OWED_TO_ME, amount = Money(100_000L)),
                    testDebt(id = 2L, direction = DebtDirection.OWED_TO_ME, amount = Money(50_000L)),
                    testDebt(id = 3L, direction = DebtDirection.I_OWE, amount = Money(40_000L)),
                    testDebt(id = 4L, direction = DebtDirection.OWED_TO_ME, amount = Money(9_000L)),
                ),
                listOf(
                    testDebtPayment(id = 1L, debtId = 1L, amount = Money(30_000L)),
                    testDebtPayment(id = 2L, debtId = 3L, amount = Money(10_000L)),
                    testDebtPayment(id = 3L, debtId = 4L, amount = Money(9_000L)),
                ),
            )

            val overview = observeDebtsOverviewUseCase().first()

            assertThat(overview.totalOwedToMe).isEqualTo(Money(120_000L))
            assertThat(overview.totalIOwe).isEqualTo(Money(30_000L))
            assertThat(overview.net).isEqualTo(Money(90_000L))
        }

    @Test
    fun `overdue is an unsettled debt whose due date is before today`() =
        runTest {
            debtRepository.setInitialData(
                listOf(
                    testDebt(id = 1L, dueDate = today.minusDays(1)),
                    testDebt(id = 2L, dueDate = today),
                    testDebt(id = 3L, dueDate = null),
                    testDebt(id = 4L, dueDate = today.minusDays(3), amount = Money(1_000L)),
                ),
                listOf(testDebtPayment(id = 1L, debtId = 4L, amount = Money(1_000L))),
            )

            val overview = observeDebtsOverviewUseCase().first()
            val overdueIds =
                (overview.owedToMe + overview.settled)
                    .filter { it.isOverdue(today) }
                    .map { it.debt.id }

            assertThat(overdueIds).containsExactly(1L)
        }

    @Test
    fun `overview updates when a payment is recorded`() =
        runTest {
            debtRepository.setInitialData(listOf(testDebt(id = 1L, amount = Money(10_000L))))
            val before = observeDebtsOverviewUseCase().first()

            debtRepository.insertPayment(testDebtPayment(id = 0L, debtId = 1L, amount = Money(10_000L)))
            val after = observeDebtsOverviewUseCase().first()

            assertThat(before.totalOwedToMe).isEqualTo(Money(10_000L))
            assertThat(after.totalOwedToMe).isEqualTo(Money.ZERO)
            assertThat(after.owedToMe).isEmpty()
            assertThat(after.settled).hasSize(1)
        }
}
