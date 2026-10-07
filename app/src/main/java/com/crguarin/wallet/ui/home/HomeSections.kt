package com.crguarin.wallet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.crguarin.wallet.ui.accounts.CardRowBalance
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneyText
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
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_upcoming_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
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

@Composable
internal fun TotalBalanceCard(totalBalance: Money) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.accounts_total_balance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            MoneyText(
                amount = totalBalance,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthSummaryCard(
    monthSummary: MonthSummary,
    onViewReports: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.account_detail_month_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
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
}

@Composable
internal fun SummaryColumn(
    label: String,
    amount: Money,
    type: TransactionType? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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

@Composable
internal fun AccountsRow(
    accounts: List<AccountWithBalance>,
    onAccountClick: (Long) -> Unit,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
) {
    // A plain scrollable Row (few accounts) so every tile takes the height of the tallest one:
    // card tiles show an extra "available" line.
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min),
    ) {
        accounts.forEach { item ->
            val cardOverview = cardOverviews[item.account.id]
            OutlinedCard(
                onClick = { onAccountClick(item.account.id) },
                shape = RoundedCornerShape(16.dp),
                modifier =
                    Modifier
                        .width(170.dp)
                        .fillMaxHeight(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(item.account.color)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = IconCatalog.get(item.account.icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = item.account.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (item.account.type == AccountType.CREDIT_CARD && cardOverview != null) {
                        CardRowBalance(
                            overview = cardOverview,
                            today = today,
                            horizontalAlignment = Alignment.Start,
                        )
                    } else {
                        MoneyText(
                            amount = item.balance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private const val STACKED_FONT_SCALE = 1.3f

@Composable
internal fun QuickActions(
    canTransfer: Boolean,
    onQuickAction: (TransactionType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.home_quick_actions))
        // Large font scales do not fit three buttons side by side: stack them instead.
        val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE
        val buttons: @Composable (Modifier) -> Unit = { buttonModifier ->
            QuickActionButton(
                label = stringResource(R.string.type_expense),
                icon = Icons.Default.ArrowDownward,
                onClick = { onQuickAction(TransactionType.EXPENSE) },
                modifier = buttonModifier,
            )
            QuickActionButton(
                label = stringResource(R.string.type_income),
                icon = Icons.Default.ArrowUpward,
                onClick = { onQuickAction(TransactionType.INCOME) },
                modifier = buttonModifier,
            )
            if (canTransfer) {
                QuickActionButton(
                    label = stringResource(R.string.type_transfer),
                    icon = Icons.Default.SwapHoriz,
                    onClick = { onQuickAction(TransactionType.TRANSFER) },
                    modifier = buttonModifier,
                )
            }
        }
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buttons(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buttons(Modifier.weight(1f))
            }
        }
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
