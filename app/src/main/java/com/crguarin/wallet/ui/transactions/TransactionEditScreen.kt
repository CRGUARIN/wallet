package com.crguarin.wallet.ui.transactions

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.ui.components.AccountPickerSheet
import com.crguarin.wallet.ui.components.BottomActionBar
import com.crguarin.wallet.ui.components.CategoryPickerSheet
import com.crguarin.wallet.ui.components.ConfirmDialog
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.ErrorState
import com.crguarin.wallet.ui.components.LoadingState
import com.crguarin.wallet.ui.motion.keycapEdge
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.safeHiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TransactionEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCategoryEdit: (CategoryType) -> Unit = {},
    onNavigateToAccountEdit: () -> Unit = {},
    viewModel: TransactionEditViewModel? = safeHiltViewModel(),
) {
    if (viewModel == null) {
        TransactionEditScreen(
            uiState = inspectionUiState(),
            onNavigateBack = onNavigateBack,
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onNoteChanged = {},
            onInstallmentsChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Progressive reveal (§9.3) is armed once, when loading finishes: creation with an empty
    // amount starts with only type, amount, keypad and Save. Never evaluated on the loading
    // state itself, where amountText is always empty.
    var revealOnce by rememberSaveable { mutableStateOf<Boolean?>(null) }
    if (!uiState.isLoading && revealOnce == null) {
        revealOnce = !uiState.isEditMode && uiState.amountText.isEmpty()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionEditEvent.NavigateBack -> onNavigateBack()
                is TransactionEditEvent.OpenCategoryEdit -> onNavigateToCategoryEdit(event.type)
                is TransactionEditEvent.OpenAccountEdit -> onNavigateToAccountEdit()
            }
        }
    }

    TransactionEditScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTypeSelected = viewModel::setType,
        onAmountChanged = viewModel::setAmountText,
        onOpenAccountSheet = viewModel::openAccountSheet,
        onDismissAccountSheet = viewModel::dismissAccountSheet,
        onAccountSelected = viewModel::setAccountId,
        onOpenToAccountSheet = viewModel::openToAccountSheet,
        onDismissToAccountSheet = viewModel::dismissToAccountSheet,
        onToAccountSelected = viewModel::setToAccountId,
        onSwapAccounts = viewModel::swapAccounts,
        onOpenCategorySheet = viewModel::openCategorySheet,
        onDismissCategorySheet = viewModel::dismissCategorySheet,
        onCategorySelected = viewModel::setCategoryId,
        onNewCategoryClick = viewModel::onNewCategoryClick,
        onNewAccountClick = viewModel::onNewAccountClick,
        onOpenDatePicker = viewModel::openDatePicker,
        onDismissDatePicker = viewModel::dismissDatePicker,
        onDateSelected = viewModel::setDate,
        onNoteChanged = viewModel::setNote,
        onInstallmentsChanged = viewModel::setInstallments,
        onSaveClick = viewModel::onSaveClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteDialog = viewModel::dismissDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onConfirmNegativeBalance = viewModel::confirmNegativeBalanceSave,
        onDismissNegativeBalance = viewModel::dismissNegativeBalanceDialog,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        revealProgressively = revealOnce ?: false,
    )
}

/**
 * Representative state used when there is no Hilt component (previews and navigation tests),
 * so the editor layout is still visible instead of an endless loading state.
 */
private fun inspectionUiState(): TransactionEditUiState =
    TransactionEditUiState(
        isLoading = false,
        date = LocalDate.now(),
        accounts =
            listOf(
                AccountWithBalance(
                    account =
                        Account(
                            id = 1L,
                            name = "Efectivo",
                            type = AccountType.CASH,
                            initialBalance = Money.ZERO,
                            color = 0xFF42A5F5.toInt(),
                            icon = "wallet",
                            createdAt = Instant.EPOCH,
                        ),
                    balance = Money.ZERO,
                ),
                AccountWithBalance(
                    account =
                        Account(
                            id = 2L,
                            name = "Ahorros",
                            type = AccountType.SAVINGS,
                            initialBalance = Money.ZERO,
                            color = 0xFF66BB6A.toInt(),
                            icon = "savings",
                            sortOrder = 1,
                            createdAt = Instant.EPOCH,
                        ),
                    balance = Money.ZERO,
                ),
            ),
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditScreen(
    uiState: TransactionEditUiState,
    onNavigateBack: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onDismissAccountSheet: () -> Unit,
    onAccountSelected: (Long) -> Unit,
    onOpenToAccountSheet: () -> Unit,
    onDismissToAccountSheet: () -> Unit,
    onToAccountSelected: (Long) -> Unit,
    onSwapAccounts: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onDismissCategorySheet: () -> Unit,
    onCategorySelected: (Long) -> Unit,
    onNewCategoryClick: () -> Unit,
    onNewAccountClick: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmNegativeBalance: () -> Unit,
    onDismissNegativeBalance: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onInstallmentsChanged: (Int) -> Unit = {},
    revealProgressively: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // The form (and with it the save button) only exists once there is something to fill in.
    val showForm =
        !uiState.isLoading &&
            !uiState.notFound &&
            !uiState.showTransferNeedsAccounts &&
            !uiState.showNoAccounts
    // The in-app keypad shows while the amount has the focus and the system keyboard is closed.
    // In creation it also shows from the very first frame, before the initial focus lands: that
    // way focusing never shrinks the viewport, and the scrollable never scrolls the type segments
    // away to keep the field in view (ContentInViewNode only reacts to shrinks while focused).
    // Clearing it on the first focus gain changes nothing visually: focused keeps it visible.
    var amountFocused by remember { mutableStateOf(false) }
    // Armed once, when loading finishes: the first composition still carries the loading state,
    // where isEditMode is always false, so capturing it with remember would show the keypad on
    // edits opened through the route. Same pattern as revealOnce above.
    var keypadFromStart by rememberSaveable { mutableStateOf<Boolean?>(null) }
    if (!uiState.isLoading && keypadFromStart == null) {
        keypadFromStart = !uiState.isEditMode
    }
    val imeVisible = WindowInsets.isImeVisible
    val showKeypad = showForm && (amountFocused || keypadFromStart == true) && !imeVisible
    val focusManager = LocalFocusManager.current
    // Back closes the in-app keypad first, as it used to close the system keyboard; the next Back
    // leaves the form as usual.
    BackHandler(enabled = showKeypad && amountFocused) { focusManager.clearFocus() }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(titleRes(uiState)),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.01).em,
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_transaction),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showForm) {
                KeypadSaveBar(
                    showKeypad = showKeypad,
                    amountText = uiState.amountText,
                    onAmountChanged = onAmountChanged,
                    isEnabled = uiState.isSaveEnabled,
                    isSaving = uiState.isSaving,
                    errorRes = uiState.generalErrorRes,
                    saveTextRes = saveTextRes(uiState),
                    onSaveClick = onSaveClick,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.notFound -> {
                    ErrorState(
                        message = stringResource(R.string.transaction_edit_not_found),
                        actionLabel = stringResource(R.string.action_back),
                        onActionClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.showTransferNeedsAccounts -> {
                    EmptyState(
                        icon = Icons.Default.SwapHoriz,
                        title = stringResource(R.string.transaction_edit_transfer_needs_accounts_title),
                        message = stringResource(R.string.transaction_edit_transfer_needs_accounts_message),
                        actionLabel = stringResource(R.string.action_new_account),
                        onActionClick = onNewAccountClick,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.showNoAccounts -> {
                    EmptyState(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = stringResource(R.string.transaction_edit_no_accounts_title),
                        message = stringResource(R.string.transaction_edit_no_accounts_message),
                        actionLabel = stringResource(R.string.action_new_account),
                        onActionClick = onNewAccountClick,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    TransactionEditForm(
                        uiState = uiState,
                        onTypeSelected = onTypeSelected,
                        onAmountChanged = onAmountChanged,
                        onOpenAccountSheet = onOpenAccountSheet,
                        onOpenToAccountSheet = onOpenToAccountSheet,
                        onSwapAccounts = onSwapAccounts,
                        onOpenCategorySheet = onOpenCategorySheet,
                        onCategorySelected = onCategorySelected,
                        onOpenDatePicker = onOpenDatePicker,
                        onNoteChanged = onNoteChanged,
                        onInstallmentsChanged = onInstallmentsChanged,
                        onAccountSelected = onAccountSelected,
                        onAmountFocusChanged = {
                            amountFocused = it
                            if (it) keypadFromStart = false
                        },
                        revealProgressively = revealProgressively,
                    )
                }
            }
        }
    }

    if (uiState.showAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts.filter { it.account.id != uiState.toAccountId },
            selectedAccountId = uiState.accountId,
            onAccountSelected = { onAccountSelected(it.account.id) },
            cardOverviews = uiState.cardOverviews,
            onDismissRequest = onDismissAccountSheet,
        )
    }

    if (uiState.showToAccountSheet) {
        AccountPickerSheet(
            accounts = uiState.accounts.filter { it.account.id != uiState.accountId },
            selectedAccountId = uiState.toAccountId,
            onAccountSelected = { onToAccountSelected(it.account.id) },
            cardOverviews = uiState.cardOverviews,
            onDismissRequest = onDismissToAccountSheet,
        )
    }

    if (uiState.showCategorySheet) {
        CategoryPickerSheet(
            categories = uiState.categoryNodes,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { onCategorySelected(it.id) },
            onNewCategoryClick = onNewCategoryClick,
            onDismissRequest = onDismissCategorySheet,
        )
    }

    if (uiState.showDatePicker && uiState.date != null) {
        TransactionDatePickerDialog(
            initialDate = uiState.date,
            onDismiss = onDismissDatePicker,
            onDateSelected = onDateSelected,
        )
    }

    if (uiState.showNegativeBalanceDialog) {
        val isOverLimit = uiState.balanceWarning == BalanceWarning.OVER_CREDIT_LIMIT
        ConfirmDialog(
            title =
                stringResource(
                    if (isOverLimit) {
                        R.string.card_over_limit_title
                    } else {
                        R.string.transaction_edit_negative_balance_title
                    },
                ),
            message =
                stringResource(
                    if (isOverLimit) {
                        R.string.card_over_limit_message
                    } else {
                        R.string.transaction_edit_negative_balance_message
                    },
                ),
            confirmText = stringResource(R.string.action_save),
            onConfirm = onConfirmNegativeBalance,
            onDismiss = onDismissNegativeBalance,
        )
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.transaction_edit_delete_title),
            message = stringResource(R.string.transaction_edit_delete_message),
            confirmText = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteDialog,
        )
    }
}

/**
 * The bottom slot of the movement form: the in-app keypad over the save button. The keypad shows
 * while the amount has the focus and the system keyboard is closed, appearing and going with an
 * expand + fade of `medium`.
 */
@Composable
private fun KeypadSaveBar(
    showKeypad: Boolean,
    amountText: String,
    onAmountChanged: (String) -> Unit,
    isEnabled: Boolean,
    isSaving: Boolean,
    @StringRes errorRes: Int?,
    @StringRes saveTextRes: Int,
    onSaveClick: () -> Unit,
) {
    val medium = MaterialTheme.walletMotion.medium
    Column {
        AnimatedVisibility(
            visible = showKeypad,
            enter = fadeIn(tween(durationMillis = medium)) + expandVertically(tween(durationMillis = medium)),
            exit = fadeOut(tween(durationMillis = medium)) + shrinkVertically(tween(durationMillis = medium)),
        ) {
            AmountKeypad(
                current = amountText,
                onAmountChanged = onAmountChanged,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.walletSpacing.screenGutter),
            )
        }
        SaveBar(
            isEnabled = isEnabled,
            isSaving = isSaving,
            errorRes = errorRes,
            saveTextRes = saveTextRes,
            onSaveClick = onSaveClick,
        )
    }
}

/**
 * Save button pinned to the bottom, above the keyboard and the navigation bar. A general error
 * (for example a rejected save) shows right above it, where it cannot be missed.
 */
@Composable
private fun SaveBar(
    isEnabled: Boolean,
    isSaving: Boolean,
    @StringRes errorRes: Int?,
    @StringRes saveTextRes: Int,
    onSaveClick: () -> Unit,
) {
    BottomActionBar(modifier = Modifier.navigationBarsPadding()) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.xs),
        ) {
            if (errorRes != null) {
                Text(
                    text = stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SaveKey(
                text = stringResource(saveTextRes),
                enabled = isEnabled && !isSaving,
                isLoading = isSaving,
                onClick = onSaveClick,
            )
        }
    }
}

private val SaveKeyHeight = 56.dp
private val SaveKeyDepth = 5.dp
private val SaveKeyShape = RoundedCornerShape(20.dp)

/** The mock's disabled save key: the whole key at 40 %, not grey. */
private const val DISABLED_SAVE_ALPHA = 0.4f

/**
 * The save button as a relieved key: 56 dp face in `primary` over a darker ledge of [SaveKeyDepth].
 * On press it sinks onto its ledge in `pressMillis`, like the keypad keys.
 */
@Composable
private fun SaveKey(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = MaterialTheme.walletMotion.pressMillis),
        label = "saveSink",
    )
    val depthPx = with(LocalDensity.current) { SaveKeyDepth.toPx() }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SaveKeyHeight + SaveKeyDepth)
                .alpha(if (enabled) 1f else DISABLED_SAVE_ALPHA),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(top = SaveKeyDepth)
                    .background(keycapEdge(colors.primary), SaveKeyShape),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SaveKeyHeight)
                    .graphicsLayer { translationY = sink * depthPx }
                    .background(colors.primary, SaveKeyShape)
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = text,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.onPrimary,
                )
            }
        }
    }
}

/** Title of the form: by type when it creates a movement, today's edit title when it edits one. */
private fun titleRes(uiState: TransactionEditUiState): Int =
    when {
        uiState.isEditMode -> R.string.transaction_edit_title
        uiState.selectedType == TransactionType.INCOME -> R.string.transaction_new_income_title
        uiState.selectedType == TransactionType.TRANSFER -> R.string.transaction_new_transfer_title
        else -> R.string.transaction_new_expense_title
    }

/** Save text by type; editing keeps the plain "Save". */
private fun saveTextRes(uiState: TransactionEditUiState): Int =
    when {
        uiState.isEditMode -> R.string.action_save
        uiState.selectedType == TransactionType.INCOME -> R.string.transaction_save_income
        uiState.selectedType == TransactionType.TRANSFER -> R.string.transaction_save_transfer
        else -> R.string.transaction_save_expense
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(name = "New expense Light", showBackground = true)
@Composable
private fun TransactionEditScreenPreviewLight() {
    WalletTheme(darkTheme = false) {
        TransactionEditScreen(
            uiState =
                inspectionUiState().copy(
                    amountText = "125000",
                    frequentCategories =
                        listOf(
                            FrequentCategory(previewCategory(10L, "Alimentación")),
                            FrequentCategory(previewCategory(11L, "Transporte")),
                            FrequentCategory(previewCategory(12L, "Café"), parent = previewCategory(10L, "Alimentación")),
                        ),
                    categoryId = 11L,
                    selectedCategory = previewCategory(11L, "Transporte"),
                ),
            onNavigateBack = {},
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onNoteChanged = {},
            onInstallmentsChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
        )
    }
}

@Preview(name = "New transfer Dark", showBackground = true)
@Composable
private fun TransactionEditScreenPreviewDark() {
    WalletTheme(darkTheme = true) {
        TransactionEditScreen(
            uiState =
                inspectionUiState().copy(
                    selectedType = TransactionType.TRANSFER,
                    amountText = "125000",
                    accountId = 1L,
                    toAccountId = 2L,
                ),
            onNavigateBack = {},
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onNoteChanged = {},
            onInstallmentsChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
        )
    }
}

private fun previewCategory(
    id: Long,
    name: String,
): Category = Category(id = id, name = name, type = CategoryType.EXPENSE, color = 0xFF66BB6A.toInt(), icon = "restaurant")

@Preview(name = "New expense progressive", showBackground = true)
@Composable
private fun TransactionEditScreenPreviewProgressive() {
    WalletTheme(darkTheme = false) {
        TransactionEditScreen(
            uiState = inspectionUiState(),
            onNavigateBack = {},
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onNoteChanged = {},
            onInstallmentsChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
            revealProgressively = true,
        )
    }
}

@Preview(name = "New expense Large font", showBackground = true, fontScale = 2f)
@Composable
private fun TransactionEditScreenPreviewLargeFont() {
    WalletTheme(darkTheme = false) {
        TransactionEditScreen(
            uiState = inspectionUiState().copy(amountText = "28000"),
            onNavigateBack = {},
            onTypeSelected = {},
            onAmountChanged = {},
            onOpenAccountSheet = {},
            onDismissAccountSheet = {},
            onAccountSelected = {},
            onOpenToAccountSheet = {},
            onDismissToAccountSheet = {},
            onToAccountSelected = {},
            onSwapAccounts = {},
            onOpenCategorySheet = {},
            onDismissCategorySheet = {},
            onCategorySelected = {},
            onNewCategoryClick = {},
            onNewAccountClick = {},
            onOpenDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onNoteChanged = {},
            onInstallmentsChanged = {},
            onSaveClick = {},
            onDeleteClick = {},
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            onConfirmNegativeBalance = {},
            onDismissNegativeBalance = {},
        )
    }
}
