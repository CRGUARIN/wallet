package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import com.crguarin.wallet.ui.util.formatMedium
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Shows the reminder chip when the statement is overdue, or pending and due within 7 days. */
fun showPaymentReminder(
    status: StatementPaymentStatus?,
    dueDate: LocalDate?,
    today: LocalDate,
): Boolean {
    if (status == null || dueDate == null) return false
    return when (status) {
        StatementPaymentStatus.OVERDUE -> true
        StatementPaymentStatus.PENDING -> !dueDate.isAfter(today.plusDays(7))
        StatementPaymentStatus.PAID -> false
    }
}

/** "Disponible $ 750.000": how much of the credit limit is left. */
@Composable
internal fun cardAvailableText(overview: CardOverview): String {
    val locale = LocalConfiguration.current.locales[0]
    return stringResource(R.string.card_available) + " " + MoneyFormatter.format(overview.available, locale)
}

/**
 * Header of a credit card: the tinted block with the debt, then [actions] (the main buttons of the
 * screen) and the section with the limit, the closing and the statement.
 */
@Composable
fun CardSummaryCard(
    overview: CardOverview,
    today: LocalDate,
    modifier: Modifier = Modifier,
    account: Account? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val inFavor = overview.debt < Money.ZERO
    val displayDebt = if (inFavor) -overview.debt else overview.debt
    // Without an account (previews, tests) the block uses the theme primary as its accent.
    val accentColor = account?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    val spacing = MaterialTheme.walletSpacing
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(accentColor = accentColor) { swatch ->
            if (account != null) {
                AccountIdentityRow(account = account, headerSwatch = swatch)
                Spacer(modifier = Modifier.height(spacing.m))
            }
            Text(
                text =
                    if (inFavor) {
                        stringResource(R.string.card_credit_in_favor)
                    } else {
                        stringResource(R.string.card_debt)
                    },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            HeaderAmount(
                amount = displayDebt,
                type = if (inFavor) TransactionType.INCOME else TransactionType.EXPENSE,
            )
        }

        if (actions != null) {
            Spacer(modifier = Modifier.height(spacing.m))
            actions()
        }

        Spacer(modifier = Modifier.height(spacing.m))

        SectionCard {
            LinearProgressIndicator(
                progress = { overview.usedPerMille / 1000f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    stringResource(
                        R.string.card_used_of_limit,
                        MoneyFormatter.format(displayDebt, locale),
                        MoneyFormatter.format(overview.creditLimit, locale),
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text =
                    stringResource(R.string.card_available) +
                        " " + MoneyFormatter.format(overview.available, locale),
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(12.dp))
            val daysUntilClosing = ChronoUnit.DAYS.between(today, overview.nextClosingDate).toInt()
            Text(
                text =
                    if (daysUntilClosing <= 0) {
                        stringResource(R.string.card_closing_today)
                    } else {
                        pluralStringResource(R.plurals.card_closing_in_days, daysUntilClosing, daysUntilClosing)
                    },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            if (overview.paymentStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                CardStatementStatusRow(overview = overview)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.card_next_statement_estimate),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                MoneyText(
                    amount = overview.nextStatementCapitalEstimate,
                    style = MaterialTheme.walletTypography.rowAmount,
                )
            }
            Text(
                text = stringResource(R.string.card_estimate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CardStatementStatusRow(overview: CardOverview) {
    val status = overview.paymentStatus ?: return
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.card_statement_pending),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (overview.pendingStatementAmount != null) {
                MoneyText(
                    amount = overview.pendingStatementAmount,
                    style = MaterialTheme.walletTypography.rowAmount,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        when (status) {
            StatementPaymentStatus.PAID -> {
                Text(
                    text = stringResource(R.string.card_paid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            StatementPaymentStatus.PENDING -> {
                val due = overview.pendingStatementDueDate
                if (due != null) {
                    Text(
                        text = stringResource(R.string.card_pay_before, due.formatMedium()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            StatementPaymentStatus.OVERDUE -> {
                Text(
                    text = stringResource(R.string.card_overdue),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
