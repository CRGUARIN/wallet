package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.repository.AccountRepository
import java.util.Locale
import javax.inject.Inject

class SaveAccountUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(account: Account): DomainResult<Long> {
            val trimmedName = account.name.trim()
            if (trimmedName.isEmpty()) {
                return DomainResult.Failure(DomainError.NameEmpty)
            }
            if (trimmedName.length > 40) {
                return DomainResult.Failure(DomainError.NameTooLong)
            }
            if (account.initialBalance < Money.ZERO) {
                return DomainResult.Failure(DomainError.AmountInvalid)
            }

            val normalizedCandidate = trimmedName.lowercase(Locale.ROOT)
            val activeAccounts = accountRepository.getAllAccounts().filter { !it.isArchived && it.id != account.id }
            if (activeAccounts.any { it.name.trim().lowercase(Locale.ROOT) == normalizedCandidate }) {
                return DomainResult.Failure(DomainError.NameDuplicated)
            }

            val accountToSave = account.copy(name = trimmedName)
            val id = accountRepository.saveAccount(accountToSave)
            return DomainResult.Success(id)
        }
    }
