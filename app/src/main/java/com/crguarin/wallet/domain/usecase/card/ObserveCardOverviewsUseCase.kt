package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.domain.model.card.CardOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObserveCardOverviewsUseCase
    @Inject
    constructor() {
        operator fun invoke(): Flow<Map<Long, CardOverview>> = flowOf(emptyMap())
    }
