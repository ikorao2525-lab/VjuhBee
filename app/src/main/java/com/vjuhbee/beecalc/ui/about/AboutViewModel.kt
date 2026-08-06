package com.vjuhbee.beecalc.ui.about

import androidx.lifecycle.ViewModel
import com.vjuhbee.beecalc.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AboutUiState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val storeChannel: String = BuildConfig.STORE_CHANNEL,
    val showTips: Boolean = BuildConfig.SHOW_TIPS,
    val tipsUrl: String = BuildConfig.TIPS_URL,
    val isBeta: Boolean = BuildConfig.IS_BETA
)

class AboutViewModel : ViewModel() {

    val uiState: StateFlow<AboutUiState> =
        MutableStateFlow(AboutUiState()).asStateFlow()
}
