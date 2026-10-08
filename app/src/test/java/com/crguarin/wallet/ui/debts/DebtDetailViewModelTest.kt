package com.crguarin.wallet.ui.debts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.debt.testDebtPayment
import com.crguarin.wallet.domain.usecase.debt.AddDebtPaymentUseCase
import com.crguarin.wallet.domain.usecase.debt.DeleteDebtPaymentUseCase
import com.crguarin.wallet.domain.usecase.debt.DeleteDebtUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtDetailUseCase
import com.crguarin.wallet.domain.usecase.debt.SettleDebtUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class DebtDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")

    private lateinit var debtRepository: FakeDebtRepository
    private lateinit var timeProvider: FakeTimeProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        debtRepository = FakeDebtRepository()
        timeProvider = FakeTimeProvider(baseInstant)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(debtId: Long = 1L): DebtDetailViewModel {
        val transactionRunner = FakeTransactionRunner()
        return DebtDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("debtId" to debtId)),
            observeDebtDetailUseCase = ObserveDebtDetailUseCase(debtRepository),
            addDebtPaymentUseCase = AddDebtPaymentUseCase(debtRepository, timeProvider, transactionRunner),
            settleDebtUseCase = SettleDebtUseCase(debtRepository, timeProvider, transactionRunner),
            deleteDebtPaymentUseCase = DeleteDebtPaymentUseCase(debtRepository),
            deleteDebtUseCase = DeleteDebtUseCase(debtRepository),
            timeProvider = timeProvider,
        )
    }

    /** A debt of 1000 with 600 already paid, so 400 is outstanding. */
    private fun seedPartiallyPaid() {
        debtRepository.setInitialData(
            listOf(testDebt(id = 1L, amount = Money(100_000L))),
            listOf(testDebtPayment(id = 1L, debtId = 1L, amount = Money(60_000L))),
        )
    }

    @Test
    fun `opening the payment dialog prefills the outstanding amount`() =
        runTest(testDispatcher) {
            seedPartiallyPaid()
            val viewModel = createViewModel()

            viewModel.openPaymentDialog()

            val state = viewModel.uiState.value
            assertThat(state.showPaymentDialog).isTrue()
            assertThat(state.paymentAmountText).isEqualTo("400")
            assertThat(state.paymentDate).isEqualTo(state.today)
        }

    @Test
    fun `recording a payment reduces the outstanding amount and shows a message`() =
        runTest(testDispatcher) {
            seedPartiallyPaid()
            val viewModel = createViewModel()
            viewModel.openPaymentDialog()
            viewModel.setPaymentAmountText("150")

            viewModel.events.test {
                viewModel.confirmPayment()
                assertThat(awaitItem()).isEqualTo(DebtDetailEvent.ShowMessage(R.string.debts_payment_recorded))
                cancelAndIgnoreRemainingEvents()
            }

            val state = viewModel.uiState.value
            assertThat(state.showPaymentDialog).isFalse()
            assertThat(state.detail?.summary?.paid).isEqualTo(Money(75_000L))
            assertThat(state.detail?.payments.orEmpty()).hasSize(2)
        }

    @Test
    fun `a payment above the outstanding amount is rejected on the amount field`() =
        runTest(testDispatcher) {
            seedPartiallyPaid()
            val viewModel = createViewModel()
            viewModel.openPaymentDialog()
            viewModel.setPaymentAmountText("500")

            viewModel.confirmPayment()

            val state = viewModel.uiState.value
            assertThat(state.paymentAmountErrorRes).isEqualTo(R.string.error_debt_payment_exceeds_remaining)
            assertThat(state.showPaymentDialog).isTrue()
            assertThat(state.isProcessing).isFalse()
            assertThat(state.detail?.payments.orEmpty()).hasSize(1)
        }

    @Test
    fun `a settled debt does not open the payment dialog`() =
        runTest(testDispatcher) {
            debtRepository.setInitialData(
                listOf(testDebt(id = 1L, amount = Money(100_000L))),
                listOf(testDebtPayment(id = 1L, debtId = 1L, amount = Money(100_000L))),
            )
            val viewModel = createViewModel()

            viewModel.openPaymentDialog()

            assertThat(viewModel.uiState.value.showPaymentDialog).isFalse()
        }

    @Test
    fun `settling a debt records the outstanding amount and marks it settled`() =
        runTest(testDispatcher) {
            seedPartiallyPaid()
            val viewModel = createViewModel()
            viewModel.openSettleDialog()

            viewModel.events.test {
                viewModel.confirmSettle()
                assertThat(awaitItem()).isEqualTo(DebtDetailEvent.ShowMessage(R.string.debts_settled_message))
                cancelAndIgnoreRemainingEvents()
            }

            val state = viewModel.uiState.value
            assertThat(state.showSettleDialog).isFalse()
            assertThat(state.detail?.summary?.isSettled).isTrue()
            assertThat(state.detail?.summary?.paid).isEqualTo(Money(100_000L))
        }

    @Test
    fun `deleting a payment raises the outstanding amount again`() =
        runTest(testDispatcher) {
            seedPartiallyPaid()
            val viewModel = createViewModel()
            val payment = checkNotNull(viewModel.uiState.value.detail).payments.single()

            viewModel.openDeletePayment(payment)
            assertThat(viewModel.uiState.value.paymentToDelete).isEqualTo(payment)

            viewModel.events.test {
                viewModel.confirmDeletePayment()
                assertThat(awaitItem()).isEqualTo(DebtDetailEvent.ShowMessage(R.string.debts_payment_deleted))
                cancelAndIgnoreRemainingEvents()
            }

            val state = viewModel.uiState.value
            assertThat(state.paymentToDelete).isNull()
            assertThat(state.detail?.summary?.remaining).isEqualTo(Money(100_000L))
        }

    @Test
    fun `deleting the debt emits deleted and removes it`() =
        runTest(testDispatcher) {
            seedPartiallyPaid()
            val viewModel = createViewModel()
            viewModel.openDeleteDebt()
            assertThat(viewModel.uiState.value.showDeleteDialog).isTrue()

            viewModel.events.test {
                viewModel.confirmDeleteDebt()
                assertThat(awaitItem()).isEqualTo(DebtDetailEvent.Deleted)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(debtRepository.getDebt(1L)).isNull()
            assertThat(viewModel.uiState.value.detail).isNull()
        }
}
