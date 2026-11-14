package com.rsalanga.knowledgebase.mvvm.feature.checkoutflow

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.mvvm.R
import com.rsalanga.knowledgebase.mvvm.core.UiText
import com.rsalanga.knowledgebase.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.mvvm.data.store.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CARD_LENGTH = 16
private const val LAST_DIGITS = 4

/** The in-progress, not-yet-persisted data the steps share. Only the flow VM writes it. */
data class CheckoutDraft(
    val cardLast4: String? = null,
    val isPlacing: Boolean = false,
    val finished: Boolean = false
)

data class CheckoutFlowUiState(
    val address: String = "",
    val cardLast4: String? = null,
    val isPlacing: Boolean = false,
    /** MVVM-E3: outcome → the flow Route leaves the whole flow. */
    val finished: Boolean = false
) {
    val hasAddress: Boolean get() = address.isNotBlank() // MVVM-M2
}

/**
 * MVVM-V5: the flow ViewModel. Created by `CheckoutFlowRoute`, so it is scoped to the single
 * `CheckoutFlowKey` entry on the app back stack and cleared when the user leaves the flow.
 */
class CheckoutFlowViewModel(
    private val cartRepository: CartRepository,
    private val userMessages: UserMessages
) : ViewModel() {

    val address = TextFieldState()
    private val draft = MutableStateFlow(CheckoutDraft())

    val uiState: StateFlow<CheckoutFlowUiState> =
        combine(snapshotFlow { address.text.toString() }, draft) { text, current ->
            CheckoutFlowUiState(
                address = text,
                cardLast4 = current.cardLast4,
                isPlacing = current.isPlacing,
                finished = current.finished
            )
        }.stateIn(viewModelScope, WhileUiSubscribed, CheckoutFlowUiState())

    /** Called by the payment step's Route with what the step VM produced (VMs never meet). */
    fun setCard(last4: String) = draft.update { it.copy(cardLast4 = last4) }

    fun placeOrder() {
        draft.update { it.copy(isPlacing = true) }
        viewModelScope.launch {
            cartRepository.clear()
            userMessages.post(UiText.Res(R.string.order_placed)) // MVVM-N6: outlives the flow
            draft.update { it.copy(isPlacing = false, finished = true) }
        }
    }

    fun finishHandled() = draft.update { it.copy(finished = false) }
}

data class PaymentUiState(val canContinue: Boolean = false, val last4: String = "")

/**
 * MVVM-V5: an optional step ViewModel for step-local logic (card validation). Scoped to the
 * payment step's inner entry. It hands its result to its Route, which calls the flow VM.
 */
class PaymentViewModel : ViewModel() {
    val cardNumber = TextFieldState()

    val uiState: StateFlow<PaymentUiState> = snapshotFlow { cardNumber.text.toString() }
        .map { digits ->
            PaymentUiState(
                canContinue = digits.length == CARD_LENGTH && digits.all(Char::isDigit),
                last4 = digits.takeLast(LAST_DIGITS)
            )
        }
        .stateIn(viewModelScope, WhileUiSubscribed, PaymentUiState())
}
