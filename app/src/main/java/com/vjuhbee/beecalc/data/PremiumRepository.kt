package com.vjuhbee.beecalc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.vjuhbee.beecalc.BuildConfig
import com.vjuhbee.beecalc.model.PremiumState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "premium_prefs"
)

/**
 * Статус премиума (SPEC.md §4).
 * Пока без RuStore SDK: состояние хранится локально в DataStore.
 * В beta-сборке можно включить премиум тестовой кнопкой.
 * Позже сюда подключится реальная покупка «чашка кофе».
 */
interface PremiumRepository {
    fun observeState(): Flow<PremiumState>
    suspend fun setPremium(enabled: Boolean)

    /** Тестовый переключатель доступен только в beta (и для отладки доната). */
    val canToggleForTest: Boolean
}

class DataStorePremiumRepository(private val context: Context) : PremiumRepository {

    private val keyPremium = booleanPreferencesKey("is_premium")

    override val canToggleForTest: Boolean =
        BuildConfig.IS_BETA || BuildConfig.SHOW_DONATE

    override fun observeState(): Flow<PremiumState> =
        context.premiumDataStore.data.map { prefs ->
            if (prefs[keyPremium] == true) PremiumState.PREMIUM else PremiumState.FREE
        }

    override suspend fun setPremium(enabled: Boolean) {
        context.premiumDataStore.edit { prefs ->
            prefs[keyPremium] = enabled
        }
    }
}
