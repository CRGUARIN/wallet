package com.crguarin.wallet.di

import com.crguarin.wallet.data.backup.BackupDocumentStoreImpl
import com.crguarin.wallet.data.backup.BackupRepositoryImpl
import com.crguarin.wallet.domain.repository.BackupDocumentStore
import com.crguarin.wallet.domain.repository.BackupRepository
import com.crguarin.wallet.ui.util.AppLanguageController
import com.crguarin.wallet.ui.util.LanguageHelperController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindBackupDocumentStore(impl: BackupDocumentStoreImpl): BackupDocumentStore

    @Binds
    abstract fun bindAppLanguageController(impl: LanguageHelperController): AppLanguageController
}
