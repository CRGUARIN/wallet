package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.crguarin.wallet.ui.components.MoneyText

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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            AccountIdentityRow(account = account)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.transit_current_balance_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            MoneyText(
                amount = balance,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )

            if (fare != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.transit_fare_value, MoneyFormatter.format(fare, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (TransitMath.isLowBalance(balance, fare)) {
                    Text(
                        text = stringResource(R.string.transit_low_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    val trips = TransitMath.tripsLeft(balance, fare)
                    Text(
                        text = pluralStringResource(R.plurals.transit_trips_left, trips, trips),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

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
            if (TransitMath.isLowBalance(balance, fare)) {
                Text(
                    text = stringResource(R.string.transit_low_balance_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
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
