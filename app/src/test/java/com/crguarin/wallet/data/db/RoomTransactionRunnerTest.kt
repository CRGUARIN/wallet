package com.crguarin.wallet.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crguarin.wallet.domain.model.AccountType
import com.google.common.truth.Truth.assertThat
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
class RoomTransactionRunnerTest {
    private lateinit var database: WalletDatabase
    private lateinit var runner: RoomTransactionRunner

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        runner = RoomTransactionRunner(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun account(name: String): AccountEntity =
        AccountEntity(
            name = name,
            type = AccountType.CASH,
            currencyCode = "COP",
            initialBalance = 0L,
            color = -1,
            icon = "wallet",
            createdAt = Instant.ofEpochMilli(1000L),
        )

    @Test
    fun `keeps every write of the block`() =
        runTest {
            val ids =
                runner.run {
                    listOf(database.accountDao().insert(account("A")), database.accountDao().insert(account("B")))
                }

            assertThat(database.accountDao().getAll().map { it.id }).containsExactlyElementsIn(ids)
        }

    @Test
    fun `discards every write when the block fails`() =
        runTest {
            val result =
                runCatching {
                    runner.run<Unit> {
                        database.accountDao().insert(account("A"))
                        error("boom")
                    }
                }

            assertThat(result.isFailure).isTrue()
            assertThat(database.accountDao().getAll()).isEmpty()
        }

    @Test
    fun `nested blocks join the outer transaction`() =
        runTest {
            val result =
                runCatching {
                    runner.run<Unit> {
                        runner.run { database.accountDao().insert(account("A")) }
                        error("boom")
                    }
                }

            assertThat(result.isFailure).isTrue()
            assertThat(database.accountDao().getAll()).isEmpty()
        }
}
