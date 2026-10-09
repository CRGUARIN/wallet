package com.crguarin.wallet.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.TABULAR_FIGURES
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSemanticColors
import com.crguarin.wallet.ui.theme.walletTypography
import java.util.Locale

/**
 * Sign shown before a monetary amount.
 *
 * [AUTO] derives it from the transaction type (income `+`, expense `−`, transfers and plain
 * amounts without a sign).
 */
enum class MoneySign { AUTO, PLUS, MINUS, NONE }

/**
 * A monetary amount with its sign, in tabular figures (every digit the same width, so amounts do
 * not jiggle) and, by default, the money style of `walletTypography.rowAmount` (Manrope 800). The
 * sign and the semantic color are never the only carrier of meaning: screen readers get the type
 * ("Gasto", "Ingreso") instead of the glyph.
 */
@Composable
fun MoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle = MaterialTheme.walletTypography.rowAmount,
    color: Color = Color.Unspecified,
    locale: Locale = LocalConfiguration.current.locales[0],
    sign: MoneySign = MoneySign.AUTO,
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    val textColor =
        when (type) {
            TransactionType.INCOME -> semanticColors.income
            TransactionType.EXPENSE -> semanticColors.expense
            TransactionType.TRANSFER -> semanticColors.transfer
            null -> color.takeOrElse { LocalContentColor.current }
        }

    val resolvedSign =
        when (sign) {
            MoneySign.AUTO ->
                when (type) {
                    TransactionType.INCOME -> MoneySign.PLUS
                    TransactionType.EXPENSE -> MoneySign.MINUS
                    TransactionType.TRANSFER, null -> MoneySign.NONE
                }
            else -> sign
        }

    val formattedAmount =
        MoneyFormatter
            .format(amount, locale)
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')

    val text =
        when (resolvedSign) {
            MoneySign.PLUS -> "+ $formattedAmount"
            MoneySign.MINUS -> "− $formattedAmount"
            MoneySign.AUTO, MoneySign.NONE -> formattedAmount
        }

    // Screen readers should say "expense" or "minus", not read the "−" glyph.
    val description =
        when (type) {
            TransactionType.INCOME -> stringResource(R.string.a11y_amount_income, formattedAmount)
            TransactionType.EXPENSE -> stringResource(R.string.a11y_amount_expense, formattedAmount)
            TransactionType.TRANSFER ->
                when (resolvedSign) {
                    MoneySign.MINUS -> stringResource(R.string.a11y_amount_transfer_out, formattedAmount)
                    MoneySign.PLUS -> stringResource(R.string.a11y_amount_transfer_in, formattedAmount)
                    MoneySign.AUTO, MoneySign.NONE -> stringResource(R.string.a11y_amount_transfer, formattedAmount)
                }
            null ->
                when (resolvedSign) {
                    MoneySign.PLUS -> stringResource(R.string.a11y_amount_positive, formattedAmount)
                    MoneySign.MINUS -> stringResource(R.string.a11y_amount_negative, formattedAmount)
                    MoneySign.AUTO, MoneySign.NONE -> formattedAmount
                }
        }

    // One line, so the sign and "$" never end up on a line apart from the figures: an amount that
    // does not fit (large font, narrow column) shrinks instead of wrapping, down to 60 % of its size.
    val merged = LocalTextStyle.current.merge(style)
    val lineHeight =
        if (merged.lineHeight.isSp && merged.fontSize.isSp) {
            (merged.lineHeight.value / merged.fontSize.value).em
        } else {
            merged.lineHeight
        }
    val fitStyle =
        merged.merge(TextStyle(color = textColor, lineHeight = lineHeight, fontFeatureSettings = TABULAR_FIGURES))
    BasicText(
        text = text,
        style = fitStyle,
        modifier = modifier.semantics { contentDescription = description },
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        autoSize =
            if (merged.fontSize.isSp) {
                TextAutoSize.StepBased(
                    minFontSize = (merged.fontSize.value * MIN_FONT_FRACTION).sp,
                    maxFontSize = merged.fontSize,
                )
            } else {
                null
            },
    )
}

private const val MIN_FONT_FRACTION = 0.6f

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun MoneyTextPreviewLight() {
    WalletTheme(darkTheme = false) {
        MoneyText(
            amount = Money(5_000_000L),
            type = TransactionType.INCOME,
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun MoneyTextPreviewDark() {
    WalletTheme(darkTheme = true) {
        MoneyText(
            amount = Money(2_000_000L),
            type = TransactionType.EXPENSE,
        )
    }
}
