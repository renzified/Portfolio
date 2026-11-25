package com.rsalanga.knowledgebase.fragment.mvvm.feature.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.ErrorState
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.FullScreenLoading
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.ProfileRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.androidx.viewmodel.ext.android.viewModel

data class ProfileUi(val name: String, val email: String)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Error(val message: UiText) : ProfileUiState

    data class Content(val profile: ProfileUi) : ProfileUiState
}

private val LoadFailed = UiText.Res(R.string.profile_load_failed)

/**
 * FMVVM-P3: no `init { viewModelScope.launch { load() } }`. The one-shot load is a cold flow inside
 * the stateIn chain, restarted by a retry trigger. It runs only when the UI subscribes, can be
 * retried cleanly, and is trivial to test.
 */
class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val retries = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProfileUiState> = retries
        .flatMapLatest { _ ->
            flow<ProfileUiState> {
                emit(ProfileUiState.Loading)
                emit(
                    repository.loadProfile().fold(
                        onSuccess = { ProfileUiState.Content(ProfileUi(it.name, it.email)) },
                        onFailure = { ProfileUiState.Error(LoadFailed) }
                    )
                )
            }
        }
        .stateIn(viewModelScope, WhileUiSubscribed, ProfileUiState.Loading)

    fun retry() = retries.update { it + 1 }
}

class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ProfileScreen(state = state, onRetryClick = viewModel::retry, onBack = { navigateUp() })
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onRetryClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Profile", onBack = onBack, modifier = modifier) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (state) {
            ProfileUiState.Loading -> FullScreenLoading(contentModifier)

            is ProfileUiState.Error -> ErrorState(state.message, onRetryClick, contentModifier)

            is ProfileUiState.Content -> Column(
                contentModifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(state.profile.name, style = MaterialTheme.typography.headlineSmall)
                Text(state.profile.email)
                OutlinedButton(onClick = onRetryClick) { Text("Reload") }
                RuleNote(
                    "P3: the first load failed on purpose; Retry bumped a trigger flow and the " +
                        "cold load ran again. Nothing runs in init {}."
                )
            }
        }
    }
}
