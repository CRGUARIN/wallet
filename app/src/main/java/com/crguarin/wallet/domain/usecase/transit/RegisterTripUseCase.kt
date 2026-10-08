package com.crguarin.wallet.domain.usecase.transit

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainError
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.SaveTransactionResult
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.transit.TransitMath
import com.crguarin.wallet.domain.model.transit.TransitSystemCategories
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.TransactionRunner
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import javax.inject.Inject

/**
 * Registers one trip on a transit card: an expense for the card's fare, dated today, in the
 * transport category. Top-ups are transfers and are saved by the regular transfer flow.
 *
 * Credit rule (Cívica): a trip is allowed while the balance is not negative. A balance of zero or
 * below the fare still lets one trip through on credit, so the balance goes negative. While the
 * balance is negative, the trip is rejected with [DomainError.TransitCreditUsed] until a top-up.
 *
 * The balance check and the save run in one [TransactionRunner] block, so two concurrent calls
 * cannot both see a non-negative balance and both go through on credit.
 */
class RegisterTripUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val saveTransactionUseCase: SaveTransactionUseCase,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
    ) {
        suspend operator fun invoke(accountId: Long): DomainResult<SaveTransactionResult> =
            transactionRunner.run { checkAndSaveTrip(accountId) }

        private suspend fun checkAndSaveTrip(accountId: Long): DomainResult<SaveTransactionResult> {
            val account =
                accountRepository.getAccount(accountId)
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (account.type != AccountType.TRANSIT_CARD) {
                return DomainResult.Failure(DomainError.InvalidTransaction)
            }
            if (account.isArchived) {
                return DomainResult.Failure(DomainError.AccountArchived)
            }
            val fare = account.fare
            if (fare == null || fare <= Money.ZERO) {
                return DomainResult.Failure(DomainError.FareInvalid)
            }
            val balance =
                accountRepository.getAccountWithBalance(accountId)?.balance
                    ?: return DomainResult.Failure(DomainError.NotFound)
            if (!TransitMath.canTravel(balance)) {
                return DomainResult.Failure(DomainError.TransitCreditUsed)
            }

            val categoryId = tripCategoryId()
            val now = timeProvider.now()
            val trip =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = fare,
                    accountId = accountId,
                    categoryId = categoryId,
                    date = timeProvider.today(),
                    note = null,
                    createdAt = now,
                    updatedAt = now,
                )
            return saveTransactionUseCase(trip)
        }

        private suspend fun tripCategoryId(): Long {
            val expenses = categoryRepository.getAllCategories(CategoryType.EXPENSE)
            return activeWithKey(expenses, TransitSystemCategories.TRIP_KEY)?.id
                ?: activeWithKey(expenses, TransitSystemCategories.FALLBACK_KEY)?.id
                ?: categoryRepository.ensureSystemCategory(
                    systemKey = TransitSystemCategories.FALLBACK_KEY,
                    type = CategoryType.EXPENSE,
                    color = TransitSystemCategories.FALLBACK_COLOR,
                    icon = TransitSystemCategories.FALLBACK_ICON,
                )
        }

        private fun activeWithKey(
            categories: List<Category>,
            systemKey: String,
        ): Category? = categories.firstOrNull { it.systemKey == systemKey && !it.isArchived }
    }
