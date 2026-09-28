package com.crguarin.wallet.di

import com.crguarin.wallet.core.time.DefaultTimeProvider
import com.crguarin.wallet.core.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds
    @Singleton
    abstract fun bindTimeProvider(defaultTimeProvider: DefaultTimeProvider): TimeProvider
}
