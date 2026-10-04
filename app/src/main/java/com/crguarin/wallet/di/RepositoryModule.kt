package com.crguarin.wallet.di

import com.crguarin.wallet.data.category.AndroidSystemCategoryNames
import com.crguarin.wallet.data.prefs.PreferencesRepositoryImpl
import com.crguarin.wallet.data.repository.AccountRepositoryImpl
import com.crguarin.wallet.data.repository.CardStatementRepositoryImpl
import com.crguarin.wallet.data.repository.CategoryRepositoryImpl
import com.crguarin.wallet.data.repository.ReportRepositoryImpl
import com.crguarin.wallet.data.repository.TransactionRepositoryImpl
import com.crguarin.wallet.domain.model.SystemCategoryNames
import com.crguarin.wallet.domain.repository.AccountRepository
import com.crguarin.wallet.domain.repository.CardStatementRepository
import com.crguarin.wallet.domain.repository.CategoryRepository
import com.crguarin.wallet.domain.repository.PreferencesRepository
import com.crguarin.wallet.domain.repository.ReportRepository
import com.crguarin.wallet.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCardStatementRepository(impl: CardStatementRepositoryImpl): CardStatementRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSystemCategoryNames(impl: AndroidSystemCategoryNames): SystemCategoryNames

    @Binds
    @Singleton
    abstract fun bindCategorySeeder(
        impl: com.crguarin.wallet.data.seed.DefaultCategoriesSeeder,
    ): com.crguarin.wallet.domain.seed.CategorySeeder
}
