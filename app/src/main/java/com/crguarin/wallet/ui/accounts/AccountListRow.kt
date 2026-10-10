package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** From this font scale up the balance of a row drops under the name and status, so neither is squeezed. */
private const val STACKED_FONT_SCALE = 1.15f

private val ROW_AVATAR_SIZE = 44.dp
private val ROW_MIN_HEIGHT = 64.dp

/**
 * The one account row of the app, used by the Accounts tab and by Home: a pebble, the name, one
 * short status line and the balance at the end.
 * - Credit card: "Pago vencido", "Pagar en 6 días" when it is due within a week, or the
 *   available credit otherwise.
 * - Transit card: its trips left, or the state of the credit trip.
 * - Any other account: its type label, only when [showTypeLabel] is set.
 *
 * [trailing] replaces the balance (the reorder arrows of the Accounts tab). With large fonts the
 * balance drops under the name and status.
 */
@Composable
internal fun AccountListRow(
    item: AccountWithBalance,
    onClick: () -> Unit,
    today: LocalDate,
    modifier: Modifier = Modifier,
    cardOverview: CardOverview? = null,
    showTypeLabel: Boolean = false,
    isArchived: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = MaterialTheme.walletSpacing
    val account = item.account
    val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE && trailing == null
    val status = accountStatus(item = item, cardOverview = cardOverview, today = today, showTypeLabel = showTypeLabel)
    val needsAttention = !isArchived && status?.needsAttention == true
    val nameColor = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val balanceColor =
        if (isArchived || !account.includeInTotal) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = spacing.l, vertical = spacing.s),
        // With the balance under the text the block is taller than the avatar: align them at the top.
        verticalAlignment = if (stacked) Alignment.Top else Alignment.CenterVertically,
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(account.icon),
            color = Color(account.color),
            size = ROW_AVATAR_SIZE,
            showRing = !isArchived,
            needsAttention = needsAttention,
        )
        Spacer(modifier = Modifier.width(spacing.m))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = nameColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null) {
                Text(
                    text = status.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = status.color,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (stacked) {
                MoneyText(
                    amount = item.balance,
                    style = MaterialTheme.walletTypography.rowAmount,
                    color = balanceColor,
                )
            }
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(spacing.s))
            trailing()
        } else if (!stacked) {
            Spacer(modifier = Modifier.width(spacing.m))
            MoneyText(
                amount = item.balance,
                style = MaterialTheme.walletTypography.rowAmount,
                color = balanceColor,
            )
        }
    }
}

/** What the status line says, the color it is drawn in, and whether it asks for attention. */
private data class AccountStatus(
    val text: String,
    val color: Color,
    val needsAttention: Boolean,
)

/** The status line of a row, or null when the row has nothing to say under its name. */
@Composable
private fun accountStatus(
    item: AccountWithBalance,
    cardOverview: CardOverview?,
    today: LocalDate,
    showTypeLabel: Boolean,
): AccountStatus? {
    val account = item.account
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val card = cardOverview?.takeIf { account.type == AccountType.CREDIT_CARD }
    if (card != null) {
        val dueDate = card.pendingStatementDueDate
        return when {
            card.paymentStatus == StatementPaymentStatus.OVERDUE ->
                AccountStatus(stringResource(R.string.card_overdue), MaterialTheme.colorScheme.error, true)
            dueDate != null && showPaymentReminder(card.paymentStatus, dueDate, today) ->
                AccountStatus(
                    text = payInText(ChronoUnit.DAYS.between(today, dueDate).toInt()),
                    color = MaterialTheme.colorScheme.tertiary,
                    needsAttention = true,
                )
            else -> AccountStatus(cardAvailableText(card), neutral, false)
        }
    }
    if (account.type == AccountType.TRANSIT_CARD) {
        val transit = transitStatus(item.balance, account.fare) ?: return null
        val color = if (transit.isWarning) MaterialTheme.colorScheme.error else neutral
        return AccountStatus(transit.text, color, transit.isWarning)
    }
    return if (showTypeLabel) AccountStatus(accountTypeLabel(account.type), neutral, false) else null
}

/** "Pagar hoy" / "Pagar mañana" / "Pagar en 6 días": as short as the status line of a row needs. */
@Composable
private fun payInText(days: Int): String =
    when {
        days <= 0 -> stringResource(R.string.card_pay_today)
        days == 1 -> stringResource(R.string.card_pay_tomorrow)
        else -> pluralStringResource(R.plurals.card_pay_in_days, days, days)
    }
