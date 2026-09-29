package com.crguarin.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.util.relativeDayLabel
import java.time.LocalDate

/**
 * Header shown before the first transaction of each day. Displays the localized day label
 * ("Hoy"/"Ayer"/full date) and the net amount of that day.
 */
@Composable
fun TransactionDayHeader(
    date: LocalDate,
    net: Money,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val absoluteNet = if (net.isNegative) -net else net
    val sign =
        when {
            net.isNegative -> MoneySign.MINUS
            net.isZero -> MoneySign.NONE
            else -> MoneySign.PLUS
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = relativeDayLabel(date = date, today = today),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .semantics { heading() },
        )
        MoneyText(
            amount = absoluteNet,
            sign = sign,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Day Header Light", showBackground = true)
@Composable
private fun TransactionDayHeaderPreviewLight() {
    WalletTheme(darkTheme = false) {
        TransactionDayHeader(
            date = LocalDate.of(2026, 9, 28),
            net = Money(2_500_000L),
            today = LocalDate.of(2026, 9, 28),
        )
    }
}

@Preview(name = "Day Header Dark", showBackground = true)
@Composable
private fun TransactionDayHeaderPreviewDark() {
    WalletTheme(darkTheme = true) {
        TransactionDayHeader(
            date = LocalDate.of(2026, 9, 27),
            net = Money(-450_000L),
            today = LocalDate.of(2026, 9, 28),
        )
    }
}
