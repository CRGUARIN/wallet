package com.crguarin.wallet.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.categoryFullDisplayName
import com.crguarin.wallet.ui.motion.depthOn
import com.crguarin.wallet.ui.theme.walletMotion
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.util.formatMedium
import java.time.LocalDate

/** Above this font scale the type segments stack instead of sitting side by side. */
private const val STACKED_FONT_SCALE = 1.15f

/** "Transferencia" is the longest segment label: it gets more of the track than the other two. */
private const val LONG_SEGMENT_WEIGHT = 1.5f

private val SegmentTrackRadius = 22.dp
private val SegmentRadius = 18.dp
private val MinTouchHeight = 48.dp

/** The movement card: one raised surface with its rows. */
private val CardRadius = 24.dp
private val CardVerticalPadding = 8.dp

/** One row of the card: 64 dp minimum, a 42 dp tile, the text and a chevron. */
private val RowMinHeight = 64.dp
private val RowHorizontalPadding = 16.dp
private val RowVerticalPadding = 9.dp
private val RowGap = 14.dp
private val TileSize = 42.dp
private val TileIconSize = 20.dp

/** Test tag of the note field in the card. */
internal const val NOTE_FIELD_TAG = "transaction-note-field"

/**
 * Scrollable body of the movement form. The order follows the thumb: type, amount, the account
 * carousel (expenses and income), one card with the rows (category or the two accounts, account,
 * date and note), the frequent categories and installments. The save button is not here: it lives
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
    onNoteChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onInstallmentsChanged: (Int) -> Unit = {},
    onAccountSelected: (Long) -> Unit = {},
    onAmountFocusChanged: (Boolean) -> Unit = {},
    revealProgressively: Boolean = false,
) {
    val spacing = MaterialTheme.walletSpacing
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var suppressAmountBringIntoView by remember { mutableStateOf(!uiState.isEditMode) }
    var amountWasFocused by remember { mutableStateOf(false) }

    // Creation focuses the amount without opening the system keyboard
    // (showKeyboardOnFocus = false): the in-app keypad shows instead. Editing starts unfocused.
    LaunchedEffect(Unit) {
        if (!uiState.isEditMode) {
            focusRequester.requestFocus()
        }
    }

    // Progressive reveal (§9.3): in creation with an initially empty amount, only the top
    // (type, amount), the keypad and Save show until the amount is typed once. Then it sticks:
    // clearing the amount never hides the rest again.
    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.amountText) {
        if (uiState.amountText.isNotEmpty()) revealed = true
    }
    val showRest = !revealProgressively || revealed || uiState.isEditMode

    // The initial programmatic focus must not scroll the form: with the keypad shrinking the
    // viewport, that first bringIntoView would push the type segments off the top on small
    // screens. Later taps scroll normally: losing the focus once removes the suppression.
    // (onFocusChanged also fires with false on attach, before any focus: only a true -> false
    // transition counts as losing it.)

    // Touching a row or a card moves the focus out of the amount, hiding the keypad.
    // (The note is a text field: tapping it moves the focus on its own.)
    val openCategorySheet: () -> Unit = {
        focusManager.clearFocus()
        onOpenCategorySheet()
    }
    val openAccountSheet: () -> Unit = {
        focusManager.clearFocus()
        onOpenAccountSheet()
    }
    val openToAccountSheet: () -> Unit = {
        focusManager.clearFocus()
        onOpenToAccountSheet()
    }
    val openDatePicker: () -> Unit = {
        focusManager.clearFocus()
        onOpenDatePicker()
    }
    val selectCategory: (Long) -> Unit = {
        focusManager.clearFocus()
        onCategorySelected(it)
    }
    // Choosing another type also moves the focus out of the amount and hides the keypad, so the
    // rest of the form fits on small screens once the user moves past the top.
    val selectType: (TransactionType) -> Unit = {
        focusManager.clearFocus()
        onTypeSelected(it)
    }

    val selectedAccount =
        uiState.accounts.firstOrNull { it.account.id == uiState.accountId }?.account
            ?: uiState.archivedAccount
    val selectedAccountBalance = uiState.accounts.firstOrNull { it.account.id == uiState.accountId }?.balance
    val selectedToAccount =
        uiState.accounts.firstOrNull { it.account.id == uiState.toAccountId }?.account
            ?: uiState.archivedToAccount
    val selectedCategoryText =
        categoryFullDisplayName(
            parent = uiState.selectedParentCategory,
            category = uiState.selectedCategory,
        )
    val isTransfer = uiState.selectedType == TransactionType.TRANSFER
    val isIncome = uiState.selectedType == TransactionType.INCOME
    val carousel = !isTransfer && useCoverflow(isTransfer = isTransfer, uiState = uiState)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter, vertical = spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        TypeSegments(selected = uiState.selectedType, onSelected = selectType)

        TransactionAmountField(
            value = uiState.amountText,
            onValueChange = onAmountChanged,
            label = stringResource(amountLabelRes(uiState.selectedType)),
            focusRequester = focusRequester,
            // "Done" on the keyboard moves on to the next thing the movement lacks: its category.
            onDone = {
                if (!isTransfer && uiState.categoryId == null) onOpenCategorySheet()
            },
            errorMessage = uiState.amountErrorRes?.let { stringResource(it) },
            onFocusChanged = {
                if (amountWasFocused && !it) suppressAmountBringIntoView = false
                amountWasFocused = it
                onAmountFocusChanged(it)
            },
            suppressBringIntoView = suppressAmountBringIntoView,
        )

        if (uiState.recurringSourceName != null) {
            Revealed(visible = showRest, index = 0) {
                Text(
                    text = stringResource(R.string.recurring_generated_by, uiState.recurringSourceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (carousel) {
            Revealed(visible = showRest, index = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    AccountCoverflow(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.accountId,
                        onAccountSelected = onAccountSelected,
                        onOpenAccountSheet = openAccountSheet,
                        cardOverviews = uiState.cardOverviews,
                        // Like the other selectors, touching a card moves the focus out of the amount.
                        onInteraction = { focusManager.clearFocus() },
                    )
                    if (selectedAccount != null && selectedAccountBalance != null) {
                        Text(
                            text =
                                stringResource(
                                    if (isIncome) R.string.transaction_caption_to else R.string.transaction_caption_from,
                                    selectedAccount.name,
                                    MoneyFormatter.format(selectedAccountBalance),
                                ),
                            style =
                                MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Revealed(visible = showRest, index = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                MovementCard {
                    if (isTransfer) {
                        FormRow(
                            label = stringResource(R.string.transaction_edit_from_label),
                            value = selectedAccount?.name ?: stringResource(R.string.transaction_edit_account_placeholder),
                            icon = Icons.Default.AccountBalanceWallet,
                            tileContainer = MaterialTheme.colorScheme.primaryContainer,
                            tileContent = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = openAccountSheet,
                        )
                        SwapRow(
                            enabled = uiState.accountId != null && uiState.toAccountId != null,
                            onSwap = onSwapAccounts,
                        )
                        FormRow(
                            label = stringResource(R.string.transaction_edit_to_label),
                            value = selectedToAccount?.name ?: stringResource(R.string.transaction_edit_to_placeholder),
                            icon = Icons.Default.AccountBalanceWallet,
                            tileContainer = MaterialTheme.colorScheme.primaryContainer,
                            tileContent = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = openToAccountSheet,
                        )
                    } else {
                        val category = uiState.selectedCategory
                        FormRow(
                            label = stringResource(R.string.transaction_edit_category_label),
                            value =
                                selectedCategoryText.ifBlank {
                                    stringResource(R.string.transaction_edit_category_placeholder)
                                },
                            icon = if (category != null) IconCatalog.get(category.icon) else Icons.Default.Category,
                            tileContainer =
                                if (category != null) {
                                    Color(category.color).copy(alpha = CATEGORY_TILE_ALPHA)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            tileContent =
                                if (category != null) {
                                    Color(category.color)
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                            onClick = openCategorySheet,
                            isError = uiState.categoryErrorRes != null,
                        )
                        if (!carousel) {
                            FormRow(
                                label = stringResource(R.string.transaction_edit_account_label),
                                value = selectedAccount?.name ?: stringResource(R.string.transaction_edit_account_placeholder),
                                icon = Icons.Default.AccountBalanceWallet,
                                tileContainer = MaterialTheme.colorScheme.primaryContainer,
                                tileContent = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = openAccountSheet,
                            )
                        }
                    }

                    FormRow(
                        label = stringResource(R.string.transaction_edit_date_label),
                        value = dateText(uiState.date, uiState.today),
                        icon = Icons.Default.CalendarMonth,
                        tileContainer = MaterialTheme.colorScheme.tertiaryContainer,
                        tileContent = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = openDatePicker,
                        isError = uiState.dateErrorRes != null,
                    )

                    NoteRow(note = uiState.note, onNoteChanged = onNoteChanged)
                }

                if (!isTransfer && uiState.categoryErrorRes != null) {
                    FieldError(stringResource(uiState.categoryErrorRes))
                }
                if (uiState.dateErrorRes != null) {
                    FieldError(stringResource(uiState.dateErrorRes))
                }
                if (uiState.note.isNotEmpty() || uiState.noteErrorRes != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.transaction_edit_note_counter,
                                uiState.note.length,
                                DomainLimits.NOTE_MAX_LENGTH,
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (uiState.noteErrorRes != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.l),
                    )
                }
                if (uiState.noteErrorRes != null) {
                    FieldError(stringResource(uiState.noteErrorRes))
                }

                // Frequent categories only help while none is chosen: once one is, the row shows it.
                if (!isTransfer && uiState.categoryId == null && uiState.frequentCategories.isNotEmpty()) {
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
                                selected = false,
                                onClick = { selectCategory(frequent.category.id) },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showInstallments) {
            Revealed(visible = showRest, index = 3) {
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
        }

        Spacer(modifier = Modifier.height(spacing.s))
    }
}

/** Alpha of the category color behind its tile: the tile reads as tinted, not as a solid block. */
private const val CATEGORY_TILE_ALPHA = 0.25f

/** Label of the amount, by type. */
private fun amountLabelRes(type: TransactionType): Int =
    when (type) {
        TransactionType.INCOME -> R.string.transaction_amount_label_income
        TransactionType.EXPENSE -> R.string.transaction_amount_label_expense
        TransactionType.TRANSFER -> R.string.transaction_amount_label_transfer
    }

/** "Hoy" and "Ayer" for those days, otherwise the date in its medium form. */
@Composable
private fun dateText(
    date: LocalDate?,
    today: LocalDate,
): String =
    when (date) {
        null -> ""
        today -> stringResource(R.string.transaction_edit_today)
        today.minusDays(1) -> stringResource(R.string.transaction_edit_yesterday)
        else -> date.formatMedium()
    }

/**
 * The 3D account coverflow replaces the account row only for expenses and income with depth on and
 * more than one active account. Transfers keep their From/To rows, a single account needs no
 * carousel, and an archived movement account keeps today's row.
 */
@Composable
private fun useCoverflow(
    isTransfer: Boolean,
    uiState: TransactionEditUiState,
): Boolean = !isTransfer && depthOn() && uiState.archivedAccount == null && uiState.accounts.size > 1

/**
 * One progressively revealed block (§9.3): faded in and expanded in `medium` with a 40 ms stagger
 * per index while appearing, kept mounted once revealed.
 */
@Composable
private fun Revealed(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    val medium = MaterialTheme.walletMotion.medium
    val delay = index * MaterialTheme.walletMotion.stagger
    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(durationMillis = medium, delayMillis = delay)) +
                expandVertically(tween(durationMillis = medium, delayMillis = delay)),
        exit = fadeOut(tween(durationMillis = medium)) + shrinkVertically(tween(durationMillis = medium)),
    ) {
        content()
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

/** The rows' card: one raised surface, rows stacked without dividers. */
@Composable
private fun MovementCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(CardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = CardVerticalPadding)) {
            content()
        }
    }
}

/**
 * A tappable row of the card: a tinted tile with the icon, the value in bold above its label and a
 * chevron. Tapping opens the matching picker.
 */
@Composable
private fun FormRow(
    label: String,
    value: String,
    icon: ImageVector,
    tileContainer: Color,
    tileContent: Color,
    onClick: () -> Unit,
    isError: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = RowMinHeight)
                .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        RowTile(icon = icon, container = tileContainer, content = tileContent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = rowValueStyle(),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = rowLabelStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The note: the last row, with an inline single-line field where the value goes. */
@Composable
private fun NoteRow(
    note: String,
    onNoteChanged: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        RowTile(
            icon = Icons.Default.Notes,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Box(modifier = Modifier.weight(1f)) {
            if (note.isEmpty()) {
                Text(
                    text = stringResource(R.string.transaction_edit_note_label),
                    style = rowValueStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = note,
                onValueChange = onNoteChanged,
                singleLine = true,
                textStyle = rowValueStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().testTag(NOTE_FIELD_TAG),
            )
        }
    }
}

/** Swap between the two accounts of a transfer: a centered button between their rows. */
@Composable
private fun SwapRow(
    enabled: Boolean,
    onSwap: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(RowVerticalPadding * 2 + TileSize / 2),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onSwap, enabled = enabled) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = stringResource(R.string.transaction_edit_swap_accounts),
            )
        }
    }
}

/** The 42 dp tile of a row: the icon on a tinted rounded square (36 % radius). */
@Composable
private fun RowTile(
    icon: ImageVector,
    container: Color,
    content: Color,
) {
    Box(
        modifier =
            Modifier
                .size(TileSize)
                .background(container, RoundedCornerShape(percent = 36)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(TileIconSize),
        )
    }
}

@Composable
private fun rowValueStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )

@Composable
private fun rowLabelStyle(): TextStyle =
    MaterialTheme.typography.bodySmall.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )

@Composable
private fun FieldError(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = MaterialTheme.walletSpacing.l),
    )
}

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    }
