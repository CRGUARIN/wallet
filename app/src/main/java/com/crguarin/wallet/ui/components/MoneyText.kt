package com.crguarin.wallet.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSemanticColors
import java.util.Locale

@Composable
fun MoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    val textColor =
        when (type) {
            TransactionType.INCOME -> semanticColors.income
            TransactionType.EXPENSE -> semanticColors.expense
            TransactionType.TRANSFER -> semanticColors.transfer
            null -> color.takeOrElse { LocalContentColor.current }
        }

    val formattedAmount =
        MoneyFormatter
            .format(amount, locale)
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')

    val text =
        when (type) {
            TransactionType.INCOME -> "+ $formattedAmount"
            TransactionType.EXPENSE -> "− $formattedAmount"
            TransactionType.TRANSFER, null -> formattedAmount
        }

    Text(
        text = text,
        style = style,
        color = textColor,
        modifier = modifier,
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
