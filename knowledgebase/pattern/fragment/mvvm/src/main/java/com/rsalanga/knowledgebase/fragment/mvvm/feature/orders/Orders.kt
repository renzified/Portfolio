package com.rsalanga.knowledgebase.fragment.mvvm.feature.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asPrice
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.Order
import com.rsalanga.knowledgebase.fragment.mvvm.data.OrderTab
import com.rsalanga.knowledgebase.fragment.mvvm.data.OrdersRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.viewmodel.ext.android.viewModel

data class OrdersUiState(
    val selectedTab: OrderTab = OrderTab.Active,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * FMVVM-S6: a tab index starts as UI state (`rememberSaveable`). Here the selected tab changes
 * what is loaded, so it moved into the VM (FMVVM-S1), stored in SavedStateHandle because it is
 * user input that should survive process death (FMVVM-P5).
 */
class OrdersViewModel(
    private val repository: OrdersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tab = savedStateHandle.getStateFlow(KEY_TAB, OrderTab.Active)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<OrdersUiState> = tab
        .flatMapLatest { selected ->
            repository.observeOrders(selected)
                .map { orders -> OrdersUiState(selected, orders, isLoading = false) }
                .onStart { emit(OrdersUiState(selected, isLoading = true)) }
        }
        .stateIn(viewModelScope, WhileUiSubscribed, OrdersUiState())

    fun selectTab(tab: OrderTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    private companion object {
        const val KEY_TAB = "tab"
    }
}

class OrdersFragment : Fragment() {
    private val viewModel: OrdersViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        OrdersScreen(state = state, onTabSelect = viewModel::selectTab, onBack = { navigateUp() })
    }
}

@Composable
fun OrdersScreen(
    state: OrdersUiState,
    onTabSelect: (OrderTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Orders", onBack = onBack, modifier = modifier) { padding ->
        Column(Modifier.padding(padding)) {
            RuleNote("S6: the tab drives the query, so the VM owns it. Rotate: it survives.")
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrderTab.entries.forEach { tab ->
                    FilterChip(
                        selected = tab == state.selectedTab,
                        onClick = { onTabSelect(tab) },
                        label = { Text(tab.name) }
                    )
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.padding(16.dp))
            LazyColumn {
                items(items = state.orders, key = { it.id }) { order ->
                    Text("${order.title} · ${order.totalCents.asPrice()}", Modifier.padding(16.dp))
                }
            }
        }
    }
}
