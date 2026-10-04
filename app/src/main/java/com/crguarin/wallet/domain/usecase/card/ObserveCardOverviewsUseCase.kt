package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.card.CardOverview
import com.crguarin.wallet.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
                // Balances change with every transaction; only a change in the set of cards
                // needs new per-card subscriptions.
                .map { accounts ->
                    accounts
                        .filter { it.account.type == AccountType.CREDIT_CARD }
                        .map { it.account.id }
                }.distinctUntilChanged()
                .flatMapLatest { cardIds ->
                    if (cardIds.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        combine(cardIds.map { observeCardOverviewUseCase(it) }) { overviews ->
                            overviews.filterNotNull().associateBy { it.accountId }
                        }
                    }
                }
    }
