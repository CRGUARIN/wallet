package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Overviews of every non-archived credit card, keyed by account id.
 */
class ObserveCardOverviewsUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val observeCardOverviewUseCase: ObserveCardOverviewUseCase,
    ) {
        operator fun invoke(): Flow<Map<Long, CardOverview>> =
            accountRepository
                .observeAccountsWithBalance(includeArchived = false)
                .flatMapLatest { accounts ->
                    val cardIds =
                        accounts
                            .filter { it.account.type == AccountType.CREDIT_CARD }
                            .map { it.account.id }
                    if (cardIds.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        combine(cardIds.map { observeCardOverviewUseCase(it) }) { overviews ->
                            overviews.filterNotNull().associateBy { it.accountId }
                        }
                    }
                }
    }
