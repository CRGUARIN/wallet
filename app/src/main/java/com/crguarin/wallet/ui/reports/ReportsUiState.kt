package com.crguarin.wallet.ui.reports

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalanceSummary
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.CategorySlice
import com.crguarin.wallet.domain.model.report.MonthTotals
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportFocus
import com.crguarin.wallet.domain.model.report.ReportMode
import com.crguarin.wallet.domain.model.report.SliceKey
import com.crguarin.wallet.domain.model.report.SpendingPace
import java.time.LocalDate
import java.time.YearMonth

/**
 * Immutable state of the Reports screen.
 *
 * [focus] selects expenses, income or the balance view; [type] derives from it and is always
 * INCOME or EXPENSE (never TRANSFER). [yearMonth] is the selected month; in YEAR mode its year is
 * the selected period. [pace] is null in YEAR mode and in the BALANCE focus.
 */
data class ReportsUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val mode: ReportMode = ReportMode.MONTH,
    val yearMonth: YearMonth = YearMonth.now(),
    /** Today's date, injected by the ViewModel so month dimming and pace phrases are testable. */
    val today: LocalDate = LocalDate.now(),
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
    val focus: ReportFocus = ReportFocus.EXPENSE,
    val balanceSummary: BalanceSummary = BalanceSummary.EMPTY,
    val balanceEvolution: BalanceEvolution? = null,
) {
    /**
     * Temporary compatibility for callers that still pass the pre-Balance `type` argument
     * (ReportsScreen preview, ReportsScreenTest). `type` maps to its [ReportFocus]. The owner of
     * those files removes the `type` argument; then this constructor is deleted.
     *
     * TODO(tarea-14): delete after merging the "tarjetas y pantalla" branch.
     */
    constructor(
        isLoading: Boolean = true,
        hasError: Boolean = false,
        mode: ReportMode = ReportMode.MONTH,
        yearMonth: YearMonth = YearMonth.now(),
        today: LocalDate = LocalDate.now(),
        type: TransactionType,
        accountId: Long? = null,
        selectedAccountName: String? = null,
        accounts: List<AccountWithBalance> = emptyList(),
        showAccountSheet: Boolean = false,
        summary: PeriodSummary = PeriodSummary(Money.ZERO, Money.ZERO, null, Money.ZERO),
        breakdown: CategoryBreakdown = CategoryBreakdown(total = Money.ZERO, slices = emptyList(), parentId = null),
        drillDownParent: CategorySlice? = null,
        selectedSliceKey: SliceKey? = null,
        monthlyTotals: List<MonthTotals> = emptyList(),
        pace: SpendingPace? = null,
        canGoNext: Boolean = false,
        isRefreshing: Boolean = false,
    ) : this(
        isLoading = isLoading,
        hasError = hasError,
        mode = mode,
        yearMonth = yearMonth,
        today = today,
        accountId = accountId,
        selectedAccountName = selectedAccountName,
        accounts = accounts,
        showAccountSheet = showAccountSheet,
        summary = summary,
        breakdown = breakdown,
        drillDownParent = drillDownParent,
        selectedSliceKey = selectedSliceKey,
        monthlyTotals = monthlyTotals,
        pace = pace,
        canGoNext = canGoNext,
        isRefreshing = isRefreshing,
        focus =
            when (type) {
                TransactionType.EXPENSE -> ReportFocus.EXPENSE
                TransactionType.INCOME -> ReportFocus.INCOME
                TransactionType.TRANSFER -> ReportFocus.EXPENSE
            },
    )

    /** Transaction type shown by [focus]; EXPENSE when the focus is BALANCE. */
    val type: TransactionType
        get() = focus.transactionType ?: TransactionType.EXPENSE

    /**
     * No movements to show: no income and no expenses in the whole monthly evolution range and, in
     * the BALANCE focus, a balance evolution without movements either (every point equals the
     * opening and the opening is zero). A flat non-zero line is NOT empty: the balance is shown.
     */
    val isEmpty: Boolean
        get() {
            if (isLoading || hasError) return false
            if (!monthlyTotals.all { it.income.isZero && it.expense.isZero }) return false
            if (focus != ReportFocus.BALANCE) return true
            val evolution = balanceEvolution ?: return true
            return evolution.opening.isZero && evolution.points.all { it.balance == evolution.opening }
        }

    /** The selected period has no movements of [type]: only the donut card is empty. */
    val hasNoDataOfType: Boolean
        get() = breakdown.total.isZero
}
