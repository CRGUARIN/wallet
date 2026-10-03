package com.crguarin.wallet.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crguarin.wallet.R
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.ui.components.EmptyState
import com.crguarin.wallet.ui.components.IconCatalog
import com.crguarin.wallet.ui.components.MoneySign
import com.crguarin.wallet.ui.components.MoneyText
import com.crguarin.wallet.ui.components.categoryDisplayName
import com.crguarin.wallet.ui.reports.charts.DonutChart
import com.crguarin.wallet.ui.theme.WalletTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth

/**
 * Category donut with its legend. Tapping a slice or a legend row selects it; slices with children
 * can be drilled down and monthly rows have a shortcut to Movements with the report filters.
 */
@Composable
internal fun CategoryBreakdownCard(
    uiState: ReportsUiState,
    onSelectSlice: (SliceKey) -> Unit,
    onDrillInto: (Long) -> Unit,
    onDrillUp: () -> Unit,
    onViewTransactions: (categoryId: Long?, accountId: Long?, type: TransactionType, yearMonth: YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val parent = uiState.drillDownParent
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (parent != null) {
                    IconButton(onClick = onDrillUp, enabled = !uiState.isRefreshing) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.reports_drill_up),
                        )
                    }
                }
                Text(
                    text = parent?.let { sliceLabel(it) } ?: stringResource(R.string.reports_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
            }

            if (uiState.hasNoDataOfType) {
                EmptyState(
                    icon = Icons.Default.PieChart,
                    title =
                        stringResource(
                            if (uiState.type == TransactionType.INCOME) {
                                R.string.reports_empty_incomes
                            } else {
                                R.string.reports_empty_expenses
                            },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val slices = uiState.breakdown.slices
                val refreshing = uiState.isRefreshing
                val othersColor = MaterialTheme.colorScheme.outline
                val uncategorizedColor = MaterialTheme.colorScheme.outlineVariant
                val donutSlices =
                    remember(uiState.breakdown, othersColor, uncategorizedColor) {
                        ReportsChartMapper.donutSlices(uiState.breakdown, othersColor, uncategorizedColor)
                    }
                val selected = slices.firstOrNull { it.key == uiState.selectedSliceKey }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DonutChart(
                        slices = donutSlices,
                        selectedKey = uiState.selectedSliceKey,
                        onSliceClick = { if (!refreshing) onSelectSlice(it) },
                        centerContent = { DonutCenter(selected = selected, total = uiState.breakdown.total) },
                        contentDescription = donutDescription(slices),
                        modifier = Modifier.size(200.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    slices.forEach { slice ->
                        // "(general)" navigates to the parent id, which would also match every
                        // subcategory in Movements, so it has no shortcut, like "Others".
                        val showTransactions =
                            uiState.mode == ReportMode.MONTH && slice.key is SliceKey.Category
                        LegendRow(
                            slice = slice,
                            selected = slice.key == uiState.selectedSliceKey,
                            showViewTransactions = showTransactions,
                            actionsEnabled = !refreshing,
                            onSelect = { if (!refreshing) onSelectSlice(slice.key) },
                            onDrillInto =
                                if (slice.hasChildren) {
                                    { parentId -> onDrillInto(parentId) }
                                } else {
                                    null
                                },
                            onViewTransactions = {
                                onViewTransactions(
                                    slice.categoryIdForNavigation(),
                                    uiState.accountId,
                                    uiState.type,
                                    uiState.yearMonth,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutCenter(
    selected: CategorySlice?,
    total: Money,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(116.dp),
    ) {
        if (selected == null) {
            CenterAmountText(
                amount = total,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.reports_total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = sliceLabel(selected),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            CenterAmountText(
                amount = selected.amount,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = perMillePercent(selected.perMille),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Amount of the donut center: a single shrinking line, formatted with [formattedMoney] exactly
 * like [MoneyText] with [MoneySign.NONE], so large amounts never overflow the ring hole.
 */
@Composable
private fun CenterAmountText(
    amount: Money,
    style: TextStyle,
) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = formattedMoney(amount, locale),
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize),
    )
}

@Composable
private fun LegendRow(
    slice: CategorySlice,
    selected: Boolean,
    showViewTransactions: Boolean,
    actionsEnabled: Boolean,
    onSelect: () -> Unit,
    onDrillInto: ((Long) -> Unit)?,
    onViewTransactions: () -> Unit,
) {
    val color =
        ReportsChartMapper.sliceColor(
            slice = slice,
            othersColor = MaterialTheme.colorScheme.outline,
            uncategorizedColor = MaterialTheme.colorScheme.outlineVariant,
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onSelect)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = IconCatalog.get(slice.icon ?: IconCatalog.DEFAULT_ICON_KEY),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sliceLabel(slice),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ProgressBar(perMille = slice.perMille, color = color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                amount = slice.amount,
                style = MaterialTheme.typography.bodyMedium,
                sign = MoneySign.NONE,
            )
            Text(
                text = perMillePercent(slice.perMille),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Both trailing slots always keep their width, so amounts, percents and progress bars
        // line up across rows whether or not the row has a chevron or a shortcut.
        if (slice.hasChildren && slice.key is SliceKey.Category) {
            IconButton(
                onClick = { onDrillInto?.invoke(slice.key.categoryId) },
                enabled = actionsEnabled,
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.reports_view_subcategories),
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
        if (showViewTransactions) {
            IconButton(onClick = onViewTransactions, enabled = actionsEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = stringResource(R.string.reports_view_transactions),
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ProgressBar(
    perMille: Int,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth((perMille / 1000f).coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(color),
        )
    }
}

/** User-visible label of a slice: category name, "Others", "Uncategorized" or "Parent (general)". */
@Composable
internal fun sliceLabel(slice: CategorySlice): String =
    when (slice.key) {
        SliceKey.Others -> stringResource(R.string.reports_others)
        SliceKey.Uncategorized -> stringResource(R.string.reports_uncategorized)
        is SliceKey.Category -> categoryDisplayName(slice.name, slice.systemKey)
        is SliceKey.ParentDirect ->
            stringResource(
                R.string.reports_parent_direct,
                categoryDisplayName(slice.name, slice.systemKey),
            )
    }

/** Category id used by "View transactions" for a slice, or null when it does not apply. */
private fun CategorySlice.categoryIdForNavigation(): Long? =
    when (val key = key) {
        is SliceKey.Category -> key.categoryId
        is SliceKey.ParentDirect -> null
        SliceKey.Others, SliceKey.Uncategorized -> null
    }

/** "45,3 %": per-mille formatted with one decimal and the percent sign of the locale. */
@Composable
internal fun perMillePercent(perMille: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatted =
        remember(locale, perMille) {
            NumberFormat
                .getNumberInstance(locale)
                .apply {
                    minimumFractionDigits = 1
                    maximumFractionDigits = 1
                }.format(BigDecimal.valueOf(perMille.toLong(), 1))
        }
    return stringResource(R.string.reports_percent, formatted)
}

/** Accessibility summary of the donut: "Gráfico de dona. Alimentación 45,3 %, …". */
@Composable
private fun donutDescription(slices: List<CategorySlice>): String {
    val separator = stringResource(R.string.reports_list_separator)
    val parts = ArrayList<String>(slices.size)
    for (slice in slices) {
        parts += stringResource(R.string.reports_cd_item, sliceLabel(slice), perMillePercent(slice.perMille))
    }
    return stringResource(R.string.reports_cd_donut, parts.joinToString(separator))
}

/**
 * Manual check for F20: a $123.456.789 total with font scale 2 must stay on a single shrinking
 * line inside the ring hole instead of overflowing it.
 */
@Preview(name = "Breakdown Large Amount Font Scale 2", showBackground = true, fontScale = 2f)
@Composable
private fun CategoryBreakdownCardLargeAmountPreview() {
    WalletTheme(darkTheme = false, dynamicColor = false) {
        CategoryBreakdownCard(
            uiState =
                ReportsUiState(
                    isLoading = false,
                    today = LocalDate.of(2026, 10, 15),
                    breakdown =
                        CategoryBreakdown(
                            total = Money(12_345_678_900L),
                            slices =
                                listOf(
                                    CategorySlice(
                                        key = SliceKey.Category(10L),
                                        name = "Alimentación",
                                        systemKey = "food",
                                        color = 0xFFEF5350.toInt(),
                                        icon = "restaurant",
                                        amount = Money(12_345_678_900L),
                                        perMille = 1000,
                                        hasChildren = false,
                                    ),
                                ),
                            parentId = null,
                        ),
                    monthlyTotals =
                        listOf(
                            MonthTotals(
                                yearMonth = YearMonth.of(2026, 10),
                                income = Money(1_000_000L),
                                expense = Money(12_345_678_900L),
                            ),
                        ),
                ),
            onSelectSlice = {},
            onDrillInto = {},
            onDrillUp = {},
            onViewTransactions = { _, _, _, _ -> },
        )
    }
}
