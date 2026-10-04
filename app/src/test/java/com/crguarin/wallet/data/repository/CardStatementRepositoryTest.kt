package com.crguarin.wallet.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.data.db.TransactionEntity
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.Transaction
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.card.CardStatement
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
 * Covers [CardStatementRepositoryImpl]: card transaction observation (including transfers
 * in both directions), the signed effect sum with inclusive cutoff, and the atomic
 * reconciliation save (insert, same-cutoff update, adjustment link, rollback on failure).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CardStatementRepositoryTest {
    private lateinit var database: WalletDatabase
    private lateinit var repository: CardStatementRepositoryImpl

    private val now = Instant.parse("2026-09-28T12:00:00Z")
    private val closing = LocalDate.of(2026, 9, 15)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository =
            CardStatementRepositoryImpl(
                walletDatabase = database,
                cardStatementDao = database.cardStatementDao(),
                transactionDao = database.transactionDao(),
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createAccount(
        name: String,
        type: AccountType = AccountType.CASH,
        initialBalance: Long = 0L,
    ): Long =
        database.accountDao().insert(
            AccountEntity(
                name = name,
                type = type,
                currencyCode = "COP",
                initialBalance = initialBalance,
                color = 1,
                icon = "wallet",
                createdAt = now,
                creditLimit = if (type == AccountType.CREDIT_CARD) 1_000_000L else null,
                statementDay = if (type == AccountType.CREDIT_CARD) 15 else null,
                paymentDueDay = if (type == AccountType.CREDIT_CARD) 5 else null,
            ),
        )

    private suspend fun insertTx(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long? = null,
        date: LocalDate = LocalDate.of(2026, 9, 10),
    ): Long =
        database.transactionDao().insert(
            TransactionEntity(
                type = type,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                date = date,
                createdAt = now,
                updatedAt = now,
            ),
        )

    private fun adjustment(
        accountId: Long,
        amount: Long = 5_000L,
    ): Transaction =
        Transaction(
            type = TransactionType.EXPENSE,
            amount = Money(amount),
            accountId = accountId,
            date = closing,
            note = "Extracto 2026-09-15",
            createdAt = now,
            updatedAt = now,
        )

    private fun statement(
        accountId: Long,
        closingDate: LocalDate = closing,
        adjustmentTransactionId: Long? = null,
    ): CardStatement =
        CardStatement(
            accountId = accountId,
            closingDate = closingDate,
            dueDate = LocalDate.of(2026, 10, 5),
            statementBalance = Money(155_000L),
            minimumPayment = Money(20_000L),
            statedInterestAndFees = Money(5_000L),
            adjustmentTransactionId = adjustmentTransactionId,
            createdAt = now,
        )

    @Test
    fun observeCardTransactions_includesExpensesAndBothTransferDirections() =
        runTest {
            val card = createAccount("Tarjeta", AccountType.CREDIT_CARD)
            val savings = createAccount("Ahorros")
            val purchaseDate = LocalDate.of(2026, 9, 10)
            insertTx(TransactionType.EXPENSE, 100_000L, card, date = purchaseDate)
            insertTx(TransactionType.TRANSFER, 80_000L, savings, toAccountId = card, date = purchaseDate)
            insertTx(TransactionType.TRANSFER, 10_000L, card, toAccountId = savings, date = purchaseDate)
            insertTx(TransactionType.EXPENSE, 1_000L, savings, date = purchaseDate)
            insertTx(TransactionType.EXPENSE, 999L, card, date = LocalDate.of(2026, 1, 1))

            val observed =
                repository.observeCardTransactions(card, from = LocalDate.of(2026, 9, 1)).first()
            assertThat(observed).hasSize(3)
            assertThat(observed.map { it.type }).containsExactly(
                TransactionType.EXPENSE,
                TransactionType.TRANSFER,
                TransactionType.TRANSFER,
            )
        }

    @Test
    fun getAccountEffectUpTo_isInclusiveAndCoversAllTypes() =
        runTest {
            val card = createAccount("Tarjeta", AccountType.CREDIT_CARD)
            val savings = createAccount("Ahorros")
            val cutoff = LocalDate.of(2026, 9, 10)
            insertTx(TransactionType.INCOME, 500L, card, date = LocalDate.of(2026, 9, 5))
            insertTx(TransactionType.EXPENSE, 200L, card, date = cutoff)
            insertTx(TransactionType.TRANSFER, 100L, card, toAccountId = savings, date = cutoff)
            insertTx(TransactionType.TRANSFER, 50L, savings, toAccountId = card, date = cutoff)
            insertTx(TransactionType.EXPENSE, 9_999L, card, date = cutoff.plusDays(1))

            assertThat(repository.getAccountEffectUpTo(card, cutoff)).isEqualTo(Money(250L))
        }

    @Test
    fun saveReconciliation_insertsStatementAndLinksAdjustment() =
        runTest {
            val card = createAccount("Tarjeta", AccountType.CREDIT_CARD)

            val id = repository.saveReconciliation(statement(card), adjustment(card))

            val statements = repository.observeStatements(card).first()
            assertThat(statements).hasSize(1)
            assertThat(statements[0].id).isEqualTo(id)
            assertThat(statements[0].closingDate).isEqualTo(closing)
            assertThat(statements[0].adjustmentTransactionId).isNotNull()
            val linked = database.transactionDao().getById(statements[0].adjustmentTransactionId!!)
            assertThat(linked?.amount).isEqualTo(5_000L)
            assertThat(linked?.date).isEqualTo(closing)
        }

    @Test
    fun saveReconciliation_sameCutoffUpdatesAndKeepsPreviousLinkWithoutNewAdjustment() =
        runTest {
            val card = createAccount("Tarjeta", AccountType.CREDIT_CARD)
            val firstId = repository.saveReconciliation(statement(card), adjustment(card))

            val secondId =
                repository.saveReconciliation(
                    statement(card).copy(statementBalance = Money(160_000L)),
                    adjustment = null,
                )

            assertThat(secondId).isEqualTo(firstId)
            val statements = repository.observeStatements(card).first()
            assertThat(statements).hasSize(1)
            assertThat(statements[0].statementBalance).isEqualTo(Money(160_000L))
            assertThat(statements[0].adjustmentTransactionId).isNotNull()
        }

    @Test
    fun saveReconciliation_sameCutoffWithNewAdjustmentReplacesLink() =
        runTest {
            val card = createAccount("Tarjeta", AccountType.CREDIT_CARD)
            val firstId = repository.saveReconciliation(statement(card), adjustment(card, amount = 5_000L))
            val firstLink = repository.observeStatements(card).first()[0].adjustmentTransactionId

            val secondId =
                repository.saveReconciliation(statement(card), adjustment(card, amount = 7_000L))

            assertThat(secondId).isEqualTo(firstId)
            val updated = repository.observeStatements(card).first()[0]
            assertThat(updated.adjustmentTransactionId).isNotNull()
            assertThat(updated.adjustmentTransactionId).isNotEqualTo(firstLink)
            assertThat(
                database.transactionDao().getById(updated.adjustmentTransactionId!!)?.amount,
            ).isEqualTo(7_000L)
        }

    @Test
    fun saveReconciliation_whenStatementFails_adjustmentIsNotPersisted() =
        runTest {
            val card = createAccount("Tarjeta", AccountType.CREDIT_CARD)
            val before =
                repository.observeCardTransactions(card, from = LocalDate.of(1970, 1, 1)).first().size
            var failed = false
            try {
                // Unknown account violates the card_statements foreign key.
                repository.saveReconciliation(statement(accountId = 9_999_999L), adjustment(card))
            } catch (e: SQLiteConstraintException) {
                failed = true
            }
            assertThat(failed).isTrue()
            assertThat(
                repository.observeCardTransactions(card, from = LocalDate.of(1970, 1, 1)).first(),
            ).hasSize(before)
        }
}
