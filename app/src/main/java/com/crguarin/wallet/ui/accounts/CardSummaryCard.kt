package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun PaymentReminderChip(
    status: StatementPaymentStatus,
    dueDate: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val overdue = status == StatementPaymentStatus.OVERDUE
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text =
                    if (overdue) {
                        stringResource(R.string.card_overdue)
                    } else {
                        stringResource(R.string.card_pay_before, dueDate.formatMedium())
                    },
            )
        },
        colors =
            if (overdue) {
                AssistChipDefaults.assistChipColors(
                    disabledLabelColor = MaterialTheme.colorScheme.error,
                )
            } else {
                AssistChipDefaults.assistChipColors()
            },
        modifier = modifier,
    )
}

/**
 * Amounts for a card row: debt (or credit in favor) with its label, available below,
 * and the payment reminder chip when it applies.
 */
@Composable
fun CardRowBalance(
    overview: CardOverview,
    today: LocalDate,
    modifier: Modifier = Modifier,
    // End for the trailing side of list rows; Start where the block sits under the account name.
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
) {
    val inFavor = overview.debt < Money.ZERO
    val displayAmount = if (inFavor) -overview.debt else overview.debt
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        Text(
            text =
                if (inFavor) {
                    stringResource(R.string.card_credit_in_favor)
                } else {
                    stringResource(R.string.card_debt)
                },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            amount = displayAmount,
            type = if (inFavor) TransactionType.INCOME else TransactionType.EXPENSE,
            style = MaterialTheme.typography.titleMedium,
        )
        val locale = LocalConfiguration.current.locales[0]
        Text(
            text =
                stringResource(
                    R.string.card_available,
                ) + " " + MoneyFormatter.format(overview.available, locale),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showPaymentReminder(overview.paymentStatus, overview.pendingStatementDueDate, today)) {
            Spacer(modifier = Modifier.height(4.dp))
            PaymentReminderChip(
                status = overview.paymentStatus!!,
                dueDate = overview.pendingStatementDueDate!!,
                today = today,
            )
        }
    }
}

@Composable
fun CardSummaryCard(
    overview: CardOverview,
    today: LocalDate,
    modifier: Modifier = Modifier,
    account: Account? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val inFavor = overview.debt < Money.ZERO
    val displayDebt = if (inFavor) -overview.debt else overview.debt
    // Without an account (previews, tests) the block uses the theme primary as its accent.
    val accentColor = account?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(accentColor = accentColor) { contentColor ->
            if (account != null) {
                AccountIdentityRow(account = account, contentColor = contentColor)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text =
                    if (inFavor) {
                        stringResource(R.string.card_credit_in_favor)
                    } else {
                        stringResource(R.string.card_debt)
                    },
                style = MaterialTheme.typography.titleSmall,
                color = contentColor.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            HeaderAmount(
                amount = displayDebt,
                contentColor = contentColor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                type = if (inFavor) TransactionType.INCOME else TransactionType.EXPENSE,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionCard(contentPadding = AccountSectionPadding) {
            LinearProgressIndicator(
                progress = { overview.usedPerMille / 1000f },
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
