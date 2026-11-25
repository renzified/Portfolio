package com.rsalanga.knowledgebase.fragment.mvvm.feature.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
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
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.CartRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.ProductRepository
import com.rsalanga.knowledgebase.fragment.mvvm.feature.cartbadge.CartBadge
import com.rsalanga.knowledgebase.fragment.mvvm.feature.cartbadge.CartBadgeViewModel
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.CheckoutKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

data class ProductUi(val id: Long, val name: String, val priceCents: Long, val inCart: Int)

data class StoreUiState(val products: List<ProductUi> = emptyList())

/** FMVVM-V1: the destination's own ViewModel. */
class StoreViewModel(
    productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    val uiState: StateFlow<StoreUiState> = combine(
        productRepository.observeProducts(),
        cartRepository.observeCart()
    ) { products, cart ->
        val inCart = cart.lines.associate { it.product.id to it.quantity }
        StoreUiState(products.map { ProductUi(it.id, it.name, it.priceCents, inCart[it.id] ?: 0) })
    }.stateIn(viewModelScope, WhileUiSubscribed, StoreUiState())

    /** FMVVM-V4: writes go to the repository; the badge VM sees them through its own flow. */
    fun addToCart(productId: Long) {
        viewModelScope.launch { cartRepository.add(productId) }
    }
}

/**
 * FMVVM-V3: two ViewModels, both obtained here in the Fragment — never below it (FMVVM-L3).
 * Each is scoped to this Fragment, so both are cleared when it is popped.
 */
class StoreFragment : Fragment() {
    private val viewModel: StoreViewModel by viewModel()
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
        StoreScreen(
            state = state,
            cartCount = cartCount,
            onAddClick = viewModel::addToCart,
            onCartClick = { findNavController().navigate(CheckoutKey) },
            onBack = { navigateUp() }
        )
    }
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    cartCount: Int,
    onAddClick: (Long) -> Unit,
    onCartClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Store", onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Column {
                    CartBadge(cartCount, onCartClick, Modifier.padding(horizontal = 16.dp))
                    RuleNote(
                        "V3/V4: the badge has its own VM. It updates because both VMs observe " +
                            "CartRepository; the VMs never reference each other."
                    )
                }
            }
            items(items = state.products, key = { it.id }) { product ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name)
                        Text("${product.priceCents.asPrice()} · in cart: ${product.inCart}")
                    }
                    OutlinedButton(onClick = { onAddClick(product.id) }) { Text("Add") }
                }
            }
        }
    }
}
