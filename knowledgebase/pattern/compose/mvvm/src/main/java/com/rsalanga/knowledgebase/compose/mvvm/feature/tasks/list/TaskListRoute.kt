package com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.ErrorState
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.FullScreenLoading
import com.rsalanga.knowledgebase.compose.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.compose.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.compose.mvvm.feature.tasks.list.components.TaskListContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * MVVM-E2: more than six actions → one stable holder instead of a long lambda list (and
 * instead of an MVI-style Action sealed class).
 */
@Suppress("LongParameterList") // A holder of lambdas is the point of this class.
@Immutable
data class TaskListActions(
    val onTaskClick: (TaskId) -> Unit,
    val onAddClick: () -> Unit,
    val onToggleDone: (TaskId) -> Unit,
    val onDelete: (TaskId) -> Unit,
    val onRename: (TaskId) -> Unit,
    val onFilterSelect: (TaskFilter) -> Unit,
    val onRefreshClick: () -> Unit,
    val onRetryClick: () -> Unit
)

/**
 * MVVM-L2: the stateful half. Obtains the VM, collects state, wires lambdas and runs effects.
 * Navigation is passed IN as lambdas (MVVM-E5); the Route never touches the back stack.
 */
@Composable
fun TaskListRoute(
    selectedTaskId: Long?,
    onOpenTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onRenameTask: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TaskListViewModel = koinViewModel()
) {
    // MVVM-L6
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val actions = remember(viewModel, onOpenTask, onAddTask, onRenameTask) {
        TaskListActions(
            onTaskClick = { id -> onOpenTask(id.value) }, // MVVM-E1: UI-only, VM not involved
            onAddClick = onAddTask,
            onToggleDone = viewModel::toggleDone, // MVVM-E2: method references to intents
            onDelete = viewModel::delete,
            onRename = { id -> onRenameTask(id.value) },
            onFilterSelect = viewModel::selectFilter,
            onRefreshClick = viewModel::refresh,
            onRetryClick = viewModel::retry
        )
    }

    // MVVM-E3 / E6: show the message, keyed on the outcome, then report it handled.
    val message = (state as? TaskListUiState.Content)?.userMessage
    if (message != null) {
        val text = message.asString()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            viewModel.messageShown()
        }
    }

    TaskListScreen(
        state = state,
        selectedTaskId = selectedTaskId?.let { TaskId(it) },
        actions = actions,
        snackbarHostState = snackbarHostState,
        onBack = onBack
    )
}

/**
 * MVVM-L2 / L3: the stateless half. State + lambdas only — no ViewModel, no Flow, no
 * repository. Previewable (TaskListPreviews.kt) and testable without a VM (MVVM-T4).
 */
@Composable
fun TaskListScreen(
    state: TaskListUiState,
    selectedTaskId: TaskId?,
    actions: TaskListActions,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DemoScaffold(
        title = "Tasks",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = actions.onAddClick) { Text("New task") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                TaskListUiState.Loading -> FullScreenLoading()
                is TaskListUiState.Error -> ErrorState(state.message, actions.onRetryClick)
                is TaskListUiState.Content -> TaskListContent(state, selectedTaskId, actions)
            }
        }
    }
}
