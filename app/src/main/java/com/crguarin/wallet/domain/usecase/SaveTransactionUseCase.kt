package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.DomainLimits
import com.crguarin.wallet.domain.model.SaveTransactionResult
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

class SaveTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
        private val checkNegativeBalanceUseCase: CheckNegativeBalanceUseCase,
    ) {
        suspend operator fun invoke(transaction: Transaction): DomainResult<SaveTransactionResult> {
            val validated =
                when (val result = validate(transaction)) {
                    is DomainResult.Success -> result.value
                    is DomainResult.Failure -> return result
                }

            val balanceWarning = checkNegativeBalanceUseCase(validated)

            val savedId = transactionRepository.saveTransaction(validated)
            return DomainResult.Success(SaveTransactionResult(savedId, balanceWarning))
        }

        /**
         * Runs every validation of [invoke] without saving, returning the transaction
         * ready to persist (with its timestamps set). Shared with reconciliation
         * adjustments so they go through the exact same rules.
         */
        suspend fun validate(transaction: Transaction): DomainResult<Transaction> {
            if (transaction.amount <= Money.ZERO || transaction.amount > Money.MAX) {
                return DomainResult.Failure(DomainError.AmountInvalid)
            }

            if (transaction.note != null && transaction.note.length > DomainLimits.NOTE_MAX_LENGTH) {
                return DomainResult.Failure(DomainError.NoteTooLong)
            }

            val maxAllowedDate = DomainLimits.maxTransactionDate(timeProvider.today())
            if (transaction.date > maxAllowedDate) {
                return DomainResult.Failure(DomainError.DateOutOfRange)
            }

            var category: Category? = null
            var toAccount: Account? = null

            when (transaction.type) {
                TransactionType.INCOME, TransactionType.EXPENSE -> {
                    if (transaction.toAccountId != null) {
                        return DomainResult.Failure(DomainError.InvalidTransaction)
                    }
                    if (transaction.categoryId == null) {
                        return DomainResult.Failure(DomainError.CategoryRequired)
                    }
                    val foundCategory =
                        categoryRepository.getCategory(transaction.categoryId)
                            ?: return DomainResult.Failure(DomainError.NotFound)

                    val expectedCategoryType =
                        if (transaction.type == TransactionType.INCOME) {
                            CategoryType.INCOME
                        } else {
                            CategoryType.EXPENSE
                        }

                    if (foundCategory.type != expectedCategoryType) {
                        return DomainResult.Failure(DomainError.CategoryTypeMismatch)
                    }
                    category = foundCategory
                }

                TransactionType.TRANSFER -> {
                    if (transaction.categoryId != null) {
                        return DomainResult.Failure(DomainError.InvalidTransaction)
                    }
                    if (transaction.toAccountId == null) {
                        return DomainResult.Failure(DomainError.InvalidTransaction)
                    }
                    if (transaction.toAccountId == transaction.accountId) {
                        return DomainResult.Failure(DomainError.SameAccountTransfer)
                    }
                    toAccount =
                        accountRepository.getAccount(transaction.toAccountId)
                            ?: return DomainResult.Failure(DomainError.NotFound)
                }
            }

            val account =
                accountRepository.getAccount(transaction.accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)

            if (!areInstallmentsValid(transaction, account)) {
                return DomainResult.Failure(DomainError.InvalidTransaction)
            }

            val isNew = transaction.id == 0L
            val existing =
                if (!isNew) {
                    transactionRepository.getTransaction(transaction.id)
                        ?: return DomainResult.Failure(DomainError.NotFound)
                } else {
                    null
                }

            if (isNew) {
                if (account.isArchived) {
                    return DomainResult.Failure(DomainError.AccountArchived)
                }
                if (toAccount != null && toAccount.isArchived) {
                    return DomainResult.Failure(DomainError.AccountArchived)
                }
                if (category != null && category.isArchived) {
                    return DomainResult.Failure(DomainError.CategoryArchived)
                }
            } else {
                val oldTx = checkNotNull(existing)
                if (account.isArchived && transaction.accountId != oldTx.accountId) {
                    return DomainResult.Failure(DomainError.AccountArchived)
                }
                if (toAccount != null && toAccount.isArchived && transaction.toAccountId != oldTx.toAccountId) {
                    return DomainResult.Failure(DomainError.AccountArchived)
                }
                if (category != null && category.isArchived && transaction.categoryId != oldTx.categoryId) {
                    return DomainResult.Failure(DomainError.CategoryArchived)
                }
            }

            val now = timeProvider.now()
            val toSave =
                if (isNew) {
                    transaction.copy(createdAt = now, updatedAt = now)
                } else {
                    val oldTx = checkNotNull(existing)
                    transaction.copy(createdAt = oldTx.createdAt, updatedAt = now)
                }
            return DomainResult.Success(toSave)
        }

        private fun areInstallmentsValid(
            transaction: Transaction,
            account: Account,
        ): Boolean {
            val installments = transaction.installments ?: return true
            return transaction.type == TransactionType.EXPENSE &&
                account.type == AccountType.CREDIT_CARD &&
                installments in 1..MAX_INSTALLMENTS
        }

        companion object {
            const val MAX_INSTALLMENTS = 48
        }
    }
