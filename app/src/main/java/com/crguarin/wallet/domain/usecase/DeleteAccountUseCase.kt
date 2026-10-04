package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import javax.inject.Inject

class DeleteAccountUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val recurringRepository: RecurringRepository,
    ) {
        suspend operator fun invoke(accountId: Long): DomainResult<Unit> {
            val account =
                accountRepository.getAccount(accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            val txCount = accountRepository.countTransactions(accountId)
            if (txCount > 0) {
                return DomainResult.Failure(DomainError.AccountHasTransactions)
            }

            if (recurringRepository.countByAccount(accountId) > 0) {
                return DomainResult.Failure(DomainError.AccountHasRecurring)
            }

            accountRepository.deleteAccount(account)
            return DomainResult.Success(Unit)
        }
    }
