package com.crguarin.wallet.ui.accounts

import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
import com.crguarin.wallet.domain.usecase.ReorderAccountsUseCase
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var observeAccountsUseCase: ObserveAccountsUseCase
    private lateinit var observeTotalBalanceUseCase: ObserveTotalBalanceUseCase
    private lateinit var reorderAccountsUseCase: ReorderAccountsUseCase
    private lateinit var viewModel: AccountsViewModel

    private val account1 =
        Account(
            id = 1L,
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

    private val account2 =
        Account(
            id = 2L,
            name = "Ahorros",
            type = AccountType.SAVINGS,
            currencyCode = "COP",
            initialBalance = Money(300_000L),
            color = -16711936,
            icon = "savings",
            includeInTotal = true,
            isArchived = false,
            sortOrder = 1,
            createdAt = Instant.EPOCH,
        )

    private val accountArchived =
        Account(
            id = 3L,
            name = "Vieja",
            type = AccountType.CASH,
            currencyCode = "COP",
            initialBalance = Money(50_000L),
            color = -16711936,
            icon = "wallet",
            includeInTotal = true,
            isArchived = true,
            sortOrder = 2,
            createdAt = Instant.EPOCH,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        observeAccountsUseCase = ObserveAccountsUseCase(accountRepository)
        observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository)
        reorderAccountsUseCase = ReorderAccountsUseCase(accountRepository)
        viewModel =
            AccountsViewModel(
                observeAccountsUseCase = observeAccountsUseCase,
                observeTotalBalanceUseCase = observeTotalBalanceUseCase,
                reorderAccountsUseCase = reorderAccountsUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState separates active and archived accounts and calculates total balance`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(account1, account2, accountArchived),
                balanceMap = mapOf(1L to Money(100_000L), 2L to Money(300_000L), 3L to Money(50_000L)),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.totalBalance).isEqualTo(Money(400_000L))
                assertThat(state.activeAccounts).hasSize(2)
                assertThat(state.archivedAccounts).hasSize(1)
                assertThat(state.isEmpty).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleArchivedExpanded toggles expanded state`() =
        runTest(testDispatcher) {
            viewModel.uiState.test {
                val initial = awaitItem()
                assertThat(initial.isArchivedExpanded).isFalse()

                viewModel.toggleArchivedExpanded()
                assertThat(awaitItem().isArchivedExpanded).isTrue()

                viewModel.toggleArchivedExpanded()
                assertThat(awaitItem().isArchivedExpanded).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `toggleReordering toggles reordering flag`() =
        runTest(testDispatcher) {
            viewModel.uiState.test {
                val initial = awaitItem()
                assertThat(initial.isReordering).isFalse()

                viewModel.toggleReordering()
                assertThat(awaitItem().isReordering).isTrue()

                viewModel.toggleReordering()
                assertThat(awaitItem().isReordering).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `moveAccountUp and moveAccountDown reorders accounts`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(account1, account2),
            )

            viewModel.uiState.test {
                val initial = awaitItem()
                assertThat(initial.activeAccounts.map { it.account.id }).containsExactly(1L, 2L).inOrder()

                viewModel.moveAccountDown(1L)
                val afterDown = awaitItem()
                assertThat(afterDown.activeAccounts.map { it.account.id }).containsExactly(2L, 1L).inOrder()

                viewModel.moveAccountUp(1L)
                val afterUp = awaitItem()
                assertThat(afterUp.activeAccounts.map { it.account.id }).containsExactly(1L, 2L).inOrder()

                cancelAndIgnoreRemainingEvents()
            }
        }
}
