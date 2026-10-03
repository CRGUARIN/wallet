package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.CategoryTotal
import com.crguarin.wallet.domain.model.report.ReportMath
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Category repartition of [type] movements in [period]. With `parentId == null` groups by root
 * category; otherwise drills down into the subcategories of `parentId`.
 *
 * At most [maxSlices] slices are returned: the smallest ones are grouped into an "Others" slice.
 */
class ObserveCategoryBreakdownUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            period: ReportPeriod,
            type: TransactionType,
            accountId: Long?,
            parentId: Long? = null,
            maxSlices: Int = 6,
        ): Flow<CategoryBreakdown> =
            reportRepository
                .observeCategoryTotals(period.start, period.end, type, accountId)
                .map { rows ->
                    if (parentId == null) {
                        rootBreakdown(rows, maxSlices)
                    } else {
                        drillDownBreakdown(rows, parentId, maxSlices)
                    }
                }

        private fun rootBreakdown(
            rows: List<CategoryTotal>,
            maxSlices: Int,
        ): CategoryBreakdown {
            val total = Money(rows.sumOf { it.total.minorUnits })
            val byRoot = LinkedHashMap<Long?, MutableList<CategoryTotal>>()
            rows.forEach { row ->
                val rootId = row.parentId ?: row.categoryId
                byRoot.getOrPut(rootId) { mutableListOf() }.add(row)
            }
            val slices =
                byRoot
                    .map { (rootId, rootRows) ->
                        val amount = Money(rootRows.sumOf { it.total.minorUnits })
                        if (rootId == null) {
                            CategorySlice(
                                key = SliceKey.Uncategorized,
                                name = null,
                                systemKey = null,
                                color = null,
                                icon = null,
                                amount = amount,
                                perMille = 0,
                                hasChildren = false,
                            )
                        } else {
                            // All rows of a root share its fields: a child row carries them in
                            // parent*, a direct root row in its own columns.
                            val source = rootRows.first()
                            CategorySlice(
                                key = SliceKey.Category(rootId),
                                name = source.parentName ?: source.name,
                                systemKey = source.parentSystemKey ?: source.systemKey,
                                color = source.parentColor ?: source.color,
                                icon = source.parentIcon ?: source.icon,
                                amount = amount,
                                perMille = 0,
                                hasChildren = rootRows.any { it.parentId != null },
                            )
                        }
                    }.sortedWith(sliceComparator)
            return CategoryBreakdown(
                total = total,
                slices = withPerMille(applyMaxSlices(slices, maxSlices)),
                parentId = null,
            )
        }

        private fun drillDownBreakdown(
            rows: List<CategoryTotal>,
            parentId: Long,
            maxSlices: Int,
        ): CategoryBreakdown {
            val relevant = rows.filter { (it.parentId ?: it.categoryId) == parentId }
            val total = Money(relevant.sumOf { it.total.minorUnits })
            val childSlices =
                relevant.mapNotNull { row ->
                    val childId = row.categoryId
                    if (row.parentId == parentId && childId != null) {
                        CategorySlice(
                            key = SliceKey.Category(childId),
                            name = row.name,
                            systemKey = row.systemKey,
                            color = row.color,
                            icon = row.icon,
                            amount = row.total,
                            perMille = 0,
                            hasChildren = false,
                        )
                    } else {
                        null
                    }
                }
            val directRows = relevant.filter { it.parentId == null && it.categoryId == parentId }
            val directSlice =
                directRows.firstOrNull()?.let { source ->
                    CategorySlice(
                        key = SliceKey.ParentDirect(parentId),
                        name = source.name,
                        systemKey = source.systemKey,
                        color = source.color,
                        icon = source.icon,
                        amount = Money(directRows.sumOf { it.total.minorUnits }),
                        perMille = 0,
                        hasChildren = false,
                    )
                }
            val sorted = (childSlices + listOfNotNull(directSlice)).sortedWith(sliceComparator)
            return CategoryBreakdown(
                total = total,
                slices = withPerMille(applyMaxSlices(sorted, maxSlices)),
                parentId = parentId,
            )
        }

        /**
         * Keeps the `maxSlices - 1` largest slices and merges the rest into a single "Others"
         * slice. A single leftover is never merged into an "Others" slice of its own.
         */
        private fun applyMaxSlices(
            slices: List<CategorySlice>,
            maxSlices: Int,
        ): List<CategorySlice> {
            if (slices.size <= maxSlices) return slices
            val keep = maxSlices - 1
            if (keep <= 0) return listOf(othersSlice(slices))
            val rest = slices.drop(keep)
            return if (rest.size == 1) slices else slices.take(keep) + othersSlice(rest)
        }

        private fun othersSlice(rest: List<CategorySlice>): CategorySlice =
            CategorySlice(
                key = SliceKey.Others,
                name = null,
                systemKey = null,
                color = null,
                icon = null,
                amount = Money(rest.sumOf { it.amount.minorUnits }),
                perMille = 0,
                hasChildren = false,
            )

        private fun withPerMille(slices: List<CategorySlice>): List<CategorySlice> {
            val shares = ReportMath.perMilleShares(slices.map { it.amount })
            return slices.mapIndexed { index, slice -> slice.copy(perMille = shares[index]) }
        }
    }

/** Largest amount first; ties break by name and then by id, so the order is stable. */
private val sliceComparator: Comparator<CategorySlice> =
    compareByDescending<CategorySlice> { it.amount.minorUnits }
        .thenBy { it.name.orEmpty() }
        .thenBy { sliceOrderId(it.key) }

private fun sliceOrderId(key: SliceKey): Long =
    when (key) {
        is SliceKey.Category -> key.categoryId
        is SliceKey.ParentDirect -> key.parentId
        SliceKey.Uncategorized -> Long.MIN_VALUE
        SliceKey.Others -> Long.MAX_VALUE
    }
