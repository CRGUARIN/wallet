package com.crguarin.wallet.ui.reports.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Line chart of the balance evolution: one point per day (month mode) or per month end (year
 * mode), with a zero line when the range crosses zero.
 *
 * @param values y positions 0..1, one per point, in order.
 * @param slotCount total x slots (days in month or 12); values.size <= slotCount.
 * @param zeroFraction y of the zero line when the range crosses zero, else null.
 * @param xLabels (slot index, label) pairs for the x axis.
 * @param yLabels y axis labels, bottom to top, evenly spaced.
 * @param contentDescription accessibility summary resolved by the caller with `stringResource`.
 *
 * Contract stub: the body is implemented by the "ejes y gráficas" owner.
 */
@Composable
fun BalanceLineChart(
    values: List<Float>,
    slotCount: Int,
    zeroFraction: Float?,
    xLabels: List<Pair<Int, String>>,
    yLabels: List<String>,
    lineColor: Color,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.semantics { this.contentDescription = contentDescription })
}
