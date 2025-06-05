package com.example.manualfocusmacrocamera.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.example.datastore.Aspect
import com.example.datastore.ImageQuality
import com.example.datastore.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Creates a DataStore<UserSettings> for the given context.
 */
fun Context.createUserSettingsDataStore(): DataStore<UserSettings> {
    return DataStoreFactory.create(
        serializer = UserSettingsSerializer,
        produceFile = { dataStoreFile("user_settings.pb") },
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { UserSettings.getDefaultInstance() }
        ),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    )
}

/**
 * Singleton instance of DataStore<UserSettings>.
 */
@Volatile
private var INSTANCE: DataStore<UserSettings>? = null

/**
 * Extension property to get or create a DataStore<UserSettings> using the Context.
 */
val Context.userSettingsDataStore: DataStore<UserSettings>
    get() = INSTANCE ?: synchronized(this) {
        INSTANCE ?: createUserSettingsDataStore().also { INSTANCE = it }
    }

/**
 * Repository for managing user preferences using Proto DataStore.
 */
class UserPreferencesProtoRepository(private val dataStore: DataStore<UserSettings>) {

    /**
     * Flow of UserSettings from the DataStore.
     */
    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(UserSettings.getDefaultInstance())
            } else {
                throw exception
            }
        }

    /**
     * Update the initial light setting.
     */
    suspend fun updateIsInitialLightOn(isInitialLightOn: Boolean) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setIsInitialLightOn(isInitialLightOn)
                .build()
        }
    }

    /**
     * Update the GPS location saving setting.
     */
    suspend fun updateIsSaveGpsLocation(isSaveGpsLocation: Boolean) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setIsSaveGpsLocation(isSaveGpsLocation)
                .build()
        }
    }

    /**
     * Update the image quality setting.
     */
    suspend fun updateQuality(quality: ImageQuality) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setQuality(quality)
                .build()
        }
    }

    /**
     * Update the aspect ratio setting.
     */
    suspend fun updateAspect(aspect: Aspect) {
        dataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setAspect(aspect)
                .build()
        }
    }

    /**
     * Convert from Proto UserSettings to domain UserPreferences.
     */
    fun toUserPreferences(userSettings: UserSettings): UserPreferences {
        return UserPreferences(
            isInitialLightOn = userSettings.isInitialLightOn,
            isSaveGpsLocation = userSettings.isSaveGpsLocation,
            quality = when (userSettings.quality) {
                ImageQuality.HIGH -> UserPreferences.Quality.HIGH
                ImageQuality.MIDDLE -> UserPreferences.Quality.MIDDLE
                ImageQuality.LOW -> UserPreferences.Quality.LOW
                else -> UserPreferences.Quality.HIGH
            },
            aspect = when (userSettings.aspect) {
                Aspect.Four_Three -> UserPreferences.AspectRatio.FOUR_THREE
                Aspect.SIXTEEN_NINE -> UserPreferences.AspectRatio.SIXTEEN_NINE
                else -> UserPreferences.AspectRatio.FOUR_THREE
            }
        )
    }

}
