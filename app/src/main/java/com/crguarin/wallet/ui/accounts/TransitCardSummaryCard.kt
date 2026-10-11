package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.transit.TransitMath
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.motion.StaggerClock
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography

/**
 * Header of a transit card: the balance, the fare per trip and how many trips the balance still
 * pays, then [actions] (register trip, top up) and the month summary.
 */
@Composable
fun TransitCardSummaryCard(
    account: Account,
    balance: Money,
    monthSummary: MonthSummary,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    staggerClock: StaggerClock? = null,
    idleInterrupted: Boolean = false,
) {
    val locale = LocalConfiguration.current.locales[0]
    val fare = account.fare
    val spacing = MaterialTheme.walletSpacing
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(
            accentColor = Color(account.color),
            accountId = account.id,
            idleInterrupted = idleInterrupted,
        ) { face ->
            AccountIdentityRow(account = account, face = face)

            Spacer(modifier = Modifier.height(spacing.m))

            Text(
                text = stringResource(R.string.transit_current_balance_label),
                style = MaterialTheme.typography.titleSmall,
                color = face.content,
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            HeaderAmount(
                amount = balance,
                rollKey = "account-hero-${account.id}",
                sign = MoneySign.NONE,
            )

            if (fare != null) {
                Spacer(modifier = Modifier.height(spacing.s))
                Text(
                    text = stringResource(R.string.transit_fare_value, MoneyFormatter.format(fare, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = face.content,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
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
                    style = MaterialTheme.typography.titleSmall,
                    color = face.content,
                )
            }
        }

        if (actions != null) {
            Spacer(modifier = Modifier.height(spacing.m))
            StaggerSlot(clock = staggerClock, index = 0, content = actions)
        }

        Spacer(modifier = Modifier.height(spacing.m))

        SectionCard(modifier = staggerModifier(clock = staggerClock, index = 1)) {
            MonthSummarySection(monthSummary = monthSummary)
        }
    }
}

/** Short status line of a transit card in a list row, and whether it asks for attention. */
internal data class TransitStatus(
    val text: String,
    val isWarning: Boolean,
)

/**
 * "3 viajes" / "Viaje a crédito" / "Recarga para viajar": what a transit card can still do, or
 * null when it has no fare.
 */
@Composable
internal fun transitStatus(
    balance: Money,
    fare: Money?,
): TransitStatus? {
    if (fare == null) return null
    return when {
        TransitMath.isCreditUsed(balance) ->
            TransitStatus(stringResource(R.string.transit_credit_used_short), isWarning = true)
        TransitMath.isCreditTrip(balance, fare) ->
            TransitStatus(stringResource(R.string.transit_credit_available_short), isWarning = true)
        else -> {
            val trips = TransitMath.tripsLeft(balance, fare)
            TransitStatus(pluralStringResource(R.plurals.transit_trips_short, trips, trips), isWarning = false)
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
            style = MaterialTheme.walletTypography.rowAmount,
            color = balanceColor,
        )
        val status = transitStatus(balance, fare)
        if (status != null) {
            Text(
                text = status.text,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (status.isWarning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
