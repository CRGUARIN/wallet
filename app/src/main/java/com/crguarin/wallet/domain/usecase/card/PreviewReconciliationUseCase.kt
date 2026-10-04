package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.card.CardMath
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CardStatementRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Compares the bank statement figures with the debt recorded in the app at [closingDate].
 */
class PreviewReconciliationUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val cardStatementRepository: CardStatementRepository,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(
            accountId: Long,
            closingDate: LocalDate,
            statementBalance: Money,
            statedInterestAndFees: Money?,
        ): DomainResult<ReconciliationPreview> {
            val account =
                accountRepository.getAccount(accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (account.type != AccountType.CREDIT_CARD) {
                return DomainResult.Failure(DomainError.NotFound)
            }
            if (statementBalance < Money.ZERO) {
                return DomainResult.Failure(DomainError.AmountInvalid)
            }
            if (closingDate > timeProvider.today()) {
                return DomainResult.Failure(DomainError.DateOutOfRange)
            }
            val effect = cardStatementRepository.getAccountEffectUpTo(accountId, closingDate)
            val appDebt = CardMath.appDebtAt(account.initialBalance, effect)
            val difference = statementBalance - appDebt
            return DomainResult.Success(
                ReconciliationPreview(
                    accountId = accountId,
                    closingDate = closingDate,
                    appDebt = appDebt,
                    statementBalance = statementBalance,
                    difference = difference,
                    interestMismatch =
                        statedInterestAndFees != null &&
                            difference.isPositive &&
                            difference != statedInterestAndFees,
                ),
            )
        }
    }
