package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.model.report.BalanceDeltas
import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.BalanceGranularity
import com.crguarin.wallet.domain.model.report.BalancePoint
import com.crguarin.wallet.domain.model.report.ReportMath
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Total balance over time for [period]: daily closing balances (month mode) or month-end
 * balances (year mode), never after today.
 *
 * Without an [accountId] filter the account set is the same one used by the Home total balance
 * (accounts with `includeInTotal && !isArchived`); with a filter it is that single account, even
 * when archived or excluded from the total. The balance of the set at a date D is the sum of the
 * account initials plus every signed effect dated on or before D, so a future-dated transaction
 * does not count until its date: the last point may differ from the Home total balance, which
 * includes future transactions.
 */
class ObserveBalanceEvolutionUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
        private val accountRepository: AccountRepository,
    ) {
        operator fun invoke(
            period: ReportPeriod,
            accountId: Long?,
            today: LocalDate,
        ): Flow<BalanceEvolution> {
            val granularity =
                when (period) {
                    is ReportPeriod.Month -> BalanceGranularity.DAILY
                    is ReportPeriod.Year -> BalanceGranularity.MONTHLY
                }
            // The ViewModel never navigates to the future; guard anyway.
            if (period.start.isAfter(today)) {
                return flowOf(BalanceEvolution(granularity, Money.ZERO, emptyList()))
            }
            val lastDate = minOf(period.end, today)
            return accountRepository
                .observeAccountsWithBalance(includeArchived = true)
                .map { accounts -> selectAccounts(accounts, accountId) }
                .distinctUntilChanged()
                .flatMapLatest { selection ->
                    if (selection.ids.isEmpty()) {
                        flowOf(BalanceEvolution(granularity, Money.ZERO, emptyList()))
                    } else {
                        reportRepository
                            .observeBalanceDeltas(selection.ids, period.start, lastDate)
                            .map { deltas -> toEvolution(granularity, period, lastDate, selection, deltas) }
                    }
                }
        }

        private fun selectAccounts(
            accounts: List<AccountWithBalance>,
            accountId: Long?,
        ): AccountSelection =
            if (accountId != null) {
                val match = accounts.singleOrNull { it.account.id == accountId }
                if (match == null) {
                    AccountSelection(emptySet(), Money.ZERO)
                } else {
                    AccountSelection(setOf(accountId), match.account.initialBalance)
                }
            } else {
                val included = accounts.filter { it.account.includeInTotal && !it.account.isArchived }
                AccountSelection(
                    ids = included.map { it.account.id }.toSet(),
                    initialsSum = Money(included.sumOf { it.account.initialBalance.minorUnits }),
                )
            }

        private fun toEvolution(
            granularity: BalanceGranularity,
            period: ReportPeriod,
            lastDate: LocalDate,
            selection: AccountSelection,
            deltas: BalanceDeltas,
        ): BalanceEvolution {
            val opening = selection.initialsSum + deltas.openingDelta
            val dates = pointDates(period, lastDate)
            val balances = ReportMath.balancePoints(opening, deltas.daily, dates)
            return BalanceEvolution(
                granularity = granularity,
                opening = opening,
                points = dates.zip(balances) { date, balance -> BalancePoint(date, balance) },
            )
        }

        private fun pointDates(
            period: ReportPeriod,
            lastDate: LocalDate,
        ): List<LocalDate> =
            when (period) {
                is ReportPeriod.Month ->
                    generateSequence(period.start) { date ->
                        date.plusDays(1).takeIf { !it.isAfter(lastDate) }
                    }.toList()
                is ReportPeriod.Year ->
                    (1..lastDate.monthValue).map { month ->
                        if (month == lastDate.monthValue) {
                            lastDate
                        } else {
                            YearMonth.of(period.year.value, month).atEndOfMonth()
                        }
                    }
            }

        /**
         * Account set of the evolution: [ids] are queried for balance deltas and [initialsSum] is
         * added to the opening. Equality on both fields drives the [distinctUntilChanged] upstream,
         * so balance-only updates do not re-query the deltas.
         */
        private data class AccountSelection(
            val ids: Set<Long>,
            val initialsSum: Money,
        )
    }
