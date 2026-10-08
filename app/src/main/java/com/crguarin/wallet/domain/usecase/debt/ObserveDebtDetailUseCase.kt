package com.crguarin.wallet.domain.usecase.debt

import com.crguarin.wallet.domain.model.debt.DebtDetail
import com.crguarin.wallet.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * One debt with its payments, newest first. Emits null when the debt does not exist.
 */
class ObserveDebtDetailUseCase
    @Inject
    constructor(
        private val debtRepository: DebtRepository,
    ) {
        operator fun invoke(id: Long): Flow<DebtDetail?> =
            combine(
                debtRepository.observeSummary(id),
                debtRepository.observePayments(id),
            ) { summary, payments ->
                summary?.let { DebtDetail(summary = it, payments = payments) }
            }
    }
