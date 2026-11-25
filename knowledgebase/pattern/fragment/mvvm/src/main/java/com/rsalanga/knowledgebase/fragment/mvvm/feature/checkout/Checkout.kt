package com.rsalanga.knowledgebase.fragment.mvvm.feature.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asPrice
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.CartLine
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.CartRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.PromoRepository
import com.rsalanga.knowledgebase.fragment.mvvm.feature.cartbadge.CartBadge
import com.rsalanga.knowledgebase.fragment.mvvm.feature.cartbadge.CartBadgeViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.CheckoutFlowKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.viewmodel.ext.android.viewModel

data class CartLineUi(
    val productId: Long,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int
)

private fun CartLine.toUi() = CartLineUi(product.id, product.name, product.priceCents, quantity)

data class CheckoutUiState(
    val lines: List<CartLineUi> = emptyList(),
    val promo: PromoState = PromoState()
) {
    // FMVVM-M2: derived values are computed, never stored next to `lines`.
    val subtotalCents: Long get() = lines.sumOf { it.unitPriceCents * it.quantity }
    val totalCents: Long
        get() = (subtotalCents - (promo.applied?.amountCents ?: 0L)).coerceAtLeast(0L)
    val canContinue: Boolean get() = lines.isNotEmpty()
}

/**
 * FMVVM-V2: one VM for the screen, one `uiState` (FMVVM-C3); the promo concern lives in the
 * [PromoCodeController] delegate it owns.
 */
class CheckoutViewModel(cartRepository: CartRepository, promoRepository: PromoRepository) :
    ViewModel() {

    /** FMVVM-I1: text the VM submits. */
    val promoCode = TextFieldState()
    private val promo = PromoCodeController(promoRepository, viewModelScope)

    val uiState: StateFlow<CheckoutUiState> =
        combine(cartRepository.observeCart(), promo.state) { cart, promoState ->
            CheckoutUiState(lines = cart.lines.map { it.toUi() }, promo = promoState)
        }.stateIn(viewModelScope, WhileUiSubscribed, CheckoutUiState())

    fun applyPromo() = promo.apply(promoCode.text.toString())
}

class CheckoutFragment : Fragment() {
    private val viewModel: CheckoutViewModel by viewModel()

    // FMVVM-V3: the same badge VM, reused on a second screen.
    private val cartBadgeViewModel: CartBadgeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val cartCount by cartBadgeViewModel.count.collectAsStateWithLifecycle()
        CheckoutScreen(
            state = state,
            cartCount = cartCount,
            promoCode = viewModel.promoCode,
            onApplyPromoClick = viewModel::applyPromo,
            onContinueClick = { findNavController().navigate(CheckoutFlowKey) },
            onBack = { navigateUp() }
        )
    }
}

@Suppress("LongParameterList") // A Screen with a text field + four callbacks; still small.
@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    cartCount: Int,
    promoCode: TextFieldState,
    onApplyPromoClick: () -> Unit,
    onContinueClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Checkout", onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CartBadge(count = cartCount, onClick = {})
            state.lines.forEach { line ->
                Text("${line.quantity} × ${line.name} · ${line.unitPriceCents.asPrice()}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    state = promoCode,
                    label = { Text("Promo code (try SAVE5)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onApplyPromoClick, enabled = !state.promo.isApplying) {
                    Text("Apply")
                }
            }
            state.promo.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
            state.promo.applied?.let { Text("Applied ${it.code}: −${it.amountCents.asPrice()}") }
            Text("Subtotal ${state.subtotalCents.asPrice()}")
            Text(
                "Total ${state.totalCents.asPrice()}",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onContinueClick,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue to checkout wizard") }
            RuleNote(
                "V2: PromoCodeController is a plain class owned by CheckoutViewModel. " +
                    "M2: subtotal and total are computed properties."
            )
        }
    }
}
