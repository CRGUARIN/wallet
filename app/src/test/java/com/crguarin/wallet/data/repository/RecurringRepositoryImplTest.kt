package com.crguarin.wallet.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.AccountEntity
import com.crguarin.wallet.data.db.CategoryEntity
import com.crguarin.wallet.data.db.WalletDatabase
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
 * Covers [RecurringRepositoryImpl.completeOccurrence] (optimistic check, atomicity, idempotency),
 * [RecurringRepositoryImpl.delete], counts, generated-transaction history and the category
 * reassignment of recurring payments.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecurringRepositoryImplTest {
    private lateinit var database: WalletDatabase
    private lateinit var repository: RecurringRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl

    private var accountId: Long = 0
    private var categoryId: Long = 0

    private val now = Instant.parse("2026-10-01T12:00:00Z")
    private val due = LocalDate.of(2026, 10, 5)
    private val next = LocalDate.of(2026, 11, 5)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository =
            RecurringRepositoryImpl(
                recurringPaymentDao = database.recurringPaymentDao(),
                transactionDao = database.transactionDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
        categoryRepository =
            CategoryRepositoryImpl(
                categoryDao = database.categoryDao(),
                walletDatabase = database,
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedAccountAndCategory() {
        accountId =
            database.accountDao().insert(
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
        categoryId =
            database.categoryDao().insert(
                CategoryEntity(
                    name = "Suscripciones",
                    type = CategoryType.EXPENSE,
                    color = 1,
                    icon = "subscriptions",
                ),
            )
    }

    private fun payment(nextDueDate: LocalDate = due) =
        RecurringPayment(
            name = "Netflix",
            type = TransactionType.EXPENSE,
            amount = Money(45_900L),
            accountId = accountId,
            categoryId = categoryId,
            note = "Mensual",
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            startDate = LocalDate.of(2026, 10, 5),
            endDate = null,
            nextDueDate = nextDueDate,
            mode = RecurringMode.AUTO,
            reminderDaysBefore = 2,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

    private fun occurrence(date: LocalDate = due) =
        Transaction(
            type = TransactionType.EXPENSE,
            amount = Money(45_900L),
            accountId = accountId,
            categoryId = categoryId,
            date = date,
            note = "Netflix",
            createdAt = now,
            updatedAt = now,
        )

    private suspend fun generatedCount(paymentId: Long): Int =
        database
            .transactionDao()
            .observeGeneratedByPayment(paymentId, Int.MAX_VALUE)
            .first()
            .size

    @Test
    fun `save and get round-trip every field including mappers`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())

            val stored = repository.get(id)
            assertThat(stored).isEqualTo(payment().copy(id = id))

            repository.observe(id).test {
                assertThat(awaitItem()).isEqualTo(payment().copy(id = id))
                cancelAndIgnoreRemainingEvents()
            }
            repository.observeAll().test {
                assertThat(awaitItem()).containsExactly(payment().copy(id = id))
                cancelAndIgnoreRemainingEvents()
            }

            // TransactionMapper carries recurringId both ways.
            val withRule = occurrence().copy(id = 11L, recurringId = id)
            assertThat(withRule.toEntity().toDomain()).isEqualTo(withRule)
            val manual = occurrence().copy(id = 12L, recurringId = null)
            assertThat(manual.toEntity().toDomain()).isEqualTo(manual)
        }

    @Test
    fun `completeOccurrence records the movement and advances the due date`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())

            val result = repository.completeOccurrence(id, due, occurrence(), next)

            assertThat(result).isInstanceOf(OccurrenceResult.Recorded::class.java)
            val transactionId = (result as OccurrenceResult.Recorded).transactionId
            assertThat(repository.get(id)?.nextDueDate).isEqualTo(next)

            val stored = database.transactionDao().getById(transactionId)
            assertThat(stored?.recurringId).isEqualTo(id)
            assertThat(stored?.amount).isEqualTo(45_900L)
            assertThat(stored?.date).isEqualTo(due)
        }

    @Test
    fun `completeOccurrence with null transaction skips without inserting`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())

            val result = repository.completeOccurrence(id, due, null, next)

            assertThat(result).isEqualTo(OccurrenceResult.Skipped)
            assertThat(repository.get(id)?.nextDueDate).isEqualTo(next)
            assertThat(generatedCount(id)).isEqualTo(0)
        }

    @Test
    fun `completeOccurrence with unexpected date or missing payment returns AlreadyHandled`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())

            assertThat(repository.completeOccurrence(id, LocalDate.of(2026, 9, 5), occurrence(), next))
                .isEqualTo(OccurrenceResult.AlreadyHandled)
            assertThat(repository.completeOccurrence(999L, due, occurrence(), next))
                .isEqualTo(OccurrenceResult.AlreadyHandled)

            assertThat(repository.get(id)?.nextDueDate).isEqualTo(due)
            assertThat(generatedCount(id)).isEqualTo(0)
        }

    @Test
    fun `two calls with the same expected date insert a single movement`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())

            val first = repository.completeOccurrence(id, due, occurrence(), next)
            val second = repository.completeOccurrence(id, due, occurrence(), next.plusMonths(1))

            assertThat(first).isInstanceOf(OccurrenceResult.Recorded::class.java)
            assertThat(second).isEqualTo(OccurrenceResult.AlreadyHandled)
            assertThat(generatedCount(id)).isEqualTo(1)
            assertThat(repository.get(id)?.nextDueDate).isEqualTo(next)
        }

    @Test
    fun `failed insert rolls back without advancing the due date`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())
            val broken = occurrence().copy(accountId = 999_999L)

            var failed = false
            try {
                repository.completeOccurrence(id, due, broken, next)
            } catch (e: Exception) {
                failed = true
            }

            assertThat(failed).isTrue()
            assertThat(repository.get(id)?.nextDueDate).isEqualTo(due)
            assertThat(generatedCount(id)).isEqualTo(0)
        }

    @Test
    fun `delete keeps generated movements with null recurringId`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())
            val recorded = repository.completeOccurrence(id, due, occurrence(), next) as OccurrenceResult.Recorded

            repository.delete(id)

            assertThat(repository.get(id)).isNull()
            assertThat(database.transactionDao().getById(recorded.transactionId)?.recurringId).isNull()
            repository.observeGeneratedTransactions(id, 10).test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeGeneratedTransactions orders newest first and respects the limit`() =
        runTest {
            seedAccountAndCategory()
            val id = repository.save(payment())
            repository.completeOccurrence(id, due, occurrence(due), LocalDate.of(2026, 11, 5))
            repository.completeOccurrence(id, LocalDate.of(2026, 11, 5), occurrence(LocalDate.of(2026, 11, 5)), next)
            repository.completeOccurrence(id, next, occurrence(next), LocalDate.of(2026, 12, 5))

            repository.observeGeneratedTransactions(id, 2).test {
                val list = awaitItem()
                assertThat(list.map { it.transaction.date })
                    .containsExactly(next, LocalDate.of(2026, 11, 5))
                    .inOrder()
                assertThat(list.first().accountName).isEqualTo("Ahorros")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `countByAccount and countByCategory reflect saved payments`() =
        runTest {
            seedAccountAndCategory()
            val otherCategoryId =
                database.categoryDao().insert(
                    CategoryEntity(name = "Other", type = CategoryType.EXPENSE, color = 2, icon = "x"),
                )
            repository.save(payment())
            repository.save(payment().copy(name = "Spotify", categoryId = otherCategoryId))

            assertThat(repository.countByAccount(accountId)).isEqualTo(2)
            assertThat(repository.countByAccount(999L)).isEqualTo(0)
            assertThat(repository.countByCategory(categoryId)).isEqualTo(1)
            assertThat(repository.countByCategory(otherCategoryId)).isEqualTo(1)
        }

    @Test
    fun `reassignAndDelete moves recurring payments with the transactions`() =
        runTest {
            seedAccountAndCategory()
            val targetId =
                categoryRepository.saveCategory(
                    Category(name = "Servicios", type = CategoryType.EXPENSE, color = 2, icon = "settings"),
                )
            val paymentId = repository.save(payment())
            val txId =
                database.transactionDao().insert(
                    occurrence().toEntity().copy(recurringId = paymentId),
                )

            categoryRepository.reassignAndDelete(fromCategoryId = categoryId, toCategoryId = targetId)

            assertThat(categoryRepository.getCategory(categoryId)).isNull()
            assertThat(repository.get(paymentId)?.categoryId).isEqualTo(targetId)
            assertThat(database.transactionDao().getById(txId)?.categoryId).isEqualTo(targetId)
        }
}
