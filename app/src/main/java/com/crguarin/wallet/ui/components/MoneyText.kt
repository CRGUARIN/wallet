package com.crguarin.wallet.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSemanticColors
import java.util.Locale

/**
 * Sign shown before a monetary amount.
 *
 * [AUTO] derives it from the transaction type (income `+`, expense `−`, transfers and plain
 * amounts without a sign).
 */
enum class MoneySign { AUTO, PLUS, MINUS, NONE }

@Composable
fun MoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
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

    Text(
        text = text,
        style = style.merge(TextStyle(fontFeatureSettings = "tnum")),
        color = textColor,
        modifier = modifier.semantics { contentDescription = description },
    )
}

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
