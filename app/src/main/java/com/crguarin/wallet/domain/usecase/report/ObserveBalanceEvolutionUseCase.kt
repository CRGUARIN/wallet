package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.report.BalanceEvolution
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

/**
 * Total balance over time for [period]: daily closing balances (month mode) or month-end
 * balances (year mode), never after today.
 *
 * Contract stub: the body is implemented by the "datos y dominio" owner.
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
        ): Flow<BalanceEvolution> = flowOf(BalanceEvolution.EMPTY)
    }
