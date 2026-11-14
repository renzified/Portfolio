package com.rsalanga.knowledgebase.mvvm.feature.tasks.detail

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rsalanga.knowledgebase.mvvm.core.WhileUiSubscribed
import com.rsalanga.knowledgebase.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.mvvm.core.ui.EmptyState
import com.rsalanga.knowledgebase.mvvm.core.ui.FullScreenLoading
import com.rsalanga.knowledgebase.mvvm.core.ui.RuleNote
import com.rsalanga.knowledgebase.mvvm.data.label.LabelRepository
import com.rsalanga.knowledgebase.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.mvvm.data.task.TaskRepository
import com.rsalanga.knowledgebase.mvvm.feature.tasks.list.TaskUi
import com.rsalanga.knowledgebase.mvvm.feature.tasks.list.toUi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TaskDetailUiState {
    data object Loading : TaskDetailUiState

    /** The task was deleted elsewhere while this screen was open (MVVM-N5: we observe). */
    data object NotFound : TaskDetailUiState

    data class Content(val task: TaskUi) : TaskDetailUiState
}

/**
 * MVVM-P5 / N2: [taskId] comes from `TaskDetailKey` through the constructor (`parametersOf`).
 * It is fixed for the VM's lifetime; there is no `load(id)` call after creation (MVVM-E5).
 *
 * MVVM-N1: the key carries only the id. This VM observes the repository, so it always shows
 * the current task, including edits made on other screens.
 */
class TaskDetailViewModel(
    private val taskId: Long,
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

@Composable
fun TaskDetailRoute(onEdit: () -> Unit, onBack: () -> Unit, viewModel: TaskDetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TaskDetailScreen(
        state = state,
        onToggleDone = viewModel::toggleDone,
        onEditClick = onEdit,
        onBack = onBack
    )
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
                    "N1/N2: TaskDetailKey carries only the id; this VM got it through its " +
                        "constructor. Toggle here and watch the list update (N5)."
                )
            }
        }
    }
}
