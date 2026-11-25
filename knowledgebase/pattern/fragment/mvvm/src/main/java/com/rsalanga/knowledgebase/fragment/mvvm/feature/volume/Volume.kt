package com.rsalanga.knowledgebase.fragment.mvvm.feature.volume

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.SettingsRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val PERCENT = 100

data class VolumeUiState(val volume: Float = 0f, val commits: Int = 0)

class VolumeViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val commits = MutableStateFlow(0)

    val uiState: StateFlow<VolumeUiState> =
        combine(repository.observeVolume(), commits) { volume, count ->
            VolumeUiState(volume, count)
        }.stateIn(viewModelScope, WhileUiSubscribed, VolumeUiState())

    /** FMVVM-I2: called once per gesture, with the final value only. */
    fun setVolume(volume: Float) {
        commits.update { it + 1 }
        viewModelScope.launch { repository.setVolume(volume) }
    }
}

class VolumeFragment : Fragment() {
    private val viewModel: VolumeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        VolumeScreen(state = state, onVolumeCommit = viewModel::setVolume, onBack = {
            navigateUp()
        })
    }
}

@Composable
fun VolumeScreen(
    state: VolumeUiState,
    onVolumeCommit: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Volume", onBack = onBack, modifier = modifier) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VolumeSlider(volume = state.volume, onVolumeCommit = onVolumeCommit)
            Text("Saved volume: ${(state.volume * PERCENT).toInt()}%")
            Text("Values sent to the VM: ${state.commits}")
            RuleNote("I2: dragging updates only local state. The VM gets one call when you let go.")
        }
    }
}

/**
 * FMVVM-I2: per-frame drag values never go to the VM. They stay in local state while dragging;
 * only the result is committed.
 */
@Composable
fun VolumeSlider(volume: Float, onVolumeCommit: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    Slider(
        value = dragging ?: volume,
        onValueChange = { dragging = it }, // per frame → local only
        onValueChangeFinished = {
            dragging?.let(onVolumeCommit) // once → VM
            dragging = null
        },
        modifier = modifier
    )
}
