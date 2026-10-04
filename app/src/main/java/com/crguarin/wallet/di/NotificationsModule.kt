package com.crguarin.wallet.di

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.WorkManager
import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import com.crguarin.wallet.domain.repository.NotificationGateway
import com.crguarin.wallet.notifications.AndroidNotificationGateway
import com.crguarin.wallet.notifications.AppLocaleSource
import com.crguarin.wallet.notifications.WorkManagerDailyScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    @Singleton
    abstract fun bindNotificationGateway(impl: AndroidNotificationGateway): NotificationGateway

    @Binds
    @Singleton
    abstract fun bindDailyWorkScheduler(impl: WorkManagerDailyScheduler): DailyWorkScheduler

    companion object {
        @Provides
        @Singleton
        @JvmStatic
        fun provideWorkManager(
            @ApplicationContext context: Context,
        ): WorkManager = WorkManager.getInstance(context)

        @Provides
        @Singleton
        @JvmStatic
        fun provideAppLocaleSource(): AppLocaleSource = AppLocaleSource { AppCompatDelegate.getApplicationLocales() }
    }
}
