package com.crguarin.wallet.ui.accounts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.money.MoneyFormatter
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.motion.PulseDot
import com.crguarin.wallet.ui.motion.StaggerClock
import com.crguarin.wallet.ui.motion.animationsOn
import com.crguarin.wallet.ui.motion.raisedSurface
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

/** Corner radius of the "Disponible" card: the surface of the detail's summary blocks. */
private val AVAILABLE_CARD_CORNER = 24.dp

/** The usage bar fills from zero on enter, in 450 ms with this easing (mock `cupo`). */
private const val USAGE_FILL_MILLIS = 450
private val USAGE_FILL_EASING = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Header of a credit card: the hero with the debt and, when there is one, the status line
 * (overdue, payment in N days), then [actions] (the main keys of the screen) and the "Disponible"
 * card with the limit used and the statement details.
 */
@Composable
internal fun CardSummaryCard(
    overview: CardOverview,
    today: LocalDate,
    modifier: Modifier = Modifier,
    account: Account? = null,
    status: AccountStatus? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    staggerClock: StaggerClock? = null,
    idleInterrupted: Boolean = false,
) {
    val locale = LocalConfiguration.current.locales[0]
    val inFavor = overview.debt < Money.ZERO
    val displayDebt = if (inFavor) -overview.debt else overview.debt
    // Without an account (previews, tests) the block uses the theme primary as its accent.
    val accentColor = account?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(
            accentColor = accentColor,
            accountId = account?.id,
            idleInterrupted = idleInterrupted,
        ) { face ->
            if (account != null) {
                AccountIdentityRow(account = account, face = face)
            }
            Column {
                Text(
                    text =
                        if (inFavor) {
                            stringResource(R.string.card_credit_in_favor)
                        } else {
                            stringResource(R.string.card_debt)
                        },
                    style = heroLabelStyle(),
                    color = face.content,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.walletSpacing.xs))
                HeaderAmount(
                    amount = displayDebt,
                    rollKey = "account-hero-${account?.id ?: "card"}",
                    type = if (inFavor) TransactionType.INCOME else TransactionType.EXPENSE,
                )
            }
            if (status != null) {
                HeroStatusLine(text = status.text, pulse = status.needsAttention, color = face.content)
            }
        }

        if (actions != null) {
            Spacer(modifier = Modifier.height(DETAIL_SECTION_GAP))
            StaggerSlot(clock = staggerClock, index = 0, content = actions)
        }

        Spacer(modifier = Modifier.height(DETAIL_SECTION_GAP))

        val shape = RoundedCornerShape(AVAILABLE_CARD_CORNER)
        Column(
            modifier =
                staggerModifier(clock = staggerClock, index = 1)
                    .fillMaxWidth()
                    .raisedSurface(shape)
                    .clip(shape)
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.card_available),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = MoneyFormatter.format(overview.available, locale),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.alignByBaseline(),
                )
            }
            UsageBar(fraction = overview.usedPerMille / 1000f)
            Text(
                text =
                    stringResource(
                        R.string.card_used_of_limit,
                        MoneyFormatter.format(displayDebt, locale),
                        MoneyFormatter.format(overview.creditLimit, locale),
                    ),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // The closing and the statement details stay in the same card, under the limit.
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
                CardStatementStatusRow(overview = overview, today = today)
            }

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

/**
 * Usage of the credit limit: an 8 dp rounded bar, track `surfaceVariant`, fill `primary`. The fill
 * grows from zero on enter; without animations it is drawn at its value at once.
 */
@Composable
private fun UsageBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val animate = animationsOn()
    val target = fraction.coerceIn(0f, 1f)
    val fill = remember { Animatable(if (animate) 0f else target) }
    LaunchedEffect(target, animate) {
        if (animate) {
            fill.animateTo(target, tween(durationMillis = USAGE_FILL_MILLIS, easing = USAGE_FILL_EASING))
        } else {
            fill.snapTo(target)
        }
    }
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(current = target, range = 0f..1f) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fill.value)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * Status of the current statement. When the statement needs attention (overdue, or pending and
 * due within 7 days) an 8 dp [PulseDot] in the state's text color pulses before the status
 * line. Decorative, without semantics: the text already says it.
 */
@Composable
private fun CardStatementStatusRow(
    overview: CardOverview,
    today: LocalDate,
) {
    val status = overview.paymentStatus ?: return
    val due = overview.pendingStatementDueDate
    val needsAttention = showPaymentReminder(status, due, today)
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
                if (due != null) {
                    StatusLine(
                        color = MaterialTheme.colorScheme.tertiary,
                        pulse = needsAttention,
                    ) {
                        Text(
                            text = stringResource(R.string.card_pay_before, due.formatMedium()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            StatementPaymentStatus.OVERDUE -> {
                StatusLine(
                    color = MaterialTheme.colorScheme.error,
                    pulse = needsAttention,
                ) {
                    Text(
                        text = stringResource(R.string.card_overdue),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** A status line, preceded by a pulsing dot when it needs attention. */
@Composable
private fun StatusLine(
    color: Color,
    pulse: Boolean,
    text: @Composable () -> Unit,
) {
    if (pulse) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseDot(color = color, size = 8.dp, pulse = true)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                text()
            }
        }
    } else {
        text()
    }
}
