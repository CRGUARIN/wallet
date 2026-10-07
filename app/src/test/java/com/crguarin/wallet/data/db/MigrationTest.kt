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
 * Verifies that [MIGRATION_1_2], [MIGRATION_2_3] and [MIGRATION_3_4] preserve every row of the
 * older databases, that the full 1→2→3→4 chain works, and that the migrated databases open
 * normally with Room.
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

    private fun seedVersion2(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO accounts (name, type, currency_code, initial_balance, color, icon, " +
                "include_in_total, is_archived, sort_order, created_at) " +
                "VALUES ('Efectivo', 'CASH', 'COP', 100000, 1, 'wallet', 1, 0, 0, $createdAt)",
        )
        db.execSQL(
            "INSERT INTO accounts (name, type, currency_code, initial_balance, color, icon, " +
                "include_in_total, is_archived, sort_order, created_at) " +
                "VALUES ('Ahorros', 'SAVINGS', 'COP', 1000000, 2, 'bank', 1, 0, 1, $createdAt)",
        )
        db.execSQL(
            "INSERT INTO accounts (name, type, currency_code, initial_balance, color, icon, " +
                "include_in_total, is_archived, sort_order, created_at, credit_limit, statement_day, " +
                "payment_due_day) " +
                "VALUES ('Visa', 'CREDIT_CARD', 'COP', 0, 3, 'card', 1, 0, 2, $createdAt, 5000000, 25, 15)",
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
            "INSERT INTO categories (name, system_key, type, parent_id, color, icon, is_archived, sort_order) " +
                "VALUES ('Suscripciones', NULL, 'EXPENSE', NULL, 4, 'subscriptions', 0, 2)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at, installments) " +
                "VALUES ('INCOME', 500000, 2, NULL, 3, ${LocalDate.of(2026, 9, 1).toEpochDay()}, 'Sueldo', " +
                "$createdAt, $createdAt, NULL)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at, installments) " +
                "VALUES ('EXPENSE', 45900, 3, NULL, 4, ${LocalDate.of(2026, 9, 10).toEpochDay()}, 'Laptop', " +
                "$createdAt, $createdAt, 3)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at, installments) " +
                "VALUES ('TRANSFER', 200000, 1, 2, NULL, ${LocalDate.of(2026, 9, 12).toEpochDay()}, 'Ahorro', " +
                "$createdAt, $createdAt, NULL)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at, installments) " +
                "VALUES ('EXPENSE', 30000, 1, NULL, 2, ${LocalDate.of(2026, 9, 15).toEpochDay()}, 'Mercado', " +
                "$createdAt, $createdAt, NULL)",
        )
        db.execSQL(
            "INSERT INTO transactions (type, amount, account_id, to_account_id, category_id, date, note, " +
                "created_at, updated_at, installments) " +
                "VALUES ('EXPENSE', 5000, 3, NULL, 4, ${LocalDate.of(2026, 9, 20).toEpochDay()}, " +
                "'Ajuste extracto', $createdAt, $createdAt, NULL)",
        )
        db.execSQL(
            "INSERT INTO card_statements (account_id, closing_date, due_date, statement_balance, " +
                "minimum_payment, stated_interest_fees, adjustment_transaction_id, created_at) " +
                "VALUES (3, ${LocalDate.of(2026, 8, 25).toEpochDay()}, ${LocalDate.of(2026, 9, 15).toEpochDay()}, " +
                "100000, 10000, 2000, 5, $createdAt)",
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
                    .addMigrations(*ALL_MIGRATIONS)
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

    @Test
    fun migrate2To3_preservesAllDataAndStartsRecurringEmpty() {
        helper.createDatabase(TEST_DB_V3, 2).apply {
            seedVersion2(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_V3, 3, true, MIGRATION_2_3)

        // Accounts keep every value, including the credit card columns.
        db.query("SELECT * FROM accounts ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(3)
            cursor.moveToFirst()
            assertThat(cursor.readString("name")).isEqualTo("Efectivo")
            assertThat(cursor.readString("type")).isEqualTo("CASH")
            assertThat(cursor.readLong("initial_balance")).isEqualTo(100_000L)
            cursor.moveToNext()
            assertThat(cursor.readString("name")).isEqualTo("Ahorros")
            assertThat(cursor.readLong("initial_balance")).isEqualTo(1_000_000L)
            cursor.moveToNext()
            assertThat(cursor.readString("name")).isEqualTo("Visa")
            assertThat(cursor.readString("type")).isEqualTo("CREDIT_CARD")
            assertThat(cursor.readLong("initial_balance")).isEqualTo(0L)
            assertThat(cursor.readLong("credit_limit")).isEqualTo(5_000_000L)
            assertThat(cursor.readLong("statement_day")).isEqualTo(25L)
            assertThat(cursor.readLong("payment_due_day")).isEqualTo(15L)
        }

        // Categories keep the root/sub hierarchy, including the custom one.
        db.query("SELECT name, system_key, parent_id FROM categories ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(4)
            val rows = mutableListOf<Triple<String?, String?, Long?>>()
            while (cursor.moveToNext()) {
                val parentIndex = cursor.getColumnIndexOrThrow("parent_id")
                rows +=
                    Triple(
                        cursor.readString("name"),
                        cursor.readString("system_key"),
                        if (cursor.isNull(parentIndex)) null else cursor.getLong(parentIndex),
                    )
            }
            assertThat(rows).containsExactly(
                Triple(null, "food", null),
                Triple(null, "groceries", 1L),
                Triple(null, "salary", null),
                Triple("Suscripciones", null, null),
            )
        }

        // Transactions keep type, amount, accounts, category, date, note and installments.
        db
            .query(
                "SELECT type, amount, account_id, to_account_id, category_id, date, note, installments " +
                    "FROM transactions ORDER BY id",
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(5)
                cursor.moveToFirst()
                assertThat(cursor.readString("type")).isEqualTo("INCOME")
                assertThat(cursor.readLong("amount")).isEqualTo(500_000L)
                assertThat(cursor.readLong("account_id")).isEqualTo(2L)
                assertThat(cursor.readLong("category_id")).isEqualTo(3L)
                assertThat(cursor.readLong("date")).isEqualTo(LocalDate.of(2026, 9, 1).toEpochDay())
                assertThat(cursor.readString("note")).isEqualTo("Sueldo")
                cursor.moveToNext()
                assertThat(cursor.readString("type")).isEqualTo("EXPENSE")
                assertThat(cursor.readLong("amount")).isEqualTo(45_900L)
                assertThat(cursor.readLong("account_id")).isEqualTo(3L)
                assertThat(cursor.readLong("category_id")).isEqualTo(4L)
                assertThat(cursor.readLong("installments")).isEqualTo(3L)
                cursor.moveToNext()
                assertThat(cursor.readString("type")).isEqualTo("TRANSFER")
                assertThat(cursor.readLong("amount")).isEqualTo(200_000L)
                assertThat(cursor.readLong("account_id")).isEqualTo(1L)
                assertThat(cursor.readLong("to_account_id")).isEqualTo(2L)
                assertThat(cursor.readString("note")).isEqualTo("Ahorro")
                cursor.moveToNext()
                assertThat(cursor.readString("type")).isEqualTo("EXPENSE")
                assertThat(cursor.readLong("amount")).isEqualTo(30_000L)
                assertThat(cursor.readLong("category_id")).isEqualTo(2L)
                assertThat(cursor.readString("note")).isEqualTo("Mercado")
                cursor.moveToNext()
                assertThat(cursor.readString("type")).isEqualTo("EXPENSE")
                assertThat(cursor.readLong("amount")).isEqualTo(5_000L)
                assertThat(cursor.readLong("account_id")).isEqualTo(3L)
                assertThat(cursor.readString("note")).isEqualTo("Ajuste extracto")
            }

        // Every transaction has NULL recurring_id.
        db.query("SELECT recurring_id FROM transactions").use { cursor ->
            assertThat(cursor.count).isEqualTo(5)
            while (cursor.moveToNext()) {
                assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("recurring_id"))).isTrue()
            }
        }

        // The card statement keeps its adjustment link.
        db.query("SELECT * FROM card_statements").use { cursor ->
            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.readLong("account_id")).isEqualTo(3L)
            assertThat(cursor.readLong("closing_date")).isEqualTo(LocalDate.of(2026, 8, 25).toEpochDay())
            assertThat(cursor.readLong("due_date")).isEqualTo(LocalDate.of(2026, 9, 15).toEpochDay())
            assertThat(cursor.readLong("statement_balance")).isEqualTo(100_000L)
            assertThat(cursor.readLong("minimum_payment")).isEqualTo(10_000L)
            assertThat(cursor.readLong("stated_interest_fees")).isEqualTo(2_000L)
            assertThat(cursor.readLong("adjustment_transaction_id")).isEqualTo(5L)
        }

        // recurring_payments exists and starts empty.
        db.query("SELECT COUNT(*) AS count FROM recurring_payments").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.readLong("count")).isEqualTo(0L)
        }

        // Every v3 index exists with its exact generated name.
        db
            .query(
                "SELECT name FROM sqlite_master WHERE type = 'index' " +
                    "AND tbl_name IN ('transactions', 'recurring_payments')",
            ).use { cursor ->
                val names = mutableListOf<String?>()
                while (cursor.moveToNext()) {
                    names += cursor.readString("name")
                }
                assertThat(names).containsExactly(
                    "index_transactions_account_id",
                    "index_transactions_to_account_id",
                    "index_transactions_category_id",
                    "index_transactions_date",
                    "index_transactions_recurring_id",
                    "index_recurring_payments_account_id",
                    "index_recurring_payments_category_id",
                    "index_recurring_payments_next_due_date",
                )
            }

        // Balances computed from the migrated rows match the pre-migration values.
        assertThat(balanceOf(db, 1)).isEqualTo(-130_000L)
        assertThat(balanceOf(db, 2)).isEqualTo(1_700_000L)
        assertThat(balanceOf(db, 3)).isEqualTo(-50_900L)

        db.close()
    }

    @Test
    fun migrate1To2To3_chainPreservesVersion1Data() {
        helper.createDatabase(TEST_DB_CHAIN, 1).apply {
            seedVersion1(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_CHAIN, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        db.query("SELECT type, amount, note FROM transactions ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(3)
            cursor.moveToFirst()
            assertThat(cursor.readString("type")).isEqualTo("INCOME")
            assertThat(cursor.readLong("amount")).isEqualTo(200_000L)
            cursor.moveToNext()
            assertThat(cursor.readString("type")).isEqualTo("EXPENSE")
            assertThat(cursor.readLong("amount")).isEqualTo(30_000L)
            cursor.moveToNext()
            assertThat(cursor.readString("type")).isEqualTo("TRANSFER")
            assertThat(cursor.readLong("amount")).isEqualTo(50_000L)
        }
        db.query("SELECT recurring_id FROM transactions").use { cursor ->
            while (cursor.moveToNext()) {
                assertThat(cursor.isNull(cursor.getColumnIndexOrThrow("recurring_id"))).isTrue()
            }
        }
        db.query("SELECT COUNT(*) AS count FROM recurring_payments").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.readLong("count")).isEqualTo(0L)
        }
        db.query("SELECT COUNT(*) AS count FROM card_statements").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.readLong("count")).isEqualTo(0L)
        }
        assertThat(balanceOf(db, 1)).isEqualTo(20_000L)
        assertThat(balanceOf(db, 2)).isEqualTo(750_000L)

        db.close()
    }

    @Test
    fun migratedTo3Database_opensWithRoomAndReadsWithDaos() =
        runTest {
            helper.createDatabase(TEST_DB_V3_ROOM, 2).apply {
                seedVersion2(this)
                close()
            }

            val context = ApplicationProvider.getApplicationContext<Context>()
            val database =
                Room
                    .databaseBuilder(context, WalletDatabase::class.java, TEST_DB_V3_ROOM)
                    .addMigrations(*ALL_MIGRATIONS)
                    .allowMainThreadQueries()
                    .build()
            try {
                val cash = database.accountDao().getAccountWithBalanceById(1)
                val savings = database.accountDao().getAccountWithBalanceById(2)
                val card = database.accountDao().getAccountWithBalanceById(3)
                assertThat(cash?.balance).isEqualTo(-130_000L)
                assertThat(savings?.balance).isEqualTo(1_700_000L)
                assertThat(card?.balance).isEqualTo(-50_900L)
                assertThat(card?.account?.creditLimit).isEqualTo(5_000_000L)

                val installmentTx = database.transactionDao().getById(2)
                assertThat(installmentTx?.installments).isEqualTo(3)
                assertThat(installmentTx?.recurringId).isNull()

                val statement =
                    database.cardStatementDao().getByAccountAndClosing(3, LocalDate.of(2026, 8, 25))
                assertThat(statement?.statementBalance).isEqualTo(100_000L)
                assertThat(statement?.adjustmentTransactionId).isEqualTo(5L)

                assertThat(database.recurringPaymentDao().getAll()).isEmpty()
            } finally {
                database.close()
            }
        }

    private fun transactionIndexNames(db: androidx.sqlite.db.SupportSQLiteDatabase): List<String> {
        val names = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'transactions'").use { cursor ->
            while (cursor.moveToNext()) {
                names += cursor.getString(0)
            }
        }
        return names
    }

    @Test
    fun migrate3To4_preservesAllDataAndReplacesTheAccountIndices() {
        // Version 3 only added nullable columns and a table to version 2, so the version-2 rows fit.
        helper.createDatabase(TEST_DB_V4, 3).apply {
            seedVersion2(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_V4, 4, true, MIGRATION_3_4)

        val indexNames = transactionIndexNames(db)
        assertThat(indexNames).containsAtLeast(
            "index_transactions_account_id_type_date_amount",
            "index_transactions_to_account_id_type_date_amount",
        )
        assertThat(indexNames).containsNoneOf("index_transactions_account_id", "index_transactions_to_account_id")
        assertThat(balanceOf(db, 1)).isEqualTo(-130_000L)
        assertThat(balanceOf(db, 2)).isEqualTo(1_700_000L)
        assertThat(balanceOf(db, 3)).isEqualTo(-50_900L)

        db.close()
    }

    @Test
    fun migrate1To4_chainPreservesVersion1Data() {
        helper.createDatabase(TEST_DB_CHAIN_V4, 1).apply {
            seedVersion1(this)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB_CHAIN_V4, 4, true, *ALL_MIGRATIONS)

        assertThat(balanceOf(db, 1)).isEqualTo(20_000L)
        assertThat(balanceOf(db, 2)).isEqualTo(750_000L)

        db.close()
    }

    @Test
    fun migratedTo4Database_readsBalancesWithDaos() =
        runTest {
            helper.createDatabase(TEST_DB_V4_ROOM, 3).apply {
                seedVersion2(this)
                close()
            }

            val context = ApplicationProvider.getApplicationContext<Context>()
            val database =
                Room
                    .databaseBuilder(context, WalletDatabase::class.java, TEST_DB_V4_ROOM)
                    .addMigrations(*ALL_MIGRATIONS)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertThat(database.accountDao().getAccountWithBalanceById(1)?.balance).isEqualTo(-130_000L)
                assertThat(database.accountDao().getAccountWithBalanceById(2)?.balance).isEqualTo(1_700_000L)
                assertThat(database.accountDao().getAccountWithBalanceById(3)?.balance).isEqualTo(-50_900L)
            } finally {
                database.close()
            }
        }

    private companion object {
        const val TEST_DB = "migration-test"
        const val TEST_DB_ROOM = "migration-test-room"
        const val TEST_DB_V3 = "migration-test-v3"
        const val TEST_DB_CHAIN = "migration-test-chain"
        const val TEST_DB_V3_ROOM = "migration-test-v3-room"
        const val TEST_DB_V4 = "migration-test-v4"
        const val TEST_DB_CHAIN_V4 = "migration-test-chain-v4"
        const val TEST_DB_V4_ROOM = "migration-test-v4-room"
    }
}
