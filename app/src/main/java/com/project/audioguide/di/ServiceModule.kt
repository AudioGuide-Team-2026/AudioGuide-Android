package com.project.audioguide.di

import com.project.audioguide.data.service.CompassServiceImpl
import com.project.audioguide.data.service.LocationServiceImpl
import com.project.audioguide.domain.service.CompassService
import com.project.audioguide.domain.service.LocationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindLocationService(locationServiceImpl: LocationServiceImpl): LocationService

    @Binds
    @Singleton
    abstract fun bindCompassService(compassServiceImpl: CompassServiceImpl): CompassService
}