package com.appvillis.nicegram.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.appvillis.lib_android_base.BaseViewModel
import com.appvillis.lib_android_base.SingleLiveEvent
import com.appvillis.lib_android_base.mvi.MviAction
import com.appvillis.lib_android_base.mvi.MviViewState
import com.appvillis.nicegram.domain.BillingManager
import com.appvillis.nicegram.domain.GetBillingSkusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NicegramPremiumViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val getBillingSkusUseCase: GetBillingSkusUseCase,
) : BaseViewModel<NicegramPremiumViewModel.ViewState, NicegramPremiumViewModel.Action>(
    ViewState(getBillingSkusUseCase.sub)
) {

    private val _eventCloseScreen = SingleLiveEvent<Unit>()
    val eventCloseScreen: LiveData<Unit> get() = _eventCloseScreen

    init {
        viewModelScope.launch {
            getBillingSkusUseCase.subPurchasedFlow.collect {
                _eventCloseScreen.postValue(Unit)
            }
        }
    }

    override fun onReduceState(viewAction: Action) = state.copy()

    data class ViewState(
        val sub: BillingManager.Sub? = null
    ) : MviViewState

    sealed class Action : MviAction
}