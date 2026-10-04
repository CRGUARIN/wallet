package com.crguarin.wallet.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyParser
import com.crguarin.wallet.core.money.formatForInput
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryNode
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import com.crguarin.wallet.domain.usecase.CheckNegativeBalanceUseCase
import com.crguarin.wallet.domain.usecase.DeleteTransactionUseCase
import com.crguarin.wallet.domain.usecase.ObserveAccountsUseCase
import com.crguarin.wallet.domain.usecase.ObserveCategoryTreeUseCase
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import com.crguarin.wallet.ui.navigation.NEW_CATEGORY_RESULT_KEY
import com.crguarin.wallet.ui.navigation.TransactionEdit
import com.crguarin.wallet.ui.util.asStringRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Transaction types offered in the creation tabs: Gasto | Ingreso | Transferencia.
 */
val TRANSACTION_CREATION_TYPES: List<TransactionType> =
    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

private const val KEY_AMOUNT = "tx_form_amount"
private const val KEY_TYPE = "tx_form_type"
private const val KEY_ACCOUNT = "tx_form_account"
private const val KEY_TO_ACCOUNT = "tx_form_to_account"
private const val KEY_CATEGORY = "tx_form_category"
private const val KEY_DATE = "tx_form_date"
private const val KEY_NOTE = "tx_form_note"
private const val KEY_INSTALLMENTS = "tx_form_installments"

data class TransactionEditUiState(
    val isLoading: Boolean = true,
    /** Editing an id that no longer exists (e.g. deleted from another screen). */
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val editingTransactionId: Long? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val amountErrorRes: Int? = null,
    val accountId: Long? = null,
    val accounts: List<AccountWithBalance> = emptyList(),
    val archivedAccount: Account? = null,
    val toAccountId: Long? = null,
    val archivedToAccount: Account? = null,
    val categoryId: Long? = null,
    val categoryNodes: List<CategoryNode> = emptyList(),
    val selectedCategory: Category? = null,
    val selectedParentCategory: Category? = null,
    val categoryErrorRes: Int? = null,
    val date: LocalDate? = null,
    val dateErrorRes: Int? = null,
    val note: String = "",
    val noteErrorRes: Int? = null,
    val generalErrorRes: Int? = null,
    val isSaving: Boolean = false,
    /** Warning that triggered the confirmation dialog; NONE when the dialog is not about balance. */
    val balanceWarning: BalanceWarning = BalanceWarning.NONE,
    /** Card debt and available credit, shown for cards in the account pickers. */
    val cardOverviews: Map<Long, CardOverview> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    /** Number of installments for a card purchase; 1 means a single payment (saved as null). */
    val installments: Int = 1,
    val showNegativeBalanceDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showToAccountSheet: Boolean = false,
    val showCategorySheet: Boolean = false,
    val showDatePicker: Boolean = false,
) {
    /** Active accounts; transfers need at least two of them. */
    val activeAccountCount: Int
        get() = accounts.size

    val showNoAccounts: Boolean
        get() =
            !isLoading &&
                !isEditMode &&
                selectedType != TransactionType.TRANSFER &&
                accounts.isEmpty() &&
                generalErrorRes == null

    /** Type of the selected origin account, looking in archived accounts too. */
    val selectedAccountType: AccountType?
        get() =
            (
                accounts.firstOrNull { it.account.id == accountId }?.account
                    ?: archivedAccount
            )?.type

    /** The installments field is only offered for card purchases. */
    val showInstallments: Boolean
        get() = selectedType == TransactionType.EXPENSE && selectedAccountType == AccountType.CREDIT_CARD

    /** A transfer needs a second active account; otherwise the transfer tab offers to create one. */
    val showTransferNeedsAccounts: Boolean
        get() =
            !isLoading &&
                !isEditMode &&
                selectedType == TransactionType.TRANSFER &&
                accounts.size < 2 &&
                generalErrorRes == null

    val isSaveEnabled: Boolean
        get() {
            val destinationValid =
                if (selectedType == TransactionType.TRANSFER) {
                    toAccountId != null && toAccountId != accountId
                } else {
                    categoryId != null
                }
            return !isLoading &&
                !isSaving &&
                !showNoAccounts &&
                !showTransferNeedsAccounts &&
                MoneyParser.parse(amountText) != null &&
                accountId != null &&
                destinationValid &&
                noteErrorRes == null &&
                date != null &&
                dateErrorRes == null
        }
}

sealed interface TransactionEditEvent {
    data object NavigateBack : TransactionEditEvent

    data class OpenCategoryEdit(
        val type: CategoryType,
    ) : TransactionEditEvent

    data object OpenAccountEdit : TransactionEditEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionEditViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val preferencesRepository: PreferencesRepository,
        private val observeAccountsUseCase: ObserveAccountsUseCase,
        private val observeCategoryTreeUseCase: ObserveCategoryTreeUseCase,
        private val saveTransactionUseCase: SaveTransactionUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val checkNegativeBalanceUseCase: CheckNegativeBalanceUseCase,
        private val timeProvider: TimeProvider,
        private val observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
    ) : ViewModel() {
        private val route: TransactionEdit = savedStateHandle.toRoute<TransactionEdit>()
        val targetTransactionId: Long? = route.transactionId
        private val initialTypeArg: TransactionType = route.initialType
        private val routeAccountId: Long? = route.accountId
        private val routeToAccountId: Long? = route.toAccountId
        private val routeInitialAmountMinor: Long? = route.initialAmountMinor

        private val selectedTypeFlow: MutableStateFlow<TransactionType>

        private val _uiState = MutableStateFlow(TransactionEditUiState())
        val uiState: StateFlow<TransactionEditUiState> = _uiState.asStateFlow()

        private val _events = Channel<TransactionEditEvent>(Channel.BUFFERED)
        val events: Flow<TransactionEditEvent> = _events.receiveAsFlow()

        init {
            val today = timeProvider.today()
            val restoredType =
                savedStateHandle.get<String>(KEY_TYPE)?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            val defaultType = restoredType ?: initialTypeArg
            selectedTypeFlow = MutableStateFlow(defaultType)
            _uiState.value =
                TransactionEditUiState(
                    today = today,
                    selectedType = defaultType,
                    amountText = savedStateHandle.get<String>(KEY_AMOUNT).orEmpty(),
                    accountId = savedStateHandle.get<Long>(KEY_ACCOUNT),
                    toAccountId = savedStateHandle.get<Long>(KEY_TO_ACCOUNT),
                    categoryId = savedStateHandle.get<Long>(KEY_CATEGORY),
                    date = savedStateHandle.get<Long>(KEY_DATE)?.let { LocalDate.ofEpochDay(it) } ?: today,
                    note = savedStateHandle.get<String>(KEY_NOTE).orEmpty(),
                    installments =
                        savedStateHandle.get<Int>(KEY_INSTALLMENTS)?.coerceIn(1, SaveTransactionUseCase.MAX_INSTALLMENTS)
                            ?: 1,
                )

            viewModelScope.launch {
                loadInitialData(today)
            }
            viewModelScope.launch {
                observeCardOverviewsUseCase().collect { overviews ->
                    _uiState.update { it.copy(cardOverviews = overviews) }
                }
            }
            viewModelScope.launch {
                observeAccountsUseCase(false).collect { accounts ->
                    _uiState.update { it.copy(accounts = accounts) }
                    clearInstallmentsWhenHidden()
                }
            }
            viewModelScope.launch {
                selectedTypeFlow
                    .flatMapLatest { type ->
                        if (type == TransactionType.TRANSFER) {
                            flowOf(emptyList())
                        } else {
                            observeCategoryTreeUseCase(type.toCategoryType())
                        }
                    }.collect { nodes ->
                        _uiState.update { it.copy(categoryNodes = nodes) }
                    }
            }
            viewModelScope.launch {
                savedStateHandle.getStateFlow<Long?>(NEW_CATEGORY_RESULT_KEY, null).collect { newId ->
                    if (newId != null) {
                        savedStateHandle.remove<Long>(NEW_CATEGORY_RESULT_KEY)
                        selectNewCategory(newId)
                    }
                }
            }
        }

        private suspend fun loadInitialData(today: LocalDate) {
            val editingId = targetTransactionId
            if (editingId != null && editingId > 0L) {
                val existing = transactionRepository.getTransaction(editingId)
                if (existing == null) {
                    _uiState.update {
                        it.copy(isLoading = false, notFound = true, generalErrorRes = DomainError.NotFound.asStringRes())
                    }
                    return
                }
                // After process death the form already holds the user's unsaved edits; only
                // the first load copies the stored transaction into it.
                val isRestoredForm = savedStateHandle.contains(KEY_TYPE)
                if (isRestoredForm) {
                    selectedTypeFlow.value = _uiState.value.selectedType
                    _uiState.update { it.copy(isEditMode = true, editingTransactionId = editingId) }
                } else {
                    selectedTypeFlow.value = existing.type
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            editingTransactionId = editingId,
                            selectedType = existing.type,
                            amountText = formatForInput(existing.amount),
                            accountId = existing.accountId,
                            toAccountId = existing.toAccountId,
                            categoryId = existing.categoryId,
                            date = existing.date,
                            note = existing.note.orEmpty(),
                            installments = existing.installments?.coerceIn(1, SaveTransactionUseCase.MAX_INSTALLMENTS) ?: 1,
                        )
                    }
                    persistForm()
                }
                resolveArchivedAccounts()
                _uiState.value.categoryId?.let { resolveSelectedCategory(it) }
                _uiState.update { it.copy(isLoading = false) }
            } else {
                applyRoutePrefill()
                val activeAccounts = accountRepository.getAllAccounts().filter { !it.isArchived }
                val currentAccountId = _uiState.value.accountId
                val defaultAccountId =
                    if (currentAccountId != null && activeAccounts.any { it.id == currentAccountId }) {
                        currentAccountId
                    } else {
                        resolveDefaultAccount(activeAccounts)
                    }
                if (defaultAccountId != null) {
                    _uiState.update { it.copy(accountId = defaultAccountId) }
                    savedStateHandle[KEY_ACCOUNT] = defaultAccountId
                }
                clearDestinationIfSameAsOrigin()
                clearInstallmentsWhenHidden()
                val currentCategoryId = _uiState.value.categoryId
                if (currentCategoryId != null) {
                    resolveSelectedCategory(currentCategoryId)
                }
                if (_uiState.value.date == null) {
                    _uiState.update { it.copy(date = today) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        private suspend fun resolveDefaultAccount(activeAccounts: List<Account>): Long? {
            if (activeAccounts.isEmpty()) return null
            val routeAccount = routeAccountId?.let { id -> activeAccounts.firstOrNull { it.id == id } }
            if (routeAccount != null) return routeAccount.id
            val lastUsedId = preferencesRepository.getUserPreferences().lastUsedAccountId
            val lastUsed = lastUsedId?.let { id -> activeAccounts.firstOrNull { it.id == id } }
            if (lastUsed != null) return lastUsed.id
            return activeAccounts.minByOrNull { it.sortOrder }?.id ?: activeAccounts.first().id
        }

        /**
         * Copies [routeToAccountId] and [routeInitialAmountMinor] into new forms.
         * Only for creation and only when there is no restored user input for that field.
         */
        private fun applyRoutePrefill() {
            if (!savedStateHandle.contains(KEY_TO_ACCOUNT) && routeToAccountId != null) {
                savedStateHandle[KEY_TO_ACCOUNT] = routeToAccountId
                _uiState.update { it.copy(toAccountId = routeToAccountId) }
            }
            if (!savedStateHandle.contains(KEY_AMOUNT) && routeInitialAmountMinor != null && routeInitialAmountMinor > 0L) {
                val prefilled = formatForInput(Money(routeInitialAmountMinor))
                savedStateHandle[KEY_AMOUNT] = prefilled
                _uiState.update { it.copy(amountText = prefilled) }
            }
        }

        /** Hides the installments field and resets it when it no longer applies. */
        private fun clearInstallmentsWhenHidden() {
            if (!_uiState.value.showInstallments && _uiState.value.installments != 1) {
                savedStateHandle[KEY_INSTALLMENTS] = 1
                _uiState.update { it.copy(installments = 1) }
            }
        }

        private suspend fun resolveArchivedAccounts() {
            val state = _uiState.value
            val account = state.accountId?.let { accountRepository.getAccount(it) }
            val toAccount = state.toAccountId?.let { accountRepository.getAccount(it) }
            _uiState.update {
                it.copy(
                    archivedAccount = account?.takeIf { account -> account.isArchived },
                    archivedToAccount = toAccount?.takeIf { account -> account.isArchived },
                )
            }
        }

        private fun clearDestinationIfSameAsOrigin() {
            val state = _uiState.value
            if (state.toAccountId != null && state.toAccountId == state.accountId) {
                savedStateHandle.remove<Long>(KEY_TO_ACCOUNT)
                _uiState.update { it.copy(toAccountId = null, archivedToAccount = null) }
            }
        }

        private suspend fun resolveSelectedCategory(categoryId: Long) {
            val category = categoryRepository.getCategory(categoryId)
            if (category == null) {
                _uiState.update { it.copy(categoryId = null, selectedCategory = null, selectedParentCategory = null) }
                return
            }
            val parent = category.parentId?.let { categoryRepository.getCategory(it) }
            _uiState.update {
                it.copy(
                    categoryId = category.id,
                    selectedCategory = category,
                    selectedParentCategory = parent,
                )
            }
        }

        private suspend fun selectNewCategory(newId: Long) {
            val category = categoryRepository.getCategory(newId) ?: return
            if (category.type != _uiState.value.selectedType.toCategoryType()) return
            val parent = category.parentId?.let { categoryRepository.getCategory(it) }
            _uiState.update {
                it.copy(
                    categoryId = category.id,
                    selectedCategory = category,
                    selectedParentCategory = parent,
                    categoryErrorRes = null,
                    generalErrorRes = null,
                )
            }
            savedStateHandle[KEY_CATEGORY] = category.id
        }

        fun setType(type: TransactionType) {
            if (type == _uiState.value.selectedType) return
            val leavingTransfer = _uiState.value.selectedType == TransactionType.TRANSFER
            selectedTypeFlow.value = type
            savedStateHandle[KEY_TYPE] = type.name
            savedStateHandle.remove<Long>(KEY_CATEGORY)
            if (leavingTransfer) {
                savedStateHandle.remove<Long>(KEY_TO_ACCOUNT)
            }
            _uiState.update {
                it.copy(
                    selectedType = type,
                    categoryId = null,
                    selectedCategory = null,
                    selectedParentCategory = null,
                    categoryErrorRes = null,
                    toAccountId = if (leavingTransfer) null else it.toAccountId,
                    archivedToAccount = if (leavingTransfer) null else it.archivedToAccount,
                    generalErrorRes = null,
                )
            }
            clearInstallmentsWhenHidden()
        }

        fun setInstallments(installments: Int) {
            val coerced = installments.coerceIn(1, SaveTransactionUseCase.MAX_INSTALLMENTS)
            savedStateHandle[KEY_INSTALLMENTS] = coerced
            _uiState.update { it.copy(installments = coerced, generalErrorRes = null) }
        }

        fun setAmountText(text: String) {
            savedStateHandle[KEY_AMOUNT] = text
            val error =
                if (text.isEmpty() || MoneyParser.parse(text) == null) {
                    DomainError.AmountInvalid.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(amountText = text, amountErrorRes = error, generalErrorRes = null) }
        }

        fun setAccountId(accountId: Long) {
            savedStateHandle[KEY_ACCOUNT] = accountId
            val clearsDestination = _uiState.value.toAccountId == accountId
            if (clearsDestination) {
                savedStateHandle.remove<Long>(KEY_TO_ACCOUNT)
            }
            _uiState.update {
                it.copy(
                    accountId = accountId,
                    archivedAccount = null,
                    toAccountId = if (clearsDestination) null else it.toAccountId,
                    archivedToAccount = if (clearsDestination) null else it.archivedToAccount,
                    showAccountSheet = false,
                    generalErrorRes = null,
                )
            }
            clearInstallmentsWhenHidden()
        }

        fun setToAccountId(toAccountId: Long) {
            if (toAccountId == _uiState.value.accountId) return
            savedStateHandle[KEY_TO_ACCOUNT] = toAccountId
            _uiState.update {
                it.copy(
                    toAccountId = toAccountId,
                    archivedToAccount = null,
                    showToAccountSheet = false,
                    generalErrorRes = null,
                )
            }
        }

        fun swapAccounts() {
            val state = _uiState.value
            val origin = state.toAccountId ?: return
            val destination = state.accountId ?: return
            savedStateHandle[KEY_ACCOUNT] = origin
            savedStateHandle[KEY_TO_ACCOUNT] = destination
            _uiState.update {
                it.copy(
                    accountId = origin,
                    toAccountId = destination,
                    // Archived accounts are not in the active list, so their names travel with them.
                    archivedAccount = it.archivedToAccount,
                    archivedToAccount = it.archivedAccount,
                    generalErrorRes = null,
                )
            }
        }

        fun setCategoryId(categoryId: Long) {
            viewModelScope.launch {
                savedStateHandle[KEY_CATEGORY] = categoryId
                resolveSelectedCategory(categoryId)
                _uiState.update { it.copy(showCategorySheet = false, categoryErrorRes = null, generalErrorRes = null) }
            }
        }

        fun setDate(date: LocalDate) {
            savedStateHandle[KEY_DATE] = date.toEpochDay()
            _uiState.update {
                it.copy(
                    date = date,
                    showDatePicker = false,
                    dateErrorRes = validateDate(date),
                    generalErrorRes = null,
                )
            }
        }

        fun setToday() = setDate(timeProvider.today())

        fun setYesterday() = setDate(timeProvider.today().minusDays(1))

        fun setNote(note: String) {
            savedStateHandle[KEY_NOTE] = note
            val error =
                if (note.length > DomainLimits.NOTE_MAX_LENGTH) {
                    DomainError.NoteTooLong.asStringRes()
                } else {
                    null
                }
            _uiState.update { it.copy(note = note, noteErrorRes = error, generalErrorRes = null) }
        }

        fun openAccountSheet() {
            _uiState.update { it.copy(showAccountSheet = true) }
        }

        fun dismissAccountSheet() {
            _uiState.update { it.copy(showAccountSheet = false) }
        }

        fun openToAccountSheet() {
            _uiState.update { it.copy(showToAccountSheet = true) }
        }

        fun dismissToAccountSheet() {
            _uiState.update { it.copy(showToAccountSheet = false) }
        }

        fun openCategorySheet() {
            _uiState.update { it.copy(showCategorySheet = true) }
        }

        fun dismissCategorySheet() {
            _uiState.update { it.copy(showCategorySheet = false) }
        }

        fun openDatePicker() {
            _uiState.update { it.copy(showDatePicker = true) }
        }

        fun dismissDatePicker() {
            _uiState.update { it.copy(showDatePicker = false) }
        }

        fun onNewCategoryClick() {
            viewModelScope.launch {
                _uiState.update { it.copy(showCategorySheet = false) }
                _events.send(TransactionEditEvent.OpenCategoryEdit(_uiState.value.selectedType.toCategoryType()))
            }
        }

        fun onNewAccountClick() {
            viewModelScope.launch {
                _events.send(TransactionEditEvent.OpenAccountEdit)
            }
        }

        fun onSaveClick() {
            if (_uiState.value.isSaving) return
            val transaction = buildTransaction(showErrors = true) ?: return
            _uiState.update { it.copy(isSaving = true, generalErrorRes = null) }
            viewModelScope.launch {
                val warning = checkNegativeBalanceUseCase(transaction)
                if (warning != BalanceWarning.NONE) {
                    _uiState.update {
                        it.copy(isSaving = false, showNegativeBalanceDialog = true, balanceWarning = warning)
                    }
                    return@launch
                }
                saveAndFinish(transaction)
            }
        }

        fun confirmNegativeBalanceSave() {
            if (_uiState.value.isSaving) return
            val transaction = buildTransaction(showErrors = false) ?: return
            _uiState.update {
                it.copy(
                    showNegativeBalanceDialog = false,
                    balanceWarning = BalanceWarning.NONE,
                    isSaving = true,
                    generalErrorRes = null,
                )
            }
            viewModelScope.launch {
                saveAndFinish(transaction)
            }
        }

        fun dismissNegativeBalanceDialog() {
            _uiState.update { it.copy(showNegativeBalanceDialog = false, balanceWarning = BalanceWarning.NONE) }
        }

        fun onDeleteClick() {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }

        fun dismissDeleteDialog() {
            _uiState.update { it.copy(showDeleteDialog = false) }
        }

        fun confirmDelete() {
            val transactionId = _uiState.value.editingTransactionId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(showDeleteDialog = false) }
                when (val result = deleteTransactionUseCase(transactionId)) {
                    is DomainResult.Success -> _events.send(TransactionEditEvent.NavigateBack)
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(generalErrorRes = result.error.asStringRes()) }
                    }
                }
            }
        }

        private suspend fun saveAndFinish(transaction: Transaction) {
            when (val result = saveTransactionUseCase(transaction)) {
                is DomainResult.Success -> {
                    preferencesRepository.setLastUsedAccountId(transaction.accountId)
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(TransactionEditEvent.NavigateBack)
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false, generalErrorRes = result.error.asStringRes()) }
                }
            }
        }

        private fun buildTransaction(showErrors: Boolean): Transaction? {
            val state = _uiState.value
            val parsedAmount = MoneyParser.parse(state.amountText)
            if (parsedAmount == null) {
                if (showErrors) {
                    _uiState.update { it.copy(amountErrorRes = DomainError.AmountInvalid.asStringRes()) }
                }
                return null
            }
            val accountId = state.accountId ?: return null

            val categoryId: Long?
            val toAccountId: Long?
            if (state.selectedType == TransactionType.TRANSFER) {
                categoryId = null
                toAccountId = state.toAccountId
                if (toAccountId == null) {
                    if (showErrors) {
                        _uiState.update { it.copy(generalErrorRes = DomainError.InvalidTransaction.asStringRes()) }
                    }
                    return null
                }
                if (toAccountId == accountId) {
                    if (showErrors) {
                        _uiState.update { it.copy(generalErrorRes = DomainError.SameAccountTransfer.asStringRes()) }
                    }
                    return null
                }
            } else {
                toAccountId = null
                categoryId = state.categoryId
                if (categoryId == null) {
                    if (showErrors) {
                        _uiState.update { it.copy(categoryErrorRes = DomainError.CategoryRequired.asStringRes()) }
                    }
                    return null
                }
            }

            if (state.note.length > DomainLimits.NOTE_MAX_LENGTH) {
                if (showErrors) {
                    _uiState.update { it.copy(noteErrorRes = DomainError.NoteTooLong.asStringRes()) }
                }
                return null
            }
            val date = state.date ?: timeProvider.today()
            val dateError = validateDate(date)
            if (dateError != null) {
                if (showErrors) {
                    _uiState.update { it.copy(dateErrorRes = dateError) }
                }
                return null
            }
            val installments =
                if (state.showInstallments) {
                    state.installments.takeIf { it > 1 }
                } else {
                    null
                }
            return Transaction(
                id = state.editingTransactionId ?: 0L,
                type = state.selectedType,
                amount = parsedAmount,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                date = date,
                note = state.note.ifBlank { null },
                installments = installments,
                createdAt = timeProvider.now(),
                updatedAt = timeProvider.now(),
            )
        }

        private fun validateDate(date: LocalDate): Int? {
            val maxAllowed = DomainLimits.maxTransactionDate(timeProvider.today())
            return if (date > maxAllowed) {
                DomainError.DateOutOfRange.asStringRes()
            } else {
                null
            }
        }

        private fun persistForm() {
            val state = _uiState.value
            savedStateHandle[KEY_AMOUNT] = state.amountText
            savedStateHandle[KEY_TYPE] = state.selectedType.name
            state.accountId?.let { savedStateHandle[KEY_ACCOUNT] = it }
            state.toAccountId?.let { savedStateHandle[KEY_TO_ACCOUNT] = it }
            state.categoryId?.let { savedStateHandle[KEY_CATEGORY] = it }
            state.date?.let { savedStateHandle[KEY_DATE] = it.toEpochDay() }
            savedStateHandle[KEY_NOTE] = state.note
            savedStateHandle[KEY_INSTALLMENTS] = state.installments
        }

        private fun TransactionType.toCategoryType(): CategoryType =
            if (this == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    }
