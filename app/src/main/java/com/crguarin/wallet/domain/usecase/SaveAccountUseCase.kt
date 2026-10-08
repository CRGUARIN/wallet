package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.normalizedName
import com.crguarin.wallet.domain.repository.AccountRepository
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
            if (trimmedName.length > DomainLimits.NAME_MAX_LENGTH) {
                return DomainResult.Failure(DomainError.NameTooLong)
            }

            val allAccounts = accountRepository.getAllAccounts()
            val existing =
                if (account.id != 0L) {
                    allAccounts.firstOrNull { it.id == account.id }
                        ?: return DomainResult.Failure(DomainError.NotFound)
                } else {
                    null
                }
            if (existing != null && crossesCardBoundary(existing.type, account.type)) {
                if (accountRepository.countTransactions(account.id) > 0) {
                    return DomainResult.Failure(DomainError.AccountTypeChangeNotAllowed)
                }
            }

            val normalizedAccount =
                if (account.type == AccountType.CREDIT_CARD) {
                    if (account.creditLimit == null || account.creditLimit <= Money.ZERO) {
                        return DomainResult.Failure(DomainError.AmountInvalid)
                    }
                    if (!isValidDay(account.statementDay) || !isValidDay(account.paymentDueDay)) {
                        return DomainResult.Failure(DomainError.CardDayInvalid)
                    }
                    // Cards may hold a negative initialBalance (current debt) or a positive one.
                    account.copy(fare = null)
                } else {
                    if (account.initialBalance < Money.ZERO) {
                        return DomainResult.Failure(DomainError.AmountInvalid)
                    }
                    if (account.type == AccountType.TRANSIT_CARD) {
                        val fare = account.fare
                        if (fare == null || fare <= Money.ZERO || fare > Money.MAX) {
                            return DomainResult.Failure(DomainError.FareInvalid)
                        }
                        account.copy(creditLimit = null, statementDay = null, paymentDueDay = null)
                    } else {
                        account.copy(creditLimit = null, statementDay = null, paymentDueDay = null, fare = null)
                    }
                }

            val normalizedCandidate = trimmedName.normalizedName()
            val activeAccounts = allAccounts.filter { !it.isArchived && it.id != account.id }
            if (activeAccounts.any { it.name.normalizedName() == normalizedCandidate }) {
                return DomainResult.Failure(DomainError.NameDuplicated)
            }
            // A color belongs to one active account. Editing an account that already has this color is allowed.
            if (existing?.color != account.color && activeAccounts.any { it.color == account.color }) {
                return DomainResult.Failure(DomainError.ColorInUse)
            }

            // createdAt, sortOrder and isArchived are owned by the domain, not by the edit form.
            val accountToSave =
                if (account.id == 0L) {
                    normalizedAccount.copy(
                        name = trimmedName,
                        createdAt = timeProvider.now(),
                        sortOrder = (allAccounts.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                        isArchived = false,
                    )
                } else {
                    val stored = checkNotNull(existing)
                    normalizedAccount.copy(
                        name = trimmedName,
                        createdAt = stored.createdAt,
                        sortOrder = stored.sortOrder,
                        isArchived = stored.isArchived,
                    )
                }
            val id = accountRepository.saveAccount(accountToSave)
            return DomainResult.Success(id)
        }

        private fun crossesCardBoundary(
            oldType: AccountType,
            newType: AccountType,
        ): Boolean = (oldType == AccountType.CREDIT_CARD) != (newType == AccountType.CREDIT_CARD)

        private fun isValidDay(day: Int?): Boolean = day != null && day in 1..31
    }
