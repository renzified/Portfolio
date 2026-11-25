package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.rename

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.R
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

data class RenameTaskUiState(
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val error: UiText? = null,
    /** FMVVM-E3: outcome → the DialogFragment closes itself. */
    val dismissed: Boolean = false
)

/**
 * FMVVM-V7: "Rename" is a dialog *destination* (`dialog<RenameTaskDialogFragment, RenameTaskKey>`)
 * with its own data and logic (it saves), so it gets its own ViewModel — unlike the confirm
 * delete dialog in the list, which is just UI state inside the list's Screen.
 */
class RenameTaskViewModel(private val taskId: Long, private val repository: TaskRepository) :
    ViewModel() {

    val name = TextFieldState()
    private val outcome = MutableStateFlow(RenameTaskUiState())
    private var prefilled = false

    private val prefill = flow {
        if (!prefilled) {
            repository.observeTask(TaskId(taskId)).first()?.let { task ->
                name.setTextAndPlaceCursorAtEnd(task.title)
            }
            prefilled = true
        }
        emit(Unit)
    }

    val uiState: StateFlow<RenameTaskUiState> = combine(
        prefill,
        snapshotFlow { name.text.isNotBlank() },
        outcome
    ) { _, hasName, current -> current.copy(canSave = hasName && !current.isSaving) }
        .stateIn(viewModelScope, WhileUiSubscribed, RenameTaskUiState())

    fun save() {
        val newName = name.text.toString()
        outcome.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = repository.rename(TaskId(taskId), newName)
            outcome.update {
                if (result.isSuccess) {
                    it.copy(isSaving = false, dismissed = true)
                } else {
                    it.copy(isSaving = false, error = UiText.Res(R.string.rename_failed))
                }
            }
        }
    }

    fun dismissHandled() = outcome.update { it.copy(dismissed = false) }
}

/**
 * FMVVM-V7: a DialogFragment destination. Same split as every destination (FMVVM-L2): the
 * DialogFragment owns the VM and the navigation, the Screen is stateless. The Navigation
 * component pops this entry when the dialog is dismissed (back, outside touch, or
 * `popBackStack()`), which clears the VM.
 */
class RenameTaskDialogFragment : DialogFragment() {

    private val viewModel: RenameTaskViewModel by viewModel() // taskId from RenameTaskKey (P5)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.dismissed) {
            if (state.dismissed) {
                viewModel.dismissHandled()
                findNavController().popBackStack()
            }
        }
        RenameTaskScreen(
            state = state,
            name = viewModel.name,
            onSaveClick = viewModel::save,
            onCancelClick = { findNavController().popBackStack() }
        )
    }
}

@Composable
fun RenameTaskScreen(
    state: RenameTaskUiState,
    name: TextFieldState,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rename task", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(state = name, label = { Text("Name") })
            state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancelClick) { Text("Cancel") }
                TextButton(onClick = onSaveClick, enabled = state.canSave) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
        }
    }
}
