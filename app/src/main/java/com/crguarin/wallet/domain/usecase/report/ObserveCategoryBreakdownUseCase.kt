package com.crguarin.wallet.domain.usecase.report

import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.report.CategoryBreakdown
import com.crguarin.wallet.domain.model.report.ReportPeriod
import com.crguarin.wallet.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
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
        ): Flow<CategoryBreakdown> = TODO("Implemented by subagent A")
    }
