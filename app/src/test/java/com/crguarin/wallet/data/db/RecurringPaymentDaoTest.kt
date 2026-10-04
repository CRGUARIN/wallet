package com.crguarin.wallet.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.AccountType
import com.crguarin.wallet.domain.model.CategoryType
import com.crguarin.wallet.domain.model.TransactionType
import com.crguarin.wallet.domain.model.recurring.RecurrenceFrequency
import com.crguarin.wallet.domain.model.recurring.RecurringMode
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
 * Covers [RecurringPaymentDao], the `recurring_id` column of [TransactionDao] and the
 * RESTRICT foreign keys of `recurring_payments`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringPaymentDaoTest {
    private lateinit var database: WalletDatabase
    private lateinit var recurringDao: RecurringPaymentDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao

    private var accountId: Long = 0
    private var otherAccountId: Long = 0
    private var categoryId: Long = 0
    private var otherCategoryId: Long = 0

    private val now = Instant.parse("2026-10-01T12:00:00Z")

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database =
                Room
                    .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            recurringDao = database.recurringPaymentDao()
            transactionDao = database.transactionDao()
            accountDao = database.accountDao()
            categoryDao = database.categoryDao()

            accountId =
                accountDao.insert(
                    AccountEntity(
                        name = "Ahorros",
                        type = AccountType.SAVINGS,
                        currencyCode = "COP",
                        initialBalance = 1_000_000L,
                        color = 1,
                        icon = "bank",
                        createdAt = now,
                    ),
                )
            otherAccountId =
                accountDao.insert(
                    AccountEntity(
                        name = "Efectivo",
                        type = AccountType.CASH,
                        currencyCode = "COP",
                        initialBalance = 0L,
                        color = 2,
                        icon = "wallet",
                        createdAt = now,
                    ),
                )
            categoryId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Suscripciones",
                        type = CategoryType.EXPENSE,
                        color = 1,
                        icon = "subscriptions",
                    ),
                )
            otherCategoryId =
                categoryDao.insert(
                    CategoryEntity(
                        name = "Salario",
                        type = CategoryType.INCOME,
                        color = 2,
                        icon = "payments",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    private fun payment(
        name: String = "Netflix",
        account: Long = accountId,
        category: Long = categoryId,
    ) = RecurringPaymentEntity(
        name = name,
        type = TransactionType.EXPENSE,
        amount = 45_900L,
        accountId = account,
        categoryId = category,
        note = "Mensual",
        frequency = RecurrenceFrequency.MONTHLY,
        interval = 1,
        startDate = LocalDate.of(2026, 10, 5),
        nextDueDate = LocalDate.of(2026, 10, 5),
        endDate = null,
        mode = RecurringMode.AUTO,
        reminderDaysBefore = 2,
        isActive = true,
        createdAt = now,
        updatedAt = now,
    )

    private suspend fun insertTx(
        paymentId: Long,
        date: LocalDate = LocalDate.of(2026, 10, 5),
        amount: Long = 45_900L,
    ): Long =
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = amount,
                accountId = accountId,
                categoryId = categoryId,
                date = date,
                note = "Netflix",
                createdAt = now,
                updatedAt = now,
                recurringId = paymentId,
            ),
        )

    @Test
    fun insertAndGet_roundTripsEveryField() =
        runTest {
            val id = recurringDao.insert(payment())

            val stored = recurringDao.getById(id)
            assertThat(stored).isNotNull()
            assertThat(stored?.name).isEqualTo("Netflix")
            assertThat(stored?.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(stored?.amount).isEqualTo(45_900L)
            assertThat(stored?.accountId).isEqualTo(accountId)
            assertThat(stored?.categoryId).isEqualTo(categoryId)
            assertThat(stored?.note).isEqualTo("Mensual")
            assertThat(stored?.frequency).isEqualTo(RecurrenceFrequency.MONTHLY)
            assertThat(stored?.interval).isEqualTo(1)
            assertThat(stored?.startDate).isEqualTo(LocalDate.of(2026, 10, 5))
            assertThat(stored?.nextDueDate).isEqualTo(LocalDate.of(2026, 10, 5))
            assertThat(stored?.endDate).isNull()
            assertThat(stored?.mode).isEqualTo(RecurringMode.AUTO)
            assertThat(stored?.reminderDaysBefore).isEqualTo(2)
            assertThat(stored?.isActive).isTrue()
            assertThat(stored?.createdAt).isEqualTo(now)
            assertThat(stored?.updatedAt).isEqualTo(now)
        }

    @Test
    fun observeAll_emitsInDueDateOrder() =
        runTest {
            recurringDao.insert(payment(name = "Later", category = categoryId).copy(nextDueDate = LocalDate.of(2026, 11, 5)))
            recurringDao.insert(payment(name = "Sooner", category = categoryId).copy(nextDueDate = LocalDate.of(2026, 10, 5)))

            recurringDao.observeAll().test {
                val list = awaitItem()
                assertThat(list.map { it.name }).containsExactly("Sooner", "Later").inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun updateNextDueDate_advancesOnlyTheDateAndTimestamp() =
        runTest {
            val id = recurringDao.insert(payment())
            val advancedAt = Instant.parse("2026-10-06T08:00:00Z")

            recurringDao.updateNextDueDate(id, LocalDate.of(2026, 11, 5), advancedAt)

            val stored = recurringDao.getById(id)
            assertThat(stored?.nextDueDate).isEqualTo(LocalDate.of(2026, 11, 5))
            assertThat(stored?.updatedAt).isEqualTo(advancedAt)
            assertThat(stored?.startDate).isEqualTo(LocalDate.of(2026, 10, 5))
            assertThat(stored?.amount).isEqualTo(45_900L)
        }

    @Test
    fun countByAccountAndCategory_countsOnlyMatchingPayments() =
        runTest {
            recurringDao.insert(payment(name = "One"))
            recurringDao.insert(payment(name = "Two", category = categoryId))
            recurringDao.insert(payment(name = "Three", account = otherAccountId, category = otherCategoryId))

            assertThat(recurringDao.countByAccount(accountId)).isEqualTo(2)
            assertThat(recurringDao.countByAccount(otherAccountId)).isEqualTo(1)
            assertThat(recurringDao.countByCategory(categoryId)).isEqualTo(2)
            assertThat(recurringDao.countByCategory(otherCategoryId)).isEqualTo(1)
        }

    @Test
    fun reassignCategory_movesPaymentsToDestination() =
        runTest {
            val id = recurringDao.insert(payment())

            recurringDao.reassignCategory(fromId = categoryId, toId = otherCategoryId)

            assertThat(recurringDao.getById(id)?.categoryId).isEqualTo(otherCategoryId)
            assertThat(recurringDao.countByCategory(categoryId)).isEqualTo(0)
            assertThat(recurringDao.countByCategory(otherCategoryId)).isEqualTo(1)
        }

    @Test
    fun deleteAccountOrCategory_withPayments_failsByRestrict() =
        runTest {
            recurringDao.insert(payment())

            val account = accountDao.getById(accountId)!!
            var accountFailed = false
            try {
                accountDao.delete(account)
            } catch (e: SQLiteConstraintException) {
                accountFailed = true
            }
            assertThat(accountFailed).isTrue()

            val category = categoryDao.getById(categoryId)!!
            var categoryFailed = false
            try {
                categoryDao.delete(category)
            } catch (e: SQLiteConstraintException) {
                categoryFailed = true
            }
            assertThat(categoryFailed).isTrue()
        }

    @Test
    fun clearRecurringId_nullsEveryRowOfThePayment() =
        runTest {
            val id = recurringDao.insert(payment())
            val txId = insertTx(id)
            val otherTxId = insertTx(id, date = LocalDate.of(2026, 11, 5))

            transactionDao.clearRecurringId(id)

            assertThat(transactionDao.getById(txId)?.recurringId).isNull()
            assertThat(transactionDao.getById(otherTxId)?.recurringId).isNull()
        }

    @Test
    fun observeGeneratedByPayment_ordersNewestFirstAndRespectsLimit() =
        runTest {
            val id = recurringDao.insert(payment())
            val otherId = recurringDao.insert(payment(name = "Other"))
            insertTx(id, date = LocalDate.of(2026, 10, 5), amount = 1L)
            insertTx(id, date = LocalDate.of(2026, 11, 5), amount = 2L)
            insertTx(id, date = LocalDate.of(2026, 12, 5), amount = 3L)
            insertTx(otherId, date = LocalDate.of(2026, 12, 6), amount = 99L)

            transactionDao.observeGeneratedByPayment(id, Int.MAX_VALUE).test {
                val list = awaitItem()
                assertThat(list.map { it.transaction.amount }).containsExactly(3L, 2L, 1L).inOrder()
                cancelAndIgnoreRemainingEvents()
            }

            transactionDao.observeGeneratedByPayment(id, 2).test {
                val list = awaitItem()
                assertThat(list.map { it.transaction.amount }).containsExactly(3L, 2L).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
