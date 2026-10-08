package com.crguarin.wallet.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.domain.model.debt.DebtDirection
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
 * Covers [DebtDao]: the paid sums of each debt, the ordering of debts and payments, and the
 * CASCADE foreign key that removes the payments with their debt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DebtDaoTest {
    private lateinit var database: WalletDatabase
    private lateinit var debtDao: DebtDao

    private val now = Instant.parse("2026-10-01T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        debtDao = database.debtDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun debt(
        name: String = "Ana",
        direction: DebtDirection = DebtDirection.OWED_TO_ME,
        amount: Long = 100_000L,
        date: LocalDate = LocalDate.of(2026, 9, 1),
    ) = DebtEntity(
        personName = name,
        direction = direction,
        amount = amount,
        date = date,
        dueDate = LocalDate.of(2026, 11, 1),
        note = "Almuerzo",
        createdAt = now,
        updatedAt = now,
    )

    private fun payment(
        debtId: Long,
        amount: Long = 10_000L,
        date: LocalDate = LocalDate.of(2026, 9, 5),
    ) = DebtPaymentEntity(
        debtId = debtId,
        amount = amount,
        date = date,
        note = null,
        createdAt = now,
    )

    @Test
    fun getFirstPaymentDate_returnsTheOldestPaymentOfThatDebtOrNull() =
        runTest {
            val first = debtDao.insert(debt())
            val second = debtDao.insert(debt(name = "Luis"))
            debtDao.insertPayment(payment(first, date = LocalDate.of(2026, 9, 20)))
            debtDao.insertPayment(payment(first, date = LocalDate.of(2026, 9, 3)))
            debtDao.insertPayment(payment(second, date = LocalDate.of(2026, 9, 2)))

            assertThat(debtDao.getFirstPaymentDate(first)).isEqualTo(LocalDate.of(2026, 9, 3))
            assertThat(debtDao.getFirstPaymentDate(debtDao.insert(debt(name = "Eva")))).isNull()
            assertThat(debtDao.count()).isEqualTo(3)
        }

    @Test
    fun insertAndGet_roundTripsEveryField() =
        runTest {
            val id = debtDao.insert(debt())

            val stored = debtDao.getById(id)
            assertThat(stored?.personName).isEqualTo("Ana")
            assertThat(stored?.direction).isEqualTo(DebtDirection.OWED_TO_ME)
            assertThat(stored?.amount).isEqualTo(100_000L)
            assertThat(stored?.date).isEqualTo(LocalDate.of(2026, 9, 1))
            assertThat(stored?.dueDate).isEqualTo(LocalDate.of(2026, 11, 1))
            assertThat(stored?.note).isEqualTo("Almuerzo")
            assertThat(stored?.createdAt).isEqualTo(now)
            assertThat(stored?.updatedAt).isEqualTo(now)
        }

    @Test
    fun insertAndGet_keepsNullDueDateAndNote() =
        runTest {
            val id = debtDao.insert(debt().copy(dueDate = null, note = null))

            val stored = debtDao.getById(id)
            assertThat(stored?.dueDate).isNull()
            assertThat(stored?.note).isNull()
        }

    @Test
    fun paidIsZeroWithoutPayments() =
        runTest {
            val id = debtDao.insert(debt())

            assertThat(debtDao.getPaid(id)).isEqualTo(0L)
            debtDao.observeWithPaid(id).test {
                assertThat(awaitItem()?.paid).isEqualTo(0L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun paidSumsOnlyThePaymentsOfThatDebt() =
        runTest {
            val id = debtDao.insert(debt())
            val otherId = debtDao.insert(debt(name = "Luis"))
            debtDao.insertPayment(payment(id, amount = 10_000L))
            debtDao.insertPayment(payment(id, amount = 25_000L, date = LocalDate.of(2026, 9, 20)))
            debtDao.insertPayment(payment(otherId, amount = 99_000L))

            assertThat(debtDao.getPaid(id)).isEqualTo(35_000L)
            assertThat(debtDao.getPaid(otherId)).isEqualTo(99_000L)
            debtDao.observeAllWithPaid().test {
                val paidById = awaitItem().associate { it.debt.id to it.paid }
                assertThat(paidById).isEqualTo(mapOf(id to 35_000L, otherId to 99_000L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAllWithPaid_ordersByStartDateThenId() =
        runTest {
            debtDao.insert(debt(name = "Later", date = LocalDate.of(2026, 9, 10)))
            debtDao.insert(debt(name = "Sooner", date = LocalDate.of(2026, 9, 1)))

            debtDao.observeAllWithPaid().test {
                val list = awaitItem()
                assertThat(list.map { it.debt.personName }).containsExactly("Sooner", "Later").inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observePayments_ordersNewestFirstThenIdDescending() =
        runTest {
            val id = debtDao.insert(debt())
            val otherId = debtDao.insert(debt(name = "Luis"))
            debtDao.insertPayment(payment(id, amount = 1L, date = LocalDate.of(2026, 9, 5)))
            debtDao.insertPayment(payment(id, amount = 2L, date = LocalDate.of(2026, 9, 10)))
            debtDao.insertPayment(payment(id, amount = 3L, date = LocalDate.of(2026, 9, 5)))
            debtDao.insertPayment(payment(otherId, amount = 50L, date = LocalDate.of(2026, 9, 12)))

            debtDao.observePayments(id).test {
                val list = awaitItem()
                assertThat(list.map { it.amount }).containsExactly(2L, 3L, 1L).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeWithPaid_emitsNullForMissingDebt() =
        runTest {
            debtDao.observeWithPaid(999L).test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun update_keepsThePaymentsOfTheDebt() =
        runTest {
            val id = debtDao.insert(debt())
            debtDao.insertPayment(payment(id, amount = 10_000L))
            val stored = debtDao.getById(id)!!

            debtDao.update(stored.copy(amount = 120_000L, dueDate = null, note = null))

            val updated = debtDao.getById(id)
            assertThat(updated?.amount).isEqualTo(120_000L)
            assertThat(updated?.dueDate).isNull()
            assertThat(updated?.note).isNull()
            assertThat(debtDao.getPaid(id)).isEqualTo(10_000L)
        }

    @Test
    fun deleteDebt_cascadesToItsPaymentsOnly() =
        runTest {
            val id = debtDao.insert(debt())
            val otherId = debtDao.insert(debt(name = "Luis"))
            val paymentId = debtDao.insertPayment(payment(id))
            val otherPaymentId = debtDao.insertPayment(payment(otherId, amount = 5_000L))

            assertThat(debtDao.deleteById(id)).isEqualTo(1)

            assertThat(debtDao.getById(id)).isNull()
            assertThat(debtDao.getPaymentById(paymentId)).isNull()
            assertThat(debtDao.getPaid(id)).isEqualTo(0L)
            assertThat(debtDao.getPaymentById(otherPaymentId)?.amount).isEqualTo(5_000L)
        }

    @Test
    fun insertPayment_withUnknownDebt_failsByForeignKey() =
        runTest {
            var failed = false
            try {
                debtDao.insertPayment(payment(debtId = 999L))
            } catch (e: SQLiteConstraintException) {
                failed = true
            }
            assertThat(failed).isTrue()
        }
}
