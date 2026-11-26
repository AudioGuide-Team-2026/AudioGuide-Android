package com.project.audioguide

import com.project.audioguide.di.RepositoryModule
import com.project.audioguide.di.ServiceModule
import com.project.audioguide.domain.repository.SightRepository
import com.project.audioguide.domain.service.CompassService
import com.project.audioguide.domain.service.LocationService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/** В тестах вместо настоящего GPS, компаса и репозитория подставляются заглушки. */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class, ServiceModule::class]
)
object TestModule {
    @Provides
    fun provideRepository(fake: FakeSightRepository): SightRepository = fake

    @Provides
    fun provideLocationService(fake: FakeLocationService): LocationService = fake

    @Provides
    fun provideCompassService(fake: FakeCompassService): CompassService = fake
}
