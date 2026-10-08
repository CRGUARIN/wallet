package com.crguarin.wallet.domain.model.debt

import androidx.annotation.Keep
import com.crguarin.wallet.core.money.Money
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

@Keep
@Serializable
enum class DebtDirection {
    OWED_TO_ME, // someone owes me (receivable)
    I_OWE, // I owe someone (payable)
}

data class Debt(
    val id: Long = 0,
    val personName: String,
    val direction: DebtDirection,
    val amount: Money, // original amount, > 0
    val date: LocalDate, // when the debt started
    val dueDate: LocalDate? = null, // optional, >= date
    val note: String? = null, // concept, <= 200 chars
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class DebtPayment(
    val id: Long = 0,
    val debtId: Long,
    val amount: Money, // > 0
    val date: LocalDate,
    val note: String? = null, // <= 200 chars
    val createdAt: Instant,
)

/** A debt plus the sum of its payments. */
data class DebtSummary(
    val debt: Debt,
    val paid: Money,
) {
    val remaining: Money get() = debt.amount - paid
    val isSettled: Boolean get() = !remaining.isPositive

    fun isOverdue(today: LocalDate): Boolean = !isSettled && debt.dueDate != null && debt.dueDate.isBefore(today)
}

data class DebtsOverview(
    val owedToMe: List<DebtSummary>, // not settled, direction OWED_TO_ME
    val iOwe: List<DebtSummary>, // not settled, direction I_OWE
    val settled: List<DebtSummary>, // settled, both directions
    val totalOwedToMe: Money, // sum of remaining of owedToMe
    val totalIOwe: Money, // sum of remaining of iOwe
) {
    val net: Money get() = totalOwedToMe - totalIOwe
    val hasActive: Boolean get() = owedToMe.isNotEmpty() || iOwe.isNotEmpty()
    val isEmpty: Boolean get() = !hasActive && settled.isEmpty()
}

data class DebtDetail(
    val summary: DebtSummary,
    val payments: List<DebtPayment>, // newest first: date DESC, id DESC
)
