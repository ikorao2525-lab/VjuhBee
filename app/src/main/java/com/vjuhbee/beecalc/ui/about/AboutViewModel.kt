package com.vjuhbee.beecalc.ui.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.BuildConfig
import com.vjuhbee.beecalc.model.PremiumState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AboutUiState(
    val premiumState: PremiumState = PremiumState.FREE,
    val versionName: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val storeChannel: String = BuildConfig.STORE_CHANNEL,
    val showDonate: Boolean = BuildConfig.SHOW_DONATE,
    val isBeta: Boolean = BuildConfig.IS_BETA,
    val canTogglePremiumForTest: Boolean = false
)

class AboutViewModel(app: Application) : AndroidViewModel(app) {

    private val premiumRepository = (app as BeeCalcApp).premiumRepository

    val uiState: StateFlow<AboutUiState> =
        premiumRepository.observeState().map { premium ->
            AboutUiState(
                premiumState = premium,
                canTogglePremiumForTest = premiumRepository.canToggleForTest
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AboutUiState(
                canTogglePremiumForTest = premiumRepository.canToggleForTest
            )
        )

    fun setPremiumForTest(enabled: Boolean) {
        if (!premiumRepository.canToggleForTest) return
        viewModelScope.launch { premiumRepository.setPremium(enabled) }
    }
}
