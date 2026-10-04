package com.crguarin.wallet.data.db

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Verifies that [MIGRATION_1_2] preserves every row of a version-1 database and that the
 * migrated database opens normally with Room.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WalletDatabase::class.java,
        )

    private val date1 = LocalDate.of(2026, 9, 5)
    private val date2 = LocalDate.of(2026, 9, 6)
    private val date3 = LocalDate.of(2026, 9, 7)
    private val createdAt = 1_758_000_000_000L

    private fun seedVersion1(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO accounts (name, type, currency_code, initial_balance, color, icon, " +
                "include_in_total, is_archived, sort_order, created_at) " +
                "VALUES ('Efectivo', 'CASH', 'COP', 100000, 1, 'wallet', 1, 0, 0, $createdAt)",
        )
        db.execSQL(
            "INSERT INTO accounts (name, type, currency_code, initial_balance, color, icon, " +
                "include_in_total, is_archived, sort_order, created_at) " +
                "VALUES ('Ahorros', 'SAVINGS', 'COP', 500000, 2, 'bank', 1, 0, 1, $createdAt)",
        )
        db.execSQL(
            "INSERT INTO categories (name, system_key, type, parent_id, color, icon, is_archived, sort_order) " +
                "VALUES (NULL, 'food', 'EXPENSE', NULL, 1, 'restaurant', 0, 0)",
        )
        db.execSQL(
            "INSERT INTO categories (name, system_key, type, parent_id, color, icon, is_archived, sort_order) " +
                "VALUES (NULL, 'groceries', 'EXPENSE', 1, 1, 'local_grocery_store', 0, 0)",
        )
        db.execSQL(
            "INSERT INTO categories (name, system_key, type, parent_id, color, icon, is_archived, sort_order) " +
                "VALUES (NULL, 'salary', 'INCOME', NULL, 3, 'payments', 0, 1)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at) " +
                "VALUES ('INCOME', 200000, 2, NULL, 3, ${date1.toEpochDay()}, 'Sueldo', $createdAt, $createdAt)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at) " +
                "VALUES ('EXPENSE', 30000, 1, NULL, 2, ${date2.toEpochDay()}, 'Mercado', $createdAt, $createdAt)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at) " +
                "VALUES ('TRANSFER', 50000, 1, 2, NULL, ${date3.toEpochDay()}, 'Ahorro', $createdAt, $createdAt)",
        )
    }

    private fun Cursor.readLong(name: String): Long = getLong(getColumnIndexOrThrow(name))

    private fun Cursor.readString(name: String): String? {
        val index = getColumnIndexOrThrow(name)
        return if (isNull(index)) null else getString(index)
    }

    @Test
    fun migrate1To2_preservesAllDataAndAddsNullColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            seedVersion1(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Accounts keep every value; new card columns are NULL.
        db.query("SELECT * FROM accounts ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(2)
            cursor.moveToFirst()
            assertThat(cursor.readString("name")).isEqualTo("Efectivo")
            assertThat(cursor.readString("type")).isEqualTo("CASH")
            assertThat(cursor.readLong("initial_balance")).isEqualTo(100_000L)
            cursor.moveToNext()
            assertThat(cursor.readString("name")).isEqualTo("Ahorros")
            assertThat(cursor.readLong("initial_balance")).isEqualTo(500_000L)
        }
        db.query("SELECT credit_limit, statement_day, payment_due_day FROM accounts").use { cursor ->
            while (cursor.moveToNext()) {
                assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("credit_limit"))).isTrue()
                assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("statement_day"))).isTrue()
                assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("payment_due_day"))).isTrue()
            }
        }

        // Categories keep the root/sub hierarchy.
        db.query("SELECT system_key, parent_id FROM categories ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(3)
            val rows = mutableListOf<Pair<String?, Long?>>()
            while (cursor.moveToNext()) {
                val parentIndex = cursor.getColumnIndexOrThrow("parent_id")
                rows += cursor.readString("system_key") to
                    if (cursor.isNull(parentIndex)) null else cursor.getLong(parentIndex)
            }
            assertThat(rows).containsExactly(
                "food" to null,
                "groceries" to 1L,
                "salary" to null,
            )
        }

        // Transactions keep type, amount, accounts, category, date and note.
        db.query("SELECT type, amount, account_id, to_account_id, category_id, date, note FROM transactions ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(3)
            cursor.moveToFirst()
            assertThat(cursor.readString("type")).isEqualTo("INCOME")
            assertThat(cursor.readLong("amount")).isEqualTo(200_000L)
            assertThat(cursor.readLong("account_id")).isEqualTo(2L)
            assertThat(cursor.readString("note")).isEqualTo("Sueldo")
            cursor.moveToNext()
            assertThat(cursor.readString("type")).isEqualTo("EXPENSE")
            assertThat(cursor.readLong("amount")).isEqualTo(30_000L)
            assertThat(cursor.readString("note")).isEqualTo("Mercado")
            cursor.moveToNext()
            assertThat(cursor.readString("type")).isEqualTo("TRANSFER")
            assertThat(cursor.readLong("amount")).isEqualTo(50_000L)
            assertThat(cursor.readLong("account_id")).isEqualTo(1L)
            assertThat(cursor.readLong("to_account_id")).isEqualTo(2L)
            assertThat(cursor.readString("note")).isEqualTo("Ahorro")
        }
        db.query("SELECT installments FROM transactions").use { cursor ->
            while (cursor.moveToNext()) {
                assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("installments"))).isTrue()
            }
        }

        // card_statements exists and starts empty.
        db.query("SELECT COUNT(*) AS count FROM card_statements").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.readLong("count")).isEqualTo(0L)
        }

        // Balances computed from the migrated rows match the pre-migration values.
        assertThat(balanceOf(db, 1)).isEqualTo(20_000L)
        assertThat(balanceOf(db, 2)).isEqualTo(750_000L)

        db.close()
    }

    private fun balanceOf(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        accountId: Long,
    ): Long {
        var balance = 0L
        db.query("SELECT initial_balance FROM accounts WHERE id = $accountId").use { cursor ->
            cursor.moveToFirst()
            balance = cursor.readLong("initial_balance")
        }
        val txQuery =
            "SELECT type, amount, account_id FROM transactions " +
                "WHERE account_id = $accountId OR to_account_id = $accountId"
        db.query(txQuery).use { tx ->
            while (tx.moveToNext()) {
                val amount = tx.readLong("amount")
                when (tx.readString("type")) {
                    "INCOME" -> balance += amount
                    "EXPENSE" -> balance -= amount
                    "TRANSFER" ->
                        if (tx.readLong("account_id") == accountId) {
                            balance -= amount
                        } else {
                            balance += amount
                        }
                }
            }
        }
        return balance
    }

    @Test
    fun migratedDatabase_opensWithRoomAndReadsAccountsWithDao() =
        runTest {
            helper.createDatabase(TEST_DB_ROOM, 1).apply {
                seedVersion1(this)
                close()
            }

            val context = ApplicationProvider.getApplicationContext<Context>()
            val database =
                Room
                    .databaseBuilder(context, WalletDatabase::class.java, TEST_DB_ROOM)
                    .addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .build()
            try {
                val accounts = database.accountDao().getAll()
                assertThat(accounts).hasSize(2)
                val cash = database.accountDao().getAccountWithBalanceById(1)
                val savings = database.accountDao().getAccountWithBalanceById(2)
                assertThat(cash?.balance).isEqualTo(20_000L)
                assertThat(savings?.balance).isEqualTo(750_000L)
                assertThat(cash?.account?.creditLimit).isNull()
                assertThat(cash?.account?.statementDay).isNull()
                assertThat(cash?.account?.paymentDueDay).isNull()
            } finally {
                database.close()
            }
        }

    private companion object {
        const val TEST_DB = "migration-test"
        const val TEST_DB_ROOM = "migration-test-room"
    }
}
