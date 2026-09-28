package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.AccountRepository
import javax.inject.Inject

class ReorderAccountsUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(accountIds: List<Long>): DomainResult<Unit> {
            val accountOrders = accountIds.mapIndexed { index, id -> id to index }
            accountRepository.updateSortOrders(accountOrders)
            return DomainResult.Success(Unit)
        }
    }
