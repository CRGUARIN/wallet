package com.crguarin.wallet.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.components.AmountVisualTransformation
import com.crguarin.wallet.ui.components.amountWithSmallSymbol
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.util.Locale

/** Size of the "$" in the form amount, relative to the digits. */
private const val FORM_SYMBOL_SCALE = 0.55f

private const val SYMBOL_PREFIX = "$ "

private val AmountLayerRadius = 24.dp
private val AmountVerticalPadding = 14.dp

/**
 * The amount of the movement form, the main figure of the screen: Manrope 800 at 44 sp with
 * tabular figures, a small dimmed "$" in front and the digits grouped as they are typed, on a
 * tonal layer. The keyboard action is "Done": it hides the keyboard and calls [onDone] (the form
 * uses it to open the category list when one is missing).
 */
@Composable
internal fun TransactionAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.walletSpacing
    val amountStyle = MaterialTheme.walletTypography.formAmount
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val symbolColor = colors.onSurfaceVariant
    val transformation =
        remember(locale, symbolColor) { SymbolPrefixTransformation(AmountVisualTransformation(locale), symbolColor) }
    val placeholder =
        remember(symbolColor) {
            amountWithSmallSymbol(SYMBOL_PREFIX + "0", FORM_SYMBOL_SCALE, symbolColor = symbolColor)
        }
    val isError = errorMessage != null

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AmountLayerRadius))
                .background(colors.surfaceContainerLow)
                .padding(horizontal = spacing.xl, vertical = AmountVerticalPadding),
    ) {
        Text(
            text = stringResource(R.string.amount_text_field_label),
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) colors.error else colors.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = { input ->
                val normalized = AmountVisualTransformation.normalizeDecimalSeparator(input, locale)
                if (AmountVisualTransformation.isValidInput(normalized, locale)) {
                    onValueChange(normalized)
                }
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            textStyle = amountStyle.copy(color = if (isError) colors.error else colors.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onDone()
                    },
                ),
            visualTransformation = if (value.isEmpty()) VisualTransformation.None else transformation,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = amountStyle,
                            color = colors.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }
    }
}

private const val PLACEHOLDER_ALPHA = 0.6f

/**
 * Puts a small "$ " before the digits that [inner] formats, keeping the cursor and selection
 * mapped: the two extra characters shift every offset.
 */
private class SymbolPrefixTransformation(
    private val inner: VisualTransformation,
    private val symbolColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = inner.filter(text)
        val shift = SYMBOL_PREFIX.length
        val prefixed =
            amountWithSmallSymbol(
                text = SYMBOL_PREFIX + formatted.text.text,
                symbolScale = FORM_SYMBOL_SCALE,
                symbolColor = symbolColor,
            )
        return TransformedText(
            prefixed,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = formatted.offsetMapping.originalToTransformed(offset) + shift

                override fun transformedToOriginal(offset: Int): Int =
                    formatted.offsetMapping.transformedToOriginal((offset - shift).coerceAtLeast(0))
            },
        )
    }
}

@Preview(name = "Amount Light", showBackground = true, widthDp = 360)
@Composable
private fun TransactionAmountFieldPreviewLight() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        TransactionAmountField(
            value = "1250000",
            onValueChange = {},
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
            focusRequester = remember { FocusRequester() },
            onDone = {},
            errorMessage = "Monto inválido",
            modifier = Modifier.padding(16.dp),
        )
    }
}
