package com.rsalanga.knowledgebase.fragment.mvvm.feature.cartbadge

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.data.store.CartRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * FMVVM-V3: a second ViewModel on a screen is allowed only for a section that
 * 1. has its own data source and logic,
 * 2. appears on several screens (Store and Checkout), and
 * 3. whose state the host screen doesn't read.
 *
 * FMVVM-V4: it stays in sync with the Store because both observe [CartRepository], not because
 * the two VMs talk (FMVVM-L7).
 */
class CartBadgeViewModel(cartRepository: CartRepository) : ViewModel() {
    val count: StateFlow<Int> = cartRepository.observeCart()
        .map { it.itemCount }
        .stateIn(viewModelScope, WhileUiSubscribed, 0)
}

/** Stateless; each host Fragment obtains [CartBadgeViewModel] and passes the count down. */
@Composable
fun CartBadge(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(onClick = onClick, label = { Text("Cart · $count") }, modifier = modifier)
}
