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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
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
import com.crguarin.wallet.ui.components.PebbleAvatar
import com.crguarin.wallet.ui.components.SectionCard
import com.crguarin.wallet.ui.components.categoryDisplayName
import com.crguarin.wallet.ui.reports.charts.DonutChart
import com.crguarin.wallet.ui.theme.WalletTheme
import com.crguarin.wallet.ui.theme.walletSpacing
import com.crguarin.wallet.ui.theme.walletTypography
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth

/**
 * Category donut with its legend. Tapping a slice selects it. Tapping a legend row drills into the
 * subcategories when it has children, or opens its movements with the report filters in month mode.
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
    SectionCard(modifier = modifier) {
        Column {
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
                val uncategorizedColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        // "(general)" and "Others" have no category to navigate to, so they only select.
                        // "(general)" must not open Movements by parent id: that would match every subcategory.
                        val category = slice.key as? SliceKey.Category
                        val drills = category != null && slice.hasChildren
                        val showsTransactions = category != null && uiState.mode == ReportMode.MONTH
                        LegendRow(
                            slice = slice,
                            selected = slice.key == uiState.selectedSliceKey,
                            actionsEnabled = !refreshing,
                            navigates = drills || showsTransactions,
                            clickLabel =
                                when {
                                    drills -> stringResource(R.string.reports_view_subcategories)
                                    showsTransactions -> stringResource(R.string.reports_view_transactions)
                                    else -> null
                                },
                            onClick = {
                                when {
                                    category != null && drills -> onDrillInto(category.categoryId)
                                    category != null && showsTransactions ->
                                        onViewTransactions(
                                            category.categoryId,
                                            uiState.accountId,
                                            uiState.type,
                                            uiState.yearMonth,
                                        )
                                    else -> onSelectSlice(slice.key)
                                }
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

/**
 * One category of the legend. The name goes on the first line with the amount and may take two
 * lines, so it is never cut ("Vivi…"); the bar goes on the second line, with a chevron when the row
 * navigates. The whole row is the touch target and [clickLabel] names its action for screen readers.
 */
@Composable
private fun LegendRow(
    slice: CategorySlice,
    selected: Boolean,
    actionsEnabled: Boolean,
    navigates: Boolean,
    clickLabel: String?,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.walletSpacing
    val color =
        ReportsChartMapper.sliceColor(
            slice = slice,
            othersColor = MaterialTheme.colorScheme.outline,
            uncategorizedColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                .clickable(enabled = actionsEnabled, onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = spacing.s, vertical = spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            PebbleAvatar(
                icon = IconCatalog.get(slice.icon ?: IconCatalog.DEFAULT_ICON_KEY),
                color = color,
                size = LEGEND_AVATAR_SIZE,
            )
            Spacer(modifier = Modifier.width(spacing.m))
            Text(
                text = sliceLabel(slice),
                style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(top = spacing.xs),
            )
            Spacer(modifier = Modifier.width(spacing.s))
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    amount = slice.amount,
                    style = MaterialTheme.walletTypography.rowAmount,
                    sign = MoneySign.NONE,
                )
                Text(
                    text = perMillePercent(slice.perMille),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = LEGEND_ACTION_SIZE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(LEGEND_AVATAR_SIZE + spacing.m))
            ProgressBar(
                perMille = slice.perMille,
                color = color,
                modifier = Modifier.weight(1f),
            )
            if (navigates) {
                Spacer(modifier = Modifier.width(spacing.xs))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(LEGEND_ACTION_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val LEGEND_AVATAR_SIZE = 36.dp
private val LEGEND_ACTION_SIZE = 20.dp

@Composable
private fun ProgressBar(
    perMille: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth((perMille / 1000f).coerceIn(0f, 1f))
                    .height(6.dp)
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
