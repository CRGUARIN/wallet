package com.crguarin.wallet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.recurring.UpcomingKind
import com.crguarin.wallet.domain.model.recurring.UpcomingPayment
import com.crguarin.wallet.domain.model.transit.TransitMath
import com.crguarin.wallet.ui.accounts.AccountListRow
import com.crguarin.wallet.ui.components.HeroAmountText
import com.crguarin.wallet.ui.components.HeroCard
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.components.bounceClick
import com.crguarin.wallet.ui.theme.WalletShapeTokens
import com.crguarin.wallet.ui.theme.tintOver
import com.crguarin.wallet.ui.theme.walletColors
import com.crguarin.wallet.ui.theme.walletSemanticColors
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Sections of the Home screen, composed by [HomeScreen].

/** Quick action pebbles: the semantic color mixed over the surface at 13% (light) and 20% (dark). */
private const val QUICK_TINT_LIGHT = 0.13f
private const val QUICK_TINT_DARK = 0.20f

/** Above this font scale the quick actions and the account balances stack instead of sitting side by side. */
private const val STACKED_FONT_SCALE = 1.3f
private const val STACKED_ROW_FONT_SCALE = 1.15f

private val QuickActionPebbleSize = 56.dp

/** Avatar of an account row, ring included: the tinted pebble inside it is 44 dp. */
internal val AccountAvatarSize = 52.dp

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
        Spacer(modifier = Modifier.height(MaterialTheme.walletSpacing.s))
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
    val spacing = MaterialTheme.walletSpacing
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
    // With large fonts the amount goes under the texts, so the name keeps the whole width.
    val stacked = LocalDensity.current.fontScale > STACKED_ROW_FONT_SCALE
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(vertical = spacing.s),
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
                fontWeight = FontWeight.Bold,
                maxLines = if (stacked) 2 else 1,
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
                text =
                    if (payment.kind == UpcomingKind.CARD_PAYMENT) {
                        whenText
                    } else {
                        "$whenText · ${payment.accountName}"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (stacked) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (stacked) {
                UpcomingPaymentAmount(payment)
            }
        }
        if (!stacked) {
            Spacer(modifier = Modifier.width(spacing.m))
            UpcomingPaymentAmount(payment)
        }
    }
}

@Composable
private fun UpcomingPaymentAmount(payment: UpcomingPayment) {
    if (payment.amount != null) {
        MoneyText(
            amount = payment.amount,
            type = payment.type,
            style = MaterialTheme.walletTypography.rowAmount,
        )
    } else {
        Text(
            text = stringResource(R.string.upcoming_amount_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Headline card with the signature gradient: the total balance of every active account. */
@Composable
internal fun TotalBalanceCard(totalBalance: Money) {
    val colors = MaterialTheme.walletColors
    val locale = LocalConfiguration.current.locales[0]
    // Same text MoneyText would draw: the formatter's no-break spaces become plain ones.
    val formatted =
        MoneyFormatter
            .format(totalBalance, locale)
            .replace(' ', ' ')
            .replace(' ', ' ')
    HeroCard(animateContours = true) {
        Text(
            text = stringResource(R.string.accounts_total_balance),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onHero,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.walletSpacing.xs))
        HeroAmountText(
            text = formatted,
            color = colors.onHero,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = formatted },
        )
    }
}

/**
 * Month totals in one compact card: the three figures sit in a row when they fit and wrap with
 * large fonts. "Ver reportes" shares the title row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthSummaryCard(
    monthSummary: MonthSummary,
    onViewReports: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    SectionCard {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(
                title = stringResource(R.string.account_detail_month_summary),
                modifier = Modifier.padding(end = spacing.s),
            )
            TextButton(onClick = onViewReports) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(text = stringResource(R.string.home_view_reports))
            }
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        // One row of three equal columns; with large fonts they stack, so no amount is squeezed.
        if (LocalDensity.current.fontScale > STACKED_FONT_SCALE) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                SummaryColumn(
                    label = stringResource(R.string.account_detail_income),
                    amount = monthSummary.income,
                    type = TransactionType.INCOME,
                    showDot = false,
                )
                SummaryColumn(
                    label = stringResource(R.string.account_detail_expense),
                    amount = monthSummary.expense,
                    type = TransactionType.EXPENSE,
                    showDot = false,
                )
                SummaryColumn(
                    label = stringResource(R.string.account_detail_net),
                    amount = monthSummary.net,
                    showDot = false,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                SummaryColumn(
                    label = stringResource(R.string.account_detail_income),
                    amount = monthSummary.income,
                    type = TransactionType.INCOME,
                    modifier = Modifier.weight(1f),
                    showDot = false,
                )
                SummaryColumn(
                    label = stringResource(R.string.account_detail_expense),
                    amount = monthSummary.expense,
                    type = TransactionType.EXPENSE,
                    modifier = Modifier.weight(1f),
                    showDot = false,
                )
                SummaryColumn(
                    label = stringResource(R.string.account_detail_net),
                    amount = monthSummary.net,
                    modifier = Modifier.weight(1f),
                    showDot = false,
                )
            }
        }
    }
}

/**
 * Label above an amount, colored by [type] when it has one. [showDot] adds the colored dot before
 * the label; the month summary leaves it out.
 */
@Composable
internal fun SummaryColumn(
    label: String,
    amount: Money,
    type: TransactionType? = null,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    val spacing = MaterialTheme.walletSpacing
    // Colored dot before the label: income, expense, or neutral for the net and for zero totals.
    val dotColor =
        when (type) {
            TransactionType.INCOME -> semanticColors.income
            TransactionType.EXPENSE -> semanticColors.expense
            TransactionType.TRANSFER -> semanticColors.transfer
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDot) {
                Spacer(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                )
                Spacer(modifier = Modifier.width(spacing.xs + spacing.xxs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
        MoneyText(
            amount = amount,
            type = type,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            // Indented so the amount lines up with the label, after the dot.
            modifier = Modifier.padding(start = if (showDot) spacing.m + spacing.xxs else 0.dp),
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
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/** Status line of a transit card: trips left, or the state of the credit trip. Null without a fare. */
@Composable
internal fun transitStatusText(
    balance: Money,
    fare: Money?,
): String? =
    when {
        fare == null -> null
        TransitMath.isCreditUsed(balance) -> stringResource(R.string.transit_credit_used_short)
        TransitMath.isCreditTrip(balance, fare) -> stringResource(R.string.transit_credit_available_short)
        else -> {
            val trips = TransitMath.tripsLeft(balance, fare)
            pluralStringResource(R.plurals.transit_trips_short, trips, trips)
        }
    }

/** True when a transit card cannot pay a regular trip: only the credit trip is left, or it is owed. */
internal fun transitNeedsAttention(
    balance: Money,
    fare: Money?,
): Boolean = TransitMath.isCreditTrip(balance, fare) || TransitMath.isCreditUsed(balance)

/**
 * All active accounts as a vertical list on one tonal layer, one [AccountListRow] after another with
 * no divider lines.
 */
@Composable
internal fun AccountsList(
    accounts: List<AccountWithBalance>,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    cardOverviews: Map<Long, CardOverview> = emptyMap(),
    today: LocalDate = LocalDate.now(),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = MaterialTheme.walletSpacing.s),
    ) {
        accounts.forEach { item ->
            AccountListRow(
                item = item,
                onClick = { onAccountClick(item.account.id) },
                today = today,
                cardOverview = cardOverviews[item.account.id],
            )
        }
    }
}

/** One quick action of the Home screen: the label and icon, the color it is drawn in, and what it does. */
private data class QuickAction(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

/**
 * Shortcuts under the balance: Expense, Income, Transfer (with two accounts) and Trip (with a
 * transit card). The section title is not drawn; it stays as a heading for screen readers.
 */
@Composable
internal fun QuickActions(
    canTransfer: Boolean,
    canRegisterTrip: Boolean,
    onQuickAction: (TransactionType) -> Unit,
    onTripClick: () -> Unit,
) {
    val semanticColors = MaterialTheme.walletSemanticColors
    val actions =
        listOfNotNull(
            QuickAction(
                key = "expense",
                label = stringResource(R.string.type_expense),
                icon = Icons.Default.ArrowDownward,
                color = semanticColors.expense,
                onClick = { onQuickAction(TransactionType.EXPENSE) },
            ),
            QuickAction(
                key = "income",
                label = stringResource(R.string.type_income),
                icon = Icons.Default.ArrowUpward,
                color = semanticColors.income,
                onClick = { onQuickAction(TransactionType.INCOME) },
            ),
            QuickAction(
                key = "transfer",
                label = stringResource(R.string.type_transfer),
                icon = Icons.Default.SwapHoriz,
                color = semanticColors.transfer,
                onClick = { onQuickAction(TransactionType.TRANSFER) },
            ).takeIf { canTransfer },
            QuickAction(
                key = "trip",
                label = stringResource(R.string.home_quick_trip),
                icon = Icons.Default.DirectionsBus,
                color = MaterialTheme.colorScheme.primary,
                onClick = onTripClick,
            ).takeIf { canRegisterTrip },
        )
    val title = stringResource(R.string.home_quick_actions)
    // Large font scales do not fit the buttons side by side: stack them instead.
    val stacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE
    val groupModifier =
        Modifier
            .fillMaxWidth()
            .semantics {
                heading()
                contentDescription = title
            }
    if (stacked) {
        Column(
            modifier = groupModifier,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.s),
        ) {
            actions.forEach { action ->
                QuickActionRow(
                    label = action.label,
                    icon = action.icon,
                    color = action.color,
                    onClick = action.onClick,
                )
            }
        }
    } else {
        Row(
            modifier = groupModifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            actions.forEach { action ->
                QuickActionItem(
                    label = action.label,
                    icon = action.icon,
                    color = action.color,
                    onClick = action.onClick,
                )
            }
        }
    }
}

/** Tint of a quick action pebble: [color] over the screen surface. */
@Composable
private fun quickActionContainer(color: Color): Color {
    val fraction = if (MaterialTheme.walletColors.isDark) QUICK_TINT_DARK else QUICK_TINT_LIGHT
    return tintOver(color, MaterialTheme.colorScheme.surface, fraction)
}

/** Quick action: a 56 dp tinted pebble with the icon in the semantic color and the label below it. */
@Composable
internal fun QuickActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(
        modifier =
            Modifier
                .widthIn(min = 72.dp)
                .clip(MaterialTheme.shapes.medium)
                .bounceClick(onClick = onClick)
                .padding(horizontal = spacing.xs, vertical = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(QuickActionPebbleSize)
                    .clip(WalletShapeTokens.pebble)
                    .background(quickActionContainer(color)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs + spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Stacked form of a quick action, for large fonts: the pebble and the label side by side, full width. */
@Composable
private fun QuickActionRow(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .bounceClick(onClick = onClick)
                .padding(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(QuickActionPebbleSize)
                    .clip(WalletShapeTokens.pebble)
                    .background(quickActionContainer(color)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.width(spacing.m))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
