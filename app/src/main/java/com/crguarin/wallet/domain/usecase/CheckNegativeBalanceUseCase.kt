package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Returns [BalanceWarning.NEGATIVE_BALANCE] when saving [transaction] as an EXPENSE would leave
 * its account with a negative balance. INCOME transactions never produce a negative balance.
 *
 * When editing, the effect of the original transaction on the current balance is
 * discounted first, so re-saving without changes does not warn. Changing the
 * account is also taken into account: only the effect on the new account matters.
 */
class CheckNegativeBalanceUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository,
    ) {
        suspend operator fun invoke(transaction: Transaction): BalanceWarning {
            if (transaction.type != TransactionType.EXPENSE && transaction.type != TransactionType.TRANSFER) {
                return BalanceWarning.NONE
            }
            val currentBalance =
                accountRepository.getAccountWithBalance(transaction.accountId)?.balance ?: Money.ZERO
            val baselineBalance =
                if (transaction.id != 0L) {
                    val existing = transactionRepository.getTransaction(transaction.id)
                    if (existing != null) {
                        currentBalance - oldEffectOn(existing, transaction.accountId)
                    } else {
                        currentBalance
                    }
                } else {
                    currentBalance
                }
            return if ((baselineBalance - transaction.amount).isNegative) {
                BalanceWarning.NEGATIVE_BALANCE
            } else {
                BalanceWarning.NONE
            }
        }

        private fun oldEffectOn(
            existing: Transaction,
            accountId: Long,
        ): Money =
            when {
                existing.type == TransactionType.INCOME && existing.accountId == accountId -> existing.amount
                existing.type == TransactionType.EXPENSE && existing.accountId == accountId -> -existing.amount
                existing.type == TransactionType.TRANSFER && existing.accountId == accountId -> -existing.amount
                existing.type == TransactionType.TRANSFER && existing.toAccountId == accountId -> existing.amount
                else -> Money.ZERO
            }
    }
