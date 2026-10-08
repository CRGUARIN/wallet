package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.transit.TransitMath
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard

/** Header of a transit card: the balance, the fare per trip and how many trips the balance still pays. */
@Composable
fun TransitCardSummaryCard(
    account: Account,
    balance: Money,
    monthSummary: MonthSummary,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val fare = account.fare
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(accentColor = Color(account.color)) { contentColor ->
            AccountIdentityRow(account = account, contentColor = contentColor)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.transit_current_balance_label),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            HeaderAmount(
                amount = balance,
                contentColor = contentColor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                sign = MoneySign.NONE,
            )

            if (fare != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.transit_fare_value, MoneyFormatter.format(fare, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                val headerText =
                    when {
                        TransitMath.isCreditUsed(balance) -> stringResource(R.string.transit_credit_used)
                        TransitMath.isCreditTrip(balance, fare) -> stringResource(R.string.transit_credit_available)
                        else -> {
                            val trips = TransitMath.tripsLeft(balance, fare)
                            pluralStringResource(R.plurals.transit_trips_left, trips, trips)
                        }
                    }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionCard(contentPadding = AccountSectionPadding) {
            MonthSummarySection(monthSummary = monthSummary)
        }
    }
}

/**
 * Balance of a transit card in a tile or list row, with the trips it still pays under it. Nothing
 * is shown under the balance when the card has no fare.
 */
@Composable
fun TransitRowBalance(
    balance: Money,
    fare: Money?,
    modifier: Modifier = Modifier,
    // End for the trailing side of list rows; Start where the block sits under the account name.
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    balanceColor: Color = Color.Unspecified,
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        MoneyText(
            amount = balance,
            style = MaterialTheme.typography.titleMedium,
            color = balanceColor,
        )
        if (fare != null) {
            when {
                TransitMath.isCreditUsed(balance) ->
                    Text(
                        text = stringResource(R.string.transit_credit_used_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                TransitMath.isCreditTrip(balance, fare) ->
                    Text(
                        text = stringResource(R.string.transit_credit_available_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                else -> {
                    val trips = TransitMath.tripsLeft(balance, fare)
                    Text(
                        text = pluralStringResource(R.plurals.transit_trips_short, trips, trips),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
