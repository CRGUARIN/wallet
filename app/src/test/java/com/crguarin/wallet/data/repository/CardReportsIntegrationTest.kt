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
import com.crguarin.wallet.domain.usecase.ObserveTotalBalanceUseCase
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
 * Verifies §1.14: card purchases and interest adjustments count as expenses in reports,
 * card payments are transfers (no double count), and the card debt lowers the total balance.
 *
 * Scenario: purchase of 100 on the card, a TRANSFER payment of 100 from savings, and an
 * interest adjustment expense of 5. Reports show expense 105 / income 0, and the combined
 * balance drops by exactly 105.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CardReportsIntegrationTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var reportRepository: ReportRepositoryImpl
    private lateinit var observeTotalBalanceUseCase: ObserveTotalBalanceUseCase

    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val date = LocalDate.of(2026, 9, 28)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountRepository = AccountRepositoryImpl(database.accountDao(), database, Dispatchers.Unconfined)
        categoryRepository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
        transactionRepository = TransactionRepositoryImpl(database.transactionDao(), Dispatchers.Unconfined)
        reportRepository = ReportRepositoryImpl(database.transactionDao(), Dispatchers.Unconfined)
        observeTotalBalanceUseCase = ObserveTotalBalanceUseCase(accountRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cardPurchasePaymentAndInterest_reportOnceAndLowerTotalByNetExpense() =
        runTest {
            val savings =
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
            val card =
                accountRepository.saveAccount(
                    Account(
                        name = "Tarjeta",
                        type = AccountType.CREDIT_CARD,
                        initialBalance = Money.ZERO,
                        color = 1,
                        icon = "credit_card",
                        createdAt = now,
                        creditLimit = Money(500_000L),
                        statementDay = 15,
                        paymentDueDay = 5,
                    ),
                )
            val category =
                categoryRepository.saveCategory(
                    Category(name = "General", type = CategoryType.EXPENSE, color = 1, icon = "more_horiz"),
                )
            // Purchase of 100 on the card.
            transactionRepository.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(100L),
                    accountId = card,
                    categoryId = category,
                    date = date,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            // Payment of 100 from savings: a transfer, not an expense.
            transactionRepository.saveTransaction(
                Transaction(
                    type = TransactionType.TRANSFER,
                    amount = Money(100L),
                    accountId = savings,
                    toAccountId = card,
                    date = date,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            // Interest adjustment of 5 on the card.
            transactionRepository.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = Money(5L),
                    accountId = card,
                    categoryId = category,
                    date = date,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

            val daily = reportRepository.observeDailyIncomeExpense(date, date, null).first()
            assertThat(daily).hasSize(1)
            assertThat(daily[0].income).isEqualTo(Money.ZERO)
            assertThat(daily[0].expense).isEqualTo(Money(105L))

            assertThat(observeTotalBalanceUseCase().first()).isEqualTo(Money(1_000_000L - 105L))
        }
}
