package com.crguarin.wallet.domain.usecase.notifications

import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.card.CardMath
import com.crguarin.wallet.domain.model.card.StatementPaymentStatus
import com.crguarin.wallet.domain.model.notifications.CardReminderMath
import com.crguarin.wallet.domain.model.notifications.NotificationEvent
import com.crguarin.wallet.domain.model.notifications.NotificationSettings
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.blockReason
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Computes every notification event except [NotificationEvent.RecurringPosted].
 *
 * An event with trigger day `T` fires when `T` is in `(lower, today]`, where
 * `lower = max(lastRun, today - 3)` (only today when [lastRun] is null).
 * Upcoming, closing and payment-due events also require their date to be
 * today or later; pending and overdue events are validated with current state.
 * Keys are stable and unique per firing, so repeating one replaces it instead
 * of duplicating it. The general `enabled` switch is not applied here: the
 * caller decides whether to show, while the run date always advances.
 */
class ComputeNotificationEventsUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
    ) {
        suspend operator fun invoke(
            lastRun: LocalDate?,
            today: LocalDate,
            settings: NotificationSettings,
        ): List<NotificationEvent> {
            val events = mutableListOf<NotificationEvent>()
            val floor = lastRun?.let { maxOf(it, today.minusDays(LOOKBACK_DAYS)) }

            fun fires(trigger: LocalDate): Boolean =
                if (floor == null) {
                    trigger == today
                } else {
                    trigger > floor && !trigger.isAfter(today)
                }

            val payments = recurringRepository.getAll()
            val accountsById = accountRepository.getAllAccounts().associateBy { it.id }
            val categoriesById = categoryRepository.getAllCategories().associateBy { it.id }

            if (settings.recurringReminders || settings.recurringPendingConfirm) {
                for (payment in payments) {
                    if (!payment.isActive || payment.isFinished) continue
                    val account = accountsById[payment.accountId] ?: continue
                    if (payment.blockReason(account, categoriesById[payment.categoryId]) != null) continue
                    if (settings.recurringReminders) {
                        val daysBefore = payment.reminderDaysBefore
                        if (daysBefore != null) {
                            val horizon = today.plusDays(MAX_REMINDER_DAYS)
                            for (date in RecurrenceMath.occurrencesBetween(payment, today, horizon)) {
                                if (payment.endDate != null && date.isAfter(payment.endDate)) continue
                                val trigger = date.minusDays(daysBefore.toLong())
                                if (fires(trigger)) {
                                    events.add(
                                        NotificationEvent.RecurringUpcoming(
                                            key = "rec-up-${payment.id}-$date",
                                            paymentId = payment.id,
                                            name = payment.name,
                                            type = payment.type,
                                            amount = payment.amount,
                                            date = date,
                                            daysUntil = ChronoUnit.DAYS.between(today, date).toInt(),
                                            accountName = account.name,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    if (settings.recurringPendingConfirm &&
                        payment.mode == RecurringMode.CONFIRM &&
                        !payment.nextDueDate.isAfter(today) &&
                        fires(payment.nextDueDate)
                    ) {
                        events.add(
                            NotificationEvent.RecurringPending(
                                key = "rec-pend-${payment.id}-${payment.nextDueDate}",
                                paymentId = payment.id,
                                name = payment.name,
                                type = payment.type,
                                amount = payment.amount,
                                date = payment.nextDueDate,
                            ),
                        )
                    }
                }
            }

            val cards =
                accountsById.values.filter {
                    it.type == AccountType.CREDIT_CARD && !it.isArchived && it.statementDay != null
                }
            if (cards.isNotEmpty() &&
                (settings.cardClosing || settings.cardPayment || settings.cardOverdue)
            ) {
                val overviews = observeCardOverviewsUseCase().first()
                for (account in cards) {
                    val statementDay = account.statementDay ?: continue
                    if (settings.cardClosing) {
                        val closing = CardMath.nextClosingDate(today, statementDay)
                        val trigger = closing.minusDays(settings.cardClosingDaysBefore.toLong())
                        if (!closing.isBefore(today) && fires(trigger)) {
                            events.add(
                                NotificationEvent.CardClosing(
                                    key = "card-close-${account.id}-$closing",
                                    accountId = account.id,
                                    cardName = account.name,
                                    closingDate = closing,
                                    daysUntil = ChronoUnit.DAYS.between(today, closing).toInt(),
                                ),
                            )
                        }
                    }
                    val overview = overviews[account.id]
                    if (overview != null && (settings.cardPayment || settings.cardOverdue)) {
                        val paymentDueDay = account.paymentDueDay ?: continue
                        val info = CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today)
                        if (info != null) {
                            if (settings.cardPayment && !info.dueDate.isBefore(today)) {
                                val triggers =
                                    if (settings.cardPaymentDaysBefore == 0) {
                                        listOf(info.dueDate)
                                    } else {
                                        listOf(
                                            info.dueDate.minusDays(settings.cardPaymentDaysBefore.toLong()),
                                            info.dueDate,
                                        )
                                    }
                                for (trigger in triggers) {
                                    if (fires(trigger)) {
                                        events.add(
                                            NotificationEvent.CardPaymentDue(
                                                key = "card-pay-${account.id}-${info.dueDate}-$trigger",
                                                accountId = account.id,
                                                cardName = account.name,
                                                dueDate = info.dueDate,
                                                daysUntil = ChronoUnit.DAYS.between(today, info.dueDate).toInt(),
                                                amount = info.amount,
                                            ),
                                        )
                                    }
                                }
                            }
                            if (settings.cardOverdue && overview.paymentStatus == StatementPaymentStatus.OVERDUE) {
                                val dueDate = overview.pendingStatementDueDate ?: overview.lastStatement?.dueDate
                                val amount = overview.pendingStatementAmount
                                if (dueDate != null && amount != null && fires(dueDate.plusDays(1))) {
                                    events.add(
                                        NotificationEvent.CardOverdue(
                                            key = "card-over-${account.id}-$dueDate",
                                            accountId = account.id,
                                            cardName = account.name,
                                            dueDate = dueDate,
                                            amount = amount,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return events
        }

        companion object {
            const val LOOKBACK_DAYS = 3L
            const val MAX_REMINDER_DAYS = 7L
        }
    }
