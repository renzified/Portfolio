package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.EmptyState
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.FullScreenLoading
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskRepository
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list.TaskUi
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list.toUi
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.EditTaskKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed interface TaskDetailUiState {
    data object Loading : TaskDetailUiState

    /** The task was deleted elsewhere while this screen was open (FMVVM-N5: we observe). */
    data object NotFound : TaskDetailUiState

    data class Content(val task: TaskUi) : TaskDetailUiState
}

/**
 * FMVVM-P5 / N2: [taskId] comes from `TaskDetailKey` through the constructor, decoded with
 * `SavedStateHandle.toRoute()` in the Koin definition (`di/AppModules.kt`).
 * It is fixed for the VM's lifetime; there is no `load(id)` call after creation (FMVVM-E5).
 *
 * FMVVM-N1: the key carries only the id. This VM observes the repository, so it always shows
 * the current task, including edits made on other screens.
 */
class TaskDetailViewModel(
    /** Public: the Fragment needs it to open the editor for this task (FMVVM-N1). */
    val taskId: Long,
    private val repository: TaskRepository,
    labelRepository: LabelRepository
) : ViewModel() {

    val uiState: StateFlow<TaskDetailUiState> = combine(
        repository.observeTask(TaskId(taskId)),
        labelRepository.observeLabels()
    ) { task, labels ->
        if (task == null) {
            TaskDetailUiState.NotFound
        } else {
            TaskDetailUiState.Content(task.toUi(labels.associate { it.id to it.name }))
        }
    }.stateIn(viewModelScope, WhileUiSubscribed, TaskDetailUiState.Loading)

    fun toggleDone() {
        viewModelScope.launch { repository.toggleDone(TaskId(taskId)) }
    }
}

class TaskDetailFragment : Fragment() {

    // FMVVM-N2 / P5: the Koin definition decodes TaskDetailKey from this Fragment's
    // SavedStateHandle and passes the id to the VM constructor. The Fragment and the Screen
    // never touch the key.
    private val viewModel: TaskDetailViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        TaskDetailScreen(
            state = state,
            onToggleDone = viewModel::toggleDone,
            onEditClick = { findNavController().navigate(EditTaskKey(viewModel.taskId)) },
            onBack = { navigateUp() }
        )
    }
}

@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    onToggleDone: () -> Unit,
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(title = "Task", onBack = onBack, modifier = modifier) { padding ->
        when (state) {
            TaskDetailUiState.Loading -> FullScreenLoading(Modifier.padding(padding))

            TaskDetailUiState.NotFound ->
                EmptyState("This task no longer exists", Modifier.padding(padding))

            is TaskDetailUiState.Content -> Column(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(state.task.title, style = MaterialTheme.typography.headlineSmall)
                Text("Priority: ${state.task.priority}")
                Text("Label: ${state.task.labelName ?: "none"}")
                Text(if (state.task.done) "Done" else "Not done")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onToggleDone) {
                        Text(if (state.task.done) "Mark not done" else "Mark done")
                    }
                    Button(onClick = onEditClick) { Text("Edit") }
                }
                RuleNote(
                    "N1/N2: TaskDetailKey carries only the id; Koin decoded it with toRoute() " +
                        "and passed it to the VM constructor. Toggle here and go back: the list " +
                        "is already updated (N5)."
                )
            }
        }
    }
}
