package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.debt.testDebtPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ObserveDebtDetailUseCaseTest {
    private lateinit var debtRepository: FakeDebtRepository
    private lateinit var observeDebtDetailUseCase: ObserveDebtDetailUseCase

    @Before
    fun setUp() {
        debtRepository = FakeDebtRepository()
        observeDebtDetailUseCase = ObserveDebtDetailUseCase(debtRepository)
    }

    @Test
    fun `emits null when the debt does not exist`() =
        runTest {
            assertThat(observeDebtDetailUseCase(99L).first()).isNull()
        }

    @Test
    fun `payments come newest first, by date then id descending`() =
        runTest {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L, amount = Money(100_000L))),
                listOf(
                    testDebtPayment(id = 1L, debtId = 1L, amount = Money(1_000L), date = LocalDate.of(2026, 9, 2)),
                    testDebtPayment(id = 2L, debtId = 1L, amount = Money(2_000L), date = LocalDate.of(2026, 9, 5)),
                    testDebtPayment(id = 3L, debtId = 1L, amount = Money(3_000L), date = LocalDate.of(2026, 9, 5)),
                    testDebtPayment(id = 4L, debtId = 2L, amount = Money(4_000L), date = LocalDate.of(2026, 9, 9)),
                ),
            )

            val detail = observeDebtDetailUseCase(1L).first()

            assertThat(detail?.payments?.map { it.id }).containsExactly(3L, 2L, 1L).inOrder()
        }

    @Test
    fun `summary carries the paid total and the remaining amount`() =
        runTest {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L, amount = Money(100_000L))),
                listOf(
                    testDebtPayment(id = 1L, debtId = 1L, amount = Money(25_000L)),
                    testDebtPayment(id = 2L, debtId = 1L, amount = Money(15_000L)),
                ),
            )

            val detail = observeDebtDetailUseCase(1L).first()

            assertThat(detail?.summary?.paid).isEqualTo(Money(40_000L))
            assertThat(detail?.summary?.remaining).isEqualTo(Money(60_000L))
            assertThat(detail?.summary?.isSettled).isFalse()
        }

    @Test
    fun `detail for a debt without payments has no payments`() =
        runTest {
            debtRepository.setInitialData(listOf(testDebt(id = 1L)))

            val detail = observeDebtDetailUseCase(1L).first()

            assertThat(detail?.payments).isEmpty()
            assertThat(detail?.summary?.paid).isEqualTo(Money.ZERO)
        }
}
