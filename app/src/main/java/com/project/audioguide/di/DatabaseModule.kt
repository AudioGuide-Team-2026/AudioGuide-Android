package com.project.audioguide.di

import android.content.Context
import androidx.room.Room
import com.project.audioguide.data.database.SightDao
import com.project.audioguide.data.database.SightDatabase
import com.project.audioguide.data.remote.ApiFactory
import com.project.audioguide.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // 1. Говорим Hilt, как создать Базу Данных
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SightDatabase {
        return Room.databaseBuilder(
            context,
            SightDatabase::class.java,
            "sights_database" // Название БД (можешь оставить такое)
        ).build()
    }

    // 2. Говорим Hilt, как достать DAO из Базы Данных
    @Provides
    fun provideSightDao(database: SightDatabase): SightDao {
        return database.sightDao() // Убедись, что в SightDatabase есть метод sightDao()
    }

    // 3. Говорим Hilt, как создать ApiService
    @Provides
    @Singleton
    fun provideApiService(apiFactory: ApiFactory): ApiService {
        return apiFactory.sightsApi
    }
}