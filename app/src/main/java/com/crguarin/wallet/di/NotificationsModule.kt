package com.crguarin.wallet.di

import com.crguarin.wallet.domain.repository.DailyWorkScheduler
import com.crguarin.wallet.domain.repository.NotificationGateway
import com.crguarin.wallet.notifications.NoopDailyWorkScheduler
import com.crguarin.wallet.notifications.NoopNotificationGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provisional bindings: Task 16 (platform) replaces the no-ops with real implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    @Singleton
    abstract fun bindNotificationGateway(impl: NoopNotificationGateway): NotificationGateway

    @Binds
    @Singleton
    abstract fun bindDailyWorkScheduler(impl: NoopDailyWorkScheduler): DailyWorkScheduler
}
