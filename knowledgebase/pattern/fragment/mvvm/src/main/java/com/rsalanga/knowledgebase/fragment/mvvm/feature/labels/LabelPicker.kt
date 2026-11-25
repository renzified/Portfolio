package com.rsalanga.knowledgebase.fragment.mvvm.feature.labels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.Label
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.LABEL_PICKED_ID
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.LABEL_PICKED_RESULT
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.viewmodel.ext.android.viewModel

/** FMVVM-M6: the domain [Label] already has the right shape, so it is used directly. */
data class LabelPickerUiState(
    val labels: List<Label> = emptyList(),
    val selectedLabelId: Long? = null
)

/**
 * FMVVM-N1 / N2: [selectedLabelId] is a small parameter from `LabelPickerKey`. Like every key
 * value, it reaches the screen through the VM (decoded in the Koin definition), never by the
 * Fragment reading its arguments.
 */
class LabelPickerViewModel(private val selectedLabelId: Long?, labelRepository: LabelRepository) :
    ViewModel() {
    val uiState: StateFlow<LabelPickerUiState> = labelRepository.observeLabels()
        .map { LabelPickerUiState(it, selectedLabelId) }
        .stateIn(
            viewModelScope,
            WhileUiSubscribed,
            LabelPickerUiState(selectedLabelId = selectedLabelId)
        )
}

/**
 * FMVVM-N4, steps 1–2: the result screen doesn't know who opened it. It sets a Fragment result
 * under an explicit request key (declared next to its nav key), then pops itself. Payload: an
 * id (FMVVM-N1).
 */
class LabelPickerFragment : Fragment() {

    private val viewModel: LabelPickerViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LabelPickerScreen(
            labels = state.labels,
            selectedLabelId = state.selectedLabelId,
            onLabelClick = ::pick,
            onBack = { navigateUp() }
        )
    }

    private fun pick(labelId: Long) {
        setFragmentResult(LABEL_PICKED_RESULT, bundleOf(LABEL_PICKED_ID to labelId))
        findNavController().popBackStack()
    }
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
                    "N4: choosing a label sets a Fragment result and pops. The editor's " +
                        "result listener forwards it to EditTaskViewModel.selectLabel() once."
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
