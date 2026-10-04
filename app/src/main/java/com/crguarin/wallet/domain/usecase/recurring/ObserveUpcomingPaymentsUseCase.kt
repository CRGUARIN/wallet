package com.crguarin.wallet.domain.usecase.recurring

import com.crguarin.wallet.core.time.TimeProvider
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.notifications.CardReminderMath
import com.crguarin.wallet.domain.model.recurring.RecurrenceMath
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.UpcomingKind
import com.crguarin.wallet.domain.model.recurring.UpcomingPayment
import com.crguarin.wallet.domain.model.recurring.pendingCount
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.RecurringRepository
import com.crguarin.wallet.domain.usecase.card.ObserveCardOverviewsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Home "upcoming payments" card: active payment occurrences in the next [days]
 * days, CONFIRM payments awaiting confirmation (one row each) and the pending
 * payment of every card, by date, capped at [MAX_ROWS] rows.
 *
 * Note: the category block is not applied here because this use case has no
 * access to the categories; a payment whose category is archived still shows.
 */
class ObserveUpcomingPaymentsUseCase
    @Inject
    constructor(
        private val recurringRepository: RecurringRepository,
        private val accountRepository: AccountRepository,
        private val observeCardOverviewsUseCase: ObserveCardOverviewsUseCase,
        private val timeProvider: TimeProvider,
    ) {
        operator fun invoke(days: Int = 7): Flow<List<UpcomingPayment>> =
            combine(
                recurringRepository.observeAll(),
                accountRepository.observeAllAccounts(),
                observeCardOverviewsUseCase(),
            ) { payments, accounts, cardOverviews ->
                val today = timeProvider.today()
                val end = today.plusDays(days.toLong())
                val accountsById = accounts.associateBy { it.id }
                val rows = mutableListOf<UpcomingPayment>()
                for (payment in payments) {
                    if (!payment.isActive || payment.isFinished) continue
                    val account = accountsById[payment.accountId] ?: continue
                    if (account.isArchived) continue
                    val pending = payment.pendingCount(today)
                    if (payment.mode == RecurringMode.CONFIRM && pending > 0) {
                        rows.add(
                            UpcomingPayment(
                                kind = UpcomingKind.RECURRING_PENDING,
                                refId = payment.id,
                                title = payment.name,
                                type = payment.type,
                                date = payment.nextDueDate,
                                amount = payment.amount,
                                accountName = account.name,
                            ),
                        )
                        continue
                    }
                    for (date in RecurrenceMath.occurrencesBetween(payment, today, end)) {
                        rows.add(
                            UpcomingPayment(
                                kind = UpcomingKind.RECURRING,
                                refId = payment.id,
                                title = payment.name,
                                type = payment.type,
                                date = date,
                                amount = payment.amount,
                                accountName = account.name,
                            ),
                        )
                    }
                }
                for ((accountId, overview) in cardOverviews) {
                    val account = accountsById[accountId] ?: continue
                    if (account.isArchived) continue
                    val statementDay = account.statementDay ?: continue
                    val paymentDueDay = account.paymentDueDay ?: continue
                    val info = CardReminderMath.paymentInfo(overview, statementDay, paymentDueDay, today) ?: continue
                    if (info.dueDate < today || info.dueDate > end) continue
                    rows.add(
                        UpcomingPayment(
                            kind = UpcomingKind.CARD_PAYMENT,
                            refId = accountId,
                            title = account.name,
                            type = TransactionType.EXPENSE,
                            date = info.dueDate,
                            amount = info.amount,
                            accountName = account.name,
                        ),
                    )
                }
                rows.sortedWith(compareBy({ it.date }, { it.title })).take(MAX_ROWS)
            }

        companion object {
            const val MAX_ROWS = 5
        }
    }
