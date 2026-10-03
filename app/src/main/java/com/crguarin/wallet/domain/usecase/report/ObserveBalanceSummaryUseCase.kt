package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.report.BalanceSummary
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

/**
 * Income, expense, net, savings rate and net change of [period].
 *
 * Contract stub: the body is implemented by the "datos y dominio" owner.
 */
class ObserveBalanceSummaryUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            period: ReportPeriod,
            accountId: Long?,
            today: LocalDate,
        ): Flow<BalanceSummary> = flowOf(BalanceSummary.EMPTY)
    }
