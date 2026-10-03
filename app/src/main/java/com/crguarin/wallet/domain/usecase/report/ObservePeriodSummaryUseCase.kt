package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.PeriodSummary
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Total, previous-period total, change percentage and average for [period].
 *
 * The average is per elapsed day in MONTH mode and per elapsed month in YEAR mode; "today" is
 * received as a parameter so the use case stays deterministic.
 */
class ObservePeriodSummaryUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            period: ReportPeriod,
            type: TransactionType,
            accountId: Long?,
            today: LocalDate,
        ): Flow<PeriodSummary> = TODO("Implemented by subagent A")
    }
