package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.DemoScaffold
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.ErrorState
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.FullScreenLoading
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.asString
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.composeScreen
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId
import com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list.components.TaskListContent
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.EditTaskKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.RenameTaskKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.TaskDetailKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.navigateUp
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * FMVVM-E2: more than six actions → one stable holder instead of a long lambda list (and
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
 * FMVVM-L1: state flows down into TaskListScreen as a value; events flow up as the lambdas below.
 * FMVVM-L2: the stateful half is the Fragment. It obtains the VM (FMVVM-L9), collects state,
 * wires lambdas, runs effects, and navigates through its NavController (FMVVM-E5).
 * FMVVM-F2: no constructor arguments and no screen state in fields.
 */
class TaskListFragment : Fragment() {

    private val viewModel: TaskListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeScreen { Content() } // FMVVM-F1

    @Composable
    private fun Content() {
        // FMVVM-L6: LocalLifecycleOwner is the viewLifecycleOwner here (FMVVM-F3).
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // FMVVM-E5: the NavController is looked up when navigating, not during composition.
        val actions = remember(viewModel) {
            TaskListActions(
                // FMVVM-E1: UI-only, the VM is not involved. FMVVM-N1: the key carries the id.
                onTaskClick = { id -> findNavController().navigate(TaskDetailKey(id.value)) },
                onAddClick = { findNavController().navigate(EditTaskKey(taskId = null)) },
                onToggleDone = viewModel::toggleDone, // FMVVM-E2: method references to intents
                onDelete = viewModel::delete,
                onRename = { id -> findNavController().navigate(RenameTaskKey(id.value)) },
                onFilterSelect = viewModel::selectFilter,
                onRefreshClick = viewModel::refresh,
                onRetryClick = viewModel::retry
            )
        }

        // FMVVM-E3 / E6: show the message, keyed on the outcome, then report it handled.
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
            actions = actions,
            snackbarHostState = snackbarHostState,
            onBack = { navigateUp() }
        )
    }
}

/**
 * FMVVM-L2 / L3: the stateless half. State + lambdas only — no ViewModel, no Flow, no
 * repository. Previewable (TaskListPreviews.kt) and testable without a VM (FMVVM-T4).
 */
@Composable
fun TaskListScreen(
    state: TaskListUiState,
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
                is TaskListUiState.Content -> TaskListContent(state, actions)
            }
        }
    }
}
