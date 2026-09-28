package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.domain.model.AccountWithBalance
import com.crguarin.wallet.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAccountsUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        operator fun invoke(includeArchived: Boolean = false): Flow<List<AccountWithBalance>> =
            accountRepository.observeAccountsWithBalance(includeArchived)
    }
