package com.crguarin.wallet.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema 1 to schema 2 (credit cards). The `card_statements` CREATE TABLE
 * and index statements below are copied verbatim from the generated
 * `app/schemas/com.crguarin.wallet.data.db.WalletDatabase/2.json`.
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN credit_limit INTEGER")
            db.execSQL("ALTER TABLE accounts ADD COLUMN statement_day INTEGER")
            db.execSQL("ALTER TABLE accounts ADD COLUMN payment_due_day INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN installments INTEGER")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `card_statements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account_id` INTEGER NOT NULL, `closing_date` INTEGER NOT NULL, `due_date` INTEGER NOT NULL, `statement_balance` INTEGER NOT NULL, `minimum_payment` INTEGER, `stated_interest_fees` INTEGER, `adjustment_transaction_id` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`adjustment_transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_card_statements_account_id_closing_date` ON `card_statements` (`account_id`, `closing_date`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_card_statements_adjustment_transaction_id` ON `card_statements` (`adjustment_transaction_id`)",
            )
        }
    }

/**
 * Migration from schema 2 to schema 3 (recurring payments). The `recurring_payments`
 * CREATE TABLE and index statements below are copied verbatim from the generated
 * `app/schemas/com.crguarin.wallet.data.db.WalletDatabase/3.json`.
 */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurring_id INTEGER")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recurring_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `account_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL, `note` TEXT, `frequency` TEXT NOT NULL, `interval` INTEGER NOT NULL, `start_date` INTEGER NOT NULL, `next_due_date` INTEGER NOT NULL, `end_date` INTEGER, `mode` TEXT NOT NULL, `reminder_days_before` INTEGER, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recurring_payments_account_id` ON `recurring_payments` (`account_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recurring_payments_category_id` ON `recurring_payments` (`category_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recurring_payments_next_due_date` ON `recurring_payments` (`next_due_date`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_recurring_id` ON `transactions` (`recurring_id`)",
            )
        }
    }

/**
 * Migration from schema 3 to schema 4 (balance indices). The per-account indices on `transactions`
 * become covering indices for the balance sums; the CREATE INDEX statements below are copied
 * verbatim from `app/schemas/com.crguarin.wallet.data.db.WalletDatabase/4.json`.
 */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_transactions_account_id`")
            db.execSQL("DROP INDEX IF EXISTS `index_transactions_to_account_id`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_account_id_type_date_amount` ON `transactions` " +
                    "(`account_id`, `type`, `date`, `amount`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_to_account_id_type_date_amount` ON `transactions` " +
                    "(`to_account_id`, `type`, `date`, `amount`)",
            )
        }
    }

/**
 * Migration from schema 4 to schema 5 (debts). The `debts` and `debt_payments` CREATE TABLE and
 * index statements below are copied verbatim from the generated
 * `app/schemas/com.crguarin.wallet.data.db.WalletDatabase/5.json`. Debts are a standalone ledger,
 * so no existing table changes.
 */
val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `person_name` TEXT NOT NULL, `direction` TEXT NOT NULL, `amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, `due_date` INTEGER, `note` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `debt_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `debt_id` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, `note` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`debt_id`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_debt_payments_debt_id` ON `debt_payments` (`debt_id`)",
            )
        }
    }

/** Every migration, oldest first, for the database builder and the migration tests. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
