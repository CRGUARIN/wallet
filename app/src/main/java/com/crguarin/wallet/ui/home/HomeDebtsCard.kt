package com.crguarin.wallet.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.crguarin.wallet.R
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.debt.DebtsOverview
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.theme.walletSpacing

/**
 * Home card with the totals of the active debts: what others owe me and what I owe. Shown only
 * when there are active debts; [onViewDebts] opens the debts list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeDebtsCard(
    debts: DebtsOverview,
    onViewDebts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.debts_title))
        Spacer(modifier = Modifier.height(MaterialTheme.walletSpacing.m))
        // FlowRow so the two totals wrap instead of overlapping with large font scales.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.walletSpacing.s),
        ) {
            SummaryColumn(
                label = stringResource(R.string.debts_total_owed_to_me),
                amount = debts.totalOwedToMe,
                // A zero total stays neutral instead of showing "+ $ 0" or "− $ 0".
                type = TransactionType.INCOME.takeIf { debts.totalOwedToMe.isPositive },
            )
            SummaryColumn(
                label = stringResource(R.string.debts_total_i_owe),
                amount = debts.totalIOwe,
                type = TransactionType.EXPENSE.takeIf { debts.totalIOwe.isPositive },
            )
        }
        TextButton(
            onClick = onViewDebts,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.debts_view_all))
        }
    }
}
