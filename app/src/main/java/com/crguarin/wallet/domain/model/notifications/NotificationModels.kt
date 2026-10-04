package com.crguarin.wallet.domain.model.notifications

import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.PostedOccurrence
import java.time.LocalDate

data class NotificationSettings(
    val enabled: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 0,
    val recurringReminders: Boolean = true,
    val recurringPosted: Boolean = true,
    val recurringPendingConfirm: Boolean = true,
    val cardClosing: Boolean = true,
    val cardClosingDaysBefore: Int = 0, // 0..7
    val cardPayment: Boolean = true,
    val cardPaymentDaysBefore: Int = 3, // 0..7
    val cardOverdue: Boolean = true,
)

sealed interface NotificationEvent {
    val key: String // stable; the notification id derives from it

    data class RecurringUpcoming(
        override val key: String,
        val paymentId: Long,
        val name: String,
        val type: TransactionType,
        val amount: Money,
        val date: LocalDate,
        val daysUntil: Int,
        val accountName: String,
    ) : NotificationEvent

    data class RecurringPending(
        override val key: String,
        val paymentId: Long,
        val name: String,
        val type: TransactionType,
        val amount: Money,
        val date: LocalDate,
    ) : NotificationEvent

    data class RecurringPosted(
        override val key: String,
        val occurrences: List<PostedOccurrence>,
    ) : NotificationEvent

    data class CardClosing(
        override val key: String,
        val accountId: Long,
        val cardName: String,
        val closingDate: LocalDate,
        val daysUntil: Int,
    ) : NotificationEvent

    data class CardPaymentDue(
        override val key: String,
        val accountId: Long,
        val cardName: String,
        val dueDate: LocalDate,
        val daysUntil: Int,
        val amount: Money?,
    ) : NotificationEvent

    data class CardOverdue(
        override val key: String,
        val accountId: Long,
        val cardName: String,
        val dueDate: LocalDate,
        val amount: Money,
    ) : NotificationEvent
}
