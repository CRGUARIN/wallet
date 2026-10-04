package com.crguarin.wallet.domain.usecase.card

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.core.result.DomainResult
import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardStatement
import com.crguarin.wallet.domain.model.card.ReconciliationPreview
import com.crguarin.wallet.domain.repository.CardStatementRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.usecase.SaveTransactionUseCase
import java.time.LocalDate
import javax.inject.Inject

const val CARD_INTEREST_FEES_KEY = "card_interest_fees"
const val CARD_ADJUSTMENT_KEY = "card_adjustment"
const val CARD_INTEREST_FEES_ICON = "credit_card"
const val CARD_ADJUSTMENT_ICON = "receipt_long"

val CARD_INTEREST_FEES_COLOR: Int = 0xFFEF5350.toInt()
val CARD_ADJUSTMENT_COLOR: Int = 0xFF66BB6A.toInt()

/**
 * Saves the statement described by [preview] and, when the difference requires it, the
 * adjustment transaction. The adjustment goes through [SaveTransactionUseCase.validate],
 * the same validation as any manually saved transaction, without duplicating it.
 */
class ApplyReconciliationUseCase
    @Inject
    constructor(
        private val cardStatementRepository: CardStatementRepository,
        private val categoryRepository: CategoryRepository,
        private val saveTransactionUseCase: SaveTransactionUseCase,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(
            preview: ReconciliationPreview,
            dueDate: LocalDate,
            minimumPayment: Money?,
            statedInterestAndFees: Money?,
            adjustmentNote: String,
            forceNegativeAdjustment: Boolean,
        ): DomainResult<Long> {
            val difference = preview.difference
            val adjustment =
                when {
                    difference.isPositive ->
                        buildAdjustment(
                            preview = preview,
                            amount = difference,
                            type = TransactionType.EXPENSE,
                            systemKey = CARD_INTEREST_FEES_KEY,
                            categoryType = CategoryType.EXPENSE,
                            color = CARD_INTEREST_FEES_COLOR,
                            icon = CARD_INTEREST_FEES_ICON,
                            note = adjustmentNote,
                        )
                    difference.isNegative && forceNegativeAdjustment ->
                        buildAdjustment(
                            preview = preview,
                            amount = -difference,
                            type = TransactionType.INCOME,
                            systemKey = CARD_ADJUSTMENT_KEY,
                            categoryType = CategoryType.INCOME,
                            color = CARD_ADJUSTMENT_COLOR,
                            icon = CARD_ADJUSTMENT_ICON,
                            note = adjustmentNote,
                        )
                    else -> null
                }
            val validatedAdjustment =
                if (adjustment != null) {
                    when (val validated = saveTransactionUseCase.validate(adjustment)) {
                        is DomainResult.Success -> validated.value
                        is DomainResult.Failure -> return validated
                    }
                } else {
                    null
                }
            val statement =
                CardStatement(
                    accountId = preview.accountId,
                    closingDate = preview.closingDate,
                    dueDate = dueDate,
                    statementBalance = preview.statementBalance,
                    minimumPayment = minimumPayment,
                    statedInterestAndFees = statedInterestAndFees,
                    adjustmentTransactionId = null,
                    createdAt = timeProvider.now(),
                )
            val id = cardStatementRepository.saveReconciliation(statement, validatedAdjustment)
            return DomainResult.Success(id)
        }

        private suspend fun buildAdjustment(
            preview: ReconciliationPreview,
            amount: Money,
            type: TransactionType,
            systemKey: String,
            categoryType: CategoryType,
            color: Int,
            icon: String,
            note: String,
        ): Transaction {
            val categoryId = categoryRepository.ensureSystemCategory(systemKey, categoryType, color, icon)
            val now = timeProvider.now()
            return Transaction(
                type = type,
                amount = amount,
                accountId = preview.accountId,
                categoryId = categoryId,
                date = preview.closingDate,
                note = note,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
