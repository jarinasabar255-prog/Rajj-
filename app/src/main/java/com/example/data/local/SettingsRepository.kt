package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.BotSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.botSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "finorix_bot_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_PAIR = stringPreferencesKey("selected_pair_symbol")
        val MIN_CONFIDENCE = intPreferencesKey("min_confidence_threshold")
        val AUTO_LOCK = booleanPreferencesKey("auto_lock_at_candle_open")
        val VOICE_CALLOUT = booleanPreferencesKey("voice_callout_enabled")
    }

    val settings: Flow<BotSettings> = context.botSettingsDataStore.data.map { prefs ->
        BotSettings(
            selectedPairSymbol = prefs[Keys.SELECTED_PAIR] ?: "EURUSD_OTC",
            minConfidenceThreshold = prefs[Keys.MIN_CONFIDENCE] ?: 78,
            autoLockAtCandleOpen = prefs[Keys.AUTO_LOCK] ?: true,
            voiceCalloutEnabled = prefs[Keys.VOICE_CALLOUT] ?: true
        )
    }

    suspend fun updateSelectedPair(symbol: String) {
        context.botSettingsDataStore.edit { it[Keys.SELECTED_PAIR] = symbol }
    }

    suspend fun updateMinConfidence(confidence: Int) {
        context.botSettingsDataStore.edit { it[Keys.MIN_CONFIDENCE] = confidence.coerceIn(65, 95) }
    }

    suspend fun updateAutoLock(enabled: Boolean) {
        context.botSettingsDataStore.edit { it[Keys.AUTO_LOCK] = enabled }
    }

    suspend fun updateVoiceCallout(enabled: Boolean) {
        context.botSettingsDataStore.edit { it[Keys.VOICE_CALLOUT] = enabled }
    }
}
