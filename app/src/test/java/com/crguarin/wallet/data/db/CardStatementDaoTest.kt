package com.crguarin.wallet.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
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
 * Covers the new credit-card columns round-trip plus [CardStatementDao]:
 * ordering, lookup by (account, closing), update, CASCADE on account delete
 * and SET NULL on adjustment delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CardStatementDaoTest {
    private lateinit var database: WalletDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var cardStatementDao: CardStatementDao

    private val now = Instant.parse("2026-09-28T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountDao = database.accountDao()
        transactionDao = database.transactionDao()
        cardStatementDao = database.cardStatementDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createCardAccount(
        name: String = "RappiCard",
        creditLimit: Long? = 2_000_000L,
        statementDay: Int? = 15,
        paymentDueDay: Int? = 5,
    ): Long =
        accountDao.insert(
            AccountEntity(
                name = name,
                type = AccountType.CREDIT_CARD,
                currencyCode = "COP",
                initialBalance = -50_000L,
                color = 1,
                icon = "credit_card",
                createdAt = now,
                creditLimit = creditLimit,
                statementDay = statementDay,
                paymentDueDay = paymentDueDay,
            ),
        )

    private suspend fun createTransaction(
        accountId: Long,
        installments: Int? = null,
    ): Long =
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 100_000L,
                accountId = accountId,
                date = LocalDate.of(2026, 9, 10),
                note = "Compra",
                createdAt = now,
                updatedAt = now,
                installments = installments,
            ),
        )

    private fun createStatement(
        accountId: Long,
        closingDate: LocalDate,
        adjustmentTransactionId: Long? = null,
    ): CardStatementEntity =
        CardStatementEntity(
            accountId = accountId,
            closingDate = closingDate,
            dueDate = LocalDate.of(2026, 10, 5),
            statementBalance = 150_000L,
            minimumPayment = 20_000L,
            statedInterestFees = 5_000L,
            adjustmentTransactionId = adjustmentTransactionId,
            createdAt = now,
        )

    @Test
    fun cardColumns_roundTripOnAccountsAndTransactions() =
        runTest {
            val accountId = createCardAccount()
            val txId = createTransaction(accountId, installments = 12)

            val account = accountDao.getById(accountId)
            assertThat(account?.creditLimit).isEqualTo(2_000_000L)
            assertThat(account?.statementDay).isEqualTo(15)
            assertThat(account?.paymentDueDay).isEqualTo(5)

            val tx = transactionDao.getById(txId)
            assertThat(tx?.installments).isEqualTo(12)
        }

    @Test
    fun statements_observedNewestClosingFirst() =
        runTest {
            val accountId = createCardAccount()
            cardStatementDao.insert(createStatement(accountId, LocalDate.of(2026, 8, 15)))
            cardStatementDao.insert(createStatement(accountId, LocalDate.of(2026, 9, 15)))
            cardStatementDao.insert(createStatement(accountId, LocalDate.of(2026, 7, 15)))

            cardStatementDao.observeByAccount(accountId).test {
                val dates = awaitItem().map { it.closingDate }
                assertThat(dates).containsExactly(
                    LocalDate.of(2026, 9, 15),
                    LocalDate.of(2026, 8, 15),
                    LocalDate.of(2026, 7, 15),
                )
            }
        }

    @Test
    fun statements_getByAccountAndClosingAndUpdate() =
        runTest {
            val accountId = createCardAccount()
            val closing = LocalDate.of(2026, 9, 15)
            val id = cardStatementDao.insert(createStatement(accountId, closing))

            val loaded = cardStatementDao.getByAccountAndClosing(accountId, closing)
            assertThat(loaded?.id).isEqualTo(id)
            assertThat(loaded?.statementBalance).isEqualTo(150_000L)
            assertThat(loaded?.minimumPayment).isEqualTo(20_000L)
            assertThat(loaded?.adjustmentTransactionId).isNull()

            cardStatementDao.update(loaded!!.copy(statementBalance = 160_000L))
            assertThat(
                cardStatementDao.getByAccountAndClosing(accountId, closing)?.statementBalance,
            ).isEqualTo(160_000L)
            assertThat(
                cardStatementDao.getByAccountAndClosing(accountId, LocalDate.of(2026, 8, 15)),
            ).isNull()
        }

    @Test
    fun deletingCardAccount_deletesItsStatements() =
        runTest {
            val accountId = createCardAccount()
            cardStatementDao.insert(createStatement(accountId, LocalDate.of(2026, 8, 15)))
            cardStatementDao.insert(createStatement(accountId, LocalDate.of(2026, 9, 15)))

            accountDao.delete(accountDao.getById(accountId)!!)

            cardStatementDao.observeByAccount(accountId).test {
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun deletingAdjustmentTransaction_setsLinkToNull() =
        runTest {
            val accountId = createCardAccount()
            val txId = createTransaction(accountId)
            val closing = LocalDate.of(2026, 9, 15)
            cardStatementDao.insert(createStatement(accountId, closing, adjustmentTransactionId = txId))

            transactionDao.deleteById(txId)

            assertThat(
                cardStatementDao.getByAccountAndClosing(accountId, closing)?.adjustmentTransactionId,
            ).isNull()
        }

    @Test
    fun categories_getBySystemKey() =
        runTest {
            val dao = database.categoryDao()
            val id =
                dao.insert(
                    CategoryEntity(
                        name = null,
                        systemKey = "card_interest_fees",
                        type = CategoryType.EXPENSE,
                        color = 1,
                        icon = "credit_card",
                    ),
                )
            assertThat(dao.getBySystemKey("card_interest_fees")?.id).isEqualTo(id)
            assertThat(dao.getBySystemKey("missing")).isNull()
        }
}
