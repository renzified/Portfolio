package com.rsalanga.knowledgebase.fragment.mvvm.feature.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.SearchRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val DEBOUNCE_MS = 300L

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Searching : SearchUiState

    data class Results(val items: List<String>) : SearchUiState

    data class Error(val message: UiText) : SearchUiState
}

/**
 * FMVVM-I4: debounce lives in the VM, not in the UI. `flatMapLatest` cancels the previous search
 * when the query changes. The query itself is a VM-owned TextFieldState (FMVVM-I1).
 */
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    val query = TextFieldState()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = snapshotFlow { query.text.toString().trim() }
        .debounce(DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { text -> if (text.isEmpty()) flowOf(SearchUiState.Idle) else search(text) }
        .stateIn(viewModelScope, WhileUiSubscribed, SearchUiState.Idle)

    private fun search(text: String) = flow<SearchUiState> {
        emit(SearchUiState.Searching)
        emit(
            repository.search(text).fold(
                onSuccess = { SearchUiState.Results(it) },
                onFailure = { SearchUiState.Error(UiText.Res(R.string.search_failed)) }
            )
        )
    }
}

class SearchFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        SearchScreen(state = state, query = viewModel.query, onBack = { navigateUp() })
    }
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    query: TextFieldState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Search fruit", onBack = onBack, modifier = modifier) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                state = query,
                label = { Text("Query (type \"error\" to fail)") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            RuleNote("I4: 300 ms debounce + flatMapLatest, in the VM.")
            when (state) {
                SearchUiState.Idle -> Unit

                SearchUiState.Searching -> LinearProgressIndicator(Modifier.fillMaxWidth())

                is SearchUiState.Error ->
                    Text(state.message.asString(), color = MaterialTheme.colorScheme.error)

                is SearchUiState.Results -> LazyColumn {
                    if (state.items.isEmpty()) item { Text("No matches", Modifier.padding(16.dp)) }
                    items(items = state.items, key = { it }) { fruit ->
                        Text(fruit, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}
