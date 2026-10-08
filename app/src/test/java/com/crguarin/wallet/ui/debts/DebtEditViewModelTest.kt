package com.crguarin.wallet.ui.debts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeDebtRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.testDebt
import com.crguarin.wallet.domain.model.debt.testDebtPayment
import com.crguarin.wallet.domain.usecase.debt.DeleteDebtUseCase
import com.crguarin.wallet.domain.usecase.debt.ObserveDebtDetailUseCase
import com.crguarin.wallet.domain.usecase.debt.SaveDebtUseCase
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class DebtEditViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val baseInstant = Instant.parse("2026-09-28T12:00:00Z")
    private val today = LocalDate.of(2026, 9, 28)

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

    private fun createViewModel(savedStateHandle: SavedStateHandle): DebtEditViewModel =
        DebtEditViewModel(
            savedStateHandle = savedStateHandle,
            observeDebtDetailUseCase = ObserveDebtDetailUseCase(debtRepository),
            saveDebtUseCase = SaveDebtUseCase(debtRepository, timeProvider, FakeTransactionRunner()),
            deleteDebtUseCase = DeleteDebtUseCase(debtRepository),
            timeProvider = timeProvider,
        )

    private fun creationHandle(): SavedStateHandle = SavedStateHandle(mapOf("debtId" to null))

    private fun editHandle(debtId: Long): SavedStateHandle = SavedStateHandle(mapOf("debtId" to debtId))

    @Test
    fun `creating a valid debt saves the trimmed values and emits saved`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.setDirection(DebtDirection.I_OWE)
            viewModel.setPersonName("  Ana  ")
            viewModel.setAmountText("500")
            viewModel.setNote("  Para el arriendo  ")

            viewModel.events.test {
                viewModel.onSaveClick()
                assertThat(awaitItem()).isEqualTo(DebtEditEvent.Saved)
                cancelAndIgnoreRemainingEvents()
            }

            val saved = debtRepository.getDebt(1L)
            assertThat(saved?.personName).isEqualTo("Ana")
            assertThat(saved?.direction).isEqualTo(DebtDirection.I_OWE)
            assertThat(saved?.amount).isEqualTo(Money(50_000L))
            assertThat(saved?.date).isEqualTo(today)
            assertThat(saved?.note).isEqualTo("Para el arriendo")
        }

    @Test
    fun `an invalid amount keeps the form from being saved`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.setPersonName("Ana")

            viewModel.setAmountText("0")

            assertThat(viewModel.uiState.value.amountErrorRes).isEqualTo(R.string.error_amount_invalid)
            assertThat(viewModel.uiState.value.canSave).isFalse()

            viewModel.setAmountText("500")

            assertThat(viewModel.uiState.value.amountErrorRes).isNull()
            assertThat(viewModel.uiState.value.canSave).isTrue()
        }

    @Test
    fun `a blank name keeps the form from being saved`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.setAmountText("500")
            viewModel.setPersonName("   ")

            assertThat(viewModel.uiState.value.personNameErrorRes).isEqualTo(R.string.error_name_empty)
            assertThat(viewModel.uiState.value.canSave).isFalse()
        }

    @Test
    fun `edit mode loads the stored debt into the form`() =
        runTest(testDispatcher) {
            debtRepository.setInitialData(
                listOf(
                    testDebt(
                        id = 3L,
                        personName = "Luis",
                        direction = DebtDirection.I_OWE,
                        amount = Money(120_000L),
                        date = LocalDate.of(2026, 9, 1),
                        dueDate = LocalDate.of(2026, 10, 1),
                        note = "Mercado",
                    ),
                ),
            )

            val state = createViewModel(editHandle(3L)).uiState.value

            assertThat(state.isLoading).isFalse()
            assertThat(state.isEditMode).isTrue()
            assertThat(state.direction).isEqualTo(DebtDirection.I_OWE)
            assertThat(state.personName).isEqualTo("Luis")
            assertThat(state.amountText).isEqualTo("1200")
            assertThat(state.date).isEqualTo(LocalDate.of(2026, 9, 1))
            assertThat(state.dueDate).isEqualTo(LocalDate.of(2026, 10, 1))
            assertThat(state.note).isEqualTo("Mercado")
            assertThat(state.canSave).isTrue()
        }

    @Test
    fun `a debt that no longer exists is reported as not found`() =
        runTest(testDispatcher) {
            val state = createViewModel(editHandle(99L)).uiState.value

            assertThat(state.isLoading).isFalse()
            assertThat(state.notFound).isTrue()
            assertThat(state.canSave).isFalse()
        }

    @Test
    fun `a due date before the start date is shown on the due date field`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(creationHandle())
            viewModel.setDate(LocalDate.of(2026, 9, 20))

            viewModel.setDueDate(LocalDate.of(2026, 9, 10))

            assertThat(viewModel.uiState.value.dueDateErrorRes).isEqualTo(R.string.error_debt_due_before_date)
        }

    @Test
    fun `an amount below what was already paid is shown on the amount field`() =
        runTest(testDispatcher) {
            debtRepository.setInitialData(
                listOf(testDebt(id = 3L, amount = Money(100_000L))),
                listOf(testDebtPayment(id = 1L, debtId = 3L, amount = Money(60_000L))),
            )
            val viewModel = createViewModel(editHandle(3L))
            viewModel.setAmountText("400")

            viewModel.onSaveClick()

            assertThat(viewModel.uiState.value.amountErrorRes).isEqualTo(R.string.error_debt_amount_below_paid)
            assertThat(viewModel.uiState.value.isSaving).isFalse()
            assertThat(debtRepository.getDebt(3L)?.amount).isEqualTo(Money(100_000L))
        }

    @Test
    fun `deleting the debt emits deleted and removes it`() =
        runTest(testDispatcher) {
            debtRepository.setInitialData(listOf(testDebt(id = 3L)))
            val viewModel = createViewModel(editHandle(3L))

            viewModel.onDeleteClick()
            assertThat(viewModel.uiState.value.showDeleteDialog).isTrue()

            viewModel.events.test {
                viewModel.confirmDelete()
                assertThat(awaitItem()).isEqualTo(DebtEditEvent.Deleted)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(debtRepository.getDebt(3L)).isNull()
        }
}
