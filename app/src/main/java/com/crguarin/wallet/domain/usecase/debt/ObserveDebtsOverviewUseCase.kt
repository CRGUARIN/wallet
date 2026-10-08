package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.debt.DebtDirection
import com.crguarin.wallet.domain.model.debt.DebtSummary
import com.crguarin.wallet.domain.model.debt.DebtsOverview
import com.crguarin.wallet.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Splits the debts into owed to me, I owe and settled, and totals the outstanding amounts of
 * the active ones. Active debts go by due date (no due date last), then start date, then id;
 * settled debts go newest first.
 */
class ObserveDebtsOverviewUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
    ) {
        operator fun invoke(): Flow<DebtsOverview> =
            debtRepository.observeSummaries().map { summaries ->
                val owedToMe =
                    summaries
                        .filter { !it.isSettled && it.debt.direction == DebtDirection.OWED_TO_ME }
                        .sortedWith(ACTIVE_ORDER)
                val iOwe =
                    summaries
                        .filter { !it.isSettled && it.debt.direction == DebtDirection.I_OWE }
                        .sortedWith(ACTIVE_ORDER)
                DebtsOverview(
                    owedToMe = owedToMe,
                    iOwe = iOwe,
                    settled = summaries.filter { it.isSettled }.sortedWith(SETTLED_ORDER),
                    totalOwedToMe = owedToMe.sumRemaining(),
                    totalIOwe = iOwe.sumRemaining(),
                )
            }

        private fun List<DebtSummary>.sumRemaining(): Money = fold(Money.ZERO) { acc, summary -> acc + summary.remaining }

        private companion object {
            val ACTIVE_ORDER: Comparator<DebtSummary> =
                compareBy<DebtSummary> { it.debt.dueDate ?: LocalDate.MAX }
                    .thenBy { it.debt.date }
                    .thenBy { it.debt.id }

            val SETTLED_ORDER: Comparator<DebtSummary> =
                compareByDescending<DebtSummary> { it.debt.date }
                    .thenByDescending { it.debt.id }
        }
    }
