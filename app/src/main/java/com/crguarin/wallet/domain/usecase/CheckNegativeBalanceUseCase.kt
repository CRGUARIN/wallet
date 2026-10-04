package com.crguarin.wallet.domain.usecase

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.BalanceWarning
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Warning shown before saving [transaction].
 *
 * On a credit card the balance is normally negative, so instead of warning about that,
 * it warns with [BalanceWarning.OVER_CREDIT_LIMIT] when the resulting balance drops below
 * the negated credit limit (the debt exceeds the limit). Other accounts keep the
 * [BalanceWarning.NEGATIVE_BALANCE] rule.
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
            val accountWithBalance = accountRepository.getAccountWithBalance(transaction.accountId)
            val currentBalance = accountWithBalance?.balance ?: Money.ZERO
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
            val resultingBalance = baselineBalance - transaction.amount
            val account = accountWithBalance?.account
            if (account?.type == AccountType.CREDIT_CARD) {
                val creditLimit = account.creditLimit ?: return BalanceWarning.NONE
                return if (resultingBalance < -creditLimit) {
                    BalanceWarning.OVER_CREDIT_LIMIT
                } else {
                    BalanceWarning.NONE
                }
            }
            return if (resultingBalance.isNegative) {
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
