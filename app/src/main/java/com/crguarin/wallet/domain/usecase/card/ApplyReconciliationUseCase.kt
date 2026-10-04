package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import java.time.LocalDate
import javax.inject.Inject

class ApplyReconciliationUseCase
    @Inject
    constructor() {
        suspend operator fun invoke(
            preview: ReconciliationPreview,
            dueDate: LocalDate,
            minimumPayment: Money?,
            statedInterestAndFees: Money?,
            adjustmentNote: String,
            forceNegativeAdjustment: Boolean,
        ): DomainResult<Long> = error("not implemented")
    }
