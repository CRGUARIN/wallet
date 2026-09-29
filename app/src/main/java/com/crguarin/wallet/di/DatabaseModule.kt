package com.crguarin.wallet.di

import android.content.Context
import androidx.room.Room
import com.crguarin.wallet.data.db.AccountDao
import com.crguarin.wallet.data.db.BackupDao
import com.crguarin.wallet.data.db.CategoryDao
import com.crguarin.wallet.data.db.TransactionDao
import com.crguarin.wallet.data.db.WalletDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "wallet.db"

    @Provides
    @Singleton
    fun provideWalletDatabase(
        @ApplicationContext context: Context,
    ): WalletDatabase =
        Room
            .databaseBuilder(
                context,
                WalletDatabase::class.java,
                DATABASE_NAME,
            ).build()

    @Provides
    fun provideAccountDao(database: WalletDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: WalletDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: WalletDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBackupDao(database: WalletDatabase): BackupDao = database.backupDao()
}
