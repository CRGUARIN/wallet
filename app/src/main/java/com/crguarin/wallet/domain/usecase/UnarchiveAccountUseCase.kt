package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.normalizedName
import com.crguarin.wallet.domain.repository.AccountRepository
import javax.inject.Inject

class UnarchiveAccountUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(accountId: Long): DomainResult<Unit> {
            val account =
                accountRepository.getAccount(accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            val normalizedName = account.name.normalizedName()
            val activeAccounts = accountRepository.getAllAccounts().filter { !it.isArchived && it.id != accountId }
            if (activeAccounts.any { it.name.normalizedName() == normalizedName }) {
                return DomainResult.Failure(DomainError.NameDuplicated)
            }

            accountRepository.saveAccount(account.copy(isArchived = false))
            return DomainResult.Success(Unit)
        }
    }
