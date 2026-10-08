package com.crguarin.wallet.ui.accounts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.fake.FakeAccountRepository
import com.crguarin.wallet.domain.fake.FakeCardStatementRepository
import com.crguarin.wallet.domain.fake.FakeCategoryRepository
import com.crguarin.wallet.domain.fake.FakePreferencesRepository
import com.crguarin.wallet.domain.fake.FakeRecurringRepository
import com.crguarin.wallet.domain.fake.FakeTimeProvider
import com.crguarin.wallet.domain.fake.FakeTransactionRepository
import com.crguarin.wallet.domain.fake.FakeTransactionRunner
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.domain.usecase.ArchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.DeleteAccountUseCase
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveDailyTotalsUseCase
import com.crguarin.wallet.domain.usecase.ObserveMonthSummaryUseCase
import com.crguarin.wallet.domain.usecase.ObserveTransactionsUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.UnarchiveAccountUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewUseCase
import com.crguarin.wallet.domain.usecase.transit.RegisterTripUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var cardStatementRepository: FakeCardStatementRepository
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var observeMonthSummaryUseCase: ObserveMonthSummaryUseCase
    private lateinit var archiveAccountUseCase: ArchiveAccountUseCase
    private lateinit var unarchiveAccountUseCase: UnarchiveAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var registerTripUseCase: RegisterTripUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
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
        cardStatementRepository = FakeCardStatementRepository()
        preferencesRepository = FakePreferencesRepository()
        timeProvider = FakeTimeProvider(Instant.parse("2026-09-28T12:00:00Z"))
        observeMonthSummaryUseCase = ObserveMonthSummaryUseCase(transactionRepository)
        archiveAccountUseCase = ArchiveAccountUseCase(accountRepository)
        unarchiveAccountUseCase = UnarchiveAccountUseCase(accountRepository)
        deleteAccountUseCase = DeleteAccountUseCase(accountRepository, FakeRecurringRepository())
        categoryRepository = FakeCategoryRepository()
        registerTripUseCase =
            RegisterTripUseCase(
                accountRepository,
                categoryRepository,
                SaveTransactionUseCase(
                    transactionRepository,
                    accountRepository,
                    categoryRepository,
                    timeProvider,
                    CheckNegativeBalanceUseCase(accountRepository, transactionRepository),
                    FakeTransactionRunner(),
                ),
                timeProvider,
            )
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository, cardStatementRepository, FakeTransactionRunner())

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
                registerTripUseCase = registerTripUseCase,
                deleteTransactionUseCase = deleteTransactionUseCase,
                timeProvider = timeProvider,
                observeCardOverviewUseCase =
                    ObserveCardOverviewUseCase(accountRepository, cardStatementRepository, timeProvider),
                cardStatementRepository = cardStatementRepository,
                preferencesRepository = preferencesRepository,
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

    @Test
    fun `card fields default to empty and pay source falls back to the first non-card account`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList =
                    listOf(
                        activeAccount(id = 1L, name = "Efectivo").copy(
                            type = AccountType.CASH,
                            sortOrder = 0,
                        ),
                        activeAccount(id = 2L, name = "Ahorros").copy(sortOrder = 1),
                        activeAccount(id = 7L, name = "RappiCard").copy(
                            type = AccountType.CREDIT_CARD,
                            icon = "credit_card",
                            sortOrder = 2,
                        ),
                    ),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.cardOverview).isNull()
                assertThat(state.statements).isEmpty()
                assertThat(state.adjustmentAmounts).isEmpty()
                assertThat(state.payFromAccountId).isEqualTo(1L)
                assertThat(state.payAmountMinor).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `pay source prefers the last used account unless it is a card`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList =
                    listOf(
                        activeAccount(id = 1L, name = "Efectivo").copy(type = AccountType.CASH, sortOrder = 0),
                        activeAccount(id = 2L, name = "Ahorros").copy(sortOrder = 1),
                        activeAccount(id = 7L, name = "RappiCard").copy(
                            type = AccountType.CREDIT_CARD,
                            icon = "credit_card",
                            sortOrder = 2,
                        ),
                    ),
            )
            preferencesRepository.setLastUsedAccountId(2L)

            viewModel.uiState.test {
                assertThat(awaitItem().payFromAccountId).isEqualTo(2L)

                preferencesRepository.setLastUsedAccountId(7L)
                assertThat(awaitItem().payFromAccountId).isEqualTo(1L)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `pay source is null when there is no non-card account`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList =
                    listOf(
                        activeAccount(id = 7L, name = "RappiCard").copy(
                            type = AccountType.CREDIT_CARD,
                            icon = "credit_card",
                        ),
                    ),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.payFromAccountId).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun card42() =
        account.copy(
            type = AccountType.CREDIT_CARD,
            initialBalance = Money(-250_000_00L),
            icon = "credit_card",
            creditLimit = Money(500_000_00L),
            statementDay = 15,
            paymentDueDay = 5,
        )

    private fun statement42(
        id: Long = 1L,
        adjustmentTransactionId: Long? = null,
    ) = CardStatement(
        id = id,
        accountId = 42L,
        closingDate = LocalDate.of(2026, 9, 15),
        dueDate = LocalDate.of(2026, 10, 20),
        statementBalance = Money(250_000_00L),
        minimumPayment = null,
        statedInterestAndFees = null,
        adjustmentTransactionId = adjustmentTransactionId,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `card detail exposes overview pending amount pay amount statements and adjustments`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(accountList = listOf(card42()))
            cardStatementRepository.setInitialData(
                listOf(statement42(adjustmentTransactionId = 99L)),
                listOf(
                    Transaction(
                        id = 99L,
                        type = TransactionType.EXPENSE,
                        amount = Money(5_000_00L),
                        accountId = 42L,
                        date = LocalDate.of(2026, 9, 15),
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                    ),
                ),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                val overview = state.cardOverview
                assertThat(overview).isNotNull()
                assertThat(overview?.debt).isEqualTo(Money(250_000_00L))
                assertThat(overview?.available).isEqualTo(Money(250_000_00L))
                assertThat(overview?.pendingStatementAmount).isEqualTo(Money(250_000_00L))
                assertThat(overview?.paymentStatus).isEqualTo(StatementPaymentStatus.PENDING)
                assertThat(state.payAmountMinor).isEqualTo(Money(250_000_00L).minorUnits)
                assertThat(state.statements.map { it.id }).containsExactly(1L)
                assertThat(state.adjustmentAmounts).containsExactly(1L, Money(5_000_00L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `fully paid statement reports paid status and no pay amount`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(accountList = listOf(card42()))
            cardStatementRepository.setInitialData(
                statementList = listOf(statement42()),
                transactionList =
                    listOf(
                        Transaction(
                            id = 50L,
                            type = TransactionType.TRANSFER,
                            amount = Money(250_000_00L),
                            accountId = 1L,
                            toAccountId = 42L,
                            date = LocalDate.of(2026, 9, 20),
                            createdAt = Instant.EPOCH,
                            updatedAt = Instant.EPOCH,
                        ),
                    ),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.cardOverview?.paymentStatus).isEqualTo(StatementPaymentStatus.PAID)
                assertThat(state.cardOverview?.pendingStatementAmount).isEqualTo(Money.ZERO)
                assertThat(state.payAmountMinor).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun transitCard(fare: Money? = Money(350_000L)) =
        account.copy(
            name = "Cívica",
            type = AccountType.TRANSIT_CARD,
            initialBalance = Money(1_000_000L),
            icon = "directions_bus",
            fare = fare,
        )

    @Test
    fun `transit card exposes the trips left and allows a trip`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money(1_050_000L)),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.isTransitCard).isTrue()
                assertThat(state.tripsLeft).isEqualTo(3)
                assertThat(state.canRegisterTrip).isTrue()
                assertThat(state.isCreditTrip).isFalse()
                assertThat(state.isCreditUsed).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `registering a trip creates an expense for the fare and emits TripRegistered`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money(1_050_000L)),
            )

            viewModel.events.test {
                viewModel.onRegisterTripClick()

                val event = awaitItem() as AccountDetailEvent.TripRegistered
                val trip = transactionRepository.getTransaction(event.transactionId)
                assertThat(trip?.type).isEqualTo(TransactionType.EXPENSE)
                assertThat(trip?.amount).isEqualTo(Money(350_000L))
                assertThat(trip?.accountId).isEqualTo(42L)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a zero balance registers the credit trip directly`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money.ZERO),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.canRegisterTrip).isTrue()
                assertThat(state.isCreditTrip).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.events.test {
                viewModel.onRegisterTripClick()

                val event = awaitItem() as AccountDetailEvent.TripRegistered
                assertThat(transactionRepository.getTransaction(event.transactionId)?.amount)
                    .isEqualTo(Money(350_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a balance below one fare registers the credit trip directly`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money(200_000L)),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.canRegisterTrip).isTrue()
                assertThat(state.isCreditTrip).isTrue()
                assertThat(state.isCreditUsed).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.events.test {
                viewModel.onRegisterTripClick()

                val event = awaitItem() as AccountDetailEvent.TripRegistered
                assertThat(transactionRepository.getTransaction(event.transactionId)?.amount)
                    .isEqualTo(Money(350_000L))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a double tap registers a single trip`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money.ZERO),
            )
            // Queue the launches so the second tap arrives while the first trip is still saving.
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))

            viewModel.events.test {
                viewModel.onRegisterTripClick()
                viewModel.onRegisterTripClick()
                advanceUntilIdle()

                assertThat(awaitItem()).isInstanceOf(AccountDetailEvent.TripRegistered::class.java)
                expectNoEvents()
            }
        }

    @Test
    fun `a negative balance does not allow another trip`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money(-1L)),
            )

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.canRegisterTrip).isFalse()
                assertThat(state.isCreditUsed).isTrue()
                assertThat(state.isCreditTrip).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `registering a trip without a fare reports the error`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(listOf(transitCard(fare = null)))

            viewModel.events.test {
                viewModel.onRegisterTripClick()

                val event = awaitItem() as AccountDetailEvent.ShowSnackbar
                assertThat(event.messageRes).isEqualTo(R.string.error_fare_invalid)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `undo removes the trip that was just registered`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList = listOf(transitCard()),
                balanceMap = mapOf(42L to Money(1_050_000L)),
            )

            viewModel.events.test {
                viewModel.onRegisterTripClick()
                val event = awaitItem() as AccountDetailEvent.TripRegistered
                assertThat(transactionRepository.getTransaction(event.transactionId)).isNotNull()

                viewModel.undoTrip(event.transactionId)
                assertThat(transactionRepository.getTransaction(event.transactionId)).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `recharge source is the last used cash or savings account other than the card`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList =
                    listOf(
                        transitCard(),
                        activeAccount(id = 1L, name = "Efectivo").copy(type = AccountType.CASH, sortOrder = 0),
                        activeAccount(id = 2L, name = "Ahorros").copy(sortOrder = 1),
                        activeAccount(id = 7L, name = "RappiCard").copy(
                            type = AccountType.CREDIT_CARD,
                            icon = "credit_card",
                            sortOrder = 2,
                        ),
                    ),
            )

            viewModel.uiState.test {
                assertThat(awaitItem().rechargeFromAccountId).isEqualTo(1L)

                preferencesRepository.setLastUsedAccountId(2L)
                assertThat(awaitItem().rechargeFromAccountId).isEqualTo(2L)

                preferencesRepository.setLastUsedAccountId(7L)
                assertThat(awaitItem().rechargeFromAccountId).isEqualTo(1L)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a transit card is never the source of a card payment`() =
        runTest(testDispatcher) {
            accountRepository.setInitialData(
                accountList =
                    listOf(
                        transitCard().copy(sortOrder = 0),
                        activeAccount(id = 1L, name = "Efectivo").copy(type = AccountType.CASH, sortOrder = 1),
                    ),
            )

            viewModel.uiState.test {
                assertThat(awaitItem().payFromAccountId).isEqualTo(1L)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
