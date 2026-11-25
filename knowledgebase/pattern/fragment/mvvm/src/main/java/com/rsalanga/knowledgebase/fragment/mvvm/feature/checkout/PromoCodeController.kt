package com.rsalanga.knowledgebase.fragment.mvvm.feature.checkout

import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.Discount
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.PromoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PromoState(
    val applied: Discount? = null,
    val isApplying: Boolean = false,
    val error: UiText? = null
)

/**
 * FMVVM-V2: a big ViewModel is split into plain delegate classes, not into more ViewModels.
 * This one owns the promo-code concern and uses the owning VM's scope. Named `XController`
 * (FMVVM-L8). It could be reused by another VM and unit-tested with a `TestScope`.
 */
class PromoCodeController(
    private val promoRepository: PromoRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PromoState())
    val state: StateFlow<PromoState> = _state.asStateFlow()

    fun apply(code: String) {
        _state.update { it.copy(isApplying = true, error = null) }
        scope.launch {
            promoRepository.validate(code)
                .onSuccess { discount -> _state.value = PromoState(applied = discount) }
                .onFailure { _ ->
                    _state.update {
                        it.copy(isApplying = false, error = UiText.Res(R.string.promo_invalid))
                    }
                }
        }
    }
}
