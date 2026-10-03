package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.SpendingPace
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Day-by-day cumulative spending (or income) of [yearMonth] against the previous month.
 *
 * "today" is received as a parameter: the current month is cut at today, past months are complete.
 */
class ObserveSpendingPaceUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
    ) {
        operator fun invoke(
            yearMonth: YearMonth,
            type: TransactionType,
            accountId: Long?,
            today: LocalDate,
        ): Flow<SpendingPace> = TODO("Implemented by subagent A")
    }
