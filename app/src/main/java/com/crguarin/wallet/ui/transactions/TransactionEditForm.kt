package com.crguarin.wallet.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.ui.components.categoryFullDisplayName
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.formatMedium
import java.time.LocalDate

/** Above this font scale the type segments and the date row stack instead of sitting side by side. */
private const val STACKED_FONT_SCALE = 1.15f

/** "Transferencia" is the longest segment label: it gets more of the track than the other two. */
private const val LONG_SEGMENT_WEIGHT = 1.5f

private val SegmentTrackRadius = 22.dp
private val SegmentRadius = 18.dp
private val MinTouchHeight = 48.dp

/**
 * Scrollable body of the movement form. The order follows the thumb: type, amount, category (with
 * the frequent ones), account, date, installments and note. The save button is not here: it lives
 * in the screen's bottom bar so it stays above the keyboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionEditForm(
    uiState: TransactionEditUiState,
    onTypeSelected: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onOpenAccountSheet: () -> Unit,
    onOpenToAccountSheet: () -> Unit,
    onSwapAccounts: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onCategorySelected: (Long) -> Unit,
    onOpenDatePicker: () -> Unit,
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit,
    onNoteChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onInstallmentsChanged: (Int) -> Unit = {},
) {
    val spacing = MaterialTheme.walletSpacing
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (!uiState.isEditMode) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val selectedAccount =
        uiState.accounts.firstOrNull { it.account.id == uiState.accountId }?.account
            ?: uiState.archivedAccount
    val selectedToAccount =
        uiState.accounts.firstOrNull { it.account.id == uiState.toAccountId }?.account
            ?: uiState.archivedToAccount
    val selectedCategoryText =
        categoryFullDisplayName(
            parent = uiState.selectedParentCategory,
            category = uiState.selectedCategory,
        )
    val isTransfer = uiState.selectedType == TransactionType.TRANSFER

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter, vertical = spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        TypeSegments(selected = uiState.selectedType, onSelected = onTypeSelected)

        TransactionAmountField(
            value = uiState.amountText,
            onValueChange = onAmountChanged,
            focusRequester = focusRequester,
            // "Done" on the keyboard moves on to the next thing the movement lacks: its category.
            onDone = {
                if (!isTransfer && uiState.categoryId == null) onOpenCategorySheet()
            },
            errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
        )

        if (uiState.recurringSourceName != null) {
            Text(
                text = stringResource(R.string.recurring_generated_by, uiState.recurringSourceName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isTransfer) {
            SelectorField(
                label = stringResource(R.string.transaction_edit_from_label),
                value = selectedAccount?.name ?: stringResource(R.string.transaction_edit_account_placeholder),
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onOpenAccountSheet,
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onSwapAccounts,
                    enabled = uiState.accountId != null && uiState.toAccountId != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.transaction_edit_swap_accounts),
                    )
                }
            }

            SelectorField(
                label = stringResource(R.string.transaction_edit_to_label),
                value = selectedToAccount?.name ?: stringResource(R.string.transaction_edit_to_placeholder),
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onOpenToAccountSheet,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                SelectorField(
                    label = stringResource(R.string.transaction_edit_category_label),
                    value =
                        selectedCategoryText.ifBlank {
                            stringResource(R.string.transaction_edit_category_placeholder)
                        },
                    icon = Icons.Default.Category,
                    isError = uiState.categoryErrorRes != null,
                    onClick = onOpenCategorySheet,
                )
                if (uiState.categoryErrorRes != null) {
                    Text(
                        text = stringResource(uiState.categoryErrorRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = spacing.l),
                    )
                }
                if (uiState.frequentCategories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.transaction_edit_frequent_categories),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = spacing.xs, top = spacing.xs),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        uiState.frequentCategories.forEach { frequent ->
                            TonalChip(
                                label = categoryFullDisplayName(parent = frequent.parent, category = frequent.category),
                                selected = uiState.categoryId == frequent.category.id,
                                onClick = { onCategorySelected(frequent.category.id) },
                            )
                        }
                    }
                }
            }

            SelectorField(
                label = stringResource(R.string.transaction_edit_account_label),
                value = selectedAccount?.name ?: stringResource(R.string.transaction_edit_account_placeholder),
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onOpenAccountSheet,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            DateRow(
                date = uiState.date,
                today = uiState.today,
                isError = uiState.dateErrorRes != null,
                onTodayClick = onTodayClick,
                onYesterdayClick = onYesterdayClick,
                onOpenDatePicker = onOpenDatePicker,
            )
            if (uiState.dateErrorRes != null) {
                Text(
                    text = stringResource(uiState.dateErrorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = spacing.l),
                )
            }
        }

        if (uiState.showInstallments) {
            // Local text so the field can be emptied while typing; an empty field means 1.
            var installmentsText by rememberSaveable { mutableStateOf(uiState.installments.toString()) }
            TextField(
                value = installmentsText,
                onValueChange = { text ->
                    val digits = text.filter(Char::isDigit)
                    val parsed = digits.toIntOrNull()
                    if (digits.isEmpty() || (parsed != null && parsed in 1..SaveTransactionUseCase.MAX_INSTALLMENTS)) {
                        installmentsText = parsed?.toString().orEmpty()
                        onInstallmentsChanged(parsed ?: 1)
                    }
                },
                label = { Text(text = stringResource(R.string.card_installments_label)) },
                supportingText = { Text(text = stringResource(R.string.card_installments_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = tonalFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        TextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            label = { Text(text = stringResource(R.string.transaction_edit_note_label)) },
            isError = uiState.noteErrorRes != null,
            minLines = 2,
            maxLines = 4,
            shape = MaterialTheme.shapes.medium,
            colors = tonalFieldColors(),
            supportingText = {
                Text(
                    text =
                        stringResource(
                            R.string.transaction_edit_note_counter,
                            uiState.note.length,
                            DomainLimits.NOTE_MAX_LENGTH,
                        ),
                    color =
                        if (uiState.noteErrorRes != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.s))
    }
}

/** Text field on a tonal layer: no indicator line and no border, the label and counter stay. */
@Composable
private fun tonalFieldColors(): TextFieldColors {
    val clear = Color.Transparent
    return TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedIndicatorColor = clear,
        unfocusedIndicatorColor = clear,
        errorIndicatorColor = clear,
        disabledIndicatorColor = clear,
    )
}

/**
 * Expense | Income | Transfer in a rounded track with no borders and no check icons, so
 * "Transferencia" fits on one line at 360 dp. The chosen one rests on `secondaryContainer`. With
 * very large fonts the three options stack.
 */
@Composable
private fun TypeSegments(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE
    val track =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SegmentTrackRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(spacing.xs)
            .selectableGroup()
    if (stacked) {
        Column(modifier = track, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            TRANSACTION_CREATION_TYPES.forEach { type ->
                TypeSegment(type = type, selected = type == selected, onClick = { onSelected(type) })
            }
        }
    } else {
        Row(modifier = track, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            TRANSACTION_CREATION_TYPES.forEach { type ->
                TypeSegment(
                    type = type,
                    selected = type == selected,
                    onClick = { onSelected(type) },
                    modifier = Modifier.weight(if (type == TransactionType.TRANSFER) LONG_SEGMENT_WEIGHT else 1f),
                )
            }
        }
    }
}

@Composable
private fun TypeSegment(
    type: TransactionType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val fade = tween<Color>(durationMillis = MaterialTheme.walletMotion.fast)
    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
        animationSpec = fade,
        label = "segmentContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        animationSpec = fade,
        label = "segmentContent",
    )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchHeight)
                .clip(RoundedCornerShape(SegmentRadius))
                .background(container)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(horizontal = MaterialTheme.walletSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(type.labelRes()),
            style = MaterialTheme.typography.titleSmall,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Date in one row: "Hoy" and "Ayer" chips (the one that matches the date is selected) and the date
 * field that opens the calendar. With very large fonts the field goes under the chips.
 */
@Composable
private fun DateRow(
    date: LocalDate?,
    today: LocalDate,
    isError: Boolean,
    onTodayClick: () -> Unit,
    onYesterdayClick: () -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE

    @Composable
    fun Chips() {
        TonalChip(
            label = stringResource(R.string.transaction_edit_today),
            selected = date == today,
            onClick = onTodayClick,
        )
        TonalChip(
            label = stringResource(R.string.transaction_edit_yesterday),
            selected = date == today.minusDays(1),
            onClick = onYesterdayClick,
        )
    }

    @Composable
    fun DateField(modifier: Modifier = Modifier) {
        SelectorField(
            label = stringResource(R.string.transaction_edit_date_label),
            value = date?.formatMedium().orEmpty(),
            icon = Icons.Default.CalendarMonth,
            isError = isError,
            onClick = onOpenDatePicker,
            modifier = modifier,
        )
    }

    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) { Chips() }
            DateField()
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chips()
            DateField(modifier = Modifier.weight(1f))
        }
    }
}

/** Tappable field that opens a list or a calendar: a tonal layer with the icon in `primary`. */
@Composable
internal fun SelectorField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val spacing = MaterialTheme.walletSpacing
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchHeight)
                    .padding(horizontal = spacing.l, vertical = spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }
