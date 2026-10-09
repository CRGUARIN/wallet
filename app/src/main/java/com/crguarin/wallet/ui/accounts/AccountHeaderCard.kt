package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.HeroAmountText
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.WalletSwatch
import com.crguarin.wallet.ui.theme.rememberWalletSwatch
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography

/** Size of the account avatar in a header. */
private val HEADER_AVATAR_SIZE = 60.dp

/** Largest size of the header balance; it shrinks to fit on one line. */
private val HEADER_AMOUNT_SIZE = 34.sp
private val HEADER_AMOUNT_LINE_HEIGHT = 40.sp

/**
 * Top block of an account header: a layer tinted with the account color (the color mixed over
 * `surfaceContainerLow`), with `onSurface` and `onSurfaceVariant` text over it. [content] receives
 * the [WalletSwatch] of that layer, which the avatar uses as its base.
 */
@Composable
internal fun AccountHeaderBlock(
    accentColor: Color,
    content: @Composable ColumnScope.(swatch: WalletSwatch) -> Unit,
) {
    val swatch = rememberWalletSwatch(accentColor)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(swatch.container)
                .padding(MaterialTheme.walletSpacing.cardPadding),
    ) {
        content(swatch)
    }
}

/**
 * Amount printed on an account header block: Manrope 800, tabular, with a small raised "$", in
 * `onSurface`, shrinking to fit one line. The sign and the screen reader text follow [type] and
 * [sign] exactly like [MoneyText], but the color is neutral: the semantic income and expense colors
 * are for rows, and here the sign and the label carry the meaning.
 */
@Composable
internal fun HeaderAmount(
    amount: Money,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    sign: MoneySign = MoneySign.AUTO,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = amount.displayText(locale)
    val resolvedSign =
        when (sign) {
            MoneySign.AUTO ->
                when (type) {
                    TransactionType.INCOME -> MoneySign.PLUS
                    TransactionType.EXPENSE -> MoneySign.MINUS
                    TransactionType.TRANSFER, null -> MoneySign.NONE
                }
            else -> sign
        }
    val text =
        when (resolvedSign) {
            MoneySign.PLUS -> "+ $formatted"
            MoneySign.MINUS -> "− $formatted"
            MoneySign.AUTO, MoneySign.NONE -> formatted
        }
    // Screen readers should say "expense" or "minus", not read the "−" glyph.
    val description =
        when (type) {
            TransactionType.INCOME -> stringResource(R.string.a11y_amount_income, formatted)
            TransactionType.EXPENSE -> stringResource(R.string.a11y_amount_expense, formatted)
            TransactionType.TRANSFER, null ->
                when (resolvedSign) {
                    MoneySign.PLUS -> stringResource(R.string.a11y_amount_positive, formatted)
                    MoneySign.MINUS -> stringResource(R.string.a11y_amount_negative, formatted)
                    MoneySign.AUTO, MoneySign.NONE -> formatted
                }
        }
    HeroAmountText(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style =
            MaterialTheme.walletTypography.heroAmount.copy(
                fontSize = HEADER_AMOUNT_SIZE,
                lineHeight = HEADER_AMOUNT_LINE_HEIGHT,
            ),
        modifier = modifier.semantics { contentDescription = description },
    )
}

/**
 * Header of a cash or savings account: the tinted block with the balance and, below it, [actions]
 * (the main buttons of the screen) and the month summary.
 */
@Composable
internal fun AccountHeaderCard(
    account: Account,
    balance: Money,
    monthSummary: MonthSummary,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val spacing = MaterialTheme.walletSpacing
    Column(modifier = modifier.fillMaxWidth()) {
        AccountHeaderBlock(accentColor = Color(account.color)) { swatch ->
            AccountIdentityRow(account = account, headerSwatch = swatch)

            Spacer(modifier = Modifier.height(spacing.m))

            Text(
                text = stringResource(R.string.account_detail_current_balance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            HeaderAmount(amount = balance, sign = MoneySign.NONE)
        }

        if (actions != null) {
            Spacer(modifier = Modifier.height(spacing.m))
            actions()
        }

        Spacer(modifier = Modifier.height(spacing.m))

        SectionCard {
            MonthSummarySection(monthSummary = monthSummary)
        }
    }
}

/** Income, expense and net of the month; shared by the account, card and transit headers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthSummarySection(monthSummary: MonthSummary) {
    val spacing = MaterialTheme.walletSpacing
    Text(
        text = stringResource(R.string.account_detail_month_summary),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(spacing.m))
    // FlowRow: the three amounts share a row when they fit and wrap with big fonts or long amounts.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        SummaryAmount(
            label = stringResource(R.string.account_detail_income),
            amount = monthSummary.income,
            type = TransactionType.INCOME,
        )
        SummaryAmount(
            label = stringResource(R.string.account_detail_expense),
            amount = monthSummary.expense,
            type = TransactionType.EXPENSE,
        )
        SummaryAmount(
            label = stringResource(R.string.account_detail_net),
            amount = monthSummary.net,
        )
    }
}

@Composable
private fun SummaryAmount(
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
            style = MaterialTheme.walletTypography.rowAmount,
        )
    }
}

/**
 * Avatar, name, type and archived badge of [account] over an account header block. [headerSwatch]
 * is the swatch of that block: the avatar tints the account color over it, with the pure color
 * only in the ring. Shared by the account, card and transit headers.
 */
@Composable
internal fun AccountIdentityRow(
    account: Account,
    headerSwatch: WalletSwatch,
) {
    val spacing = MaterialTheme.walletSpacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        PebbleAvatar(
            icon = IconCatalog.get(account.icon),
            color = Color(account.color),
            size = HEADER_AVATAR_SIZE,
            showRing = true,
            base = headerSwatch.container,
        )
        Spacer(modifier = Modifier.width(spacing.l))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = accountTypeLabel(account.type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (account.isArchived) {
            Text(
                text = stringResource(R.string.account_detail_archived_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier =
                    Modifier
                        .padding(start = spacing.s)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = spacing.s, vertical = spacing.xs),
            )
        }
    }
}

/** Display name of an account type. */
@Composable
internal fun accountTypeLabel(type: AccountType): String =
    when (type) {
        AccountType.CASH -> stringResource(R.string.account_type_cash)
        AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
        AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
        AccountType.TRANSIT_CARD -> stringResource(R.string.account_type_transit_card)
    }
