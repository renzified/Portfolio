package com.rsalanga.knowledgebase.mvvm.feature.store

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.mvvm.core.ui.asPrice
import com.rsalanga.knowledgebase.mvvm.data.store.CartRepository
import com.rsalanga.knowledgebase.mvvm.data.store.ProductRepository
import com.rsalanga.knowledgebase.mvvm.feature.cartbadge.CartBadge
import com.rsalanga.knowledgebase.mvvm.feature.cartbadge.CartBadgeViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class ProductUi(val id: Long, val name: String, val priceCents: Long, val inCart: Int)

data class StoreUiState(val products: List<ProductUi> = emptyList())

/** MVVM-V1: the destination's own ViewModel. */
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

    /** MVVM-V4: writes go to the repository; the badge VM sees them through its own flow. */
    fun addToCart(productId: Long) {
        viewModelScope.launch { cartRepository.add(productId) }
    }
}

/** MVVM-V3: two ViewModels, both obtained here in the Route — never below it (MVVM-L3). */
@Composable
fun StoreRoute(
    onOpenCheckout: () -> Unit,
    onBack: () -> Unit,
    viewModel: StoreViewModel = koinViewModel(),
    cartBadgeViewModel: CartBadgeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cartCount by cartBadgeViewModel.count.collectAsStateWithLifecycle()
    StoreScreen(
        state = state,
        cartCount = cartCount,
        onAddClick = viewModel::addToCart,
        onCartClick = onOpenCheckout,
        onBack = onBack
    )
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
