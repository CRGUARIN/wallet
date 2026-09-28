package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(accountId: Long): DomainResult<Unit> {
            val account =
                accountRepository.getAccount(accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            val txCount = accountRepository.countTransactions(accountId)
            if (txCount > 0) {
                return DomainResult.Failure(DomainError.AccountHasTransactions)
            }

            accountRepository.deleteAccount(account)
            return DomainResult.Success(Unit)
        }
    }
