package com.crguarin.wallet.domain.model.recurring

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

enum class RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

enum class RecurringMode {
    AUTO,
    CONFIRM,
}

enum class RecurringBlockReason {
    ACCOUNT_ARCHIVED,
    CATEGORY_ARCHIVED,
}

data class RecurringPayment(
    val id: Long = 0,
    val name: String,
    val type: TransactionType, // EXPENSE or INCOME
    val amount: Money,
    val accountId: Long,
    val categoryId: Long,
    val note: String? = null,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1, // 1..12
    val startDate: LocalDate, // first occurrence and calendar anchor
    val endDate: LocalDate? = null, // inclusive
    val nextDueDate: LocalDate, // next occurrence not recorded nor skipped yet
    val mode: RecurringMode = RecurringMode.AUTO,
    val reminderDaysBefore: Int? = null, // null or 1..7
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isFinished: Boolean get() = endDate != null && nextDueDate > endDate
}

data class RecurringItem(
    val payment: RecurringPayment,
    val accountName: String,
    val accountType: AccountType,
    val category: Category?,
    val pendingCount: Int, // CONFIRM occurrences due up to today (0 for AUTO)
    val blockReason: RecurringBlockReason?,
    val monthlyEquivalent: Money,
)

data class RecurringOverview(
    val pending: List<RecurringItem>, // CONFIRM with pendingCount > 0, oldest nextDueDate first
    val active: List<RecurringItem>, // active and not finished, by nextDueDate
    val inactive: List<RecurringItem>, // paused or finished, by name
    val monthlyExpense: Money, // sum of monthlyEquivalent of active EXPENSE items
    val monthlyIncome: Money,
)

data class PostedOccurrence(
    val paymentId: Long,
    val name: String,
    val type: TransactionType,
    val transactionId: Long,
    val date: LocalDate,
    val amount: Money,
    val accountId: Long,
    val accountName: String,
)

enum class UpcomingKind {
    RECURRING,
    RECURRING_PENDING,
    CARD_PAYMENT,
}

data class UpcomingPayment(
    val kind: UpcomingKind,
    val refId: Long, // payment id or card account id
    val title: String, // payment name or card name
    val type: TransactionType, // EXPENSE for cards
    val date: LocalDate,
    val amount: Money?, // null when the card statement was not reconciled
    val accountName: String,
)

sealed interface OccurrenceResult {
    data class Recorded(
        val transactionId: Long,
    ) : OccurrenceResult

    data object Skipped : OccurrenceResult

    data object AlreadyHandled : OccurrenceResult // nextDueDate no longer matched
}
