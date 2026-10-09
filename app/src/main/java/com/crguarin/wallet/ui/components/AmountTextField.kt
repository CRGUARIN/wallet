package com.crguarin.wallet.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.util.Locale

/** Corner radius of the amount field layer. */
private val AMOUNT_FIELD_RADIUS = 24.dp

/** Size of the "$" relative to the digits. */
private const val AMOUNT_SYMBOL_SCALE = 0.55f

/** Width of a Manrope 800 tabular digit as a fraction of the font size, rounded up on purpose. */
private const val DIGIT_WIDTH_EM = 0.64f

/** Smallest size (sp) the amount shrinks to so a long amount still fits on one line. */
private const val MIN_AMOUNT_FONT_SP = 12f

/** Inner padding the text field leaves on each side of the text, plus room for the "$". */
private const val FIELD_SIDE_PADDING_DP = 16f
private const val SYMBOL_ROOM_DP = 28f

/**
 * Amount field: a tonal layer (`surfaceContainerLow`, radius 24) with no indicator line, the
 * amount in `walletTypography.formAmount` (Manrope 800, tabular figures) and a smaller "$" in
 * front. The label ("Monto"), the thousands separators of [AmountVisualTransformation] and the
 * semantics are the same as ever; a long amount shrinks, down to 12 sp, so it keeps fitting in one
 * line at any font scale.
 *
 * [imeAction] and [keyboardActions] let the screen react to the keyboard action (for example "Listo").
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = stringResource(R.string.amount_text_field_label),
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    locale: Locale = LocalConfiguration.current.locales[0],
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val density = LocalDensity.current
    val transformation = remember(locale) { AmountVisualTransformation(locale) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }

    val baseStyle = textStyle ?: MaterialTheme.walletTypography.formAmount
    val shownLength =
        transformation
            .filter(AnnotatedString(value))
            .text.text.length
    val fontSize =
        fitAmountFontSize(
            base = baseStyle.fontSize,
            shownLength = shownLength,
            availableDp = with(density) { fieldWidthPx.toDp().value } - 2 * FIELD_SIDE_PADDING_DP - SYMBOL_ROOM_DP,
            fontScale = density.fontScale,
        )
    val fieldStyle =
        if (fontSize == baseStyle.fontSize) {
            baseStyle
        } else {
            baseStyle.copy(fontSize = fontSize, lineHeight = lineHeightFor(baseStyle, fontSize))
        }
    val symbolColor = MaterialTheme.colorScheme.onSurfaceVariant
    val layer = MaterialTheme.colorScheme.surfaceContainerLow
    val colors =
        TextFieldDefaults.colors(
            focusedContainerColor = layer,
            unfocusedContainerColor = layer,
            disabledContainerColor = layer,
            errorContainerColor = layer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        )

    TextField(
        value = value,
        onValueChange = { input ->
            val normalized = AmountVisualTransformation.normalizeDecimalSeparator(input, locale)
            if (AmountVisualTransformation.isValidInput(normalized, locale)) {
                onValueChange(normalized)
            }
        },
        modifier = modifier.onSizeChanged { fieldWidthPx = it.width },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = fieldStyle,
        label = label?.let { { Text(text = it) } },
        prefix = {
            Text(
                text = amountWithSmallSymbol("$", symbolScale = AMOUNT_SYMBOL_SCALE, symbolColor = symbolColor),
                style = fieldStyle,
                modifier = Modifier.padding(end = MaterialTheme.walletSpacing.xs),
            )
        },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(text = it) } },
        singleLine = true,
        shape = RoundedCornerShape(AMOUNT_FIELD_RADIUS),
        colors = colors,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = transformation,
    )
}

/**
 * Font size for an amount of [shownLength] characters (separators included) in a field with
 * [availableDp] of room for the text: [base] if it fits, otherwise the largest size that does,
 * never below 12 sp. Holds at any [fontScale], because the text grows with it.
 */
internal fun fitAmountFontSize(
    base: TextUnit,
    shownLength: Int,
    availableDp: Float,
    fontScale: Float,
): TextUnit {
    if (!base.isSp || shownLength <= 0 || availableDp <= 0f) return base
    val fits = availableDp / (shownLength * DIGIT_WIDTH_EM * fontScale)
    return fits.coerceIn(minimumValue = minOf(MIN_AMOUNT_FONT_SP, base.value), maximumValue = base.value).sp
}

/** Keeps the line height proportional to the font size of [style] while the size shrinks. */
private fun lineHeightFor(
    style: TextStyle,
    fontSize: TextUnit,
): TextUnit =
    if (style.lineHeight.isSp && style.fontSize.isSp && fontSize.isSp) {
        (style.lineHeight.value / style.fontSize.value * fontSize.value).sp
    } else {
        style.lineHeight
    }

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun AmountTextFieldPreviewLight() {
    WalletTheme(darkTheme = false) {
        AmountTextField(
            value = "1250000",
            onValueChange = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun AmountTextFieldPreviewDark() {
    WalletTheme(darkTheme = true) {
        AmountTextField(
            value = "1250000",
            onValueChange = {},
            errorMessage = "Monto inválido",
        )
    }
}
