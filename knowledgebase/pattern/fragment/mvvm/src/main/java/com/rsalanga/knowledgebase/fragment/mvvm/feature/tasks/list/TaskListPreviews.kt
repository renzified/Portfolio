package com.rsalanga.knowledgebase.fragment.mvvm.feature.tasks.list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.core.ui.MvvmTheme
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.Priority
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskFilter
import com.rsalanga.knowledgebase.fragment.mvvm.data.task.TaskId

// FMVVM-T4: one preview per sealed phase. Possible only because the Screen is stateless (L2).

private val NoActions = TaskListActions(
    onTaskClick = {},
    onAddClick = {},
    onToggleDone = {},
    onDelete = {},
    onRename = {},
    onFilterSelect = {},
    onRefreshClick = {},
    onRetryClick = {}
)

private val SampleTasks = listOf(
    TaskUi(TaskId(1), "Buy milk", done = false, priority = Priority.Low, labelName = "Errands"),
    TaskUi(TaskId(2), "Write rules", done = true, priority = Priority.High, labelName = "Work")
)

@Composable
private fun PreviewFrame(state: TaskListUiState) {
    MvvmTheme {
        TaskListScreen(
            state = state,
            actions = NoActions,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun TaskListLoadingPreview() = PreviewFrame(TaskListUiState.Loading)

@Preview
@Composable
private fun TaskListErrorPreview() = PreviewFrame(TaskListUiState.Error(UiText.Raw("Offline")))

@Preview
@Composable
private fun TaskListContentPreview() =
    PreviewFrame(TaskListUiState.Content(tasks = SampleTasks, filter = TaskFilter.All))

@Preview
@Composable
private fun TaskListEmptyPreview() =
    PreviewFrame(TaskListUiState.Content(tasks = emptyList(), filter = TaskFilter.Done))
