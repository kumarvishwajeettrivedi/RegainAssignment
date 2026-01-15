package com.example.regainassignment.di

import com.example.regainassignment.data.repository.UsageRepository
import com.example.regainassignment.data.repository.UsageRepositoryImpl
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
    abstract fun bindUsageRepository(
        usageRepositoryImpl: UsageRepositoryImpl
    ): UsageRepository
}
