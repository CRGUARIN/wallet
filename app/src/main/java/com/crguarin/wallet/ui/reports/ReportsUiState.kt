package com.crguarin.wallet.ui.reports

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.model.report.SpendingPace
import java.time.LocalDate
import java.time.YearMonth

/**
 * Immutable state of the Reports screen.
 *
 * [type] is always INCOME or EXPENSE (never TRANSFER). [yearMonth] is the selected month; in YEAR
 * mode its year is the selected period. [pace] is null in YEAR mode.
 */
data class ReportsUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val mode: ReportMode = ReportMode.MONTH,
    val yearMonth: YearMonth = YearMonth.now(),
    /** Today's date, injected by the ViewModel so month dimming and pace phrases are testable. */
    val today: LocalDate = LocalDate.now(),
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: Long? = null,
    val selectedAccountName: String? = null,
    val accounts: List<AccountWithBalance> = emptyList(),
    val showAccountSheet: Boolean = false,
    val summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO, null, Money.ZERO),
    val breakdown: CategoryBreakdown = CategoryBreakdown(total = Money.ZERO, slices = emptyList(), parentId = null),
    val drillDownParent: CategorySlice? = null,
    val selectedSliceKey: SliceKey? = null,
    val monthlyTotals: List<MonthTotals> = emptyList(),
    val pace: SpendingPace? = null,
    val canGoNext: Boolean = false,
    /** True mientras se cargan los datos de una selección nueva; los datos mostrados son de la selección anterior. */
    val isRefreshing: Boolean = false,
) {
    /** No income and no expenses in the whole monthly evolution range. */
    val isEmpty: Boolean
        get() = !isLoading && !hasError && monthlyTotals.all { it.income.isZero && it.expense.isZero }

    /** The selected period has no movements of [type]: only the donut card is empty. */
    val hasNoDataOfType: Boolean
        get() = breakdown.total.isZero
}
