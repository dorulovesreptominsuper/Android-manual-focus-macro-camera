package com.example.manualfocusmacrocamera.di

import android.content.Context
import com.example.manualfocusmacrocamera.data.UserPreferencesProtoRepository
import com.example.manualfocusmacrocamera.data.userSettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing DataStore related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides a singleton instance of UserPreferencesProtoRepository.
     */
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesProtoRepository {
        return UserPreferencesProtoRepository(context.userSettingsDataStore)
    }
}
