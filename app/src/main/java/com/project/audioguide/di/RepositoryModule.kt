package com.project.audioguide.di

import com.project.audioguide.data.repository.SightRepositoryImpl
import com.project.audioguide.domain.repository.SightRepository
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
    abstract fun bindSightRepository(impl: SightRepositoryImpl): SightRepository
}