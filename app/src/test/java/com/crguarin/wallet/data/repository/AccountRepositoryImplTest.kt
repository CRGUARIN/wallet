package com.crguarin.wallet.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.crguarin.wallet.core.money.Money
import com.crguarin.wallet.data.db.WalletDatabase
import com.crguarin.wallet.domain.model.Account
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountRepositoryImplTest {
    private lateinit var database: WalletDatabase
    private lateinit var repository: AccountRepositoryImpl

    private val now = Instant.parse("2026-09-28T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = AccountRepositoryImpl(database.accountDao(), database, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saveAccount inserts and updates successfully`() =
        runTest {
            val account =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money(15_000L),
                    color = -1,
                    icon = "wallet",
                    createdAt = now,
                )

            val id = repository.saveAccount(account)
            assertThat(id).isGreaterThan(0L)

            val loaded = repository.getAccount(id)
            assertThat(loaded).isNotNull()
            assertThat(loaded?.name).isEqualTo("Efectivo")
            assertThat(loaded?.initialBalance).isEqualTo(Money(15_000L))

            // Update
            val updated = loaded!!.copy(name = "Efectivo Actualizado")
            val updatedId = repository.saveAccount(updated)
            assertThat(updatedId).isEqualTo(id)

            val reloaded = repository.getAccount(id)
            assertThat(reloaded?.name).isEqualTo("Efectivo Actualizado")
        }

    @Test
    fun `deleteAccount removes account from database`() =
        runTest {
            val account =
                Account(
                    name = "Efectivo",
                    type = AccountType.CASH,
                    initialBalance = Money.ZERO,
                    color = -1,
                    icon = "wallet",
                    createdAt = now,
                )
            val id = repository.saveAccount(account)
            val saved = repository.getAccount(id)!!

            repository.deleteAccount(saved)
            assertThat(repository.getAccount(id)).isNull()
        }

    @Test
    fun `observeAccountsWithBalance filters archived accounts properly`() =
        runTest {
            val a1 =
                Account(
                    name = "Activa",
                    type = AccountType.CASH,
                    initialBalance = Money(500L),
                    color = -1,
                    icon = "wallet",
                    isArchived = false,
                    createdAt = now,
                )
            val a2 =
                Account(
                    name = "Archivada",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(1000L),
                    color = -1,
                    icon = "savings",
                    isArchived = true,
                    createdAt = now,
                )
            repository.saveAccount(a1)
            repository.saveAccount(a2)

            repository.observeAccountsWithBalance(includeArchived = false).test {
                val list = awaitItem()
                assertThat(list).hasSize(1)
                assertThat(list.first().account.name).isEqualTo("Activa")
                cancelAndIgnoreRemainingEvents()
            }

            repository.observeAccountsWithBalance(includeArchived = true).test {
                val list = awaitItem()
                assertThat(list).hasSize(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateSortOrders updates sort orders for multiple accounts`() =
        runTest {
            val a1Id =
                repository.saveAccount(
                    Account(name = "A1", type = AccountType.CASH, initialBalance = Money.ZERO, color = 1, icon = "w", createdAt = now),
                )
            val a2Id =
                repository.saveAccount(
                    Account(name = "A2", type = AccountType.CASH, initialBalance = Money.ZERO, color = 1, icon = "w", createdAt = now),
                )

            repository.updateSortOrders(listOf(a1Id to 5, a2Id to 10))

            assertThat(repository.getAccount(a1Id)?.sortOrder).isEqualTo(5)
            assertThat(repository.getAccount(a2Id)?.sortOrder).isEqualTo(10)
        }
}
