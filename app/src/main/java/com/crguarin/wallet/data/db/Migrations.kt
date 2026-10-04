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
