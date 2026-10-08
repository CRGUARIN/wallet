package com.crguarin.wallet.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.MonthSummary
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.LocalWalletSemanticColors
import com.crguarin.wallet.ui.theme.WalletBrushes
import com.crguarin.wallet.ui.theme.WalletSemanticColors
import com.crguarin.wallet.ui.theme.contentColorOver

/** Padding of the surface-container section that sits under an account header block. */
internal val AccountSectionPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)

/**
 * Top block of an account header: the account gradient, with text and icons in the readable color over it.
 * [content] receives that content color.
 */
@Composable
internal fun AccountHeaderBlock(
    accentColor: Color,
    content: @Composable ColumnScope.(contentColor: Color) -> Unit,
) {
    val contentColor = contentColorOver(accentColor)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(WalletBrushes.account(accentColor))
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
    ) {
        content(contentColor)
    }
}

/**
 * Amount printed on an account header block. The sign and the screen reader text still follow [type]
 * and [sign], but the color is [contentColor]: the semantic income and expense colors do not read on the gradient.
 */
@Composable
internal fun HeaderAmount(
    amount: Money,
    contentColor: Color,
    style: TextStyle,
    type: TransactionType? = null,
    sign: MoneySign = MoneySign.AUTO,
) {
    val blockColors = WalletSemanticColors(income = contentColor, expense = contentColor, transfer = contentColor)
    CompositionLocalProvider(LocalWalletSemanticColors provides blockColors) {
        MoneyText(
            amount = amount,
            type = type,
            style = style,
            color = contentColor,
            sign = sign,
        )
    }
}

@Composable
internal fun AccountHeaderCard(
    account: Account,
    balance: Money,
    monthSummary: MonthSummary,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AccountHeaderBlock(accentColor = Color(account.color)) { contentColor ->
            AccountIdentityRow(account = account, contentColor = contentColor)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.account_detail_current_balance),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            HeaderAmount(
                amount = balance,
                contentColor = contentColor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                sign = MoneySign.NONE,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionCard(contentPadding = AccountSectionPadding) {
            MonthSummarySection(monthSummary = monthSummary)
        }
    }
}

/** Income, expense and net of the month; shared by the account and card headers. */
@Composable
internal fun MonthSummarySection(monthSummary: MonthSummary) {
    Text(
        text = stringResource(R.string.account_detail_month_summary),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * Icon, name, type and archived badge of [account], drawn in [contentColor] over an account header block.
 * Shared by the account, card and transit headers.
 */
@Composable
internal fun AccountIdentityRow(
    account: Account,
    contentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconCatalog.get(account.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val typeText =
                when (account.type) {
                    AccountType.CASH -> stringResource(R.string.account_type_cash)
                    AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
                    AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
                    AccountType.TRANSIT_CARD -> stringResource(R.string.account_type_transit_card)
                }
            Text(
                text = typeText,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.85f),
            )
        }
        if (account.isArchived) {
            Text(
                text = stringResource(R.string.account_detail_archived_badge),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                modifier =
                    Modifier
                        .padding(start = 8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(contentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
