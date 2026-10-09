package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.walletSemanticColors

/**
 * Balance summary of the selected period: the big net (income − expense) with its hint, the
 * income/expense row, the savings-rate/net-change row and, without an account filter, the scope
 * footnote explaining which accounts count.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BalanceSummaryCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.balanceSummary
    val semanticColors = MaterialTheme.walletSemanticColors
    val net = summary.net
    val netColor = if (net.isNegative) semanticColors.expense else semanticColors.income
    val netSign =
        when {
            net.isPositive -> MoneySign.PLUS
            net.isNegative -> MoneySign.MINUS
            else -> MoneySign.NONE
        }

    SectionCard(modifier = modifier) {
        Column {
            Text(
                text = stringResource(R.string.reports_net),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // The sign is explicit (MoneyText prints it) so the absolute value is passed.
            MoneyText(
                amount = Money(kotlin.math.abs(net.minorUnits)),
                style = MaterialTheme.typography.headlineMedium,
                color = netColor,
                sign = netSign,
            )
            val hintRes =
                when {
                    net.isPositive -> R.string.reports_net_positive_hint
                    net.isNegative -> R.string.reports_net_negative_hint
                    else -> null
                }
            if (hintRes != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(hintRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = netColor,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // FlowRow so the two data points wrap instead of overlapping with large font scales.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BalanceColumn(label = stringResource(R.string.account_detail_income)) {
                    MoneyText(
                        amount = summary.income,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = semanticColors.income,
                        sign = MoneySign.NONE,
                    )
                }
                BalanceColumn(label = stringResource(R.string.account_detail_expense)) {
                    MoneyText(
                        amount = summary.expense,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = semanticColors.expense,
                        sign = MoneySign.NONE,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BalanceColumn(label = stringResource(R.string.reports_savings_rate)) {
                    val rate = summary.savingsRatePerMille
                    if (rate == null) {
                        Text(
                            text = stringResource(R.string.reports_savings_rate_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = perMillePercent(rate),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        )
                    }
                }
                BalanceColumn(
                    label =
                        stringResource(
                            if (uiState.mode == ReportMode.MONTH) {
                                R.string.reports_net_vs_previous_month
                            } else {
                                R.string.reports_net_vs_previous_year
                            },
                        ),
                ) {
                    NetChangeValue(netChange = summary.netChange)
                }
            }
            if (uiState.accountId == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.reports_balance_scope_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BalanceColumn(
    label: String,
    value: @Composable () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        value()
    }
}

/**
 * Net change in money against the previous period ("▲ 120.000 $" / "▼ 80.000 $"), or the
 * no-previous-data line when [netChange] is null. A zero change shows the amount without an arrow.
 */
@Composable
private fun NetChangeValue(netChange: Money?) {
    if (netChange == null) {
        Text(
            text = stringResource(R.string.reports_no_previous_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val semanticColors = MaterialTheme.walletSemanticColors
    val changeColor =
        when {
            netChange.isPositive -> semanticColors.income
            netChange.isNegative -> semanticColors.expense
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val arrow =
        when {
            netChange.isPositive -> stringResource(R.string.reports_change_up)
            netChange.isNegative -> stringResource(R.string.reports_change_down)
            else -> null
        }
    val amount = formattedMoney(Money(kotlin.math.abs(netChange.minorUnits)), locale)
    val label = if (arrow == null) amount else "$arrow $amount"
    val description =
        when {
            netChange.isPositive -> stringResource(R.string.reports_cd_net_change_up, amount)
            netChange.isNegative -> stringResource(R.string.reports_cd_net_change_down, amount)
            else -> amount
        }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = changeColor,
        modifier = Modifier.semantics { contentDescription = description },
    )
}
