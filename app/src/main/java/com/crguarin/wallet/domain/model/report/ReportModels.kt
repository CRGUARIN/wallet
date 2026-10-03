package com.crguarin.wallet.domain.model.report

import com.crguarin.wallet.core.money.Money
import java.time.LocalDate
import java.time.YearMonth

/**
 * One row of the aggregated category totals query: one leaf category (or a root category with
 * direct movements). Category columns are nullable because `category_id` may be null or the
 * referenced category may have been deleted.
 */
data class CategoryTotal(
    val categoryId: Long?,
    val name: String?,
    val systemKey: String?,
    val color: Int?,
    val icon: String?,
    val parentId: Long?,
    val parentName: String?,
    val parentSystemKey: String?,
    val parentColor: Int?,
    val parentIcon: String?,
    val total: Money,
    val transactionCount: Int,
)

/** Income and expense totals for a single day. */
data class DailyIncomeExpense(
    val date: LocalDate,
    val income: Money,
    val expense: Money,
)

/**
 * Identifies a slice of the donut chart. [Category] is a leaf category, [Uncategorized] groups
 * movements without a category, [Others] groups the smallest categories and [ParentDirect] groups
 * the movements assigned directly to a root category while drilled down.
 */
sealed interface SliceKey {
    data class Category(
        val categoryId: Long,
    ) : SliceKey

    data object Uncategorized : SliceKey

    data object Others : SliceKey

    data class ParentDirect(
        val parentId: Long,
    ) : SliceKey
}

/**
 * One slice of the category donut.
 *
 * [perMille] is the share of the breakdown total in per-mille (0..1000); all slices add up to 1000.
 */
data class CategorySlice(
    val key: SliceKey,
    val name: String?,
    val systemKey: String?,
    val color: Int?,
    val icon: String?,
    val amount: Money,
    val perMille: Int,
    val hasChildren: Boolean,
)

/** Category repartition for one period and type. [parentId] is null at root level. */
data class CategoryBreakdown(
    val total: Money,
    val slices: List<CategorySlice>,
    val parentId: Long?,
)

/** Income and expense totals for one month, used by the monthly evolution chart. */
data class MonthTotals(
    val yearMonth: YearMonth,
    val income: Money,
    val expense: Money,
)

/**
 * Summary of a period for one transaction type.
 *
 * [changePercent] is null when the previous period total is zero.
 */
data class PeriodSummary(
    val total: Money,
    val previousTotal: Money,
    val changePercent: Int?,
    val average: Money,
)

/**
 * Cumulative amount per day of one month, compared against the same days of the previous month.
 *
 * [current] has one entry per day up to [comparisonDay]; [previous] covers the whole previous
 * month. [currentAtDay] and [previousAtDay] are the amounts at [comparisonDay] (clamped to the
 * previous month length). [changePercent] is null when [previousAtDay] is zero.
 */
data class SpendingPace(
    val yearMonth: YearMonth,
    val current: List<Money>,
    val previous: List<Money>,
    val daysInMonth: Int,
    val comparisonDay: Int,
    val currentAtDay: Money,
    val previousAtDay: Money,
    val changePercent: Int?,
)
