package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.repository.AccountRepository
import java.util.Locale
import javax.inject.Inject

class SaveAccountUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val timeProvider: TimeProvider,
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

            val allAccounts = accountRepository.getAllAccounts()
            val normalizedCandidate = trimmedName.lowercase(Locale.ROOT)
            val activeAccounts = allAccounts.filter { !it.isArchived && it.id != account.id }
            if (activeAccounts.any { it.name.trim().lowercase(Locale.ROOT) == normalizedCandidate }) {
                return DomainResult.Failure(DomainError.NameDuplicated)
            }

            // createdAt, sortOrder and isArchived are owned by the domain, not by the edit form.
            val accountToSave =
                if (account.id == 0L) {
                    account.copy(
                        name = trimmedName,
                        createdAt = timeProvider.now(),
                        sortOrder = (allAccounts.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                        isArchived = false,
                    )
                } else {
                    val existing =
                        allAccounts.firstOrNull { it.id == account.id }
                            ?: return DomainResult.Failure(DomainError.NotFound)
                    account.copy(
                        name = trimmedName,
                        createdAt = existing.createdAt,
                        sortOrder = existing.sortOrder,
                        isArchived = existing.isArchived,
                    )
                }
            val id = accountRepository.saveAccount(accountToSave)
            return DomainResult.Success(id)
        }
    }
