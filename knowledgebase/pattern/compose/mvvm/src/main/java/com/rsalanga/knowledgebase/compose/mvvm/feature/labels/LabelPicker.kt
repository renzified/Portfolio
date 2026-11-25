package com.rsalanga.knowledgebase.compose.mvvm.feature.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.compose.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.compose.mvvm.data.label.Label
import com.rsalanga.knowledgebase.compose.mvvm.data.label.LabelRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

/** MVVM-M6: the domain [Label] already has the right shape, so it is used directly. */
data class LabelPickerUiState(val labels: List<Label> = emptyList())

class LabelPickerViewModel(labelRepository: LabelRepository) : ViewModel() {
    val uiState: StateFlow<LabelPickerUiState> = labelRepository.observeLabels()
        .map { LabelPickerUiState(it) }
        .stateIn(viewModelScope, WhileUiSubscribed, LabelPickerUiState())
}

/**
 * MVVM-N4, step 1: the result screen exposes a lambda and doesn't know who opened it. The entry
 * block implements [onLabelPicked] with `sendResult` + pop.
 *
 * MVVM-N1: [selectedLabelId] arrives in the key as a small parameter.
 */
@Composable
fun LabelPickerRoute(
    selectedLabelId: Long?,
    onLabelPicked: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LabelPickerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LabelPickerScreen(
        labels = state.labels,
        selectedLabelId = selectedLabelId,
        onLabelClick = onLabelPicked,
        onBack = onBack
    )
}

@Composable
fun LabelPickerScreen(
    labels: List<Label>,
    selectedLabelId: Long?,
    onLabelClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Pick a label", onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                RuleNote(
                    "N4: choosing a label sends it through ResultEventBus and pops. The editor's " +
                        "entry block forwards it to EditTaskViewModel.selectLabel() once."
                )
            }
            items(items = labels, key = { it.id }) { label ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onLabelClick(label.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = label.id == selectedLabelId,
                        onClick = { onLabelClick(label.id) }
                    )
                    Text(label.name)
                }
            }
        }
    }
}
