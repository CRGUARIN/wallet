package com.crguarin.wallet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.recurring.UpcomingKind
import com.crguarin.wallet.domain.model.recurring.UpcomingPayment
import com.crguarin.wallet.domain.model.transit.TransitMath
import com.crguarin.wallet.ui.accounts.CardRowBalance
import com.crguarin.wallet.ui.accounts.TransitRowBalance
import com.crguarin.wallet.ui.accounts.showPaymentReminder
import com.crguarin.wallet.ui.components.HeroCard
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.components.StoryAvatar
import com.crguarin.wallet.ui.components.bounceClick
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.walletSemanticColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Sections of the Home screen, composed by [HomeScreen].

@Composable
internal fun UpcomingPaymentsCard(
    payments: List<UpcomingPayment>,
    today: LocalDate,
    onUpcomingRecurringClick: (Long) -> Unit,
    onUpcomingPendingClick: () -> Unit,
    onUpcomingCardClick: (Long) -> Unit,
    onViewAllRecurring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.home_upcoming_title))
        Spacer(modifier = Modifier.height(8.dp))
        payments.take(5).forEach { payment ->
            UpcomingPaymentRow(
                payment = payment,
                today = today,
                onClick = {
                    when (payment.kind) {
                        UpcomingKind.RECURRING -> onUpcomingRecurringClick(payment.refId)
                        UpcomingKind.RECURRING_PENDING -> onUpcomingPendingClick()
                        UpcomingKind.CARD_PAYMENT -> onUpcomingCardClick(payment.refId)
                    }
                },
            )
        }
        TextButton(
            onClick = onViewAllRecurring,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.home_view_all))
        }
    }
}

@Composable
internal fun UpcomingPaymentRow(
    payment: UpcomingPayment,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val daysUntil =
        ChronoUnit.DAYS
            .between(today, payment.date)
            .toInt()
            .coerceAtLeast(0)
    val whenText =
        when (daysUntil) {
            0 -> stringResource(R.string.upcoming_today)
            1 -> stringResource(R.string.upcoming_tomorrow)
            else -> pluralStringResource(R.plurals.upcoming_in_days, daysUntil, daysUntil)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val title =
                if (payment.kind == UpcomingKind.CARD_PAYMENT) {
                    stringResource(R.string.upcoming_card_payment, payment.title)
                } else {
                    payment.title
                }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (payment.kind == UpcomingKind.RECURRING_PENDING) {
                Text(
                    text = stringResource(R.string.recurring_section_pending),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "$whenText · ${payment.accountName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (payment.amount != null) {
            MoneyText(
                amount = payment.amount,
                type = payment.type,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        } else {
            Text(
                text = stringResource(R.string.upcoming_amount_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Headline card with the signature gradient: the total balance of every active account. */
@Composable
internal fun TotalBalanceCard(totalBalance: Money) {
    HeroCard {
        Text(
            text = stringResource(R.string.accounts_total_balance),
            style = MaterialTheme.typography.titleSmall,
            color = LocalContentColor.current.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        MoneyText(
            amount = totalBalance,
            style = MaterialTheme.typography.headlineLarge,
            color = LocalContentColor.current,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthSummaryCard(
    monthSummary: MonthSummary,
    onViewReports: () -> Unit,
) {
    SectionCard {
        SectionHeader(title = stringResource(R.string.account_detail_month_summary))
        Spacer(modifier = Modifier.height(12.dp))
        // FlowRow so the three amounts wrap instead of overlapping with large font scales.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryColumn(
                label = stringResource(R.string.account_detail_income),
                amount = monthSummary.income,
                type = TransactionType.INCOME,
            )
            SummaryColumn(
                label = stringResource(R.string.account_detail_expense),
                amount = monthSummary.expense,
                type = TransactionType.EXPENSE,
            )
            SummaryColumn(
                label = stringResource(R.string.account_detail_net),
                amount = monthSummary.net,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onViewReports,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.home_view_reports))
        }
    }
}

@Composable
internal fun SummaryColumn(
    label: String,
    amount: Money,
    type: TransactionType? = null,
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    // Colored dot before the label: income, expense, or neutral for the net and for zero totals.
    val dotColor =
        when (type) {
            TransactionType.INCOME -> semanticColors.income
            TransactionType.EXPENSE -> semanticColors.expense
            TransactionType.TRANSFER -> semanticColors.transfer
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            // Indented so the amount lines up with the label, after the dot.
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Accounts as Instagram-style stories: a colored circle per account, with a warm ring when the
 * account needs attention (a card payment reminder or a transit card that cannot pay one trip).
 */
@Composable
internal fun AccountsRow(
    accounts: List<AccountWithBalance>,
    onAccountClick: (Long) -> Unit,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        accounts.forEach { item ->
            val cardOverview = cardOverviews[item.account.id]
            val color = Color(item.account.color)
            val needsAttention =
                when {
                    cardOverview != null && item.account.type == AccountType.CREDIT_CARD ->
                        showPaymentReminder(cardOverview.paymentStatus, cardOverview.pendingStatementDueDate, today)
                    item.account.type == AccountType.TRANSIT_CARD -> TransitMath.isLowBalance(item.balance, item.account.fare)
                    else -> false
                }
            StoryAvatar(
                icon = IconCatalog.get(item.account.icon),
                color = color,
                label = item.account.name,
                onClick = { onAccountClick(item.account.id) },
                ring = if (needsAttention) WalletBrushes.attentionRing else WalletBrushes.storyRing(color),
                itemWidth = 136.dp,
                subtitle = {
                    when {
                        cardOverview != null && item.account.type == AccountType.CREDIT_CARD ->
                            CardRowBalance(
                                overview = cardOverview,
                                today = today,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            )
                        item.account.type == AccountType.TRANSIT_CARD ->
                            TransitRowBalance(
                                balance = item.balance,
                                fare = item.account.fare,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            )
                        else ->
                            MoneyText(
                                amount = item.balance,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                    }
                },
            )
        }
    }
}

private const val STACKED_FONT_SCALE = 1.3f

/** One quick action of the Home screen: the label and icon, the color it is drawn in, and the type it creates. */
private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val type: TransactionType,
)

@Composable
internal fun QuickActions(
    canTransfer: Boolean,
    onQuickAction: (TransactionType) -> Unit,
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    val actions =
        listOfNotNull(
            QuickAction(
                label = stringResource(R.string.type_expense),
                icon = Icons.Default.ArrowDownward,
                color = semanticColors.expense,
                type = TransactionType.EXPENSE,
            ),
            QuickAction(
                label = stringResource(R.string.type_income),
                icon = Icons.Default.ArrowUpward,
                color = semanticColors.income,
                type = TransactionType.INCOME,
            ),
            QuickAction(
                label = stringResource(R.string.type_transfer),
                icon = Icons.Default.SwapHoriz,
                color = semanticColors.transfer,
                type = TransactionType.TRANSFER,
            ).takeIf { canTransfer },
        )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.home_quick_actions))
        // Large font scales do not fit three buttons side by side: stack them instead.
        val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEach { action ->
                    QuickActionButton(
                        label = action.label,
                        icon = action.icon,
                        onClick = { onQuickAction(action.type) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                actions.forEach { action ->
                    QuickActionItem(
                        label = action.label,
                        icon = action.icon,
                        color = action.color,
                        onClick = { onQuickAction(action.type) },
                    )
                }
            }
        }
    }
}

/** Circular quick action: a tinted circle with the icon and the label below it. */
@Composable
internal fun QuickActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .bounceClick(onClick = onClick)
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
