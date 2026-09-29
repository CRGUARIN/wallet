package com.crguarin.wallet.di

import com.crguarin.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersionName

@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {
    @Provides
    @AppVersionName
    fun provideAppVersionName(): String = BuildConfig.VERSION_NAME
}
