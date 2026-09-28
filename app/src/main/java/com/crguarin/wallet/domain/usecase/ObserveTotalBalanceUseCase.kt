package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveTotalBalanceUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        operator fun invoke(): Flow<Money> =
            accountRepository.observeAccountsWithBalance(includeArchived = false).map { accounts ->
                accounts
                    .filter { it.account.includeInTotal && !it.account.isArchived }
                    .fold(Money.ZERO) { acc, accountWithBalance -> acc + accountWithBalance.balance }
            }
    }
