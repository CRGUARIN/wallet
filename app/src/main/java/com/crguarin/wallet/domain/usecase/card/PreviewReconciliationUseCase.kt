package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import java.time.LocalDate
import javax.inject.Inject

class PreviewReconciliationUseCase
    @Inject
    constructor() {
        suspend operator fun invoke(
            accountId: Long,
            closingDate: LocalDate,
            statementBalance: Money,
            statedInterestAndFees: Money?,
        ): DomainResult<ReconciliationPreview> = error("not implemented")
    }
