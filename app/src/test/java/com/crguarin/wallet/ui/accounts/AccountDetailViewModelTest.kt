package com.crguarin.wallet.ui.accounts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.usecase.ArchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteAccountUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveAccountUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class AccountDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var observeMonthSummaryUseCase: ObserveMonthSummaryUseCase
    private lateinit var archiveAccountUseCase: ArchiveAccountUseCase
    private lateinit var unarchiveAccountUseCase: UnarchiveAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: AccountDetailViewModel

    private val account =
        Account(
            id = 42L,
            name = "Efectivo",
            type = AccountType.CASH,
            currencyCode = "COP",
            initialBalance = Money(100_000L),
            color = -16711936,
            icon = "wallet",
            includeInTotal = true,
            isArchived = false,
            sortOrder = 0,
            createdAt = Instant.EPOCH,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        transactionRepository = FakeTransactionRepository()
        timeProvider = FakeTimeProvider(Instant.parse("2026-09-28T12:00:00Z"))
        observeMonthSummaryUseCase = ObserveMonthSummaryUseCase(transactionRepository)
        archiveAccountUseCase = ArchiveAccountUseCase(accountRepository)
        unarchiveAccountUseCase = UnarchiveAccountUseCase(accountRepository)
        deleteAccountUseCase = DeleteAccountUseCase(accountRepository)

        // Mock savedStateHandle for SavedStateHandle.toRoute<AccountDetail>()
        savedStateHandle = SavedStateHandle(mapOf("accountId" to 42L))

        accountRepository.setInitialData(
            accountList = listOf(account),
            balanceMap = mapOf(42L to Money(250_000L)),
        )

        viewModel =
            AccountDetailViewModel(
                savedStateHandle = savedStateHandle,
                accountRepository = accountRepository,
                observeMonthSummaryUseCase = observeMonthSummaryUseCase,
                observeTransactionsUseCase = ObserveTransactionsUseCase(transactionRepository),
                observeDailyTotalsUseCase = ObserveDailyTotalsUseCase(transactionRepository),
                archiveAccountUseCase = archiveAccountUseCase,
                unarchiveAccountUseCase = unarchiveAccountUseCase,
                deleteAccountUseCase = deleteAccountUseCase,
                timeProvider = timeProvider,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState observes account with balance and month summary`() =
        runTest(testDispatcher) {
            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.accountWithBalance?.account?.name).isEqualTo("Efectivo")
                assertThat(state.accountWithBalance?.balance).isEqualTo(Money(250_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deleteAccount without transactions succeeds and emits NavigateBack`() =
        runTest(testDispatcher) {
            viewModel.events.test {
                viewModel.deleteAccount()
                assertThat(awaitItem()).isEqualTo(AccountDetailEvent.NavigateBack)
                assertThat(accountRepository.getAllAccounts()).isEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deleteAccount with transactions shows offer archive dialog`() =
        runTest(testDispatcher) {
            accountRepository.setTransactionCount(42L, 5)

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.deleteAccount()
                val state = awaitItem()
                assertThat(state.showOfferArchiveDialog).isTrue()
                assertThat(state.showDeleteConfirmDialog).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `confirmArchiveInsteadOfDelete archives account and emits snackbar`() =
        runTest(testDispatcher) {
            viewModel.events.test {
                viewModel.confirmArchiveInsteadOfDelete()
                val event = awaitItem() as AccountDetailEvent.ShowSnackbar
                assertThat(event.messageRes).isEqualTo(R.string.account_detail_archived_success)

                val updated = accountRepository.getAccount(42L)
                assertThat(updated?.isArchived).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `archiveAccount and unarchiveAccount succeed and emit snackbars`() =
        runTest(testDispatcher) {
            viewModel.events.test {
                viewModel.archiveAccount()
                val archiveEvent = awaitItem() as AccountDetailEvent.ShowSnackbar
                assertThat(archiveEvent.messageRes).isEqualTo(R.string.account_detail_archived_success)
                assertThat(accountRepository.getAccount(42L)?.isArchived).isTrue()

                viewModel.unarchiveAccount()
                val unarchiveEvent = awaitItem() as AccountDetailEvent.ShowSnackbar
                assertThat(unarchiveEvent.messageRes).isEqualTo(R.string.account_detail_unarchived_success)
                assertThat(accountRepository.getAccount(42L)?.isArchived).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unarchiveAccount with duplicated active name fails and emits NameDuplicated error`() =
        runTest(testDispatcher) {
            val duplicate =
                Account(
                    id = 99L,
                    name = "Efectivo",
                    type = AccountType.CASH,
                    currencyCode = "COP",
                    initialBalance = Money.ZERO,
                    color = -16711936,
                    icon = "wallet",
                    includeInTotal = true,
                    isArchived = false,
                    sortOrder = 1,
                    createdAt = Instant.EPOCH,
                )
            accountRepository.saveAccount(account.copy(isArchived = true))
            accountRepository.saveAccount(duplicate)

            viewModel.events.test {
                viewModel.unarchiveAccount()
                val event = awaitItem() as AccountDetailEvent.ShowSnackbar
                assertThat(event.messageRes).isEqualTo(R.string.error_name_duplicated)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `canTransfer is false when there is no other active account`() =
        runTest(testDispatcher) {
            viewModel.uiState.test {
                assertThat(awaitItem().canTransfer).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `canTransfer is true when another active account exists`() =
        runTest(testDispatcher) {
            accountRepository.saveAccount(activeAccount(id = 7L, name = "Ahorros"))

            viewModel.uiState.test {
                assertThat(awaitItem().canTransfer).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `an archived second account does not enable transfers`() =
        runTest(testDispatcher) {
            accountRepository.saveAccount(activeAccount(id = 7L, name = "Ahorros").copy(isArchived = true))

            viewModel.uiState.test {
                assertThat(awaitItem().canTransfer).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `paging query filters by this account as source or destination`() =
        runTest(testDispatcher) {
            val job = backgroundScope.launch { viewModel.pagingData.collect {} }
            advanceUntilIdle()

            assertThat(transactionRepository.lastPagedFilter?.accountId).isEqualTo(42L)

            job.cancel()
        }

    private fun activeAccount(
        id: Long,
        name: String,
    ) = Account(
        id = id,
        name = name,
        type = AccountType.SAVINGS,
        currencyCode = "COP",
        initialBalance = Money.ZERO,
        color = -16711936,
        icon = "savings",
        includeInTotal = true,
        isArchived = false,
        sortOrder = 1,
        createdAt = Instant.EPOCH,
    )
}
