package com.example.manualfocusmacrocamera.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Data class representing user preferences.
 * This class corresponds to the structure defined in user_settings.proto.
 */
data class UserPreferences(
    val isInitialLightOn: Boolean = false,
    val isSaveGpsLocation: Boolean = false,
    val quality: Quality = Quality.HIGH,
    val aspect: AspectRatio = AspectRatio.FOUR_THREE
) {
    /**
     * Enum representing image quality options.
     * Corresponds to ImageQuality enum in user_settings.proto.
     */
    enum class Quality {
        HIGH, MIDDLE, LOW;
        
        companion object {
            fun fromInt(value: Int): Quality = when(value) {
                0 -> HIGH
                1 -> MIDDLE
                2 -> LOW
                else -> HIGH
            }
            
            fun toInt(quality: Quality): Int = when(quality) {
                HIGH -> 0
                MIDDLE -> 1
                LOW -> 2
            }
        }
    }

    /**
     * Enum representing aspect ratio options.
     * Corresponds to Aspect enum in user_settings.proto.
     */
    enum class AspectRatio {
        FOUR_THREE, SIXTEEN_NINE;
        
        companion object {
            fun fromInt(value: Int): AspectRatio = when(value) {
                0 -> FOUR_THREE
                1 -> SIXTEEN_NINE
                else -> FOUR_THREE
            }
            
            fun toInt(aspect: AspectRatio): Int = when(aspect) {
                FOUR_THREE -> 0
                SIXTEEN_NINE -> 1
            }
        }
    }
}
