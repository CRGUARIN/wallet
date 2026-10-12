package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.ui.components.AmountVisualTransformation
import com.crguarin.wallet.ui.components.HERO_SYMBOL_RAISE
import com.crguarin.wallet.ui.components.amountWithSmallSymbol
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletTypography
import java.util.Locale

/** Size of the "$" in the form amount, relative to the digits. */
private const val FORM_SYMBOL_SCALE = 0.55f

private const val SYMBOL_PREFIX = "$ "

/** The figure of the amount: 48 sp, centered. */
private val AmountFigureSize = 48.sp
private val AmountFigureLineHeight = 56.sp

/**
 * The amount of the movement form, the main figure of the screen: a centered [label] above Manrope
 * 800 at 48 sp with tabular figures, a small dimmed "$" in front and the digits grouped as they are
 * typed. No box or background. The keyboard action is "Done": it hides the keyboard and calls
 * [onDone] (the form uses it to open the category list when one is missing).
 *
 * Focusing never opens the system keyboard (`showKeyboardOnFocus = false`): while focused the
 * form shows its own [AmountKeypad] instead. Tapping the already-focused field opens the system
 * keyboard for cents and pasting, and a long press still shows the native paste bar, because the
 * field stays a plain editable text field.
 *
 * The field is a [TextFieldState] so that text set from outside (the keypad) puts the caret at
 * its end instead of keeping the old index. [value] stays the form's copy of the text.
 */
@Composable
internal fun TransactionAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    locale: Locale = LocalConfiguration.current.locales[0],
    onFocusChanged: (Boolean) -> Unit = {},
    suppressBringIntoView: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val amountStyle =
        MaterialTheme.walletTypography.formAmount.copy(
            fontSize = AmountFigureSize,
            lineHeight = AmountFigureLineHeight,
            textAlign = TextAlign.Center,
        )
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val symbolColor = colors.onSurfaceVariant
    val state = remember { TextFieldState(value, TextRange(value.length)) }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    // Texts this field sent to the form that the form has not echoed back yet. The echo arrives a
    // frame later, so without this a fast typist's newer text could be overwritten by an older one.
    val pendingEchoes = remember { ArrayDeque<String>() }
    // Form -> field (the keypad): a text the field did not send replaces it, caret at the end.
    LaunchedEffect(value) {
        val echo = pendingEchoes.indexOf(value)
        if (echo >= 0) {
            repeat(echo + 1) { pendingEchoes.removeFirst() }
        } else {
            pendingEchoes.clear()
            if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    // Field -> form: the input transformation has already normalized what the field holds.
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (text != currentValue) {
                pendingEchoes.addLast(text)
                currentOnValueChange(text)
            }
        }
    }
    val inputTransformation = remember(locale) { amountInputTransformation(locale) }
    val outputTransformation = remember(locale, symbolColor) { amountOutputTransformation(locale, symbolColor) }
    val placeholder =
        remember(symbolColor) {
            amountWithSmallSymbol(SYMBOL_PREFIX + "0", FORM_SYMBOL_SCALE, symbolColor = symbolColor)
        }
    val isError = errorMessage != null
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (suppressBringIntoView) Modifier.suppressBringIntoView() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
            color = if (isError) colors.error else colors.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                state = state,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            focused = it.isFocused
                            onFocusChanged(it.isFocused)
                        },
                inputTransformation = inputTransformation,
                outputTransformation = outputTransformation,
                textStyle = amountStyle.copy(color = if (isError) colors.error else colors.onSurface),
                lineLimits = TextFieldLineLimits.SingleLine,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        showKeyboardOnFocus = false,
                    ),
                onKeyboardAction = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onDone()
                },
                decorator =
                    TextFieldDecorator { innerTextField ->
                        Box {
                            if (state.text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = amountStyle,
                                    color = colors.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            innerTextField()
                        }
                    },
            )
            // Programmatic focus does not open the system keyboard, so an unfocused tap only focuses
            // the field here and the in-app keypad shows. Once focused this layer is gone: the next
            // tap reaches the field, which then opens the system keyboard.
            if (!focused) {
                Box(
                    modifier =
                        Modifier.matchParentSize().pointerInput(Unit) {
                            detectTapGestures { focusRequester.requestFocus() }
                        },
                )
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val PLACEHOLDER_ALPHA = 0.6f

/**
 * Keeps only what the amount accepts: digits and one decimal separator, the one of [locale], with
 * at most two decimals. A dot and a comma both become that separator, because some keypads only
 * offer one of them. Invalid edits are reverted; a swapped separator keeps the caret in place.
 */
private fun amountInputTransformation(locale: Locale): InputTransformation =
    InputTransformation {
        val raw = asCharSequence().toString()
        val normalized = AmountVisualTransformation.normalizeDecimalSeparator(raw, locale)
        if (!AmountVisualTransformation.isValidInput(normalized, locale)) {
            revertAllChanges()
        } else if (normalized != raw) {
            if (normalized.length == raw.length) {
                // Same length: replace only the characters that differ, so the caret does not move.
                for (index in normalized.indices) {
                    if (normalized[index] != raw[index]) replace(index, index + 1, normalized[index].toString())
                }
            } else {
                replace(0, length, normalized)
                placeCursorAtEnd()
            }
        }
    }

/**
 * Shows the amount as the form always has: the digits grouped as [AmountVisualTransformation]
 * groups them and a small dimmed "$ " in front. It only inserts characters, so the caret maps
 * through the formatting. Empty text gets no output, so the placeholder shows.
 */
private fun amountOutputTransformation(
    locale: Locale,
    symbolColor: Color,
): OutputTransformation {
    val grouping = AmountVisualTransformation(locale)
    return OutputTransformation {
        val raw = asCharSequence().toString()
        if (raw.isNotEmpty()) {
            // Formatting only adds grouping separators, so each raw character is met in order.
            val formatted = grouping.filter(AnnotatedString(raw)).text.text
            var index = 0
            for (ch in formatted) {
                if (index < length && charAt(index) == ch) {
                    index++
                } else {
                    insert(index, ch.toString())
                    index++
                }
            }
            insert(0, SYMBOL_PREFIX)
            addStyle(
                SpanStyle(
                    color = symbolColor,
                    fontSize = FORM_SYMBOL_SCALE.em,
                    baselineShift = BaselineShift(HERO_SYMBOL_RAISE),
                ),
                start = 0,
                end = SYMBOL_PREFIX.length,
            )
        }
    }
}

/**
 * Swallows `bringIntoView` requests from the amount field while the form settles on its initial
 * programmatic focus (§9.1): the focusable itself asks to be brought into view on every focus
 * gain, and with the in-app keypad shrinking the viewport that first request would scroll the
 * type segments off the top on small screens. On a phone the amount is already visible, so
 * dropping that one request changes nothing there; later taps scroll normally because the form
 * removes this modifier on the first focus loss.
 */
@Suppress("ModifierInspectorInfo")
private fun Modifier.suppressBringIntoView(): Modifier = this.then(SuppressBringIntoViewElement)

private data object SuppressBringIntoViewElement : ModifierNodeElement<SuppressBringIntoViewNode>() {
    override fun create(): SuppressBringIntoViewNode = SuppressBringIntoViewNode()

    override fun update(node: SuppressBringIntoViewNode) = Unit
}

private class SuppressBringIntoViewNode :
    Modifier.Node(),
    BringIntoViewModifierNode {
    override suspend fun bringIntoView(
        childCoordinates: LayoutCoordinates,
        boundsProvider: () -> Rect?,
    ) {
        // Drop the request on purpose; see [suppressBringIntoView].
    }
}

@Preview(name = "Amount Light", showBackground = true, widthDp = 360)
@Composable
private fun TransactionAmountFieldPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        TransactionAmountField(
            value = "1250000",
            onValueChange = {},
            label = "¿Cuánto fue?",
            focusRequester = remember { FocusRequester() },
            onDone = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Amount Dark empty", showBackground = true, widthDp = 360)
@Composable
private fun TransactionAmountFieldPreviewDark() {
    WalletTheme(darkTheme = true, dynamicColor = false) {
        TransactionAmountField(
            value = "",
            onValueChange = {},
            label = "¿Cuánto fue?",
            focusRequester = remember { FocusRequester() },
            onDone = {},
            errorMessage = "Monto inválido",
            modifier = Modifier.padding(16.dp),
        )
    }
}
