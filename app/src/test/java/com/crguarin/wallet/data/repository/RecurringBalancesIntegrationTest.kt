package com.crguarin.wallet.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Category
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.OccurrenceResult
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
import com.crguarin.wallet.domain.model.recurring.RecurringPayment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * Scenario from §2.4: generated occurrences behave like normal movements in balances and
 * reports; deleting the rule keeps them, deleting the movement restores the initial values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringBalancesIntegrationTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var recurringRepository: RecurringRepositoryImpl

    private val now = Instant.parse("2026-10-05T12:00:00Z")
    private val date = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val dispatcher = Dispatchers.Unconfined
        accountRepository = AccountRepositoryImpl(database.accountDao(), database, dispatcher)
        categoryRepository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = dispatcher,
            )
        transactionRepository = TransactionRepositoryImpl(database.transactionDao(), dispatcher)
        recurringRepository =
            RecurringRepositoryImpl(
                recurringPaymentDao = database.recurringPaymentDao(),
                transactionDao = database.transactionDao(),
                walletDatabase = database,
                ioDispatcher = dispatcher,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun balanceOf(accountId: Long): Money = Money(database.accountDao().getAccountWithBalanceById(accountId)!!.balance)

    private suspend fun totalOf(vararg accountIds: Long): Money = accountIds.fold(Money.ZERO) { acc, id -> acc + balanceOf(id) }

    private suspend fun dailyIncomeExpense(): Pair<Long, Long> {
        val rows = database.transactionDao().observeDailyIncomeExpense(date, date).first()
        assertThat(rows).hasSize(1)
        return rows.first().income to rows.first().expense
    }

    private suspend fun expenseTotalFor(categoryId: Long): Long =
        database
            .transactionDao()
            .observeCategoryTotals(date, date, TransactionType.EXPENSE)
            .first()
            .first { it.categoryId == categoryId }
            .total

    @Test
    fun `generated occurrences move balances and reports, rule deletion keeps them`() =
        runTest {
            val savingsId =
                accountRepository.saveAccount(
                    Account(
                        name = "Ahorros",
                        type = AccountType.SAVINGS,
                        initialBalance = Money(1_000_000L),
                        color = 1,
                        icon = "bank",
                        createdAt = now,
                    ),
                )
            val cardId =
                accountRepository.saveAccount(
                    Account(
                        name = "Visa",
                        type = AccountType.CREDIT_CARD,
                        initialBalance = Money.ZERO,
                        color = 2,
                        icon = "card",
                        createdAt = now,
                        creditLimit = Money(5_000_000L),
                        statementDay = 25,
                        paymentDueDay = 15,
                    ),
                )
            val subscriptionsId =
                categoryRepository.saveCategory(
                    Category(name = "Suscripciones", type = CategoryType.EXPENSE, color = 1, icon = "s"),
                )
            val salaryId =
                categoryRepository.saveCategory(
                    Category(name = "Salario", type = CategoryType.INCOME, color = 2, icon = "p"),
                )
            val expensePaymentId =
                recurringRepository.save(
                    RecurringPayment(
                        name = "Netflix",
                        type = TransactionType.EXPENSE,
                        amount = Money(45_900L),
                        accountId = cardId,
                        categoryId = subscriptionsId,
                        frequency = RecurrenceFrequency.MONTHLY,
                        startDate = date,
                        nextDueDate = date,
                        mode = RecurringMode.AUTO,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            val incomePaymentId =
                recurringRepository.save(
                    RecurringPayment(
                        name = "Sueldo",
                        type = TransactionType.INCOME,
                        amount = Money(500_000L),
                        accountId = savingsId,
                        categoryId = salaryId,
                        frequency = RecurrenceFrequency.MONTHLY,
                        startDate = date,
                        nextDueDate = date,
                        mode = RecurringMode.AUTO,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            assertThat(totalOf(savingsId, cardId)).isEqualTo(Money(1_000_000L))

            val recordedExpense =
                recurringRepository.completeOccurrence(
                    expensePaymentId,
                    date,
                    Transaction(
                        type = TransactionType.EXPENSE,
                        amount = Money(45_900L),
                        accountId = cardId,
                        categoryId = subscriptionsId,
                        date = date,
                        note = "Netflix",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    date.plusMonths(1),
                ) as OccurrenceResult.Recorded
            val recordedIncome =
                recurringRepository.completeOccurrence(
                    incomePaymentId,
                    date,
                    Transaction(
                        type = TransactionType.INCOME,
                        amount = Money(500_000L),
                        accountId = savingsId,
                        categoryId = salaryId,
                        date = date,
                        note = "Sueldo",
                        createdAt = now,
                        updatedAt = now,
                    ),
                    date.plusMonths(1),
                ) as OccurrenceResult.Recorded

            assertThat(balanceOf(savingsId)).isEqualTo(Money(1_500_000L))
            assertThat(balanceOf(cardId)).isEqualTo(Money(-45_900L))
            assertThat(totalOf(savingsId, cardId)).isEqualTo(Money(1_454_100L))

            val (income, expense) = dailyIncomeExpense()
            assertThat(income).isEqualTo(500_000L)
            assertThat(expense).isEqualTo(45_900L)

            assertThat(expenseTotalFor(subscriptionsId)).isEqualTo(45_900L)
            assertThat(database.transactionDao().getAccountEffectUpTo(cardId, date)).isEqualTo(-45_900L)

            // Deleting the rules keeps balances and reports unchanged.
            recurringRepository.delete(expensePaymentId)
            recurringRepository.delete(incomePaymentId)
            assertThat(balanceOf(savingsId)).isEqualTo(Money(1_500_000L))
            assertThat(balanceOf(cardId)).isEqualTo(Money(-45_900L))
            assertThat(totalOf(savingsId, cardId)).isEqualTo(Money(1_454_100L))
            assertThat(dailyIncomeExpense()).isEqualTo(500_000L to 45_900L)
            assertThat(transactionRepository.getTransaction(recordedExpense.transactionId)?.recurringId).isNull()
            assertThat(transactionRepository.getTransaction(recordedIncome.transactionId)?.recurringId).isNull()

            // Deleting the generated movements restores the initial values.
            transactionRepository.deleteTransaction(recordedExpense.transactionId)
            transactionRepository.deleteTransaction(recordedIncome.transactionId)
            assertThat(balanceOf(savingsId)).isEqualTo(Money(1_000_000L))
            assertThat(balanceOf(cardId)).isEqualTo(Money.ZERO)
            assertThat(totalOf(savingsId, cardId)).isEqualTo(Money(1_000_000L))
            assertThat(database.transactionDao().observeDailyIncomeExpense(date, date).first()).isEmpty()
            assertThat(database.transactionDao().getAccountEffectUpTo(cardId, date)).isEqualTo(0L)
        }
}
